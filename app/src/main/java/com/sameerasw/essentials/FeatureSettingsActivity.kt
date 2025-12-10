package com.sameerasw.essentials

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticFeedbackType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.ui.composables.configs.StatusBarIconSettingsUI
import com.sameerasw.essentials.ui.composables.configs.CaffeinateSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ScreenOffWidgetSettingsUI
import com.sameerasw.essentials.ui.composables.configs.EdgeLightingSettingsUI
import com.sameerasw.essentials.ui.composables.configs.SoundModeTileSettingsUI
import com.sameerasw.essentials.viewmodels.CaffeinateViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.viewmodels.StatusBarIconViewModel
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet

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
            "Edge lighting" to "Preview edge lighting effects on new notifications",
            "Sound mode tile" to "QS tile to toggle sound mode"
        )
        val description = featureDescriptions[feature] ?: ""
        setContent {
            EssentialsTheme {
                val context = LocalContext.current
                val prefs = context.getSharedPreferences("essentials_prefs", MODE_PRIVATE)

                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
                }

                val viewModel: MainViewModel = viewModel()
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

                // Show permission sheet if feature has missing permissions
                LaunchedEffect(feature, isAccessibilityEnabled, isWriteSecureSettingsEnabled, isOverlayPermissionGranted, isEdgeLightingAccessibilityEnabled, isNotificationListenerEnabled) {
                    val hasMissingPermissions = when (feature) {
                        "Screen off widget" -> !isAccessibilityEnabled
                        "Statusbar icons" -> !isWriteSecureSettingsEnabled
                        "Edge lighting" -> !isOverlayPermissionGranted || !isEdgeLightingAccessibilityEnabled || !isNotificationListenerEnabled
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
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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
                                val statusBarViewModel: StatusBarIconViewModel = viewModel()
                                LaunchedEffect(Unit) {
                                    statusBarViewModel.check(context)
                                }
                                StatusBarIconSettingsUI(
                                    viewModel = statusBarViewModel,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            "Caffeinate" -> {
                                val caffeinateViewModel: CaffeinateViewModel = viewModel()
                                LaunchedEffect(Unit) {
                                    caffeinateViewModel.check(context)
                                }
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