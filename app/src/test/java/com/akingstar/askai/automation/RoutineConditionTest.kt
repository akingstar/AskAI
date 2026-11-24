package com.akingstar.askai.automation

import org.junit.Assert.*
import org.junit.Test

class RoutineConditionTest {
    @Test fun batteryLessThan() {
        assertEquals(true, RoutineConditionEvaluator.evaluate(
            RoutineCondition(RoutineConditionType.BATTERY_PERCENT,
                RoutineConditionOperator.LESS_THAN, 20),
            RoutineConditionSnapshot(batteryPercent = 15)
        ))
    }

    @Test fun batteryAtLeast() {
        assertEquals(true, RoutineConditionEvaluator.evaluate(
            RoutineCondition(RoutineConditionType.BATTERY_PERCENT,
                RoutineConditionOperator.AT_LEAST, 20),
            RoutineConditionSnapshot(batteryPercent = 20)
        ))
    }

    @Test fun batteryRejectsUnavailableOrInvalidValues() {
        val c = RoutineCondition(RoutineConditionType.BATTERY_PERCENT,
            RoutineConditionOperator.EQUALS, 50)
        assertNull(RoutineConditionEvaluator.evaluate(c, RoutineConditionSnapshot()))
        assertNull(RoutineConditionEvaluator.evaluate(c,
            RoutineConditionSnapshot(batteryPercent = 101)))
    }

    @Test fun timeWindowIncludesBoundaries() {
        val c = RoutineCondition(RoutineConditionType.TIME_OF_DAY,
            RoutineConditionOperator.BETWEEN, 600, 660)
        assertEquals(true, RoutineConditionEvaluator.evaluate(c,
            RoutineConditionSnapshot(minuteOfDay = 600)))
        assertEquals(true, RoutineConditionEvaluator.evaluate(c,
            RoutineConditionSnapshot(minuteOfDay = 660)))
    }

    @Test fun timeWindowRejectsOutsideRange() {
        val c = RoutineCondition(RoutineConditionType.TIME_OF_DAY,
            RoutineConditionOperator.BETWEEN, 600, 660)
        assertEquals(false, RoutineConditionEvaluator.evaluate(c,
            RoutineConditionSnapshot(minuteOfDay = 700)))
    }

    @Test fun timeWindowSupportsMidnightCrossing() {
        val c = RoutineCondition(RoutineConditionType.TIME_OF_DAY,
            RoutineConditionOperator.BETWEEN, 1320, 360)
        assertEquals(true, RoutineConditionEvaluator.evaluate(c,
            RoutineConditionSnapshot(minuteOfDay = 60)))
        assertEquals(false, RoutineConditionEvaluator.evaluate(c,
            RoutineConditionSnapshot(minuteOfDay = 720)))
    }

    @Test fun timeRejectsInvalidSnapshot() {
        val c = RoutineCondition(RoutineConditionType.TIME_OF_DAY,
            RoutineConditionOperator.BETWEEN, 100, 200)
        assertNull(RoutineConditionEvaluator.evaluate(c,
            RoutineConditionSnapshot(minuteOfDay = 1440)))
    }

    @Test fun previousStepSuccessAndFailure() {
        assertEquals(true, RoutineConditionEvaluator.evaluate(
            RoutineCondition(RoutineConditionType.PREVIOUS_STEP,
                RoutineConditionOperator.SUCCEEDED),
            RoutineConditionSnapshot(previousStepStatus = "SUCCESS")))
        assertEquals(true, RoutineConditionEvaluator.evaluate(
            RoutineCondition(RoutineConditionType.PREVIOUS_STEP,
                RoutineConditionOperator.FAILED),
            RoutineConditionSnapshot(previousStepStatus = "FAILED")))
    }

    @Test fun previousStepRejectsUnknownStatus() {
        val c = RoutineCondition(RoutineConditionType.PREVIOUS_STEP,
            RoutineConditionOperator.SUCCEEDED)
        assertNull(RoutineConditionEvaluator.evaluate(c,
            RoutineConditionSnapshot(previousStepStatus = "RUNNING")))
    }

    @Test fun rejectsUnsupportedOperatorForConditionType() {
        val c = RoutineCondition(RoutineConditionType.TIME_OF_DAY,
            RoutineConditionOperator.LESS_THAN, 500)
        assertNull(RoutineConditionEvaluator.evaluate(c,
            RoutineConditionSnapshot(minuteOfDay = 300)))
    }
}
