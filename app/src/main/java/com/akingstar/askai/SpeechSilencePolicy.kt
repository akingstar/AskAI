package com.akingstar.askai

/** Silence hints for the speech recognizer. Recognizers may ignore them. */
object SpeechSilencePolicy {
    const val COMPLETE_SILENCE_MS = 1000
    const val POSSIBLY_COMPLETE_SILENCE_MS = 800

    fun isValid(
        complete: Int = COMPLETE_SILENCE_MS,
        possibly: Int = POSSIBLY_COMPLETE_SILENCE_MS
    ): Boolean = complete in 600..2500 && possibly in 400..complete
}
