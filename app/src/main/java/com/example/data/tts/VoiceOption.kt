package com.example.data.tts

enum class TtsEngineType(val displayName: String, val isOnline: Boolean) {
    GOOGLE_GEMINI_AI("Google Gemini AI (Studio)", true),
    GOOGLE_SYSTEM_TTS("Google Offline Engine", false)
}

data class VoiceOption(
    val id: String,
    val name: String,
    val description: String,
    val gender: String,
    val engineType: TtsEngineType,
    val isFeatured: Boolean = false
) {
    companion object {
        val ALL_VOICES = listOf(
            VoiceOption(
                id = "Puck",
                name = "Puck (Google AI)",
                description = "Playful, expressive, crisp & energetic tone",
                gender = "Male",
                engineType = TtsEngineType.GOOGLE_GEMINI_AI,
                isFeatured = true
            ),
            VoiceOption(
                id = "Charon",
                name = "Charon (Google AI)",
                description = "Deep, resonant, authoritative & narrative",
                gender = "Male",
                engineType = TtsEngineType.GOOGLE_GEMINI_AI
            ),
            VoiceOption(
                id = "Kore",
                name = "Kore (Google AI)",
                description = "Warm, gentle, soothing & conversational",
                gender = "Female",
                engineType = TtsEngineType.GOOGLE_GEMINI_AI
            ),
            VoiceOption(
                id = "Fenrir",
                name = "Fenrir (Google AI)",
                description = "Dynamic, strong, modern & engaging",
                gender = "Male",
                engineType = TtsEngineType.GOOGLE_GEMINI_AI
            ),
            VoiceOption(
                id = "Aoede",
                name = "Aoede (Google AI)",
                description = "Melodic, calm, natural & friendly",
                gender = "Female",
                engineType = TtsEngineType.GOOGLE_GEMINI_AI
            ),
            VoiceOption(
                id = "offline_google_default",
                name = "Google Engine (Offline)",
                description = "High-speed offline synthesis without internet",
                gender = "Neutral",
                engineType = TtsEngineType.GOOGLE_SYSTEM_TTS
            )
        )
    }
}
