package com.sameerasw.essentials

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.domain.HapticFeedbackType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.domain.registry.PermissionRegistry
import com.sameerasw.essentials.ui.components.linkActions.LinkPickerScreen
import com.sameerasw.essentials.ui.composables.configs.StatusBarIconSettingsUI
import com.sameerasw.essentials.ui.composables.configs.CaffeinateSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ScreenOffWidgetSettingsUI
import com.sameerasw.essentials.ui.composables.configs.NotificationLightingSettingsUI
import com.sameerasw.essentials.ui.composables.configs.SoundModeTileSettingsUI
import com.sameerasw.essentials.ui.composables.configs.QuickSettingsTilesSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ButtonRemapSettingsUI
import com.sameerasw.essentials.ui.composables.configs.DynamicNightLightSettingsUI
import com.sameerasw.essentials.ui.composables.configs.SnoozeNotificationsSettingsUI
import com.sameerasw.essentials.ui.composables.configs.AmbientMusicGlanceSettingsUI
import com.sameerasw.essentials.ui.composables.configs.LocationReachedSettingsUI
import com.sameerasw.essentials.ui.composables.configs.BatteriesSettingsUI
import com.sameerasw.essentials.ui.composables.configs.MapsPowerSavingSettingsUI
import com.sameerasw.essentials.viewmodels.CaffeinateViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.viewmodels.StatusBarIconViewModel
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet

import com.sameerasw.essentials.ui.composables.configs.AppLockSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ScreenLockedSecuritySettingsUI
import com.sameerasw.essentials.ui.composables.configs.KeyboardSettingsUI
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.domain.registry.FeatureRegistry

@OptIn(ExperimentalMaterial3Api::class)
class FeatureSettingsActivity : FragmentActivity() {

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
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.check(context)
            }
            
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val view = LocalView.current
                val prefs = context.getSharedPreferences("essentials_prefs", MODE_PRIVATE)

                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
                }

                var selectedHaptic by remember {
                    val name = prefs.getString("haptic_feedback_type", HapticFeedbackType.NONE.name)
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
                val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
                val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
                val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted
                val isNotificationLightingAccessibilityEnabled by viewModel.isNotificationLightingAccessibilityEnabled
                val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled

                // FAB State for Notification Lighting
                var fabExpanded by remember { mutableStateOf(true) }
                LaunchedEffect(featureId) {
                    if (featureId == "Notification lighting") {
                        fabExpanded = true
                        delay(3000)
                        fabExpanded = false
                    }
                }

                // Show permission sheet if feature has missing permissions
                LaunchedEffect(featureId, isAccessibilityEnabled, isWriteSecureSettingsEnabled, isOverlayPermissionGranted, isNotificationLightingAccessibilityEnabled, isNotificationListenerEnabled) {
                    val hasMissingPermissions = when (featureId) {
                        "Screen off widget" -> !isAccessibilityEnabled
                        "Statusbar icons" -> !isWriteSecureSettingsEnabled
                        "Notification lighting" -> !isOverlayPermissionGranted || !isNotificationLightingAccessibilityEnabled || !isNotificationListenerEnabled
                        "Button remap" -> !isAccessibilityEnabled
                        "Dynamic night light" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled
                        "Snooze system notifications" -> !isNotificationListenerEnabled
                        "Screen locked security" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled || !viewModel.isDeviceAdminEnabled.value
                        "App lock" -> !isAccessibilityEnabled
                        "Freeze" -> !com.sameerasw.essentials.utils.ShellUtils.hasPermission(context)
                        "Location reached" -> !viewModel.isLocationPermissionGranted.value || !viewModel.isBackgroundLocationPermissionGranted.value
                        "Quick settings tiles" -> !viewModel.isWriteSettingsEnabled.value
                        else -> false
                    }
                    showPermissionSheet = hasMissingPermissions
                }

                if (showPermissionSheet) {
                    val permissionItems = when (featureId) {
                        "Screen off widget" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = R.string.perm_accessibility_title,
                                description = R.string.perm_accessibility_desc_common,
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = R.string.perm_action_grant,
                                action = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                isGranted = isAccessibilityEnabled
                            )
                        )
                        "Statusbar icons" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = R.string.perm_write_secure_title,
                                description = R.string.perm_write_secure_desc_common,
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = R.string.perm_action_copy_adb,
                                action = {
                                    val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                                    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("adb_command", adbCommand)
                                    clipboard.setPrimaryClip(clip)
                                },
                                secondaryActionLabel = R.string.perm_action_check,
                                secondaryAction = {
                                    viewModel.isWriteSecureSettingsEnabled.value = viewModel.canWriteSecureSettings(context)
                                },
                                isGranted = isWriteSecureSettingsEnabled
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = R.string.perm_write_settings_title,
                                description = R.string.perm_write_settings_desc,
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SETTINGS"),
                                actionLabel = R.string.perm_action_grant,
                                action = {
                                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = viewModel.isWriteSettingsEnabled.value
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_adb_24,
                                title = R.string.perm_shizuku_title,
                                description = R.string.perm_shizuku_desc,
                                dependentFeatures = listOf("Advanced Statusbar control"),
                                actionLabel = if (com.sameerasw.essentials.utils.ShizukuUtils.isShizukuAvailable()) R.string.perm_shizuku_grant_title else R.string.perm_shizuku_install_action,
                                action = {
                                    if (com.sameerasw.essentials.utils.ShizukuUtils.isShizukuAvailable()) {
                                        com.sameerasw.essentials.utils.ShizukuUtils.requestPermission()
                                    } else {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    }
                                },
                                isGranted = com.sameerasw.essentials.utils.ShizukuUtils.hasPermission()
                            )
                        )
                        "Notification lighting" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_magnify_fullscreen_24,
                                title = R.string.perm_overlay_title,
                                description = R.string.perm_overlay_desc,
                                dependentFeatures = PermissionRegistry.getFeatures("DRAW_OVERLAYS"),
                                actionLabel = R.string.perm_action_grant,
                                action = {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isOverlayPermissionGranted
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = R.string.perm_accessibility_title,
                                description = R.string.perm_accessibility_desc_lighting,
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = R.string.perm_action_enable,
                                action = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isNotificationLightingAccessibilityEnabled
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_notifications_unread_24,
                                title = R.string.perm_notif_listener_title,
                                description = R.string.perm_notif_listener_desc_lighting,
                                dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                                actionLabel = R.string.perm_action_grant,
                                action = { viewModel.requestNotificationListenerPermission(context) },
                                isGranted = isNotificationListenerEnabled
                            )
                        )
                        "Button remap" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = R.string.perm_accessibility_title,
                                description = R.string.perm_accessibility_desc_remap,
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = R.string.perm_action_enable,
                                action = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                isGranted = isAccessibilityEnabled
                            )
                        )
                        "Dynamic night light" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = R.string.perm_accessibility_title,
                                description = R.string.perm_accessibility_desc_night_light,
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = R.string.perm_action_enable,
                                action = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isAccessibilityEnabled
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = R.string.perm_write_secure_title,
                                description = R.string.perm_write_secure_desc_night_light,
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = R.string.perm_action_copy_adb,
                                action = {
                                    val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                                    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("adb_command", adbCommand)
                                    clipboard.setPrimaryClip(clip)
                                },
                                secondaryActionLabel = R.string.perm_action_check,
                                secondaryAction = {
                                    viewModel.isWriteSecureSettingsEnabled.value = viewModel.canWriteSecureSettings(context)
                                },
                                isGranted = isWriteSecureSettingsEnabled
                            )
                        )
                        "Snooze system notifications" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_snooze_24,
                                title = R.string.perm_notif_listener_title,
                                description = R.string.perm_notif_listener_desc_snooze,
                                dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                                actionLabel = R.string.perm_action_grant,
                                action = { viewModel.requestNotificationListenerPermission(context) },
                                isGranted = isNotificationListenerEnabled
                            )
                        )
                        "Screen locked security" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = R.string.perm_accessibility_title,
                                description = R.string.perm_accessibility_desc_common,
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = R.string.perm_action_enable,
                                action = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                isGranted = isAccessibilityEnabled
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = R.string.perm_write_secure_title,
                                description = R.string.perm_write_secure_desc_common,
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = R.string.perm_action_copy_adb,
                                action = {
                                    val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                                    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("adb_command", adbCommand)
                                    clipboard.setPrimaryClip(clip)
                                },
                                isGranted = isWriteSecureSettingsEnabled
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = R.string.perm_device_admin_title,
                                description = R.string.perm_device_admin_desc,
                                dependentFeatures = PermissionRegistry.getFeatures("DEVICE_ADMIN"),
                                actionLabel = R.string.action_enable_in_settings,
                                action = {
                                    viewModel.requestDeviceAdmin(context)
                                },
                                isGranted = viewModel.isDeviceAdminEnabled.value
                            )
                        )
                        "App lock" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = R.string.perm_accessibility_title,
                                description = R.string.perm_accessibility_desc_common,
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = R.string.perm_action_enable,
                                action = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                isGranted = isAccessibilityEnabled
                            )
                        )
                        "Freeze" -> {
                            val isRootEnabled = com.sameerasw.essentials.utils.ShellUtils.isRootEnabled(context)
                            listOf(
                                if (isRootEnabled) {
                                    PermissionItem(
                                        iconRes = R.drawable.rounded_numbers_24,
                                        title = R.string.perm_root_title,
                                        description = R.string.perm_root_desc,
                                        dependentFeatures = PermissionRegistry.getFeatures("ROOT"),
                                        actionLabel = R.string.perm_action_grant,
                                        action = { viewModel.check(context) },
                                        isGranted = viewModel.isRootPermissionGranted.value
                                    )
                                } else {
                                    PermissionItem(
                                        iconRes = R.drawable.rounded_mode_cool_24,
                                        title = R.string.perm_shizuku_title,
                                        description = R.string.perm_shizuku_desc,
                                        dependentFeatures = PermissionRegistry.getFeatures("SHIZUKU"),
                                        actionLabel = R.string.perm_action_grant,
                                        action = { viewModel.requestShizukuPermission() },
                                        isGranted = viewModel.isShizukuPermissionGranted.value
                                    )
                                }
                            )
                        }
                        "Location reached" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_navigation_24,
                                title = R.string.perm_location_title,
                                description = R.string.perm_location_desc,
                                dependentFeatures = PermissionRegistry.getFeatures("LOCATION"),
                                actionLabel = R.string.perm_action_grant,
                                action = { viewModel.requestLocationPermission(this) },
                                isGranted = viewModel.isLocationPermissionGranted.value
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_navigation_24,
                                title = R.string.perm_bg_location_title,
                                description = R.string.perm_bg_location_desc,
                                dependentFeatures = PermissionRegistry.getFeatures("BACKGROUND_LOCATION"),
                                actionLabel = R.string.perm_action_grant,
                                action = { viewModel.requestBackgroundLocationPermission(this) },
                                isGranted = viewModel.isBackgroundLocationPermissionGranted.value
                            )
                        )
                        "Quick settings tiles" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = R.string.perm_write_settings_title,
                                description = R.string.perm_write_settings_desc,
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SETTINGS"),
                                actionLabel = R.string.perm_action_grant,
                                action = {
                                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = viewModel.isWriteSettingsEnabled.value
                            )
                        )
                        else -> emptyList()
                    }

                    if (permissionItems.isNotEmpty()) {
                        PermissionsBottomSheet(
                            onDismissRequest = { showPermissionSheet = false },
                            featureTitle = if (featureObj != null) stringResource(featureObj.title) else featureId,
                            permissions = permissionItems
                        )
                    }
                }

                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = if (featureObj != null) stringResource(featureObj.title) else featureId,
                            hasBack = true,
                            hasSearch = false,
                            onBackClick = { finish() },
                            scrollBehavior = scrollBehavior,
                            subtitle = if (featureObj != null) stringResource(featureObj.description) else "",
                            isBeta = featureObj?.isBeta ?: false
                        )
                    },
                    floatingActionButton = {
                        if (featureId == "Notification lighting") {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.triggerNotificationLighting(context)
                                },
                                expanded = fabExpanded,
                                icon = { Icon(painter = painterResource(id = R.drawable.rounded_play_arrow_24), contentDescription = null) },
                                text = { Text(stringResource(R.string.action_preview)) },
                                modifier = Modifier.height(64.dp)
                            )
                        }
                    }
                ) { innerPadding ->
                    val hasScroll = featureId != "Sound mode tile"
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .then(if (hasScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    ) {
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
                                    modifier = Modifier.padding(top = 16.dp),
                                    highlightSetting = highlightSetting
                                )
                            }
                            "Location reached" -> {
                                LocationReachedSettingsUI(
                                    mainViewModel = viewModel,
                                    modifier = Modifier.padding(top = 16.dp),
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
                            "Ambient music glance" -> {
                                AmbientMusicGlanceSettingsUI(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(top = 16.dp),
                                    highlightSetting = highlightSetting
                                )
                            }
<<<<<<< HEAD
                            "Calendar Sync" -> {
                                com.sameerasw.essentials.ui.composables.configs.CalendarSyncSettingsUI(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(top = 16.dp),
                                    highlightKey = highlightSetting
=======
                            "Maps power saving mode" -> {
                                MapsPowerSavingSettingsUI(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(top = 16.dp),
                                    highlightSetting = highlightSetting
>>>>>>> 37baec7 (Fix #162 Attempt at maps power saving language independent detection)
                                )
                            }
                            // else -> default UI (optional cleanup)
                        }
                    }
                }
            }
        }
    }
}