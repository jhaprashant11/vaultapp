package com.localvault.app.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class AesGcmTest {

    @Test
    fun roundTrip() {
        val key = ByteArray(32) { it.toByte() }
        val plain = "secret payload".toByteArray()
        val (iv, ct) = AesGcm.encrypt(key, plain)
        val out = AesGcm.decrypt(key, iv, ct)
        assertArrayEquals(plain, out)
    }
}
