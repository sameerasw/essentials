/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: AppVolumeOverlayHandler.kt
 * Description: Shows a button under the system volume panel that opens volume controls for the apps playing now.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.activities.AppVolumeControlActivity
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.VolumePanelDetector
import com.sameerasw.essentials.utils.media.AppVolumeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class AppVolumeOverlayHandler(
    private val service: AccessibilityService,
) {
    private val settings = SettingsRepository(service)
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val density = service.resources.displayMetrics.density
    private val iconCache = ConcurrentHashMap<String, Bitmap>()
    private val ovalOutline =
        object : ViewOutlineProvider() {
            override fun getOutline(
                view: View,
                outline: Outline,
            ) = outline.setOval(0, 0, view.width, view.height)
        }

    private var button: ButtonView? = null
    private var params: WindowManager.LayoutParams? = null
    private var isShown = false
    private var isButtonPressed = false
    private var hidePending = false
    private var isPolling = false
    private val scanning = AtomicBoolean(false)
    private var rescan = false
    private var lastTouchAt = 0L

    private val pollRunnable =
        Runnable {
            isPolling = false
            if (isShown) onWindowsChanged()
        }
    private val parkRunnable = Runnable { setTouchable(false) }
    private val hideRunnable =
        object : Runnable {
            override fun run() {
                if (isButtonPressed) {
                    handler.postDelayed(this, HIDE_GRACE_MS)
                    return
                }
                hidePending = false
                isShown = false
                button?.animateOut()
                handler.removeCallbacks(parkRunnable)
                handler.postDelayed(parkRunnable, EXIT_MS + PARK_MARGIN_MS)
            }
        }

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SettingsRepository.KEY_APP_VOLUME_OVERLAY_ENABLED || key == SettingsRepository.KEY_APP_VOLUME_ENABLED) {
                handler.post { onWindowsChanged() }
            }
        }

    init {
        settings.registerOnSharedPreferenceChangeListener(prefsListener)
        scope.launch {
            AppVolumeController.playingPackages.collect { packages ->
                withContext(Dispatchers.IO) { packages.forEach { loadIcon(it) } }
                button?.setPlaying(packages)
                onWindowsChanged()
            }
        }
    }

    fun onWindowsChanged() {
        if (!isFeatureOn()) {
            handler.removeCallbacksAndMessages(null)
            hidePending = false
            removeView()
            return
        }
        if (button == null) create()
        if (!isEnabled()) {
            applyBounds(null)
            return
        }
        if (!scanning.compareAndSet(false, true)) {
            rescan = true
            return
        }
        scope.launch(Dispatchers.Default) {
            val bounds = findVolumePanelBounds()
            withContext(Dispatchers.Main) {
                scanning.set(false)
                applyBounds(bounds)
                if (rescan) {
                    rescan = false
                    onWindowsChanged()
                }
            }
        }
    }

    private fun applyBounds(bounds: Rect?) {
        if (bounds == null) {
            scheduleHide()
            return
        }
        handler.removeCallbacks(hideRunnable)
        hidePending = false
        handler.removeCallbacks(parkRunnable)
        show(bounds)
        if (!isPolling) {
            isPolling = true
            handler.postDelayed(pollRunnable, POLL_INTERVAL_MS)
        }
    }

    fun onScreenOff() {
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(pollRunnable)
        handler.removeCallbacks(parkRunnable)
        isPolling = false
        hidePending = false
        isButtonPressed = false
        isShown = false
        button?.hideImmediately()
        setTouchable(false)
    }

    fun onDestroy() {
        settings.unregisterOnSharedPreferenceChangeListener(prefsListener)
        handler.removeCallbacksAndMessages(null)
        removeView()
        scope.cancel()
    }

    private fun isFeatureOn() = settings.isAppVolumeOverlayEnabled() && settings.isAppVolumeEnabled()

    private fun isEnabled() = isFeatureOn() && AppVolumeController.playingPackages.value.isNotEmpty()

    private fun findVolumePanelBounds(): Rect? =
        try {
            service.windows
                ?.firstOrNull(VolumePanelDetector::isVolumePanel)?.let { window -> Rect().also(window::getBoundsInScreen).takeUnless { it.isEmpty } }
        } catch (_: Exception) {
            null
        }

    private fun scheduleHide() {
        if (hidePending || !isShown) return
        hidePending = true
        val remaining = TOUCH_GRACE_MS - (SystemClock.uptimeMillis() - lastTouchAt)
        if (isButtonPressed || remaining > 0) handler.postDelayed(hideRunnable, remaining.coerceAtLeast(HIDE_GRACE_MS)) else hideRunnable.run()
    }

    private fun show(panel: Rect) {
        val current = params ?: return
        val view = button ?: return
        val x = (panel.centerX() - dp(WINDOW_WIDTH_DP) / 2).coerceAtLeast(0)
        val y = panel.bottom + dp(PANEL_GAP_DP)
        val flags = current.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        if (current.x != x || current.y != y || current.flags != flags) {
            current.x = x
            current.y = y
            current.flags = flags
            runCatching { windowManager?.updateViewLayout(view, current) }
        }
        if (!isShown) {
            isShown = true
            view.animateIn()
        }
    }

    private fun setTouchable(touchable: Boolean) {
        val current = params ?: return
        val view = button ?: return
        val flags =
            if (touchable) {
                current.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            } else {
                current.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
        if (flags == current.flags) return
        current.flags = flags
        runCatching { windowManager?.updateViewLayout(view, current) }
    }

    private fun create() {
        val view = ButtonView(service)
        val layoutParams = layoutParams()
        try {
            windowManager?.addView(view, layoutParams)
            button = view
            params = layoutParams
            view.setPlaying(AppVolumeController.playingPackages.value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeView() {
        isShown = false
        isPolling = false
        handler.removeCallbacks(pollRunnable)
        button?.let { runCatching { windowManager?.removeView(it) } }
        button = null
        params = null
    }

    private fun openControls() {
        val intent =
            Intent(service, AppVolumeControlActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        service.startActivity(intent)
    }

    private fun layoutParams() =
        WindowManager.LayoutParams(
            dp(WINDOW_WIDTH_DP),
            dp(WINDOW_HEIGHT_DP),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

    private fun colorScheme(): ColorScheme {
        val isDark =
            (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDark -> dynamicDarkColorScheme(service)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(service)
            isDark -> darkColorScheme()
            else -> lightColorScheme()
        }
    }

    private fun loadIcon(packageName: String): Bitmap? =
        iconCache[packageName]
            ?: runCatching {
                AppUtil.drawableToBitmap(service.packageManager.getApplicationIcon(packageName))
            }.getOrNull()?.also { iconCache[packageName] = it }

    private fun dp(value: Int) = (value * density).toInt()

    @SuppressLint("ViewConstructor", "ClickableViewAccessibility")
    private inner class ButtonView(
        context: Context,
    ) : FrameLayout(context) {
        private val circle = FrameLayout(context)
        private val icons = LinkedHashMap<String, ImageView>()

        init {
            circle.layoutParams = LayoutParams(dp(BUTTON_SIZE_DP), dp(BUTTON_SIZE_DP), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            circle.background =
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(colorScheme().primaryContainer.toArgb())
                }
            circle.outlineProvider = ovalOutline
            circle.clipToOutline = true
            circle.contentDescription = context.getString(R.string.app_volume_overlay_button_desc)
            circle.alpha = 0f
            circle.scaleX = HIDDEN_SCALE
            circle.scaleY = HIDDEN_SCALE
            circle.setOnTouchListener { _, event -> onTouch(event) }
            addView(circle)
        }

        private fun onTouch(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isButtonPressed = true
                    lastTouchAt = SystemClock.uptimeMillis()
                    animate().scaleX(PRESSED_SCALE).scaleY(PRESSED_SCALE).setDuration(PRESS_MS).start()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isButtonPressed = false
                    lastTouchAt = SystemClock.uptimeMillis()
                    animate().scaleX(1f).scaleY(1f).setDuration(PRESS_MS).start()
                    if (event.actionMasked == MotionEvent.ACTION_UP && isInside(event)) {
                        HapticUtil.performVirtualKeyHaptic(this)
                        openControls()
                    }
                }
            }
            return true
        }

        private fun isInside(event: MotionEvent) = event.x in 0f..circle.width.toFloat() && event.y in 0f..circle.height.toFloat()

        fun animateIn() {
            circle.animate().cancel()
            circle
                .animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ENTER_MS)
                .setInterpolator(OvershootInterpolator(OVERSHOOT_TENSION))
                .start()
        }

        fun animateOut() {
            circle.animate().cancel()
            circle
                .animate()
                .alpha(0f)
                .scaleX(HIDDEN_SCALE)
                .scaleY(HIDDEN_SCALE)
                .setDuration(EXIT_MS)
                .setInterpolator(AccelerateInterpolator())
                .start()
        }

        fun hideImmediately() {
            circle.animate().cancel()
            circle.alpha = 0f
            circle.scaleX = HIDDEN_SCALE
            circle.scaleY = HIDDEN_SCALE
        }

        fun setPlaying(packages: List<String>) {
            val top = packages.take(MAX_STACKED)
            icons.keys.filter { it !in top }.forEach { packageName ->
                val icon = icons.remove(packageName) ?: return@forEach
                icon
                    .animate()
                    .alpha(0f)
                    .scaleX(HIDDEN_SCALE)
                    .scaleY(HIDDEN_SCALE)
                    .setDuration(EXIT_MS)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction { circle.removeView(icon) }
                    .start()
            }
            top.forEachIndexed { depth, packageName ->
                val icon =
                    icons[packageName] ?: createIcon(packageName)?.also {
                        icons[packageName] = it
                        circle.addView(it)
                        it.alpha = 0f
                        it.scaleX = HIDDEN_SCALE
                        it.scaleY = HIDDEN_SCALE
                        it.translationY = offsetFor(depth, top.size)
                    } ?: return@forEachIndexed
                val scale = 1f - DEPTH_SCALE_STEP * depth
                icon
                    .animate()
                    .translationY(offsetFor(depth, top.size))
                    .scaleX(scale)
                    .scaleY(scale)
                    .alpha(1f - DEPTH_FADE_STEP * depth)
                    .setDuration(STACK_MS)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            top.indices.reversed().forEach { icons[top[it]]?.bringToFront() }
        }

        private fun offsetFor(depth: Int, count: Int) = if (count == 1) 0f else (FRONT_SHIFT_DP - STACK_STEP_DP * depth) * density

        private fun createIcon(packageName: String): ImageView? {
            val bitmap = loadIcon(packageName) ?: return null
            return ImageView(context).apply {
                layoutParams = LayoutParams(dp(ICON_SIZE_DP), dp(ICON_SIZE_DP), Gravity.CENTER)
                setImageBitmap(bitmap)
                outlineProvider = ovalOutline
                clipToOutline = true
            }
        }
    }

    private companion object {
        const val WINDOW_WIDTH_DP = 72
        const val WINDOW_HEIGHT_DP = 64
        const val BUTTON_SIZE_DP = 52
        const val ICON_SIZE_DP = 28
        const val PANEL_GAP_DP = 14
        const val FRONT_SHIFT_DP = 4
        const val STACK_STEP_DP = 8
        const val MAX_STACKED = 3
        const val DEPTH_SCALE_STEP = 0.2f
        const val DEPTH_FADE_STEP = 0.25f
        const val HIDDEN_SCALE = 0.5f
        const val PRESSED_SCALE = 0.9f
        const val OVERSHOOT_TENSION = 1.4f
        const val PRESS_MS = 90L
        const val ENTER_MS = 320L
        const val STACK_MS = 280L
        const val EXIT_MS = 150L
        const val PARK_MARGIN_MS = 50L
        const val HIDE_GRACE_MS = 150L
        const val TOUCH_GRACE_MS = 800L
        const val POLL_INTERVAL_MS = 250L
    }
}
