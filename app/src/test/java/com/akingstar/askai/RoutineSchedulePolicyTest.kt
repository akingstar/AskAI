package com.akingstar.askai

import com.akingstar.askai.automation.RoutineSchedulePolicy
import com.akingstar.askai.automation.RoutineWorkNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineSchedulePolicyTest {

    @Test
    fun acceptsValidRoutineNames() {
        assertTrue(RoutineSchedulePolicy.isValidName("Morning routine"))
        assertTrue(RoutineSchedulePolicy.isValidName(" रूटीन "))
        assertFalse(RoutineSchedulePolicy.isValidName("  "))
        assertFalse(RoutineSchedulePolicy.isValidName("a".repeat(41)))
    }

    @Test
    fun validatesOneTimeDelayBounds() {
        assertFalse(RoutineSchedulePolicy.isValidDelay(0))
        assertTrue(RoutineSchedulePolicy.isValidDelay(1))
        assertTrue(RoutineSchedulePolicy.isValidDelay(525600))
        assertFalse(RoutineSchedulePolicy.isValidDelay(525601))
    }

    @Test
    fun validatesRecurringIntervalBounds() {
        assertFalse(RoutineSchedulePolicy.isValidRecurringInterval(14))
        assertTrue(RoutineSchedulePolicy.isValidRecurringInterval(15))
        assertTrue(RoutineSchedulePolicy.isValidRecurringInterval(525600))
        assertFalse(RoutineSchedulePolicy.isValidRecurringInterval(525601))
    }

    @Test
    fun oneTimeAndRecurringWorkNamesAreIndependent() {
        assertFalse(
            RoutineWorkNames.oneTime("Morning") ==
                RoutineWorkNames.recurring("Morning")
        )
    }

    @Test
    fun workNamesAreCaseAndWhitespaceNormalized() {
        assertEquals(
            RoutineWorkNames.oneTime(" Morning "),
            RoutineWorkNames.oneTime("morning")
        )
        assertEquals(
            RoutineWorkNames.recurring(" Morning "),
            RoutineWorkNames.recurring("morning")
        )
    }
}
