package com.localvault.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SqlCipherLoadInstrumentedTest {

    @Test
    fun sqlcipher_loads() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
        assertTrue(true)
    }
}
