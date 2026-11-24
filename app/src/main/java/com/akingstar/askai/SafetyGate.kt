package com.akingstar.askai

import java.text.Normalizer
import java.util.Locale

object SafetyGate {
    private val blockedPhrases = listOf(
        "send money", "transfer money", "make payment",
        "pay now", "payment confirmation", "upi pin",
        "share pin", "share password", "password share",
        "otp share", "one time password", "factory reset",
        "wipe data", "format phone", "delete all",
        "पैसे भेजो", "पैसा भेजो", "रुपये भेजो",
        "भुगतान करो", "भुगतान की पुष्टि",
        "यूपीआई पिन", "पिन बताओ", "पिन साझा",
        "पासवर्ड बताओ", "पासवर्ड साझा",
        "ओटीपी बताओ", "ओटीपी साझा",
        "फैक्टरी रीसेट", "सारा डेटा मिटा",
        "सब कुछ डिलीट"
    )

    private val blockedTokens = setOf(
        "otp", "password", "passcode", "upi",
        "pin", "पासवर्ड", "ओटीपी"
    )

    private val neverAllowedActions = setOf(
        "call", "sms", "contacts", "delete",
        "uninstall", "factory_reset", "payment",
        "transfer", "send_money"
    )

    private val riskyApps = listOf(
        "anydesk", "teamviewer", "quicksupport",
        "quick support", "screen sharing"
    )

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}+]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    fun blocked(text: String): Boolean {
        val normalized = normalize(text)
        if (blockedPhrases.any { normalized.contains(normalize(it)) }) return true

        val tokens = normalized.split(" ").toSet()
        return tokens.any { it in blockedTokens }
    }

    fun isNeverAllowedAction(action: String): Boolean =
        action.lowercase(Locale.ROOT) in neverAllowedActions

    fun warningForApp(name: String): String? =
        if (riskyApps.any { name.contains(it, true) }) {
            "सावधान: यह ऐप आपकी स्क्रीन किसी दूसरे व्यक्ति को दिखा सकता है। OTP, PIN या पासवर्ड साझा न करें।"
        } else null

    fun needsConfirmation(action: String, target: String): Boolean =
        action.lowercase(Locale.ROOT) in setOf(
            "call", "sms", "delete", "uninstall",
            "security_settings"
        ) || target.matches(Regex("\\+?[0-9]{7,15}"))
}
