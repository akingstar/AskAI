package com.akingstar.askai

/** Hindi status text and spoken text for HTTP failures from online AI providers. */
internal object AIErrorMessages {
    fun forServerError(code: Int, provider: String = ""): Pair<String, String> {
        val base = baseMessages(code)
        return if (provider.isBlank()) base else Pair("$provider: ${base.first}", base.second)
    }

    private fun baseMessages(code: Int): Pair<String, String> = when {
        code == 401 || code == 403 -> Pair(
            "AI key गलत या बंद है। Settings में key जाँचें।",
            "AI की key सही नहीं है। कृपया सेटिंग्स में जाँचें।"
        )
        code == 404 -> Pair(
            "AI मॉडल उपलब्ध नहीं है। ऐप अपडेट की ज़रूरत हो सकती है।",
            "AI मॉडल अभी उपलब्ध नहीं है।"
        )
        code == 429 -> Pair(
            "AI की लिमिट पूरी हो गई है। थोड़ी देर बाद कोशिश करें।",
            "AI की लिमिट पूरी हो गई है। थोड़ी देर बाद कोशिश करें।"
        )
        code >= 500 -> Pair(
            "AI सेवा अभी busy है। थोड़ी देर बाद कोशिश करें।",
            "AI सेवा अभी व्यस्त है। थोड़ी देर बाद कोशिश करें।"
        )
        else -> Pair(
            "AI सेवा में समस्या है। HTTP $code",
            "AI सेवा अभी उपलब्ध नहीं है।"
        )
    }
}

/** Tags HTTP failures with the provider that produced them; other results are unchanged. */
internal fun AIRequestResult.withProvider(name: String): AIRequestResult =
    if (this is AIRequestResult.ServerError) this.copy(provider = name) else this
