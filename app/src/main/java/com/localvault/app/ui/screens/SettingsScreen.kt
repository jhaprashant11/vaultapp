package com.localvault.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.localvault.app.R
import com.localvault.app.VaultSession
import com.localvault.app.security.BiometricPromptHelper
import com.localvault.app.security.LockPreferences
import com.localvault.app.security.PasswordTools
import com.localvault.app.ui.components.PasswordStrengthBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    session: VaultSession,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val minutes by session.lockPreferences.autoLockMinutes.collectAsState(initial = LockPreferences.DEFAULT_AUTO_LOCK_MINUTES)
    val themeMode by session.lockPreferences.themeMode.collectAsState(initial = LockPreferences.THEME_SYSTEM)
    var minutesDraft by remember { mutableIntStateOf(minutes) }

    LaunchedEffect(minutes) {
        minutesDraft = minutes
    }

    var currentPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var bioConfirmPw by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val lockOptions = listOf(
        0 to stringResource(R.string.lock_immediately),
        1 to stringResource(R.string.lock_1_min),
        5 to stringResource(R.string.lock_5_min),
        15 to stringResource(R.string.lock_15_min),
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip(
                    label = stringResource(R.string.theme_system),
                    selected = themeMode == LockPreferences.THEME_SYSTEM,
                    onClick = {
                        scope.launch { session.lockPreferences.setThemeMode(LockPreferences.THEME_SYSTEM) }
                    },
                )
                ThemeChip(
                    label = stringResource(R.string.theme_light),
                    selected = themeMode == LockPreferences.THEME_LIGHT,
                    onClick = {
                        scope.launch { session.lockPreferences.setThemeMode(LockPreferences.THEME_LIGHT) }
                    },
                )
                ThemeChip(
                    label = stringResource(R.string.theme_dark),
                    selected = themeMode == LockPreferences.THEME_DARK,
                    onClick = {
                        scope.launch { session.lockPreferences.setThemeMode(LockPreferences.THEME_DARK) }
                    },
                )
            }
            Text(stringResource(R.string.os_autofill), style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    } else {
                        Intent(Settings.ACTION_SETTINGS)
                    }
                    runCatching { context.startActivity(intent) }
                        .onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.enable_os_autofill))
            }
            Text(
                stringResource(R.string.os_autofill_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f),
            )
            Text(stringResource(R.string.auto_lock), style = MaterialTheme.typography.titleMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp),
            ) {
                items(lockOptions) { option ->
                    FilterChip(
                        selected = minutesDraft == option.first,
                        onClick = {
                            minutesDraft = option.first
                            scope.launch {
                                session.lockPreferences.setAutoLockMinutes(option.first)
                                message = "Saved auto-lock"
                            }
                        },
                        label = { Text(option.second) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (minutesDraft > 0) minutesDraft-- }) { Text("-") }
                Text(if (minutesDraft == 0) "0" else "$minutesDraft", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { if (minutesDraft < 120) minutesDraft++ }) { Text("+") }
                Button(onClick = {
                    scope.launch {
                        session.lockPreferences.setAutoLockMinutes(minutesDraft)
                        message = "Saved auto-lock"
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
            }
            Text(stringResource(R.string.change_master_password), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = currentPw,
                onValueChange = { currentPw = it },
                label = { Text(stringResource(R.string.current_password)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            PasswordStrengthBar(strength = PasswordTools.strength(newPw))
            OutlinedTextField(
                value = newPw,
                onValueChange = { newPw = it },
                label = { Text(stringResource(R.string.new_password)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = confirmPw,
                onValueChange = { confirmPw = it },
                label = { Text(stringResource(R.string.confirm_password)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (newPw.length < 10) {
                        message = context.getString(R.string.error_weak)
                        return@Button
                    }
                    if (newPw != confirmPw) {
                        message = context.getString(R.string.error_passwords_match)
                        return@Button
                    }
                    scope.launch {
                        val old = currentPw.toCharArray()
                        val new = newPw.toCharArray()
                        val result = session.changeMasterPassword(old, new)
                        old.fill('\u0000')
                        new.fill('\u0000')
                        currentPw = ""
                        newPw = ""
                        confirmPw = ""
                        message = if (result.isSuccess) {
                            "Password changed. Unlock again."
                        } else {
                            result.exceptionOrNull()?.message ?: "Failed"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.change_master_password))
            }
            Text(stringResource(R.string.enable_biometric), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = bioConfirmPw,
                onValueChange = { bioConfirmPw = it },
                label = { Text(stringResource(R.string.confirm_master_password)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (!BiometricPromptHelper.canAuthenticateStrong(context)) {
                        message = "Biometrics not available"
                        return@Button
                    }
                    val pwd = bioConfirmPw.toCharArray()
                    session.enableBiometricFromSettings(activity, pwd) { result ->
                        pwd.fill('\u0000')
                        bioConfirmPw = ""
                        message = if (result.isSuccess) "Biometrics enabled" else result.exceptionOrNull()?.message
                    }
                },
                enabled = !session.isBiometricEnabled(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.enable_biometric))
            }
            OutlinedButton(
                onClick = {
                    session.disableBiometric()
                    message = "Biometrics disabled"
                },
                enabled = session.isBiometricEnabled(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.disable_biometric))
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(stringResource(R.string.banking_notice), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}
