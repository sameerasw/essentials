package com.sameerasw.essentials

import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.linkActions.LinkPickerScreen
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.composables.configs.AlwaysOnDisplaySettingsUI
import com.sameerasw.essentials.ui.composables.configs.AmbientMusicGlanceSettingsUI

import com.sameerasw.essentials.ui.composables.configs.AppLockSettingsUI
import com.sameerasw.essentials.ui.composables.configs.BatteriesSettingsUI
import com.sameerasw.essentials.ui.composables.configs.BatteryNotificationSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ButtonRemapSettingsUI
import com.sameerasw.essentials.ui.composables.configs.CaffeinateSettingsUI
import com.sameerasw.essentials.ui.composables.configs.DynamicNightLightSettingsUI
import com.sameerasw.essentials.ui.composables.configs.KeyboardSettingsUI
import com.sameerasw.essentials.ui.composables.configs.LiveWallpaperSettingsUI
import com.sameerasw.essentials.ui.composables.configs.LocationReachedSettingsUI
import com.sameerasw.essentials.ui.composables.configs.MapsPowerSavingSettingsUI
import com.sameerasw.essentials.ui.composables.configs.NotificationLightingSettingsUI
import com.sameerasw.essentials.ui.composables.configs.OtherCustomizationsSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ShutUpSettingsUI
import com.sameerasw.essentials.ui.composables.configs.QuickSettingsTilesSettingsUI
import com.sameerasw.essentials.ui.composables.configs.RefreshRateSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ScreenLockedSecuritySettingsUI
import com.sameerasw.essentials.ui.composables.configs.ScreenOffWidgetSettingsUI
import com.sameerasw.essentials.ui.composables.configs.SnoozeNotificationsSettingsUI
import com.sameerasw.essentials.ui.composables.configs.SoundModeTileSettingsUI
import com.sameerasw.essentials.ui.composables.configs.StatusBarIconSettingsUI
import com.sameerasw.essentials.ui.composables.configs.TextAnimationsSettingsUI
import com.sameerasw.essentials.ui.composables.configs.WatchSettingsUI
import com.sameerasw.essentials.ui.modifiers.BlurDirection
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
        
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
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
                    val view = LocalView.current
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

                    // FAB State for Notification Lighting
                    var fabExpanded by remember { mutableStateOf(true) }
                    LaunchedEffect(featureId) {
                        if (featureId == "Notification lighting") {
                            fabExpanded = true
                            delay(3000)
                            fabExpanded = false
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
                        isShizukuPermissionGranted
                    ) {
                        val hasMissingPermissions = when (featureId) {
                            "Screen off widget" -> !isAccessibilityEnabled
                            "Statusbar icons" -> !isWriteSecureSettingsEnabled
                            "Notification lighting" -> !isOverlayPermissionGranted || !isNotificationLightingAccessibilityEnabled || !isNotificationListenerEnabled
                            "Button remap" -> !isAccessibilityEnabled
                            "Dynamic night light" -> (if (viewModel.isUseUsageAccess.value) !viewModel.isUsageStatsPermissionGranted.value else !isAccessibilityEnabled) || !isWriteSecureSettingsEnabled
                            "Snooze system notifications" -> !isNotificationListenerEnabled
                            "Screen locked security" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled || !viewModel.isDeviceAdminEnabled.value
                            "App lock" -> if (viewModel.isUseUsageAccess.value) !viewModel.isUsageStatsPermissionGranted.value else !isAccessibilityEnabled
                            "Freeze" -> !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                context
                            )

                            "Location reached" -> !viewModel.isLocationPermissionGranted.value || !viewModel.isBackgroundLocationPermissionGranted.value
                            "Quick settings tiles" -> !viewModel.isWriteSettingsEnabled.value
                            "Screen refresh rate" -> !viewModel.isShizukuPermissionGranted.value
                            // Top level checks for other features (rarely hit if they are children, but safe to add)
                            "Ambient music glance" -> !isAccessibilityEnabled || !isNotificationListenerEnabled
                            "Call vibrations" -> !isReadPhoneStateEnabled || !isNotificationListenerEnabled
                            "Maps power saving mode" -> !isNotificationListenerEnabled || !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                context
                            )

                            "Caffeinate" -> !viewModel.isPostNotificationsEnabled.value
                            "Battery notification" -> !viewModel.isPostNotificationsEnabled.value || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !viewModel.isBluetoothPermissionGranted.value)
                            "Text and animations" -> !viewModel.isWriteSettingsEnabled.value || !isWriteSecureSettingsEnabled
                            "Always on Display" -> !isWriteSecureSettingsEnabled
                            "Lock screen clock" -> !isWriteSecureSettingsEnabled
                            "Other customizations" -> !com.sameerasw.essentials.utils.ShellUtils.hasPermission(context)
                            "Shut-Up!" -> !isWriteSecureSettingsEnabled || !viewModel.isUsageStatsPermissionGranted.value
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
                        com.sameerasw.essentials.ui.components.sheets.FeatureHelpBottomSheet(
                            onDismissRequest = {
                                showHelpSheet = false
                                selectedHelpFeature = null
                            },
                            feature = selectedHelpFeature!!
                        )
                    }

                    if (showInstructionsSheet) {
                        com.sameerasw.essentials.ui.components.sheets.InstructionsBottomSheet(
                            onDismissRequest = { showInstructionsSheet = false }
                        )
                    }

                    if (showWatchInstallHelpSheet) {
                        com.sameerasw.essentials.ui.components.sheets.WatchInstallHelpBottomSheet(
                            onDismissRequest = { showWatchInstallHelpSheet = false }
                        )
                    }

                    val pageTitle = if (featureObj != null) stringResource(featureObj.title) else featureId
                    val hasMenu = featureObj != null && featureObj.aboutDescription != null

                    val statusBarHeightPx = with(LocalDensity.current) {
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
                    }
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

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
                        val hasScroll = featureId != "Sound mode tile" && featureId != "Quick settings tiles" && featureId != "Location reached" && featureId != "Lock screen clock"
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .progressiveBlur(
                                    blurRadius = if (isBlurEnabled) 40f else 0f,
                                    height = with(LocalDensity.current) { 150.dp.toPx() },
                                    direction = BlurDirection.BOTTOM
                                )
                                .then(if (hasScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                        ) {
                            // Top padding for status bar
                            if (featureId != "Quick settings tiles" && featureId != "Location reached") {
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(statusBarHeight))
                            }

                            if (featureId == "Watch") {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                LaunchedEffect(Unit) {
                                    watchViewModel.check(context)
                                }
                                WatchSettingsUI(
                                    viewModel = watchViewModel,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }

                            val children =
                                FeatureRegistry.ALL_FEATURES.filter { it.parentFeatureId == featureId }
                            if (children.isNotEmpty()) {
                                RoundedCardContainer(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 16.dp)
                                ) {
                                    children.forEachIndexed { index, child ->
                                        val permissionAwareToggle: (Boolean) -> Unit = { enabled ->
                                            val missingPermission = when (child.id) {
                                                "Screen off widget" -> !isAccessibilityEnabled
                                                "Statusbar icons" -> !isWriteSecureSettingsEnabled
                                                "Notification lighting" -> !isOverlayPermissionGranted || !isNotificationLightingAccessibilityEnabled || !isNotificationListenerEnabled
                                                "Button remap" -> !isAccessibilityEnabled
                                                "Dynamic night light" -> (if (viewModel.isUseUsageAccess.value) !viewModel.isUsageStatsPermissionGranted.value else !isAccessibilityEnabled) || !isWriteSecureSettingsEnabled
                                                "Snooze system notifications" -> !isNotificationListenerEnabled
                                                "Screen locked security" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled || !viewModel.isDeviceAdminEnabled.value
                                                "App lock" -> if (viewModel.isUseUsageAccess.value) !viewModel.isUsageStatsPermissionGranted.value else !isAccessibilityEnabled
                                                "Freeze" -> !com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                                                    context
                                                )

                                                "Ambient music glance" -> !isAccessibilityEnabled || !isNotificationListenerEnabled
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
                                                "Screen refresh rate" -> !viewModel.isShizukuPermissionGranted.value
                                                "Shut-Up!" -> !isWriteSecureSettingsEnabled || !viewModel.isUsageStatsPermissionGranted.value
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
                                                        child.onToggle(viewModel, context, enabled)
                                                    }
                                                )
                                            }
                                        }

                                        FeatureCard(
                                            title = child.title,
                                            description = child.description,
                                            iconRes = child.iconRes,
                                            isEnabled = child.isEnabled(viewModel),
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
                                                BiometricSecurityHelper.runWithAuth(
                                                    activity = this@FeatureSettingsActivity,
                                                    feature = child,
                                                    action = {
                                                        child.onClick(context, viewModel)
                                                    }
                                                )
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
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Statusbar icons" -> {
                                        StatusBarIconSettingsUI(
                                            viewModel = statusBarViewModel,
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

                                    "Dynamic night light" -> {
                                        DynamicNightLightSettingsUI(
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

                                    "Screen locked security" -> {
                                        ScreenLockedSecuritySettingsUI(
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
                                        com.sameerasw.essentials.ui.composables.configs.FreezeSettingsUI(
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

                                    "Ambient music glance" -> {
                                        AmbientMusicGlanceSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Calendar Sync" -> {
                                        com.sameerasw.essentials.ui.composables.configs.CalendarSyncSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightKey = highlightSetting
                                        )
                                    }

                                    "Lock from Watch" -> {
                                        com.sameerasw.essentials.ui.composables.configs.RemoteLockSettingsUI(
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
                                        com.sameerasw.essentials.ui.composables.configs.FlashlightPulseSettingsUI(
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

                                    "Other customizations" -> {
                                        OtherCustomizationsSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Lock screen clock" -> {
                                        com.sameerasw.essentials.ui.composables.configs.LockScreenClockSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightSetting = highlightSetting
                                        )
                                    }

                                    "Shut-Up!" -> {
                                        ShutUpSettingsUI(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(top = 16.dp),
                                            highlightKey = highlightSetting
                                        )
                                    }
                                }

                            }
                            // Bottom padding for toolbar
                            if (featureId != "Quick settings tiles" && featureId != "Location reached") {
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(150.dp))
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
