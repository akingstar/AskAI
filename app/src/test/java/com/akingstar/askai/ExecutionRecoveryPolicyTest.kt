package com.akingstar.askai

import com.akingstar.askai.automation.ExecutionRecoveryPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class ExecutionRecoveryPolicyTest {
    @Test
    fun retriesOnlyFailedSteps() {
        val results = listOf(
            StepExecutionResult(0, "torch", "", "SUCCESS"),
            StepExecutionResult(1, "open_app", "youtube", "FAILED"),
            StepExecutionResult(2, "sms", "", "BLOCKED"),
            StepExecutionResult(3, "camera", "", "CANCELLED"),
            StepExecutionResult(4, "timer", "", "FAILED")
        )

        assertEquals(listOf(1, 4), ExecutionRecoveryPolicy.retryableIndices(results, 5))
    }

    @Test
    fun ignoresInvalidAndDuplicateIndexes() {
        val results = listOf(
            StepExecutionResult(2, "timer", "", "FAILED"),
            StepExecutionResult(2, "timer", "", "FAILED"),
            StepExecutionResult(-1, "timer", "", "FAILED"),
            StepExecutionResult(8, "timer", "", "FAILED"),
            StepExecutionResult(1, "camera", "", "SUCCESS")
        )

        assertEquals(listOf(2), ExecutionRecoveryPolicy.retryableIndices(results, 3))
    }

    @Test
    fun emptyPlanHasNoRetryableSteps() {
        assertEquals(
            emptyList<Int>(),
            ExecutionRecoveryPolicy.retryableIndices(
                listOf(StepExecutionResult(0, "timer", "", "FAILED")),
                0
            )
        )
    }
}
