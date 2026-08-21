package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.HeaderBar
import com.example.ui.components.SaveAudioDialog
import com.example.ui.components.TemplateDialog
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OfflineEditorScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PuckElectricIndigo
import com.example.ui.theme.PuckPrimary
import com.example.ui.theme.PuckSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PuckTtsApp()
            }
        }
    }
}

@Composable
fun PuckTtsApp(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allScripts by viewModel.allScripts.collectAsStateWithLifecycle()
    val audioRecordings by viewModel.audioRecordings.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar messages
    LaunchedEffect(uiState.successSnackbarMessage, uiState.errorSnackbarMessage) {
        uiState.successSnackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
        uiState.errorSnackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            HeaderBar(
                onApiKeyClick = { viewModel.showApiKeyDialog(true) },
                hasCustomApiKey = uiState.customApiKey.isNotBlank()
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Studio Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.STUDIO,
                    onClick = { viewModel.setTab(AppTab.STUDIO) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.STUDIO) Icons.Default.RecordVoiceOver else Icons.Outlined.RecordVoiceOver,
                            contentDescription = "Studio",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Studio (স্টুডিও)",
                            fontWeight = if (uiState.currentTab == AppTab.STUDIO) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PuckPrimary,
                        selectedTextColor = PuckPrimary,
                        indicatorColor = PuckPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_studio")
                )

                // Offline Editor Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.OFFLINE_EDITOR,
                    onClick = { viewModel.setTab(AppTab.OFFLINE_EDITOR) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (allScripts.isNotEmpty()) {
                                    Badge(
                                        containerColor = PuckElectricIndigo,
                                        contentColor = Color.White
                                    ) {
                                        Text("${allScripts.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.currentTab == AppTab.OFFLINE_EDITOR) Icons.Default.EditNote else Icons.Default.Description,
                                contentDescription = "Offline Editor",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Editor (এডিটর)",
                            fontWeight = if (uiState.currentTab == AppTab.OFFLINE_EDITOR) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PuckPrimary,
                        selectedTextColor = PuckPrimary,
                        indicatorColor = PuckPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_editor")
                )

                // Audio Library Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.LIBRARY,
                    onClick = { viewModel.setTab(AppTab.LIBRARY) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (audioRecordings.isNotEmpty()) {
                                    Badge(
                                        containerColor = PuckSecondary,
                                        contentColor = Color.Black
                                    ) {
                                        Text("${audioRecordings.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.currentTab == AppTab.LIBRARY) Icons.Default.LibraryMusic else Icons.Outlined.LibraryMusic,
                                contentDescription = "Audio Library",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Audio Files (অডিও)",
                            fontWeight = if (uiState.currentTab == AppTab.LIBRARY) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PuckSecondary,
                        selectedTextColor = PuckSecondary,
                        indicatorColor = PuckSecondary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_library")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = uiState.currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    AppTab.STUDIO -> {
                        StudioScreen(
                            uiState = uiState,
                            playbackState = playbackState,
                            onTextChange = { viewModel.updateInputText(it) },
                            onTitleChange = { viewModel.updateScriptTitle(it) },
                            onVoiceSelected = { viewModel.selectVoice(it) },
                            onSpeechRateChange = { viewModel.updateSpeechRate(it) },
                            onPitchChange = { viewModel.updatePitch(it) },
                            onGenerateAudio = { viewModel.generateAudio() },
                            onTogglePlayPause = {
                                uiState.currentGeneratedFile?.let { file ->
                                    viewModel.audioPlayer.togglePlayPause(file.absolutePath, uiState.scriptTitle)
                                }
                            },
                            onSeekTo = { viewModel.audioPlayer.seekTo(it) },
                            onPlayerSpeedChange = { viewModel.audioPlayer.setSpeed(it) },
                            onSaveToStorage = { viewModel.showSaveStorageDialog(true) },
                            onShareAudio = { viewModel.shareCurrentAudio() },
                            onOpenExternal = {
                                uiState.currentGeneratedFile?.let { file ->
                                    viewModel.audioPlayer.playFile(file.absolutePath)
                                }
                            },
                            onOpenTemplates = { viewModel.showTemplatesDialog(true) }
                        )
                    }

                    AppTab.OFFLINE_EDITOR -> {
                        OfflineEditorScreen(
                            uiState = uiState,
                            scripts = allScripts,
                            onTitleChange = { viewModel.updateEditorTitle(it) },
                            onContentChange = { viewModel.updateEditorContent(it) },
                            onCategoryChange = { viewModel.updateEditorCategory(it) },
                            onNewScript = { viewModel.startNewEditorScript() },
                            onLoadScript = { viewModel.loadScriptIntoEditor(it) },
                            onDeleteScript = { viewModel.deleteScript(it) },
                            onSendToStudio = { viewModel.sendEditorTextToStudio() },
                            onSearchChange = { viewModel.updateSearchQuery(it) },
                            onCategoryFilterChange = { viewModel.updateCategoryFilter(it) }
                        )
                    }

                    AppTab.LIBRARY -> {
                        LibraryScreen(
                            audioRecordings = audioRecordings,
                            playbackState = playbackState,
                            onTogglePlayPause = { filePath, title ->
                                viewModel.audioPlayer.togglePlayPause(filePath, title)
                            },
                            onSaveToStorage = { viewModel.saveAudioRecordToStorage(it) },
                            onShareAudio = { viewModel.shareScriptAudio(it) },
                            onDeleteAudio = { viewModel.deleteScript(it) }
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (uiState.showSaveStorageDialog) {
        SaveAudioDialog(
            fileName = uiState.saveStorageFileName,
            onFileNameChange = { viewModel.updateSaveStorageFileName(it) },
            onConfirm = { viewModel.saveCurrentAudioToDeviceStorage(uiState.saveStorageFileName) },
            onDismiss = { viewModel.showSaveStorageDialog(false) }
        )
    }

    if (uiState.showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = uiState.customApiKey,
            onSaveKey = { viewModel.saveCustomApiKey(it) },
            onDismiss = { viewModel.showApiKeyDialog(false) }
        )
    }

    if (uiState.showTemplatesDialog) {
        TemplateDialog(
            onSelectTemplate = { text, title ->
                viewModel.insertTemplate(text, title)
            },
            onDismiss = { viewModel.showTemplatesDialog(false) }
        )
    }
}
