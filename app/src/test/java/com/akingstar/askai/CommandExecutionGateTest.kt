package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class CommandExecutionGateTest {

    @Test
    fun firstRequestAcquiresAndSecondIsRejected() {
        val gate = CommandExecutionGate()

        val first = gate.tryAcquire()

        assertNotNull(first)
        assertTrue(gate.isBusy())
        assertNull(gate.tryAcquire())
    }

    @Test
    fun releasingActiveTokenAllowsNextRequest() {
        val gate = CommandExecutionGate()

        val first = gate.tryAcquire()!!
        assertTrue(gate.release(first))
        assertFalse(gate.isBusy())

        val second = gate.tryAcquire()
        assertNotNull(second)
        assertNotEquals(first, second)
    }

    @Test
    fun staleTokenCannotReleaseCurrentExecution() {
        val gate = CommandExecutionGate()

        val first = gate.tryAcquire()!!
        assertTrue(gate.release(first))

        val second = gate.tryAcquire()!!
        assertFalse(gate.release(first))
        assertTrue(gate.isBusy())

        assertTrue(gate.release(second))
        assertFalse(gate.isBusy())
    }

    @Test
    fun duplicateReleaseIsRejected() {
        val gate = CommandExecutionGate()
        val token = gate.tryAcquire()!!

        assertTrue(gate.release(token))
        assertFalse(gate.release(token))
        assertFalse(gate.isBusy())
    }

    @Test
    fun invalidTokenCannotReleaseGate() {
        val gate = CommandExecutionGate()
        val token = gate.tryAcquire()!!

        assertFalse(gate.release(token + 1))
        assertTrue(gate.isBusy())
        assertTrue(gate.release(token))
    }
}
