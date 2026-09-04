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
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
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

    private val wallpaperChangeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            invalidateWallpaperCache()
        }
    }

    private val wallpaperColorsListener =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            WallpaperManager.OnColorsChangedListener { _, _ ->
                invalidateWallpaperCache()
            }
        } else {
            null
        }

    private var isReceiverRegistered = false

    init {
        registerWallpaperChangeListeners()
    }

    private fun registerWallpaperChangeListeners() {
        if (!isReceiverRegistered) {
            try {
                val filter = android.content.IntentFilter(Intent.ACTION_WALLPAPER_CHANGED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    service.registerReceiver(
                        wallpaperChangeReceiver,
                        filter,
                        AccessibilityService.RECEIVER_NOT_EXPORTED,
                    )
                } else {
                    service.registerReceiver(wallpaperChangeReceiver, filter)
                }
                isReceiverRegistered = true
            } catch (e: Exception) {
                Log.e("AodWallpaperOverlay", "Failed to register wallpaper broadcast receiver", e)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wallpaperColorsListener?.let {
                try {
                    val wm = WallpaperManager.getInstance(service)
                    wm.addOnColorsChangedListener(it, handler)
                } catch (e: Exception) {
                    Log.e("AodWallpaperOverlay", "Failed to add wallpaper colors changed listener", e)
                }
            }
        }
    }

    private fun unregisterWallpaperChangeListeners() {
        if (isReceiverRegistered) {
            try {
                service.unregisterReceiver(wallpaperChangeReceiver)
            } catch (e: Exception) {
                // Ignore
            }
            isReceiverRegistered = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wallpaperColorsListener?.let {
                try {
                    val wm = WallpaperManager.getInstance(service)
                    wm.removeOnColorsChangedListener(it)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
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

    private fun applyBlurEffect(imageView: ImageView, blurRadius: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (blurRadius > 0f) {
                val effect = android.graphics.RenderEffect.createBlurEffect(
                    blurRadius * 4f,
                    blurRadius * 4f,
                    Shader.TileMode.CLAMP,
                )
                imageView.setRenderEffect(effect)
            } else {
                imageView.setRenderEffect(null)
            }
        }
    }

    private fun buildMaskedContainer(vignetteIntensity: Float): FrameLayout {
        return object : FrameLayout(service) {
            private val vignettePaint = android.graphics.Paint().apply {
                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
                isAntiAlias = true
            }

            override fun dispatchDraw(canvas: android.graphics.Canvas) {
                if (width == 0 || height == 0) {
                    super.dispatchDraw(canvas)
                    return
                }

                val intensity = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_VIGNETTE, 0f)
                val hasVignette = intensity > 0f

                val saveCount = if (hasVignette) {
                    canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
                } else {
                    -1
                }

                super.dispatchDraw(canvas)

                if (hasVignette) {
                    val cx = width / 2f
                    val cy = height / 2f
                    val radius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
                    val edgeAlpha = ((1f - intensity / 100f).coerceIn(0f, 1f) * 255).toInt()
                    vignettePaint.shader = RadialGradient(
                        cx, cy, radius,
                        intArrayOf(
                            Color.BLACK,
                            Color.BLACK,
                            Color.argb(edgeAlpha, 0, 0, 0),
                        ),
                        floatArrayOf(0f, 0.45f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
                    canvas.restoreToCount(saveCount)
                }
            }
        }.apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun showOverlay() {
        val opacity = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_OPACITY, 0.3f)
        val blurRadius = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_BLUR, 0f)
        val blackThreshold = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_BLACK_THRESHOLD, 15f)

        val luminanceFilter = android.graphics.ColorMatrixColorFilter(
            android.graphics.ColorMatrix(
                floatArrayOf(
                    1.2f, 0f, 0f, 0f, 0f,
                    0f, 1.2f, 0f, 0f, 0f,
                    0f, 0f, 1.2f, 0f, 0f,
                    0.5f, 1.5f, 0.2f, 0f, -blackThreshold,
                )
            )
        )

        if (overlayContainer == null) {
            val root = buildMaskedContainer(
                prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_VIGNETTE, 0f)
            )
            val imageView = ImageView(service).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                alpha = opacity
                colorFilter = luminanceFilter
            }
            applyBlurEffect(imageView, blurRadius)
            root.addView(imageView)
            wallpaperImageView = imageView
            overlayContainer = root
        } else {
            wallpaperImageView?.alpha = opacity
            wallpaperImageView?.colorFilter = luminanceFilter
            applyBlurEffect(wallpaperImageView ?: return, blurRadius)
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

    private var lastWallpaperId = -1

    private fun loadAndApplyWallpaper() {
        val wallpaperManager = WallpaperManager.getInstance(service)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val currentLockId = wallpaperManager.getWallpaperId(WallpaperManager.FLAG_LOCK)
            val currentSystemId = wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            val effectiveId = if (currentLockId > 0) currentLockId else currentSystemId
            if (effectiveId > 0 && effectiveId != lastWallpaperId) {
                cachedWallpaperBitmap = null
                lastWallpaperId = effectiveId
            }
        }

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
            if (prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_CUSTOM_IMAGE, false)) {
                val file = java.io.File(service.filesDir, "custom_aod_wallpaper.png")
                if (file.exists()) {
                    val customBmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    if (customBmp != null) return customBmp
                }
            }

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
        lastWallpaperId = -1
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
        unregisterWallpaperChangeListeners()
        handler.removeCallbacks(burnInShiftRunnable)
        handler.removeCallbacks(timeoutRunnable)
        hideOverlay()
        overlayContainer = null
        wallpaperImageView = null
        cachedWallpaperBitmap = null
    }
}
