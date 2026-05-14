package com.localvault.app.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Arrays

class Argon2KdfTest {

    @Test
    fun derive_isDeterministic_forSameInputs() {
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val password = "test-password-argon".toCharArray()
        val a = Argon2Kdf.derive(password, salt, memoryKiB = 64, iterations = 1, parallelism = 1)
        val b = Argon2Kdf.derive(password, salt, memoryKiB = 64, iterations = 1, parallelism = 1)
        assertArrayEquals(a, b)
        assertTrue(a.isNotEmpty())
        Arrays.fill(password, '\u0000')
    }

    @Test
    fun wrongPassword_producesDifferentKey() {
        val salt = Argon2Kdf.randomSalt()
        val a = Argon2Kdf.derive("one".toCharArray(), salt, memoryKiB = 64, iterations = 1, parallelism = 1)
        val b = Argon2Kdf.derive("two".toCharArray(), salt, memoryKiB = 64, iterations = 1, parallelism = 1)
        assertTrue(!a.contentEquals(b))
    }
}
