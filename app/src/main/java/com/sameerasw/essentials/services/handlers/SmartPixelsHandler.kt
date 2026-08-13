/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: SmartPixelsHandler.kt
 * Description: Background service component for SmartPixelsHandler.kt.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.sameerasw.essentials.data.repository.SettingsRepository

class SmartPixelsHandler(private val service: AccessibilityService) {

    private var windowManager: WindowManager? = null
    private var overlayView: SmartPixelsOverlayView? = null
    private var isOverlayAdded = false

    private val handler = Handler(Looper.getMainLooper())
    private val shiftPatternRunnable = object : Runnable {
        override fun run() {
            overlayView?.shiftPattern()
            handler.postDelayed(this, 30 * 60 * 1000L) // Shift pattern every 30 minutes to prevent burn-in
        }
    }

    private val prefs by lazy {
        service.getSharedPreferences(
            SettingsRepository.PREFS_NAME,
            AccessibilityService.MODE_PRIVATE
        )
    }

    fun init() {
        windowManager =
            service.getSystemService(AccessibilityService.WINDOW_SERVICE) as? WindowManager
        updateState()
    }

    fun updateState() {
        val enabled = prefs.getBoolean(SettingsRepository.KEY_SMART_PIXELS_ENABLED, false)
        val intensity = prefs.getFloat(SettingsRepository.KEY_SMART_PIXELS_INTENSITY, 50f)

        if (enabled) {
            showOverlay(intensity)
        } else {
            hideOverlay()
        }
    }

    private fun showOverlay(intensity: Float) {
        if (overlayView == null) {
            overlayView = SmartPixelsOverlayView(service)
        }

        overlayView?.setIntensity(intensity)

        if (!isOverlayAdded && windowManager != null && overlayView != null) {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            try {
                overlayView?.alpha = 0f
                windowManager?.addView(overlayView, params)
                isOverlayAdded = true

                // Fade in animation
                ObjectAnimator.ofFloat(overlayView, "alpha", 0f, 1f).apply {
                    duration = 300
                    start()
                }

                // Schedule burn-in prevention pattern shifting
                handler.removeCallbacks(shiftPatternRunnable)
                handler.postDelayed(shiftPatternRunnable, 30 * 60 * 1000L)
            } catch (e: Exception) {
                Log.e("SmartPixelsHandler", "Failed to add Smart Pixels accessibility overlay", e)
            }
        } else if (isOverlayAdded) {
            overlayView?.invalidate()
        }
    }

    private fun hideOverlay() {
        if (isOverlayAdded && windowManager != null && overlayView != null) {
            handler.removeCallbacks(shiftPatternRunnable)
            val currentView = overlayView
            // Fade out animation before removing
            ObjectAnimator.ofFloat(currentView, "alpha", currentView?.alpha ?: 1f, 0f).apply {
                duration = 300
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        try {
                            if (isOverlayAdded && currentView != null) {
                                windowManager?.removeView(currentView)
                            }
                        } catch (e: Exception) {
                            Log.e("SmartPixelsHandler", "Failed to remove Smart Pixels accessibility overlay", e)
                        }
                        isOverlayAdded = false
                    }
                })
                start()
            }
        }
    }

    fun destroy() {
        handler.removeCallbacks(shiftPatternRunnable)
        hideOverlay()
        overlayView = null
    }

    private class SmartPixelsOverlayView(context: AccessibilityService) : View(context) {
        private val paint = Paint()
        private var cachedPatternBitmap: Bitmap? = null
        private var lastWidth = 0
        private var lastHeight = 0
        private var currentIntensity = 50f
        private var patternOffset = 0

        init {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        fun setIntensity(intensity: Float) {
            if (currentIntensity != intensity) {
                currentIntensity = intensity
                cachedPatternBitmap?.recycle()
                cachedPatternBitmap = null
                invalidate()
            }
        }

        fun shiftPattern() {
            patternOffset = (patternOffset + 1) % 4
            cachedPatternBitmap?.recycle()
            cachedPatternBitmap = null
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w != lastWidth || h != lastHeight) {
                lastWidth = w
                lastHeight = h
                cachedPatternBitmap?.recycle()
                cachedPatternBitmap = null
            }
        }

        private fun createPatternBitmap(w: Int, h: Int): Bitmap {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val p = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = false
            }
            val alphaValue = (currentIntensity / 100f * 255).toInt().coerceIn(20, 230)
            p.alpha = alphaValue

            val step = when {
                currentIntensity >= 75f -> 2
                currentIntensity >= 50f -> 3
                currentIntensity >= 30f -> 4
                else -> 6
            }

            val offsetX = (patternOffset % 2) * step
            val offsetY = (patternOffset / 2) * step

            var y = 0
            while (y < h) {
                var x = ((y / step % 2) * step + offsetX) % (step * 2)
                while (x < w) {
                    canvas.drawRect(
                        x.toFloat(),
                        (y + offsetY).toFloat(),
                        (x + step).toFloat(),
                        (y + offsetY + step).toFloat(),
                        p
                    )
                    x += step * 2
                }
                y += step
            }
            return bmp
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width
            val h = height
            if (w <= 0 || h <= 0) return

            var bitmap = cachedPatternBitmap
            if (bitmap == null || bitmap.isRecycled || bitmap.width != w || bitmap.height != h) {
                bitmap = createPatternBitmap(w, h)
                cachedPatternBitmap = bitmap
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            cachedPatternBitmap?.recycle()
            cachedPatternBitmap = null
        }
    }
}
