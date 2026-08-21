package com.example.ui

import com.example.data.local.ScriptEntity
import com.example.data.player.PlaybackState
import com.example.data.tts.VoiceOption
import java.io.File

enum class AppTab(val titleEn: String, val titleBn: String) {
    STUDIO("Studio", "স্টুডিও"),
    OFFLINE_EDITOR("Offline Editor", "অফলাইন এডিটর"),
    LIBRARY("Library", "অডিও লাইব্রেরি")
}

data class MainUiState(
    val currentTab: AppTab = AppTab.STUDIO,
    
    // Studio TTS State
    val inputText: String = "",
    val scriptTitle: String = "My Speech",
    val selectedCategory: String = "General",
    val selectedVoice: VoiceOption = VoiceOption.ALL_VOICES.first { it.id == "Puck" },
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val isGeneratingAudio: Boolean = false,
    val generationError: String? = null,
    val currentGeneratedFile: File? = null,
    val currentGeneratedDurationMs: Long = 0L,
    val isCurrentAudioSavedToStorage: Boolean = false,
    
    // Offline Editor State
    val editingScriptId: Int? = null,
    val editorTitle: String = "",
    val editorContent: String = "",
    val editorCategory: String = "General",
    val searchQuery: String = "",
    val selectedCategoryFilter: String = "All",
    val isAutoSaved: Boolean = true,
    
    // Dialogs & Modals
    val showApiKeyDialog: Boolean = false,
    val customApiKey: String = "",
    val showSaveStorageDialog: Boolean = false,
    val saveStorageFileName: String = "",
    val showTemplatesDialog: Boolean = false,
    val successSnackbarMessage: String? = null,
    val errorSnackbarMessage: String? = null
)
