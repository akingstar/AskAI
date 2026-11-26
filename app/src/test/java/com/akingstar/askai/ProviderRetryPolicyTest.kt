package com.akingstar.askai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderRetryPolicyTest {
    @Test fun networkAndServerErrorsRetryOnce() {
        assertTrue(ProviderRetryPolicy.shouldRetry(AIRequestResult.NetworkError, 0))
        assertTrue(ProviderRetryPolicy.shouldRetry(AIRequestResult.ServerError(500), 0))
        assertTrue(ProviderRetryPolicy.shouldRetry(AIRequestResult.ServerError(503), 0))
        assertFalse(ProviderRetryPolicy.shouldRetry(AIRequestResult.NetworkError, 1))
        assertFalse(ProviderRetryPolicy.shouldRetry(AIRequestResult.ServerError(503), 1))
    }

    @Test fun rateLimitAuthAndInvalidResponsesNeverRetry() {
        assertFalse(ProviderRetryPolicy.shouldRetry(AIRequestResult.ServerError(429), 0))
        assertFalse(ProviderRetryPolicy.shouldRetry(AIRequestResult.ServerError(401), 0))
        assertFalse(ProviderRetryPolicy.shouldRetry(AIRequestResult.ServerError(404), 0))
        assertFalse(ProviderRetryPolicy.shouldRetry(AIRequestResult.InvalidResponse, 0))
        assertFalse(ProviderRetryPolicy.shouldRetry(AIRequestResult.UsageLimitReached, 0))
        assertFalse(ProviderRetryPolicy.shouldRetry(AIRequestResult.DuplicateRequest, 0))
    }
}
