package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Tracks real-time physical dance motion energy using the device's accelerometer
 * without requiring runtime permissions.
 */
class DanceMotionTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _grooveEnergy = MutableStateFlow(0f) // 0f..1f
    val grooveEnergy: StateFlow<Float> = _grooveEnergy.asStateFlow()

    private val _movePoints = MutableStateFlow(0)
    val movePoints: StateFlow<Int> = _movePoints.asStateFlow()

    val hasSensor: Boolean get() = accelerometer != null

    private var smoothedEnergy = 0f
    private var lastStepTimestamp = 0L

    fun startTracking() {
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopTracking() {
        sensorManager?.unregisterListener(this)
    }

    fun resetSession() {
        smoothedEnergy = 0f
        _grooveEnergy.value = 0f
        _movePoints.value = 0
    }

    /**
     * Allows manual beat-tap boost when testing stationary or on devices without motion.
     */
    fun boostBeatTap() {
        smoothedEnergy = (smoothedEnergy + 0.35f).coerceIn(0f, 1f)
        _grooveEnergy.value = smoothedEnergy
        _movePoints.value += 10
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        val x = values[0]
        val y = values[1]
        val z = values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        // Deviation from standard gravity (~9.81 m/s^2)
        val delta = abs(magnitude - SensorManager.GRAVITY_EARTH)

        val instantEnergy = (delta / 8.5f).coerceIn(0f, 1f)
        smoothedEnergy = smoothedEnergy * 0.82f + instantEnergy * 0.18f
        _grooveEnergy.value = smoothedEnergy

        val now = System.currentTimeMillis()
        if (delta > 2.4f && now - lastStepTimestamp > 320L) {
            lastStepTimestamp = now
            _movePoints.value += (delta * 2.5f).toInt().coerceIn(5, 25)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
