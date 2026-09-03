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
                windowManager?.addView(container, params)
                isOverlayAdded = true

                ObjectAnimator.ofFloat(container, "alpha", 0f, 1f).apply {
                    duration = 250
                    start()
                }
            } catch (e: Exception) {
                Log.e("AodWallpaperOverlay", "Failed to add AOD wallpaper overlay", e)
            }
        } else if (isOverlayAdded && overlayContainer != null) {
            overlayContainer?.visibility = View.VISIBLE
            overlayContainer?.alpha = 1f
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
        if (isOverlayAdded && overlayContainer != null) {
            val currentView = overlayContainer ?: return
            currentView.visibility = View.GONE
            try {
                windowManager?.removeView(currentView)
            } catch (_: Exception) {
            }
            isOverlayAdded = false
        }
    }

    fun removeOverlay() {
        hideOverlay()
        overlayContainer = null
        wallpaperImageView = null
        cachedWallpaperBitmap = null
    }
}
