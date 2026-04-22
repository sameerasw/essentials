package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import com.sameerasw.essentials.utils.OmniTriggerUtil

class OmniGestureOverlayHandler(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val vibrator = getVibratorInstance()
    
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop
    
    private var startX = 0f
    private var startY = 0f
    private var isLongPressActive = false

    private val longPressRunnable = Runnable {
        isLongPressActive = false
        OmniTriggerUtil.trigger(service)
        triggerFinalTick()
    }

    private val fallbackEffect: VibrationEffect? by lazy {
        val segments = 25
        val timings = LongArray(segments) { 20L }
        val amplitudes = IntArray(segments) { i ->
            val progress = (i + 1).toFloat() / segments
            val curve = progress * progress
            (3 + (57 * curve)).toInt().coerceAtMost(60)
        }
        runCatching { VibrationEffect.createWaveform(timings, amplitudes, -1) }.getOrNull()
    }

    fun updateOverlay(enabled: Boolean) {
        handler.post {
            if (enabled) showOverlay() else removeOverlay()
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return

        overlayView = View(service).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                handleTouch(event)
                true
            }
        }

        val params = WindowManager.LayoutParams(
            dpToPx(WIDTH_DP),
            dpToPx(HEIGHT_DP),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        runCatching { windowManager.addView(overlayView, params) }
    }

    private fun handleTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                isLongPressActive = true
                handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT)
                startRampingHaptic()
            }
            MotionEvent.ACTION_MOVE -> {
                if (Math.abs(event.x - startX) > touchSlop || Math.abs(event.y - startY) > touchSlop) {
                    cancelLongPress()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelLongPress()
        }
    }

    private fun cancelLongPress() {
        if (!isLongPressActive) return
        isLongPressActive = false
        handler.removeCallbacks(longPressRunnable)
        vibrator?.cancel()
    }

    fun removeOverlay() {
        cancelLongPress()
        overlayView?.let {
            runCatching { windowManager.removeView(it) }
            overlayView = null
        }
    }

    private fun startRampingHaptic() {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.6f)
                    .compose()
                v.vibrate(effect)
            }.onFailure { fallbackRampingWaveform(v) }
        } else {
            fallbackRampingWaveform(v)
        }
    }

    private fun fallbackRampingWaveform(v: Vibrator) {
        fallbackEffect?.let {
            runCatching { v.vibrate(it) }
        } ?: v.vibrate(VibrationEffect.createOneShot(LONG_PRESS_TIMEOUT, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun triggerFinalTick() {
        val v = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                v.vibrate(VibrationEffect.createOneShot(30, 180))
            }
        }
    }

    private fun getVibratorInstance(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            service.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            service.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun dpToPx(dp: Float) = (dp * service.resources.displayMetrics.density).toInt()

    companion object {
        private const val LONG_PRESS_TIMEOUT = 500L
        private const val WIDTH_DP = 240f
        private const val HEIGHT_DP = 48f
    }
}
