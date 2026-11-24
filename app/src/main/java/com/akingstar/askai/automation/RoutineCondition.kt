package com.akingstar.askai.automation

enum class RoutineConditionType {
    BATTERY_PERCENT,
    TIME_OF_DAY,
    PREVIOUS_STEP
}

enum class RoutineConditionOperator {
    LESS_THAN,
    AT_LEAST,
    EQUALS,
    BETWEEN,
    SUCCEEDED,
    FAILED
}

data class RoutineCondition(
    val type: RoutineConditionType,
    val operator: RoutineConditionOperator,
    val value: Int = 0,
    val upperValue: Int = 0
)

data class RoutineConditionSnapshot(
    val batteryPercent: Int? = null,
    val minuteOfDay: Int? = null,
    val previousStepStatus: String? = null
)

object RoutineConditionEvaluator {
    fun evaluate(
        condition: RoutineCondition,
        snapshot: RoutineConditionSnapshot
    ): Boolean? {
        return when (condition.type) {
            RoutineConditionType.BATTERY_PERCENT -> {
                val actual = snapshot.batteryPercent ?: return null
                if (actual !in 0..100 || condition.value !in 0..100) return null
                when (condition.operator) {
                    RoutineConditionOperator.LESS_THAN -> actual < condition.value
                    RoutineConditionOperator.AT_LEAST -> actual >= condition.value
                    RoutineConditionOperator.EQUALS -> actual == condition.value
                    else -> null
                }
            }

            RoutineConditionType.TIME_OF_DAY -> {
                val actual = snapshot.minuteOfDay ?: return null
                if (actual !in 0..1439 ||
                    condition.value !in 0..1439 ||
                    condition.upperValue !in 0..1439 ||
                    condition.operator != RoutineConditionOperator.BETWEEN
                ) return null

                if (condition.value <= condition.upperValue) {
                    actual in condition.value..condition.upperValue
                } else {
                    actual >= condition.value || actual <= condition.upperValue
                }
            }

            RoutineConditionType.PREVIOUS_STEP -> {
                val status = snapshot.previousStepStatus ?: return null
                if (status != "SUCCESS" && status != "FAILED") return null
                when (condition.operator) {
                    RoutineConditionOperator.SUCCEEDED -> status == "SUCCESS"
                    RoutineConditionOperator.FAILED -> status == "FAILED"
                    else -> null
                }
            }
        }
    }
}
