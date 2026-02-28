package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

class AodForceTurnOffHandler(private val service: AccessibilityService) {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private var isRunning = false

    fun forceTurnOff() {
        val powerManager = service.getSystemService(Context.POWER_SERVICE) as PowerManager
        // Only run if screen is not interactive (currently in AOD or off)
        if (powerManager.isInteractive || isRunning) {
            Log.d("AodForceTurnOff", "Skipping forceTurnOff: isInteractive=${powerManager.isInteractive}, isRunning=$isRunning")
            return
        }
        
        Log.d("AodForceTurnOff", "Starting forceTurnOff sequence")
        isRunning = true
        showOverlay()
        
        // Sequence: Overlay -> Wake -> Lock -> Remove Overlay
        // Using slightly longer delays to ensure system registers actions
        handler.postDelayed({
            wakeScreen()
            
            handler.postDelayed({
                lockScreen()
                
                // Allow time for the lock action to process and screen to turn off
                handler.postDelayed({
                    removeOverlay()
                    isRunning = false
                    Log.d("AodForceTurnOff", "ForceTurnOff sequence completed")
                }, 600) 
            }, 100)
        }, 50)
    }

    private fun showOverlay() {
        if (overlayView != null) return

        windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = FrameLayout(service).apply {
            setBackgroundColor(Color.BLACK)
        }

        val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            overlayView = null
            isRunning = false
        }
    }

    private fun wakeScreen() {
        val powerManager = service.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Essentials:ForceTurnOffWake"
        )
        wakeLock.acquire(100)
    }

    private fun lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    fun removeOverlay() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
            overlayView = null
        }
        isRunning = false
    }
}
