package com.akingstar.askai

internal object AIClientFallbackPolicy {
    fun shouldUseOfflineParser(endpoint: String): Boolean = endpoint.isBlank()
}

internal sealed class AIRequestResult {
    data class Success(
        val plan: ActionPlan,
        val offline: Boolean = false
    ) : AIRequestResult()

    object UnrecognizedCommand : AIRequestResult()
    object InvalidEndpoint : AIRequestResult()
    object NetworkError : AIRequestResult()
    data class ServerError(val statusCode: Int) : AIRequestResult()
    object InvalidResponse : AIRequestResult()
}
