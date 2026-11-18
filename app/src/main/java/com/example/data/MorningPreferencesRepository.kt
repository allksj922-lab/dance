package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.morningDanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "morning_dance_prefs"
)

data class MorningAlarmSettings(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val activeDays: Set<Int> = setOf(0, 1, 2, 3, 4), // 0=Mon..6=Sun
    val routineMinutes: Int = 5,
    val fadeInEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val volume: Float = 0.85f
) {
    fun formattedTime(): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%02d:%02d %s", displayHour, minute, period)
    }

    fun formatted24Hour(): String = String.format("%02d:%02d", hour, minute)
}

class MorningPreferencesRepository(private val context: Context) {

    private object Keys {
        val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        val ALARM_HOUR = intPreferencesKey("alarm_hour")
        val ALARM_MINUTE = intPreferencesKey("alarm_minute")
        val ACTIVE_DAYS = stringPreferencesKey("active_days")
        val ROUTINE_MINUTES = intPreferencesKey("routine_minutes")
        val FADE_IN_ENABLED = booleanPreferencesKey("fade_in_enabled")
        val VIBRATE_ENABLED = booleanPreferencesKey("vibrate_enabled")
        val MASTER_VOLUME = floatPreferencesKey("master_volume")
    }

    val settingsFlow: Flow<MorningAlarmSettings> = context.morningDanceDataStore.data.map { prefs ->
        val rawDays = prefs[Keys.ACTIVE_DAYS] ?: "0,1,2,3,4"
        val parsedDays = rawDays.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0..6 }
            .toSet()

        MorningAlarmSettings(
            enabled = prefs[Keys.ALARM_ENABLED] ?: false,
            hour = prefs[Keys.ALARM_HOUR] ?: 7,
            minute = prefs[Keys.ALARM_MINUTE] ?: 0,
            activeDays = parsedDays.ifEmpty { setOf(0, 1, 2, 3, 4) },
            routineMinutes = prefs[Keys.ROUTINE_MINUTES] ?: 5,
            fadeInEnabled = prefs[Keys.FADE_IN_ENABLED] ?: true,
            vibrateEnabled = prefs[Keys.VIBRATE_ENABLED] ?: true,
            volume = prefs[Keys.MASTER_VOLUME] ?: 0.85f
        )
    }

    suspend fun updateAlarmEnabled(enabled: Boolean) {
        context.morningDanceDataStore.edit { it[Keys.ALARM_ENABLED] = enabled }
    }

    suspend fun updateAlarmTime(hour: Int, minute: Int) {
        context.morningDanceDataStore.edit { prefs ->
            prefs[Keys.ALARM_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.ALARM_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun toggleActiveDay(dayIndex: Int) {
        context.morningDanceDataStore.edit { prefs ->
            val current = (prefs[Keys.ACTIVE_DAYS] ?: "0,1,2,3,4")
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 0..6 }
                .toMutableSet()
            if (current.contains(dayIndex)) {
                if (current.size > 1) current.remove(dayIndex)
            } else {
                current.add(dayIndex)
            }
            prefs[Keys.ACTIVE_DAYS] = current.sorted().joinToString(",")
        }
    }

    suspend fun updateRoutineMinutes(minutes: Int) {
        context.morningDanceDataStore.edit { it[Keys.ROUTINE_MINUTES] = minutes }
    }

    suspend fun updateFadeInEnabled(enabled: Boolean) {
        context.morningDanceDataStore.edit { it[Keys.FADE_IN_ENABLED] = enabled }
    }

    suspend fun updateVibrateEnabled(enabled: Boolean) {
        context.morningDanceDataStore.edit { it[Keys.VIBRATE_ENABLED] = enabled }
    }

    suspend fun updateVolume(volume: Float) {
        context.morningDanceDataStore.edit { it[Keys.MASTER_VOLUME] = volume.coerceIn(0f, 1f) }
    }
}
