package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.api.AudioDataHelper
import com.example.data.api.GeminiTtsService
import com.example.data.local.AppDatabase
import com.example.data.local.ScriptEntity
import com.example.data.storage.AudioStorageManager
import com.example.data.tts.NativeTtsEngine
import com.example.data.tts.TtsEngineType
import com.example.data.tts.VoiceOption
import kotlinx.coroutines.flow.Flow
import java.io.File

class ScriptRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val scriptDao = db.scriptDao()
    private val geminiService = GeminiTtsService()
    private val nativeTtsEngine = NativeTtsEngine(context)

    val allScripts: Flow<List<ScriptEntity>> = scriptDao.getAllScripts()
    val audioRecordings: Flow<List<ScriptEntity>> = scriptDao.getAudioRecordings()

    fun searchScripts(query: String): Flow<List<ScriptEntity>> = scriptDao.searchScripts(query)

    suspend fun getScriptById(id: Int): ScriptEntity? = scriptDao.getScriptById(id)

    suspend fun saveScript(script: ScriptEntity): Long = scriptDao.insertScript(script)

    suspend fun updateScript(script: ScriptEntity) = scriptDao.updateScript(script)

    suspend fun deleteScript(script: ScriptEntity) {
        script.audioFilePath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (ignored: Exception) {}
        }
        scriptDao.deleteScript(script)
    }

    suspend fun deleteScriptById(id: Int) = scriptDao.deleteById(id)

    /**
     * Synthesize audio from text using either Google Gemini API (Puck, Charon, Kore, etc.)
     * or Google Offline TTS Engine.
     */
    suspend fun generateAudio(
        text: String,
        voiceOption: VoiceOption,
        customApiKey: String? = null,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f
    ): Result<File> {
        return if (voiceOption.engineType == TtsEngineType.GOOGLE_GEMINI_AI) {
            val apiResult = geminiService.generateSpeech(
                text = text,
                voiceName = voiceOption.id,
                customApiKey = customApiKey
            )
            apiResult.fold(
                onSuccess = { (base64Data, mimeType) ->
                    val file = AudioDataHelper.saveBase64AudioToFile(
                        context = context,
                        base64Data = base64Data,
                        mimeType = mimeType,
                        fileNamePrefix = "${voiceOption.id.lowercase()}_tts"
                    )
                    if (file != null && file.exists()) {
                        Result.success(file)
                    } else {
                        Result.failure(Exception("Failed to decode and save audio data"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } else {
            // Use Native Google TTS Engine
            nativeTtsEngine.synthesizeToFile(
                text = text,
                speechRate = speechRate,
                pitch = pitch,
                voiceName = voiceOption.id
            )
        }
    }

    /**
     * Save generated audio directly to user's phone storage (Music/PuckTTS)
     */
    suspend fun saveAudioToPhoneStorage(sourceFile: File, title: String): Result<Uri> {
        return AudioStorageManager.saveToPhoneStorage(context, sourceFile, title)
    }

    fun shareAudioFile(audioFile: File, title: String) {
        AudioStorageManager.shareAudio(context, audioFile, title)
    }

    fun openInExternalPlayer(audioFile: File) {
        AudioStorageManager.openInExternalPlayer(context, audioFile)
    }

    fun cleanup() {
        nativeTtsEngine.shutdown()
    }
}
