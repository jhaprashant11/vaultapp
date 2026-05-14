package com.localvault.app

import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.localvault.app.ui.LocalVaultRoot
import com.localvault.app.ui.theme.LocalVaultTheme

class MainActivity : AppCompatActivity() {

    private val session by lazy { (application as LocalVaultApplication).vaultSession }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val themeMode by session.lockPreferences.themeMode.collectAsState(initial = 0)
            LocalVaultTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocalVaultRoot(
                        session = session,
                        onLock = { session.lock() },
                    )
                }
            }
        }
    }
}
