package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class PlanValidatorTest {
    @Test fun acceptsAccessibilityNavigationActions() {
        val actions = listOf(
            "scroll_up", "scroll_down", "home", "back",
            "recents", "notifications", "quick_settings",
            "read_screen", "accessibility_settings"
        )
        actions.forEach { action ->
            val json = """{"language":"hi-IN","steps":[{"action":"$action","target":"","value":""}]}"""
            assertNotNull("$action should be accepted", PlanValidator.parse(json))
        }
    }

    @Test fun acceptsTextActionsWithRequiredArguments() {
        val tap = """{"language":"hi-IN","steps":[{"action":"tap_text","target":"Settings","value":""}]}"""
        val press = """{"language":"hi-IN","steps":[{"action":"long_press_text","target":"Photo","value":""}]}"""
        val type = """{"language":"hi-IN","steps":[{"action":"type_text","target":"Search","value":"weather"}]}"""
        assertNotNull(PlanValidator.parse(tap))
        assertNotNull(PlanValidator.parse(press))
        assertNotNull(PlanValidator.parse(type))
    }

    @Test fun rejectsAmbiguousOrMalformedTextActions() {
        val noTarget = """{"language":"hi-IN","steps":[{"action":"tap_text","target":"","value":""}]}"""
        val typeNoHint = """{"language":"hi-IN","steps":[{"action":"type_text","target":"","value":"hello"}]}"""
        val typeNoValue = """{"language":"hi-IN","steps":[{"action":"type_text","target":"Search","value":""}]}"""
        val navWithTarget = """{"language":"hi-IN","steps":[{"action":"home","target":"YouTube","value":""}]}"""
        assertNull(PlanValidator.parse(noTarget))
        assertNull(PlanValidator.parse(typeNoHint))
        assertNull(PlanValidator.parse(typeNoValue))
        assertNull(PlanValidator.parse(navWithTarget))
    }

    @Test fun acceptsExactSchema() {
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":""}]}"""
        assertNotNull(PlanValidator.parse(p))
    }
    @Test fun rejectsUnknownAction() {
        val p = """{"language":"hi-IN","steps":[{"action":"hack","target":"","value":""}]}"""
        assertNull(PlanValidator.parse(p))
    }


    @Test fun rejectsExtraRootField() {
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":""}],"extra":true}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsMissingRootField() {
        val p = """{"language":"hi-IN","steps":[]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsEmptySteps() {
        val p = """{"language":"hi-IN","steps":[]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsMoreThanFiveSteps() {
        val steps = List(6) { """{"action":"torch","target":"","value":""}""" }
        val p = """{"language":"hi-IN","steps":[${steps.joinToString(",")}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsExtraStepField() {
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","extra":1}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsWrongFieldTypes() {
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":12,"value":""}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsControlCharacters() {
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"\u0001","value":""}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsOversizedTarget() {
        val target = "a".repeat(161)
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"$target","value":""}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsOversizedValue() {
        val value = "a".repeat(501)
        val p = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"$value"}]}"""
        assertNull(PlanValidator.parse(p))
    }

    @Test fun rejectsOversizedJson() {
        assertNull(PlanValidator.parse(" ".repeat(8193)))
    }

    @Test fun rejectsInvalidLanguage() {
        val blank = """{"language":" ","steps":[{"action":"torch","target":"","value":""}]}"""
        val long = """{"language":"${"a".repeat(21)}","steps":[{"action":"torch","target":"","value":""}]}"""
        assertNull(PlanValidator.parse(blank))
        assertNull(PlanValidator.parse(long))
    }

    @Test fun rejectsMalformedJson() {
        assertNull(PlanValidator.parse("""{"language":"hi-IN","steps":["""))
    }


    @Test fun acceptsLegacyRoutineWithoutCondition() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":""}]}"""
        val plan = PlanValidator.parse(json)
        assertNotNull(plan)
        assertNull(plan!!.steps[0].condition)
    }

    @Test fun parsesAndEncodesBatteryCondition() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"BATTERY_PERCENT","operator":"LESS_THAN","value":20,"upperValue":0}}]}"""
        val plan = PlanValidator.parse(json)
        assertNotNull(plan)
        assertEquals(20, plan!!.steps[0].condition!!.value)
        val restored = PlanValidator.parse(PlanValidator.encode(plan))
        assertEquals(plan, restored)
    }

    @Test fun rejectsInvalidConditionOperator() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"BATTERY_PERCENT","operator":"BETWEEN","value":20,"upperValue":50}}]}"""
        assertNull(PlanValidator.parse(json))
    }

    @Test fun rejectsOutOfRangeConditionValue() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"BATTERY_PERCENT","operator":"LESS_THAN","value":101,"upperValue":0}}]}"""
        assertNull(PlanValidator.parse(json))
    }

    @Test fun rejectsUnknownConditionField() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"BATTERY_PERCENT","operator":"LESS_THAN","value":20,"upperValue":0,"extra":true}}]}"""
        assertNull(PlanValidator.parse(json))
    }

    @Test fun rejectsFractionalConditionValue() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"BATTERY_PERCENT","operator":"LESS_THAN","value":20.5,"upperValue":0}}]}"""
        assertNull(PlanValidator.parse(json))
    }


    @Test fun rejectsPreviousStepConditionOnFirstStep() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"PREVIOUS_STEP","operator":"SUCCEEDED","value":0,"upperValue":0}}]}"""
        assertNull(PlanValidator.parse(json))
    }

    @Test fun acceptsPreviousStepConditionAfterFirstStep() {
        val json = """{"language":"hi-IN","steps":[{"action":"camera","target":"","value":""},{"action":"torch","target":"","value":"","condition":{"type":"PREVIOUS_STEP","operator":"SUCCEEDED","value":0,"upperValue":0}}]}"""
        val plan = PlanValidator.parse(json)
        assertNotNull(plan)
        assertEquals(
            com.akingstar.askai.automation.RoutineConditionType.PREVIOUS_STEP,
            plan!!.steps[1].condition!!.type
        )
    }

    @Test fun acceptsOvernightTimeCondition() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"TIME_OF_DAY","operator":"BETWEEN","value":1320,"upperValue":360}}]}"""
        val plan = PlanValidator.parse(json)
        assertNotNull(plan)
        assertEquals(1320, plan!!.steps[0].condition!!.value)
        assertEquals(360, plan.steps[0].condition!!.upperValue)
    }

    @Test fun rejectsOutOfRangeTimeCondition() {
        val json = """{"language":"hi-IN","steps":[{"action":"torch","target":"","value":"","condition":{"type":"TIME_OF_DAY","operator":"BETWEEN","value":1440,"upperValue":360}}]}"""
        assertNull(PlanValidator.parse(json))
    }

}
