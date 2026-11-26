package com.akingstar.askai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechSilencePolicyTest {
    @Test fun defaultsAreValid() {
        assertTrue(SpeechSilencePolicy.isValid())
    }

    @Test fun tooShortOrInconsistentValuesAreRejected() {
        assertFalse(SpeechSilencePolicy.isValid(complete = 500, possibly = 400))
        assertFalse(SpeechSilencePolicy.isValid(complete = 1000, possibly = 300))
        assertFalse(SpeechSilencePolicy.isValid(complete = 1000, possibly = 1200))
        assertFalse(SpeechSilencePolicy.isValid(complete = 3000, possibly = 800))
    }
}
