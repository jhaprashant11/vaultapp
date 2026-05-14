package com.localvault.app.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lockDataStore: DataStore<Preferences> by preferencesDataStore(name = "lock_settings")

class LockPreferences(private val context: Context) {

    private val keyAutoLockMinutes = intPreferencesKey("auto_lock_minutes")
    private val keyThemeMode = intPreferencesKey("theme_mode")

    val autoLockMinutes: Flow<Int> = context.lockDataStore.data.map { prefs ->
        prefs[keyAutoLockMinutes] ?: DEFAULT_AUTO_LOCK_MINUTES
    }

    val themeMode: Flow<Int> = context.lockDataStore.data.map { prefs ->
        prefs[keyThemeMode] ?: THEME_SYSTEM
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.lockDataStore.edit { it[keyAutoLockMinutes] = minutes.coerceIn(0, 120) }
    }

    suspend fun setThemeMode(mode: Int) {
        context.lockDataStore.edit { it[keyThemeMode] = mode.coerceIn(THEME_SYSTEM, THEME_DARK) }
    }

    companion object {
        const val DEFAULT_AUTO_LOCK_MINUTES = 5
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }
}
