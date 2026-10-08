/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: LockscreenShortcutsHandler.kt
 * Description: Draws Essentials shortcut buttons on the lock screen when System UI shows none of its own.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.LockscreenShortcutSide
import com.sameerasw.essentials.domain.model.SystemShortcutsState
import com.sameerasw.essentials.island.service.OverlayLifecycleOwner
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.ui.activities.LockscreenActionActivity
import com.sameerasw.essentials.utils.ActivityLauncherUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.LockscreenShortcutDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LockscreenShortcutsHandler(
    private val service: AccessibilityService,
    private val flashlightHandler: FlashlightHandler,
) {
    private val settings = SettingsRepository(service)
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val keyguardManager = service.getSystemService(KeyguardManager::class.java)
    private val powerManager = service.getSystemService(PowerManager::class.java)

    private val overlays = mutableMapOf<LockscreenShortcutSide, ShortcutOverlay>()
    private var actionJob: Job? = null

    private class ShortcutOverlay(
        val view: View,
        val owner: OverlayLifecycleOwner,
        val action: Action,
        val params: WindowManager.LayoutParams,
        var isVisible: Boolean = false,
    )

    private val scanRunnable = Runnable { updateState() }

    // Prebuilt while the screen is off so both sides appear together on wake
    fun onScreenOn() {
        if (canShowFromLastScan() && keyguardManager?.isKeyguardLocked == true) {
            syncOverlays()
            setVisible(true)
        }
        scheduleScans(SCREEN_ON_SCAN_DELAYS_MS)
    }

    fun onScreenOff() {
        handler.removeCallbacks(scanRunnable)
        if (canShowFromLastScan()) {
            syncOverlays()
            setVisible(false)
        } else {
            removeAll()
        }
    }

    fun onUserPresent() {
        handler.removeCallbacks(scanRunnable)
        removeAll()
    }

    fun onWindowsChanged() {
        if (keyguardManager?.isKeyguardLocked == true) scheduleScans(WINDOW_CHANGE_SCAN_DELAYS_MS)
    }

    fun onConfigurationChanged() {
        removeAll()
        updateState()
    }

    fun onDestroy() {
        onUserPresent()
        scope.cancel()
    }

    fun updateState() {
        if (keyguardManager?.isKeyguardLocked != true) {
            onUserPresent()
            return
        }
        if (powerManager?.isInteractive != true) {
            setVisible(false)
            return
        }

        val screen = screenSize()
        val result = LockscreenShortcutDetector.scan(service, screen.x, screen.y)
        if (result.isLockscreenVisible) {
            val state = if (result.hasSystemShortcuts) SystemShortcutsState.PRESENT else SystemShortcutsState.NONE
            if (state != settings.getLockscreenSystemShortcutsState()) {
                settings.setLockscreenSystemShortcutsState(state)
            }
        }

        val isEnabled = settings.isLockscreenShortcutsEnabled()
        if (isEnabled && settings.getLockscreenSystemShortcutsState() == SystemShortcutsState.NONE) {
            syncOverlays()
            // Hidden, not removed, while the shade or bouncer covers the lock screen
            setVisible(result.isLockscreenVisible)
        } else {
            removeAll()
        }
    }

    private fun canShowFromLastScan() =
        settings.isLockscreenShortcutsEnabled() &&
            settings.getLockscreenSystemShortcutsState() == SystemShortcutsState.NONE

    private fun scheduleScans(delays: List<Long>) {
        handler.removeCallbacks(scanRunnable)
        delays.forEach { handler.postDelayed(scanRunnable, it) }
    }

    private fun syncOverlays() {
        LockscreenShortcutSide.entries.forEach { side ->
            val action = settings.getRemapActions(side.prefKey).firstOrNull()
            val existing = overlays[side]
            when {
                action == null -> remove(side)
                existing == null -> create(side, action)
                existing.action != action -> {
                    val wasVisible = existing.isVisible
                    remove(side)
                    create(side, action)
                    if (wasVisible) setVisible(true)
                }
            }
        }
    }

    private fun setVisible(visible: Boolean) {
        overlays.values.forEach { overlay ->
            if (overlay.isVisible == visible) return@forEach
            overlay.isVisible = visible
            // Alpha, not INVISIBLE: an invisible window stops receiving the outside touches that trigger rescans
            overlay.view.alpha = if (visible) 1f else 0f
            overlay.params.flags =
                if (visible) {
                    overlay.params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                } else {
                    overlay.params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                }
            try {
                windowManager?.updateViewLayout(overlay.view, overlay.params)
            } catch (_: Exception) {
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun create(
        side: LockscreenShortcutSide,
        action: Action,
    ) {
        val owner = OverlayLifecycleOwner()
        owner.onCreate()
        val container =
            FrameLayout(service).apply {
                alpha = 0f
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                // Shade and bouncer changes on the lock screen don't change windows, but always start with a touch
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) scheduleScans(OUTSIDE_TOUCH_SCAN_DELAYS_MS)
                    false
                }
            }
        val app =
            when (action) {
                is Action.OpenApp -> loadApp(action.packageName)
                is Action.OpenActivity -> loadActivity(action)
                else -> null
            }
        val label =
            service.getString(R.string.lockscreen_shortcut_content_desc, app?.label ?: service.getString(action.title))
        container.addView(
            ComposeView(service).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    ShortcutButton(
                        iconRes = action.icon,
                        appIcon = app?.icon,
                        label = label,
                        onTap = {
                            Toast.makeText(service, R.string.lockscreen_shortcut_hold_hint, Toast.LENGTH_SHORT).show()
                        },
                        onLongPress = { runAction(action) },
                    )
                }
            },
        )

        val params = layoutParams(side)
        try {
            windowManager?.addView(container, params)
            overlays[side] = ShortcutOverlay(container, owner, action, params)
        } catch (e: Exception) {
            e.printStackTrace()
            owner.onDestroy()
        }
    }

    private fun remove(side: LockscreenShortcutSide) {
        val overlay = overlays.remove(side) ?: return
        try {
            windowManager?.removeView(overlay.view)
        } catch (_: Exception) {
        }
        overlay.owner.onDestroy()
    }

    private fun removeAll() = LockscreenShortcutSide.entries.forEach { remove(it) }

    private fun runAction(action: Action) {
        if (actionJob?.isActive == true) return
        actionJob =
            scope.launch {
                HapticUtil.performHapticForService(service)
                try {
                    when {
                        action is Action.ToggleFlashlight -> flashlightHandler.toggleFlashlight()
                        opensUi(action) -> LockscreenActionActivity.start(service, action)
                        else -> CombinedActionExecutor.execute(service, action)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    private class AppInfo(
        val label: String,
        val icon: ImageBitmap,
    )

    private fun loadApp(packageName: String): AppInfo? {
        if (packageName.isBlank()) return null
        return try {
            val pm = service.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            val size = (APP_ICON_SIZE_DP * service.resources.displayMetrics.density).toInt()
            AppInfo(
                label = pm.getApplicationLabel(info).toString(),
                icon = pm.getApplicationIcon(info).toBitmap(size, size).asImageBitmap(),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun loadActivity(action: Action.OpenActivity): AppInfo? {
        val size = (APP_ICON_SIZE_DP * service.resources.displayMetrics.density).toInt()
        val icon = ActivityLauncherUtil.loadShortcutIcon(service, action, size) ?: return null
        return AppInfo(
            label = action.label.ifBlank { service.getString(action.title) },
            icon = icon.asImageBitmap(),
        )
    }

    // These would open behind the keyguard without unlocking first
    private fun opensUi(action: Action): Boolean =
        action is Action.OpenApp ||
            action is Action.OpenActivity ||
            action is Action.AIAssistant ||
            action is Action.EssentialSearch ||
            action is Action.OpenNowPlayingApp ||
            action is Action.OpenVideoCamera ||
            action is Action.OpenQrScanner

    // AOSP keyguard affordance placement
    private fun layoutParams(side: LockscreenShortcutSide): WindowManager.LayoutParams {
        val density = service.resources.displayMetrics.density
        val windowSize = (WINDOW_SIZE_DP * density).toInt()
        val inset = ((BUTTON_SIZE_DP - WINDOW_SIZE_DP) / 2f * density).toInt()
        return WindowManager.LayoutParams(
            windowSize,
            windowSize,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or if (side == LockscreenShortcutSide.LEFT) Gravity.START else Gravity.END
            x = (SIDE_MARGIN_DP * density).toInt() + inset
            y = (BOTTOM_MARGIN_DP * density).toInt() + inset
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun screenSize(): Point =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            val metrics = service.resources.displayMetrics
            Point(metrics.widthPixels, metrics.heightPixels)
        }

    @Composable
    private fun ShortcutButton(
        iconRes: Int,
        appIcon: ImageBitmap?,
        label: String,
        onTap: () -> Unit,
        onLongPress: () -> Unit,
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
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(if (isPressed) PRESSED_SCALE else 1f, label = "shortcutScale")

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .size(BUTTON_SIZE_DP.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }.clip(CircleShape)
                        .background(colors.surfaceContainerHigh)
                        .semantics { contentDescription = label }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                },
                                onTap = { onTap() },
                                onLongPress = { onLongPress() },
                            )
                        },
                contentAlignment = Alignment.Center,
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = null,
                        modifier = Modifier.size(APP_ICON_SIZE_DP.dp).clip(CircleShape),
                    )
                } else {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = colors.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }

    companion object {
        private const val BUTTON_SIZE_DP = 48f
        private const val APP_ICON_SIZE_DP = 36f
        private const val WINDOW_SIZE_DP = 60f
        private const val SIDE_MARGIN_DP = 16f
        private const val BOTTOM_MARGIN_DP = 32f
        private const val PRESSED_SCALE = 1.2f
        private val SCREEN_ON_SCAN_DELAYS_MS = listOf(300L, 900L, 1800L)
        private val WINDOW_CHANGE_SCAN_DELAYS_MS = listOf(150L, 600L)
        private val OUTSIDE_TOUCH_SCAN_DELAYS_MS = listOf(150L, 450L, 900L, 1500L)
    }
}
