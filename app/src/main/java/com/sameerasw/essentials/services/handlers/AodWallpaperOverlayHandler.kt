/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: AodWallpaperOverlayHandler.kt
 * Description: Background service component managing AOD wallpaper overlay at 30% opacity.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.animation.ObjectAnimator
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.sameerasw.essentials.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AodWallpaperOverlayHandler(
    private val service: AccessibilityService,
) {
    private var windowManager: WindowManager? = null
    private var overlayContainer: FrameLayout? = null
    private var wallpaperImageView: ImageView? = null
    private var isOverlayAdded = false
    private var isScreenOff = false
    private var cachedWallpaperBitmap: Bitmap? = null

    private val handler = Handler(Looper.getMainLooper())
    private val handlerScope = CoroutineScope(Dispatchers.Main + Job())

    private val BURN_IN_INTERVAL_MS = 5 * 60 * 1000L
    // private val BURN_IN_INTERVAL_MS = 10 * 1500L // 10s for testing

    private val burnInShiftRunnable = object : Runnable {
        override fun run() {
            if (isOverlayAdded && isScreenOff) {
                applyBurnInShift(animated = false)
                handler.postDelayed(this, BURN_IN_INTERVAL_MS)
            }
        }
    }

    private val timeoutRunnable = Runnable {
        if (isOverlayAdded && isScreenOff) {
            hideOverlay()
        }
    }

    private val prefs by lazy {
        service.getSharedPreferences(
            SettingsRepository.PREFS_NAME,
            AccessibilityService.MODE_PRIVATE,
        )
    }

    fun onScreenOff() {
        isScreenOff = true
        updateState()
    }

    fun onScreenOn() {
        isScreenOff = false
        handler.removeCallbacks(timeoutRunnable)
        hideOverlay()
    }

    fun updateState() {
        val enabled = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_ENABLED, false)
        if (enabled && isScreenOff) {
            showOverlay()
        } else {
            hideOverlay()
        }
    }

    private fun applyBurnInShift(animated: Boolean) {
        val imageView = wallpaperImageView ?: return
        val maxShiftPx = (8 * service.resources.displayMetrics.density).toInt()
        val targetX = (-maxShiftPx..maxShiftPx).random().toFloat()
        val targetY = (-maxShiftPx..maxShiftPx).random().toFloat()

        if (animated) {
            ObjectAnimator.ofFloat(imageView, "scaleX", imageView.scaleX, 1.05f).apply {
                duration = 1000
                start()
            }
            ObjectAnimator.ofFloat(imageView, "scaleY", imageView.scaleY, 1.05f).apply {
                duration = 1000
                start()
            }
            ObjectAnimator.ofFloat(imageView, "translationX", imageView.translationX, targetX).apply {
                duration = 1000
                start()
            }
            ObjectAnimator.ofFloat(imageView, "translationY", imageView.translationY, targetY).apply {
                duration = 1000
                start()
            }
        } else {
            imageView.scaleX = 1.05f
            imageView.scaleY = 1.05f
            imageView.translationX = targetX
            imageView.translationY = targetY
        }
    }

    private fun showOverlay() {
        val opacity = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_OPACITY, 0.3f)
        if (overlayContainer == null) {
            val root = FrameLayout(service).apply {
                setBackgroundColor(Color.TRANSPARENT)
            }
            val imageView = ImageView(service).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                alpha = opacity
            }
            root.addView(imageView)
            wallpaperImageView = imageView
            overlayContainer = root
        } else {
            wallpaperImageView?.alpha = opacity
        }

        loadAndApplyWallpaper()

        if (windowManager == null) {
            windowManager = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as? WindowManager
        }

        if (!isOverlayAdded && windowManager != null && overlayContainer != null) {
            val params =
                WindowManager.LayoutParams(
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
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }

            try {
                val container = overlayContainer ?: return
                container.visibility = View.VISIBLE
                container.alpha = 0f
                applyBurnInShift(animated = false)
                windowManager?.addView(container, params)
                isOverlayAdded = true

                ObjectAnimator.ofFloat(container, "alpha", 0f, 1f).apply {
                    duration = 2000
                    start()
                }

                handler.removeCallbacks(burnInShiftRunnable)
                handler.postDelayed(burnInShiftRunnable, BURN_IN_INTERVAL_MS)
                scheduleTimeout()
            } catch (e: Exception) {
                Log.e("AodWallpaperOverlay", "Failed to add AOD wallpaper overlay", e)
            }
        } else if (isOverlayAdded && overlayContainer != null) {
            val container = overlayContainer ?: return
            container.visibility = View.VISIBLE
            ObjectAnimator.ofFloat(container, "alpha", container.alpha, 1f).apply {
                duration = 2000
                start()
            }
            handler.removeCallbacks(burnInShiftRunnable)
            handler.postDelayed(burnInShiftRunnable, BURN_IN_INTERVAL_MS)
            scheduleTimeout()
        }
    }

    private fun scheduleTimeout() {
        handler.removeCallbacks(timeoutRunnable)
        val timeoutMinutes = prefs.getInt(SettingsRepository.KEY_AOD_WALLPAPER_TIMEOUT, 3)
        if (timeoutMinutes > 0) {
            handler.postDelayed(timeoutRunnable, timeoutMinutes * 60_000L)
        }
    }

    private fun loadAndApplyWallpaper() {
        if (cachedWallpaperBitmap != null) {
            wallpaperImageView?.setImageBitmap(cachedWallpaperBitmap)
            return
        }

        handlerScope.launch(Dispatchers.IO) {
            val bitmap = extractCurrentWallpaper()
            if (bitmap != null) {
                cachedWallpaperBitmap = bitmap
                withContext(Dispatchers.Main) {
                    wallpaperImageView?.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun extractCurrentWallpaper(): Bitmap? {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(service)
            val drawable: Drawable? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.getDrawable(WallpaperManager.FLAG_LOCK)
                        ?: wallpaperManager.drawable
                } else {
                    wallpaperManager.drawable
                }

            drawableToBitmap(drawable)
        } catch (e: Exception) {
            Log.e("AodWallpaperOverlay", "Error extracting current wallpaper", e)
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 2400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun invalidateWallpaperCache() {
        cachedWallpaperBitmap = null
    }

    private fun hideOverlay() {
        handler.removeCallbacks(burnInShiftRunnable)
        handler.removeCallbacks(timeoutRunnable)
        if (isOverlayAdded && overlayContainer != null) {
            val currentView = overlayContainer ?: return
            val imageView = wallpaperImageView

            if (imageView != null) {
                if (imageView.translationX != 0f || imageView.translationY != 0f) {
                    ObjectAnimator.ofFloat(imageView, "translationX", imageView.translationX, 0f).apply {
                        duration = 500
                        start()
                    }
                    ObjectAnimator.ofFloat(imageView, "translationY", imageView.translationY, 0f).apply {
                        duration = 500
                        start()
                    }
                }
                if (imageView.scaleX != 1.0f || imageView.scaleY != 1.0f) {
                    ObjectAnimator.ofFloat(imageView, "scaleX", imageView.scaleX, 1.0f).apply {
                        duration = 500
                        start()
                    }
                    ObjectAnimator.ofFloat(imageView, "scaleY", imageView.scaleY, 1.0f).apply {
                        duration = 500
                        start()
                    }
                }
            }

            ObjectAnimator.ofFloat(currentView, "alpha", currentView.alpha, 0f).apply {
                duration = 500
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        currentView.visibility = View.GONE
                        imageView?.translationX = 0f
                        imageView?.translationY = 0f
                        imageView?.scaleX = 1.0f
                        imageView?.scaleY = 1.0f
                        try {
                            windowManager?.removeView(currentView)
                        } catch (_: Exception) {
                        }
                        isOverlayAdded = false
                    }
                })
                start()
            }
        }
    }

    fun removeOverlay() {
        handler.removeCallbacks(burnInShiftRunnable)
        handler.removeCallbacks(timeoutRunnable)
        hideOverlay()
        overlayContainer = null
        wallpaperImageView = null
        cachedWallpaperBitmap = null
    }
}
