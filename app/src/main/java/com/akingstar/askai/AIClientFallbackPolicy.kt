package com.akingstar.askai

internal object AIClientFallbackPolicy {
    fun shouldUseOfflineParser(endpoint: String): Boolean = endpoint.isBlank()

    fun shouldAttemptFallback(
        endpoint: String,
        failure: AIRequestResult
    ): Boolean {
        if (endpoint.isBlank()) return false

        val secureEndpoint = runCatching {
            val url = java.net.URL(endpoint)
            url.protocol.equals("https", ignoreCase = true) &&
                url.host.isNotBlank() &&
                url.userInfo == null
        }.getOrDefault(false)

        if (!secureEndpoint) return false

        return when (failure) {
            AIRequestResult.NetworkError,
            AIRequestResult.InvalidResponse -> true

            is AIRequestResult.ServerError ->
                failure.statusCode == 429 || failure.statusCode >= 500

            else -> false
        }
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
