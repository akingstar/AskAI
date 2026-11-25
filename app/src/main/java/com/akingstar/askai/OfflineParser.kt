package com.akingstar.askai

import java.text.Normalizer
import java.util.Locale

object OfflineParser {
    private fun clean(s: String) = Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFKC)
        .replace(Regex("[^\\p{L}\\p{M}\\p{N} +]"), " ")
        .replace(Regex("\\s+"), " ").trim()

    private val commands = listOf(
        listOf("home", "go home", "home screen", "होम जाओ", "होम स्क्रीन", "होम खोलो"),
        listOf("back", "go back", "पीछे जाओ", "वापस जाओ", "बैक करो"),
        listOf("recents", "recent apps", "हाल के ऐप", "हाल के ऐप दिखाओ"),
        listOf("notifications", "show notifications", "नोटिफिकेशन दिखाओ", "सूचनाएं दिखाओ"),
        listOf("quick_settings", "quick settings", "क्विक सेटिंग", "क्विक सेटिंग खोलो"),
        listOf("scroll_up", "scroll up", "ऊपर स्क्रॉल"),
        listOf("scroll_down", "scroll down", "नीचे स्क्रॉल"),
        listOf("read_screen", "read screen", "स्क्रीन पढ़ो", "यहाँ क्या लिखा है"),
        listOf("discover_controls", "discover controls", "show controls", "स्क्रीन के बटन दिखाओ", "कंट्रोल दिखाओ", "यहाँ कौन से बटन हैं"),
        listOf("accessibility_settings", "accessibility settings", "एक्सेसिबिलिटी सेटिंग"),
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
        listOf("music", "music", "गाना चलाओ", "म्यूजिक"),
        listOf("phonepe", "phonepe", "फोन पे"),
        listOf("paytm", "paytm", "पेटीएम"),
        listOf("gpay", "gpay", "google pay", "गूगल पे"),
        listOf("irctc", "irctc", "रेलवे"),
        listOf("hotstar", "jiohotstar", "हॉटस्टार"),
        listOf("jio", "myjio", "माय जिओ"),
        listOf("airtel", "airtel thanks", "एयरटेल")
    )

    private fun matchesCommandAlias(part: String, rawKey: String): Boolean {
        val key = clean(rawKey)
        if (key.isBlank()) return false
        if (part == key) return true
        if (!part.startsWith("$key ")) return false

        val suffix = part.removePrefix("$key ").trim()
        val allowedSuffixes = setOf(
            "खोलो", "खोल दो", "खोलें", "खोलिए",
            "जलाओ", "jalao", "चालू करो", "on करो",
            "बंद", "बंद करो", "बंद कर दो", "band", "band karo",
            "दिखाओ", "बताओ", "चलाओ", "लो", "करो", "कर दो",
            "बढ़ाओ", "बढ़ा दो", "कम करो", "घटाओ",
            "settings kholo", "settings खोलो"
        )
        return suffix in allowedSuffixes
    }

    private fun hasUnsupportedCondition(text: String): Boolean {
        val markers = listOf(
            " agar ", " yadi ", " jab ", " if ", " when ", " unless ",
            " अगर ", " यदि ", " जब ",
            " below ", " above ", " less than ", " greater than ",
            " at least ", " more than ", " ho to ", " se kam ",
            " se jyada ", " se zyada ", " के बीच ", " से कम ",
            " से ज्यादा ", " से ज़्यादा ", " हो तो ",
            " previous step ", " pichla step ", " pichhle step ",
            " पिछला step ", " पिछले step "
        )
        val padded = " $text "
        return markers.any { padded.contains(it) }
    }


    private fun parseWebSearch(part: String): ActionStep? {
        val prefixes = listOf(
            "google par search ",
            "google पर search ",
            "google par khojo ",
            "google पर खोजो ",
            "search ",
            "खोजो ",
            "सर्च करो "
        )

        val prefix = prefixes.firstOrNull { part.startsWith(it) } ?: return null
        val query = part.removePrefix(prefix).trim()

        if (query.isBlank() || query.length > 500) return null
        return ActionStep("web", value = query)
    }

    private fun parseDynamic(part: String): ActionStep? {
        val text = clean(part)

        fun target(raw: String): String? {
            val value = clean(raw)
            if (value.isBlank() || value.length > 160) return null
            if (value in setOf(
                "button", "बटन", "field", "फील्ड",
                "input", "text field", "screen", "स्क्रीन"
            )) return null
            return value
        }

        fun typed(field: String, content: String): ActionStep? {
            val t = target(field) ?: return null
            val v = clean(content)
            if (v.isBlank() || v.length > 500) return null
            if (SafetyGate.isSensitiveInputTarget(t) ||
                SensitiveDataFilter.containsSensitive(v)
            ) return null
            return ActionStep("type_text", t, v)
        }

        Regex(
            "^(.{1,160}?)\\s+(?:में|me)\\s+(.{1,500}?)\\s+" +
                "(?:लिखो|लिखें|टाइप करो|भर दो|likho|type karo)$"
        ).matchEntire(text)?.let {
            return typed(it.groupValues[1], it.groupValues[2])
        }

        Regex("^type\\s+(.{1,500}?)\\s+in\\s+(.{1,160})$")
            .matchEntire(text)?.let {
                return typed(it.groupValues[2], it.groupValues[1])
            }

        Regex(
            "^(.{1,160}?)\\s+(?:(?:par|पर)\\s+)?" +
                "(?:long press करो|long press karo|long press|लंबे समय तक दबाओ)$"
        ).matchEntire(text)?.let {
            val t = target(it.groupValues[1]) ?: return null
            if (SafetyGate.isRiskyTapTarget(t)) return null
            return ActionStep("long_press_text", t)
        }

        Regex(
            "^(.{1,160}?)\\s+(?:(?:button|बटन)\\s+)?" +
                "(?:(?:par|पर)\\s+)?" +
                "(?:दबाओ|दबाएं|क्लिक करो|click karo|click|tap karo|tap)$"
        ).matchEntire(text)?.let {
            val t = target(it.groupValues[1]) ?: return null
            if (SafetyGate.isRiskyTapTarget(t)) return null
            return ActionStep("tap_text", t)
        }

        Regex("^(?:tap|click)\\s+(.{1,160})$")
            .matchEntire(text)?.let {
                val t = target(it.groupValues[1]) ?: return null
                if (SafetyGate.isRiskyTapTarget(t)) return null
                return ActionStep("tap_text", t)
            }

        return null
    }

    fun parse(input: String): ActionPlan? {
        val text = clean(input)
        if (text.isBlank() || hasUnsupportedCondition(text)) return null
        val parts = text.split(Regex("\\s+(?:aur|and|फिर|और|then)\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty() || parts.size > 5) return null

        val steps = parts.map { part ->
            parseDynamic(part)?.let { return@map it }
            parseWebSearch(part)?.let { return@map it }

            val found = commands.mapNotNull { group ->
                val score = group.drop(1).maxOfOrNull { key ->
                    when {
                        matchesCommandAlias(part, key) -> 100
                        else -> 0
                    }
                } ?: 0
                if (score > 0) group.first() to score else null
            }.maxByOrNull { it.second }?.first
                ?: return null

            when (found) {
                "youtube" -> ActionStep(found, value = "youtube")
                "maps" -> ActionStep(found, value = "maps")
                else -> ActionStep(found)
            }
        }
        if (steps.isEmpty()) return null
        return ActionPlan(if (input.any { it.code in 0x0900..0x097F }) "hi-IN" else "en-IN", steps)
    }
}
