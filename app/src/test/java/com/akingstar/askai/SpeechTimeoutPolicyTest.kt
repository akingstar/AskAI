package com.akingstar.askai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTimeoutPolicyTest {

    @Test
    fun timeoutIsSixtySeconds() {
        assertTrue(SpeechTimeoutPolicy.TIMEOUT_MS == 60_000L)
    }

    @Test
    fun doesNotExpireBeforeTimeout() {
        assertFalse(SpeechTimeoutPolicy.hasExpired(0L))
        assertFalse(SpeechTimeoutPolicy.hasExpired(59_999L))
    }

    @Test
    fun expiresAtTimeoutBoundary() {
        assertTrue(SpeechTimeoutPolicy.hasExpired(60_000L))
    }

    @Test
    fun remainsExpiredAfterTimeout() {
        assertTrue(SpeechTimeoutPolicy.hasExpired(60_001L))
    }

    @Test
    fun negativeElapsedTimeDoesNotExpire() {
        assertFalse(SpeechTimeoutPolicy.hasExpired(-1L))
    }
}
