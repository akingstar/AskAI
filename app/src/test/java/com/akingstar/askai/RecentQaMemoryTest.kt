package com.akingstar.askai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentQaMemoryTest {
    @Test
    fun keepsOnlyLastThreeSafeQuestionAnswerPairs() {
        val memory = RecentQaMemory()
        repeat(4) { memory.record("Question $it?", "Answer $it") }

        val turns = memory.snapshot()
        assertEquals(3, turns.size)
        assertEquals("Question 1?", turns.first().question)
        assertEquals("Answer 3", turns.last().answer)
    }

    @Test
    fun rejectsSensitiveQuestionOrAnswer() {
        val memory = RecentQaMemory()
        memory.record("What is my OTP 123456?", "It is private")
        memory.record("What is the capital?", "Your password is secret")
        memory.record("Email test@example.com", "A safe answer")
        assertTrue(memory.snapshot().isEmpty())
    }

    @Test
    fun rejectsOverlongAndMultilineText() {
        val memory = RecentQaMemory()
        memory.record("q".repeat(241), "answer")
        memory.record("question", "a".repeat(401))
        memory.record("question\ncontinued", "answer")
        assertTrue(memory.snapshot().isEmpty())
    }

    @Test
    fun clearRemovesAllSessionQa() {
        val memory = RecentQaMemory()
        memory.record("India ki capital kya hai?", "New Delhi")
        memory.clear()
        assertTrue(memory.snapshot().isEmpty())
    }

    @Test
    fun allowsClearReferentialFollowUpQuestions() {
        assertTrue(QaContextPolicy.shouldInclude("Wahan ka weather kaisa hai?"))
        assertTrue(QaContextPolicy.shouldInclude("What is the weather there?"))
        assertTrue(QaContextPolicy.shouldInclude("What about it?"))
    }

    @Test
    fun doesNotAttachQaContextToNewQuestionsOrPhoneActions() {
        assertFalse(QaContextPolicy.shouldInclude("India ki capital kya hai?"))
        assertFalse(QaContextPolicy.shouldInclude("torch on karo"))
        assertFalse(QaContextPolicy.shouldInclude("wahan call karo"))
        assertFalse(QaContextPolicy.shouldInclude("open settings there"))
        assertFalse(QaContextPolicy.shouldInclude("OTP 123456 wahan bhejo"))
        assertFalse(QaContextPolicy.shouldInclude("wahan maps kholo"))
        assertFalse(QaContextPolicy.shouldInclude("wahan jao"))
        assertFalse(QaContextPolicy.shouldInclude("navigate there"))
        assertFalse(QaContextPolicy.shouldInclude("wahan map open karo"))
    }
}
