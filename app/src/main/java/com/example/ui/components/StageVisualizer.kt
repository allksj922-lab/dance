package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.model.BeatTelemetry
import com.example.model.DanceTrack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.SunriseAmber

@Composable
fun StageVisualizer(
    track: DanceTrack,
    telemetry: BeatTelemetry,
    grooveEnergy: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylAngle"
    )

    val beatPulse = if (telemetry.isPlaying) {
        1f + (1f - telemetry.beatPhase) * 0.06f + grooveEnergy * 0.04f
    } else {
        1f
    }
    val discScale by animateFloatAsState(
        targetValue = beatPulse,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "discScale"
    )

    val accent = track.genre.accentHex
    val secondary = track.genre.secondaryHex

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rotating glowing vinyl disc + expanding beat rings
        Box(
            modifier = Modifier
                .size(210.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.36f
                if (telemetry.isPlaying) {
                    val ring1Progress = telemetry.beatPhase
                    val ring2Progress = (telemetry.beatPhase + 0.5f) % 1f
                    drawCircle(
                        color = accent.copy(alpha = (1f - ring1Progress) * 0.45f),
                        radius = baseRadius + ring1Progress * (size.minDimension * 0.14f),
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCircle(
                        color = secondary.copy(alpha = (1f - ring2Progress) * 0.32f),
                        radius = baseRadius + ring2Progress * (size.minDimension * 0.14f),
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                } else {
                    drawCircle(
                        color = accent.copy(alpha = 0.22f),
                        radius = baseRadius + 10.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(152.dp)
                    .scale(discScale)
                    .rotate(if (telemetry.isPlaying) spinAngle else 0f)
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(listOf(accent, secondary, ElectricCyan, accent)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = track.coverResId),
                    contentDescription = "${track.title} 앨범 커버",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Center vinyl spindle hole
                Surface(
                    modifier = Modifier.size(26.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.7f))
                ) {}
            }

            // Section badge floating on top-right
            if (telemetry.isPlaying) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp),
                    shape = CircleShape,
                    color = accent
                ) {
                    Text(
                        text = telemetry.section.labelEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 16-bar live spectrum visualizer
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 8.dp)
        ) {
            val bars = telemetry.spectrumBars
            val count = bars.size.coerceAtLeast(1)
            val gap = 6.dp.toPx()
            val totalGap = gap * (count - 1)
            val barWidth = ((size.width - totalGap) / count).coerceAtLeast(4f)
            val maxH = size.height

            bars.forEachIndexed { idx, rawLevel ->
                val level = if (telemetry.isPlaying) rawLevel else 0.14f + (idx % 3) * 0.05f
                val barH = (maxH * level).coerceAtLeast(8f)
                val x = idx * (barWidth + gap)
                val y = (maxH - barH) / 2f
                val t = idx.toFloat() / (count - 1).coerceAtLeast(1)
                val barColor = when {
                    t < 0.35f -> NeonCoral
                    t < 0.7f -> SunriseAmber
                    else -> ElectricCyan
                }
                drawRoundRect(
                    color = barColor.copy(alpha = if (telemetry.isPlaying) 0.95f else 0.4f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (telemetry.isPlaying) "LIVE SYNTH · ${telemetry.section.labelKo}" else "READY TO DANCE",
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
            Text(
                text = "${track.bpm} BPM · ${track.keyLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
