package com.akingstar.askai

object SpeechTimeoutPolicy {
    const val TIMEOUT_MS = 60_000L

    fun hasExpired(elapsedMillis: Long): Boolean {
        return elapsedMillis >= TIMEOUT_MS
    }
}
