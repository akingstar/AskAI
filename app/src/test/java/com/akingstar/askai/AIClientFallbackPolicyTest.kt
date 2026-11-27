package com.akingstar.askai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIClientFallbackPolicyTest {
    @Test fun blankEndpointAllowsIntentionalOfflineMode() {
        assertTrue(AIClientFallbackPolicy.shouldUseOfflineParser(""))
        assertTrue(AIClientFallbackPolicy.shouldUseOfflineParser("   "))
    }

    @Test fun configuredEndpointNeverEnablesIntentionalOfflineMode() {
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser("https://worker.example"))
    }

    @Test fun transientProviderFailuresAllowFallback() {
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(AIRequestResult.NetworkError))
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(AIRequestResult.InvalidResponse))
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(AIRequestResult.ServerError(429)))
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(AIRequestResult.ServerError(500)))
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(AIRequestResult.ServerError(503)))
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(AIRequestResult.ServerError(599)))
    }

    @Test fun clientErrorsAndNonTransientFailuresDoNotAllowFallback() {
        listOf(400, 401, 403, 404, 499, 600).forEach { code ->
            assertFalse("HTTP $code must not fall back",
                AIClientFallbackPolicy.shouldAttemptFallback(AIRequestResult.ServerError(code)))
        }
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(AIRequestResult.UsageLimitReached))
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(AIRequestResult.DuplicateRequest))
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(AIRequestResult.InvalidEndpoint))
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(AIRequestResult.UnrecognizedCommand))
    }

    @Test fun noConfiguredProvidersAllowsOfflineParser() {
        assertTrue(AIClientFallbackPolicy.shouldUseOfflineParser(
            true, AIRequestResult.InvalidEndpoint
        ))
    }

    @Test fun configuredProvidersUseOnlyTransientFailureFallback() {
        assertTrue(AIClientFallbackPolicy.shouldUseOfflineParser(
            false, AIRequestResult.NetworkError
        ))
        assertTrue(AIClientFallbackPolicy.shouldUseOfflineParser(
            false, AIRequestResult.ServerError(503)
        ))
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser(
            false, AIRequestResult.ServerError(401)
        ))
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser(
            false, AIRequestResult.ServerError(404)
        ))
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser(
            false, AIRequestResult.UsageLimitReached
        ))
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser(
            false, AIRequestResult.DuplicateRequest
        ))
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser(
            false, AIRequestResult.InvalidEndpoint
        ))
    }

    @Test fun endpointSpecificFallbackRequiresSecureEndpoint() {
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.NetworkError
        ))
        assertTrue(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.ServerError(429)
        ))
        assertFalse(AIClientFallbackPolicy.shouldAttemptFallback(
            "https://worker.example", AIRequestResult.ServerError(401)
        ))
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
    }

    @Test fun invalidNonBlankEndpointDoesNotEnableIntentionalOfflineMode() {
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser("http://insecure.example"))
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser("not-a-url"))
    }
}
