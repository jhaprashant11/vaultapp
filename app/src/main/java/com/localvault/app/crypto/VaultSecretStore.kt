package com.localvault.app.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted preferences for vault bootstrap material (salt, wrapped DEK, biometric blob).
 */
class VaultSecretStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    companion object {
        private const val PREFS_NAME = "vault_secret_store"
        const val KEY_SETUP_COMPLETE = "setup_complete"
        const val KEY_KDF_SALT_B64 = "kdf_salt_b64"
        const val KEY_WRAPPED_DEK_B64 = "wrapped_dek_b64"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_BIOMETRIC_BLOB_B64 = "biometric_blob_b64"
    }
}
