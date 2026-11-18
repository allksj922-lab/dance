package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DanceTrack
import com.example.ui.MorningDanceUiState
import com.example.ui.theme.SunriseAmber

@Composable
fun DailyLineupScreen(
    uiState: MorningDanceUiState,
    onPreviewTrack: (DanceTrack) -> Unit,
    onPausePlayback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "매일매일 바뀌는 데일리 댄스",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "매일 자정마다 새로운 장르, 리듬, 멜로디의 댄스곡이 자동으로 열려요. 아래 카드를 눌러 날짜별 음악을 미리 들어보세요!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(uiState.lineupTracks, key = { it.id }) { track ->
                val isToday = track.date == uiState.todayTrack.date
                val isPlayingThis = uiState.telemetry.isPlaying && uiState.telemetry.currentTrackId == track.id
                val dayOffset = (track.date.toEpochDay() - uiState.todayTrack.date.toEpochDay()).toInt()
                val dayBadge = when (dayOffset) {
                    -1 -> "어제"
                    0 -> "오늘의 댄스"
                    1 -> "내일"
                    else -> "+${dayOffset}일 뒤"
                }

                DailyLineupTrackCard(
                    track = track,
                    dayBadge = dayBadge,
                    isToday = isToday,
                    isPlaying = isPlayingThis,
                    onPlayOrPause = {
                        if (isPlayingThis) onPausePlayback() else onPreviewTrack(track)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DailyLineupTrackCard(
    track: DanceTrack,
    dayBadge: String,
    isToday: Boolean,
    isPlaying: Boolean,
    onPlayOrPause: () -> Unit
) {
    val accent = track.genre.accentHex
    Card(
        onClick = onPlayOrPause,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lineup_card_${track.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = when {
            isPlaying -> BorderStroke(2.dp, accent)
            isToday -> BorderStroke(1.5.dp, accent.copy(alpha = 0.65f))
            else -> null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = track.coverResId),
                contentDescription = "${track.title} 앨범 아트",
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isToday) accent else MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "$dayBadge · ${track.date.formattedShort()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "${track.bpm} BPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }

                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.genre.displayNameKo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(track.energyStars) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = SunriseAmber,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            FilledIconButton(
                onClick = onPlayOrPause,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("preview_track_${track.id}"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isPlaying) accent else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = when {
                        isPlaying -> Icons.Rounded.Pause
                        else -> Icons.Rounded.PlayArrow
                    },
                    contentDescription = if (isPlaying) "일시정지" else "트랙 미리듣기"
                )
            }
        }
    }
}
