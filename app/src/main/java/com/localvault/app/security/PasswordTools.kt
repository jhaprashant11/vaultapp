package com.localvault.app.security

import java.security.SecureRandom
import java.util.Collections

object PasswordTools {
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?/|"
    private val random = SecureRandom()

    fun generate(
        length: Int,
        useSymbols: Boolean,
        useNumbers: Boolean,
    ): String {
        val pools = buildList {
            add(LOWER)
            add(UPPER)
            if (useNumbers) add(DIGITS)
            if (useSymbols) add(SYMBOLS)
        }
        val all = pools.joinToString("")
        val chars = MutableList(length.coerceIn(8, 64)) {
            all[random.nextInt(all.length)]
        }
        pools.forEachIndexed { index, pool ->
            if (index < chars.size) {
                chars[index] = pool[random.nextInt(pool.length)]
            }
        }
        Collections.shuffle(chars, random)
        return chars.joinToString("")
    }

    fun strength(password: String): PasswordStrength {
        if (password.isBlank()) return PasswordStrength.Empty
        var score = 0
        if (password.length >= 10) score++
        if (password.length >= 14) score++
        if (password.any(Char::isLowerCase) && password.any(Char::isUpperCase)) score++
        if (password.any(Char::isDigit)) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score <= 2 -> PasswordStrength.Weak
            score <= 4 -> PasswordStrength.Medium
            else -> PasswordStrength.Strong
        }
    }
}

enum class PasswordStrength(
    val label: String,
    val progress: Float,
) {
    Empty("Strength", 0f),
    Weak("Weak", 0.3f),
    Medium("Medium", 0.65f),
    Strong("Strong", 1f),
}
