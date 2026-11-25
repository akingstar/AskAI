package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class WhatsAppSpeechRetryPolicyTest {

    @Test
    fun allowsExactlyTwoRetries() {
        val policy = WhatsAppSpeechRetryPolicy()
        assertTrue(policy.shouldRetry())
        assertTrue(policy.shouldRetry())
        assertFalse(policy.shouldRetry())
        assertEquals(2, policy.retriesUsed())
    }

    @Test
    fun successResetsRetryBudget() {
        val policy = WhatsAppSpeechRetryPolicy()
        assertTrue(policy.shouldRetry())
        assertTrue(policy.shouldRetry())
        policy.onSuccess()
        assertEquals(0, policy.retriesUsed())
        assertTrue(policy.shouldRetry())
        assertTrue(policy.shouldRetry())
        assertFalse(policy.shouldRetry())
    }

    @Test
    fun resetStartsFreshSession() {
        val policy = WhatsAppSpeechRetryPolicy()
        policy.shouldRetry()
        policy.shouldRetry()
        policy.reset()
        assertEquals(0, policy.retriesUsed())
        assertTrue(policy.shouldRetry())
        assertTrue(policy.shouldRetry())
        assertFalse(policy.shouldRetry())
    }

    @Test
    fun zeroRetryBudgetNeverRetries() {
        val policy = WhatsAppSpeechRetryPolicy(0)
        assertFalse(policy.shouldRetry())
    }

    @Test
    fun customRetryBudgetIsRespected() {
        val policy = WhatsAppSpeechRetryPolicy(1)
        assertTrue(policy.shouldRetry())
        assertFalse(policy.shouldRetry())
    }
}
