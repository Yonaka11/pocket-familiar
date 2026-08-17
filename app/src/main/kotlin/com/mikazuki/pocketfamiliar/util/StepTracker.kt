package com.mikazuki.pocketfamiliar.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build

/**
 * Lightweight live step-counter adapter. It only starts when the platform
 * permission is already granted. The UI can request permission separately.
 */
class StepTracker(
    private val context: Context,
    private val onSteps: (Int) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var lastTotal: Int? = null

    fun register() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) return

        stepCounter?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
        lastTotal = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val total = event?.values?.firstOrNull()?.toInt() ?: return
        val previous = lastTotal
        lastTotal = total
        if (previous == null) return
        val delta = (total - previous).coerceAtLeast(0)
        if (delta > 0) onSteps(delta)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
