package com.akingstar.askai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstPolicyTest {
    private fun plan(vararg steps: ActionStep) = ActionPlan("en-IN", steps.toList())

    @Test fun fixedSingleCommandsAreFastPathSafe() {
        val goHome = OfflineParser.parse("go home")
        assertNotNull(goHome)
        assertTrue(OfflineFirstPolicy.isFastPathSafe(goHome!!))
        val brightness = OfflineParser.parse("brightness 30 percent kar do")
        assertNotNull(brightness)
        assertTrue(OfflineFirstPolicy.isFastPathSafe(brightness!!))
    }

    @Test fun multipleFixedCommandsAreFastPathSafe() {
        val parsed = OfflineParser.parse("go home and volume up")
        assertNotNull(parsed)
        assertEquals(2, parsed!!.steps.size)
        assertTrue(OfflineFirstPolicy.isFastPathSafe(parsed))
    }

    @Test fun freeFormActionsStayOnline() {
        assertFalse(OfflineFirstPolicy.isFastPathSafe(plan(ActionStep("tap_text", "ok"))))
        assertFalse(OfflineFirstPolicy.isFastPathSafe(plan(ActionStep("long_press_text", "ok"))))
        assertFalse(OfflineFirstPolicy.isFastPathSafe(plan(ActionStep("type_text", "field", "hello"))))
    }

    @Test fun webSearchIsSafeAloneButNotInsideMultiStepPlans() {
        assertTrue(OfflineFirstPolicy.isFastPathSafe(plan(ActionStep("web", value = "delhi weather"))))
        assertFalse(OfflineFirstPolicy.isFastPathSafe(
            plan(ActionStep("web", value = "weather"), ActionStep("music"))
        ))
    }

    @Test fun emptyPlanIsNotFastPathSafe() {
        assertFalse(OfflineFirstPolicy.isFastPathSafe(ActionPlan("en-IN", emptyList())))
    }
}
