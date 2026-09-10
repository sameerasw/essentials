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
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Lightweight touch anchor view positioned strictly over the camera cutout.
 * Intercepts gestures (single tap, double tap, long press, horizontal swipe)
 * only within its circular radius, passing all surrounding screen touches through.
 */
class DuoTouchAnchorView(context: Context) : View(context) {

    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    var onSwipeLeft: (() -> Unit)? = null
    var onTouchDown: (() -> Unit)? = null
    var isHapticEnabled: Boolean = true

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onSingleTap?.invoke()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap?.invoke()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
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

        if (event.action == MotionEvent.ACTION_DOWN) {
            if (dx * dx + dy * dy > radius * radius) {
                return false
            }
            performTactileHaptic()
            onTouchDown?.invoke()
        }

        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
}
