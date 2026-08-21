package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateSpeech(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateAudioRequest
    ): GenerateAudioResponse
}

class GeminiTtsService {

    companion object {
        private const val TAG = "GeminiTtsService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"
        const val MODEL_TTS_PRIMARY = "gemini-2.5-flash-preview-tts"
        const val MODEL_TTS_FALLBACK = "gemini-2.5-flash"
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service = retrofit.create(GeminiApiService::class.java)

    /**
     * Synthesize speech using Google Gemini Audio Generation API.
     * Voice options: Puck, Charon, Kore, Fenrir, Aoede
     */
    suspend fun generateSpeech(
        text: String,
        voiceName: String = "Puck",
        customApiKey: String? = null
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) {
            customApiKey.trim()
        } else {
            BuildConfig.GEMINI_API_KEY
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please set your API key in settings or Secrets panel.")
            )
        }

        val request = GenerateAudioRequest(
            contents = listOf(
                RequestContent(
                    parts = listOf(
                        RequestPart(text = "Please read the following text aloud with natural tone and clear pronunciation:\n\n$text")
                    )
                )
            ),
            generationConfig = AudioGenerationConfig(
                responseModalities = listOf("AUDIO"),
                speechConfig = AudioSpeechConfig(
                    voiceConfig = AudioVoiceConfig(
                        prebuiltVoiceConfig = AudioPrebuiltVoiceConfig(voiceName = voiceName)
                    )
                )
            )
        )

        // Try primary model first, then fallback
        val modelsToTry = listOf(MODEL_TTS_PRIMARY, MODEL_TTS_FALLBACK)
        var lastError: Exception? = null

        for (model in modelsToTry) {
            try {
                Log.d(TAG, "Calling Gemini TTS model: $model with voice: $voiceName")
                val response = service.generateSpeech(model, apiKey, request)
                
                val inlineData = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull { it.inlineData != null }
                    ?.inlineData

                if (inlineData != null && inlineData.data.isNotBlank()) {
                    Log.d(TAG, "Successfully received audio with mimeType: ${inlineData.mimeType}")
                    return@withContext Result.success(Pair(inlineData.data, inlineData.mimeType))
                } else if (response.error != null) {
                    val errMsg = response.error.message ?: "Unknown API error"
                    Log.e(TAG, "API error from $model: $errMsg")
                    lastError = Exception(errMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Request to $model failed: ${e.message}", e)
                lastError = e
            }
        }

        Result.failure(lastError ?: Exception("Failed to generate audio from Gemini API"))
    }
}
