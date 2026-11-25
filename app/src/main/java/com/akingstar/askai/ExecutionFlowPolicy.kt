package com.akingstar.askai

object ExecutionFlowPolicy {
    fun shouldStopAfter(status: String): Boolean =
        status == "CANCELLED" || status == "BLOCKED"

    fun remainingStepsAfter(position: Int, total: Int): Int =
        (total - position - 1).coerceAtLeast(0)
}
