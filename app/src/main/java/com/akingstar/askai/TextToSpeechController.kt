package com.akingstar.askai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechController(
    context: Context,
    private val ready: (Boolean) -> Unit
) {
    private var engine: TextToSpeech? = null
    private var languageTag: String = "hi-IN"

    init {
        engine = TextToSpeech(context) { status ->
            ready(status == TextToSpeech.SUCCESS)
            setLanguage(languageTag)
        }
    }

    fun setLanguage(tag: String) {
        languageTag = tag
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
