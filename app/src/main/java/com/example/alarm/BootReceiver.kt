package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MorningDanceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as MorningDanceApplication
                val settings = app.preferencesRepository.settingsFlow.first()
                if (settings.enabled) {
                    AlarmScheduler.scheduleNextAlarm(context, settings)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
