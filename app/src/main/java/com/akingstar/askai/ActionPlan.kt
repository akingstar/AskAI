package com.akingstar.askai

import org.json.JSONArray
import org.json.JSONObject

data class ActionStep(val action: String, val target: String = "", val value: String = "")
data class ActionPlan(val language: String, val steps: List<ActionStep>)

object PlanValidator {
    private val allowed = setOf(
        "open_app", "call", "sms", "contacts", "alarm", "timer", "camera",
        "selfie", "gallery", "torch", "torch_off", "volume_up", "volume_down", "brightness",
        "dnd", "ringer", "rotation", "youtube", "maps", "web", "music",
        "phonepe", "paytm", "gpay", "irctc", "jio", "airtel", "hotstar",
        "wifi_settings", "bluetooth_settings", "mobile_settings", "hotspot_settings",
        "airplane_settings", "location_settings", "battery", "time", "date",
        "storage", "network", "location"
    )

    fun parse(json: String): ActionPlan? = try {
        val root = JSONObject(json)
        if (root.keys().asSequence().toSet() != setOf("language", "steps")) return null
        val language = root.getString("language")
        val array = root.getJSONArray("steps")
        if (array.length() !in 1..5) return null
        val steps = ArrayList<ActionStep>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            if (item.keys().asSequence().toSet() != setOf("action", "target", "value")) return null
            val action = item.getString("action")
            val target = item.getString("target")
            val value = item.getString("value")
            if (action !in allowed || target.length > 160 || value.length > 500) return null
            steps.add(ActionStep(action, target, value))
        }
        ActionPlan(language, steps)
    } catch (_: Exception) { null }

    fun encode(plan: ActionPlan): String {
        val arr = JSONArray()
        plan.steps.forEach {
            arr.put(JSONObject().put("action", it.action).put("target", it.target).put("value", it.value))
        }
        return JSONObject().put("language", plan.language).put("steps", arr).toString()
    }
}
