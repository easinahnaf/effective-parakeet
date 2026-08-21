package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.AudioDataHelper
import com.example.data.local.ScriptEntity
import com.example.data.player.AppAudioPlayer
import com.example.data.player.PlaybackState
import com.example.data.repository.ScriptRepository
import com.example.data.tts.VoiceOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScriptRepository(application)
    val audioPlayer = AppAudioPlayer(application)

    private val prefs = application.getSharedPreferences("puck_tts_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        MainUiState(
            customApiKey = prefs.getString("custom_gemini_api_key", "") ?: "",
            inputText = "Hello! I am Puck, your advanced Google AI voice assistant. Type or edit your text offline and convert it into high-fidelity speech instantly!"
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val allScripts: StateFlow<List<ScriptEntity>> = repository.allScripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioRecordings: StateFlow<List<ScriptEntity>> = repository.audioRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackState> = audioPlayer.playbackState

    fun setTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, generationError = null)
    }

    fun updateScriptTitle(title: String) {
        _uiState.value = _uiState.value.copy(scriptTitle = title)
    }

    fun selectVoice(voice: VoiceOption) {
        _uiState.value = _uiState.value.copy(selectedVoice = voice)
    }

    fun updateSpeechRate(rate: Float) {
        _uiState.value = _uiState.value.copy(speechRate = rate)
    }

    fun updatePitch(pitch: Float) {
        _uiState.value = _uiState.value.copy(pitch = pitch)
    }

    /**
     * Generate Audio using Google Puck / Gemini TTS or Google Engine
     */
    fun generateAudio() {
        val currentState = _uiState.value
        val text = currentState.inputText.trim()

        if (text.isBlank()) {
            _uiState.value = currentState.copy(generationError = "Please enter some text to generate audio.")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isGeneratingAudio = true,
                generationError = null
            )

            val customKey = currentState.customApiKey.ifBlank { null }
            val result = repository.generateAudio(
                text = text,
                voiceOption = currentState.selectedVoice,
                customApiKey = customKey,
                speechRate = currentState.speechRate,
                pitch = currentState.pitch
            )

            result.fold(
                onSuccess = { audioFile ->
                    val durationMs = AudioDataHelper.getAudioDurationMs(audioFile.absolutePath)
                    
                    // Save record in Room DB
                    val scriptEntity = ScriptEntity(
                        title = currentState.scriptTitle.ifBlank { "Speech by ${currentState.selectedVoice.id}" },
                        content = text,
                        category = currentState.selectedCategory,
                        voiceName = currentState.selectedVoice.name,
                        audioFilePath = audioFile.absolutePath,
                        audioDurationMs = durationMs,
                        isSavedToStorage = false
                    )
                    repository.saveScript(scriptEntity)

                    _uiState.value = _uiState.value.copy(
                        isGeneratingAudio = false,
                        currentGeneratedFile = audioFile,
                        currentGeneratedDurationMs = durationMs,
                        isCurrentAudioSavedToStorage = false,
                        successSnackbarMessage = "Audio generated successfully with ${currentState.selectedVoice.id}!"
                    )

                    // Automatically start playback
                    audioPlayer.playFile(audioFile.absolutePath, currentState.scriptTitle)
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Failed to generate speech."
                    _uiState.value = _uiState.value.copy(
                        isGeneratingAudio = false,
                        generationError = errorMsg
                    )
                }
            )
        }
    }

    /**
     * Save generated audio to Phone Storage (Music / PuckTTS)
     */
    fun saveCurrentAudioToDeviceStorage(customFileName: String? = null) {
        val audioFile = _uiState.value.currentGeneratedFile ?: return
        val title = customFileName?.ifBlank { null } ?: _uiState.value.scriptTitle.ifBlank { "Puck_Audio" }

        viewModelScope.launch {
            val result = repository.saveAudioToPhoneStorage(audioFile, title)
            result.fold(
                onSuccess = { uri ->
                    _uiState.value = _uiState.value.copy(
                        isCurrentAudioSavedToStorage = true,
                        showSaveStorageDialog = false,
                        successSnackbarMessage = "Saved directly to Phone Storage (Music/PuckTTS)!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        showSaveStorageDialog = false,
                        errorSnackbarMessage = "Could not save to storage: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Save specific audio file to Phone Storage
     */
    fun saveAudioRecordToStorage(script: ScriptEntity) {
        val filePath = script.audioFilePath ?: return
        val file = File(filePath)
        if (!file.exists()) {
            _uiState.value = _uiState.value.copy(errorSnackbarMessage = "Audio file no longer exists")
            return
        }

        viewModelScope.launch {
            val result = repository.saveAudioToPhoneStorage(file, script.title)
            result.fold(
                onSuccess = {
                    repository.updateScript(script.copy(isSavedToStorage = true))
                    _uiState.value = _uiState.value.copy(
                        successSnackbarMessage = "Saved '${script.title}' to Phone Storage!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorSnackbarMessage = "Failed to save: ${error.message}"
                    )
                }
            )
        }
    }

    fun shareCurrentAudio() {
        val audioFile = _uiState.value.currentGeneratedFile ?: return
        repository.shareAudioFile(audioFile, _uiState.value.scriptTitle)
    }

    fun shareScriptAudio(script: ScriptEntity) {
        val path = script.audioFilePath ?: return
        val file = File(path)
        if (file.exists()) {
            repository.shareAudioFile(file, script.title)
        }
    }

    // --- Offline Editor Actions ---

    fun startNewEditorScript() {
        _uiState.value = _uiState.value.copy(
            editingScriptId = null,
            editorTitle = "",
            editorContent = "",
            editorCategory = "Script",
            isAutoSaved = true
        )
    }

    fun loadScriptIntoEditor(script: ScriptEntity) {
        _uiState.value = _uiState.value.copy(
            editingScriptId = script.id,
            editorTitle = script.title,
            editorContent = script.content,
            editorCategory = script.category,
            isAutoSaved = true,
            currentTab = AppTab.OFFLINE_EDITOR
        )
    }

    fun updateEditorTitle(title: String) {
        _uiState.value = _uiState.value.copy(editorTitle = title, isAutoSaved = false)
        autoSaveEditorDraft()
    }

    fun updateEditorContent(content: String) {
        _uiState.value = _uiState.value.copy(editorContent = content, isAutoSaved = false)
        autoSaveEditorDraft()
    }

    fun updateEditorCategory(category: String) {
        _uiState.value = _uiState.value.copy(editorCategory = category, isAutoSaved = false)
        autoSaveEditorDraft()
    }

    private fun autoSaveEditorDraft() {
        val title = _uiState.value.editorTitle.trim()
        val content = _uiState.value.editorContent.trim()
        if (title.isBlank() && content.isBlank()) return

        viewModelScope.launch {
            val scriptId = _uiState.value.editingScriptId
            val existing = if (scriptId != null) repository.getScriptById(scriptId) else null

            val scriptToSave = ScriptEntity(
                id = scriptId ?: 0,
                title = title.ifBlank { "Untitled Script" },
                content = content,
                category = _uiState.value.editorCategory,
                voiceName = existing?.voiceName ?: "Puck",
                audioFilePath = existing?.audioFilePath,
                audioDurationMs = existing?.audioDurationMs ?: 0L,
                isSavedToStorage = existing?.isSavedToStorage ?: false,
                isFavorite = existing?.isFavorite ?: false,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val savedId = repository.saveScript(scriptToSave)
            if (scriptId == null) {
                _uiState.value = _uiState.value.copy(
                    editingScriptId = savedId.toInt(),
                    isAutoSaved = true
                )
            } else {
                _uiState.value = _uiState.value.copy(isAutoSaved = true)
            }
        }
    }

    fun sendEditorTextToStudio() {
        val content = _uiState.value.editorContent
        val title = _uiState.value.editorTitle
        if (content.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                inputText = content,
                scriptTitle = title.ifBlank { "Speech Script" },
                currentTab = AppTab.STUDIO
            )
        }
    }

    fun deleteScript(script: ScriptEntity) {
        viewModelScope.launch {
            repository.deleteScript(script)
            if (_uiState.value.editingScriptId == script.id) {
                startNewEditorScript()
            }
            _uiState.value = _uiState.value.copy(successSnackbarMessage = "Script deleted")
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateCategoryFilter(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryFilter = category)
    }

    // --- API Key & Settings ---

    fun showApiKeyDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showApiKeyDialog = show)
    }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
        _uiState.value = _uiState.value.copy(
            customApiKey = key.trim(),
            showApiKeyDialog = false,
            successSnackbarMessage = "API Key updated successfully!"
        )
    }

    fun showSaveStorageDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showSaveStorageDialog = show,
            saveStorageFileName = _uiState.value.scriptTitle
        )
    }

    fun updateSaveStorageFileName(name: String) {
        _uiState.value = _uiState.value.copy(saveStorageFileName = name)
    }

    fun showTemplatesDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showTemplatesDialog = show)
    }

    fun insertTemplate(text: String, title: String) {
        _uiState.value = _uiState.value.copy(
            inputText = text,
            scriptTitle = title,
            showTemplatesDialog = false
        )
    }

    fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(
            successSnackbarMessage = null,
            errorSnackbarMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        repository.cleanup()
    }
}
