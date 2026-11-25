package com.akingstar.askai

class WhatsAppSpeechRetryPolicy(
    private val maxRetries: Int = 2
) {
    private var retriesUsed = 0

    init {
        require(maxRetries >= 0)
    }

    fun shouldRetry(): Boolean {
        if (retriesUsed >= maxRetries) return false
        retriesUsed++
        return true
    }

    fun onSuccess() {
        retriesUsed = 0
    }

    fun reset() {
        retriesUsed = 0
    }

    fun retriesUsed(): Int = retriesUsed
}
