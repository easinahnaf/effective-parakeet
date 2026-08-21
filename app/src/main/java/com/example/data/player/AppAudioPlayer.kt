package com.example.data.player

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val currentFilePath: String? = null,
    val currentTitle: String = "",
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val speed: Float = 1.0f,
    val amplitudes: List<Float> = List(30) { 0.15f }
)

class AppAudioPlayer(private val context: Context) {

    companion object {
        private const val TAG = "AppAudioPlayer"
    }

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun playFile(filePath: String, title: String = "Puck TTS Audio") {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: $filePath")
            return
        }

        try {
            if (_playbackState.value.currentFilePath == filePath && mediaPlayer != null) {
                if (!_playbackState.value.isPlaying) {
                    mediaPlayer?.start()
                    _playbackState.value = _playbackState.value.copy(isPlaying = true)
                    startProgressTracker()
                }
                return
            }

            stop()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setSpeedInternal(_playbackState.value.speed)
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = _playbackState.value.totalDurationMs
                    )
                    progressJob?.cancel()
                }
                start()
            }

            val duration = mediaPlayer?.duration?.toLong() ?: 0L
            _playbackState.value = _playbackState.value.copy(
                currentFilePath = filePath,
                currentTitle = title,
                isPlaying = true,
                currentPositionMs = 0L,
                totalDurationMs = duration
            )

            startProgressTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                    progressJob?.cancel()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio", e)
        }
    }

    fun togglePlayPause(filePath: String, title: String = "") {
        if (_playbackState.value.currentFilePath == filePath && _playbackState.value.isPlaying) {
            pause()
        } else {
            playFile(filePath, if (title.isNotBlank()) title else _playbackState.value.currentTitle)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio", e)
        }
    }

    fun setSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(speed = speed)
        setSpeedInternal(speed)
    }

    private fun setSpeedInternal(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.playbackParams = player.playbackParams.setSpeed(speed)
                    } else {
                        val params = PlaybackParams().setSpeed(speed)
                        player.playbackParams = params
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting playback speed", e)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                currentPositionMs = 0L
            )
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
                val total = mediaPlayer?.duration?.toLong() ?: _playbackState.value.totalDurationMs

                // Generate active wave animation
                val progressRatio = if (total > 0) current.toFloat() / total else 0f
                val simulatedAmplitudes = List(30) { index ->
                    val wavePos = (index / 30f + progressRatio * 3f) % 1f
                    (0.2f + 0.8f * kotlin.math.sin(wavePos * Math.PI.toFloat()).coerceAtLeast(0.1f))
                }

                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = current,
                    totalDurationMs = total,
                    amplitudes = simulatedAmplitudes
                )
                delay(100)
            }
        }
    }

    fun release() {
        stop()
    }
}
