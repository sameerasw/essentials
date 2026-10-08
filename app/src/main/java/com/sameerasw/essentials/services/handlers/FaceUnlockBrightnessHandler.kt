/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: FaceUnlockBrightnessHandler.kt
 * Description: Shows an Illuminate pill on the lock screen in low light that boosts brightness for face unlock.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.biometrics.BiometricManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.service.OverlayLifecycleOwner
import com.sameerasw.essentials.ui.theme.GoogleSansFlexRounded
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShellUtils
import kotlin.concurrent.thread

class FaceUnlockBrightnessHandler(
    private val service: AccessibilityService,
) {
    private val settings = SettingsRepository(service)
    private val handler = Handler(Looper.getMainLooper())
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val keyguardManager = service.getSystemService(KeyguardManager::class.java)
    private val powerManager = service.getSystemService(PowerManager::class.java)
    private val sensorManager = service.getSystemService(SensorManager::class.java)
    private val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private class PillOverlay(
        val view: View,
        val owner: OverlayLifecycleOwner,
        val visibleState: MutableTransitionState<Boolean>,
    )

    private var pill: PillOverlay? = null
    private var brightnessView: View? = null
    private var tintView: View? = null
    private var isListening = false
    private var isLowLight = false
    private var isListeningProximity = false
    private var isProximityCovered: Boolean? = null
    private var wakeTime = 0L
    private var autoHandled = false
    private var biometricsBlocked = false

    private val scanRunnable = Runnable { update() }
    private val endBumpRunnable = Runnable { endBump() }
    private var leavingPill: PillOverlay? = null
    private val finishLeaveRunnable = Runnable { destroyPill(leavingPill.also { leavingPill = null }) }

    private val sensorListener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!canRun()) return
                val lux = event.values[0]
                val threshold = settings.getFaceUnlockAmbientThreshold()
                val low = if (isLowLight) lux < threshold * ENOUGH_LUX_RATIO else lux < threshold
                if (low == isLowLight) return
                isLowLight = low
                if (low) scheduleScans(SCREEN_ON_SCAN_DELAYS_MS) else update()
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int,
            ) {}
        }

    private val proximityListener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val covered = event.values[0] < minOf(event.sensor.maximumRange, PROXIMITY_NEAR_CM)
                if (covered == isProximityCovered) return
                isProximityCovered = covered
                if (!covered && isLowLight) scheduleScans(WINDOW_CHANGE_SCAN_DELAYS_MS)
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int,
            ) {}
        }

    fun onScreenOn() {
        resetState()
        biometricsBlocked = !isFaceUnlockAvailable()
        if (!canRun()) return
        wakeTime = SystemClock.elapsedRealtime()
        checkStrongAuthRequired(wakeTime)
        if (settings.isFaceUnlockAutoIlluminateEnabled()) {
            proximitySensor?.let {
                isListeningProximity =
                    sensorManager?.registerListener(proximityListener, it, SensorManager.SENSOR_DELAY_NORMAL) == true
            }
        }
        val sensor = lightSensor ?: return
        isListening = sensorManager?.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL) == true
    }

    fun onWindowsChanged() {
        if (pill != null || isLowLight) scheduleScans(WINDOW_CHANGE_SCAN_DELAYS_MS)
    }

    fun onConfigurationChanged() {
        removePill(animate = false)
        update()
    }

    fun onScreenOff() = resetState()

    fun onUserPresent() = resetState()

    fun onDestroy() = resetState()

    // isDeviceLocked is false while a trust agent such as a watch keeps the device unlocked behind the lock screen
    private fun canRun() =
        settings.isFaceUnlockBrightnessEnabled() &&
            keyguardManager?.isKeyguardLocked == true &&
            keyguardManager.isDeviceLocked &&
            !biometricsBlocked &&
            powerManager?.isInteractive == true

    private fun isFaceUnlockAvailable(): Boolean {
        if (!service.packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        val result =
            service
                .getSystemService(BiometricManager::class.java)
                ?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun checkStrongAuthRequired(token: Long) {
        if (!ShellUtils.isAvailable(service) || !ShellUtils.hasPermission(service)) return
        thread {
            val output =
                ShellUtils.runCommandWithOutput(
                    service,
                    "settings get secure face_keyguard_enabled; dumpsys trust",
                    notifyOnError = false,
                ).orEmpty()
            val faceDisabled = output.lineSequence().firstOrNull()?.trim() == "0"
            val strongAuth =
                output
                    .lineSequence()
                    .firstOrNull { "(current)" in it }
                    ?.let { Regex("strongAuthRequired=0x([0-9a-fA-F]+)").find(it)?.groupValues?.get(1)?.toLongOrNull(16) }
                    ?: 0L
            if (!faceDisabled && strongAuth == 0L) return@thread
            handler.post {
                if (token != wakeTime) return@post
                resetState()
                biometricsBlocked = true
            }
        }
    }

    private fun resetState() {
        handler.removeCallbacks(scanRunnable)
        stopListening()
        removePill(animate = false)
        handler.removeCallbacks(finishLeaveRunnable)
        destroyPill(leavingPill.also { leavingPill = null })
        endTint(animate = false)
        endBump()
        isLowLight = false
        autoHandled = false
    }

    private fun stopListening() {
        if (isListeningProximity) {
            isListeningProximity = false
            sensorManager?.unregisterListener(proximityListener)
        }
        isProximityCovered = null
        if (!isListening) return
        isListening = false
        sensorManager?.unregisterListener(sensorListener)
    }

    private fun scheduleScans(delays: List<Long>) {
        handler.removeCallbacks(scanRunnable)
        delays.forEach { handler.postDelayed(scanRunnable, it) }
    }

    private fun update() {
        if (!canRun() || !isLowLight || brightnessView != null) {
            removePill()
            return
        }
        if (!isMainLockscreenVisible()) {
            removePill()
            return
        }
        if (shouldAutoIlluminate()) {
            autoHandled = true
            illuminate(auto = true)
        } else {
            showPill()
        }
    }

    // Only right after waking, and only once the proximity sensor reports the screen isn't covered
    private fun shouldAutoIlluminate(): Boolean {
        if (autoHandled || !settings.isFaceUnlockAutoIlluminateEnabled()) return false
        if (SystemClock.elapsedRealtime() - wakeTime > AUTO_ILLUMINATE_WINDOW_MS) return false
        return proximitySensor == null || isProximityCovered == false
    }

    // Auto illumination never triggers the unlock, so waking the screen doesn't also open the bouncer
    private fun illuminate(auto: Boolean = false) {
        removePill()
        bump()
        if (settings.isFaceUnlockLightTintEnabled()) showTint()
        if (!auto && settings.isFaceUnlockTriggerUnlockEnabled()) showBouncer()
    }

    private fun isMainLockscreenVisible(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) service.clearCache()
        val refreshEachNode = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        val roots =
            try {
                service.windows.mapNotNull { it.root }.filter { it.packageName?.toString() == SYSTEM_UI }
            } catch (_: Exception) {
                emptyList()
            }
        var lockscreenVisible = false
        var occluded = false
        for (root in roots) {
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            var visited = 0
            while (queue.isNotEmpty() && visited < MAX_NODES) {
                val node = queue.removeFirst()
                visited++
                if (refreshEachNode) node.refresh()
                if (!node.isVisibleToUser) continue
                val id = node.viewIdResourceName?.substringAfter(":id/").orEmpty()
                if (LOCKSCREEN_MARKERS.any { id == it }) lockscreenVisible = true
                if (OCCLUDING_MARKERS.any { id.contains(it, ignoreCase = true) }) occluded = true
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { queue.add(it) }
                }
            }
        }
        return lockscreenVisible && !occluded
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showPill() {
        if (pill != null) return
        leavingPill?.let { returning ->
            handler.removeCallbacks(finishLeaveRunnable)
            leavingPill = null
            returning.visibleState.targetState = true
            pill = returning
            return
        }
        val visibleState = MutableTransitionState(false)
        val owner = OverlayLifecycleOwner()
        owner.onCreate()
        val container =
            FrameLayout(service).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) scheduleScans(OUTSIDE_TOUCH_SCAN_DELAYS_MS)
                    false
                }
            }
        val label = service.getString(R.string.face_unlock_illuminate)
        container.addView(
            ComposeView(service).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    IlluminatePill(
                        label = label,
                        visibleState = visibleState,
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(container)
                            illuminate()
                        },
                    )
                }
            },
        )
        try {
            windowManager?.addView(container, pillLayoutParams())
            pill = PillOverlay(container, owner, visibleState)
            visibleState.targetState = true
        } catch (_: Exception) {
            owner.onDestroy()
        }
    }

    private fun removePill(animate: Boolean = true) {
        val overlay = pill ?: return
        pill = null
        if (!animate) {
            destroyPill(overlay)
            return
        }
        overlay.visibleState.targetState = false
        leavingPill?.let { destroyPill(it) }
        leavingPill = overlay
        handler.removeCallbacks(finishLeaveRunnable)
        handler.postDelayed(finishLeaveRunnable, EXIT_DURATION_MS + 50L)
    }

    private fun destroyPill(overlay: PillOverlay?) {
        overlay ?: return
        try {
            windowManager?.removeView(overlay.view)
        } catch (_: Exception) {
        }
        overlay.owner.onDestroy()
    }

    private fun pillLayoutParams(): WindowManager.LayoutParams {
        val density = service.resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (WINDOW_BOTTOM_MARGIN_DP * density).toInt()
        }
    }

    private fun showBouncer() {
        if (!ShellUtils.isAvailable(service) || !ShellUtils.hasPermission(service)) return
        thread { ShellUtils.runCommand(service, "input keyevent KEYCODE_SPACE", notifyOnError = false) }
    }

    private fun bump() {
        if (brightnessView != null) return
        val params =
            WindowManager.LayoutParams(
                1,
                1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                screenBrightness = settings.getFaceUnlockMaxBrightness() / 100f
            }
        val view = View(service)
        try {
            windowManager?.addView(view, params)
            brightnessView = view
            handler.postDelayed(endBumpRunnable, BUMP_DURATION_MS)
        } catch (_: Exception) {
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showTint() {
        if (tintView != null) return
        val view =
            View(service).apply {
                setBackgroundColor(Color.WHITE)
                alpha = 0f
                // Not touchable so input reaches the lock screen; a touch landing on it is reported as outside
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) endTint()
                    false
                }
            }
        val params =
            WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        try {
            windowManager?.addView(view, params)
            tintView = view
            view.animate().alpha(TINT_ALPHA).setDuration(TINT_FADE_MS).start()
        } catch (_: Exception) {
        }
    }

    private fun endTint(animate: Boolean = true) {
        val view = tintView ?: return
        tintView = null
        if (!animate) {
            removeTintView(view)
            return
        }
        view.animate().alpha(0f).setDuration(TINT_FADE_MS).withEndAction { removeTintView(view) }.start()
    }

    private fun removeTintView(view: View) {
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
        }
    }

    private fun endBump() {
        handler.removeCallbacks(endBumpRunnable)
        endTint()
        val view = brightnessView ?: return
        brightnessView = null
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
        }
        scheduleScans(AFTER_BUMP_SCAN_DELAYS_MS)
    }

    @Composable
    private fun IlluminatePill(
        label: String,
        visibleState: MutableTransitionState<Boolean>,
        onClick: () -> Unit,
    ) {
        val isDark =
            (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val colors =
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDark -> dynamicDarkColorScheme(service)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(service)
                isDark -> darkColorScheme()
                else -> lightColorScheme()
            }
        val density = service.resources.displayMetrics.density
        val travelPx = ((PILL_LIFT_DP + PILL_TRAVEL_DP) * density).toInt()

        Box(modifier = Modifier.padding(bottom = PILL_LIFT_DP.dp)) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = slideInVertically(tween(ENTER_DURATION_MS)) { it + travelPx } + fadeIn(tween(ENTER_DURATION_MS)),
                exit = slideOutVertically(tween(EXIT_DURATION_MS.toInt())) { it + travelPx } + fadeOut(tween(EXIT_DURATION_MS.toInt())),
            ) {
                Row(
                    modifier =
                        Modifier
                            .background(colors.surfaceContainerHigh, CircleShape)
                            .semantics {
                                contentDescription = label
                                role = Role.Button
                            }.pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
                            .padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_lightbulb_24),
                        contentDescription = null,
                        tint = colors.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = GoogleSansFlexRounded),
                        color = colors.onSurface,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"
        private const val MAX_NODES = 400
        private const val ENOUGH_LUX_RATIO = 1.5f
        private const val AUTO_ILLUMINATE_WINDOW_MS = 3_000L
        private const val PROXIMITY_NEAR_CM = 5f
        private const val BUMP_DURATION_MS = 5_000L
        private const val TINT_ALPHA = 0.5f
        private const val TINT_FADE_MS = 200L
        private const val WINDOW_BOTTOM_MARGIN_DP = 24f
        private const val PILL_LIFT_DP = 48f
        private const val PILL_TRAVEL_DP = 24f
        private const val ENTER_DURATION_MS = 350
        private const val EXIT_DURATION_MS = 250L
        private val LOCKSCREEN_MARKERS =
            listOf("element:lockscreen", "keyguard_root_view", "keyguard_bottom_area", "keyguard_indication_area")
        private val OCCLUDING_MARKERS =
            listOf("bouncer", "element:shade", "element:quickSettings", "qs_frame", "shade_header_root", "quick_qs_panel")
        private val SCREEN_ON_SCAN_DELAYS_MS = listOf(300L, 900L, 1800L)
        private val WINDOW_CHANGE_SCAN_DELAYS_MS = listOf(150L, 600L)
        private val AFTER_BUMP_SCAN_DELAYS_MS = listOf(250L)
        private val OUTSIDE_TOUCH_SCAN_DELAYS_MS = listOf(120L, 350L, 700L)
    }
}
