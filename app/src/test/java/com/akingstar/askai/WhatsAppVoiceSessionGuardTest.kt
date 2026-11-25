package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class WhatsAppVoiceSessionGuardTest {

    @Test
    fun beginCreatesActiveSession() {
        val guard = WhatsAppVoiceSessionGuard()
        val session = guard.begin()

        assertTrue(guard.isActive(session))
        assertEquals(session, guard.currentSessionId())
    }

    @Test
    fun newSessionInvalidatesPreviousSession() {
        val guard = WhatsAppVoiceSessionGuard()
        val oldSession = guard.begin()
        val newSession = guard.begin()

        assertFalse(guard.isActive(oldSession))
        assertTrue(guard.isActive(newSession))
    }

    @Test
    fun invalidateRejectsOldSession() {
        val guard = WhatsAppVoiceSessionGuard()
        val session = guard.begin()

        guard.invalidate()

        assertFalse(guard.isActive(session))
        assertNull(guard.currentSessionId())
    }

    @Test
    fun invalidatedSessionCannotBecomeActiveAgain() {
        val guard = WhatsAppVoiceSessionGuard()
        val oldSession = guard.begin()

        guard.invalidate()
        val newSession = guard.begin()

        assertFalse(guard.isActive(oldSession))
        assertTrue(guard.isActive(newSession))
    }

    @Test
    fun initiallyHasNoActiveSession() {
        val guard = WhatsAppVoiceSessionGuard()

        assertNull(guard.currentSessionId())
        assertFalse(guard.isActive(1L))
    }
}
