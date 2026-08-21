package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.player.PlaybackState
import com.example.ui.MainUiState
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.VoiceSelector
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PuckElectricIndigo
import com.example.ui.theme.PuckPrimary
import com.example.ui.theme.PuckSecondary

@Composable
fun StudioScreen(
    uiState: MainUiState,
    playbackState: PlaybackState,
    onTextChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onVoiceSelected: (com.example.data.tts.VoiceOption) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onGenerateAudio: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayerSpeedChange: (Float) -> Unit,
    onSaveToStorage: () -> Unit,
    onShareAudio: () -> Unit,
    onOpenExternal: () -> Unit,
    onOpenTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAdvancedSettings by remember { mutableStateOf(false) }

    val wordCount = if (uiState.inputText.isBlank()) 0 else uiState.inputText.trim().split(Regex("\\s+")).size
    val charCount = uiState.inputText.length
    val estimatedDurationSecs = (wordCount / 2.5f).toInt() // Average speaking speed ~150 wpm

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Script Title input
            OutlinedTextField(
                value = uiState.scriptTitle,
                onValueChange = onTitleChange,
                label = { Text("Title (অডিওর শিরোনাম)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Title, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("script_title_input")
            )
        }

        item {
            // Main Text Input Area with Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enter Text to Speak (টেক্সট লিখুন)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Sample Templates
                            TextButton(
                                onClick = onOpenTemplates,
                                modifier = Modifier.testTag("open_templates_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = PuckPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Templates", fontSize = 12.sp, color = PuckPrimary)
                            }

                            // Paste from Clipboard
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val item = clipboard.primaryClip?.getItemAt(0)
                                    val pasteText = item?.text?.toString() ?: ""
                                    if (pasteText.isNotBlank()) {
                                        onTextChange(uiState.inputText + if (uiState.inputText.isNotBlank()) "\n" + pasteText else pasteText)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Clear
                            if (uiState.inputText.isNotEmpty()) {
                                IconButton(
                                    onClick = { onTextChange("") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = onTextChange,
                        placeholder = {
                            Text("Type or paste any text or script here to generate crystal-clear speech with Google Puck voice...")
                        },
                        minLines = 5,
                        maxLines = 10,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_text_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stats: Word count, character count, estimated time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "$wordCount words",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$charCount chars",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Est. duration: ~${estimatedDurationSecs}s",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = PuckPrimary
                        )
                    }
                }
            }
        }

        item {
            // Voice Selector Component
            VoiceSelector(
                selectedVoice = uiState.selectedVoice,
                onVoiceSelected = onVoiceSelected
            )
        }

        item {
            // Advanced Tuning (Speed & Pitch)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedSettings = !showAdvancedSettings },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Audio Speed & Pitch Tuning (গতি ও পিচ)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (showAdvancedSettings) "Hide ▲" else "Adjust ▼",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PuckPrimary
                        )
                    }

                    AnimatedVisibility(visible = showAdvancedSettings) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            // Speech Rate
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Speech Rate (গতি): ${String.format("%.2f", uiState.speechRate)}x",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (uiState.speechRate != 1.0f) {
                                    Text(
                                        text = "Reset",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PuckPrimary,
                                        modifier = Modifier.clickable { onSpeechRateChange(1.0f) }
                                    )
                                }
                            }
                            Slider(
                                value = uiState.speechRate,
                                onValueChange = onSpeechRateChange,
                                valueRange = 0.5f..2.0f,
                                steps = 14,
                                colors = SliderDefaults.colors(thumbColor = PuckPrimary, activeTrackColor = PuckPrimary)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Pitch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Voice Pitch (পিচ): ${String.format("%.2f", uiState.pitch)}x",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (uiState.pitch != 1.0f) {
                                    Text(
                                        text = "Reset",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PuckPrimary,
                                        modifier = Modifier.clickable { onPitchChange(1.0f) }
                                    )
                                }
                            }
                            Slider(
                                value = uiState.pitch,
                                onValueChange = onPitchChange,
                                valueRange = 0.5f..2.0f,
                                steps = 14,
                                colors = SliderDefaults.colors(thumbColor = PuckSecondary, activeTrackColor = PuckSecondary)
                            )
                        }
                    }
                }
            }
        }

        // Error message banner if any
        if (uiState.generationError != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.generationError,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed
                        )
                    }
                }
            }
        }

        item {
            // Main Generate Action Button
            Button(
                onClick = onGenerateAudio,
                enabled = !uiState.isGeneratingAudio && uiState.inputText.isNotBlank(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (uiState.inputText.isNotBlank() && !uiState.isGeneratingAudio) {
                            Brush.horizontalGradient(
                                listOf(PuckPrimary, PuckElectricIndigo, PuckSecondary)
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    )
                    .testTag("generate_audio_button")
            ) {
                if (uiState.isGeneratingAudio) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Generating Audio with ${uiState.selectedVoice.id}...",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = if (uiState.inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Audio with ${uiState.selectedVoice.id} (অডিও তৈরি করুন)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (uiState.inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Generated Player Card
        if (uiState.currentGeneratedFile != null && uiState.currentGeneratedFile.exists()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Generated Speech Output (তৈরি হওয়া অডিও)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                AudioPlayerCard(
                    audioFile = uiState.currentGeneratedFile,
                    title = uiState.scriptTitle,
                    voiceName = uiState.selectedVoice.name,
                    playbackState = playbackState,
                    isSavedToStorage = uiState.isCurrentAudioSavedToStorage,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekTo = onSeekTo,
                    onSpeedChange = onPlayerSpeedChange,
                    onSaveToStorage = onSaveToStorage,
                    onShare = onShareAudio,
                    onOpenExternal = onOpenExternal
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
