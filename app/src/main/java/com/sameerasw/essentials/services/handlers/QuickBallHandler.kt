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
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.PathInterpolator
import androidx.core.content.getSystemService
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.island.ui.IslandHaptics
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import com.sameerasw.essentials.ui.activities.LockscreenActionActivity
import com.sameerasw.essentials.ui.features.system.quickball.FloatPanelView
import com.sameerasw.essentials.ui.features.system.quickball.FloatTouchView
import com.sameerasw.essentials.ui.features.system.quickball.GestureListener
import com.sameerasw.essentials.ui.features.system.quickball.SideKickView
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
        private const val STASH_DELAY_MS = 2500L
        private const val EDGE_PADDING_DP = 4f
    }

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val keyguardManager by lazy { service.getSystemService<KeyguardManager>() }
    private val settings = SettingsRepository(service)
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val density get() = service.resources.displayMetrics.density
    private val fabSizePx get() = (settings.getQuickBallSize() * density).roundToInt()
    private val edgePaddingPx get() = (EDGE_PADDING_DP * density).roundToInt()
    private val topBoundary get() = (80f * density).roundToInt()
    private val bottomBoundary get() = (80f * density).roundToInt()

    // Views & LayoutParams
    private var fabView: FloatTouchView? = null
    private var fabParams: WindowManager.LayoutParams? = null

    private var pillView: SideKickView? = null
    private var pillParams: WindowManager.LayoutParams? = null

    private var menuView: FloatPanelView? = null
    private var menuParams: WindowManager.LayoutParams? = null

    // Position State
    private var isOnRight = true
    private var fabX = 0
    private var fabY = 0

    // Lifecycle & Movement State
    private var isExpanded = false
    private var isDragging = false
    private var isStashed = false
    private var isStashing = false
    private var fabAnimator: ValueAnimator? = null
    private var lastForegroundPackage = ""

    private val stashRunnable = Runnable { stashFab(animated = true) }

    fun init() {
        updateState()
    }

    fun updateState() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { updateState() }
            return
        }

        if (!settings.isQuickBallEnabled()) {
            destroy()
            return
        }

        isOnRight = settings.getQuickBallPositionSide() == 0

        refreshBallVisibility()
        updateFabAppearance()
        updatePillAppearance()
    }

    fun onScreenOff() {
        handler.post {
            collapseMenu()
            stashFab(animated = false)
        }
    }

    fun onForegroundPackage(packageName: String) {
        lastForegroundPackage = packageName
        refreshBallVisibility()
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        refreshBallVisibility()
        recalculatePosition()
    }

    fun onWindowsChanged() {
        refreshBallVisibility()
    }

    fun destroy() {
        scope.cancel()
        stopInactivityTimer()
        removeMenuWindow()
        removePillWindow()
        removeFabWindow()
        handler.removeCallbacksAndMessages(null)
    }

    /* -------------------- Visibility Engine -------------------- */

    private fun refreshBallVisibility() {
        if (!settings.isQuickBallEnabled()) {
            destroy()
            return
        }

        val isLocked = keyguardManager?.isKeyguardLocked == true
        if (isLocked) {
            if (settings.isQuickBallShowLockscreen()) {
                if (!isExpanded) {
                    showBall()
                    if (settings.isQuickBallStickToEdge() && !isStashed && !isStashing) {
                        stashFab(animated = false)
                    }
                }
            } else {
                hideAll()
            }
            return
        }

        val isLandscape = service.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape && settings.isQuickBallHideLandscape()) {
            hideAll()
            return
        }

        val autoHideApps = settings.getQuickBallAutoHideApps()
        if (autoHideApps.contains(lastForegroundPackage)) {
            hideAll()
            return
        }

        showBall()
    }

    private fun showBall() {
        if (isStashed) {
            if (pillView == null && settings.isQuickBallStickToEdge()) {
                showPill()
            }
            return
        }
        if (fabView == null) {
            createFabWindow()
            isStashed = false
            if (settings.isQuickBallStickToEdge()) {
                stashFab(animated = false)
            } else {
                resetInactivityTimer()
            }
        }
    }

    private fun hideAll() {
        stopInactivityTimer()
        removeMenuWindow()
        removePillWindow()
        removeFabWindow()
    }

    /* -------------------- FAB Window Management -------------------- */

    @SuppressLint("ClickableViewAccessibility")
    private fun createFabWindow() {
        if (fabView != null) return

        val (screenWidth, screenHeight) = getScreenDimensions()
        val savedYRatio = settings.getQuickBallPositionY()
        fabY = (screenHeight * savedYRatio).roundToInt().coerceIn(topBoundary, screenHeight - fabSizePx - bottomBoundary)
        fabX = getEdgeX(screenWidth)

        fabParams = createOverlayParams(fabSizePx, fabSizePx).apply {
            x = fabX
            y = fabY
        }

        var initialWindowX = 0
        var initialWindowY = 0
        var screenW = 0
        var screenH = 0

        fabView = FloatTouchView(service).apply {
            setExpanded(isExpanded, animate = false)

            listener = object : GestureListener {
                override fun onTouchDown() {
                    if (isStashing) return
                    stopInactivityTimer()
                    fabAnimator?.cancel()
                    isStashed = false
                    initialWindowX = fabX
                    initialWindowY = fabY

                    val (sw, sh) = getScreenDimensions()
                    screenW = sw
                    screenH = sh
                }

                override fun onTouchCancel() {
                    isDragging = false
                    resetInactivityTimer()
                }

                override fun onDragMove(dx: Float, dy: Float) {
                    if (isStashing) return
                    isDragging = true
                    stopInactivityTimer()
                    alpha = 1.0f

                    fabX = (initialWindowX + dx).roundToInt().coerceIn(0, screenW - fabSizePx)
                    fabY = (initialWindowY + dy).roundToInt().coerceIn(topBoundary, screenH - fabSizePx - bottomBoundary)

                    fabParams?.x = fabX
                    fabParams?.y = fabY
                    updateViewLayoutSafe(this@apply, fabParams)
                }

                override fun onDragEnd() {
                    isDragging = false
                    if (!isStashing) {
                        snapToEdge()
                    }
                }

                override fun onSingleTap() {
                    isDragging = false
                    if (isExpanded) {
                        collapseMenu()
                        return
                    }
                    if (isStashed || isStashing) {
                        unstashFab()
                    }
                    if (settings.isQuickBallHaptic()) {
                        IslandHaptics.openClick(service)
                    }
                    expandMenu()
                }

                override fun onDoubleTap() = executeGestureAction("DOUBLE_TAP")
                override fun onTripleTap() = executeGestureAction("TRIPLE_TAP")
                override fun onLongPress() = executeGestureAction("LONG_PRESS")
                override fun onSwipeUp() = executeGestureAction("SWIPE_UP")
                override fun onSwipeDown() = executeGestureAction("SWIPE_DOWN")
            }
        }

        addViewSafe(fabView ?: return, fabParams ?: return)
        resetInactivityTimer()
    }

    private fun removeFabWindow() {
        fabView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            fabView = null
        }
        fabParams = null
    }

    private fun updateFabAppearance() {
        val fab = fabView ?: return
        val params = fabParams ?: return

        params.width = fabSizePx
        params.height = fabSizePx
        fab.updateAppearance()
        if (!isStashed && !isStashing) {
            updateViewLayoutSafe(fab, params)
        }
    }

    private fun snapToEdge() {
        val (screenWidth, screenHeight) = getScreenDimensions()
        isOnRight = (fabX + fabSizePx / 2) > screenWidth / 2
        settings.setQuickBallPositionSide(if (isOnRight) 0 else 1)

        val targetYRatio = if (screenHeight > 0) fabY.toFloat() / screenHeight else 0.5f
        settings.setQuickBallPositionY(targetYRatio)

        val targetX = getEdgeX(screenWidth)
        val distance = abs(targetX - fabX)
        val duration = (distance.toFloat() / screenWidth * 320f).coerceIn(160f, 300f).toLong()

        animateFabX(targetX, duration) {
            if (settings.isQuickBallHaptic()) {
                IslandHaptics.thresholdReached(service)
            }
            resetInactivityTimer()
        }
    }

    private fun stashFab(animated: Boolean = true) {
        if (isExpanded) return
        val view = fabView ?: return
        val params = fabParams ?: return

        if (!settings.isQuickBallStickToEdge()) {
            val idleAlpha = settings.getQuickBallIdleAlpha()
            if (animated) {
                animateFabX(fabX, 220L, alpha = idleAlpha)
            } else {
                view.alpha = idleAlpha
            }
            return
        }

        if (isStashed) return
        val (screenWidth, _) = getScreenDimensions()
        val targetX = if (isOnRight) screenWidth + 100 else -fabSizePx - 100

        fabAnimator?.cancel()
        val idleAlpha = settings.getQuickBallIdleAlpha()

        if (animated) {
            isStashing = true
            animateFabX(targetX, 240L, alpha = 0f) {
                isStashing = false
                isStashed = true
                view.visibility = View.GONE
                view.alpha = 0f
                params.x = targetX
                updateViewLayoutSafe(view, params)
                showPill()
            }
        } else {
            view.visibility = View.GONE
            view.alpha = 0f
            fabX = targetX
            params.x = targetX
            updateViewLayoutSafe(view, params)
            isStashed = true
            isStashing = false
            showPill()
        }
    }

    private fun unstashFab(animated: Boolean = true, onFinished: (() -> Unit)? = null) {
        if (!isStashed && !isStashing) {
            onFinished?.invoke()
            return
        }
        if (fabView == null) {
            createFabWindow()
            isStashed = false
        }
        val view = fabView ?: run {
            onFinished?.invoke()
            return
        }
        val params = fabParams ?: run {
            onFinished?.invoke()
            return
        }

        val (screenWidth, _) = getScreenDimensions()
        val targetX = getEdgeX(screenWidth)

        isStashing = false
        removePillWindow()
        fabAnimator?.cancel()
        view.visibility = View.VISIBLE
        view.alpha = 1.0f

        if (animated) {
            params.x = if (isOnRight) screenWidth - fabSizePx else 0
            updateViewLayoutSafe(view, params)
            fabX = params.x

            animateFabX(targetX, 220L, alpha = 1.0f) {
                isStashed = false
                resetInactivityTimer()
                onFinished?.invoke()
            }
        } else {
            view.alpha = 1.0f
            fabX = targetX
            params.x = targetX
            updateViewLayoutSafe(view, params)
            isStashed = false
            resetInactivityTimer()
            onFinished?.invoke()
        }
    }

    private fun animateFabX(
        targetX: Int,
        duration: Long,
        alpha: Float? = null,
        onEnd: (() -> Unit)? = null,
    ) {
        val view = fabView ?: return
        val params = fabParams ?: return
        val startAlpha = view.alpha

        fabAnimator?.cancel()
        val animator = ValueAnimator.ofInt(fabX, targetX).apply {
            this.duration = duration
            interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
            addUpdateListener { anim ->
                val currentX = anim.animatedValue as Int
                fabX = currentX
                params.x = currentX
                updateViewLayoutSafe(view, params)

                if (alpha != null) {
                    val fraction = anim.animatedFraction
                    view.alpha = startAlpha + (alpha - startAlpha) * fraction
                }
            }
            var isCancelled = false
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    isCancelled = true
                }
                override fun onAnimationEnd(animation: Animator) {
                    if (!isCancelled) {
                        onEnd?.invoke()
                    }
                }
            })
        }
        fabAnimator = animator
        animator.start()
    }

    /* -------------------- Stashed Pill Window -------------------- */

    @SuppressLint("ClickableViewAccessibility")
    private fun showPill() {
        if (pillView != null) return

        val pillWidth = (settings.getQuickBallPillTouchWidth() * density).roundToInt()
        val pillHeight = (settings.getQuickBallPillHeight() * density).roundToInt()
        val (screenWidth, _) = getScreenDimensions()

        val targetX = if (isOnRight) screenWidth - pillWidth else 0
        val targetY = fabY + (fabSizePx - pillHeight) / 2

        pillView = SideKickView(service).apply {
            onRight = isOnRight
            listener = object : GestureListener {
                override fun onSingleTap() {
                    if (settings.isQuickBallHaptic()) {
                        IslandHaptics.openClick(service)
                    }
                    removePillWindow()
                    unstashFab(animated = false)
                    expandMenu()
                }
                override fun onDoubleTap() = executeGestureAction("DOUBLE_TAP")
                override fun onTripleTap() = executeGestureAction("TRIPLE_TAP")
                override fun onLongPress() = executeGestureAction("LONG_PRESS")
                override fun onSwipeUp() = executeGestureAction("SWIPE_UP")
                override fun onSwipeDown() = executeGestureAction("SWIPE_DOWN")
            }
        }

        pillParams = createOverlayParams(pillWidth, pillHeight).apply {
            x = targetX
            y = targetY
        }

        addViewSafe(pillView ?: return, pillParams ?: return)
    }

    private fun removePillWindow() {
        pillView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            pillView = null
        }
        pillParams = null
    }

    private fun updatePillAppearance() {
        val pill = pillView ?: return
        val params = pillParams ?: return

        val pillWidth = (settings.getQuickBallPillTouchWidth() * density).roundToInt()
        val pillHeight = (settings.getQuickBallPillHeight() * density).roundToInt()
        val (screenWidth, _) = getScreenDimensions()

        params.width = pillWidth
        params.height = pillHeight
        params.x = if (isOnRight) screenWidth - pillWidth else 0
        params.y = fabY + (fabSizePx - pillHeight) / 2

        pill.update()
        updateViewLayoutSafe(pill, params)
    }

    private fun executeGestureAction(gestureName: String) {
        if (!settings.isQuickBallGesturesEnabled()) return
        if (settings.isQuickBallHaptic()) {
            IslandHaptics.commit(service)
        }
        val action = settings.getQuickBallGestureAction(gestureName) ?: return
        executeAction(action)
        resetInactivityTimer()
    }

    /* -------------------- Expanded Radial Menu Window -------------------- */

    private fun expandMenu() {
        if (isExpanded) return
        isExpanded = true
        fabView?.setExpanded(true)
        stopInactivityTimer()
        if (settings.isQuickBallHaptic()) {
            IslandHaptics.openClick(service)
        }

        val (screenWidth, screenHeight) = getScreenDimensions()
        val actualFabX = getEdgeX(screenWidth)
        val density = service.resources.displayMetrics.density

        val radiusPx = (settings.getQuickBallMenuRadius() * density).toInt()
        val buttonSizePx = (settings.getQuickBallMenuSize() * density).toInt()
        val maxReach = radiusPx + buttonSizePx + (24 * density).toInt()

        val isOnRight = actualFabX + fabSizePx / 2 > screenWidth / 2
        val menuWidth = (maxReach + fabSizePx).coerceAtMost(screenWidth)
        val menuHeight = (2 * maxReach + fabSizePx).coerceAtMost(screenHeight)

        val windowX = if (isOnRight) screenWidth - menuWidth else 0
        val windowY = (fabY + fabSizePx / 2 - menuHeight / 2).coerceIn(0, (screenHeight - menuHeight).coerceAtLeast(0))

        val localFabX = if (isOnRight) menuWidth - fabSizePx - edgePaddingPx else edgePaddingPx
        val localFabY = fabY - windowY

        val slotCount = settings.getQuickBallSlotCount()
        val actionList = (1..slotCount).map { settings.getQuickBallSlotAction(it) }

        menuView = FloatPanelView(
            context = service,
            fabX = localFabX,
            fabY = localFabY,
            fabSize = fabSizePx,
            actions = actionList,
            isOnRight = isOnRight,
            panelHeight = menuHeight,
            onDismiss = {
                collapseMenu()
            },
            onDismissFinished = {
                removeMenuWindow()
                if (keyguardManager?.isKeyguardLocked == true) {
                    stashFab(animated = false)
                } else {
                    resetInactivityTimer()
                }
            },
            onActionClicked = { action ->
                collapseMenu()
                executeAction(action)
            },
        )

        menuParams = createOverlayParams(
            menuWidth,
            menuHeight,
        ).apply {
            this.x = windowX
            this.y = windowY
            flags = flags or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        }

        addViewSafe(menuView ?: return, menuParams ?: return)
        menuView?.animateExpand()
    }

    private fun collapseMenu() {
        if (!isExpanded) return
        isExpanded = false
        fabView?.setExpanded(false)

        menuParams?.let { params ->
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            updateViewLayoutSafe(menuView ?: return@let, params)
        }

        menuView?.animateCollapse()
    }

    private fun executeAction(action: Action) {
        if (settings.isQuickBallHaptic()) {
            IslandHaptics.commit(service)
        }
        val isLocked = keyguardManager?.isKeyguardLocked == true
        if (isLocked && opensUi(action)) {
            LockscreenActionActivity.start(service, action)
            return
        }

        when (action) {
            is Action.ToggleFlashlight -> {
                val fh = (service as? ScreenOffAccessibilityService)?.flashlightHandler
                if (fh != null) {
                    fh.toggleFlashlight()
                } else {
                    scope.launch { CombinedActionExecutor.execute(service, action) }
                }
            }
            is Action.TurnOnFlashlight -> {
                val fh = (service as? ScreenOffAccessibilityService)?.flashlightHandler
                if (fh != null) {
                    if (!fh.isTorchOn) fh.toggleFlashlight()
                } else {
                    scope.launch { CombinedActionExecutor.execute(service, action) }
                }
            }
            is Action.TurnOffFlashlight -> {
                val fh = (service as? ScreenOffAccessibilityService)?.flashlightHandler
                if (fh != null) {
                    if (fh.isTorchOn) fh.toggleFlashlight()
                } else {
                    scope.launch { CombinedActionExecutor.execute(service, action) }
                }
            }
            is Action.Back -> {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }
            is Action.Home -> {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }
            is Action.RecentApps -> {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }
            is Action.OpenNotifications -> {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            }
            is Action.OpenQuickSettings -> {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            }
            is Action.PowerDialog -> {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            }
            is Action.LockScreen -> {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            }
            is Action.TakeScreenshot -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }
            else -> {
                scope.launch {
                    CombinedActionExecutor.execute(service, action)
                }
            }
        }
    }

    private fun opensUi(action: Action): Boolean =
        action is Action.OpenApp ||
            action is Action.OpenActivity ||
            action is Action.AIAssistant ||
            action is Action.EssentialSearch ||
            action is Action.OpenNowPlayingApp ||
            action is Action.OpenVideoCamera ||
            action is Action.OpenQrScanner

    private fun removeMenuWindow() {
        menuView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            menuView = null
        }
        menuParams = null
    }

    /* -------------------- Helpers & Calculations -------------------- */

    private fun resetInactivityTimer() {
        stopInactivityTimer()
        if (settings.isQuickBallStickToEdge() && !isExpanded) {
            val delay = settings.getQuickBallStashDelay().coerceIn(500L, 8000L)
            handler.postDelayed(stashRunnable, delay)
        }
    }

    private fun stopInactivityTimer() {
        handler.removeCallbacks(stashRunnable)
    }

    private fun recalculatePosition() {
        val (screenWidth, screenHeight) = getScreenDimensions()
        val savedYRatio = settings.getQuickBallPositionY()
        fabY = (screenHeight * savedYRatio).roundToInt().coerceIn(topBoundary, screenHeight - fabSizePx - bottomBoundary)

        if (isStashed || isStashing) {
            updatePillAppearance()
            return
        }

        fabX = getEdgeX(screenWidth)

        fabParams?.let { params ->
            params.x = fabX
            params.y = fabY
            fabView?.let { updateViewLayoutSafe(it, params) }
        }
    }

    private fun getEdgeX(screenWidth: Int = getScreenDimensions().first): Int {
        return if (isOnRight) screenWidth - fabSizePx - edgePaddingPx else edgePaddingPx
    }

    private fun getScreenDimensions(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val dm = service.resources.displayMetrics
            dm.widthPixels to dm.heightPixels
        }
    }

    private fun createOverlayParams(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            this.width = width
            this.height = height
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun addViewSafe(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.addView(view, params)
        } catch (e: WindowManager.BadTokenException) {
            Log.w(TAG, "BadTokenException for overlay type ${params.type}, attempting fallback", e)
            if (params.type == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY &&
                android.provider.Settings.canDrawOverlays(service)) {
                params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                try {
                    windowManager.addView(view, params)
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback to TYPE_APPLICATION_OVERLAY failed", e2)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add window", e)
        }
    }

    private fun updateViewLayoutSafe(view: View, params: ViewGroup.LayoutParams?) {
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }
}
