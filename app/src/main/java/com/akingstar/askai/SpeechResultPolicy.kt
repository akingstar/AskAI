package com.akingstar.askai

object SpeechResultPolicy {

    fun normalize(candidates: List<String>?): String? {
        if (candidates == null) return null

        for (candidate in candidates) {
            val normalized = candidate.trim()
            if (normalized.isNotEmpty()) {
                return normalized
            }
        }

        return null
    }
}
