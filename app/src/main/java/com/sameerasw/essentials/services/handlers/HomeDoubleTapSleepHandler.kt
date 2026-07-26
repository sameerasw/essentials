package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.TouchInteractionController
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.utils.performHapticFeedback
import kotlin.math.abs

class HomeDoubleTapSleepHandler(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val keyguardManager =
        service.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val powerManager = service.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val accessibilityManager =
        service.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    private val handler = Handler(Looper.getMainLooper())
    private val prefs =
        service.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
    private val viewConfig = ViewConfiguration.get(service)
    private val touchSlop = viewConfig.scaledTouchSlop
    private val doubleTapSlop = viewConfig.scaledDoubleTapSlop
    private val vibrator = getVibratorInstance()

    private var touchController: TouchControllerBridge? = null
    private var accessibilityListenersRegistered = false
    private var isLauncherActive = false
    private var isLockScreenActive = false
    private var isScreenInteractive = true
    private var controllerHealthy = true
    private var gestureInProgress = false
    private var downX = 0f
    private var downY = 0f
    private var downSurface = HomeDoubleTapSurface.NONE
    private var lastTapTime = 0L
    private var lastTapRawX = 0f
    private var lastTapRawY = 0f
    private var lastTapSurface = HomeDoubleTapSurface.NONE

    private val updateRunnable = Runnable { refreshControllerState() }
    private val longPressRunnable = Runnable {
        if (gestureInProgress) {
            resetTapHistory()
            delegateCurrentInteraction()
        }
    }
    private val accessibilityStateListener =
        AccessibilityManager.AccessibilityStateChangeListener { scheduleUpdate() }
    private val touchExplorationStateListener =
        AccessibilityManager.TouchExplorationStateChangeListener { scheduleUpdate() }

    fun onServiceConnected() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (touchController == null) {
            touchController = Api33TouchController()
        }
        registerAccessibilityListeners()
        controllerHealthy = true
        scheduleUpdate()
    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isEnabled() || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val packageName = event.packageName?.toString()
        val isRelevantPackage = packageName == HomeDoubleTapSleepRules.PIXEL_LAUNCHER_PACKAGE ||
                packageName == HomeDoubleTapSleepRules.SYSTEM_UI_PACKAGE ||
                (packageName == null && (isLauncherActive || isLockScreenActive))
        if (isRelevantPackage && event.eventType in RELEVANT_EVENT_TYPES) {
            scheduleUpdate()
        }
    }

    fun onForegroundPackageChanged(isLauncher: Boolean, isLockScreen: Boolean) {
        isLauncherActive = isLauncher
        isLockScreenActive = isLockScreen
        if (isLauncher || isLockScreen) {
            scheduleUpdate()
        } else {
            deactivateCapture()
        }
    }

    fun onScreenInteractiveChanged(isInteractive: Boolean) {
        isScreenInteractive = isInteractive
        if (isInteractive) {
            scheduleUpdate()
        } else {
            deactivateCapture()
        }
    }

    fun onUserPresent() {
        scheduleUpdate()
    }

    fun onConfigurationChanged() {
        scheduleUpdate()
    }

    fun onSettingChanged() {
        controllerHealthy = true
        if (isEnabled()) {
            scheduleUpdate()
        } else {
            deactivateCapture()
        }
    }

    // Kept for the existing service lifecycle call sites.
    fun removeOverlays() {
        deactivateCapture()
    }

    fun destroy() {
        unregisterAccessibilityListeners()
        touchController?.destroy()
        touchController = null
        handler.removeCallbacksAndMessages(null)
        resetGestureState()
    }

    private fun scheduleUpdate() {
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, UPDATE_DEBOUNCE_MS)
    }

    private fun refreshControllerState() {
        if (!canCaptureTouches()) {
            deactivateCapture()
            return
        }

        val snapshot = buildSurfaceSnapshot()
        touchController?.setCaptureEnabled(snapshot.surface != HomeDoubleTapSurface.NONE)
    }

    private fun canCaptureTouches(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                isEnabled() &&
                controllerHealthy &&
                isScreenInteractive &&
                powerManager.isInteractive &&
                !hasCompetingTouchExplorationService()
    }

    private fun deactivateCapture() {
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(longPressRunnable)
        resetGestureState()
        touchController?.setCaptureEnabled(false)
    }

    private fun isEnabled(): Boolean {
        return prefs.getBoolean(SettingsRepository.KEY_HOME_DOUBLE_TAP_SLEEP_ENABLED, false)
    }

    private fun handleMotionEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleActionDown(event)

            MotionEvent.ACTION_POINTER_DOWN -> {
                resetTapHistory()
                delegateCurrentInteraction()
            }

            MotionEvent.ACTION_MOVE -> {
                if (!gestureInProgress) return
                if (event.pointerCount > 1 ||
                    abs(event.rawX - downX) > touchSlop ||
                    abs(event.rawY - downY) > touchSlop
                ) {
                    resetTapHistory()
                    delegateCurrentInteraction()
                }
            }

            MotionEvent.ACTION_UP -> handleActionUp(event)

            MotionEvent.ACTION_CANCEL -> resetGestureState()
        }
    }

    private fun handleActionDown(event: MotionEvent) {
        handler.removeCallbacks(longPressRunnable)

        val snapshot = buildSurfaceSnapshot()
        if (!isBlankPoint(snapshot, event.rawX, event.rawY)) {
            resetTapHistory()
            delegateCurrentInteraction()
            scheduleUpdate()
            return
        }

        gestureInProgress = true
        downX = event.rawX
        downY = event.rawY
        downSurface = snapshot.surface
        handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
    }

    private fun handleActionUp(event: MotionEvent) {
        if (!gestureInProgress) return
        handler.removeCallbacks(longPressRunnable)

        val snapshot = buildSurfaceSnapshot()
        val isStillBlank = snapshot.surface == downSurface &&
                isBlankPoint(snapshot, event.rawX, event.rawY)
        if (!isStillBlank) {
            resetTapHistory()
            delegateCurrentInteraction()
            scheduleUpdate()
            return
        }

        val now = event.eventTime
        val isDoubleTap = lastTapSurface == downSurface &&
                HomeDoubleTapSleepRules.isDoubleTap(
                    firstTapTime = lastTapTime,
                    secondTapTime = now,
                    firstX = lastTapRawX,
                    firstY = lastTapRawY,
                    secondX = event.rawX,
                    secondY = event.rawY,
                    slop = doubleTapSlop,
                    timeoutMs = DOUBLE_TAP_TIMEOUT_MS
                )

        if (isDoubleTap) {
            resetGestureState()
            if (canCaptureTouches() && snapshot.surface != HomeDoubleTapSurface.NONE) {
                triggerLockScreen()
            }
            return
        }

        lastTapTime = now
        lastTapRawX = event.rawX
        lastTapRawY = event.rawY
        lastTapSurface = downSurface
        resetCurrentInteraction()
        vibrator?.let { performHapticFeedback(it, HapticFeedbackType.TICK) }
        delegateCurrentInteraction()
    }

    private fun isBlankPoint(snapshot: SurfaceSnapshot, rawX: Float, rawY: Float): Boolean {
        if (snapshot.surface == HomeDoubleTapSurface.NONE) return false
        val x = rawX.toInt()
        val y = rawY.toInt()
        if (!snapshot.usableBounds.contains(x, y)) return false
        return snapshot.occupiedBounds.none { it.contains(x, y) }
    }

    private fun delegateCurrentInteraction() {
        handler.removeCallbacks(longPressRunnable)
        resetCurrentInteraction()
        if (touchController?.requestDelegating() == true) return

        Log.w(TAG, "Unable to delegate touch interaction; suspending double tap handling")
        controllerHealthy = false
        handler.post { deactivateCapture() }
    }

    private fun triggerLockScreen() {
        vibrator?.let { performHapticFeedback(it, HapticFeedbackType.CLICK) }
        if (!service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) {
            Log.w(TAG, "GLOBAL_ACTION_LOCK_SCREEN was not available")
        }
    }

    private fun buildSurfaceSnapshot(): SurfaceSnapshot {
        if (!isScreenInteractive || !powerManager.isInteractive) return SurfaceSnapshot.NONE

        val root = service.rootInActiveWindow ?: return SurfaceSnapshot.NONE
        return try {
            val packageName = root.packageName?.toString() ?: return SurfaceSnapshot.NONE
            val usableBounds = getUsableDisplayBounds()
            if (usableBounds.isEmpty) return SurfaceSnapshot.NONE

            val scan = TreeScan()
            scanAccessibilityTree(root, usableBounds, scan)
            val surface = classifySurface(packageName, scan)
            if (surface == HomeDoubleTapSurface.NONE || scan.occupiedBounds.isEmpty()) {
                SurfaceSnapshot.NONE
            } else {
                SurfaceSnapshot(surface, usableBounds, scan.occupiedBounds)
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to inspect the active accessibility tree", error)
            SurfaceSnapshot.NONE
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun scanAccessibilityTree(
        node: AccessibilityNodeInfo,
        usableBounds: Rect,
        scan: TreeScan
    ) {
        if (!node.isVisibleToUser) return

        val resourceId = node.viewIdResourceName?.lowercase().orEmpty()
        val shortId = resourceId.substringAfterLast('/')
        scan.hasWorkspace = scan.hasWorkspace || shortId == "workspace" ||
                shortId == "workspace_page_container"
        scan.hasLauncherExclusion = scan.hasLauncherExclusion ||
                HomeDoubleTapSleepRules.isLauncherExcludedId(shortId)
        scan.hasLockScreenMarker = scan.hasLockScreenMarker ||
                HomeDoubleTapSleepRules.isLockScreenMarkerId(shortId)
        scan.hasLockScreenExclusion = scan.hasLockScreenExclusion ||
                HomeDoubleTapSleepRules.isLockScreenExcludedId(shortId) ||
                node.isPassword || node.isEditable

        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty && Rect.intersects(rect, usableBounds)) {
            rect.intersect(usableBounds)
            if (isMeaningfulOccupiedNode(node, rect, usableBounds)) {
                scan.occupiedBounds.add(rect.withPadding(occupiedPaddingPx(), usableBounds))
            }
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            scanAccessibilityTree(child, usableBounds, scan)
            @Suppress("DEPRECATION")
            child.recycle()
        }
    }

    private fun classifySurface(packageName: String, scan: TreeScan): HomeDoubleTapSurface {
        return HomeDoubleTapSleepRules.classifySurface(
            packageName = packageName,
            isKeyguardLocked = keyguardManager.isKeyguardLocked,
            hasWorkspace = scan.hasWorkspace,
            hasLauncherExclusion = scan.hasLauncherExclusion,
            hasLockScreenMarker = scan.hasLockScreenMarker,
            hasLockScreenExclusion = scan.hasLockScreenExclusion
        )
    }

    private fun isMeaningfulOccupiedNode(
        node: AccessibilityNodeInfo,
        rect: Rect,
        usableBounds: Rect
    ): Boolean {
        if (rect.width() < minNodeSizePx() || rect.height() < minNodeSizePx()) return false

        val isInteractive = node.isClickable || node.isLongClickable || node.isFocusable ||
                node.isScrollable || node.isEditable
        val hasLabel = !node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()
        if (!isInteractive && !hasLabel) return false

        val isNearlyFullScreen =
            rect.width() >= usableBounds.width() * FULL_WIDTH_RATIO &&
                    rect.height() >= usableBounds.height() * FULL_HEIGHT_RATIO
        return !isNearlyFullScreen
    }

    private fun hasCompetingTouchExplorationService(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                val resolvedService = info.resolveInfo?.serviceInfo ?: return@any false
                val isThisService = resolvedService.packageName == service.packageName &&
                        resolvedService.name == service.javaClass.name
                !isThisService &&
                        info.flags and AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE != 0
            }
    }

    private fun registerAccessibilityListeners() {
        if (accessibilityListenersRegistered) return
        accessibilityManager.addAccessibilityStateChangeListener(accessibilityStateListener)
        accessibilityManager.addTouchExplorationStateChangeListener(touchExplorationStateListener)
        accessibilityListenersRegistered = true
    }

    private fun unregisterAccessibilityListeners() {
        if (!accessibilityListenersRegistered) return
        accessibilityManager.removeAccessibilityStateChangeListener(accessibilityStateListener)
        accessibilityManager.removeTouchExplorationStateChangeListener(touchExplorationStateListener)
        accessibilityListenersRegistered = false
    }

    private fun resetCurrentInteraction() {
        handler.removeCallbacks(longPressRunnable)
        gestureInProgress = false
        downX = 0f
        downY = 0f
        downSurface = HomeDoubleTapSurface.NONE
    }

    private fun resetTapHistory() {
        lastTapTime = 0L
        lastTapRawX = 0f
        lastTapRawY = 0f
        lastTapSurface = HomeDoubleTapSurface.NONE
    }

    private fun resetGestureState() {
        resetCurrentInteraction()
        resetTapHistory()
    }

    private fun getUsableDisplayBounds(): Rect {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return Rect()
        val metrics = windowManager.currentWindowMetrics
        val bounds = Rect(metrics.bounds)
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
        )
        return Rect(
            bounds.left + insets.left,
            bounds.top + insets.top,
            bounds.right - insets.right,
            bounds.bottom - insets.bottom
        )
    }

    private fun Rect.withPadding(padding: Int, bounds: Rect): Rect {
        return Rect(
            (left - padding).coerceAtLeast(bounds.left),
            (top - padding).coerceAtLeast(bounds.top),
            (right + padding).coerceAtMost(bounds.right),
            (bottom + padding).coerceAtMost(bounds.bottom)
        )
    }

    private fun minNodeSizePx() = dpToPx(MIN_NODE_SIZE_DP)

    private fun occupiedPaddingPx() = dpToPx(OCCUPIED_PADDING_DP)

    private fun dpToPx(dp: Int): Int {
        return (dp * service.resources.displayMetrics.density).toInt()
    }

    private fun getVibratorInstance(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            service.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            service.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private interface TouchControllerBridge {
        fun setCaptureEnabled(enabled: Boolean)
        fun requestDelegating(): Boolean
        fun destroy()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private inner class Api33TouchController : TouchControllerBridge {
        private val controller = service.getTouchInteractionController(Display.DEFAULT_DISPLAY)
        private var callbackRegistered = false
        private var captureEnabled = false
        private val callback = object : TouchInteractionController.Callback {
            override fun onMotionEvent(event: MotionEvent) {
                handleMotionEvent(event)
            }

            override fun onStateChanged(state: Int) {
                if (state == TouchInteractionController.STATE_CLEAR) {
                    resetCurrentInteraction()
                }
            }
        }

        init {
            controller.registerCallback(service.mainExecutor, callback)
            callbackRegistered = true
        }

        override fun setCaptureEnabled(enabled: Boolean) {
            if (captureEnabled == enabled) return
            val info = service.serviceInfo ?: return
            val requestedFlag = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            val updatedFlags = if (enabled) {
                info.flags or requestedFlag
            } else {
                info.flags and requestedFlag.inv()
            }
            if (updatedFlags != info.flags) {
                info.flags = updatedFlags
                service.serviceInfo = info
            }
            captureEnabled = enabled
        }

        override fun requestDelegating(): Boolean {
            return runCatching {
                if (controller.state != TouchInteractionController.STATE_TOUCH_INTERACTING) {
                    return@runCatching false
                }
                controller.requestDelegating()
                true
            }.onFailure {
                Log.w(TAG, "Touch interaction delegation failed", it)
            }.getOrDefault(false)
        }

        override fun destroy() {
            setCaptureEnabled(false)
            if (callbackRegistered) {
                controller.unregisterCallback(callback)
                callbackRegistered = false
            }
        }
    }

    private data class SurfaceSnapshot(
        val surface: HomeDoubleTapSurface,
        val usableBounds: Rect,
        val occupiedBounds: List<Rect>
    ) {
        companion object {
            val NONE = SurfaceSnapshot(HomeDoubleTapSurface.NONE, Rect(), emptyList())
        }
    }

    private class TreeScan {
        var hasWorkspace = false
        var hasLauncherExclusion = false
        var hasLockScreenMarker = false
        var hasLockScreenExclusion = false
        val occupiedBounds = mutableListOf<Rect>()
    }

    companion object {
        private const val TAG = "HomeDoubleTapSleep"
        private const val UPDATE_DEBOUNCE_MS = 175L
        private const val DOUBLE_TAP_TIMEOUT_MS = 500L
        private const val MIN_NODE_SIZE_DP = 4
        private const val OCCUPIED_PADDING_DP = 12
        private const val FULL_WIDTH_RATIO = 0.9f
        private const val FULL_HEIGHT_RATIO = 0.75f

        private val RELEVANT_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_CLICKED
        )

    }
}

internal enum class HomeDoubleTapSurface {
    NONE,
    PIXEL_HOME,
    LOCK_SCREEN
}

internal object HomeDoubleTapSleepRules {
    const val PIXEL_LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher"
    const val SYSTEM_UI_PACKAGE = "com.android.systemui"

    private val launcherExcludedIds = listOf(
        "apps_view",
        "search_container_all_apps",
        "overview_panel",
        "primary_widgets_list_view",
        "widgets_full_sheet",
        "popup_container",
        "folder_content",
        "drop_target_bar"
    )

    private val lockScreenMarkerIds = listOf(
        "keyguard_root",
        "keyguard_status",
        "keyguard_clock",
        "lockscreen_clock",
        "lock_icon",
        "keyguard_indication"
    )

    private val lockScreenExcludedIds = listOf(
        "bouncer",
        "keyguard_security",
        "keyguard_pin",
        "keyguard_password",
        "keyguard_pattern",
        "password_entry",
        "pin_entry",
        "lock_pattern",
        "emergency",
        "quick_qs",
        "qs_panel",
        "qs_frame",
        "expanded_qs",
        "quick_settings"
    )

    fun isLauncherExcludedId(resourceId: String): Boolean {
        return launcherExcludedIds.any(resourceId::contains)
    }

    fun isLockScreenMarkerId(resourceId: String): Boolean {
        return lockScreenMarkerIds.any(resourceId::contains)
    }

    fun isLockScreenExcludedId(resourceId: String): Boolean {
        return lockScreenExcludedIds.any(resourceId::contains)
    }

    fun classifySurface(
        packageName: String,
        isKeyguardLocked: Boolean,
        hasWorkspace: Boolean,
        hasLauncherExclusion: Boolean,
        hasLockScreenMarker: Boolean,
        hasLockScreenExclusion: Boolean
    ): HomeDoubleTapSurface {
        if (packageName == PIXEL_LAUNCHER_PACKAGE &&
            !isKeyguardLocked &&
            hasWorkspace &&
            !hasLauncherExclusion
        ) {
            return HomeDoubleTapSurface.PIXEL_HOME
        }

        if (packageName == SYSTEM_UI_PACKAGE &&
            isKeyguardLocked &&
            hasLockScreenMarker &&
            !hasLockScreenExclusion
        ) {
            return HomeDoubleTapSurface.LOCK_SCREEN
        }

        return HomeDoubleTapSurface.NONE
    }

    fun isDoubleTap(
        firstTapTime: Long,
        secondTapTime: Long,
        firstX: Float,
        firstY: Float,
        secondX: Float,
        secondY: Float,
        slop: Int,
        timeoutMs: Long
    ): Boolean {
        return secondTapTime - firstTapTime in 0..timeoutMs &&
                abs(secondX - firstX) <= slop &&
                abs(secondY - firstY) <= slop
    }
}
