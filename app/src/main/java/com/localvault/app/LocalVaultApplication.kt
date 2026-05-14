package com.localvault.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import net.sqlcipher.database.SQLiteDatabase

class LocalVaultApplication : Application() {

    lateinit var vaultSession: VaultSession
        private set

    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            vaultSession.markBackgrounded()
        }

        override fun onStart(owner: LifecycleOwner) {
            vaultSession.maybeAutoLockOnForeground()
        }
    }

    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
        vaultSession = VaultSession(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
    }
}
