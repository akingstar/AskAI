package com.akingstar.askai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AnswerModeTest {
    private fun plan(vararg steps: String) =
        """{"language":"hi-IN","steps":[${steps.joinToString(",")}]}"""

    private fun answer(value: String, target: String = "") =
        """{"action":"answer","target":"$target","value":"$value"}"""

    private val home = """{"action":"home","target":"","value":""}"""

    @Test fun acceptsSingleAnswerStep() {
        val parsed = PlanValidator.parse(plan(answer("Monsoon barsaat ka mausam hota hai.")))
        assertNotNull(parsed)
        assertEquals("answer", parsed!!.steps.single().action)
    }

    @Test fun acceptsMaximumLengthAnswer() {
        assertNotNull(PlanValidator.parse(plan(answer("a".repeat(400)))))
    }

    @Test fun rejectsTooLongEmptyOrTargetedAnswers() {
        assertNull(PlanValidator.parse(plan(answer("a".repeat(401)))))
        assertNull(PlanValidator.parse(plan(answer(""))))
        assertNull(PlanValidator.parse(plan(answer("hello", target = "YouTube"))))
    }

    @Test fun rejectsAnswerMixedWithPhoneActions() {
        assertNull(PlanValidator.parse(plan(answer("hello"), home)))
        assertNull(PlanValidator.parse(plan(home, answer("hello"))))
    }

    @Test fun rejectsAnswerWithCondition() {
        val json = """{"language":"hi-IN","steps":[{"action":"answer","target":"","value":"hi","condition":{"type":"BATTERY_PERCENT","operator":"LESS_THAN","value":20,"upperValue":0}}]}"""
        assertNull(PlanValidator.parse(json))
    }

    @Test fun displayTextOnlyForSingleAnswerPlans() {
        val single = ActionPlan("hi-IN", listOf(ActionStep("answer", value = "ok")))
        val mixed = ActionPlan("hi-IN", listOf(ActionStep("answer", value = "ok"), ActionStep("home")))
        val action = ActionPlan("hi-IN", listOf(ActionStep("home")))
        assertEquals("ok", AnswerPolicy.displayText(single))
        assertNull(AnswerPolicy.displayText(mixed))
        assertNull(AnswerPolicy.displayText(action))
    }

    @Test fun encodeParseRoundTripKeepsAnswer() {
        val original = ActionPlan("hi-IN", listOf(ActionStep("answer", value = "ok")))
        assertEquals(original, PlanValidator.parse(PlanValidator.encode(original)))
    }
}
