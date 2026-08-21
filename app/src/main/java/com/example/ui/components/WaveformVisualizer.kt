package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AudioWaveActive
import com.example.ui.theme.PuckElectricIndigo
import com.example.ui.theme.PuckPrimary
import com.example.ui.theme.PuckSecondary

@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    amplitudes: List<Float> = emptyList(),
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    height: Dp = 48.dp,
    barWidth: Dp = 3.dp,
    activeColor: Color = AudioWaveActive
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bars = if (amplitudes.isNotEmpty()) amplitudes else List(barCount) { 0.2f }

        for (i in 0 until barCount) {
            val amp = if (i < bars.size) bars[i] else 0.2f
            
            // Calculate height fraction
            val heightFraction = if (isPlaying) {
                val waveFactor = kotlin.math.sin((i / barCount.toFloat() + animatedPhase) * Math.PI * 2).toFloat()
                (amp * 0.7f + (waveFactor * 0.3f + 0.3f) * 0.3f).coerceIn(0.15f, 1.0f)
            } else {
                (amp * 0.5f).coerceIn(0.12f, 0.4f)
            }

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isPlaying) {
                            Brush.verticalGradient(
                                listOf(PuckSecondary, PuckElectricIndigo, PuckPrimary)
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    activeColor.copy(alpha = 0.4f),
                                    activeColor.copy(alpha = 0.2f)
                                )
                            )
                        }
                    )
            )
        }
    }
}
