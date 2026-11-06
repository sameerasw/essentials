/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Hardware Utilities
 * File: ShakeDetector.kt
 * Description: Accelerometer based shake gesture detector.
 */

package com.sameerasw.essentials.utils.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onJerk: (count: Int) -> Unit = {},
    private val onShake: () -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var jerkCount = 0
    private var windowStartMs = 0L
    private var lastJerkMs = 0L
    private var lastShakeMs = 0L

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        jerkCount = 0
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (gForce < SHAKE_THRESHOLD_G) return

        val nowMs = event.timestamp / NANOS_PER_MILLI
        if (nowMs - lastShakeMs < COOLDOWN_MS) return
        if (nowMs - lastJerkMs < MIN_JERK_GAP_MS) return
        lastJerkMs = nowMs

        if (nowMs - windowStartMs > WINDOW_MS) {
            windowStartMs = nowMs
            jerkCount = 0
        }
        jerkCount++

        if (jerkCount >= REQUIRED_JERKS) {
            jerkCount = 0
            lastShakeMs = nowMs
            onShake()
        } else {
            onJerk(jerkCount)
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) = Unit

    private companion object {
        const val SHAKE_THRESHOLD_G = 1.9f
        const val MIN_JERK_GAP_MS = 100L
        const val WINDOW_MS = 1000L
        const val REQUIRED_JERKS = 3
        const val COOLDOWN_MS = 1500L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
