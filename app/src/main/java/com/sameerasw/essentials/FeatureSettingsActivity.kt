package com.sameerasw.essentials

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.sameerasw.essentials.utils.HapticFeedbackType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.ui.components.linkActions.LinkPickerScreen
import com.sameerasw.essentials.ui.composables.configs.StatusBarIconSettingsUI
import com.sameerasw.essentials.ui.composables.configs.CaffeinateSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ScreenOffWidgetSettingsUI
import com.sameerasw.essentials.ui.composables.configs.EdgeLightingSettingsUI
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
import com.sameerasw.essentials.ui.composables.configs.PixelImsSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ScreenLockedSecuritySettingsUI
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
class FeatureSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val feature = intent.getStringExtra("feature") ?: "Feature"
        val featureDescriptions = mapOf(
            "Screen off widget" to "Invisible widget to turn the screen off",
            "Statusbar icons" to "Control statusbar icons visibility",
            "Caffeinate" to "Keep the screen awake",
            "Edge lighting" to "Lighting effects for new notifications",
            "Sound mode tile" to "QS tile to toggle sound mode",
            "Link actions" to "Handle links with multiple apps",
            "Flashlight toggle" to "Toggle flashlight while screen off",
            "Dynamic night light" to "Toggle based on current app",
            "Snooze system notifications" to "Automatically snooze persistent notifications",
            "Quick settings tiles" to "All available QS tiles",
            "Pixel IMS" to "Force enable IMS for Pixels",
            "Button remap" to "Remap hardware buttons",
            "Screen locked security" to "Protect network settings from lock screen"
        )
        val description = featureDescriptions[feature] ?: ""
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
                val isEdgeLightingAccessibilityEnabled by viewModel.isEdgeLightingAccessibilityEnabled
                val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled

                // FAB State for Edge Lighting
                var fabExpanded by remember { mutableStateOf(true) }
                LaunchedEffect(feature) {
                    if (feature == "Edge lighting") {
                        fabExpanded = true
                        delay(3000)
                        fabExpanded = false
                    }
                }

                // Show permission sheet if feature has missing permissions
                LaunchedEffect(feature, isAccessibilityEnabled, isWriteSecureSettingsEnabled, isOverlayPermissionGranted, isEdgeLightingAccessibilityEnabled, isNotificationListenerEnabled) {
                    val hasMissingPermissions = when (feature) {
                        "Screen off widget" -> !isAccessibilityEnabled
                        "Statusbar icons" -> !isWriteSecureSettingsEnabled
                        "Edge lighting" -> !isOverlayPermissionGranted || !isEdgeLightingAccessibilityEnabled || !isNotificationListenerEnabled
                        "Button remap" -> !isAccessibilityEnabled
                        "Dynamic night light" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled
                        "Snooze system notifications" -> !isNotificationListenerEnabled
                        "Screen locked security" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled || !viewModel.isDeviceAdminEnabled.value
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
                                description = "Required to perform screen off actions via widget",
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
                                description = "Required to change status bar icon visibility",
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
                        "Edge lighting" -> listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_magnify_fullscreen_24,
                                title = "Overlay Permission",
                                description = "Required to display the edge lighting overlay on the screen",
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
                                description = "Required to trigger edge lighting on new notifications",
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = "Enable in Settings",
                                action = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isEdgeLightingAccessibilityEnabled
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
                                description = "Required to detect lock screen interactions and dismiss panel",
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
                                description = "Required to temporarily adjust animation scale for spam prevention",
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
                                description = "Required to hard-lock the device (disabling biometrics) on unauthorized access attempts",
                                dependentFeatures = PermissionRegistry.getFeatures("DEVICE_ADMIN"),
                                actionLabel = "Enable Admin",
                                action = {
                                    viewModel.requestDeviceAdmin(context)
                                },
                                isGranted = viewModel.isDeviceAdminEnabled.value
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
                        if (feature == "Edge lighting") {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.triggerEdgeLighting(context)
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
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            "Statusbar icons" -> {
                                StatusBarIconSettingsUI(
                                    viewModel = statusBarViewModel,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            "Caffeinate" -> {
                                CaffeinateSettingsUI(
                                    viewModel = caffeinateViewModel,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            "Edge lighting" -> {
                                EdgeLightingSettingsUI(viewModel = viewModel, modifier = Modifier.padding(top = 16.dp))
                            }
                            "Sound mode tile" -> {
                                SoundModeTileSettingsUI(modifier = Modifier.padding(top = 16.dp))
                            }
                            "Button remap" -> {
                                ButtonRemapSettingsUI(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            "Dynamic night light" -> {
                                DynamicNightLightSettingsUI(viewModel = viewModel)
                }
                "Pixel IMS" -> {
                    PixelImsSettingsUI(viewModel = viewModel)
                }
                            "Snooze system notifications" -> {
                                SnoozeNotificationsSettingsUI(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            "Screen locked security" -> {
                                ScreenLockedSecuritySettingsUI(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            "Quick settings tiles" -> {
                                QuickSettingsTilesSettingsUI(
                                    modifier = Modifier.padding(top = 16.dp)
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