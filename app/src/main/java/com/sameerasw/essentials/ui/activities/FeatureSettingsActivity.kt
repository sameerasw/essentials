/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: FeatureSettingsActivity.kt
 * Description: Activity component for FeatureSettingsActivity.kt.
 */

package com.sameerasw.essentials

import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.components.animations.LottieFeatureAnimation
import com.sameerasw.essentials.ui.components.linkActions.LinkPickerScreen
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.features.battery.BatteriesSettingsUI
import com.sameerasw.essentials.ui.features.security.AppLockSettingsUI
import com.sameerasw.essentials.ui.features.system.AlwaysOnDisplaySettingsUI
import com.sameerasw.essentials.ui.features.system.BatteryNotificationSettingsUI
import com.sameerasw.essentials.ui.features.system.ButtonRemapSettingsUI
import com.sameerasw.essentials.ui.features.system.CaffeinateSettingsUI
import com.sameerasw.essentials.ui.features.system.CalendarSyncSettingsUI
import com.sameerasw.essentials.ui.features.system.DynamicNightLightSettingsUI
import com.sameerasw.essentials.ui.features.system.EssentialsOnDisplaySettingsUI
import com.sameerasw.essentials.ui.features.system.FlashlightPulseSettingsUI
import com.sameerasw.essentials.ui.features.system.FlashlightSettingsUI
import com.sameerasw.essentials.ui.features.system.FreezeSettingsUI
import com.sameerasw.essentials.ui.features.system.KeyboardSettingsUI
import com.sameerasw.essentials.ui.features.system.LiveWallpaperSettingsUI
import com.sameerasw.essentials.ui.features.system.LocationReachedSettingsUI
import com.sameerasw.essentials.ui.features.system.LockScreenClockSettingsUI
import com.sameerasw.essentials.ui.features.system.MapsPowerSavingSettingsUI
import com.sameerasw.essentials.ui.features.system.NavigationSettingsUI
import com.sameerasw.essentials.ui.features.system.NetworksSettingsUI
import com.sameerasw.essentials.ui.features.system.NotificationLightingSettingsUI
import com.sameerasw.essentials.ui.features.system.NotificationSnoozingSettingsUI
import com.sameerasw.essentials.ui.features.system.OtherCustomizationsSettingsUI
import com.sameerasw.essentials.ui.features.system.PerAppRefreshRateSettingsUI
import com.sameerasw.essentials.ui.features.system.PocketModeSettingsUI
import com.sameerasw.essentials.ui.features.system.PowerAndBatterySettingsUI
import com.sameerasw.essentials.ui.features.system.QuickSettingsTilesSettingsUI
import com.sameerasw.essentials.ui.features.system.RefreshRateSettingsUI
import com.sameerasw.essentials.ui.features.system.RemoteLockSettingsUI
import com.sameerasw.essentials.ui.features.system.ScreenLockedSecuritySettingsUI
import com.sameerasw.essentials.ui.features.system.ScreenOffWidgetSettingsUI
import com.sameerasw.essentials.ui.features.system.ShutUpSettingsUI
import com.sameerasw.essentials.ui.features.system.SnoozeNotificationsSettingsUI
import com.sameerasw.essentials.ui.features.system.SoundModeTileSettingsUI
import com.sameerasw.essentials.ui.features.system.StandbyAppsSettingsUI
import com.sameerasw.essentials.ui.features.system.StatusBarIconSettingsUI
import com.sameerasw.essentials.ui.features.system.TextAnimationsSettingsUI
import com.sameerasw.essentials.ui.features.system.WatchControlsSettingsUI
import com.sameerasw.essentials.ui.features.watch.WatchNotificationSettingsUI
import com.sameerasw.essentials.ui.features.watch.WatchSettingsUI
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.BiometricSecurityHelper
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.CaffeinateViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.viewmodels.StatusBarIconViewModel
import com.sameerasw.essentials.viewmodels.WatchViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
class FeatureSettingsActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val isDarkMode =
            (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawableResource(if (isDarkMode) android.R.color.black else R.color.app_window_background)
        val featureId = intent.getStringExtra("feature") ?: ""
        val featureObj = FeatureRegistry.ALL_FEATURES.find { it.id == featureId }
        val highlightSetting = intent.getStringExtra("highlight_setting")

        if (featureId == "Link actions") {
            setContent {
                val viewModel: MainViewModel = viewModel()
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    viewModel.check(context)
                }
                val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
                EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                    LinkPickerScreen(
                        uri = "https://sameerasw.com".toUri(),
                        onFinish = { finish() },
                        modifier = Modifier.fillMaxSize(),
                        demo = true
                    )
                }
            }
            return
        }

        setContent {
            val context = LocalContext.current
            val viewModel: MainViewModel = viewModel()
            val statusBarViewModel: StatusBarIconViewModel = viewModel()
            val caffeinateViewModel: CaffeinateViewModel = viewModel()
            val watchViewModel: WatchViewModel = viewModel()

            // Automatic refresh on resume
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.check(context)
                        if (featureId == "Statusbar icons") {
                            statusBarViewModel.check(context)
                        }
                        if (featureId == "Caffeinate") {
                            caffeinateViewModel.check(context)
                        }
                        if (featureId == "Watch") {
                            watchViewModel.check(context)
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Initialize synchronously so settingsRepository is ready before first composition
            remember(context) { viewModel.check(context) }

            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            val isBlurEnabled by viewModel.isBlurEnabled
            val pinnedFeatureKeys by viewModel.pinnedFeatureKeys

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.sameerasw.essentials.ui.state.LocalMenuStateManager provides remember { com.sameerasw.essentials.ui.state.MenuStateManager() }
                ) {
                    LocalView.current
                    val prefs = context.getSharedPreferences("essentials_prefs", MODE_PRIVATE)

                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
                    }

                    var selectedHaptic by remember {
                        val name =
                            prefs.getString("haptic_feedback_type", HapticFeedbackType.NONE.name)
                        mutableStateOf(
                            try {
                                HapticFeedbackType.valueOf(name ?: HapticFeedbackType.NONE.name)
                            } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
                                HapticFeedbackType.NONE
                            }
                        )
                    }

                    // Permission sheet state
                    var showPermissionSheet by remember { mutableStateOf(false) }
                    var childFeatureForPermissions by remember { mutableStateOf<String?>(null) }

                    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
                    val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
                    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted
                    val isNotificationLightingAccessibilityEnabled by viewModel.isNotificationLightingAccessibilityEnabled
                    val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled
                    val isReadPhoneStateEnabled by viewModel.isReadPhoneStateEnabled
                    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
                    val isWriteSettingsEnabled by viewModel.isWriteSettingsEnabled
                    val isUsageStatsPermissionGranted by viewModel.isUsageStatsPermissionGranted
                    val isPostNotificationsEnabled by viewModel.isPostNotificationsEnabled

                    var watchAdbWifiEnabled by remember {
                        mutableStateOf(prefs.getBoolean("watch_adb_wifi_enabled", false))
                    }
                    var watchSyncSoundModeEnabled by remember {
                        mutableStateOf(prefs.getBoolean("watch_sync_sound_mode_enabled", false))
                    }
                    var watchSyncLocationReachedEnabled by remember {
                        mutableStateOf(
                            prefs.getBoolean(
                                "watch_sync_location_reached_enabled",
                                true
                            )
                        )
                    }
                    androidx.compose.runtime.DisposableEffect(prefs) {
                        val listener =
                            android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                                if (key == "watch_adb_wifi_enabled") {
                                    watchAdbWifiEnabled = p.getBoolean(key, false)
                                } else if (key == "watch_sync_sound_mode_enabled") {
                                    watchSyncSoundModeEnabled = p.getBoolean(key, false)
                                } else if (key == "watch_sync_location_reached_enabled") {
                                    watchSyncLocationReachedEnabled = p.getBoolean(key, true)
                                }
                            }
                        prefs.registerOnSharedPreferenceChangeListener(listener)
                        onDispose {
                            prefs.unregisterOnSharedPreferenceChangeListener(listener)
                        }
                    }

                    // FAB State for Notification Lighting
                    var fabExpanded by remember { mutableStateOf(true) }
                    LaunchedEffect(featureId) {
                        if (featureId == "Notification lighting") {
                            fabExpanded = true
                            delay(3000)
                            fabExpanded = false
                        }
                        if (featureId == "Watch") {
                            val messageClient =
                                com.google.android.gms.wearable.Wearable.getMessageClient(context)
                            val nodeClient =
                                com.google.android.gms.wearable.Wearable.getNodeClient(context)
                            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                                for (node in nodes) {
                                    messageClient.sendMessage(
                                        node.id,
                                        "/request_watch_status",
                                        byteArrayOf()
                                    )
                                }
                            }
                        }
                    }

                    // Help Sheet State
                    var showHelpSheet by remember { mutableStateOf(false) }
                    var showInstructionsSheet by remember { mutableStateOf(false) }
                    var showWatchInstallHelpSheet by remember { mutableStateOf(false) }
                    var selectedHelpFeature by remember {
                        mutableStateOf<com.sameerasw.essentials.domain.model.Feature?>(
                            null
                        )
                    }


                    // Show permission sheet if feature has missing permissions
                    LaunchedEffect(
                        featureId,
                        isAccessibilityEnabled,
                        isWriteSecureSettingsEnabled,
                        isOverlayPermissionGranted,
                        isNotificationLightingAccessibilityEnabled,
                        isNotificationListenerEnabled,
                        isReadPhoneStateEnabled,
                        isShizukuPermissionGranted,
                        isWriteSettingsEnabled,
                        isUsageStatsPermissionGranted,
                        isPostNotificationsEnabled
                    ) {
                        val hasMissingPermissions = when (featureId) {
                            "Screen off widget" -> !isAccessibilityEnabled
                            "Statusbar icons" -> !isWriteSecureSettingsEnabled
                            "Notification lighting" -> !isOverlayPermissionGranted || !isNotificationLightingAccessibilityEnabled || !isNotificationListenerEnabled
                            "Button remap" -> !isAccessibilityEnabled
                            "Pocket mode" -> !isAccessibilityEnabled
                            "Dynamic night light" -> (if (viewModel.isUseUsageAccess.value) !viewModel.isUsageStatsPermissionGranted.value else !isAccessibilityEnabled) || !isWriteSecureSettingsEnabled
                            "Snooze system notifications" -> !isNotificationListenerEnabled
                            "Screen locked security" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled || !viewModel.isDeviceAdminEnabled.value
                            "App lock" -> !isAccessibilityEnabled || (if (viewModel.isUseUsageAccess.value) !viewModel.isUsageStatsPermissionGranted.value else false)
                            "Freeze" -> !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                context
                            )

                            "Location reached" -> !viewModel.isLocationPermissionGranted.value || !viewModel.isBackgroundLocationPermissionGranted.value
                            "Quick settings tiles" -> !viewModel.isWriteSettingsEnabled.value
                            "Screen refresh rate" -> !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                context
                            )
                            "Shut-Up!" -> !isWriteSecureSettingsEnabled || !isWriteSettingsEnabled || !isUsageStatsPermissionGranted || !isPostNotificationsEnabled
                            "Per app refresh rate" -> (if (viewModel.isUseUsageAccess.value) !viewModel.isUsageStatsPermissionGranted.value else !isAccessibilityEnabled) || !isShizukuPermissionGranted
                            // Top level checks for other features (rarely hit if they are children, but safe to add)
                            "Essentials On Display" -> !isAccessibilityEnabled || !isNotificationListenerEnabled
                            "Call vibrations" -> !isReadPhoneStateEnabled || !isNotificationListenerEnabled
                            "Maps power saving mode" -> !isNotificationListenerEnabled || !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                context
                            )

                            "Caffeinate" -> !viewModel.isPostNotificationsEnabled.value
                            "Battery notification" -> !viewModel.isPostNotificationsEnabled.value || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !viewModel.isBluetoothPermissionGranted.value)
                            "Text and animations" -> !viewModel.isWriteSettingsEnabled.value || !isWriteSecureSettingsEnabled
                            "Always on Display" -> !isWriteSecureSettingsEnabled
                            "Lock screen clock" -> !isWriteSecureSettingsEnabled
                            "Other customizations" -> !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                context
                            )

                            "Power and Battery" -> !isWriteSecureSettingsEnabled
                            "Networks" -> !isWriteSecureSettingsEnabled && !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                context
                            )

                            else -> false
                        }
                        if (hasMissingPermissions) {
                            showPermissionSheet = true
                        }
                    }


                    if (showPermissionSheet) {
                        val featureIdForPermissions = childFeatureForPermissions ?: featureId
                        val featureObjForPermissions =
                            FeatureRegistry.ALL_FEATURES.find { it.id == featureIdForPermissions }

                        val permissionItems = if (featureObjForPermissions != null) {
                            com.sameerasw.essentials.utils.PermissionUIHelper.getPermissionItems(
                                featureObjForPermissions.permissionKeys,
                                context,
                                viewModel,
                                this@FeatureSettingsActivity
                            )
                        } else {
                            emptyList()
                        }

                        if (permissionItems.isNotEmpty()) {
                            PermissionsBottomSheet(
                                onDismissRequest = {
                                    showPermissionSheet = false
                                    childFeatureForPermissions = null
                                },
                                featureTitle = if (featureObjForPermissions != null && childFeatureForPermissions == null) stringResource(
                                    featureObjForPermissions.title
                                ) else featureIdForPermissions,
                                permissions = permissionItems
                            )
                        }
                    }

                    if (showHelpSheet && selectedHelpFeature != null) {
                        com.sameerasw.essentials.ui.core.sheets.FeatureHelpBottomSheet(
                            onDismissRequest = {
                                showHelpSheet = false
                                selectedHelpFeature = null
                            },
                            feature = selectedHelpFeature!!
                        )
                    }

                    if (showInstructionsSheet) {
                        com.sameerasw.essentials.ui.core.sheets.InstructionsBottomSheet(
                            onDismissRequest = { showInstructionsSheet = false }
                        )
                    }

                    if (showWatchInstallHelpSheet) {
                        com.sameerasw.essentials.ui.features.watch.sheets.WatchInstallHelpBottomSheet(
                            onDismissRequest = { showWatchInstallHelpSheet = false }
                        )
                    }

                    val pageTitle =
                        if (featureObj != null) stringResource(featureObj.title) else featureId
                    val hasMenu = featureObj != null && featureObj.aboutDescription != null
                    val view = LocalView.current

                    val density = LocalDensity.current
                    val minHeaderHeight = 200.dp
                    val maxHeaderHeight = 400.dp
                    var headerHeight by remember { mutableStateOf(minHeaderHeight) }

                    val nestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                val delta = available.y
                                if (delta < 0 && headerHeight > minHeaderHeight) {
                                    val oldHeight = headerHeight
                                    headerHeight = with(density) {
                                        (oldHeight.toPx() + delta).toDp()
                                    }.coerceAtLeast(minHeaderHeight)
                                    val consumed = oldHeight - headerHeight
                                    return Offset(0f, with(density) { -consumed.toPx() })
                                }
                                return Offset.Zero
                            }

                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                val delta = available.y
                                if (delta > 0) {
                                    val oldHeight = headerHeight
                                    headerHeight = with(density) {
                                        (oldHeight.toPx() + delta).toDp()
                                    }.coerceAtMost(maxHeaderHeight)

                                    if (headerHeight == maxHeaderHeight && oldHeight < maxHeaderHeight) {
                                        HapticUtil.performLightHaptic(view)
                                    }

                                    val produced = headerHeight - oldHeight
                                    return Offset(0f, with(density) { produced.toPx() })
                                }
                                return Offset.Zero
                            }
                        }
                    }

                    val statusBarHeightPx = with(LocalDensity.current) {
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
                    }
                    val statusBarHeight =
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .progressiveBlur(
                                blurRadius = if (isBlurEnabled) 40f else 0f,
                                height = statusBarHeightPx * 1.15f,
                                direction = BlurDirection.TOP
                            )
                    ) {
                        val hasScroll =
                            featureId != "Sound mode tile" && featureId != "Quick settings tiles" && featureId != "Location reached" && featureId != "Watch Controls"
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .progressiveBlur(
                                    blurRadius = if (isBlurEnabled) 40f else 0f,
                                    height = with(LocalDensity.current) { 150.dp.toPx() },
                                    direction = BlurDirection.BOTTOM
                                )
                                .then(
                                    if (hasScroll) Modifier
                                        .nestedScroll(nestedScrollConnection)
                                        .verticalScroll(rememberScrollState()) else Modifier
                                )
                        ) {
                            // Top padding for status bar
                            if (featureId != "Quick settings tiles" && featureId != "Location reached") {
                                androidx.compose.foundation.layout.Spacer(
                                    modifier = Modifier.height(
                                        statusBarHeight
                                    )
                                )
                            }

                            if (featureObj != null && featureObj.animationRes != 0) {
                                LottieFeatureAnimation(
                                    resId = featureObj.animationRes,
                                    height = headerHeight,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }

                            if (featureId == "Watch") {
                                val context = LocalContext.current
                                LaunchedEffect(Unit) {
                                    watchViewModel.check(context)
                                }
                                WatchSettingsUI(
                                    viewModel = watchViewModel,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            val children = FeatureRegistry.getFilteredFeatures(
                                context,
                                viewModel.isEnableUnsupportedFeatures.value
                            ).filter { it.parentFeatureId == featureId }
                            if (children.isNotEmpty() && featureId != "Networks") {
                                val sectionChildLists = run {
                                    val childMap = children.associateBy { it.id }
                                    val definedSections = when (featureId) {
                                        "Display" -> listOf(
                                            listOf(
                                                "Essentials On Display",
                                                "Always on Display",
                                                "Statusbar icons",
                                                "Lock screen clock"
                                            ),
                                            listOf(
                                                "Text and animations",
                                                "Screen refresh rate",
                                                "Per app refresh rate",
                                                "Navigation"
                                            ),
                                            listOf(
                                                "Caffeinate",
                                                "Dynamic night light",
                                                "Smart pixels"
                                            ),
                                            listOf(
                                                "Other customizations"
                                            )
                                        )

                                        "Notifications" -> listOf(
                                            listOf(
                                                "Notification lighting",
                                                "Flashlight pulse"
                                            ),
                                            listOf(
                                                "Notification snoozing",
                                                "Snooze system notifications"
                                            )
                                        )

                                        "Widgets" -> listOf(
                                            listOf(
                                                "Pixel Searchbar"
                                            ),
                                            listOf(
                                                "Screen off widget",
                                                "Batteries"
                                            )
                                        )

                                        "Input" -> listOf(
                                            listOf(
                                                "Button remap",
                                                "Flashlight"
                                            ),
                                            listOf(
                                                "Link actions",
                                                "System Keyboard"
                                            )
                                        )

                                        "Power and battery" -> listOf(
                                            listOf(
                                                "Power and Battery",
                                                "Standby apps"
                                            ),
                                            listOf(
                                                "Battery notification"
                                            )
                                        )

                                        "Watch" -> listOf(
                                            listOf(
                                                "Notification Sync",
                                                "Call Sync",
                                                "Watch Controls",
                                                "Lock from Watch"
                                            ),
                                            listOf(
                                                "Calendar Sync",
                                                "Sync sound mode",
                                                "Sync location reached status"
                                            ),
                                            listOf(
                                                "Watch Wireless Debugging"
                                            )
                                        )

                                        "Security" -> listOf(
                                            listOf(
                                                "Screen locked security",
                                                "App lock",
                                                "Shut-Up!"
                                            ),
                                            listOf(
                                                "Lockdown mode"
                                            )
                                        )

                                        else -> null
                                    }

                                    if (definedSections != null) {
                                        val assignedIds = definedSections.flatten().toSet()
                                        val unassigned = children.filter { it.id !in assignedIds }
                                        definedSections.map { ids -> ids.mapNotNull { childMap[it] } }
                                            .filter { it.isNotEmpty() } + if (unassigned.isNotEmpty()) listOf(
                                            unassigned
                                        ) else emptyList()
                                    } else {
                                        listOf(children)
                                    }
                                }

                                sectionChildLists.forEach { sectionChildren ->
                                    RoundedCardContainer(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .padding(top = 16.dp)
                                    ) {
                                        sectionChildren.forEach { child ->
                                            val permissionAwareToggle: (Boolean) -> Unit =
                                                { enabled ->
                                                    val missingPermission = when (child.id) {
                                                        "Screen off widget" -> !isAccessibilityEnabled
                                                        "Statusbar icons" -> !isWriteSecureSettingsEnabled
                                                        "Notification lighting" -> !isOverlayPermissionGranted || !isNotificationLightingAccessibilityEnabled || !isNotificationListenerEnabled
                                                        "Button remap" -> !isAccessibilityEnabled
                                                        "Dynamic night light" -> (if (viewModel.isUseUsageAccess.value) !viewModel.isUsageStatsPermissionGranted.value else !isAccessibilityEnabled) || !isWriteSecureSettingsEnabled
                                                        "Snooze system notifications" -> !isNotificationListenerEnabled
                                                        "Screen locked security" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled || !viewModel.isDeviceAdminEnabled.value
                                                        "App lock" -> !isAccessibilityEnabled || (if (viewModel.isUseUsageAccess.value) !viewModel.isUsageStatsPermissionGranted.value else false)
                                                        "Freeze" -> !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                                            context
                                                        )

                                                        "Essentials On Display" -> !isAccessibilityEnabled || !isNotificationListenerEnabled
                                                        "Call vibrations" -> !isReadPhoneStateEnabled || !isNotificationListenerEnabled
                                                        "Calendar Sync" -> androidx.core.content.ContextCompat.checkSelfPermission(
                                                            context,
                                                            android.Manifest.permission.READ_CALENDAR
                                                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED

                                                        "Batteries" -> (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && androidx.core.content.ContextCompat.checkSelfPermission(
                                                            context,
                                                            android.Manifest.permission.BLUETOOTH_CONNECT
                                                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED)

                                                        "Maps power saving mode" -> !isNotificationListenerEnabled || !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                                            context
                                                        )

                                                        "Caffeinate" -> !viewModel.isPostNotificationsEnabled.value
                                                        "Battery notification" -> !viewModel.isPostNotificationsEnabled.value || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !viewModel.isBluetoothPermissionGranted.value)
                                                        "Text and animations" -> !viewModel.isWriteSettingsEnabled.value || !isWriteSecureSettingsEnabled
                                                        "Lock screen clock" -> !isWriteSecureSettingsEnabled
                                                        "Screen refresh rate" -> !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                                            context
                                                        )

                                                        "Shut-Up!" -> !isWriteSecureSettingsEnabled || !viewModel.isWriteSettingsEnabled.value || !viewModel.isUsageStatsPermissionGranted.value || !viewModel.isPostNotificationsEnabled.value
                                                        "Per app refresh rate" -> (if (viewModel.isUseUsageAccess.value) !viewModel.isUsageStatsPermissionGranted.value else !isAccessibilityEnabled) || !viewModel.isShizukuPermissionGranted.value
                                                        "Power and Battery" -> !isWriteSecureSettingsEnabled
                                                        "Networks" -> !isWriteSecureSettingsEnabled && !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                                            context
                                                        )

                                                        "Call Sync" -> !com.sameerasw.essentials.utils.PermissionUtils.hasCallPermissions(context)
                                                        "Notification Sync" -> !viewModel.isNotificationListenerEnabled.value

                                                        "Disable safe volume warning" -> !isWriteSecureSettingsEnabled
                                                        "Notification snoozing" -> !isWriteSecureSettingsEnabled
                                                        else -> false
                                                    }

                                                    if (missingPermission) {
                                                        childFeatureForPermissions = child.id
                                                        showPermissionSheet = true
                                                    } else {
                                                        BiometricSecurityHelper.runWithAuth(
                                                            activity = this@FeatureSettingsActivity,
                                                            feature = child,
                                                            isToggle = true,
                                                            action = {
                                                                child.onToggle(
                                                                    viewModel,
                                                                    context,
                                                                    enabled
                                                                )
                                                            }
                                                        )
                                                    }
                                                }

                                            FeatureCard(
                                                modifier = Modifier.highlight(highlightSetting == child.id),
                                                title = child.title,
                                                description = child.description,
                                                iconRes = child.iconRes,
                                                isEnabled = when (child.id) {
                                                    "Watch Wireless Debugging" -> watchAdbWifiEnabled
                                                    "Sync sound mode" -> watchSyncSoundModeEnabled
                                                    "Sync location reached status" -> watchSyncLocationReachedEnabled
                                                    else -> child.isEnabled(viewModel)
                                                },
                                                isToggleEnabled = child.isToggleEnabled(
                                                    viewModel,
                                                    context
                                                ),
                                                showToggle = child.showToggle,
                                                onDisabledToggleClick = { permissionAwareToggle(true) },
                                                hasMoreSettings = child.hasMoreSettings,
                                                isBeta = child.isBeta,
                                                onToggle = permissionAwareToggle,
                                                onClick = {
                                                    if (child.hasMoreSettings) {
                                                        BiometricSecurityHelper.runWithAuth(
                                                            activity = this@FeatureSettingsActivity,
                                                            feature = child,
                                                            action = {
                                                                child.onClick(context, viewModel)
                                                            }
                                                        )
                                                    }
                                                },
                                                isPinned = pinnedFeatureKeys.contains(child.id),
                                                onPinToggle = { viewModel.togglePinFeature(child.id) },
                                                onHelpClick = if (child.aboutDescription != null) {
                                                    {
                                                        selectedHelpFeature = child
                                                        showHelpSheet = true
                                                    }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            } else {
                                when (featureId) {
                                    "Screen off widget" -> {
                                        ScreenOffWidgetSettingsUI(
                                            viewModel = viewModel,
                                            selectedHaptic = selectedHaptic,
                                            onHapticSelected = { type -> selectedHaptic = type },
                                            vibrator = vibrator,
                                            prefs = prefs,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting,
                                            onShowPermissionSheet = { showPermissionSheet = it },
                                            onSetChildFeatureForPermissions = {
                                                childFeatureForPermissions = it
                                            }
                                        )
                                    }

                                    "Statusbar icons" -> {
                                        StatusBarIconSettingsUI(
                                            viewModel = statusBarViewModel,
                                            mainViewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Caffeinate" -> {
                                        CaffeinateSettingsUI(
                                            viewModel = caffeinateViewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Notification lighting" -> {
                                        NotificationLightingSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Sound mode tile" -> {
                                        SoundModeTileSettingsUI(
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Button remap" -> {
                                        ButtonRemapSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Flashlight" -> {
                                        FlashlightSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Dynamic night light" -> {
                                        DynamicNightLightSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Smart pixels" -> {
                                        com.sameerasw.essentials.ui.features.system.SmartPixelsSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Snooze system notifications" -> {
                                        SnoozeNotificationsSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Notification snoozing" -> {
                                        NotificationSnoozingSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Screen locked security" -> {
                                        ScreenLockedSecuritySettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Pocket mode" -> {
                                        PocketModeSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "App lock" -> {
                                        AppLockSettingsUI(
                                            viewModel = viewModel,
                                            highlightKey = highlightSetting
                                        )
                                    }

                                    "Freeze" -> {
                                        FreezeSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightKey = highlightSetting
                                        )
                                    }

                                    "Quick settings tiles" -> {
                                        QuickSettingsTilesSettingsUI(
                                            modifier = Modifier.fillMaxSize(),
                                            highlightSetting = highlightSetting,
                                            contentPadding = PaddingValues(
                                                top = statusBarHeight,
                                                bottom = 150.dp
                                            )
                                        )
                                    }

                                    "Location reached" -> {
                                        LocationReachedSettingsUI(
                                            mainViewModel = viewModel,
                                            modifier = Modifier.fillMaxSize(),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "System Keyboard" -> {
                                        KeyboardSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Batteries" -> {
                                        BatteriesSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp)
                                        )
                                    }

                                    "Battery notification" -> {
                                        BatteryNotificationSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightKey = highlightSetting
                                        )
                                    }

                                    "Essentials On Display" -> {
                                        EssentialsOnDisplaySettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Calendar Sync" -> {
                                        CalendarSyncSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightKey = highlightSetting
                                        )
                                    }

                                    "Notification Sync" -> {
                                        WatchNotificationSettingsUI(
                                            modifier = Modifier.padding(top = 16.dp)
                                        )
                                    }

                                    "Watch Controls" -> {
                                        WatchControlsSettingsUI(
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Lock from Watch" -> {
                                        RemoteLockSettingsUI(
                                            mainViewModel = viewModel,
                                            watchViewModel = watchViewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Maps power saving mode" -> {
                                        MapsPowerSavingSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Flashlight pulse" -> {
                                        FlashlightPulseSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Text and animations" -> {
                                        TextAnimationsSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Screen refresh rate" -> {
                                        RefreshRateSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }
                                    "Shut-Up!" -> {
                                        ShutUpSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }
                                    "Per app refresh rate" -> {
                                        PerAppRefreshRateSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }
                                    "Always on Display" -> {
                                        AlwaysOnDisplaySettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "LiveWallpaper" -> {
                                        LiveWallpaperSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Navigation" -> {
                                        NavigationSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Other customizations" -> {
                                        OtherCustomizationsSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Lock screen clock" -> {
                                        LockScreenClockSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }
                                    "Power and Battery" -> {
                                        PowerAndBatterySettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Networks" -> {
                                        NetworksSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Standby apps" -> {
                                        StandbyAppsSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp)
                                        )
                                    }
                                }

                            }
                            // Bottom padding for toolbar
                            if (featureId != "Quick settings tiles" && featureId != "Location reached") {
                                androidx.compose.foundation.layout.Spacer(
                                    modifier = Modifier.height(
                                        150.dp
                                    )
                                )
                            }
                        }

                        EssentialsFloatingToolbar(
                            title = pageTitle,
                            isBeta = featureObj?.isBeta ?: false,
                            onBackClick = { finish() },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(1f),
                            onHelpClick = {
                                if (featureId == "Watch") {
                                    showWatchInstallHelpSheet = true
                                } else if (hasMenu) {
                                    selectedHelpFeature = featureObj
                                    showHelpSheet = true
                                } else {
                                    showInstructionsSheet = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
