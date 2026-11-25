package com.akingstar.askai

/**
 * Identifies the currently authorized WhatsApp voice conversation.
 * Invalidated sessions cannot trigger delayed callbacks.
 */
class WhatsAppVoiceSessionGuard {
    private var generation = 0L
    private var activeSession: Long? = null

    fun begin(): Long {
        generation += 1
        activeSession = generation
        return generation
    }

    fun invalidate() {
        generation += 1
        activeSession = null
    }

    fun currentSessionId(): Long? = activeSession

    fun isActive(sessionId: Long): Boolean =
        activeSession == sessionId
}
