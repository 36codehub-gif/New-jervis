package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechManager(context: Context, private val onSpeakingStateChanged: (Boolean) -> Unit) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                val hindiLocale = Locale("hi", "IN")
                val result = tts?.setLanguage(hindiLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to English (India) or default
                    tts?.setLanguage(Locale("en", "IN"))
                }
                tts?.setSpeechRate(1.05f)
                tts?.setPitch(1.0f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingStateChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }

                    override fun onError(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }
                })
            }
        }
    }

    fun speak(text: String, isHindiPreferred: Boolean = true) {
        if (!isInitialized || tts == null || text.isBlank()) return

        if (isHindiPreferred) {
            val res = tts?.setLanguage(Locale("hi", "IN"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("en", "IN"))
            }
        } else {
            tts?.setLanguage(Locale("en", "IN"))
        }

        val utteranceId = System.currentTimeMillis().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        onSpeakingStateChanged(false)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
