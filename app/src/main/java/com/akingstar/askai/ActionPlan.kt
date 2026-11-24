package com.akingstar.askai

import org.json.JSONArray
import org.json.JSONObject

import com.akingstar.askai.automation.RoutineCondition
import com.akingstar.askai.automation.RoutineConditionType
import com.akingstar.askai.automation.RoutineConditionOperator

data class ActionStep(
    val action: String,
    val target: String = "",
    val value: String = "",
    val condition: RoutineCondition? = null
)
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

                val keys = item.keys().asSequence().toSet()
                if (keys != setOf("action", "target", "value") &&
                    keys != setOf("action", "target", "value", "condition")
                ) return null

                val action = safeString(item.opt("action")) ?: return null
                val target = safeString(item.opt("target")) ?: return null
                val value = safeString(item.opt("value")) ?: return null

                if (action !in allowed ||
                    target.length > 160 ||
                    value.length > 500 ||
                    (action == "open_app" && target.isBlank() && value.isBlank())
                ) return null

                if (SafetyGate.isNeverAllowedAction(action)) return null

                val condition = if (keys.contains("condition")) {
                    parseCondition(item.opt("condition")) ?: return null
                } else null

                if (i == 0 &&
                    condition?.type == RoutineConditionType.PREVIOUS_STEP
                ) return null

                steps.add(ActionStep(action, target, value, condition))
            }

            ActionPlan(language, steps)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseCondition(raw: Any?): RoutineCondition? {
        val obj = raw as? JSONObject ?: return null
        if (obj.keys().asSequence().toSet() !=
            setOf("type", "operator", "value", "upperValue")
        ) return null

        val typeName = safeString(obj.opt("type")) ?: return null
        val operatorName = safeString(obj.opt("operator")) ?: return null
        val type = runCatching { RoutineConditionType.valueOf(typeName) }.getOrNull()
            ?: return null
        val operator = runCatching {
            RoutineConditionOperator.valueOf(operatorName)
        }.getOrNull() ?: return null

        fun integer(key: String): Int? {
            val number = obj.opt(key) as? Number ?: return null
            val value = number.toDouble()
            if (!value.isFinite() || value % 1.0 != 0.0 ||
                value < Int.MIN_VALUE || value > Int.MAX_VALUE
            ) return null
            return value.toInt()
        }

        val value = integer("value") ?: return null
        val upperValue = integer("upperValue") ?: return null

        val valid = when (type) {
            RoutineConditionType.BATTERY_PERCENT ->
                value in 0..100 && upperValue == 0 &&
                    operator in setOf(
                        RoutineConditionOperator.LESS_THAN,
                        RoutineConditionOperator.AT_LEAST,
                        RoutineConditionOperator.EQUALS
                    )

            RoutineConditionType.TIME_OF_DAY ->
                value in 0..1439 && upperValue in 0..1439 &&
                    operator == RoutineConditionOperator.BETWEEN

            RoutineConditionType.PREVIOUS_STEP ->
                value == 0 && upperValue == 0 &&
                    operator in setOf(
                        RoutineConditionOperator.SUCCEEDED,
                        RoutineConditionOperator.FAILED
                    )
        }

        if (!valid) return null
        return RoutineCondition(type, operator, value, upperValue)
    }

    fun encode(plan: ActionPlan): String {
        val arr = JSONArray()
        plan.steps.forEach {
            arr.put(
                JSONObject()
                    .put("action", it.action)
                    .put("target", it.target)
                    .put("value", it.value)
                    .apply {
                        it.condition?.let { condition ->
                            put(
                                "condition",
                                JSONObject()
                                    .put("type", condition.type.name)
                                    .put("operator", condition.operator.name)
                                    .put("value", condition.value)
                                    .put("upperValue", condition.upperValue)
                            )
                        }
                    }
            )
        }
        return JSONObject()
            .put("language", plan.language)
            .put("steps", arr)
            .toString()
    }
}
