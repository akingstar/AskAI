package com.akingstar.askai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationMemoryTest {
    private fun completedSummary() = ExecutionSummary(
        totalSteps = 1,
        succeededSteps = 1,
        failedSteps = 0,
        cancelledSteps = 0,
        stepResults = listOf(
            StepExecutionResult(
                index = 0,
                action = "torch",
                target = "",
                status = "SUCCESS"
            )
        )
    )

    @Test
    fun keepsOnlyLastThreeCompletedTurns() {
        val memory = ConversationMemory()
        repeat(4) { memory.record("command $it", completedSummary()) }

        val turns = memory.snapshot()
        assertEquals(3, turns.size)
        assertEquals("command 1", turns.first().command)
        assertEquals("command 3", turns.last().command)
    }

    @Test
    fun excludesSensitiveCommands() {
        val memory = ConversationMemory()
        memory.record("send OTP 123456", completedSummary())
        memory.record("call this number 9876543210", completedSummary())
        memory.record("email test@example.com", completedSummary())
        assertTrue(memory.snapshot().isEmpty())
    }

    @Test
    fun excludesIncompleteExecution() {
        val memory = ConversationMemory()
        memory.record(
            "torch on karo",
            ExecutionSummary(1, 0, 0, 0, emptyList())
        )
        assertTrue(memory.snapshot().isEmpty())
    }

    @Test
    fun clearRemovesSessionContext() {
        val memory = ConversationMemory()
        memory.record("torch on karo", completedSummary())
        memory.clear()
        assertTrue(memory.snapshot().isEmpty())
    }
}
