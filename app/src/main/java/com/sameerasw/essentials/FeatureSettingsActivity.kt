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
import com.sameerasw.essentials.viewmodels.CaffeinateViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.viewmodels.StatusBarIconViewModel
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet

import com.sameerasw.essentials.ui.composables.configs.AppLockSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ScreenLockedSecuritySettingsUI
import com.sameerasw.essentials.utils.HapticUtil

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
        val feature = intent.getStringExtra("feature") ?: "Feature"
        val featureDescriptions = mapOf(
            "Screen off widget" to "Invisible widget to turn the screen off",
            "Statusbar icons" to "Control statusbar icons visibility",
            "Caffeinate" to "Keep the screen awake",
            "Notification lighting" to "Lighting effects for new notifications",
            "Sound mode tile" to "QS tile to toggle sound mode",
            "Link actions" to "Handle links with multiple apps",
            "Flashlight toggle" to "Toggle flashlight while screen off",
            "Dynamic night light" to "Toggle based on current app",
            "Snooze system notifications" to "Automatically snooze persistent notifications",
            "Quick settings tiles" to "All available QS tiles",
            "Button remap" to "Remap hardware buttons",
            "Screen locked security" to "Protect network settings from lock screen",
            "App lock" to "Secure individual apps with biometrics",
            "Freeze" to "Disable rarely used apps"
        )
        val description = featureDescriptions[feature] ?: ""
        val highlightSetting = intent.getStringExtra("highlight_setting")
        setContent {
            EssentialsTheme {
                val context = LocalContext.current
                val view = LocalView.current
                val prefs = context.getSharedPreferences("essentials_prefs", MODE_PRIVATE)

                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
                }

                val viewModel: MainViewModel = viewModel()
                val statusBarViewModel: StatusBarIconViewModel = viewModel()
                val caffeinateViewModel: CaffeinateViewModel = viewModel()

                // Automatic refresh on resume
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.check(context)
                            if (feature == "Statusbar icons") {
                                statusBarViewModel.check(context)
                            }
                            if (feature == "Caffeinate") {
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
                LaunchedEffect(feature) {
                    if (feature == "Notification lighting") {
                        fabExpanded = true
                        delay(3000)
                        fabExpanded = false
                    }
                }

                // Show permission sheet if feature has missing permissions
                LaunchedEffect(feature, isAccessibilityEnabled, isWriteSecureSettingsEnabled, isOverlayPermissionGranted, isNotificationLightingAccessibilityEnabled, isNotificationListenerEnabled) {
                    val hasMissingPermissions = when (feature) {
                        "Screen off widget" -> !isAccessibilityEnabled
                        "Statusbar icons" -> !isWriteSecureSettingsEnabled
                        "Notification lighting" -> !isOverlayPermissionGranted || !isNotificationLightingAccessibilityEnabled || !isNotificationListenerEnabled
                        "Button remap" -> !isAccessibilityEnabled
                        "Dynamic night light" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled
                        "Snooze system notifications" -> !isNotificationListenerEnabled
                        "Screen locked security" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled || !viewModel.isDeviceAdminEnabled.value
                        "App lock" -> !isAccessibilityEnabled
                        "Freeze" -> !viewModel.isShizukuAvailable.value || !viewModel.isShizukuPermissionGranted.value
                        else -> false
                    }
                    showPermissionSheet = hasMissingPermissions
                }

                if (showPermissionSheet) {
                    val permissionItems = when (feature) {
                        "Screen off widget" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = "Accessibility",
                                description = "Required for App Lock, Screen off widget and other features to detect interactions",
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = "Grant Permission",
                                action = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                isGranted = isAccessibilityEnabled
                            )
                        )
                        "Statusbar icons" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = "Write Secure Settings",
                                description = "Required for Statusbar icons and Screen Locked Security",
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = "Copy ADB",
                                action = {
                                    val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                                    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("adb_command", adbCommand)
                                    clipboard.setPrimaryClip(clip)
                                },
                                secondaryActionLabel = "Check",
                                secondaryAction = {
                                    viewModel.isWriteSecureSettingsEnabled.value = viewModel.canWriteSecureSettings(context)
                                },
                                isGranted = isWriteSecureSettingsEnabled
                            )
                        )
                        "Notification lighting" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_magnify_fullscreen_24,
                                title = "Overlay Permission",
                                description = "Required to display the notification lighting overlay on the screen",
                                dependentFeatures = PermissionRegistry.getFeatures("DRAW_OVERLAYS"),
                                actionLabel = "Grant Permission",
                                action = {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isOverlayPermissionGranted
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = "Accessibility Service",
                                description = "Required to trigger notification lighting on new notifications",
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = "Enable in Settings",
                                action = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isNotificationLightingAccessibilityEnabled
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_notifications_unread_24,
                                title = "Notification Listener",
                                description = "Required to detect new notifications",
                                dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                                actionLabel = if (isNotificationListenerEnabled) "Permission granted" else "Grant listener",
                                action = { viewModel.requestNotificationListenerPermission(context) },
                                isGranted = isNotificationListenerEnabled
                            )
                        )
                        "Button remap" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = "Accessibility Service",
                                description = "Required to intercept hardware button events",
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = "Enable in Settings",
                                action = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                isGranted = isAccessibilityEnabled
                            )
                        )
                        "Dynamic night light" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = "Accessibility Service",
                                description = "Needed to monitor foreground applications.",
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = "Enable Service",
                                action = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isAccessibilityEnabled
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = "Write Secure Settings",
                                description = "Needed to toggle Night Light. Grant via ADB or root.",
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = "Copy ADB",
                                action = {
                                    val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                                    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("adb_command", adbCommand)
                                    clipboard.setPrimaryClip(clip)
                                },
                                secondaryActionLabel = "Check",
                                secondaryAction = {
                                    viewModel.isWriteSecureSettingsEnabled.value = viewModel.canWriteSecureSettings(context)
                                },
                                isGranted = isWriteSecureSettingsEnabled
                            )
                        )
                        "Snooze system notifications" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_snooze_24,
                                title = "Notification Listener",
                                description = "Required to detect and snooze notifications",
                                dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                                actionLabel = if (isNotificationListenerEnabled) "Permission granted" else "Grant listener",
                                action = { viewModel.requestNotificationListenerPermission(context) },
                                isGranted = isNotificationListenerEnabled
                            )
                        )
                        "Screen locked security" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = "Accessibility Service",
                                description = "Required for App Lock, Screen Locked Security and other features to detect interactions",
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = "Enable in Settings",
                                action = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                isGranted = isAccessibilityEnabled
                            ),
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = "Write Secure Settings",
                                description = "Required for Statusbar icons and Screen Locked Security",
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = "Copy ADB",
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
                                title = "Device Administrator",
                                description = "Required to lock the device on unauthorized access attempts for Screen Locked Security",
                                dependentFeatures = PermissionRegistry.getFeatures("DEVICE_ADMIN"),
                                actionLabel = "Enable Admin",
                                action = {
                                    viewModel.requestDeviceAdmin(context)
                                },
                                isGranted = viewModel.isDeviceAdminEnabled.value
                            )
                        )
                        "App lock" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = "Accessibility Service",
                                description = "Required for App Lock and other features to detect app launches",
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = "Enable in Settings",
                                action = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                isGranted = isAccessibilityEnabled
                            )
                        )
                        "Freeze" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_mode_cool_24,
                                title = "Shizuku Service",
                                description = "Required to disable/freeze applications",
                                dependentFeatures = PermissionRegistry.getFeatures("SHIZUKU"),
                                actionLabel = if (viewModel.isShizukuPermissionGranted.value) "Permission granted" else "Grant Shizuku",
                                action = { viewModel.requestShizukuPermission() },
                                isGranted = viewModel.isShizukuPermissionGranted.value
                            )
                        )
                        else -> emptyList()
                    }

                    if (permissionItems.isNotEmpty()) {
                        PermissionsBottomSheet(
                            onDismissRequest = { showPermissionSheet = false },
                            featureTitle = feature,
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
                            title = feature,
                            hasBack = true,
                            hasSearch = false,
                            onBackClick = { finish() },
                            scrollBehavior = scrollBehavior,
                            subtitle = description
                        )
                    },
                    floatingActionButton = {
                        if (feature == "Notification lighting") {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.triggerNotificationLighting(context)
                                },
                                expanded = fabExpanded,
                                icon = { Icon(painter = painterResource(id = R.drawable.rounded_play_arrow_24), contentDescription = null) },
                                text = { Text("Preview") },
                                modifier = Modifier.height(64.dp)
                            )
                        }
                    }
                ) { innerPadding ->
                    val hasScroll = feature != "Sound mode tile"
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .then(if (hasScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    ) {
                        when (feature) {
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
                            "Link actions" -> {
                                setContent {
                                    EssentialsTheme {
                                        LinkPickerScreen(
                                            uri = "https://sameerasw.com".toUri(),
                                            onFinish = { finish() },
                                            modifier = Modifier.fillMaxSize(),
                                            demo = true
                                        )
                                    }
                                }
                            }
                            else -> {
                                ScreenOffWidgetSettingsUI(
                                    viewModel = viewModel,
                                    selectedHaptic = selectedHaptic,
                                    onHapticSelected = { type -> selectedHaptic = type },
                                    vibrator = vibrator,
                                    prefs = prefs,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}