package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class SpeechResultPolicyTest {

    @Test
    fun nullCandidatesReturnNull() {
        assertNull(SpeechResultPolicy.normalize(null))
    }

    @Test
    fun emptyCandidatesReturnNull() {
        assertNull(SpeechResultPolicy.normalize(emptyList()))
    }

    @Test
    fun blankCandidatesReturnNull() {
        assertNull(
            SpeechResultPolicy.normalize(listOf("", "   ", "\n"))
        )
    }

    @Test
    fun skipsBlankCandidatesAndFindsFirstValidResult() {
        assertEquals(
            "नमस्ते",
            SpeechResultPolicy.normalize(listOf("", "  ", " नमस्ते ", "हैलो"))
        )
    }

    @Test
    fun trimsRecognizedText() {
        assertEquals(
            "hello",
            SpeechResultPolicy.normalize(listOf("  hello  "))
        )
    }
}
