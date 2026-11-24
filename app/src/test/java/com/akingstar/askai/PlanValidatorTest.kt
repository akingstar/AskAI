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
}
