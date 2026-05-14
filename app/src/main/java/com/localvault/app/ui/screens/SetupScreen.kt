package com.localvault.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.localvault.app.security.PasswordTools
import com.localvault.app.ui.components.AnimatedVaultLogo
import com.localvault.app.ui.components.PasswordStrengthBar
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(session: VaultSession) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var enableBio by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AnimatedVaultLogo()
                Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text(stringResource(R.string.master_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordStrengthBar(strength = PasswordTools.strength(password))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it; error = null },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = enableBio, onCheckedChange = { enableBio = it })
                    Text(stringResource(R.string.enable_biometric))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(modifier = Modifier.height(2.dp))
                Button(
                    onClick = {
                        if (password.length < 10) {
                            error = context.getString(R.string.error_weak)
                            return@Button
                        }
                        if (password != confirm) {
                            error = context.getString(R.string.error_passwords_match)
                            return@Button
                        }
                        if (enableBio && activity == null) {
                            error = "Biometrics require activity context"
                            return@Button
                        }
                        busy = true
                        val pwd = password.toCharArray()
                        scope.launch {
                            val result = session.completeSetup(
                                masterPassword = pwd,
                                enableBiometric = enableBio,
                                activity = if (enableBio) activity else null,
                            )
                            pwd.fill('\u0000')
                            busy = false
                            if (result.isFailure) {
                                error = result.exceptionOrNull()?.message ?: "Setup failed"
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text(stringResource(R.string.create_vault))
                }
                Text(
                    stringResource(R.string.banking_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                )
            }
        }
    }
}
