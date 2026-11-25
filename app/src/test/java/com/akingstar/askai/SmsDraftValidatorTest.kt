package com.akingstar.askai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsDraftValidatorTest {
    @Test
    fun trimsMessageWhitespace() {
        assertEquals("नमस्ते", SmsDraftValidator.normalize("  नमस्ते  "))
    }

    @Test
    fun preservesMultilineMessage() {
        assertEquals(
            "नमस्ते\nकैसे हैं?",
            SmsDraftValidator.normalize("नमस्ते\nकैसे हैं?")
        )
    }

    @Test
    fun rejectsEmptyMessage() {
        assertNull(SmsDraftValidator.normalize("   "))
    }

    @Test
    fun rejectsMessageLongerThanLimit() {
        assertNull(SmsDraftValidator.normalize("a".repeat(1001)))
        assertEquals(1000, SmsDraftValidator.normalize("a".repeat(1000))?.length)
    }

    @Test
    fun rejectsUnsupportedControlCharacters() {
        assertNull(SmsDraftValidator.normalize("hello\u0000world"))
    }

    @Test
    fun preservesHindiAndEmoji() {
        assertEquals(
            "नमस्ते 😊",
            SmsDraftValidator.normalize("नमस्ते 😊")
        )
    }
}
