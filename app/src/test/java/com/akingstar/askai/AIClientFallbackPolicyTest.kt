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

    @Test fun invalidNonBlankEndpointDoesNotEnableOfflineFallback() {
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser("http://insecure.example"))
        assertFalse(AIClientFallbackPolicy.shouldUseOfflineParser("not-a-url"))
    }
}
