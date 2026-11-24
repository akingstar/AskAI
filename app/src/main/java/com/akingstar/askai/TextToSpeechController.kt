package com.akingstar.askai

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechController(
    context: Context,
    private val ready: (Boolean) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var languageTag: String = "hi-IN"
    private var initialized = false
    private var closed = false
    private var pendingText: String? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            main.post {
                if (closed) return@post
                initialized = status == TextToSpeech.SUCCESS
                if (initialized) {
                    applyLanguage()
                    pendingText?.let {
                        pendingText = null
                        speakNow(it)
                    }
                } else {
                    pendingText = null
                }
                ready(initialized)
            }
        }
    }

    fun setLanguage(tag: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { setLanguage(tag) }
            return
        }
        if (closed) return
        languageTag = tag
        if (initialized) applyLanguage()
    }

    private fun applyLanguage() {
        val current = engine ?: return
        val requested = Locale.forLanguageTag(languageTag)
        val result = current.setLanguage(requested)

        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (languageTag != "en-IN") {
                current.setLanguage(Locale.forLanguageTag("en-IN"))
            }
        }
    }

    fun speak(text: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { speak(text) }
            return
        }
        if (closed || text.isBlank()) return
        if (!initialized) {
            pendingText = text.take(2000)
            return
        }
        speakNow(text)
    }

    private fun speakNow(text: String) {
        if (closed || !initialized) return
        try {
            engine?.speak(
                text.take(2000),
                TextToSpeech.QUEUE_FLUSH,
                null,
                "askai-${System.nanoTime()}"
            )
        } catch (_: RuntimeException) {
            // Speech failure must not crash the assistant.
        }
    }

    fun close() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { close() }
            return
        }
        if (closed) return
        closed = true
        initialized = false
        pendingText = null
        engine?.let {
            try { it.stop() } catch (_: RuntimeException) {}
            try { it.shutdown() } catch (_: RuntimeException) {}
        }
        engine = null
    }
}
