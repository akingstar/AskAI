package com.akingstar.askai

object SensitiveDataFilter {
    private val patterns = listOf(
        Regex("(?i)\\b(?:otp|one time password)\\s*[:=-]?\\s*\\d{4,8}\\b"),
        Regex("(?i)\\b(?:pin|password|passcode)\\s*[:=-]?\\s*[A-Za-z0-9@#$!]{4,}\\b"),
        Regex("\\b\\d{12}\\b"),
        Regex("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b"),
        Regex("\\b(?:\\d[ -]?){13,19}\\b"),
        Regex("\\b\\d{3,6}\\b")
    )

    fun mask(input: String): String {
        var output = input
        patterns.forEach { output = it.replace(output, "[छिपाई गई निजी जानकारी]") }
        return output
    }

    fun containsSensitive(input: String): Boolean = mask(input) != input
}
