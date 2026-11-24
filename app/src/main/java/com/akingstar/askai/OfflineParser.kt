package com.akingstar.askai

import java.text.Normalizer
import java.util.Locale

object OfflineParser {
    private fun clean(s: String) = Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").replace(Regex("[^\\p{L}\\p{N} +]"), " ")
        .replace(Regex("\\s+"), " ").trim()

    private val commands = listOf(
        listOf("torch", "flashlight", "torch on", "flashlight on", "टॉर्च", "टॉर्च जलाओ", "torch jalao"),
        listOf("torch_off", "flashlight off", "टॉर्च बंद", "torch band"),
        listOf("volume_up", "volume up", "आवाज बढ़ाओ", "volume badhao", "आवाज़ तेज"),
        listOf("volume_down", "volume down", "आवाज कम", "volume kam", "आवाज़ घटाओ"),
        listOf("camera", "open camera", "camera open", "camera kholo", "कैमरा खोलो", "फोटो खींचो", "photo lo"),
        listOf("selfie", "selfie", "सेल्फी", "सेल्फी लो"),
        listOf("gallery", "gallery kholo", "गैलरी खोलो", "photos kholo"),
        listOf("wifi_settings", "wifi", "वाईफाई", "वाई फाई सेटिंग"),
        listOf("bluetooth_settings", "bluetooth", "ब्लूटूथ"),
        listOf("airplane_settings", "airplane mode", "flight mode", "हवाई जहाज मोड"),
        listOf("location_settings", "location", "gps", "लोकेशन"),
        listOf("battery", "battery", "बैटरी"),
        listOf("time", "time", "समय बताओ", "kitna time"),
        listOf("date", "date", "आज तारीख", "तारीख बताओ"),
        listOf("storage", "storage", "स्टोरेज", "जगह कितनी"),
        listOf("network", "network", "नेटवर्क"),
        listOf("youtube", "youtube kholo", "यूट्यूब खोलो"),
        listOf("maps", "maps kholo", "गूगल मैप खोलो", "रास्ता बताओ"),
        listOf("web", "search", "google par search", "खोजो", "सर्च करो"),
        listOf("music", "music", "गाना चलाओ", "म्यूजिक"),
        listOf("phonepe", "phonepe", "फोन पे"),
        listOf("paytm", "paytm", "पेटीएम"),
        listOf("gpay", "gpay", "google pay", "गूगल पे"),
        listOf("irctc", "irctc", "रेलवे"),
        listOf("hotstar", "jiohotstar", "हॉटस्टार"),
        listOf("jio", "myjio", "माय जिओ"),
        listOf("airtel", "airtel thanks", "एयरटेल")
    )

    fun parse(input: String): ActionPlan? {
        val text = clean(input)
        if (text.isBlank()) return null
        val parts = text.split(Regex("\\s+(?:aur|and|फिर|और|then)\\s+")).filter { it.isNotBlank() }
        val steps = parts.mapNotNull { part ->
            val found = commands.mapNotNull { group ->
                val score = group.drop(1).maxOfOrNull { key ->
                    when {
                        part == clean(key) -> 100
                        part.contains(clean(key)) -> 85
                        clean(key).contains(part) && part.length >= 4 -> 65
                        else -> 0
                    }
                } ?: 0
                if (score > 0) group.first() to score else null
            }.maxByOrNull { it.second }?.first
            if (found == null) null else ActionStep(found, "", part)
        }
        if (steps.isEmpty() || steps.size > 5) return null
        return ActionPlan(if (input.any { it.code in 0x0900..0x097F }) "hi-IN" else "en-IN", steps)
    }
}
