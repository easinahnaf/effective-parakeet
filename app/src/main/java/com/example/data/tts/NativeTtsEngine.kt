package com.example.data.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

class NativeTtsEngine(private val context: Context) {

    companion object {
        private const val TAG = "NativeTtsEngine"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initError: String? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale.getDefault()
                Log.d(TAG, "Native TextToSpeech initialized successfully")
            } else {
                isInitialized = false
                initError = "TTS initialization failed with code $status"
                Log.e(TAG, initError ?: "Unknown error")
            }
        }
    }

    fun getAvailableVoices(): List<Voice> {
        return try {
            tts?.voices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun synthesizeToFile(
        text: String,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f,
        voiceName: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!isInitialized || tts == null) {
            return@withContext Result.failure(IllegalStateException(initError ?: "TTS Engine is not ready yet"))
        }

        suspendCancellableCoroutine { continuation ->
            val audioDir = File(context.filesDir, "audios")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val outputFile = File(audioDir, "google_tts_${System.currentTimeMillis()}.wav")
            val utteranceId = "utterance_${System.currentTimeMillis()}"

            tts?.setSpeechRate(speechRate)
            tts?.setPitch(pitch)

            // Select matching voice if available
            if (!voiceName.isNullOrBlank()) {
                val voices = tts?.voices
                val matchingVoice = voices?.firstOrNull { it.name.contains(voiceName, ignoreCase = true) }
                if (matchingVoice != null) {
                    tts?.voice = matchingVoice
                }
            }

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS synthesis started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS synthesis completed: $utteranceId, size=${outputFile.length()} bytes")
                    if (continuation.isActive) {
                        if (outputFile.exists() && outputFile.length() > 0) {
                            continuation.resume(Result.success(outputFile))
                        } else {
                            continuation.resume(Result.failure(Exception("Synthesized audio file is empty or missing")))
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS synthesis failed for utterance: $utteranceId")
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("Native TTS synthesis failed")))
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(TAG, "TTS synthesis error code: $errorCode for utterance: $utteranceId")
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("Native TTS synthesis error: $errorCode")))
                    }
                }
            })

            val result = tts?.synthesizeToFile(text, params, outputFile, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(Exception("Failed to start TTS synthesis to file")))
                }
            }
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
