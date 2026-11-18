package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DanceTrack
import com.example.ui.MorningDanceUiState
import com.example.ui.components.StageVisualizer
import com.example.ui.theme.AcidLime
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.SunriseAmber

@Composable
fun TodayStageScreen(
    uiState: MorningDanceUiState,
    onTogglePlayPause: () -> Unit,
    onReturnToToday: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onRoutineMinutesChange: (Int) -> Unit,
    onBeatTap: () -> Unit,
    onResetSession: () -> Unit,
    onDismissCelebration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = uiState.selectedTrack
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 720.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1040.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Celebratory Morning Dance Goal Banner
            AnimatedVisibility(
                visible = uiState.showGoalCelebration,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                CelebrationBanner(
                    routineMinutes = uiState.alarmSettings.routineMinutes,
                    movePoints = uiState.movePoints,
                    onDismiss = onDismissCelebration
                )
            }

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    StageHeroCard(
                        track = track,
                        uiState = uiState,
                        onTogglePlayPause = onTogglePlayPause,
                        onReturnToToday = onReturnToToday,
                        onVolumeChange = onVolumeChange,
                        modifier = Modifier.weight(1.1f)
                    )
                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RoutineAndGrooveCard(
                            track = track,
                            uiState = uiState,
                            onRoutineMinutesChange = onRoutineMinutesChange,
                            onBeatTap = onBeatTap,
                            onResetSession = onResetSession
                        )
                    }
                }
            } else {
                StageHeroCard(
                    track = track,
                    uiState = uiState,
                    onTogglePlayPause = onTogglePlayPause,
                    onReturnToToday = onReturnToToday,
                    onVolumeChange = onVolumeChange
                )
                RoutineAndGrooveCard(
                    track = track,
                    uiState = uiState,
                    onRoutineMinutesChange = onRoutineMinutesChange,
                    onBeatTap = onBeatTap,
                    onResetSession = onResetSession
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StageHeroCard(
    track: DanceTrack,
    uiState: MorningDanceUiState,
    onTogglePlayPause: () -> Unit,
    onReturnToToday: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying = uiState.telemetry.isPlaying && uiState.telemetry.currentTrackId == track.id
    val accent = track.genre.accentHex

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle sunrise backdrop artwork at top of stage card
            Image(
                painter = painterResource(id = R.drawable.img_hero_morning_stage),
                contentDescription = "모닝 댄스 스테이지 배경",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.42f
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Date & Auto-Change Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = SunriseAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${track.date.formattedKorean()} · DAY #${track.date.dayOfYear()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (uiState.isPreviewingOtherDay) {
                        FilledTonalButton(
                            onClick = onReturnToToday,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("return_today_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("오늘 곡으로", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "매일 새 음악",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                            }
                        }
                    }
                }

                // Live Vinyl & Spectrum Visualizer
                StageVisualizer(
                    track = track,
                    telemetry = if (isPlaying) uiState.telemetry else uiState.telemetry.copy(isPlaying = false),
                    grooveEnergy = uiState.grooveEnergy
                )

                // Track Info Block
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = track.genre.displayNameKo,
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(track.energyStars) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = SunriseAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Play / Pause CTA & Volume Control
                Button(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("play_pause_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.surfaceContainerHigh else accent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "일시정지" else "댄스 음악 재생",
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying) "잠깐 멈추기" else "모닝 댄스 시작하기!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "볼륨",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = uiState.alarmSettings.volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("volume_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Text(
                        text = "${(uiState.alarmSettings.volume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineAndGrooveCard(
    track: DanceTrack,
    uiState: MorningDanceUiState,
    onRoutineMinutesChange: (Int) -> Unit,
    onBeatTap: () -> Unit,
    onResetSession: () -> Unit
) {
    val dancedMin = uiState.sessionDancedSeconds / 60
    val dancedSec = uiState.sessionDancedSeconds % 60
    val goalMin = uiState.alarmSettings.routineMinutes

    val groovePercent = (uiState.grooveEnergy * 100).toInt().coerceIn(0, 100)
    val grooveEncouragement = when {
        !uiState.telemetry.isPlaying -> "재생 버튼을 누르고 가볍게 몸을 흔들어 보세요!"
        groovePercent >= 65 -> "최고의 모닝 피버! 에너지가 넘쳐요!"
        groovePercent >= 30 -> "리듬 딱 맞아요! 어깨와 무릎 바운스!"
        else -> track.danceCue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Morning Routine Timer Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                    Text(
                        text = "아침 댄스 루틴",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("%02d:%02d / %02d:00", dancedMin, dancedSec, goalMin),
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    if (uiState.sessionDancedSeconds > 0) {
                        IconButton(
                            onClick = onResetSession,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "타이머 초기화",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { uiState.routineProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = ElectricCyan,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            // Routine Duration Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(3, 5, 10).forEach { minutes ->
                    val selected = uiState.alarmSettings.routineMinutes == minutes
                    Surface(
                        onClick = { onRoutineMinutesChange(minutes) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("routine_${minutes}m_chip"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) ElectricCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer,
                        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan) else null
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${minutes}분 루틴",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Motion Groove Meter & Choreography Tip
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsRun,
                                contentDescription = null,
                                tint = AcidLime,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "모닝 그루브 에너지",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = SunriseAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${uiState.movePoints} PT",
                                style = MaterialTheme.typography.labelLarge,
                                color = SunriseAmber
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { uiState.grooveEnergy },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = AcidLime,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = grooveEncouragement,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        FilledTonalButton(
                            onClick = onBeatTap,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("beat_tap_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("리듬 터치", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationBanner(
    routineMinutes: Int,
    movePoints: Int,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D3B36))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = AcidLime.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Celebration,
                        contentDescription = null,
                        tint = AcidLime
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "오늘의 ${routineMinutes}분 모닝댄스 완료!",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "모닝 에너지 ${movePoints} PT 달성! 상쾌한 하루 시작하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCCEFE8)
                )
            }
            FilledTonalButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    }
}
