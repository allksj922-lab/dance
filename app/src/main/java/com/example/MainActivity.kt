package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.MorningAlarmReceiver
import com.example.ui.DanceTab
import com.example.ui.MorningDanceUiState
import com.example.ui.MorningDanceViewModel
import com.example.ui.screens.DailyLineupScreen
import com.example.ui.screens.MorningAlarmScreen
import com.example.ui.screens.TodayStageScreen
import com.example.ui.theme.MidnightPlum
import com.example.ui.theme.MorningDanceTheme
import com.example.ui.theme.StageIndigo

class MainActivity : ComponentActivity() {

    private val viewModel: MorningDanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.getBooleanExtra(MorningAlarmReceiver.EXTRA_OPEN_DANCE_STAGE, false) == true) {
            viewModel.openFromAlarmIntent()
        }

        setContent {
            MorningDanceTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                MorningDanceRoot(
                    uiState = uiState,
                    onSelectTab = viewModel::selectTab,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onReturnToToday = viewModel::returnToTodayTrack,
                    onVolumeChange = viewModel::setVolume,
                    onRoutineMinutesChange = viewModel::setRoutineMinutes,
                    onBeatTap = viewModel::tapBeatPad,
                    onResetSession = viewModel::resetSessionCounter,
                    onDismissCelebration = viewModel::dismissCelebration,
                    onPreviewTrack = { track -> viewModel.playTrack(track) },
                    onPausePlayback = viewModel::pausePlayback,
                    onToggleAlarm = viewModel::setAlarmEnabled,
                    onTimeSelected = viewModel::setAlarmTime,
                    onToggleDay = viewModel::toggleAlarmDay,
                    onToggleFadeIn = viewModel::setFadeInEnabled,
                    onToggleVibrate = viewModel::setVibrateEnabled,
                    onTestMorningWakeUp = viewModel::triggerMorningWakeUpExperience
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTodayIfNeeded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(MorningAlarmReceiver.EXTRA_OPEN_DANCE_STAGE, false)) {
            viewModel.openFromAlarmIntent()
        }
    }
}

@Composable
fun MorningDanceRoot(
    uiState: MorningDanceUiState,
    onSelectTab: (DanceTab) -> Unit,
    onTogglePlayPause: () -> Unit,
    onReturnToToday: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onRoutineMinutesChange: (Int) -> Unit,
    onBeatTap: () -> Unit,
    onResetSession: () -> Unit,
    onDismissCelebration: () -> Unit,
    onPreviewTrack: (com.example.model.DanceTrack) -> Unit,
    onPausePlayback: () -> Unit,
    onToggleAlarm: (Boolean) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onToggleDay: (Int) -> Unit,
    onToggleFadeIn: (Boolean) -> Unit,
    onToggleVibrate: (Boolean) -> Unit,
    onTestMorningWakeUp: () -> Unit
) {
    BackHandler(enabled = uiState.activeTab != DanceTab.TODAY) {
        onSelectTab(DanceTab.TODAY)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MidnightPlum, StageIndigo, MidnightPlum)
                )
            )
    ) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                MorningDanceTopBar(uiState = uiState, onAlarmBadgeClick = { onSelectTab(DanceTab.ALARM) })
            },
            bottomBar = {
                if (!useNavRail) {
                    Column {
                        AnimatedVisibility(
                            visible = uiState.activeTab != DanceTab.TODAY && uiState.telemetry.isPlaying
                        ) {
                            MiniNowPlayingBar(
                                uiState = uiState,
                                onTogglePlayPause = onTogglePlayPause,
                                onOpenStage = { onSelectTab(DanceTab.TODAY) }
                            )
                        }
                        MorningDanceBottomBar(
                            activeTab = uiState.activeTab,
                            onSelectTab = onSelectTab
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    MorningDanceNavRail(
                        activeTab = uiState.activeTab,
                        onSelectTab = onSelectTab
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    if (useNavRail && uiState.activeTab != DanceTab.TODAY && uiState.telemetry.isPlaying) {
                        MiniNowPlayingBar(
                            uiState = uiState,
                            onTogglePlayPause = onTogglePlayPause,
                            onOpenStage = { onSelectTab(DanceTab.TODAY) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (uiState.activeTab) {
                            DanceTab.TODAY -> TodayStageScreen(
                                uiState = uiState,
                                onTogglePlayPause = onTogglePlayPause,
                                onReturnToToday = onReturnToToday,
                                onVolumeChange = onVolumeChange,
                                onRoutineMinutesChange = onRoutineMinutesChange,
                                onBeatTap = onBeatTap,
                                onResetSession = onResetSession,
                                onDismissCelebration = onDismissCelebration
                            )
                            DanceTab.LINEUP -> DailyLineupScreen(
                                uiState = uiState,
                                onPreviewTrack = onPreviewTrack,
                                onPausePlayback = onPausePlayback
                            )
                            DanceTab.ALARM -> MorningAlarmScreen(
                                uiState = uiState,
                                onToggleAlarm = onToggleAlarm,
                                onTimeSelected = onTimeSelected,
                                onToggleDay = onToggleDay,
                                onRoutineMinutesChange = onRoutineMinutesChange,
                                onToggleFadeIn = onToggleFadeIn,
                                onToggleVibrate = onToggleVibrate,
                                onTestMorningWakeUp = onTestMorningWakeUp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MorningDanceTopBar(
    uiState: MorningDanceUiState,
    onAlarmBadgeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "매일 아침 새로운 댄스 리듬",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            onClick = onAlarmBadgeClick,
            shape = CircleShape,
            color = if (uiState.alarmSettings.enabled) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("top_alarm_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = "기상 알람 상태",
                    tint = if (uiState.alarmSettings.enabled) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (uiState.alarmSettings.enabled) {
                        uiState.alarmSettings.formatted24Hour()
                    } else {
                        "알람 꺼짐"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.alarmSettings.enabled) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun MiniNowPlayingBar(
    uiState: MorningDanceUiState,
    onTogglePlayPause: () -> Unit,
    onOpenStage: () -> Unit
) {
    val track = uiState.selectedTrack
    val accent = track.genre.accentHex

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onOpenStage)
            .testTag("mini_player_bar"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Equalizer,
                contentDescription = null,
                tint = accent
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.genre.displayNameKo} · ${track.bpm} BPM",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (uiState.telemetry.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (uiState.telemetry.isPlaying) "일시정지" else "재생",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun MorningDanceBottomBar(
    activeTab: DanceTab,
    onSelectTab: (DanceTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets.navigationBars
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        NavigationBarItem(
            selected = activeTab == DanceTab.TODAY,
            onClick = { onSelectTab(DanceTab.TODAY) },
            icon = {
                Icon(
                    imageVector = if (activeTab == DanceTab.TODAY) Icons.Rounded.MusicNote else Icons.Outlined.MusicNote,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(id = R.string.nav_today)) },
            colors = itemColors,
            modifier = Modifier.testTag("nav_today")
        )
        NavigationBarItem(
            selected = activeTab == DanceTab.LINEUP,
            onClick = { onSelectTab(DanceTab.LINEUP) },
            icon = {
                Icon(
                    imageVector = if (activeTab == DanceTab.LINEUP) Icons.Rounded.CalendarMonth else Icons.Outlined.CalendarMonth,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(id = R.string.nav_lineup)) },
            colors = itemColors,
            modifier = Modifier.testTag("nav_lineup")
        )
        NavigationBarItem(
            selected = activeTab == DanceTab.ALARM,
            onClick = { onSelectTab(DanceTab.ALARM) },
            icon = {
                Icon(
                    imageVector = if (activeTab == DanceTab.ALARM) Icons.Rounded.Alarm else Icons.Outlined.Alarm,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(id = R.string.nav_alarm)) },
            colors = itemColors,
            modifier = Modifier.testTag("nav_alarm")
        )
    }
}

@Composable
private fun MorningDanceNavRail(
    activeTab: DanceTab,
    onSelectTab: (DanceTab) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight()
    ) {
        val railColors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(0.2f))
        NavigationRailItem(
            selected = activeTab == DanceTab.TODAY,
            onClick = { onSelectTab(DanceTab.TODAY) },
            icon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
            label = { Text(stringResource(id = R.string.nav_today)) },
            colors = railColors
        )
        NavigationRailItem(
            selected = activeTab == DanceTab.LINEUP,
            onClick = { onSelectTab(DanceTab.LINEUP) },
            icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
            label = { Text(stringResource(id = R.string.nav_lineup)) },
            colors = railColors
        )
        NavigationRailItem(
            selected = activeTab == DanceTab.ALARM,
            onClick = { onSelectTab(DanceTab.ALARM) },
            icon = { Icon(Icons.Rounded.Alarm, contentDescription = null) },
            label = { Text(stringResource(id = R.string.nav_alarm)) },
            colors = railColors
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}
