/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Input
 * File: PowerPressDetector.kt
 * Description: Component file for PowerPressDetector.kt.
 */

package com.sameerasw.essentials.input

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class PowerGestureTracker(
    private val doublePressWindowMs: Long = 300,
) {
    private var pendingDownMs: Long? = null

    fun onDown(timeMs: Long): Boolean {
        val thatDown = pendingDownMs
        return if (thatDown != null && timeMs - thatDown <= doublePressWindowMs) {
            pendingDownMs = null
            true
        } else {
            pendingDownMs = timeMs
            false
        }
    }

    fun isGestureStart(timeMs: Long): Boolean {
        val thatDown = pendingDownMs
        return thatDown == null || timeMs - thatDown > doublePressWindowMs
    }

    fun onHold() {
        pendingDownMs = null
    }
}

class PowerPressDetector(
    private val devicePath: String,
    private val isScreenOn: () -> Boolean,
    private val holdThresholdMs: Long = 500,
) {
    private val _events = MutableSharedFlow<PowerPressEvent>()
    val events = _events.asSharedFlow()

    private val isRunning = AtomicBoolean(false)
    val isListening: Boolean
        get() = isRunning.get()

    private var detectionJob: Job? = null
    private var holdJob: Job? = null
    private val reader = InputEventReader(devicePath)
    private val tracker = PowerGestureTracker()
    private var gestureScreenOn = false
    private var gestureFirstPressAt = 0L
    private var pendingDouble = false
    private var listeningScope: CoroutineScope? = null

    fun startListening(scope: CoroutineScope): Boolean {
        if (isRunning.getAndSet(true)) return false
        listeningScope = scope

        detectionJob =
            scope.launch(Dispatchers.IO) {
                if (!reader.open()) {
                    isRunning.set(false)
                    return@launch
                }

                try {
                    while (isRunning.get() && isActive) {
                        val event = reader.readEvent()
                        if (event == null) {
                            delay(20)
                            continue
                        }

                        if (event.type != InputEventReader.EV_KEY) continue
                        if (event.code != InputEventReader.KEY_POWER) continue

                        when (event.value) {
                            1 -> {
                                val t = event.timeSecond * 1000 + event.timeMicro / 1000
                                if (tracker.isGestureStart(t)) {
                                    gestureScreenOn = isScreenOn()
                                    gestureFirstPressAt = SystemClock.elapsedRealtime()
                                }
                                if (tracker.onDown(t)) {
                                    holdJob?.cancel()
                                    pendingDouble = true
                                } else {
                                    holdJob?.cancel()
                                    holdJob =
                                        listeningScope?.launch(Dispatchers.IO) {
                                            delay(holdThresholdMs)
                                            tracker.onHold()
                                            _events.emit(
                                                PowerPressEvent(
                                                    PowerGesture.HOLD,
                                                    gestureScreenOn,
                                                    gestureFirstPressAt,
                                                ),
                                            )
                                        }
                                }
                            }

                            0 -> {
                                holdJob?.cancel()
                                if (pendingDouble) {
                                    pendingDouble = false
                                    // Android toggles screen on release; emit after release so handler acts after it
                                    _events.emit(
                                        PowerPressEvent(
                                            PowerGesture.DOUBLE_PRESS,
                                            gestureScreenOn,
                                            gestureFirstPressAt,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    holdJob?.cancel()
                    reader.close()
                    isRunning.set(false)
                }
            }
        return true
    }

    fun stopListening() {
        isRunning.set(false)
        detectionJob?.cancel()
        holdJob?.cancel()
        reader.close()
    }
}
