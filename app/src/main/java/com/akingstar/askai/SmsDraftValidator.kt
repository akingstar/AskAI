package com.akingstar.askai

/**
 * Validates an SMS draft before handing it to the user's messaging app.
 * This class never sends messages.
 */
object SmsDraftValidator {
    fun normalize(raw: String): String? {
        val message = raw.trim()
        if (message.isEmpty() || message.length > 1000) return null

        if (message.any {
            Character.isISOControl(it) && it != '\n' && it != '\t'
        }) return null

        return message
    }
}
