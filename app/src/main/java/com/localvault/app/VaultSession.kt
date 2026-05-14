package com.localvault.app

import android.app.Application
import android.util.Base64
import androidx.fragment.app.FragmentActivity
import androidx.room.Room
import com.localvault.app.crypto.AesGcm
import com.localvault.app.crypto.Argon2Kdf
import com.localvault.app.crypto.BiometricKeyManager
import com.localvault.app.crypto.VaultSecretStore
import com.localvault.app.data.VaultDatabase
import com.localvault.app.security.BiometricPromptHelper
import com.localvault.app.security.LockPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import java.util.Arrays
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

sealed class VaultLockState {
    data object SetupRequired : VaultLockState()
    data object Locked : VaultLockState()
    data object Unlocked : VaultLockState()
    data class Error(val message: String) : VaultLockState()
}

/**
 * Coordinates encrypted DB lifecycle, KDF unwrap, and biometric-wrapped DEK.
 */
class VaultSession(
    private val app: Application,
    private val io: CoroutineContext = Dispatchers.IO,
) {
    private val secretStore = VaultSecretStore(app)
    private val biometricKeyManager = BiometricKeyManager()
    val lockPreferences = LockPreferences(app)

    private val scope = CoroutineScope(SupervisorJob() + io)

    private val _lockState = MutableStateFlow<VaultLockState>(VaultLockState.Locked)
    val lockState: StateFlow<VaultLockState> = _lockState.asStateFlow()

    private val _database = MutableStateFlow<VaultDatabase?>(null)
    val database: StateFlow<VaultDatabase?> = _database.asStateFlow()

    private val backgroundAt = AtomicLong(0L)

    init {
        refreshInitialState()
    }

    fun refreshInitialState() {
        _lockState.value = if (!isSetup()) {
            VaultLockState.SetupRequired
        } else {
            VaultLockState.Locked
        }
    }

    fun isSetup(): Boolean =
        secretStore.prefs.getBoolean(VaultSecretStore.KEY_SETUP_COMPLETE, false)

    fun isBiometricEnabled(): Boolean =
        secretStore.prefs.getBoolean(VaultSecretStore.KEY_BIOMETRIC_ENABLED, false)

    fun markBackgrounded() {
        if (_lockState.value == VaultLockState.Unlocked) {
            val timeoutMinutes = runBlocking { lockPreferences.autoLockMinutes.first() }
            if (timeoutMinutes == 0) {
                lock()
            } else {
                val markedAt = System.currentTimeMillis()
                backgroundAt.set(markedAt)
                scope.launch {
                    delay(timeoutMinutes.toLong() * 60_000L)
                    if (backgroundAt.get() == markedAt && _lockState.value == VaultLockState.Unlocked) {
                        lock()
                    }
                }
            }
        }
    }

    fun maybeAutoLockOnForeground() {
        val wentBg = backgroundAt.get()
        if (wentBg == 0L) return
        if (_lockState.value != VaultLockState.Unlocked) {
            backgroundAt.set(0L)
            return
        }
        val timeoutMs = runBlocking {
            lockPreferences.autoLockMinutes.first().toLong() * 60_000L
        }
        val elapsed = System.currentTimeMillis() - wentBg
        if (elapsed >= timeoutMs) {
            lock()
        }
        backgroundAt.set(0L)
    }

    fun lock() {
        _database.value?.close()
        _database.value = null
        if (isSetup()) {
            _lockState.value = VaultLockState.Locked
        }
    }

    suspend fun completeSetup(
        masterPassword: CharArray,
        enableBiometric: Boolean,
        activity: FragmentActivity?,
    ): Result<Unit> = withContext(io) {
        try {
            if (isSetup()) return@withContext Result.failure(IllegalStateException("Already configured"))
            val salt = Argon2Kdf.randomSalt()
            val dek = ByteArray(Argon2Kdf.KEY_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
            val kek = Argon2Kdf.derive(masterPassword, salt)
            val (iv, ct) = AesGcm.encrypt(kek, dek)
            secretStore.prefs.edit()
                .putBoolean(VaultSecretStore.KEY_SETUP_COMPLETE, true)
                .putString(VaultSecretStore.KEY_KDF_SALT_B64, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(VaultSecretStore.KEY_WRAPPED_DEK_B64, AesGcm.toBase64(iv, ct))
                .putBoolean(VaultSecretStore.KEY_BIOMETRIC_ENABLED, false)
                .remove(VaultSecretStore.KEY_BIOMETRIC_BLOB_B64)
                .apply()
            Arrays.fill(kek, 0)
            val db = openDatabaseLocked(dek)
            _database.value = db
            _lockState.value = VaultLockState.Unlocked
            if (enableBiometric && activity != null && BiometricPromptHelper.canAuthenticateStrong(app)) {
                val copy = dek.copyOf()
                Arrays.fill(dek, 0)
                withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { cont ->
                        enableBiometricInternal(activity, copy) {
                            Arrays.fill(copy, 0)
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }
                }
            } else {
                Arrays.fill(dek, 0)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlockWithMasterPassword(masterPassword: CharArray): Result<Unit> = withContext(io) {
        try {
            if (!isSetup()) return@withContext Result.failure(IllegalStateException("Not setup"))
            val saltB64 = secretStore.prefs.getString(VaultSecretStore.KEY_KDF_SALT_B64, null)
                ?: return@withContext Result.failure(IllegalStateException("Missing salt"))
            val wrappedB64 = secretStore.prefs.getString(VaultSecretStore.KEY_WRAPPED_DEK_B64, null)
                ?: return@withContext Result.failure(IllegalStateException("Missing wrap"))
            val salt = Base64.decode(saltB64, Base64.NO_WRAP)
            val kek = Argon2Kdf.derive(masterPassword, salt)
            val (iv, ct) = AesGcm.fromBase64(wrappedB64)
            val dek = try {
                AesGcm.decrypt(kek, iv, ct)
            } catch (_: Exception) {
                Arrays.fill(kek, 0)
                return@withContext Result.failure(IllegalArgumentException("Wrong password"))
            }
            Arrays.fill(kek, 0)
            val db = openDatabaseLocked(dek)
            _database.value = db
            _lockState.value = VaultLockState.Unlocked
            Arrays.fill(dek, 0)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun prepareBiometricDecryptCipher(): Result<Pair<javax.crypto.Cipher, ByteArray>> {
        val blobB64 = secretStore.prefs.getString(VaultSecretStore.KEY_BIOMETRIC_BLOB_B64, null)
            ?: return Result.failure(IllegalStateException("No biometric blob"))
        return try {
            val combined = Base64.decode(blobB64, Base64.NO_WRAP)
            val ivLength = 12
            val iv = combined.copyOfRange(0, ivLength)
            val cipher = biometricKeyManager.initDecryptCipher(iv)
            Result.success(cipher to combined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun finishBiometricUnlock(cipher: javax.crypto.Cipher, combinedBlob: ByteArray): Result<Unit> {
        return try {
            val ivLength = 12
            val ciphertext = combinedBlob.copyOfRange(ivLength, combinedBlob.size)
            val dek = cipher.doFinal(ciphertext)
            val db = openDatabaseLocked(dek)
            _database.value = db
            _lockState.value = VaultLockState.Unlocked
            Arrays.fill(dek, 0)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startBiometricUnlock(
        activity: FragmentActivity,
        onDone: (Result<Unit>) -> Unit,
    ) {
        if (!isBiometricEnabled()) {
            onDone(Result.failure(IllegalStateException("Biometrics off")))
            return
        }
        val prep = prepareBiometricDecryptCipher()
        val cipher = prep.getOrNull()?.first ?: run {
            onDone(Result.failure(prep.exceptionOrNull() ?: IllegalStateException()))
            return
        }
        val combined = prep.getOrNull()?.second ?: return
        BiometricPromptHelper.authenticateWithCipher(
            activity = activity,
            title = activity.getString(R.string.biometric_prompt_title),
            subtitle = activity.getString(R.string.biometric_prompt_subtitle),
            cipher = cipher,
            onSuccess = { authenticatedCipher ->
                scope.launch {
                    val result = withContext(io) {
                        finishBiometricUnlock(authenticatedCipher, combined)
                    }
                    onDone(result)
                }
            },
            onFailed = { msg ->
                onDone(Result.failure(IllegalStateException(msg)))
            },
        )
    }

    fun enableBiometricFromSettings(activity: FragmentActivity, masterPassword: CharArray, onDone: (Result<Unit>) -> Unit) {
        scope.launch {
            val dek = withContext(io) { unwrapDekFromPrefs(masterPassword) }
            if (dek == null) {
                onDone(Result.failure(IllegalArgumentException("Wrong password")))
                return@launch
            }
            withContext(Dispatchers.Main) {
                enableBiometricInternal(activity, dek) { r ->
                    Arrays.fill(dek, 0)
                    onDone(r)
                }
            }
        }
    }

    private fun unwrapDekFromPrefs(masterPassword: CharArray): ByteArray? {
        return try {
            val saltB64 = secretStore.prefs.getString(VaultSecretStore.KEY_KDF_SALT_B64, null) ?: return null
            val wrappedB64 = secretStore.prefs.getString(VaultSecretStore.KEY_WRAPPED_DEK_B64, null) ?: return null
            val salt = Base64.decode(saltB64, Base64.NO_WRAP)
            val kek = Argon2Kdf.derive(masterPassword, salt)
            val (iv, ct) = AesGcm.fromBase64(wrappedB64)
            val dek = AesGcm.decrypt(kek, iv, ct)
            Arrays.fill(kek, 0)
            dek
        } catch (_: Exception) {
            null
        }
    }

    private fun enableBiometricInternal(
        activity: FragmentActivity,
        dek: ByteArray,
        onDone: ((Result<Unit>) -> Unit)? = null,
    ) {
        try {
            biometricKeyManager.deleteKey()
            biometricKeyManager.getOrCreateSecretKey()
            val encryptCipher = biometricKeyManager.initEncryptCipher()
            BiometricPromptHelper.authenticateWithCipher(
                activity = activity,
                title = activity.getString(R.string.biometric_enable_title),
                subtitle = activity.getString(R.string.biometric_enable_subtitle),
                cipher = encryptCipher,
                onSuccess = { cipher ->
                    scope.launch(io) {
                        try {
                            val encrypted = cipher.doFinal(dek)
                            val iv = cipher.iv ?: ByteArray(0)
                            if (iv.size != 12) {
                                onDone?.invoke(Result.failure(IllegalStateException("Unexpected IV")))
                                return@launch
                            }
                            val blob = iv + encrypted
                            secretStore.prefs.edit()
                                .putBoolean(VaultSecretStore.KEY_BIOMETRIC_ENABLED, true)
                                .putString(VaultSecretStore.KEY_BIOMETRIC_BLOB_B64, Base64.encodeToString(blob, Base64.NO_WRAP))
                                .apply()
                            onDone?.invoke(Result.success(Unit))
                        } catch (e: Exception) {
                            onDone?.invoke(Result.failure(e))
                        }
                    }
                },
                onFailed = { msg ->
                    onDone?.invoke(Result.failure(IllegalStateException(msg)))
                },
            )
        } catch (e: Exception) {
            onDone?.invoke(Result.failure(e))
        }
    }

    fun disableBiometric() {
        biometricKeyManager.deleteKey()
        secretStore.prefs.edit()
            .putBoolean(VaultSecretStore.KEY_BIOMETRIC_ENABLED, false)
            .remove(VaultSecretStore.KEY_BIOMETRIC_BLOB_B64)
            .apply()
    }

    suspend fun changeMasterPassword(oldPassword: CharArray, newPassword: CharArray): Result<Unit> =
        withContext(io) {
            try {
                val saltB64 = secretStore.prefs.getString(VaultSecretStore.KEY_KDF_SALT_B64, null)
                    ?: return@withContext Result.failure(IllegalStateException("Missing salt"))
                val wrappedB64 = secretStore.prefs.getString(VaultSecretStore.KEY_WRAPPED_DEK_B64, null)
                    ?: return@withContext Result.failure(IllegalStateException("Missing wrap"))
                val salt = Base64.decode(saltB64, Base64.NO_WRAP)
                val oldKek = Argon2Kdf.derive(oldPassword, salt)
                val (iv, ct) = AesGcm.fromBase64(wrappedB64)
                val dek = try {
                    AesGcm.decrypt(oldKek, iv, ct)
                } catch (_: Exception) {
                    Arrays.fill(oldKek, 0)
                    return@withContext Result.failure(IllegalArgumentException("Wrong password"))
                }
                Arrays.fill(oldKek, 0)
                _database.value?.close()
                _database.value = null
                val newKek = Argon2Kdf.derive(newPassword, salt)
                val (nIv, nCt) = AesGcm.encrypt(newKek, dek)
                Arrays.fill(newKek, 0)
                secretStore.prefs.edit()
                    .putString(VaultSecretStore.KEY_WRAPPED_DEK_B64, AesGcm.toBase64(nIv, nCt))
                    .apply()
                disableBiometric()
                Arrays.fill(dek, 0)
                lock()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun openDatabaseLocked(passphrase: ByteArray): VaultDatabase {
        _database.value?.close()
        val factory = SupportFactory(passphrase)
        val path = app.getDatabasePath(DB_NAME).absolutePath
        val db = Room.databaseBuilder(app, VaultDatabase::class.java, path)
            .openHelperFactory(factory)
            .addMigrations(VaultDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
        db.openHelper.writableDatabase
        return db
    }

    companion object {
        private const val DB_NAME = "vault.db"
    }
}
