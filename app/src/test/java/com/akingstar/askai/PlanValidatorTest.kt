package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class PlanValidatorTest {
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

}
