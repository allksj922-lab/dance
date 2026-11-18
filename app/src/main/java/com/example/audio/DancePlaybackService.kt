package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.MorningDanceApplication
import com.example.R
import com.example.model.DailyTrackCatalog
import com.example.model.DanceDate
import com.example.model.DanceTrack

/**
 * Foreground mediaPlayback service so morning dance music continues playing smoothly
 * even when the user sets their phone down or the screen dims while dancing.
 */
class DancePlaybackService : Service() {

    private val engine: DanceMusicEngine
        get() = (application as MorningDanceApplication).musicEngine

    private var audioFocusRequest: AudioFocusRequest? = null
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlaybackAndService()
                return START_NOT_STICKY
            }
            ACTION_PLAY -> {
                val epochDay = intent.getLongExtra(EXTRA_EPOCH_DAY, DanceDate.today().toEpochDay())
                val fadeInSeconds = intent.getIntExtra(EXTRA_FADE_IN_SECONDS, 0)
                val track = DailyTrackCatalog.trackForDate(DanceDate.fromEpochDay(epochDay))
                requestFocus()
                startForeground(NOTIFICATION_ID, buildNotification(track))
                engine.start(track, fadeInSeconds = fadeInSeconds)
                return START_STICKY
            }
            else -> {
                val track = DailyTrackCatalog.getTodayTrack()
                startForeground(NOTIFICATION_ID, buildNotification(track))
                return START_NOT_STICKY
            }
        }
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        stopPlaybackAndService()
                    }
                }
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun stopPlaybackAndService() {
        engine.stop()
        abandonFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        engine.stop()
        abandonFocus()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "매일 아침 신나는 댄스 음악 재생"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(track: DanceTrack): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, DancePlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("${track.title} · ${track.bpm} BPM")
            .setContentText("${track.genre.displayNameKo} · ${track.subtitle}")
            .setContentIntent(openPending)
            .setOngoing(true)
            .addAction(0, getString(R.string.action_stop), stopPending)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "morning_dance_playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.example.action.PLAY_DANCE"
        const val ACTION_STOP = "com.example.action.STOP_DANCE"
        const val EXTRA_EPOCH_DAY = "extra_epoch_day"
        const val EXTRA_FADE_IN_SECONDS = "extra_fade_in_seconds"

        fun startTrack(context: Context, track: DanceTrack, fadeInSeconds: Int = 0) {
            val intent = Intent(context, DancePlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_EPOCH_DAY, track.date.toEpochDay())
                putExtra(EXTRA_FADE_IN_SECONDS, fadeInSeconds)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopTrack(context: Context) {
            context.stopService(Intent(context, DancePlaybackService::class.java))
        }
    }
}
