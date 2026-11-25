package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class WhatsAppDraftFlowTest {

    @Test
    fun beginRequestsMessage() {
        val flow = WhatsAppDraftFlow()

        assertEquals(
            WhatsAppDraftFlow.Result.AskForMessage,
            flow.begin()
        )
        assertEquals(
            WhatsAppDraftFlow.State.AWAITING_MESSAGE,
            flow.state
        )
    }

    @Test
    fun dictatedHindiMessageCreatesPreview() {
        val flow = WhatsAppDraftFlow()
        flow.begin()

        assertEquals(
            WhatsAppDraftFlow.Result.Preview("नमस्ते, कैसे हैं?"),
            flow.accept("नमस्ते, कैसे हैं?")
        )
    }

    @Test
    fun confirmationOpensDraftButDoesNotSend() {
        val flow = WhatsAppDraftFlow()
        flow.begin()
        flow.accept("नमस्ते")

        assertEquals(
            WhatsAppDraftFlow.Result.OpenDraft("नमस्ते"),
            flow.accept("हाँ")
        )
        assertEquals(WhatsAppDraftFlow.State.IDLE, flow.state)
    }

    @Test
    fun repeatKeepsSameDraft() {
        val flow = WhatsAppDraftFlow()
        flow.begin()
        flow.accept("नमस्ते")

        assertEquals(
            WhatsAppDraftFlow.Result.RepeatPreview,
            flow.accept("दोबारा सुनाओ")
        )
        assertEquals("नमस्ते", flow.currentDraft())
    }

    @Test
    fun editReturnsToMessageCapture() {
        val flow = WhatsAppDraftFlow()
        flow.begin()
        flow.accept("पहला संदेश")

        assertEquals(
            WhatsAppDraftFlow.Result.AskForMessage,
            flow.accept("बदलो")
        )
        assertEquals(
            WhatsAppDraftFlow.State.AWAITING_MESSAGE,
            flow.state
        )
    }

    @Test
    fun cancelResetsFlow() {
        val flow = WhatsAppDraftFlow()
        flow.begin()
        flow.accept("नमस्ते")

        assertEquals(
            WhatsAppDraftFlow.Result.Cancelled,
            flow.accept("रद्द करो")
        )
        assertNull(flow.currentDraft())
        assertEquals(WhatsAppDraftFlow.State.IDLE, flow.state)
    }

    @Test
    fun rejectsEmptyAndOversizedMessages() {
        val flow = WhatsAppDraftFlow()
        flow.begin()

        assertEquals(
            WhatsAppDraftFlow.Result.MessageRejected,
            flow.accept("   ")
        )
        assertEquals(
            WhatsAppDraftFlow.Result.MessageRejected,
            flow.accept("क".repeat(1001))
        )
    }

    @Test
    fun doesNotTreatUnknownConfirmationAsApproval() {
        val flow = WhatsAppDraftFlow()
        flow.begin()
        flow.accept("नमस्ते")

        assertEquals(
            WhatsAppDraftFlow.Result.Clarify,
            flow.accept("शायद")
        )
        assertEquals("नमस्ते", flow.currentDraft())
    }

    @Test
    fun ignoresInputWhenFlowIsIdle() {
        val flow = WhatsAppDraftFlow()

        assertEquals(
            WhatsAppDraftFlow.Result.Ignored,
            flow.accept("हाँ")
        )
    }
}
