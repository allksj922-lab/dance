package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MorningDanceApplication
import com.example.R
import com.example.audio.DancePlaybackService
import com.example.model.DailyTrackCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MorningAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as MorningDanceApplication
                val settings = app.preferencesRepository.settingsFlow.first()
                val todayTrack = DailyTrackCatalog.getTodayTrack()

                showWakeUpNotification(context, todayTrack.title, todayTrack.genre.displayNameKo, todayTrack.bpm, settings.vibrateEnabled)

                val fadeSeconds = if (settings.fadeInEnabled) 12 else 0
                try {
                    DancePlaybackService.startTrack(context, todayTrack, fadeInSeconds = fadeSeconds)
                } catch (_: Throwable) {
                    // Fallback if foreground service launch from background is restricted on specific OEMs
                    app.musicEngine.start(todayTrack, fadeInSeconds = fadeSeconds)
                }

                // Schedule next active morning occurrence
                AlarmScheduler.scheduleNextAlarm(context, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MORNING_DANCE_ALARM = "com.example.action.MORNING_DANCE_ALARM"
        const val ALARM_CHANNEL_ID = "morning_dance_alarm_channel"
        const val ALARM_NOTIFICATION_ID = 2001
        const val EXTRA_OPEN_DANCE_STAGE = "extra_open_dance_stage"

        fun showWakeUpNotification(
            context: Context,
            trackTitle: String,
            genreKo: String,
            bpm: Int,
            vibrate: Boolean
        ) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    ALARM_CHANNEL_ID,
                    context.getString(R.string.notification_channel_alarm),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "매일 아침 댄스 음악 기상 알람"
                    enableVibration(vibrate)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        null
                    )
                }
                manager.createNotificationChannel(channel)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_DANCE_STAGE, true)
            }
            val openPending = PendingIntent.getActivity(
                context,
                ALARM_NOTIFICATION_ID,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(context.getString(R.string.alarm_notification_title))
                .setContentText("오늘의 트랙: $trackTitle ($genreKo · $bpm BPM)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(openPending)
                .build()

            manager.notify(ALARM_NOTIFICATION_ID, notification)
        }
    }
}
