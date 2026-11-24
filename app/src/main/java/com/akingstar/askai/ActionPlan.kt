package com.akingstar.askai

import org.json.JSONArray
import org.json.JSONObject

data class ActionStep(val action: String, val target: String = "", val value: String = "")
data class ActionPlan(val language: String, val steps: List<ActionStep>)

object PlanValidator {
    private val allowed = setOf(
        "open_app", "alarm", "timer", "camera", "selfie", "gallery",
        "torch", "torch_off", "volume_up", "volume_down", "brightness",
        "dnd", "ringer", "rotation", "youtube", "maps", "web", "music",
        "phonepe", "paytm", "gpay", "irctc", "jio", "airtel", "hotstar",
        "wifi_settings", "bluetooth_settings", "mobile_settings",
        "hotspot_settings", "airplane_settings", "location_settings",
        "battery", "time", "date", "storage", "network", "location"
    )

    private fun safeString(value: Any?): String? {
        val text = value as? String ?: return null
        if (text.any { it.isISOControl() }) return null
        return text
    }

    fun parse(json: String): ActionPlan? {
        return try {
            if (json.length > 8192) return null

            val root = JSONObject(json)
            if (root.keys().asSequence().toSet() != setOf("language", "steps")) {
                return null
            }

            val language = safeString(root.opt("language")) ?: return null
            if (language.isBlank() || language.length > 20) return null

            val array = root.opt("steps") as? JSONArray ?: return null
            if (array.length() !in 1..5) return null

            val steps = ArrayList<ActionStep>()

            for (i in 0 until array.length()) {
                val item = array.opt(i) as? JSONObject ?: return null

                if (item.keys().asSequence().toSet() != setOf("action", "target", "value")) {
                    return null
                }

                val action = safeString(item.opt("action")) ?: return null
                val target = safeString(item.opt("target")) ?: return null
                val value = safeString(item.opt("value")) ?: return null

                if (action !in allowed ||
                    target.length > 160 ||
                    value.length > 500 ||
                    (action == "open_app" && target.isBlank() && value.isBlank())
                ) return null

                if (SafetyGate.isNeverAllowedAction(action)) return null

                steps.add(ActionStep(action, target, value))
            }

            ActionPlan(language, steps)
        } catch (_: Exception) {
            null
        }
    }

    fun encode(plan: ActionPlan): String {
        val arr = JSONArray()
        plan.steps.forEach {
            arr.put(
                JSONObject()
                    .put("action", it.action)
                    .put("target", it.target)
                    .put("value", it.value)
            )
        }
        return JSONObject()
            .put("language", plan.language)
            .put("steps", arr)
            .toString()
    }
}
