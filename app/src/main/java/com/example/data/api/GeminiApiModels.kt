package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateAudioRequest(
    @Json(name = "contents") val contents: List<RequestContent>,
    @Json(name = "generationConfig") val generationConfig: AudioGenerationConfig
)

@JsonClass(generateAdapter = true)
data class RequestContent(
    @Json(name = "parts") val parts: List<RequestPart>
)

@JsonClass(generateAdapter = true)
data class RequestPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class AudioGenerationConfig(
    @Json(name = "responseModalities") val responseModalities: List<String> = listOf("AUDIO"),
    @Json(name = "speechConfig") val speechConfig: AudioSpeechConfig
)

@JsonClass(generateAdapter = true)
data class AudioSpeechConfig(
    @Json(name = "voiceConfig") val voiceConfig: AudioVoiceConfig
)

@JsonClass(generateAdapter = true)
data class AudioVoiceConfig(
    @Json(name = "prebuiltVoiceConfig") val prebuiltVoiceConfig: AudioPrebuiltVoiceConfig
)

@JsonClass(generateAdapter = true)
data class AudioPrebuiltVoiceConfig(
    @Json(name = "voiceName") val voiceName: String
)

@JsonClass(generateAdapter = true)
data class GenerateAudioResponse(
    @Json(name = "candidates") val candidates: List<ResponseCandidate>? = null,
    @Json(name = "error") val error: ApiErrorDetail? = null
)

@JsonClass(generateAdapter = true)
data class ResponseCandidate(
    @Json(name = "content") val content: ResponseContent? = null
)

@JsonClass(generateAdapter = true)
data class ResponseContent(
    @Json(name = "parts") val parts: List<ResponsePart>? = null
)

@JsonClass(generateAdapter = true)
data class ResponsePart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: ResponseInlineData? = null
)

@JsonClass(generateAdapter = true)
data class ResponseInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class ApiErrorDetail(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)
