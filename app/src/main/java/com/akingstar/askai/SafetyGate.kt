package com.akingstar.askai

object SafetyGate {
    private val blocked = listOf(
        "factory reset", "फैक्टरी रीसेट", "payment confirmation",
        "भुगतान की पुष्टि", "send money", "पैसे भेजो", "upi pin",
        "ओटीपी बताओ", "otp share", "password share"
    )
    private val riskyApps = listOf("anydesk", "teamviewer", "quicksupport", "quick support", "screen sharing")

    fun blocked(text: String): Boolean = blocked.any { text.contains(it, true) }

    fun warningForApp(name: String): String? =
        if (riskyApps.any { name.contains(it, true) }) "सावधान: यह ऐप आपकी स्क्रीन किसी दूसरे व्यक्ति को दिखा सकता है। OTP, PIN या पासवर्ड साझा न करें।" else null

    fun needsConfirmation(action: String, target: String): Boolean =
        action in setOf("call", "sms", "delete", "uninstall", "security_settings") ||
            target.matches(Regex("\\+?[0-9]{7,15}"))
}
