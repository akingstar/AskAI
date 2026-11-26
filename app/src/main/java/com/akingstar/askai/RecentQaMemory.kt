package com.akingstar.askai

internal data class RecentQaTurn(
    val question: String,
    val answer: String
)

/** Session-only memory for safe, completed general-information answers. */
internal class RecentQaMemory(private val maxTurns: Int = 3) {
    private val turns = mutableListOf<RecentQaTurn>()

    @Synchronized
    fun snapshot(): List<RecentQaTurn> = turns.toList()

    @Synchronized
    fun clear() {
        turns.clear()
    }

    @Synchronized
    fun record(question: String, answer: String) {
        if (maxTurns <= 0) return
        val cleanQuestion = question.trim()
        val cleanAnswer = answer.trim()

        if (!isSafeText(cleanQuestion, 240) ||
            !isSafeText(cleanAnswer, AnswerPolicy.MAX_ANSWER_CHARS)
        ) return

        turns.add(RecentQaTurn(cleanQuestion, cleanAnswer))
        while (turns.size > maxTurns) turns.removeAt(0)
    }

    private fun isSafeText(value: String, maxLength: Int): Boolean {
        if (value.isBlank() || value.length > maxLength) return false
        if (value.any { it.code < 32 || it.code in 127..159 }) return false
        if (SensitiveDataFilter.containsSensitive(value)) return false
        if (sensitiveWords.containsMatchIn(value)) return false
        if (emailPattern.containsMatchIn(value) || phonePattern.containsMatchIn(value)) return false
        return true
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

/** Only attach Q&A history when the current request clearly refers to it. */
internal object QaContextPolicy {
    private val referencePattern = Regex(
        """(?i)\b(?:there|that|those|it|its|they|them|same|above|previous|earlier|former|latter|wahan|wahaan|waha|udhar|uska|uski|uske|usse|unka|unki|unke|wahi)\b|वहाँ|वहां|वहा|उधर|उसका|उसकी|उसके|उसे|उनका|उनकी|उनके|वही|इसका|इसकी|इसके"""
    )

    private val actionPattern = Regex(
        """(?i)\b(?:open|launch|close|turn on|turn off|enable|disable|tap|click|press|type|write|send|call|dial|delete|remove|uninstall|install|set|change|increase|decrease|play|pause|stop|scroll|torch|flashlight|brightness|volume|wifi|bluetooth|camera|alarm|timer|settings|home|back|navigate|navigation|map|maps|go|move|travel|route|directions|jao|jaana|jana|jaana|chalo|chaliye|khol|kholo|kholna)\b|खोलो|खोलना|बंद करो|चालू करो|दबाओ|क्लिक|लिखो|भेजो|कॉल|फोन करो|हटाओ|मिटाओ|चलाओ|रोक दो|सेट करो|बढ़ाओ|घटाओ|टॉर्च|कैमरा|सेटिंग|होम जाओ|पीछे जाओ|जाओ|जाना|चलो|चलिए|नेविगेट"""
    )

    fun shouldInclude(question: String): Boolean {
        if (question.isBlank() || question.length > 4000) return false
        if (SensitiveDataFilter.containsSensitive(question)) return false
        if (OfflineParser.parse(question) != null) return false
        if (actionPattern.containsMatchIn(question)) return false
        return referencePattern.containsMatchIn(question)
    }
}
