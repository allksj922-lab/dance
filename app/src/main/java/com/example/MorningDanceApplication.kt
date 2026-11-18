package com.example

import android.app.Application
import com.example.audio.DanceMusicEngine
import com.example.data.MorningPreferencesRepository

class MorningDanceApplication : Application() {
    val musicEngine: DanceMusicEngine by lazy { DanceMusicEngine() }
    val preferencesRepository: MorningPreferencesRepository by lazy {
        MorningPreferencesRepository(applicationContext)
    }
}
