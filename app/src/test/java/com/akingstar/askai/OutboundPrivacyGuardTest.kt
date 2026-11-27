package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class OutboundPrivacyGuardTest {
    @Test fun permitsOrdinaryQuestionsAndPhoneCommands() {
        val payload = OutboundPrivacyGuard.prepare("भारत की राजधानी क्या है?",
            listOf(ConversationTurn("torch on karo", "success=1; failed=0")),
            listOf(RecentQaTurn("भारत की राजधानी?", "नई दिल्ली")))
        assertNotNull(payload); assertEquals("भारत की राजधानी क्या है?", payload?.text)
        assertEquals(1, payload?.conversation?.size); assertEquals(1, payload?.qaContext?.size)
    }

    @Test fun blocksPasswordsOtpPinAndFinancialCredentials() {
        listOf("password is hunter2", "OTP 123456", "पिन: 1234", "सीवीवी 123",
            "पासवर्ड: secret123", "UPI ID name@okaxis").forEach {
            assertNull("must not transmit: $it", OutboundPrivacyGuard.prepare(it, emptyList(), emptyList()))
        }
    }

    @Test fun blocksPersonalIdentifiersInRequestBeforeAnyOnlineCall() {
        listOf("Call 9876543210", "Contact test@example.com", "Aadhaar 1234 5678 9012",
            "Card 4111111111111111", "Call +1 202 555 0123").forEach {
            assertNull("must not transmit: $it", OutboundPrivacyGuard.prepare(it, emptyList(), emptyList()))
        }
    }

    @Test fun dropsSensitiveHistoryButKeepsSafeContext() {
        val payload = OutboundPrivacyGuard.prepare("What was the capital?",
            listOf(ConversationTurn("My password is hunter2", "success"), ConversationTurn("torch on karo", "success=1")),
            listOf(RecentQaTurn("OTP 123456", "private"), RecentQaTurn("India ki capital?", "New Delhi")))!!
        assertEquals(listOf("torch on karo"), payload.conversation.map { it.command })
        assertEquals(listOf("India ki capital?"), payload.qaContext.map { it.question })
    }

    @Test fun boundedInputFailsClosed() {
        assertNull(OutboundPrivacyGuard.prepare("x".repeat(4001), emptyList(), emptyList()))
        assertFalse(OutboundPrivacyGuard.canTransmit("x".repeat(16_001)))
    }

    @Test fun rejectsSensitiveSerializedPayloadButAllowsLegitimatePrompt() {
        assertFalse(OutboundPrivacyGuard.canTransmit("Current user request: OTP 123456"))
        assertTrue(OutboundPrivacyGuard.canTransmit("Current user request: भारत की राजधानी क्या है?"))
    }

    @Test
    fun trustedSystemPromptIsSubjectToPayloadSizeLimitOnly() {
        val payload = AIClient.SYSTEM_PROMPT + "\nCurrent user request: भारत की राजधानी क्या है?"
        assertTrue(OutboundPrivacyGuard.isPayloadWithinLimit(payload))
        assertFalse(OutboundPrivacyGuard.isPayloadWithinLimit("x".repeat(16_001)))
    }

}
