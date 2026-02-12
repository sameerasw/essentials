package com.sameerasw.essentials.input

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class VolumeLongPressDetector(
    private val volumeDevicePath: String,
    private val longPressThresholdMs: Long = 500
) {
    private val _events = MutableSharedFlow<VolumePressEvent>()
    val events = _events.asSharedFlow()

    private val isRunning = AtomicBoolean(false)
    private var detectionJob: Job? = null
    private val reader = InputEventReader(volumeDevicePath)

    private var volumeUpPressTime = 0L
    private var volumeDownPressTime = 0L
    private var volumeUpPressed = false
    private var volumeDownPressed = false
    private var listeningScope: CoroutineScope? = null

    fun startListening(scope: CoroutineScope): Boolean {
        if (isRunning.getAndSet(true)) return false
        listeningScope = scope

        detectionJob = scope.launch(Dispatchers.IO) {
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

                    when (event.code) {
                        InputEventReader.KEY_VOLUMEUP -> handleButton(VolumeDirection.UP, event.value == 1, event.timeSecond * 1000 + event.timeMicro / 1000)
                        InputEventReader.KEY_VOLUMEDOWN -> handleButton(VolumeDirection.DOWN, event.value == 1, event.timeSecond * 1000 + event.timeMicro / 1000)
                    }
                }
            } catch (e: Exception) {
                // Handle read errors gracefully
            } finally {
                reader.close()
                isRunning.set(false)
            }
        }
        return true
    }

    fun stopListening() {
        isRunning.set(false)
        detectionJob?.cancel()
        reader.close()
    }

    private var upJob: Job? = null
    private var downJob: Job? = null
    private var isUpTriggered = false
    private var isDownTriggered = false

    private suspend fun handleButton(direction: VolumeDirection, pressed: Boolean, eventTime: Long) {
        if (pressed) {
            // Cancel existing job if any (debounce/overlap safety)
            if (direction == VolumeDirection.UP) upJob?.cancel() else downJob?.cancel()
            
            val job = listeningScope?.launch(Dispatchers.IO) {
                 delay(longPressThresholdMs)
                 if (direction == VolumeDirection.UP) isUpTriggered = true else isDownTriggered = true
                 _events.emit(VolumePressEvent.LongPress(direction, longPressThresholdMs, System.currentTimeMillis()))
            }
            
            if (direction == VolumeDirection.UP) {
                upJob = job
                isUpTriggered = false
            } else {
                downJob = job
                isDownTriggered = false
            }
            
        } else {
            // Button Released
            if (direction == VolumeDirection.UP) {
                upJob?.cancel()
                if (!isUpTriggered) {
                     _events.emit(VolumePressEvent.ShortPress(direction, eventTime))
                }
                isUpTriggered = false
            } else {
                downJob?.cancel()
                if (!isDownTriggered) {
                     _events.emit(VolumePressEvent.ShortPress(direction, eventTime))
                }
                isDownTriggered = false
            }
        }
    }
}
