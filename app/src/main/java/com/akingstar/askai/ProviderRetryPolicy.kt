package com.akingstar.askai

/** One short retry for transient failures only. 429 is never retried (respect the limit). */
internal object ProviderRetryPolicy {
    const val RETRY_DELAY_MS = 700L

    fun shouldRetry(failure: AIRequestResult, retriesDone: Int): Boolean {
        if (retriesDone >= 1) return false
        return when (failure) {
            AIRequestResult.NetworkError -> true
            is AIRequestResult.ServerError -> failure.statusCode in 500..599
            else -> false
        }
    }
}
