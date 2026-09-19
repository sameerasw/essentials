/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Handlers
 * File: IslandTouchHandler.kt
 * Description: Touch handler managing gestures (tap to open, swipe up to dismiss) on Island overlay.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.widget.Toast
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.model.ActiveNotificationAlert
import com.sameerasw.essentials.domain.model.NotificationActionItem
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.IslandOverlayView
import com.sameerasw.essentials.utils.island.FlashlightHit
import com.sameerasw.essentials.utils.island.IslandSwipeDirections
import kotlin.math.abs
import kotlin.math.hypot

class IslandTouchHandler(
    private val service: AccessibilityService,
) {
    private val settingsRepository by lazy { SettingsRepository(service) }
    var overlayView: IslandOverlayView? = null

    var onNotificationDismissRequested: (() -> Unit)? = null
    var onNotificationSwitched: (() -> Unit)? = null
    var onNotificationExpandToggled: ((Boolean) -> Unit)? = null
    var onCatchUpRestored: (() -> Unit)? = null
    var onMediaDismissRequested: (() -> Unit)? = null
    var onMediaTapped: (() -> Unit)? = null
    var onCalendarToggled: (() -> Unit)? = null
    var onMediaFullPlayerToggled: ((Boolean) -> Unit)? = null
    var onMediaControlTapped: ((Int) -> Unit)? = null
    var onMediaBackgroundTapped: (() -> Unit)? = null
    var onFlashlightLevelChanged: ((Float) -> Unit)? = null
    var onFlashlightTurnOff: (() -> Unit)? = null
    var onConsciousGateToggled: (() -> Unit)? = null

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downTime: Long = 0L
    private var isTouchActive: Boolean = false
    private var isLongPressed: Boolean = false
    private var isDragging: Boolean = false
    private var lastHapticDist: Float = 0f
    private var hasTriggeredThresholdHaptic: Boolean = false
    private var isMediaTouch: Boolean = false

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val isPlainMediaLongPressEligible: Boolean
        get() = isMediaTouch &&
            overlayView?.isMediaPlaybackActive == true &&
            overlayView?.isNotificationAlertActive == false &&
            overlayView?.isCalendarActive == false

    private val isLongPressEligible: Boolean
        get() = (overlayView?.isNotificationAlertActive == true && overlayView?.isCatchUpMode != true) ||
            isPlainMediaLongPressEligible

    private val longPressRampRunnable1 = Runnable {
        if (isTouchActive && !isDragging && isLongPressEligible) {
            HapticUtil.performCustomHaptic(service, 0.18f)
        }
    }

    private val longPressRampRunnable2 = Runnable {
        if (isTouchActive && !isDragging && isLongPressEligible) {
            HapticUtil.performCustomHaptic(service, 0.42f)
        }
    }

    private val longPressRampRunnable3 = Runnable {
        if (isTouchActive && !isDragging && isLongPressEligible) {
            HapticUtil.performCustomHaptic(service, 0.70f)
        }
    }

    private val longPressRunnable = Runnable {
        if (isTouchActive && !isDragging && overlayView?.isNotificationAlertActive == true && overlayView?.isCatchUpMode != true) {
            isLongPressed = true
            HapticUtil.performStrongTickHaptic(service)
            if (settingsRepository.getIslandTapAction() == SettingsRepository.ISLAND_TAP_ACTION_EXPAND) {
                val alert = overlayView?.getActiveNotificationAlert()
                if (alert != null) {
                    launchNotificationApp(alert)
                    dismissNotification()
                }
            } else {
                val isNowExpanded = overlayView?.toggleExpansion() ?: false
                onNotificationExpandToggled?.invoke(isNowExpanded)
            }
        } else if (isTouchActive && !isDragging && isPlainMediaLongPressEligible) {
            isLongPressed = true
            HapticUtil.performStrongTickHaptic(service)
            if (settingsRepository.getIslandTapAction() == SettingsRepository.ISLAND_TAP_ACTION_EXPAND) {
                onMediaBackgroundTapped?.invoke()
            } else {
                val isNowExpanded = overlayView?.toggleMediaFullPlayer() ?: false
                onMediaFullPlayerToggled?.invoke(isNowExpanded)
            }
        }
    }

    private val density: Float
        get() = service.resources.displayMetrics.density

    private val touchSlopPx: Float
        get() = 10f * density

    private val graceAreaPx: Float
        get() = 12f * density

    private var flashlightHit = FlashlightHit.NONE
    private var isFlashlightDragging = false
    private var lastFlashlightStep = -1
    private var isFlashlightLongPressed = false

    private val flashlightRamps = listOf(110L to 0.18f, 210L to 0.42f, 300L to 0.70f).map { (_, strength) ->
        Runnable { if (!isFlashlightDragging && !isFlashlightLongPressed) HapticUtil.performCustomHaptic(service, strength) }
    }

    private val flashlightLongPressRunnable = Runnable {
        if (isFlashlightDragging) return@Runnable
        isFlashlightLongPressed = true
        HapticUtil.performStrongTickHaptic(service)
        onFlashlightTurnOff?.invoke()
    }

    private fun cancelFlashlightLongPress() {
        mainHandler.removeCallbacks(flashlightLongPressRunnable)
        flashlightRamps.forEach { mainHandler.removeCallbacks(it) }
    }

    private fun handleFlashlightTouch(event: MotionEvent): Boolean {
        val ov = overlayView ?: return false
        val x = event.rawX
        val y = event.rawY
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                downTime = SystemClock.uptimeMillis()
                flashlightHit = ov.flashlightHitTest(x, y)
                isFlashlightDragging = false
                isFlashlightLongPressed = false
                cancelFlashlightLongPress()
                if (flashlightHit == FlashlightHit.SLIDER && ov.flashlightSupportsLevels) {
                    isFlashlightDragging = true
                    lastFlashlightStep = -1
                    ov.setFlashlightDragging(true)
                    dispatchFlashlightLevel(ov, x)
                } else {
                    val delays = longArrayOf(110L, 210L, 300L)
                    flashlightRamps.forEachIndexed { i, r -> mainHandler.postDelayed(r, delays[i]) }
                    mainHandler.postDelayed(flashlightLongPressRunnable, 370L)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isFlashlightDragging) {
                    dispatchFlashlightLevel(ov, x)
                } else if (hypot(x - downX, y - downY) > graceAreaPx) {
                    cancelFlashlightLongPress()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelFlashlightLongPress()
                if (isFlashlightLongPressed) {
                    isFlashlightLongPressed = false
                } else if (isFlashlightDragging) {
                    isFlashlightDragging = false
                    ov.setFlashlightDragging(false)
                    HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                } else if (event.actionMasked == MotionEvent.ACTION_UP &&
                    hypot(x - downX, y - downY) < touchSlopPx * 2.0f &&
                    SystemClock.uptimeMillis() - downTime < 600L
                ) {
                    when (flashlightHit) {
                        FlashlightHit.ICON -> {
                            HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                            onFlashlightTurnOff?.invoke()
                        }
                        FlashlightHit.PILL, FlashlightHit.SLIDER -> {
                            HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                            ov.toggleFlashlightExpansion()
                        }
                        FlashlightHit.NONE -> {}
                    }
                }
            }
        }
        return true
    }

    private fun dispatchFlashlightLevel(ov: IslandOverlayView, x: Float) {
        val fraction = ov.flashlightFractionAt(x)
        ov.setFlashlightLevelFraction(fraction)
        onFlashlightLevelChanged?.invoke(fraction)
        val step = (fraction * 20f).toInt()
        if (step != lastFlashlightStep) {
            lastFlashlightStep = step
            HapticUtil.performCustomHaptic(service, 0.2f)
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (overlayView?.isFlashlightActive == true) return handleFlashlightTouch(event)
        val x = event.rawX
        val y = event.rawY

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                downTime = SystemClock.uptimeMillis()
                isTouchActive = true
                isLongPressed = false
                isDragging = false
                lastHapticDist = 0f
                hasTriggeredThresholdHaptic = false
                isMediaTouch = overlayView?.isPointInsideMedia(x, y) == true

                mainHandler.removeCallbacks(longPressRunnable)
                mainHandler.removeCallbacks(longPressRampRunnable1)
                mainHandler.removeCallbacks(longPressRampRunnable2)
                mainHandler.removeCallbacks(longPressRampRunnable3)

                mainHandler.postDelayed(longPressRampRunnable1, 110L)
                mainHandler.postDelayed(longPressRampRunnable2, 210L)
                mainHandler.postDelayed(longPressRampRunnable3, 300L)
                mainHandler.postDelayed(longPressRunnable, 370L)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isTouchActive) return false
                val dx = x - downX
                val dy = y - downY
                val dist = hypot(dx, dy)

                if (dist > graceAreaPx) {
                    mainHandler.removeCallbacks(longPressRunnable)
                    mainHandler.removeCallbacks(longPressRampRunnable1)
                    mainHandler.removeCallbacks(longPressRampRunnable2)
                    mainHandler.removeCallbacks(longPressRampRunnable3)

                    if (!isLongPressed) {
                        isDragging = true
                        val cameraX = overlayView?.cameraCenterX ?: (service.resources.displayMetrics.widthPixels / 2f)
                        val isTowardCamera = when {
                            downX < cameraX -> dx > 0f
                            downX > cameraX -> dx < 0f
                            else -> false
                        }

                        // Subtle relative haptic ticks as finger moves
                        val hapticStepPx = 16f * density
                        if (abs(dist - lastHapticDist) >= hapticStepPx) {
                            lastHapticDist = dist
                            HapticUtil.performCustomHaptic(service, 0.20f)
                        }

                        // Threshold trigger point haptic check
                        val isThresholdReached = abs(dx) > (touchSlopPx * 2.2f) || dy < (-touchSlopPx * 1.8f)
                        if (isThresholdReached && !hasTriggeredThresholdHaptic) {
                            hasTriggeredThresholdHaptic = true
                            HapticUtil.performCustomHaptic(service, 0.85f)
                        } else if (!isThresholdReached && hasTriggeredThresholdHaptic) {
                            hasTriggeredThresholdHaptic = false
                            HapticUtil.performCustomHaptic(service, 0.12f)
                        }

                        if (isTowardCamera || dy < -touchSlopPx) {
                            val dragDistTowardsCamera = when {
                                downX < cameraX -> (dx - graceAreaPx).coerceAtLeast(0f)
                                downX > cameraX -> (-dx - graceAreaPx).coerceAtLeast(0f)
                                else -> (abs(dx) - graceAreaPx).coerceAtLeast(0f)
                            }
                            val maxDragDist = 120f * density
                            val dragFraction = (dragDistTowardsCamera / maxDragDist).coerceIn(0f, 1f)
                            overlayView?.updateDragTranslation(0f, 0f)
                            if (overlayView?.isNotificationAlertActive == true) {
                                overlayView?.updateDragCollapseFraction(dragFraction, IslandOverlayView.DragCollapseTarget.CAMERA)
                            } else if (overlayView?.isMediaPlaybackActive == true && overlayView?.isMediaCompact == false) {
                                overlayView?.updateMediaCompactFraction(dragFraction)
                            } else if (overlayView?.isConsciousGateActive == true && overlayView?.isConsciousGateCompact == false) {
                                overlayView?.updateConsciousGateCompactFraction(dragFraction)
                            }
                        } else {
                            val effectiveDx = if (dx > 0) dx - graceAreaPx else dx + graceAreaPx
                            overlayView?.updateDragCollapseFraction(0f)
                            overlayView?.updateDragTranslation(effectiveDx, 0f)
                        }
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                mainHandler.removeCallbacks(longPressRunnable)
                mainHandler.removeCallbacks(longPressRampRunnable1)
                mainHandler.removeCallbacks(longPressRampRunnable2)
                mainHandler.removeCallbacks(longPressRampRunnable3)

                if (!isTouchActive) return false

                if (isLongPressed) {
                    isTouchActive = false
                    isLongPressed = false
                    isDragging = false
                    overlayView?.resetDragOffset()
                    return true
                }

                val elapsed = SystemClock.uptimeMillis() - downTime
                val dx = x - downX
                val dy = y - downY
                val totalDist = hypot(dx, dy)

                if (isMediaTouch && overlayView?.isMediaPlaybackActive == true) {
                    val controlAction = overlayView?.getMediaControlActionAt(x, y) ?: -1
                    val cameraX = overlayView?.cameraCenterX ?: (service.resources.displayMetrics.widthPixels / 2f)
                    val swipe = IslandSwipeDirections.classify(downX, dx, dy, cameraX, touchSlopPx)
                    val isSwipeTowardCamera = swipe.isTowardCamera || (swipe.isSwipeUp && overlayView?.isMediaFullPlayerActive != true)
                    val currentCompactFraction = overlayView?.mediaCompactFraction ?: 0f

                    if (controlAction >= 0 && totalDist < touchSlopPx * 2.0f && elapsed < 600L) {
                        overlayView?.resetDragOffset()
                        onMediaControlTapped?.invoke(controlAction)
                        HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                    } else if (overlayView?.isMediaFullPlayerActive == true) {
                        overlayView?.resetDragOffset()
                        if (totalDist < touchSlopPx * 2.0f && elapsed < 600L) {
                            if (settingsRepository.getIslandTapAction() == SettingsRepository.ISLAND_TAP_ACTION_EXPAND) {
                                val isNowExpanded = overlayView?.toggleMediaFullPlayer() ?: false
                                onMediaFullPlayerToggled?.invoke(isNowExpanded)
                            } else {
                                onMediaBackgroundTapped?.invoke()
                            }
                            HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                        }
                    } else if (isDragging && overlayView?.isMediaCompact == false) {
                        val shouldCompact = isSwipeTowardCamera || currentCompactFraction > 0.35f
                        if (shouldCompact) {
                            HapticUtil.performRumbleHaptic(service)
                            HapticUtil.performStrongTickHaptic(service)
                            overlayView?.animateMediaToCompact(true)
                        } else {
                            overlayView?.animateMediaToCompact(false)
                        }
                    } else if (isSwipeTowardCamera && overlayView?.isMediaCompact == false) {
                        HapticUtil.performRumbleHaptic(service)
                        HapticUtil.performStrongTickHaptic(service)
                        overlayView?.animateMediaToCompact(true)
                    } else if (totalDist < touchSlopPx * 2.0f && elapsed < 600L) {
                        overlayView?.resetDragOffset()
                        if (overlayView?.isMediaCompact == true) {
                            overlayView?.setMediaCompact(false)
                            onMediaTapped?.invoke()
                        } else {
                            if (settingsRepository.getIslandTapAction() == SettingsRepository.ISLAND_TAP_ACTION_EXPAND) {
                                val isNowExpanded = overlayView?.toggleMediaFullPlayer() ?: false
                                onMediaFullPlayerToggled?.invoke(isNowExpanded)
                            } else {
                                onMediaBackgroundTapped?.invoke()
                            }
                        }
                        HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                    } else {
                        overlayView?.resetDragOffset()
                    }
                } else if (overlayView?.isNotificationAlertActive == true) {
                    val cameraX = overlayView?.cameraCenterX ?: (service.resources.displayMetrics.widthPixels / 2f)
                    val swipe = IslandSwipeDirections.classify(downX, dx, dy, cameraX, touchSlopPx)
                    val isSwipeUp = swipe.isSwipeUp
                    val isSwipeTowardCamera = swipe.isTowardCamera
                    val isSwipeAwayFromCamera = swipe.isAwayFromCamera

                    if (isSwipeAwayFromCamera && overlayView?.isCatchUpMode != true) {
                        HapticUtil.performRumbleHaptic(service)
                        HapticUtil.performStrongTickHaptic(service)

                        val alert = overlayView?.getActiveNotificationAlert()
                        if (alert != null) {
                            NotificationListener.dismissNotification(alert.key)
                        }

                        val dir = if (dx > 0f) 1f else -1f
                        overlayView?.animateHorizontalSwipeDismiss(dir) {
                            val hasNext = overlayView?.advanceToNextNotification() ?: false
                            if (hasNext) {
                                onNotificationSwitched?.invoke()
                            } else {
                                dismissNotification()
                            }
                        }
                    } else if (isSwipeUp || isSwipeTowardCamera) {
                        HapticUtil.performRumbleHaptic(service)
                        HapticUtil.performStrongTickHaptic(service)

                        overlayView?.animateDragDismissCollapse(IslandOverlayView.DragCollapseTarget.CAMERA) {
                            val hasNext = overlayView?.advanceToNextNotification() ?: false
                            if (hasNext) {
                                onNotificationSwitched?.invoke()
                            } else {
                                dismissNotification()
                            }
                        }
                    } else if (isDragging) {
                        overlayView?.animateDragSnapBack()
                    } else if (totalDist < touchSlopPx * 2.0f && elapsed < 600L && settingsRepository.isIslandTapActionEnabled()) {
                        overlayView?.resetDragOffset()

                        if (overlayView?.isCatchUpMode == true) {
                            overlayView?.canEnterCatchUp = false
                            overlayView?.exitCatchUpMode()
                            HapticUtil.performStrongTickHaptic(service)
                            onCatchUpRestored?.invoke()
                        } else {
                            val action = overlayView?.getActionAt(x, y)

                            if (action != null) {
                                handleNotificationAction(action)
                            } else {
                                val queuedIdx = overlayView?.getQueuedAlertIndexAt(x, y) ?: -1
                                if (queuedIdx >= 0) {
                                    val switched = overlayView?.switchToQueuedNotification(queuedIdx) ?: false
                                    if (switched) {
                                        onNotificationSwitched?.invoke()
                                        HapticUtil.performStrongTickHaptic(service)
                                    }
                                } else if (settingsRepository.getIslandTapAction() == SettingsRepository.ISLAND_TAP_ACTION_EXPAND) {
                                    HapticUtil.performStrongTickHaptic(service)
                                    val isNowExpanded = overlayView?.toggleExpansion() ?: false
                                    onNotificationExpandToggled?.invoke(isNowExpanded)
                                } else {
                                    val alert = overlayView?.getActiveNotificationAlert()
                                    if (alert != null) {
                                        launchNotificationApp(alert)
                                        dismissNotification()
                                    }
                                    HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                                }
                            }
                        }
                    } else {
                        overlayView?.animateDragSnapBack()
                    }
                } else if (overlayView?.isConsciousGateActive == true) {
                    val cameraX = overlayView?.cameraCenterX ?: (service.resources.displayMetrics.widthPixels / 2f)
                    val swipe = IslandSwipeDirections.classify(downX, dx, dy, cameraX, touchSlopPx)
                    val isSwipeTowardCamera = swipe.isTowardCamera || swipe.isSwipeUp
                    val currentCompactFraction = overlayView?.consciousGateCompactFraction ?: 0f

                    if (isDragging && overlayView?.isConsciousGateCompact == false) {
                        val shouldCompact = isSwipeTowardCamera || currentCompactFraction > 0.35f
                        if (shouldCompact) {
                            HapticUtil.performRumbleHaptic(service)
                            HapticUtil.performStrongTickHaptic(service)
                            overlayView?.animateConsciousGateToCompact(true)
                            onConsciousGateToggled?.invoke()
                        } else {
                            overlayView?.animateConsciousGateToCompact(false)
                        }
                    } else if (isSwipeTowardCamera && overlayView?.isConsciousGateCompact == false) {
                        HapticUtil.performRumbleHaptic(service)
                        HapticUtil.performStrongTickHaptic(service)
                        overlayView?.animateConsciousGateToCompact(true)
                        onConsciousGateToggled?.invoke()
                    } else if (totalDist < touchSlopPx * 2.0f && elapsed < 600L) {
                        overlayView?.resetDragOffset()
                        overlayView?.toggleConsciousGateExpansion()
                        onConsciousGateToggled?.invoke()
                        HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                    } else if (isDragging) {
                        overlayView?.animateDragSnapBack()
                    } else {
                        overlayView?.resetDragOffset()
                    }
                } else if (overlayView?.isCalendarActive == true) {
                    if (totalDist < touchSlopPx * 2.0f && elapsed < 600L) {
                        overlayView?.toggleCalendarExpansion()
                        onCalendarToggled?.invoke()
                        HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                    }
                } else if (overlayView?.isMediaPlaybackActive == true) {
                    val cameraX = overlayView?.cameraCenterX ?: (service.resources.displayMetrics.widthPixels / 2f)
                    val isSwipeTowardCamera = IslandSwipeDirections.classify(downX, dx, dy, cameraX, touchSlopPx).isTowardCamera

                    if (isSwipeTowardCamera) {
                        HapticUtil.performRumbleHaptic(service)
                        HapticUtil.performStrongTickHaptic(service)
                        overlayView?.animateDragDismissCollapse(IslandOverlayView.DragCollapseTarget.COMPACT) {
                            overlayView?.setMediaCompact(true)
                        }
                    } else if (totalDist < touchSlopPx * 2.0f && elapsed < 600L) {
                        overlayView?.resetDragOffset()
                        overlayView?.setMediaCompact(false)
                        onMediaTapped?.invoke()
                        HapticUtil.performHapticForService(service, HapticFeedbackType.CLICK)
                    } else if (isDragging) {
                        overlayView?.animateDragSnapBack()
                    } else {
                        overlayView?.resetDragOffset()
                    }
                } else {
                    overlayView?.resetDragOffset()
                }

                isTouchActive = false
                isLongPressed = false
                isDragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
                if (overlayView?.isMediaPlaybackActive == true && overlayView?.isMediaCompact == false && overlayView?.isNotificationAlertActive != true) {
                    overlayView?.animateMediaToCompact(false)
                } else if (overlayView?.isConsciousGateActive == true && overlayView?.isConsciousGateCompact == false && overlayView?.isNotificationAlertActive != true) {
                    overlayView?.animateConsciousGateToCompact(false)
                } else {
                    overlayView?.animateDragSnapBack()
                }
                isTouchActive = false
                isLongPressed = false
                isDragging = false
                return false
            }
        }
        return false
    }

    private fun dismissNotification() {
        onNotificationDismissRequested?.invoke()
    }

    private fun launchNotificationApp(alert: ActiveNotificationAlert) {
        val pendingIntent = alert.contentIntent
        if (pendingIntent != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val options = ActivityOptions.makeBasic().apply {
                        pendingIntentBackgroundActivityStartMode =
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    }
                    pendingIntent.send(service, 0, null, null, null, null, options.toBundle())
                } else {
                    pendingIntent.send()
                }
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            val pm = service.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(alert.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                service.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleNotificationAction(action: NotificationActionItem) {
        HapticUtil.performStrongTickHaptic(service)

        if (action.isQuickReply) {
            // Dismiss overlay and expand the notification shade for native system typing
            dismissNotification()
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        } else {
            val notifListener = NotificationListener.instance
            if (action.pendingIntent != null && notifListener != null) {
                notifListener.performNotificationAction(action)
            }

            overlayView?.animateActionExecution(action) {
                val hasNext = overlayView?.advanceToNextNotification() ?: false
                if (hasNext) {
                    onNotificationSwitched?.invoke()
                }
            }
        }
    }
}
