package com.akingstar.askai

/**
 * The single Android-side policy boundary for data sent to AI providers or the Worker.
 * User command text is never silently modified: if it contains a detected secret or
 * personal identifier, online transmission is denied. Safe local parsing remains available.
 */
internal object OutboundPrivacyGuard {
    data class SafePayload(
        val text: String,
        val conversation: List<ConversationTurn>,
        val qaContext: List<RecentQaTurn>
    )

    /**
     * Returns null if the current request contains data that should not leave the device.
     * Previously collected context is independently screened and unsafe turns are dropped.
     */
    fun prepare(
        text: String,
        conversation: List<ConversationTurn>,
        qaContext: List<RecentQaTurn>
    ): SafePayload? {
        if (text.isBlank() || text.length > 4000 || containsPrivateData(text)) return null

        val safeConversation = conversation.takeLast(3).filter { turn ->
            turn.command.length <= 240 && turn.outcome.length <= 180 &&
                !containsPrivateData(turn.command) && !containsPrivateData(turn.outcome)
        }
        val safeQa = qaContext.takeLast(3).filter { turn ->
            turn.question.length <= 240 && turn.answer.length <= 400 &&
                !containsPrivateData(turn.question) && !containsPrivateData(turn.answer)
        }
        return SafePayload(text, safeConversation, safeQa)
    }

    /** Also used at the final HTTP serialization boundary as a defense-in-depth check. */
    fun canTransmit(value: String): Boolean =
        value.length <= 16_000 && !containsPrivateData(value)

    private fun containsPrivateData(value: String): Boolean {
        if (SensitiveDataFilter.containsSensitive(value)) return true
        return credentialAssignment.containsMatchIn(value) ||
            labeledCode.containsMatchIn(value) ||
            email.containsMatchIn(value) ||
            upiIdentifier.containsMatchIn(value) ||
            indianPhone.containsMatchIn(value) ||
            internationalPhone.containsMatchIn(value) ||
            longNumericIdentifier.containsMatchIn(value)
    }

    private val credentialAssignment = Regex(
        """(?i)(?:password|passcode|secret|credential|पासवर्ड|पासकोड)\s*(?:है\s*)?[:=]?\s*\S+"""
    )
    private val labeledCode = Regex(
        """(?i)(?:otp|one[ -]?time password|verification code|security code|pin|cvv|cvc|ओटीपी|सत्यापन कोड|सुरक्षा कोड|पिन|सीवीवी)\s*(?:[:=]|है)?\s*[A-Za-z0-9]{3,10}"""
    )
    private val email = Regex("""(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b""")
    private val upiIdentifier = Regex("""(?i)(?<![A-Z0-9._-])[A-Z0-9._-]{2,}@[A-Z][A-Z0-9.-]{1,}(?![A-Z0-9.-])""")
    private val indianPhone = Regex("""(?<!\d)(?:\+?91[ .()-]?)?[6-9]\d{4}[ .()-]?\d{5}(?!\d)""")
    private val internationalPhone = Regex("""(?<!\d)\+\d{1,3}[ .()-]\d{2,4}[ .()-]\d{3,4}[ .()-]\d{3,4}(?!\d)""")
    private val longNumericIdentifier = Regex("""(?<!\d)(?:\d[ -]?){12,19}(?!\d)""")
}
