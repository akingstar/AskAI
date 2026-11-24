package com.akingstar.askai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechController(
    context: Context,
    private val ready: (Boolean) -> Unit
) {
    private var engine: TextToSpeech? = null
    var language: String = "hi-IN"

    init {
        engine = TextToSpeech(context) { status ->
            ready(status == TextToSpeech.SUCCESS)
            setLanguage(language)
        }
    }

    fun setLanguage(tag: String) {
        language = tag
        val locale = Locale.forLanguageTag(tag)
        engine?.language = locale
    }

    fun speak(text: String) {
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "askai-${System.currentTimeMillis()}")
    }

    fun close() {
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}
