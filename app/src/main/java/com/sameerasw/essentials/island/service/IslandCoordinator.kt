package com.sameerasw.essentials.island.service

import com.sameerasw.essentials.island.plugins.timer.TimerPlugin
import com.sameerasw.essentials.island.plugins.call.CallPlugin
import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.gestures.CompactGestureController
import com.sameerasw.essentials.island.gestures.CompactGestures
import com.sameerasw.essentials.island.model.IslandPlugin
import com.sameerasw.essentials.island.model.IslandPluginContext
import com.sameerasw.essentials.island.model.IslandStage
import com.sameerasw.essentials.island.plugins.calendar.CalendarPlugin
import com.sameerasw.essentials.island.plugins.consciousgate.ConsciousGatePlugin
import com.sameerasw.essentials.island.plugins.flashlight.FlashlightPlugin
import com.sameerasw.essentials.island.plugins.media.MediaPlugin
import com.sameerasw.essentials.island.plugins.notifications.NotificationsPlugin
import com.sameerasw.essentials.island.plugins.timebattery.TimeBatteryPlugin
import com.sameerasw.essentials.island.state.CameraAnchor
import com.sameerasw.essentials.island.state.CameraGeometry
import com.sameerasw.essentials.island.state.Cancellable
import com.sameerasw.essentials.island.state.DelayScheduler
import com.sameerasw.essentials.island.state.IslandController
import com.sameerasw.essentials.island.ui.IslandActions
import com.sameerasw.essentials.island.ui.IslandFontFamily
import com.sameerasw.essentials.island.ui.IslandLayoutSpec
import com.sameerasw.essentials.island.ui.IslandRoot
import com.sameerasw.essentials.utils.ShellUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class IslandCoordinator(
    private val service: AccessibilityService,
) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val settings by lazy { SettingsRepository(service) }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val wm by lazy { service.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val keyguardManager by lazy { service.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }
    private val windowHost by lazy { IslandWindowHost(service, wm) }

    private val controller = IslandController(
        scheduler = DelayScheduler { delayMs, action ->
            val runnable = Runnable(action)
            mainHandler.postDelayed(runnable, delayMs)
            Cancellable { mainHandler.removeCallbacks(runnable) }
        },
        anchorProvider = { geometry?.anchor ?: CameraAnchor.Center },
    )

    private val plugins: List<IslandPlugin> = listOf(
        CallPlugin(),
        TimeBatteryPlugin(),
        NotificationsPlugin(),
        MediaPlugin(),
        CalendarPlugin(),
        ConsciousGatePlugin(),
        FlashlightPlugin(),
        TimerPlugin(),
    )

    private var scope: CoroutineScope? = null
    private var geometry: CameraGeometry? = null
    private val spec = MutableStateFlow(IslandLayoutSpec())

    private var isScreenOff = false
    private var isLandscape = false
    private var isFullscreenApp = false
    private var running = false
    private var foregroundPackage: String? = null
    private var textInputActive = false

    private val isWindowSuppressed get() = isLandscape || isFullscreenApp
    private val isContentSuppressed: Boolean
        get() = isWindowSuppressed ||
            (settings.isIslandHideWhenScreenOffEnabled() && (isScreenOff || keyguardManager?.isKeyguardLocked == true))

    private val compactGestures = CompactGestureController(service, settings) { scope }

    private val actions = object : IslandActions {
        override val compactGestures: CompactGestures get() = this@IslandCoordinator.compactGestures
        override fun onTap(itemKey: String?) = controller.onTap(itemKey)
        override fun onLongPress(itemKey: String?) = controller.onLongPress(itemKey)
        override fun onCollapse() = controller.collapse()
        override fun onDismiss(): Boolean = controller.dismissFocused()
        override fun onInteraction() {
            controller.onUserInteraction()
            val focused = controller.state.value.focusedKey
            plugins.forEach { it.onUserInteraction(focused) }
        }

        override fun onTextInputChanged(active: Boolean) {
            textInputActive = active
            windowHost.setTextInput(active)
        }

        override fun onAdvance(): Boolean = controller.advanceFocused()

        override fun onOpenFocused() {
            controller.state.value.focused?.onOpen?.invoke()
            controller.collapse()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> isScreenOff = true
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> isScreenOff = false
                else -> return
            }
            applySuppression()
            plugins.forEach { it.onScreenStateChanged() }
        }
    }

    init {
        settings.registerOnSharedPreferenceChangeListener(this)
        isLandscape = service.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        service.registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            },
        )
        controller.onStageChanged = { stage ->
            if (stage != IslandStage.Expanded) windowHost.setTextInput(false)
            windowHost.onStageChanged(stage)
            syncStatusBar(stage)
        }
        updateState()
    }

    fun updateState() {
        mainHandler.post {
            val shouldRun = settings.isIslandEnabled() && !isWindowSuppressed
            if (shouldRun) start() else stop()
            if (running) {
                applyConfig()
                applySuppression()
                plugins.forEach { it.refresh() }
            }
        }
    }

    // Anything the user touches outside the island collapses the expanded card, when enabled.
    private fun onOutsideTouch() {
        if (!running || !settings.isIslandDismissOnOutsideEnabled()) return
        if (textInputActive) return
        if (controller.state.value.stage != IslandStage.Expanded) return
        mainHandler.post { controller.collapse() }
    }

    fun onForegroundPackage(packageName: String) {
        if (foregroundPackage == packageName) return
        foregroundPackage = packageName
        applyOwnerAppHiding()
    }

    private fun applyOwnerAppHiding() {
        controller.setHiddenPackage(foregroundPackage.takeIf { settings.isIslandHideInOwnerAppEnabled() })
    }

    fun updateConsciousGateState() {
        mainHandler.post { plugins.filterIsInstance<ConsciousGatePlugin>().forEach { it.refresh() } }
    }

    fun setFullscreen(fullscreen: Boolean) {
        if (isFullscreenApp == fullscreen) return
        isFullscreenApp = fullscreen
        updateState()
    }

    fun onConfigurationChanged() {
        val landscape = service.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (landscape != isLandscape) {
            isLandscape = landscape
            updateState()
        } else if (running) {
            applyConfig()
        }
    }

    fun onDestroy() {
        settings.unregisterOnSharedPreferenceChangeListener(this)
        try {
            service.unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        stop()
    }

    private fun start() {
        if (running) return
        running = true
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope = newScope
        applyConfig()
        val geo = geometry ?: return
        windowHost.onOutsideTouch = ::onOutsideTouch
        val attached = windowHost.attach(geo) {
            val state by controller.state.collectAsState()
            val layoutSpec by spec.collectAsState()
            val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(service) else darkColorScheme()
            MaterialTheme(colorScheme = colors, typography = IslandTypography) {
                val base = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(base.density, base.fontScale * layoutSpec.fontScale),
                ) {
                    IslandRoot(state, layoutSpec, actions, windowHost::onTargetBoundsChanged) { controller.collapseAnimator = it }
                }
            }
        }
        // Fails until the accessibility service is connected; onServiceConnected calls updateState() again.
        if (!attached) {
            running = false
            newScope.cancel()
            scope = null
            return
        }
        val context = IslandPluginContext(
            service = service,
            settings = settings,
            scope = newScope,
            mainHandler = mainHandler,
            request = { request -> mainHandler.post { controller.handle(request) } },
            isContentSuppressed = { isContentSuppressed },
            currentStage = { controller.state.value.stage },
            focusedKey = { controller.state.value.focusedKey },
        )
        plugins.forEach { plugin ->
            plugin.start(context)
            newScope.launch { plugin.items.collect { controller.setItems(plugin.id, it) } }
        }
        if (settings.isIslandSuppressSystemHeadsUpEnabled()) settings.applyHeadsUpSuppression(true)
    }

    private fun stop() {
        if (!running) return
        running = false
        plugins.forEach {
            it.stop()
            controller.setItems(it.id, emptyList())
        }
        scope?.cancel()
        scope = null
        windowHost.detach()
        IslandStatusBarHider.restore(service)
        if (settings.isIslandSuppressSystemHeadsUpEnabled()) settings.applyHeadsUpSuppression(false)
    }

    private fun applyConfig() {
        val geo = CameraGeometryResolver.resolve(service, wm, settings)
        geometry = geo
        val density = service.resources.displayMetrics.density
        val screenWidthDp = geo.screenWidth / density
        val slotDp = geo.cameraSlotWidth / density
        val available = when (geo.anchor) {
            CameraAnchor.Center -> screenWidthDp - 16f
            CameraAnchor.Start -> screenWidthDp - (geo.centerX / density - slotDp / 2f) - 8f
            CameraAnchor.End -> geo.centerX / density + slotDp / 2f - 8f
        }.coerceAtLeast(slotDp * 3f)
        val scale = settings.getIslandExpandedScale().coerceIn(1f, 1.3f)
        val lineWidth = minOf(settings.getIslandMaxWidth(), available)
        val expandedWidth = minOf(settings.getIslandExpandedWidth(), available / scale)
        spec.value = IslandLayoutSpec(
            cameraDiameter = (geo.diameter / density).dp,
            cameraGap = (geo.gap / density).dp,
            surfaceTop = (geo.surfaceTop / density).dp,
            lineWidth = lineWidth.dp,
            expandedWidth = expandedWidth.dp,
            expandedCorner = settings.getIslandExpandedRoundness().dp,
            expandedPadding = settings.getIslandExpandedPadding().dp,
            expandedTopPadding = settings.getIslandExpandedTopPadding().dp,
            expandedScale = settings.getIslandExpandedScale().coerceIn(1f, 1.3f),
            fontScale = settings.getIslandFontScale().coerceIn(0.8f, 1.3f),
            expandedOutset = (expandedWidth * (scale - 1f) / 2f).dp,
            cameraAnchor = geo.anchor,
        )
        windowHost.maxWidthPx = (maxOf(lineWidth, expandedWidth * settings.getIslandExpandedScale().coerceIn(1f, 1.3f)) * density).toInt()
        windowHost.updateGeometry(geo)
        controller.lineStageEnabled = settings.isIslandLineStageEnabled()
        controller.relayout()
        controller.expandedTimeoutMs = settings.getIslandExpandedTimeoutMs()
    }

    private fun applySuppression() {
        controller.setSuppressed(isContentSuppressed)
    }

    private fun syncStatusBar(stage: IslandStage) {
        val enabled = settings.getBoolean(SettingsRepository.KEY_ISLAND_DYNAMIC_HIDE_STATUS_BAR, false)
        if (!enabled || !ShellUtils.hasPermission(service)) {
            IslandStatusBarHider.restore(service)
            return
        }
        IslandStatusBarHider.apply(service, stage == IslandStage.Line || stage == IslandStage.Expanded)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        key ?: return
        when (key) {
            SettingsRepository.KEY_ISLAND_ENABLED -> updateState()
            SettingsRepository.KEY_ISLAND_DYNAMIC_HIDE_STATUS_BAR -> syncStatusBar(controller.state.value.stage)
            SettingsRepository.KEY_ISLAND_HIDE_WHEN_SCREEN_OFF -> applySuppression()
            SettingsRepository.KEY_ISLAND_HIDE_IN_OWNER_APP -> applyOwnerAppHiding()
            SettingsRepository.KEY_ISLAND_SUPPRESS_SYSTEM_HEADS_UP ->
                if (running) settings.applyHeadsUpSuppression(settings.isIslandSuppressSystemHeadsUpEnabled())
            in CONFIG_KEYS -> if (running) applyConfig()
        }
        if (running) plugins.filter { key in it.settingKeys }.forEach { it.refresh() }
    }

    private companion object {
        val CONFIG_KEYS = setOf(
            SettingsRepository.KEY_ISLAND_USE_AUTO_DETECT,
            SettingsRepository.KEY_ISLAND_CAMERA_OFFSET_X,
            SettingsRepository.KEY_ISLAND_CAMERA_OFFSET_Y,
            SettingsRepository.KEY_ISLAND_CAMERA_SIZE,
            SettingsRepository.KEY_ISLAND_MAX_WIDTH,
            SettingsRepository.KEY_ISLAND_CUTOUT_GAP,
            SettingsRepository.KEY_ISLAND_EXPANDED_WIDTH,
            SettingsRepository.KEY_ISLAND_EXPANDED_ROUNDNESS,
            SettingsRepository.KEY_ISLAND_EXPANDED_PADDING,
            SettingsRepository.KEY_ISLAND_EXPANDED_TOP_PADDING,
            SettingsRepository.KEY_ISLAND_EXPANDED_TIMEOUT_MS,
            SettingsRepository.KEY_ISLAND_LINE_STAGE_ENABLED,
            SettingsRepository.KEY_ISLAND_EXPANDED_SCALE,
            SettingsRepository.KEY_ISLAND_FONT_SCALE,
            SettingsRepository.KEY_ISLAND_CAMERA_POSITION,
        )
    }
}

private val IslandTypography = Typography().run {
    copy(
        bodyLarge = bodyLarge.copy(fontFamily = IslandFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = IslandFontFamily),
        bodySmall = bodySmall.copy(fontFamily = IslandFontFamily),
        labelLarge = labelLarge.copy(fontFamily = IslandFontFamily),
        labelMedium = labelMedium.copy(fontFamily = IslandFontFamily),
        labelSmall = labelSmall.copy(fontFamily = IslandFontFamily),
        titleMedium = titleMedium.copy(fontFamily = IslandFontFamily),
        titleSmall = titleSmall.copy(fontFamily = IslandFontFamily),
    )
}
