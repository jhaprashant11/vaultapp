package com.localvault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.localvault.app.VaultLockState
import com.localvault.app.VaultSession
import com.localvault.app.ui.navigation.VaultNavHost
import com.localvault.app.ui.screens.SetupScreen
import com.localvault.app.ui.screens.UnlockScreen

@Composable
fun LocalVaultRoot(
    session: VaultSession,
    onLock: () -> Unit,
) {
    val lockState by session.lockState.collectAsState()
    when (lockState) {
        VaultLockState.SetupRequired -> SetupScreen(session = session)
        VaultLockState.Locked -> UnlockScreen(session = session)
        VaultLockState.Unlocked -> VaultNavHost(session = session, onLock = onLock)
        is VaultLockState.Error -> UnlockScreen(session = session)
    }
}
