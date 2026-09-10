/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays
 * File: DuoTouchAnchorView.kt
 * Description: Circular touch anchor view positioned over the camera cutout for gesture dispatching.
 */

package com.sameerasw.essentials.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Lightweight touch anchor view positioned strictly over the camera cutout.
 * Intercepts gestures (single tap, double tap, long press, horizontal swipe,
 * and virtual rotary dial dragging) only within its circular radius, passing
 * all surrounding screen touches through.
 */
class DuoTouchAnchorView(context: Context) : View(context) {

    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    var onSwipeLeft: (() -> Unit)? = null
    var onTouchDown: (() -> Unit)? = null
    var onRotaryStart: (() -> Unit)? = null
    var onRotaryDelta: ((deltaAngle: Float) -> Unit)? = null
    var onRotaryEnd: (() -> Unit)? = null
    var onLongPressProgress: ((fraction: Float) -> Unit)? = null

    var isHapticEnabled: Boolean = true
    var isRotaryEnabled: Boolean = true

    private var isTouchActive = false
    private var isRotaryActive = false
    private var touchDownTime = 0L
    private var lastAngleDeg = 0f
    private var accumulatedRotaryAngle = 0f
    private var hapticAngleAccumulator = 0f

    private val longPressProgressRunnable = object : Runnable {
        override fun run() {
            if (!isTouchActive || isRotaryActive) return
            val elapsed = SystemClock.uptimeMillis() - touchDownTime
            val fraction = (elapsed / 500f).coerceIn(0f, 1f)
            onLongPressProgress?.invoke(fraction)
            if (elapsed < 500L) {
                postDelayed(this, 16L)
            }
        }
    }

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onSingleTap?.invoke()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap?.invoke()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                removeCallbacks(longPressProgressRunnable)
                onLongPressProgress?.invoke(1.0f)
                performTactileHaptic(HapticFeedbackConstants.LONG_PRESS)
                onLongPress?.invoke()
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                if (e1 == null) return false
                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y

                if (abs(deltaX) > abs(deltaY) && abs(velocityX) > 400f) {
                    if (deltaX > 0) {
                        onSwipeRight?.invoke()
                        return true
                    } else {
                        onSwipeLeft?.invoke()
                        return true
                    }
                }
                return false
            }
        },
    )

    fun performTactileHaptic(feedbackConstant: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
        if (isHapticEnabled) {
            performHapticFeedback(feedbackConstant)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = width.coerceAtMost(height) / 2f
        val dx = event.x - centerX
        val dy = event.y - centerY
        val dist = hypot(dx, dy)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (dist > radius) {
                    return false
                }
                isTouchActive = true
                isRotaryActive = false
                accumulatedRotaryAngle = 0f
                hapticAngleAccumulator = 0f
                touchDownTime = SystemClock.uptimeMillis()
                lastAngleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                performTactileHaptic()
                onTouchDown?.invoke()
                removeCallbacks(longPressProgressRunnable)
                post(longPressProgressRunnable)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isTouchActive) return false
                val currentAngleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                var delta = currentAngleDeg - lastAngleDeg
                if (delta > 180f) delta -= 360f
                else if (delta < -180f) delta += 360f
                lastAngleDeg = currentAngleDeg

                if (isRotaryEnabled && dist >= radius * 0.25f && dist <= radius * 1.35f) {
                    if (!isRotaryActive) {
                        accumulatedRotaryAngle += abs(delta)
                        if (accumulatedRotaryAngle >= 16f) {
                            isRotaryActive = true
                            removeCallbacks(longPressProgressRunnable)
                            onLongPressProgress?.invoke(0f)
                            onRotaryStart?.invoke()
                        }
                    }
                    if (isRotaryActive) {
                        onRotaryDelta?.invoke(delta)
                        hapticAngleAccumulator += delta
                        if (abs(hapticAngleAccumulator) >= 12f) {
                            performTactileHaptic(HapticFeedbackConstants.CLOCK_TICK)
                            hapticAngleAccumulator = 0f
                        }
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasRotary = isRotaryActive
                isTouchActive = false
                isRotaryActive = false
                removeCallbacks(longPressProgressRunnable)
                onLongPressProgress?.invoke(0f)
                if (wasRotary) {
                    onRotaryEnd?.invoke()
                    return true
                }
            }
        }

        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(longPressProgressRunnable)
    }
}
