package com.example.ui

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MorningDanceApplication
import com.example.alarm.AlarmScheduler
import com.example.alarm.MorningAlarmReceiver
import com.example.audio.DancePlaybackService
import com.example.data.MorningAlarmSettings
import com.example.model.BeatTelemetry
import com.example.model.DailyTrackCatalog
import com.example.model.DanceDate
import com.example.model.DanceTrack
import com.example.sensor.DanceMotionTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class DanceTab {
    TODAY,
    LINEUP,
    ALARM
}

data class MorningDanceUiState(
    val activeTab: DanceTab = DanceTab.TODAY,
    val todayTrack: DanceTrack = DailyTrackCatalog.getTodayTrack(),
    val selectedTrack: DanceTrack = DailyTrackCatalog.getTodayTrack(),
    val lineupTracks: List<DanceTrack> = DailyTrackCatalog.getLineupAround(),
    val telemetry: BeatTelemetry = BeatTelemetry(),
    val alarmSettings: MorningAlarmSettings = MorningAlarmSettings(),
    val grooveEnergy: Float = 0f,
    val movePoints: Int = 0,
    val sessionDancedSeconds: Int = 0,
    val showGoalCelebration: Boolean = false,
    val hasAccelerometer: Boolean = true
) {
    val isPreviewingOtherDay: Boolean
        get() = selectedTrack.date != todayTrack.date

    val routineGoalSeconds: Int
        get() = alarmSettings.routineMinutes * 60

    val routineProgress: Float
        get() = (sessionDancedSeconds.toFloat() / routineGoalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
}

class MorningDanceViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MorningDanceApplication
    private val musicEngine = app.musicEngine
    private val prefsRepo = app.preferencesRepository
    private val motionTracker = DanceMotionTracker(application)

    private val _activeTab = MutableStateFlow(DanceTab.TODAY)
    private val _todayTrack = MutableStateFlow(DailyTrackCatalog.getTodayTrack())
    private val _selectedTrack = MutableStateFlow(_todayTrack.value)
    private val _lineupTracks = MutableStateFlow(DailyTrackCatalog.getLineupAround(_todayTrack.value.date))
    private val _sessionDancedSeconds = MutableStateFlow(0)
    private val _showGoalCelebration = MutableStateFlow(false)

    private var timerJob: Job? = null
    private var celebrationShownForSession = false

    val uiState: StateFlow<MorningDanceUiState> = combine(
        _activeTab,
        _todayTrack,
        _selectedTrack,
        _lineupTracks,
        musicEngine.telemetry,
        prefsRepo.settingsFlow,
        motionTracker.grooveEnergy,
        motionTracker.movePoints,
        _sessionDancedSeconds,
        _showGoalCelebration
    ) { values ->
        val tab = values[0] as DanceTab
        val today = values[1] as DanceTrack
        val selected = values[2] as DanceTrack
        @Suppress("UNCHECKED_CAST")
        val lineup = values[3] as List<DanceTrack>
        val telemetry = values[4] as BeatTelemetry
        val settings = values[5] as MorningAlarmSettings
        val energy = values[6] as Float
        val points = values[7] as Int
        val dancedSec = values[8] as Int
        val celebrate = values[9] as Boolean

        MorningDanceUiState(
            activeTab = tab,
            todayTrack = today,
            selectedTrack = selected,
            lineupTracks = lineup,
            telemetry = telemetry,
            alarmSettings = settings,
            grooveEnergy = energy,
            movePoints = points,
            sessionDancedSeconds = dancedSec,
            showGoalCelebration = celebrate,
            hasAccelerometer = motionTracker.hasSensor
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MorningDanceUiState(
            hasAccelerometer = motionTracker.hasSensor
        )
    )

    init {
        viewModelScope.launch {
            prefsRepo.settingsFlow.collect { settings ->
                musicEngine.setVolume(settings.volume)
            }
        }
        viewModelScope.launch {
            musicEngine.telemetry.collect { telemetry ->
                if (telemetry.isPlaying) {
                    ensureSessionTimerRunning()
                    motionTracker.startTracking()
                } else {
                    stopSessionTimer()
                    motionTracker.stopTracking()
                }
            }
        }
    }

    fun selectTab(tab: DanceTab) {
        _activeTab.value = tab
    }

    fun refreshTodayIfNeeded() {
        val currentToday = DanceDate.today()
        if (currentToday != _todayTrack.value.date) {
            val newTodayTrack = DailyTrackCatalog.trackForDate(currentToday)
            val wasOnOldToday = _selectedTrack.value.date == _todayTrack.value.date
            _todayTrack.value = newTodayTrack
            _lineupTracks.value = DailyTrackCatalog.getLineupAround(currentToday)
            if (wasOnOldToday) {
                _selectedTrack.value = newTodayTrack
                if (musicEngine.isPlaying()) {
                    DancePlaybackService.startTrack(app, newTodayTrack)
                }
            }
        }
    }

    fun togglePlayPause() {
        if (musicEngine.isPlaying()) {
            pausePlayback()
        } else {
            playTrack(_selectedTrack.value)
        }
    }

    fun playTrack(track: DanceTrack, fadeInSeconds: Int = 0) {
        _selectedTrack.value = track
        try {
            DancePlaybackService.startTrack(app, track, fadeInSeconds = fadeInSeconds)
        } catch (_: Throwable) {
            musicEngine.start(track, fadeInSeconds = fadeInSeconds)
        }
    }

    fun pausePlayback() {
        musicEngine.stop()
        DancePlaybackService.stopTrack(app)
    }

    fun returnToTodayTrack() {
        val today = _todayTrack.value
        _selectedTrack.value = today
        if (musicEngine.isPlaying()) {
            playTrack(today)
        }
    }

    fun tapBeatPad() {
        motionTracker.boostBeatTap()
    }

    fun dismissCelebration() {
        _showGoalCelebration.value = false
    }

    fun resetSessionCounter() {
        _sessionDancedSeconds.value = 0
        celebrationShownForSession = false
        _showGoalCelebration.value = false
        motionTracker.resetSession()
    }

    fun setAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateAlarmEnabled(enabled)
            val updated = uiState.value.alarmSettings.copy(enabled = enabled)
            if (enabled) {
                AlarmScheduler.scheduleNextAlarm(app, updated)
            } else {
                AlarmScheduler.cancelAlarm(app)
            }
        }
    }

    fun setAlarmTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.updateAlarmTime(hour, minute)
            val updated = uiState.value.alarmSettings.copy(hour = hour, minute = minute)
            if (updated.enabled) {
                AlarmScheduler.scheduleNextAlarm(app, updated)
            }
        }
    }

    fun toggleAlarmDay(dayIndex: Int) {
        viewModelScope.launch {
            prefsRepo.toggleActiveDay(dayIndex)
            val currentDays = uiState.value.alarmSettings.activeDays.toMutableSet()
            if (currentDays.contains(dayIndex)) {
                if (currentDays.size > 1) currentDays.remove(dayIndex)
            } else {
                currentDays.add(dayIndex)
            }
            val updated = uiState.value.alarmSettings.copy(activeDays = currentDays)
            if (updated.enabled) {
                AlarmScheduler.scheduleNextAlarm(app, updated)
            }
        }
    }

    fun setRoutineMinutes(minutes: Int) {
        viewModelScope.launch {
            prefsRepo.updateRoutineMinutes(minutes)
        }
    }

    fun setFadeInEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateFadeInEnabled(enabled)
        }
    }

    fun setVibrateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateVibrateEnabled(enabled)
        }
    }

    fun setVolume(volume: Float) {
        musicEngine.setVolume(volume)
        viewModelScope.launch {
            prefsRepo.updateVolume(volume)
        }
    }

    fun triggerMorningWakeUpExperience() {
        val settings = uiState.value.alarmSettings
        val today = _todayTrack.value
        MorningAlarmReceiver.showWakeUpNotification(
            context = app,
            trackTitle = today.title,
            genreKo = today.genre.displayNameKo,
            bpm = today.bpm,
            vibrate = settings.vibrateEnabled
        )
        if (settings.vibrateEnabled) {
            pulseWakeVibration()
        }
        _activeTab.value = DanceTab.TODAY
        val fadeSeconds = if (settings.fadeInEnabled) 8 else 0
        playTrack(today, fadeInSeconds = fadeSeconds)
    }

    fun openFromAlarmIntent() {
        refreshTodayIfNeeded()
        _activeTab.value = DanceTab.TODAY
        val today = _todayTrack.value
        _selectedTrack.value = today
        if (!musicEngine.isPlaying()) {
            val fadeSeconds = if (uiState.value.alarmSettings.fadeInEnabled) 8 else 0
            playTrack(today, fadeInSeconds = fadeSeconds)
        }
    }

    private fun pulseWakeVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 180, 100, 180), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = app.getSystemService(Vibrator::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 100, 180), -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(250)
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun ensureSessionTimerRunning() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (musicEngine.isPlaying()) {
                    val next = _sessionDancedSeconds.value + 1
                    _sessionDancedSeconds.value = next
                    val goal = uiState.value.routineGoalSeconds
                    if (!celebrationShownForSession && next >= goal) {
                        celebrationShownForSession = true
                        _showGoalCelebration.value = true
                    }
                }
            }
        }
    }

    private fun stopSessionTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        motionTracker.stopTracking()
        stopSessionTimer()
        super.onCleared()
    }
}
