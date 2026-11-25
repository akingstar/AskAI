package com.akingstar.askai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIClientFallbackPolicyTest {
    @Test fun blankEndpointAllowsIntentionalOfflineMode() {
        assertTrue(AIClientFallbackPolicy.shouldUseOfflineParser(""))
        assertTrue(AIClientFallbackPolicy.shouldUseOfflineParser("   "))
    }

    @Test fun configuredEndpointNeverFallsBackToOfflineParser() {
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser("https://worker.example"))
    }

    @Test fun networkAndInvalidResponsesAllowFallbackForConfiguredEndpoint() {
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.NetworkError
        ))
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.InvalidResponse
        ))
    }

    @Test fun onlyRateLimitAndServerErrorsAllowHttpFallback() {
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.ServerError(429)
        ))
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.ServerError(500)
        ))
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.ServerError(503)
        ))
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.ServerError(400)
        ))
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.ServerError(401)
        ))
    }

    @Test fun invalidEndpointAndBlankEndpointDoNotUseTransientFallbackPolicy() {
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(
            "http://worker.example", AIRequestResult.NetworkError
        ))
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(
            "not-a-url", AIRequestResult.InvalidResponse
        ))
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://user:pass@worker.example", AIRequestResult.NetworkError
        ))
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(
            "", AIRequestResult.NetworkError
        ))
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.InvalidEndpoint
        ))
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.UnrecognizedCommand
        ))
    }

    @Test fun invalidNonBlankEndpointDoesNotEnableOfflineFallback() {
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser("http://insecure.example"))
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser("not-a-url"))
    }
}
