package com.localvault.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = VaultGreen,
    secondary = VaultAccent,
    tertiary = VaultGold,
    background = VaultMist,
    surface = Color.White,
    surfaceVariant = Color(0xFFE7EFEC),
    onPrimary = Color.White,
    onBackground = VaultInk,
    onSurface = VaultInk,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CE0D0),
    secondary = Color(0xFFFFD27A),
    tertiary = Color(0xFFA8B9FF),
    background = Color(0xFF0D1514),
    surface = Color(0xFF14211F),
    surfaceVariant = Color(0xFF20312E),
    onPrimary = Color(0xFF06201D),
    onBackground = Color(0xFFEAF3F0),
    onSurface = Color(0xFFEAF3F0),
)

@Composable
fun LocalVaultTheme(
    themeMode: Int = 0,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content,
    )
}
