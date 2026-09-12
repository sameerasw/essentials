/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Handlers
 * File: StatusGlanceTouchHandler.kt
 * Description: Touch handler managing gestures on Status Glance ambient chip.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.StatusGlanceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

class StatusGlanceTouchHandler(
    private val service: AccessibilityService,
) {
    private val settingsRepository by lazy { SettingsRepository(service) }
    private val audioManager by lazy { service.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var glanceView: StatusGlanceView? = null
    var activeMediaControllerProvider: (() -> MediaController?)? = null

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downTime: Long = 0L

    private var lastTapTime: Long = 0L
    private var lastTapX: Float = 0f
    private var lastTapY: Float = 0f
    private var isDoubleTapPending: Boolean = false

    private var isTouchActive: Boolean = false
    private var isLongPressTriggered: Boolean = false
    private var isSwipeThresholdReached: Boolean = false
    private var isSwiping: Boolean = false

    private val density: Float
        get() = service.resources.displayMetrics.density

    private val touchSlopPx: Float
        get() = 10f * density

    private val swipeTriggerPx: Float
        get() = 36f * density

    private val maxSwipeDistancePx: Float
        get() = 80f * density

    private val longPressTimeoutMs = 450L
    private val doubleTapTimeoutMs = 260L

    private var pendingSingleTapRunnable: Runnable? = null

    private val longPressRunnable = Runnable {
        isLongPressTriggered = true
        val action = settingsRepository.getStatusGlanceLongPressAction()
        if (action != null) {
            HapticUtil.performHapticForService(service, HapticFeedbackType.DOUBLE)
            glanceView?.triggerTapAnimation()
            executeAction(action)
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.rawX
        val y = event.rawY

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isTouchActive = true
                downX = x
                downY = y
                downTime = SystemClock.uptimeMillis()
                isLongPressTriggered = false
                isSwipeThresholdReached = false
                isSwiping = false

                val doubleTapAction = settingsRepository.getStatusGlanceDoubleTapAction()
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

                if (settingsRepository.getStatusGlanceLongPressAction() != null) {
                    handler.postDelayed(longPressRunnable, longPressTimeoutMs)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isTouchActive) return false

                val dx = x - downX
                val dy = y - downY
                val totalDist = hypot(dx, dy)

                if (totalDist > touchSlopPx) {
                    handler.removeCallbacks(longPressRunnable)
                    isDoubleTapPending = false
                }

                if (isLongPressTriggered) return true

                val isMediaActive = isMediaCurrentlyActive()
                val isHorizontalDominant = dx > 0f && dx > abs(dy) * 1.1f

                if (isMediaActive && (isSwiping || isHorizontalDominant)) {
                    isSwiping = true
                    val clampedOffset = (dx * 0.75f).coerceIn(0f, maxSwipeDistancePx)
                    glanceView?.setSwipeArtworkOffset(clampedOffset)

                    val isPastThreshold = dx >= swipeTriggerPx
                    if (isPastThreshold && !isSwipeThresholdReached) {
                        isSwipeThresholdReached = true
                        HapticUtil.performHapticForService(service, HapticFeedbackType.TICK)
                    } else if (!isPastThreshold && isSwipeThresholdReached) {
                        isSwipeThresholdReached = false
                    }
                    return true
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isTouchActive) return false
                isTouchActive = false
                handler.removeCallbacks(longPressRunnable)
                glanceView?.releaseSwipeArtwork()

                val elapsed = SystemClock.uptimeMillis() - downTime
                val dx = x - downX
                val dy = y - downY
                val totalDist = hypot(dx, dy)

                if (isSwiping && isSwipeThresholdReached) {
                    HapticUtil.performHapticForService(service, HapticFeedbackType.DOUBLE)
                    skipToNextTrack()
                    return true
                }

                val didPerformAnyGesture = isLongPressTriggered || (isSwiping && isSwipeThresholdReached)

                if (!didPerformAnyGesture && totalDist < touchSlopPx && elapsed < 350L) {
                    val doubleTapAction = settingsRepository.getStatusGlanceDoubleTapAction()
                    val isMediaActive = isMediaCurrentlyActive()

                    if (isDoubleTapPending && doubleTapAction != null) {
                        isDoubleTapPending = false
                        lastTapTime = 0L
                        glanceView?.triggerTapAnimation()
                        HapticUtil.performHapticForService(service, HapticFeedbackType.DOUBLE)
                        executeAction(doubleTapAction)
                    } else if (doubleTapAction != null) {
                        lastTapTime = SystemClock.uptimeMillis()
                        lastTapX = downX
                        lastTapY = downY
                        val runnable = Runnable {
                            pendingSingleTapRunnable = null
                            handleSingleTap(isMediaActive)
                        }
                        pendingSingleTapRunnable = runnable
                        handler.postDelayed(runnable, doubleTapTimeoutMs)
                    } else {
                        lastTapTime = 0L
                        handleSingleTap(isMediaActive)
                    }
                }

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                isTouchActive = false
                handler.removeCallbacks(longPressRunnable)
                glanceView?.releaseSwipeArtwork()
                return true
            }
        }

        return false
    }

    private fun handleSingleTap(isMediaActive: Boolean) {
        glanceView?.triggerTapAnimation()
        HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)

        if (isMediaActive) {
            togglePlayPause()
        } else {
            val tapAction = settingsRepository.getStatusGlanceTapAction()
            if (tapAction != null) {
                executeAction(tapAction)
            }
        }
    }

    private fun isMediaCurrentlyActive(): Boolean {
        return glanceView?.isMediaPlaying == true || audioManager.isMusicActive
    }

    private fun togglePlayPause() {
        val controller = activeMediaControllerProvider?.invoke()
        if (controller != null) {
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                controller.transportControls?.pause()
            } else {
                controller.transportControls?.play()
            }
        } else {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
    }

    private fun skipToNextTrack() {
        val controller = activeMediaControllerProvider?.invoke()
        if (controller != null) {
            controller.transportControls?.skipToNext()
        } else {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
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
