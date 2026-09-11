/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Handlers
 * File: DuoTouchHandler.kt
 * Description: Touch handler managing gestures on Duo camera cutout overlay.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.utils.DuoOverlayView
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

class DuoTouchHandler(
    private val service: AccessibilityService,
) {
    private val settingsRepository by lazy { SettingsRepository(service) }
    private val audioManager by lazy { service.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var overlayView: DuoOverlayView? = null
    var cameraCenterX: Float = 0f
    var cameraCenterY: Float = 0f
    var cameraRadiusPx: Float = 36f
    var ringRadiusScale: Float = 1.0f

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downTime: Long = 0L
    private var lastSlideX: Float = 0f

    private var lastTapTime: Long = 0L
    private var lastTapX: Float = 0f
    private var lastTapY: Float = 0f
    private var isDoubleTapPending: Boolean = false

    private var isTouchActiveInCutout: Boolean = false
    private var isLongPressTriggered: Boolean = false
    private var isSwipeDownTriggered: Boolean = false
    private var isTrackTriggered: Boolean = false
    private var isTrackThresholdReached: Boolean = false
    private var isSoundModeTriggered: Boolean = false
    private var isVolumeOrBrightnessAdjusted: Boolean = false

    private val density: Float
        get() = service.resources.displayMetrics.density

    private val touchSlopPx: Float
        get() = 12f * density

    private val slideStepPx: Float
        get() = 18f * density

    private val swipeDownTriggerPx: Float
        get() = 24f * density

    private val horizontalTriggerPx: Float
        get() = 38f * density

    private val trackTriggerPx: Float
        get() = 65f * density

    private val longPressTimeoutMs = 450L
    private val doubleTapTimeoutMs = 260L

    private var pendingSingleTapRunnable: Runnable? = null

    private val longPressRunnable = Runnable {
        isLongPressTriggered = true
        val action = settingsRepository.getDuoLongPressAction()
        if (action != null) {
            HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
            executeAction(action)
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (cameraCenterX <= 0f && cameraCenterY <= 0f) return false

        val x = event.rawX
        val y = event.rawY

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val effectiveRadius = (cameraRadiusPx + 20f * density) * ringRadiusScale
                val dist = hypot(x - cameraCenterX, y - cameraCenterY)
                if (dist > effectiveRadius) {
                    isTouchActiveInCutout = false
                    return false
                }

                isTouchActiveInCutout = true
                downX = x
                downY = y
                lastSlideX = x
                downTime = SystemClock.uptimeMillis()
                isLongPressTriggered = false
                isSwipeDownTriggered = false
                isTrackTriggered = false
                isTrackThresholdReached = false
                isSoundModeTriggered = false
                isVolumeOrBrightnessAdjusted = false

                overlayView?.triggerTapAnimation()

                val doubleTapAction = settingsRepository.getDuoDoubleTapAction()
                val isSecondTapInWindow = (downTime - lastTapTime) < doubleTapTimeoutMs &&
                    hypot(x - lastTapX, y - lastTapY) < touchSlopPx * 1.5f

                if (doubleTapAction != null && isSecondTapInWindow) {
                    pendingSingleTapRunnable?.let { handler.removeCallbacks(it) }
                    pendingSingleTapRunnable = null
                    isDoubleTapPending = true
                } else {
                    isDoubleTapPending = false
                    if (pendingSingleTapRunnable != null) {
                        pendingSingleTapRunnable?.let {
                            handler.removeCallbacks(it)
                            it.run()
                        }
                        pendingSingleTapRunnable = null
                    }
                }

                if (settingsRepository.getDuoLongPressAction() != null) {
                    handler.postDelayed(longPressRunnable, longPressTimeoutMs)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isTouchActiveInCutout) return false

                val dx = x - downX
                val dy = y - downY
                val totalDist = hypot(dx, dy)

                if (totalDist > touchSlopPx) {
                    handler.removeCallbacks(longPressRunnable)
                    isDoubleTapPending = false
                }

                if (isLongPressTriggered) return true

                val slideMode = settingsRepository.getDuoSlideMode()
                val swipeDownAction = settingsRepository.getDuoSwipeDownAction()

                val isHorizontalDominant = abs(dx) > abs(dy) * 1.1f
                val isVerticalDominant = dy > 0f && dy > abs(dx) * 1.1f

                if (isHorizontalDominant && slideMode != "none") {
                    handleHorizontalSlide(slideMode, x, dx)
                    return true
                }

                if (swipeDownAction != null && isVerticalDominant) {
                    val pullOffset = (dy * 0.4f).coerceAtMost(25f * density)
                    val stretch = 1.0f + (pullOffset / (65f * density)).coerceAtMost(0.22f)
                    overlayView?.setPullDownOffset(pullOffset, stretch)

                    if (!isSwipeDownTriggered && dy >= swipeDownTriggerPx) {
                        isSwipeDownTriggered = true
                        HapticUtil.performHapticForService(service, HapticFeedbackType.DOUBLE)
                        executeAction(swipeDownAction)
                    }
                    return true
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isTouchActiveInCutout) return false
                handler.removeCallbacks(longPressRunnable)
                overlayView?.releasePullDown()
                overlayView?.releaseTrackRotation()

                val elapsed = SystemClock.uptimeMillis() - downTime
                val dx = x - downX
                val dy = y - downY
                val totalDist = hypot(dx, dy)

                val slideMode = settingsRepository.getDuoSlideMode()
                if (slideMode == "track" && isTrackThresholdReached) {
                    isTrackTriggered = true
                    val isInverted = settingsRepository.isDuoSlideInvertDirectionEnabled()
                    HapticUtil.performHapticForService(service, HapticFeedbackType.DOUBLE)
                    val key = if (dx > 0) {
                        if (isInverted) KeyEvent.KEYCODE_MEDIA_PREVIOUS else KeyEvent.KEYCODE_MEDIA_NEXT
                    } else {
                        if (isInverted) KeyEvent.KEYCODE_MEDIA_NEXT else KeyEvent.KEYCODE_MEDIA_PREVIOUS
                    }
                    dispatchMediaKey(key)
                }

                val didPerformAnyGesture = isLongPressTriggered ||
                    isSwipeDownTriggered ||
                    isTrackTriggered ||
                    isSoundModeTriggered ||
                    isVolumeOrBrightnessAdjusted

                if (!didPerformAnyGesture && totalDist < touchSlopPx && elapsed < 350L) {
                    val doubleTapAction = settingsRepository.getDuoDoubleTapAction()
                    val tapAction = settingsRepository.getDuoTapAction()

                    if (isDoubleTapPending && doubleTapAction != null) {
                        isDoubleTapPending = false
                        lastTapTime = 0L
                        overlayView?.triggerTapAnimation()
                        HapticUtil.performHapticForService(service, HapticFeedbackType.DOUBLE)
                        executeAction(doubleTapAction)
                    } else if (doubleTapAction != null) {
                        lastTapTime = SystemClock.uptimeMillis()
                        lastTapX = downX
                        lastTapY = downY
                        if (tapAction != null) {
                            val runnable = Runnable {
                                pendingSingleTapRunnable = null
                                HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                                executeAction(tapAction)
                            }
                            pendingSingleTapRunnable = runnable
                            handler.postDelayed(runnable, doubleTapTimeoutMs)
                        }
                    } else if (tapAction != null) {
                        lastTapTime = 0L
                        HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                        executeAction(tapAction)
                    }
                }

                isTouchActiveInCutout = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                overlayView?.releasePullDown()
                overlayView?.releaseTrackRotation()
                isTouchActiveInCutout = false
                isTrackThresholdReached = false
                isDoubleTapPending = false
                return false
            }
        }

        return isTouchActiveInCutout
    }

    private fun handleHorizontalSlide(mode: String, currentX: Float, totalDx: Float) {
        when (mode) {
            "volume" -> {
                val deltaX = currentX - lastSlideX
                if (abs(deltaX) >= slideStepPx) {
                    val direction = if (deltaX > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
                    HapticUtil.performHapticForService(service, HapticFeedbackType.SUBTLE)
                    lastSlideX = currentX
                    isVolumeOrBrightnessAdjusted = true
                }
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                overlayView?.setInteractiveVolume(currentVol, maxVol)
            }

            "brightness" -> {
                val deltaX = currentX - lastSlideX
                if (abs(deltaX) >= slideStepPx) {
                    try {
                        val currentBrightness = Settings.System.getInt(
                            service.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            128
                        )
                        val step = if (deltaX > 0) 15 else -15
                        val newBrightness = (currentBrightness + step).coerceIn(1, 255)
                        Settings.System.putInt(
                            service.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            newBrightness
                        )
                        HapticUtil.performHapticForService(service, HapticFeedbackType.SUBTLE)
                        overlayView?.setInteractiveBrightness(newBrightness, 255)
                    } catch (_: Exception) {
                    }
                    lastSlideX = currentX
                    isVolumeOrBrightnessAdjusted = true
                }
            }

            "track" -> {
                val isInverted = settingsRepository.isDuoSlideInvertDirectionEnabled()
                val effectiveRadius = ((cameraRadiusPx + 14f * density) * ringRadiusScale).coerceAtLeast(1f)
                val baseAngleDeg = (totalDx / effectiveRadius) * (180f / Math.PI.toFloat())
                val angleDeg = if (isInverted) baseAngleDeg else -baseAngleDeg
                overlayView?.setInteractiveTrackRotation(angleDeg)
                val isPastThreshold = abs(totalDx) >= trackTriggerPx
                if (isPastThreshold && !isTrackThresholdReached) {
                    isTrackThresholdReached = true
                    HapticUtil.performHapticForService(service, HapticFeedbackType.TICK)
                } else if (!isPastThreshold && isTrackThresholdReached) {
                    isTrackThresholdReached = false
                }
            }

            "sound_mode" -> {
                val isInverted = settingsRepository.isDuoSlideInvertDirectionEnabled()
                if (!isSoundModeTriggered) {
                    if (abs(totalDx) >= horizontalTriggerPx) {
                        isSoundModeTriggered = true
                        val isForward = if (isInverted) totalDx < 0 else totalDx > 0
                        cycleSoundMode(isForward = isForward)
                        HapticUtil.performHapticForService(service, HapticFeedbackType.DOUBLE)
                    }
                }
                overlayView?.setInteractiveSoundMode(audioManager.ringerMode)
            }
        }
    }

    private fun cycleSoundMode(isForward: Boolean) {
        try {
            val currentMode = audioManager.ringerMode
            val nextMode = if (isForward) {
                when (currentMode) {
                    AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                    AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                    else -> AudioManager.RINGER_MODE_NORMAL
                }
            } else {
                when (currentMode) {
                    AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_VIBRATE
                    AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_NORMAL
                    else -> AudioManager.RINGER_MODE_SILENT
                }
            }
            audioManager.ringerMode = nextMode
        } catch (_: Exception) {
        }
    }

    private fun dispatchMediaKey(keyCode: Int) {
        try {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        } catch (_: Exception) {
        }
    }

    private fun executeAction(action: Action) {
        scope.launch {
            try {
                CombinedActionExecutor.execute(service, action)
            } catch (_: Exception) {
            }
        }
    }
}

