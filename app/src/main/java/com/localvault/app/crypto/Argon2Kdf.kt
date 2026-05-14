package com.localvault.app.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Arrays

object Argon2Kdf {
    const val KEY_LENGTH_BYTES = 32
    private const val DEFAULT_SALT_LENGTH = 16

    fun randomSalt(length: Int = DEFAULT_SALT_LENGTH): ByteArray {
        val salt = ByteArray(length)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Derives a 256-bit key using Argon2id.
     * [memoryKiB] and [iterations] can be lowered for unit tests only.
     */
    fun derive(
        password: CharArray,
        salt: ByteArray,
        memoryKiB: Int = 65536,
        iterations: Int = 3,
        parallelism: Int = 2,
    ): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withParallelism(parallelism)
            .withMemoryAsKB(memoryKiB)
            .withIterations(iterations)
            .build()
        val gen = Argon2BytesGenerator()
        gen.init(params)
        val passwordBytes = encodePasswordUtf8(password)
        return try {
            val out = ByteArray(KEY_LENGTH_BYTES)
            gen.generateBytes(passwordBytes, out, 0, out.size)
            out
        } finally {
            Arrays.fill(passwordBytes, 0)
        }
    }

    private fun encodePasswordUtf8(password: CharArray): ByteArray {
        val encoder = StandardCharsets.UTF_8.newEncoder()
        val buffer = encoder.encode(CharBuffer.wrap(password))
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
    }
}
