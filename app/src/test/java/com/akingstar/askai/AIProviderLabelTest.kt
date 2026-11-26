package com.akingstar.askai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AIProviderLabelTest {
    @Test fun providerPrefixesStatusTextOnly() {
        val labeled = AIErrorMessages.forServerError(401, "Gemini")
        val plain = AIErrorMessages.forServerError(401)
        assertTrue(labeled.first.startsWith("Gemini: "))
        assertEquals(plain.second, labeled.second)
    }

    @Test fun blankProviderKeepsOriginalText() {
        assertEquals(
            AIErrorMessages.forServerError(503),
            AIErrorMessages.forServerError(503, "")
        )
    }

    @Test fun withProviderTagsServerErrorsOnly() {
        val tagged = AIRequestResult.ServerError(429).withProvider("Groq")
        assertEquals(AIRequestResult.ServerError(429, "Groq"), tagged)
        assertSame(
            AIRequestResult.NetworkError,
            AIRequestResult.NetworkError.withProvider("Groq")
        )
    }
}
