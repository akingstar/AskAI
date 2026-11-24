package com.akingstar.askai

internal object AIClientFallbackPolicy {
    fun shouldUseOfflineParser(endpoint: String): Boolean = endpoint.isBlank()
}
