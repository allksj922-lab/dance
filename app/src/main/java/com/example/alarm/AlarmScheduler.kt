package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.MorningAlarmSettings
import java.util.Calendar

object AlarmScheduler {

    private const val ALARM_REQUEST_CODE = 7701

    fun scheduleNextAlarm(context: Context, settings: MorningAlarmSettings): Long? {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(context)

        if (!settings.enabled) {
            alarmManager.cancel(pendingIntent)
            return null
        }

        val triggerMillis = computeNextTriggerMillis(
            hour = settings.hour,
            minute = settings.minute,
            activeDays = settings.activeDays
        )

        val showIntent = PendingIntent.getActivity(
            context,
            ALARM_REQUEST_CODE + 1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            val info = AlarmManager.AlarmClockInfo(triggerMillis, showIntent)
            alarmManager.setAlarmClock(info, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }

        return triggerMillis
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildAlarmPendingIntent(context))
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        activeDays: Set<Int>,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val targetDays = activeDays.ifEmpty { setOf(0, 1, 2, 3, 4, 5, 6) }
        val base = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (offset in 0..7) {
            val candidate = (base.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val monBasedDay = calendarDayToMonIndex(candidate.get(Calendar.DAY_OF_WEEK))
            if (targetDays.contains(monBasedDay) && candidate.timeInMillis > nowMillis) {
                return candidate.timeInMillis
            }
        }
        return base.timeInMillis + 24L * 60L * 60L * 1000L
    }

    private fun calendarDayToMonIndex(calendarDayOfWeek: Int): Int = when (calendarDayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        else -> 6 // Calendar.SUNDAY
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MorningAlarmReceiver::class.java).apply {
            action = MorningAlarmReceiver.ACTION_MORNING_DANCE_ALARM
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
