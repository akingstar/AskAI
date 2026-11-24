package com.akingstar.askai

internal data class ConversationTurn(
    val command: String,
    val outcome: String
)

internal class ConversationMemory(private val maxTurns: Int = 3) {
    private val turns = mutableListOf<ConversationTurn>()

    @Synchronized
    fun snapshot(): List<ConversationTurn> = turns.toList()

    @Synchronized
    fun clear() {
        turns.clear()
    }

    @Synchronized
    fun record(command: String, summary: ExecutionSummary) {
        if (maxTurns <= 0 || summary.totalSteps <= 0 || summary.stepResults.isEmpty()) return

        val clean = command.trim()
        if (clean.isEmpty() || clean.length > 240) return
        if (clean.any { it.code < 32 || it.code in 127..159 }) return
        if (SensitiveDataFilter.containsSensitive(clean)) return
        if (sensitiveWords.containsMatchIn(clean)) return
        if (emailPattern.containsMatchIn(clean) || phonePattern.containsMatchIn(clean)) return

        // Store action names and statuses only; never store step targets or reasons.
        val actions = summary.stepResults.take(5).joinToString(",") {
            "${it.action}:${it.status}"
        }
        val outcome = "success=${summary.succeededSteps}; failed=${summary.failedSteps}; " +
            "cancelled=${summary.cancelledSteps}; skipped=${summary.skippedSteps}; actions=$actions"

        turns.add(ConversationTurn(clean, outcome.take(180)))
        while (turns.size > maxTurns) turns.removeAt(0)
    }

    private companion object {
        val sensitiveWords = Regex(
            """(?i)\b(?:otp|pin|password|passcode|payment|pay|transfer|bank|account|call|sms|message|delete|uninstall|factory reset)\b|ओटीपी|पासवर्ड|पिन|भुगतान|पैसे|कॉल|मैसेज|संदेश|डिलीट|हटाओ|मिटाओ"""
        )
        val emailPattern = Regex(
            """[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}""",
            RegexOption.IGNORE_CASE
        )
        val phonePattern = Regex("""(?:\+?\d[\d\s().-]{7,}\d)""")
    }
}
