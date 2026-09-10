/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Handlers
 * File: DuoOverlayHandler.kt
 * Description: Background handler managing the Duo ambient camera overlay.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.DuoOverlayView
import com.sameerasw.essentials.utils.OverlayHelper

class DuoOverlayHandler(
    private val service: AccessibilityService,
) {
    private var windowManager: WindowManager? = null
    private var overlayView: DuoOverlayView? = null
    private var isOverlayAdded = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val settingsRepository by lazy { SettingsRepository(service) }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale
                    overlayView?.batteryLevel = batteryPct
                }
            }
        }
    }
    private var isBatteryReceiverRegistered = false

    private var isScreenOff: Boolean = false

    fun init() {
        windowManager = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as? WindowManager
        updateState()
    }

    fun onScreenOn() {
        isScreenOff = false
        overlayView?.isScreenOff = false
        updateState()
    }

    fun onScreenOff() {
        isScreenOff = true
        overlayView?.isScreenOff = true
        updateState()
    }

    fun updateState() {
        if (!settingsRepository.isDuoEnabled()) {
            removeOverlay()
            return
        }
        showOrUpdateOverlay()
    }

    private fun showOrUpdateOverlay() {
        mainHandler.post {
            val wm = windowManager ?: return@post

            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels.toFloat()
            val screenHeight = displayMetrics.heightPixels.toFloat()
            val density = displayMetrics.density

            var centerX = screenWidth * (settingsRepository.getDuoCameraOffsetX() / 100f)
            var centerY = screenHeight * (settingsRepository.getDuoCameraOffsetY() / 100f)
            var cameraRadiusPx = 18f * density * settingsRepository.getDuoCameraSize()

            if (settingsRepository.isDuoAutoDetectEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                val cutout = wm.defaultDisplay.cutout
                if (cutout != null && cutout.boundingRects.isNotEmpty()) {
                    // Find cutout rect closest to top center of screen
                    val topCutouts = cutout.boundingRects.filter { it.top < screenHeight / 4 }
                    val targetRect = topCutouts.minByOrNull { kotlin.math.abs(it.centerX() - screenWidth / 2f) }
                    if (targetRect != null) {
                        centerX = targetRect.centerX().toFloat()
                        centerY = targetRect.centerY().toFloat()
                        val computedRadius = (targetRect.width().coerceAtLeast(targetRect.height()) / 2f) * settingsRepository.getDuoCameraSize()
                        if (computedRadius > 0) {
                            cameraRadiusPx = computedRadius
                        }
                    }
                }
            }

            val isNightMode = (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            if (overlayView == null) {
                overlayView = DuoOverlayView(service)
            }

            overlayView?.apply {
                this.cameraCenterX = centerX
                this.cameraCenterY = centerY
                this.cameraRadiusPx = cameraRadiusPx
                this.ringRadiusScale = settingsRepository.getDuoRingRadius()
                this.arcThicknessPx = settingsRepository.getDuoArcThickness() * density
                this.dotRadiusPx = settingsRepository.getDuoDotSize() * density
                this.isDarkTheme = isNightMode
                this.isScreenOff = this@DuoOverlayHandler.isScreenOff
                this.showNetworks = settingsRepository.isDuoShowNetworksEnabled()
            }

            if (!isOverlayAdded) {
                val params = OverlayHelper.createOverlayLayoutParams(
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    isTouchable = false
                )
                try {
                    wm.addView(overlayView, params)
                    isOverlayAdded = true
                } catch (e: Exception) {
                    android.util.Log.e("DuoOverlayHandler", "Failed to add Duo overlay", e)
                }
            } else {
                overlayView?.invalidate()
            }

            registerBatteryReceiver()
        }
    }

    private fun registerBatteryReceiver() {
        if (!isBatteryReceiverRegistered) {
            try {
                val intent = service.registerReceiver(
                    batteryReceiver,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                isBatteryReceiverRegistered = true
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    if (level >= 0 && scale > 0) {
                        overlayView?.batteryLevel = (level * 100) / scale
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DuoOverlayHandler", "Failed to register battery receiver", e)
            }
        }
    }

    private fun unregisterBatteryReceiver() {
        if (isBatteryReceiverRegistered) {
            try {
                service.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
            isBatteryReceiverRegistered = false
        }
    }

    fun removeOverlay() {
        mainHandler.post {
            if (isOverlayAdded && overlayView != null) {
                try {
                    windowManager?.removeView(overlayView)
                } catch (_: Exception) {}
                isOverlayAdded = false
            }
            unregisterBatteryReceiver()
        }
    }

    fun destroy() {
        removeOverlay()
        overlayView = null
    }
}
