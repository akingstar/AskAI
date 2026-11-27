package com.akingstar.askai

internal object AIClientFallbackPolicy {
    // Intentional offline mode is available only when no online provider is configured.
    fun shouldUseOfflineParser(endpoint: String): Boolean = endpoint.isBlank()

    // Central decision used after all configured providers have been attempted.
    fun shouldUseOfflineParser(
        noProviders: Boolean,
        failure: AIRequestResult
    ): Boolean = noProviders || shouldAttemptFallback(failure)

    // Provider-independent transient failure policy for Gemini, Groq and Worker.
    fun shouldAttemptFallback(failure: AIRequestResult): Boolean = when (failure) {
        AIRequestResult.NetworkError,
        AIRequestResult.InvalidResponse -> true

        is AIRequestResult.ServerError ->
            failure.statusCode == 429 || failure.statusCode in 500..599

        else -> false
    }

    // Retained for endpoint-specific callers/tests; an insecure endpoint never qualifies.
    fun shouldAttemptFallback(
        endpoint: String,
        failure: AIRequestResult
    ): Boolean {
        val secureEndpoint = runCatching {
            val url = java.net.URL(endpoint)
            url.protocol.equals("https", ignoreCase = true) &&
                url.host.isNotBlank() &&
                url.userInfo == null
        }.getOrDefault(false)

        return secureEndpoint && shouldAttemptFallback(failure)
    }
}

internal sealed class AIRequestResult {
    data class Success(
        val plan: ActionPlan,
        val offline: Boolean = false
    ) : AIRequestResult()

    object UnrecognizedCommand : AIRequestResult()
    object UsageLimitReached : AIRequestResult()
    object DuplicateRequest : AIRequestResult()
    object InvalidEndpoint : AIRequestResult()
    object NetworkError : AIRequestResult()
    data class ServerError(val statusCode: Int, val provider: String = "") : AIRequestResult()
    object InvalidResponse : AIRequestResult()
}
