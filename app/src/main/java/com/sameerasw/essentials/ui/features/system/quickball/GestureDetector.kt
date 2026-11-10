/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Gestures
 * File: GestureDetector.kt
 * Description: Recognizes single tap, double tap, triple tap, long press, and vertical swipe gestures.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.hypot

interface GestureListener {
    fun onTouchDown() {}
    fun onTouchCancel() {}
    fun onSingleTap() {}
    fun onDoubleTap() {}
    fun onTripleTap() {}
    fun onLongPress() {}
    fun onSwipeUp() {}
    fun onSwipeDown() {}
    fun onDragMove(dx: Float, dy: Float) {}
    fun onDragEnd() {}
    fun onFlickStart() {}
    fun onFlickMove(rawX: Float, rawY: Float) {}
    fun onFlickEnd() {}
}

class QuickBallGestureDetector(
    context: Context,
    var listener: GestureListener? = null,
    var isGestureEnabled: () -> Boolean = { true },
) {
    private val handler = Handler(Looper.getMainLooper())
    private val swipeThreshold = 24f * context.resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val tapTimeoutMs = 220L
    private val longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong()

    private var startX = 0f
    private var startY = 0f
    private var downTime = 0L
    private var isSwipeTriggered = false
    private var isLongPressTriggered = false
    private var tapCount = 0

    private val tapRunnable =
        Runnable {
            when (tapCount) {
                1 -> listener?.onSingleTap()
                2 -> listener?.onDoubleTap()
            }
            tapCount = 0
        }

    private val longPressRunnable =
        Runnable {
            isLongPressTriggered = true
            handler.removeCallbacks(tapRunnable)
            tapCount = 0
            listener?.onLongPress()
        }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isGestureEnabled()) {
            return when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    listener?.onTouchDown()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    listener?.onSingleTap()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    listener?.onTouchCancel()
                    true
                }
                else -> false
            }
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                listener?.onTouchDown()
                startX = event.rawX
                startY = event.rawY
                downTime = System.currentTimeMillis()
                isSwipeTriggered = false
                isLongPressTriggered = false
                handler.removeCallbacks(longPressRunnable)
                handler.postDelayed(longPressRunnable, longPressTimeoutMs)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isSwipeTriggered || isLongPressTriggered) return true

                val dx = event.rawX - startX
                val dy = event.rawY - startY

                if (hypot(dx, dy) > touchSlop) {
                    handler.removeCallbacks(longPressRunnable)
                }

                if (abs(dy) > swipeThreshold && abs(dy) > abs(dx) * 1.8f) {
                    isSwipeTriggered = true
                    handler.removeCallbacks(longPressRunnable)
                    handler.removeCallbacks(tapRunnable)
                    tapCount = 0

                    if (dy < 0) {
                        listener?.onSwipeUp()
                    } else {
                        listener?.onSwipeDown()
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                if (!isSwipeTriggered && !isLongPressTriggered && event.action == MotionEvent.ACTION_UP) {
                    val duration = System.currentTimeMillis() - downTime
                    if (duration < ViewConfiguration.getLongPressTimeout()) {
                        tapCount++
                        handler.removeCallbacks(tapRunnable)
                        if (tapCount >= 3) {
                            listener?.onTripleTap()
                            tapCount = 0
                        } else {
                            handler.postDelayed(tapRunnable, tapTimeoutMs)
                        }
                    }
                } else if (event.action == MotionEvent.ACTION_CANCEL) {
                    listener?.onTouchCancel()
                }
                isSwipeTriggered = false
                isLongPressTriggered = false
                return true
            }

            else -> return false
        }
    }

    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
    }
}
