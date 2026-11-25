package com.akingstar.askai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionFlowPolicyTest {
    @Test
    fun cancellationStopsRemainingExecution() {
        assertTrue(ExecutionFlowPolicy.shouldStopAfter("CANCELLED"))
    }

    @Test
    fun blockedActionStopsRemainingExecution() {
        assertTrue(ExecutionFlowPolicy.shouldStopAfter("BLOCKED"))
    }

    @Test
    fun successAndOrdinaryFailureKeepExistingContinuationBehavior() {
        assertFalse(ExecutionFlowPolicy.shouldStopAfter("SUCCESS"))
        assertFalse(ExecutionFlowPolicy.shouldStopAfter("FAILED"))
        assertFalse(ExecutionFlowPolicy.shouldStopAfter("SKIPPED"))
    }

    @Test
    fun countsOnlyStepsAfterCurrentPositionAsSkipped() {
        assertEquals(3, ExecutionFlowPolicy.remainingStepsAfter(1, 5))
        assertEquals(0, ExecutionFlowPolicy.remainingStepsAfter(4, 5))
        assertEquals(0, ExecutionFlowPolicy.remainingStepsAfter(5, 5))
    }
}
