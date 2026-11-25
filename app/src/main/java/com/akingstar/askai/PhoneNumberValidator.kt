package com.akingstar.askai

/**
 * Validates a user-selected phone number before opening the system dialer.
 * This class does not initiate calls or access contacts.
 */
object PhoneNumberValidator {
    fun normalize(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        if (!value.matches(Regex("[+0-9() .-]+"))) return null
        if (value.count { it == '+' } > 1) return null
        if ('+' in value && !value.startsWith("+")) return null

        val digits = value.filter { it.isDigit() }
        if (digits.length !in 7..15) return null

        return (if (value.startsWith("+")) "+" else "") + digits
    }
}
