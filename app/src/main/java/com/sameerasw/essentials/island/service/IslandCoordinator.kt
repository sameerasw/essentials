package com.sameerasw.essentials.island.service

import android.provider.Settings
import com.sameerasw.essentials.island.plugins.alarm.AlarmPlugin
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
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
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
import com.sameerasw.essentials.island.plugins.network.NetworkPlugin
import com.sameerasw.essentials.island.plugins.devices.DevicesPlugin
import com.sameerasw.essentials.island.plugins.brief.BriefPlugin
import com.sameerasw.essentials.island.plugins.progress.ProgressPlugin
import com.sameerasw.essentials.island.plugins.soundmode.SoundModePlugin
import com.sameerasw.essentials.island.plugins.weather.WeatherPlugin
import com.sameerasw.essentials.island.plugins.travel.TravelPlugin
import com.sameerasw.essentials.island.plugins.caffeinate.CaffeinatePlugin
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.ui.components.IslandIcon

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
        ProgressPlugin(),
        MediaPlugin(),
        CalendarPlugin(),
        ConsciousGatePlugin(),
        FlashlightPlugin(),
        TimerPlugin(),
        SoundModePlugin(),
        AlarmPlugin(),
        WeatherPlugin(),
        CaffeinatePlugin(),
        TravelPlugin(),
        NetworkPlugin(),
        DevicesPlugin(),
        BriefPlugin(),
    )

    private var scope: CoroutineScope? = null
    private var geometry: CameraGeometry? = null
    private val spec = MutableStateFlow(IslandLayoutSpec())
    private val showRing = MutableStateFlow(false)

    private var isScreenOff = false
    private var isLandscape = false
    private var isFullscreenApp = false
    private var isShadeExpanded = false
    private var running = false
    private var foregroundPackage: String? = null
    private var textInputActive = false

    private val isWindowSuppressed get() = isLandscape || isFullscreenApp
    private val isContentSuppressed: Boolean
        get() = isWindowSuppressed ||
            when (settings.getIslandShowWhen()) {
                SettingsRepository.ISLAND_SHOW_WHEN_ALWAYS -> false
                SettingsRepository.ISLAND_SHOW_WHEN_SCREEN_ON -> isScreenOff
                else -> isScreenOff || keyguardManager?.isKeyguardLocked == true
            } ||
            (settings.isIslandHideOnShadeEnabled() && isShadeExpanded)

    private val compactGestures = CompactGestureController(
        context = service,
        settings = settings,
        scope = { scope },
        openBrief = { openBrief() },
    )

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
            if (!running) return
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
        controller.fallbackTapKey = { BriefPlugin.ITEM_KEY.takeIf { settings.isIslandBriefEnabled() } }
        controller.onStageChanged = { stage ->
            if (stage != IslandStage.Expanded) windowHost.setTextInput(false)
            windowHost.onStageChanged(stage)
            syncStatusBar(stage)
            reportVisibility(stage != IslandStage.Hidden)
        }
        settings.setIslandPreviewRingEnabled(false)
        settings.setIslandPreviewStage(SettingsRepository.ISLAND_PREVIEW_STAGE_AUTO)
        updateState()
    }

    // Fires with true while the island has anything on screen
    var onVisibilityChanged: ((Boolean) -> Unit)? = null
    private var lastVisible = false

    private fun reportVisibility(visible: Boolean) {
        if (visible == lastVisible) return
        lastVisible = visible
        onVisibilityChanged?.invoke(visible)
    }

    fun openBrief() {
        mainHandler.post { controller.expand(BriefPlugin.ITEM_KEY) }
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

    private var pendingPackage: String? = null
    private val applyForegroundPackage = Runnable {
        val packageName = pendingPackage ?: return@Runnable
        if (isTransientPackage(packageName) || foregroundPackage == packageName) return@Runnable
        foregroundPackage = packageName
        applyOwnerAppHiding()
        onLauncher = packageName in launcherPackages
        applyLauncherOnly()
    }

    fun onForegroundPackage(packageName: String) {
        if (pendingPackage == packageName) return
        pendingPackage = packageName
        mainHandler.removeCallbacks(applyForegroundPackage)
        mainHandler.postDelayed(applyForegroundPackage, SETTLE_MS)
    }

    private var onLauncher = true

    private val launcherPackages: Set<String> by lazy {
        try {
            service.packageManager
                .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                .map { it.activityInfo.packageName }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun isTransientPackage(packageName: String): Boolean =
        packageName == "android" ||
            packageName == "com.android.systemui" ||
            packageName == service.packageName ||
            packageName == Settings.Secure.getString(service.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.substringBefore('/')

    private fun applyLauncherOnly() {
        val sources = LAUNCHER_ONLY_KEYS.filterValues { settings.getBoolean(it, false) }.keys
        controller.setLauncherState(onLauncher, sources)
    }

    private fun applyOwnerAppHiding() {
        controller.setHiddenPackage(foregroundPackage.takeIf { settings.isIslandHideInOwnerAppEnabled() })
    }

    fun updateConsciousGateState() {
        mainHandler.post { plugins.filterIsInstance<ConsciousGatePlugin>().forEach { it.refresh() } }
    }

    private var pendingFullscreen = false
    private val applyFullscreen = Runnable {
        if (isFullscreenApp == pendingFullscreen) return@Runnable
        isFullscreenApp = pendingFullscreen
        updateState()
    }

    fun setFullscreen(fullscreen: Boolean) {
        mainHandler.post {
            if (pendingFullscreen == fullscreen) return@post
            pendingFullscreen = fullscreen
            mainHandler.removeCallbacks(applyFullscreen)
            if (isFullscreenApp != fullscreen) mainHandler.postDelayed(applyFullscreen, SETTLE_MS)
        }
    }

    private var pendingShadeExpanded = false
    private val applyShadeExpanded = Runnable {
        if (isShadeExpanded == pendingShadeExpanded) return@Runnable
        isShadeExpanded = pendingShadeExpanded
        if (running) applySuppression()
    }

    fun setShadeExpanded(expanded: Boolean) {
        mainHandler.post {
            if (pendingShadeExpanded == expanded) return@post
            pendingShadeExpanded = expanded
            mainHandler.removeCallbacks(applyShadeExpanded)
            if (isShadeExpanded != expanded) mainHandler.postDelayed(applyShadeExpanded, SETTLE_MS)
        }
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
        mainHandler.removeCallbacks(applyForegroundPackage)
        mainHandler.removeCallbacks(applyFullscreen)
        mainHandler.removeCallbacks(applyShadeExpanded)
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
        applyLauncherOnly()
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
                    val ring by showRing.collectAsState()
                    IslandRoot(
                        state, layoutSpec, actions, windowHost::onTargetBoundsChanged,
                        registerCollapseAnimator = { controller.collapseAnimator = it },
                        showCameraRing = ring,
                    )
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
        newScope.launch {
            controller.state
                .map { it.stage to it.focusedKey }
                .distinctUntilChanged()
                .collect { (stage, key) -> plugins.forEach { it.onFocusChanged(stage, key) } }
        }
        if (settings.isIslandSuppressSystemHeadsUpEnabled()) settings.applyHeadsUpSuppression(true)
        applyPreviewStage()
    }

    private fun applyPreviewRing() {
        showRing.value = settings.isIslandPreviewRingEnabled()
    }

    private fun applyPreviewStage() {
        mainHandler.post {
            if (!running) return@post
            val stage = settings.getIslandPreviewStage()
            val locked = stage != SettingsRepository.ISLAND_PREVIEW_STAGE_AUTO
            controller.holdFocus = locked
            controller.setItems(PREVIEW_SOURCE, if (locked) listOf(previewItem()) else emptyList())
            controller.collapseImmediately()
            when (stage) {
                SettingsRepository.ISLAND_PREVIEW_STAGE_PEEK -> controller.peek(PREVIEW_KEY, 0L, force = true)
                SettingsRepository.ISLAND_PREVIEW_STAGE_EXPANDED -> {
                    val hasBrief = controller.state.value.items[BriefPlugin.ITEM_KEY]?.expanded != null
                    controller.expand(if (hasBrief) BriefPlugin.ITEM_KEY else PREVIEW_KEY)
                }
            }
        }
    }

    private fun previewItem() = IslandItem(
        key = PREVIEW_KEY,
        priority = IslandPriority.NOTIFICATION,
        placement = CompactPlacement.Dynamic,
        compact = listOf(CompactCell("preview.icon") { IslandIcon(R.drawable.rounded_notifications_unread_24, size = 18.dp) }),
        line = LineContent(
            icon = { IslandIcon(R.drawable.rounded_notifications_unread_24, size = 24.dp) },
            start = service.getString(R.string.island_preview_sample_title),
            end = service.getString(R.string.island_preview_sample_text),
        ),
        expanded = ExpandedContent { scope ->
            Column(
                Modifier
                    .padding(scope.spec.expandedOutset)
                    .padding(horizontal = 20.dp)
                    .padding(top = scope.spec.expandedTopPadding + scope.spec.compactHeight, bottom = scope.spec.expandedBottomPadding),
            ) {
                Text(stringResource(R.string.island_preview_sample_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.island_preview_sample_text), style = MaterialTheme.typography.bodyMedium)
            }
        },
        compactVisible = false,
    )

    private fun stop() {
        if (!running) return
        running = false
        controller.holdFocus = false
        controller.setItems(PREVIEW_SOURCE, emptyList())
        plugins.forEach {
            it.stop()
            controller.setItems(it.id, emptyList())
        }
        scope?.cancel()
        scope = null
        windowHost.detach()
        reportVisibility(false)
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
            verticalGap = (geo.verticalGap / density).dp,
            surfaceTop = (geo.surfaceTop / density).dp,
            lineWidth = lineWidth.dp,
            expandedWidth = expandedWidth.dp,
            expandedCorner = settings.getIslandExpandedRoundness().dp,
            expandedPadding = settings.getIslandExpandedPadding().dp,
            expandedTopPadding = settings.getIslandExpandedTopPadding().dp,
            expandedBottomPadding = settings.getIslandExpandedBottomPadding().dp,
            expandedScale = settings.getIslandExpandedScale().coerceIn(1f, 1.3f),
            fontScale = settings.getIslandFontScale().coerceIn(0.8f, 1.3f),
            expandedOutset = (expandedWidth * (scale - 1f) / 2f).dp,
            cameraAnchor = geo.anchor,
            outlineColor = if (settings.isIslandBorderOutlineEnabled()) {
                runCatching { Color(AndroidColor.parseColor(settings.getIslandBorderOutlineColor())) }
                    .getOrElse { Color(AndroidColor.parseColor(SettingsRepository.ISLAND_BORDER_OUTLINE_DEFAULT_COLOR)) }
            } else {
                null
            },
            outlineThickness = settings.getIslandBorderOutlineThickness().coerceIn(1f, 5f).dp,
            outlineHiddenWhenExpanded = settings.isIslandBorderOutlineHiddenWhenExpanded(),
            pulseShadow = settings.isIslandPulseShadowEnabled(),
            pulseSize = settings.getIslandPulseShadowSize().coerceIn(0.2f, 1f),
            pulseYShift = settings.getIslandPulseShadowYShift().coerceIn(0f, 1f),
            pulseSpread = settings.getIslandPulseShadowSpread().coerceIn(1f, 4f),
            pulseDurationMs = settings.getIslandPulseShadowDurationMs().coerceIn(300f, 4000f).toInt(),
        )
        windowHost.maxWidthPx = (maxOf(lineWidth, expandedWidth * settings.getIslandExpandedScale().coerceIn(1f, 1.3f)) * density).toInt()
        windowHost.updateGeometry(geo)
        controller.lineStageEnabled = settings.isIslandLineStageEnabled()
        controller.relayout()
        controller.expandedTimeoutMs = settings.getIslandExpandedTimeoutMs()
        applyPreviewRing()
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
            SettingsRepository.KEY_ISLAND_HIDE_WHEN_SCREEN_OFF, SettingsRepository.KEY_ISLAND_SHOW_WHEN -> applySuppression()
            SettingsRepository.KEY_ISLAND_HIDE_IN_OWNER_APP -> applyOwnerAppHiding()
            in LAUNCHER_ONLY_KEYS.values -> applyLauncherOnly()
            SettingsRepository.KEY_ISLAND_HIDE_ON_SHADE -> applySuppression()
            SettingsRepository.KEY_ISLAND_PREVIEW_RING -> if (running) applyPreviewRing()
            SettingsRepository.KEY_ISLAND_PREVIEW_STAGE -> applyPreviewStage()
            SettingsRepository.KEY_ISLAND_SUPPRESS_SYSTEM_HEADS_UP ->
                if (running) settings.applyHeadsUpSuppression(settings.isIslandSuppressSystemHeadsUpEnabled())
            in CONFIG_KEYS -> if (running) applyConfig()
        }
        if (running) plugins.filter { key in it.settingKeys }.forEach { it.refresh() }
    }

    private companion object {
        const val SETTLE_MS = 300L
        const val PREVIEW_SOURCE = "preview"
        const val PREVIEW_KEY = "preview.sample"
        val LAUNCHER_ONLY_KEYS = mapOf(
            "time_battery" to SettingsRepository.KEY_ISLAND_TIME_BATTERY_LAUNCHER_ONLY,
            "weather" to SettingsRepository.KEY_ISLAND_WEATHER_LAUNCHER_ONLY,
            "calendar" to SettingsRepository.KEY_ISLAND_CALENDAR_LAUNCHER_ONLY,
            "alarm" to SettingsRepository.KEY_ISLAND_ALARM_LAUNCHER_ONLY,
        )
        val CONFIG_KEYS = setOf(
            SettingsRepository.KEY_ISLAND_USE_AUTO_DETECT,
            SettingsRepository.KEY_ISLAND_CAMERA_OFFSET_X,
            SettingsRepository.KEY_ISLAND_CAMERA_OFFSET_Y,
            SettingsRepository.KEY_ISLAND_CAMERA_SIZE,
            SettingsRepository.KEY_ISLAND_MAX_WIDTH,
            SettingsRepository.KEY_ISLAND_CUTOUT_GAP,
            SettingsRepository.KEY_ISLAND_EXPANDED_WIDTH,
            SettingsRepository.KEY_ISLAND_BORDER_OUTLINE_ENABLED,
            SettingsRepository.KEY_ISLAND_BORDER_OUTLINE_COLOR,
            SettingsRepository.KEY_ISLAND_BORDER_OUTLINE_THICKNESS,
            SettingsRepository.KEY_ISLAND_BORDER_OUTLINE_HIDE_EXPANDED,
            SettingsRepository.KEY_ISLAND_PULSE_SHADOW_ON_NOTIFICATION,
            SettingsRepository.KEY_ISLAND_PULSE_SHADOW_SIZE,
            SettingsRepository.KEY_ISLAND_PULSE_SHADOW_Y_SHIFT,
            SettingsRepository.KEY_ISLAND_PULSE_SHADOW_SPREAD,
            SettingsRepository.KEY_ISLAND_PULSE_SHADOW_DURATION_MS,
            SettingsRepository.KEY_ISLAND_EXPANDED_ROUNDNESS,
            SettingsRepository.KEY_ISLAND_EXPANDED_PADDING,
            SettingsRepository.KEY_ISLAND_EXPANDED_TOP_PADDING,
            SettingsRepository.KEY_ISLAND_EXPANDED_BOTTOM_PADDING,
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

