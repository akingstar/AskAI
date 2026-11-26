package com.akingstar.askai

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIErrorMessagesTest {
    @Test fun authErrorsMentionKey() {
        assertTrue(AIErrorMessages.forServerError(401).first.contains("key"))
        assertTrue(AIErrorMessages.forServerError(403).first.contains("key"))
    }

    @Test fun differentCausesGetDifferentMessages() {
        val auth = AIErrorMessages.forServerError(401).first
        val missing = AIErrorMessages.forServerError(404).first
        val limit = AIErrorMessages.forServerError(429).first
        val busy = AIErrorMessages.forServerError(503).first
        assertNotEquals(auth, missing)
        assertNotEquals(auth, limit)
        assertNotEquals(limit, busy)
    }

    @Test fun unknownCodeShowsCode() {
        assertTrue(AIErrorMessages.forServerError(418).first.contains("418"))
    }
}
