package com.akingstar.askai

import org.json.JSONArray

/**
 * Converts the accessibility control inventory into concise Hindi speech.
 * Never speaks password-field labels or editable field contents.
 */
object ScreenNarrator {
    fun describe(json: String): String {
        val items = runCatching { JSONArray(json) }.getOrNull()
            ?: return "स्क्रीन के कंट्रोल पढ़े नहीं जा सके।"

        if (items.length() == 0) {
            return "इस स्क्रीन पर पढ़ने योग्य कंट्रोल नहीं मिले।"
        }

        val descriptions = linkedSetOf<String>()
        var total = 0

        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val role = item.optString("role", "text")
            val enabled = item.optBoolean("enabled", true)
            val rawLabel = item.optString("label", "").trim()

            val description = when (role) {
                "password_field" -> "सुरक्षित पासवर्ड फ़ील्ड"
                "text_field" -> {
                    val label = SensitiveDataFilter.mask(rawLabel).trim()
                    if (label.isBlank()) "लिखने की जगह" else "$label लिखने की जगह"
                }
                "scroll_area" -> "स्क्रॉल करने वाला हिस्सा"
                "checkable" -> {
                    val label = SensitiveDataFilter.mask(rawLabel).trim()
                    if (label.isBlank()) "चुनने वाला विकल्प" else "$label विकल्प"
                }
                "clickable" -> {
                    val label = SensitiveDataFilter.mask(rawLabel).trim()
                    if (label.isBlank()) "बटन" else "$label बटन"
                }
                else -> {
                    val label = SensitiveDataFilter.mask(rawLabel).trim()
                    if (label.isBlank()) continue
                    label
                }
            }

            val finalDescription = if (enabled) description
                else "$description, अभी उपलब्ध नहीं"

            total++
            descriptions.add(
                finalDescription.lowercase()
                    .replace(Regex("\\s+"), " ")
            )
        }

        if (descriptions.isEmpty()) {
            return "इस स्क्रीन पर पढ़ने योग्य नाम वाले कंट्रोल नहीं मिले।"
        }

        val shown = descriptions.take(8)
        val remaining = (descriptions.size - shown.size).coerceAtLeast(0)
        val details = shown.joinToString("، ")
        val suffix = if (remaining > 0) " और $remaining अन्य कंट्रोल" else ""

        return "इस स्क्रीन पर $details$suffix हैं। कुल ${descriptions.size} अलग कंट्रोल मिले।"
    }
}
