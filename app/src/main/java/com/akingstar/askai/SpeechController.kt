package com.akingstar.askai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.concurrent.atomic.AtomicLong

class SpeechController(
    private val context: Context,
    private val onText: (String) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var stopped = false
    private val lifecycleGeneration = AtomicLong(0L)

    var language: String = "hi-IN"

    fun start() {
        val request = lifecycleGeneration.incrementAndGet()
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { startOnMain(request) }
            return
        }
        startOnMain(request)
    }

    private fun startOnMain(request: Long) {
        if (lifecycleGeneration.get() != request) return
        stopped = false
        stopRecognizer()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatus("इस फोन में Speech Recognition उपलब्ध नहीं है।")
            return
        }

        try {
            val current = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = current

            fun active(): Boolean = !stopped && lifecycleGeneration.get() == request && recognizer === current

            fun finish() {
                if (recognizer === current) {
                    recognizer = null
                    try { current.destroy() } catch (_: RuntimeException) {}
                }
            }

            current.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (active()) onStatus("सुन रहा हूँ…")
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    if (active()) onStatus("समझ रहा हूँ…")
                }

                override fun onError(error: Int) {
                    if (!active()) return
                    finish()
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO ->
                            "माइक्रोफोन में समस्या है।"
                        SpeechRecognizer.ERROR_CLIENT ->
                            "Voice सेवा रुक गई। फिर कोशिश करें।"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "माइक्रोफोन की अनुमति जरूरी है।"
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Voice पहचानने के लिए नेटवर्क उपलब्ध नहीं है।"
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "आवाज़ समझ नहीं आई। कृपया फिर बोलिए।"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                            "Voice सेवा व्यस्त है। थोड़ी देर बाद कोशिश करें।"
                        else ->
                            "Voice पहचानने में समस्या हुई। फिर कोशिश करें।"
                    }
                    onStatus(message)
                }

                override fun onResults(results: Bundle?) {
                    if (!active()) return
                    val words = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    val text = words?.firstOrNull()?.trim().orEmpty()
                    finish()
                    if (text.isNotBlank()) onText(text)
                    else onStatus("कृपया फिर बोलिए।")
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            current.startListening(intent)
        } catch (_: SecurityException) {
            stopRecognizer()
            onStatus("माइक्रोफोन की अनुमति जरूरी है।")
        } catch (_: RuntimeException) {
            stopRecognizer()
            onStatus("Voice सेवा शुरू नहीं हो सकी। फिर कोशिश करें।")
        }
    }

    private fun stopRecognizer() {
        val old = recognizer
        recognizer = null
        if (old != null) {
            try { old.cancel() } catch (_: RuntimeException) {}
            try { old.destroy() } catch (_: RuntimeException) {}
        }
    }

    fun stop() {
        val request = lifecycleGeneration.incrementAndGet()
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { stopOnMain(request) }
            return
        }
        stopOnMain(request)
    }

    private fun stopOnMain(request: Long) {
        if (lifecycleGeneration.get() != request) return
        stopped = true
        stopRecognizer()
    }
}
