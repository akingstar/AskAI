package com.akingstar.askai

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class TextToSpeechController(
    context: Context,
    private val ready: (Boolean) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private val utteranceIds = AtomicLong(0L)

    private var engine: TextToSpeech? = null
    private var languageTag: String = "hi-IN"
    private var initialized = false
    private var closed = false
    private var pendingText: String? = null
    private var pendingCompletion: (() -> Unit)? = null
    private var activeUtteranceId: String? = null
    private var activeCompletion: (() -> Unit)? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            main.post {
                if (closed) return@post

                initialized = status == TextToSpeech.SUCCESS

                if (initialized) {
                    applyLanguage()
                    engine?.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String) = Unit

                            override fun onDone(utteranceId: String) {
                                completeUtterance(utteranceId)
                            }

                            override fun onError(utteranceId: String) {
                                completeUtterance(utteranceId)
                            }
                        }
                    )

                    val text = pendingText
                    val completion = pendingCompletion
                    pendingText = null
                    pendingCompletion = null

                    if (text != null) {
                        speakNow(text, completion)
                    } else {
                        completion?.let { main.post(it) }
                    }
                } else {
                    pendingText = null
                    val completion = pendingCompletion
                    pendingCompletion = null
                    completion?.let { main.post(it) }
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
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            if (languageTag != "en-IN") {
                current.setLanguage(Locale.forLanguageTag("en-IN"))
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { speak(text, onDone) }
            return
        }

        if (closed) return
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }

        if (!initialized) {
            pendingText = text.take(2000)
            pendingCompletion = onDone
            return
        }

        speakNow(text, onDone)
    }

    private fun speakNow(text: String, onDone: (() -> Unit)?) {
        if (closed || !initialized) {
            onDone?.invoke()
            return
        }

        val id = "askai-${utteranceIds.incrementAndGet()}"
        activeUtteranceId = id
        activeCompletion = onDone

        try {
            val result = engine?.speak(
                text.take(2000),
                TextToSpeech.QUEUE_FLUSH,
                null,
                id
            ) ?: TextToSpeech.ERROR

            if (result == TextToSpeech.ERROR) {
                completeUtterance(id)
            }
        } catch (_: RuntimeException) {
            completeUtterance(id)
        }
    }

    private fun completeUtterance(id: String) {
        main.post {
            if (closed || activeUtteranceId != id) return@post

            activeUtteranceId = null
            val completion = activeCompletion
            activeCompletion = null
            completion?.invoke()
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
        pendingCompletion = null
        activeUtteranceId = null
        activeCompletion = null

        engine?.let {
            try { it.stop() } catch (_: RuntimeException) {}
            try { it.shutdown() } catch (_: RuntimeException) {}
        }
        engine = null
    }
}
