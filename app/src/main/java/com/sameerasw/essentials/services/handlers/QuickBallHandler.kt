/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Handlers
 * File: QuickBallHandler.kt
 * Description: Background handler managing the Quick Ball floating overlay, gestures, and radial shortcuts.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.island.service.OverlayLifecycleOwner
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallExpandedOverlay
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallFloatingBall
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class QuickBallHandler(
    private val service: AccessibilityService,
) {
    companion object {
        private const val TAG = "QuickBallHandler"
        private const val IDLE_TIMEOUT_MS = 3000L
    }

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val settingsRepository = SettingsRepository(service)

    private var overlayRoot: FrameLayout? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private var isOverlayAdded = false
    private var isExpanded by mutableStateOf(false)
    private var isIdle by mutableStateOf(false)

    private var ballSizeDp by mutableStateOf(48f)
    private var idleAlpha by mutableStateOf(0.4f)
    private var isAutoHide by mutableStateOf(true)
    private var isHapticEnabled by mutableStateOf(true)
    private var isDockedLeft by mutableStateOf(false)
    private var currentActions by mutableStateOf<List<Action>>(emptyList())

    // Window Layout Params
    private val windowParams =
        WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

    // Touch and drag tracking
    private var initialX = 0
    private var initialY = 0
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isDragging = false

    private val idleRunnable =
        Runnable {
            if (!isExpanded) {
                isIdle = true
            }
        }

    fun init() {
        updateState()
    }

    fun updateState() {
        val enabled = settingsRepository.isQuickBallEnabled()
        if (!enabled) {
            removeOverlay()
            return
        }

        ballSizeDp = settingsRepository.getQuickBallSize()
        idleAlpha = settingsRepository.getQuickBallIdleAlpha()
        isAutoHide = settingsRepository.isQuickBallAutoHide()
        isHapticEnabled = settingsRepository.isQuickBallHaptic()
        isDockedLeft = settingsRepository.getQuickBallPositionSide() == 1
        currentActions =
            listOf(
                settingsRepository.getQuickBallSlotAction(1),
                settingsRepository.getQuickBallSlotAction(2),
                settingsRepository.getQuickBallSlotAction(3),
                settingsRepository.getQuickBallSlotAction(4),
                settingsRepository.getQuickBallSlotAction(5),
            )

        if (!isOverlayAdded) {
            showOverlay()
        } else {
            updateWindowDimensions()
        }
    }

    fun onScreenOff() {
        mainHandler.post {
            collapseMenu(animate = false)
            isIdle = true
        }
    }

    private fun showOverlay() {
        if (isOverlayAdded) return

        val density = service.resources.displayMetrics.density
        val ballSizePx = (ballSizeDp * density).roundToInt()
        val metrics = getDisplayMetrics()

        val savedSide = settingsRepository.getQuickBallPositionSide()
        isDockedLeft = savedSide == 1

        val initialYPos =
            (metrics.heightPixels * settingsRepository.getQuickBallPositionY())
                .roundToInt()
                .coerceIn(100, (metrics.heightPixels - ballSizePx - 100).coerceAtLeast(100))

        windowParams.width = ballSizePx
        windowParams.height = ballSizePx
        windowParams.x = if (isDockedLeft) 0 else (metrics.widthPixels - ballSizePx)
        windowParams.y = initialYPos

        val owner = OverlayLifecycleOwner().also { it.onCreate() }
        lifecycleOwner = owner

        val compose =
            ComposeView(service).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    EssentialsTheme {
                        if (isExpanded) {
                            val curX = (windowParams.x + (ballSizePx / 2)).toFloat()
                            val curY = (windowParams.y + (ballSizePx / 2)).toFloat()
                            QuickBallExpandedOverlay(
                                ballCenterX = curX,
                                ballCenterY = curY,
                                isDockedLeft = isDockedLeft,
                                actions = currentActions,
                                onActionClick = { action ->
                                    handleActionClick(action)
                                },
                                onDismissRequest = {
                                    collapseMenu()
                                },
                            )
                        } else {
                            QuickBallFloatingBall(
                                sizeDp = ballSizeDp,
                                idleAlpha = idleAlpha,
                                isIdle = isIdle,
                                isAutoHide = isAutoHide,
                                isDockedLeft = isDockedLeft,
                            )
                        }
                    }
                }
            }
        composeView = compose

        val root =
            FrameLayout(service).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                addView(compose)
                setOnTouchListener { v, event ->
                    handleTouchEvent(event)
                }
            }
        overlayRoot = root

        try {
            windowManager.addView(root, windowParams)
            isOverlayAdded = true
            resetIdleTimer()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add Quick Ball overlay", e)
        }
    }

    private fun handleTouchEvent(event: MotionEvent): Boolean {
        if (isExpanded) {
            // While expanded, Compose handles touches via fullscreen gestures
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                initialX = windowParams.x
                initialY = windowParams.y
                touchDownX = event.rawX
                touchDownY = event.rawY
                isIdle = false
                mainHandler.removeCallbacks(idleRunnable)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - touchDownX
                val dy = event.rawY - touchDownY

                if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    isDragging = true
                }

                if (isDragging) {
                    val metrics = getDisplayMetrics()
                    val density = service.resources.displayMetrics.density
                    val ballSizePx = (ballSizeDp * density).roundToInt()

                    windowParams.x = (initialX + dx.roundToInt()).coerceIn(0, metrics.widthPixels - ballSizePx)
                    windowParams.y = (initialY + dy.roundToInt()).coerceIn(60, metrics.heightPixels - ballSizePx - 60)
                    try {
                        windowManager.updateViewLayout(overlayRoot, windowParams)
                    } catch (_: Exception) {}
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isDragging) {
                    // Tap detected: expand menu
                    expandMenu()
                } else {
                    // Drag released: snap to nearest edge
                    snapToEdge()
                }
                return true
            }
        }
        return false
    }

    private fun expandMenu() {
        if (isExpanded) return
        isExpanded = true
        isIdle = false
        mainHandler.removeCallbacks(idleRunnable)

        if (isHapticEnabled) {
            HapticUtil.performHapticForService(service)
        }

        // Expand window to fullscreen so the radial buttons and tap-outside scrim work
        val metrics = getDisplayMetrics()
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.height = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.x = 0
        windowParams.y = 0
        try {
            windowManager.updateViewLayout(overlayRoot, windowParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to expand window", e)
        }
    }

    private fun collapseMenu(animate: Boolean = true) {
        if (!isExpanded && isOverlayAdded) return
        isExpanded = false

        if (isHapticEnabled && animate) {
            HapticUtil.performHapticForService(service)
        }

        updateWindowDimensions()
        resetIdleTimer()
    }

    private fun handleActionClick(action: Action) {
        if (isHapticEnabled) {
            HapticUtil.performHapticForService(service)
        }
        collapseMenu()
        scope.launch {
            CombinedActionExecutor.execute(service, action)
        }
    }

    private fun snapToEdge() {
        val metrics = getDisplayMetrics()
        val density = service.resources.displayMetrics.density
        val ballSizePx = (ballSizeDp * density).roundToInt()

        val screenWidth = metrics.widthPixels
        val currentX = windowParams.x
        val snapToLeft = currentX < (screenWidth / 2)
        isDockedLeft = snapToLeft
        val targetX = if (snapToLeft) 0 else (screenWidth - ballSizePx)

        val animator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 240L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                windowParams.x = anim.animatedValue as Int
                try {
                    windowManager.updateViewLayout(overlayRoot, windowParams)
                } catch (_: Exception) {}
            }
        }
        animator.start()

        // Persist position
        settingsRepository.setQuickBallPositionSide(if (snapToLeft) 1 else 0)
        val yRatio = (windowParams.y.toFloat() / metrics.heightPixels.toFloat()).coerceIn(0.1f, 0.9f)
        settingsRepository.setQuickBallPositionY(yRatio)

        resetIdleTimer()
    }

    private fun updateWindowDimensions() {
        if (!isOverlayAdded || overlayRoot == null) return
        val density = service.resources.displayMetrics.density
        val ballSizePx = (ballSizeDp * density).roundToInt()
        val metrics = getDisplayMetrics()

        if (!isExpanded) {
            windowParams.width = ballSizePx
            windowParams.height = ballSizePx
            windowParams.x = if (isDockedLeft) 0 else (metrics.widthPixels - ballSizePx)
            val savedYRatio = settingsRepository.getQuickBallPositionY()
            windowParams.y =
                (metrics.heightPixels * savedYRatio)
                    .roundToInt()
                    .coerceIn(60, metrics.heightPixels - ballSizePx - 60)
        } else {
            windowParams.width = WindowManager.LayoutParams.MATCH_PARENT
            windowParams.height = WindowManager.LayoutParams.MATCH_PARENT
            windowParams.x = 0
            windowParams.y = 0
        }

        try {
            windowManager.updateViewLayout(overlayRoot, windowParams)
        } catch (_: Exception) {}
    }

    private fun resetIdleTimer() {
        mainHandler.removeCallbacks(idleRunnable)
        mainHandler.postDelayed(idleRunnable, IDLE_TIMEOUT_MS)
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    fun removeOverlay() {
        mainHandler.removeCallbacks(idleRunnable)
        if (isOverlayAdded && overlayRoot != null) {
            try {
                windowManager.removeView(overlayRoot)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove Quick Ball overlay", e)
            }
            isOverlayAdded = false
            overlayRoot = null
            composeView = null
        }
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    fun destroy() {
        removeOverlay()
        scope.cancel()
    }
}
