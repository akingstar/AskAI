package com.akingstar.askai

import org.json.JSONObject

/**
 * Extract provider response text only.
 * Extracted text must pass PlanValidator before use.
 */
internal object ProviderResponseParser {

    fun geminiText(raw: String): String? {
        return runCatching {
            val root = JSONObject(raw)
            val candidates = root.optJSONArray("candidates")
                ?: return@runCatching null
            if (candidates.length() == 0) return@runCatching null

            val content = candidates.getJSONObject(0).optJSONObject("content")
                ?: return@runCatching null
            val parts = content.optJSONArray("parts")
                ?: return@runCatching null

            buildString {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    val text = part.optString("text")
                    if (text.isNotBlank()) append(text)
                }
            }.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun groqText(raw: String): String? {
        return runCatching {
            val choices = JSONObject(raw).optJSONArray("choices")
                ?: return@runCatching null
            if (choices.length() == 0) return@runCatching null

            choices.getJSONObject(0)
                .optJSONObject("message")
                ?.optString("content")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
