package com.akingstar.askai.automation

import com.akingstar.askai.StepExecutionResult

object ExecutionRecoveryPolicy {
    fun retryableIndices(
        results: List<StepExecutionResult>,
        planSize: Int
    ): List<Int> {
        if (planSize <= 0) return emptyList()
        return results.asSequence()
            .filter { it.status == "FAILED" && it.index in 0 until planSize }
            .map { it.index }
            .distinct()
            .sorted()
            .toList()
    }
}
