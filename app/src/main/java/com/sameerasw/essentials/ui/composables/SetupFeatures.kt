package com.sameerasw.essentials.ui.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.PermissionRegistry
import com.sameerasw.essentials.R
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val previewMainViewModel = MainViewModel()
private const val FEATURE_MAPS_POWER_SAVING = "Maps power saving mode"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SetupFeatures(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    searchRequested: Boolean = false,
    onSearchHandled: () -> Unit = {}
) {
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
    val isWidgetEnabled by viewModel.isWidgetEnabled
    val isStatusBarIconControlEnabled by viewModel.isStatusBarIconControlEnabled
    val isCaffeinateActive by viewModel.isCaffeinateActive
    val isShizukuAvailable by viewModel.isShizukuAvailable
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
    val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled
    val isMapsPowerSavingEnabled by viewModel.isMapsPowerSavingEnabled
    val isEdgeLightingEnabled by viewModel.isEdgeLightingEnabled
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted
    val isEdgeLightingAccessibilityEnabled by viewModel.isEdgeLightingAccessibilityEnabled
    val isButtonRemapEnabled = viewModel.isButtonRemapEnabled.value
    val isDynamicNightLightEnabled = viewModel.isDynamicNightLightEnabled.value
    val isPixelImsEnabled = viewModel.isPixelImsEnabled.value
    val isScreenLockedSecurityEnabled = viewModel.isScreenLockedSecurityEnabled.value
    val context = LocalContext.current

    fun buildMapsPowerSavingPermissionItems(): List<PermissionItem> {
        val items = mutableListOf<PermissionItem>()
        if (!isShizukuAvailable) {
            items.add(
                PermissionItem(
                    iconRes = R.drawable.rounded_adb_24,
                    title = "Shizuku",
                    description = "Required for advanced commands. Install Shizuku from the Play Store.",
                    dependentFeatures = PermissionRegistry.getFeatures("SHIZUKU"),
                    actionLabel = "Install Shizuku",
                    action = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    },
                    isGranted = isShizukuAvailable
                )
            )
        } else if (!isShizukuPermissionGranted) {
            items.add(
                PermissionItem(
                    iconRes = R.drawable.rounded_adb_24,
                    title = "Shizuku permission",
                    description = "Required to run power-saving commands while maps is navigating.",
                    dependentFeatures = PermissionRegistry.getFeatures("SHIZUKU"),
                    actionLabel = "Grant permission",
                    action = { viewModel.requestShizukuPermission() },
                    isGranted = isShizukuPermissionGranted
                )
            )
        }

        if (!isNotificationListenerEnabled) {
            items.add(
                PermissionItem(
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    title = "Notification listener",
                    description = "Required to detect when Maps is navigating.",
                    dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                    actionLabel = if (isNotificationListenerEnabled) "Permission granted" else "Grant listener",
                    action = { viewModel.requestNotificationListenerPermission(context) },
                    isGranted = isNotificationListenerEnabled
                )
            )
        }

        return items
    }

    var showSheet by remember { mutableStateOf(false) }
    var currentFeature by remember { mutableStateOf<String?>(null) }

    // Periodic check for Caffeinate status
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.checkCaffeinateActive(context)
            delay(2000)
        }
    }

    LaunchedEffect(
        showSheet,
        isAccessibilityEnabled,
        isWriteSecureSettingsEnabled,
        isShizukuAvailable,
        isShizukuPermissionGranted,
        isNotificationListenerEnabled,
        isOverlayPermissionGranted,
        isEdgeLightingAccessibilityEnabled,
        currentFeature
    ) {
        if (showSheet && currentFeature != null) {
            val missing = mutableListOf<PermissionItem>()
            when (currentFeature) {
                "Screen off widget" -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = "Accessibility",
                                description = "Required to perform screen off actions via widget",
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                action = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                isGranted = isAccessibilityEnabled
                            )
                        )
                    }
                }
                "Statusbar icons" -> {
                    if (!isWriteSecureSettingsEnabled) {
                        missing.add(
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
                    }
                }
                FEATURE_MAPS_POWER_SAVING -> {
                    missing.addAll(buildMapsPowerSavingPermissionItems())
                }
                "Edge lighting" -> {
                    if (!isOverlayPermissionGranted) {
                        missing.add(
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
                            )
                        )
                    }
                    if (!isEdgeLightingAccessibilityEnabled) {
                        missing.add(
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
                            )
                        )
                    }
                    if (!isNotificationListenerEnabled) {
                        missing.add(
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
                    }
                }
                "Button remap" -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = "Accessibility Service",
                                description = "Required to intercept hardware button events",
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = "Enable in Settings",
                                action = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isAccessibilityEnabled
                            )
                        )
                    }
                }
                "Dynamic night light" -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
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
                            )
                        )
                    }
                    if (!isWriteSecureSettingsEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = "Write Secure Settings",
                                description = "Needed to toggle Night Light. Grant via ADB or root.",
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = "How to grant",
                                action = {
                                    // Maybe show a dialog or link to instructions
                                },
                                isGranted = isWriteSecureSettingsEnabled
                            )
                        )
                    }
                }
                "Pixel IMS" -> {
                    if (!isShizukuAvailable) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_adb_24,
                                title = "Shizuku",
                                description = "Required for Pixel IMS. Install Shizuku from the Play Store.",
                                dependentFeatures = PermissionRegistry.getFeatures("SHIZUKU"),
                                actionLabel = "Install Shizuku",
                                action = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isShizukuAvailable
                            )
                        )
                    } else if (!isShizukuPermissionGranted) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_adb_24,
                                title = "Shizuku permission",
                                description = "Required to override carrier configurations.",
                                dependentFeatures = PermissionRegistry.getFeatures("SHIZUKU"),
                                actionLabel = "Grant permission",
                                action = { viewModel.requestShizukuPermission() },
                                isGranted = isShizukuPermissionGranted
                            )
                        )
                    }
                }
                "Screen locked security" -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = "Accessibility Service",
                                description = "Required to detect lock screen interactions and dismiss panel",
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = "Enable Service",
                                action = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isAccessibilityEnabled
                            )
                        )
                    }
                    if (!isWriteSecureSettingsEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = "Write Secure Settings",
                                description = "Required to temporarily adjust animation scale for spam prevention",
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = "Copy ADB",
                                action = {
                                    val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("adb_command", adbCommand)
                                    clipboard.setPrimaryClip(clip)
                                },
                                isGranted = isWriteSecureSettingsEnabled
                            )
                        )
                    }
                    if (!viewModel.isDeviceAdminEnabled.value) {
                        missing.add(
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
                    }
                }
            }

            if (missing.isEmpty()) {
                showSheet = false
            }
        }
    }

    if (showSheet && currentFeature != null) {
        val permissionItems = when (currentFeature) {
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
                    iconRes = R.drawable.rounded_chevron_right_24,
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
            FEATURE_MAPS_POWER_SAVING -> buildMapsPowerSavingPermissionItems()
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
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        },
                        isGranted = isAccessibilityEnabled
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
                        actionLabel = "How to grant",
                        action = { /* instructions */ },
                        isGranted = isWriteSecureSettingsEnabled
                    )
                )
                "Screen locked security" -> listOf(
                    PermissionItem(
                        iconRes = R.drawable.rounded_settings_accessibility_24,
                        title = "Accessibility Service",
                        description = "Required to detect lock screen interactions and dismiss panel",
                        dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                        actionLabel = "Enable Service",
                        action = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        },
                        isGranted = isAccessibilityEnabled
                    )
                )
                "Pixel IMS" -> buildMapsPowerSavingPermissionItems() // Reusing the same Shizuku logic
            else -> emptyList()
        }

        if (permissionItems.isNotEmpty()) {
            PermissionsBottomSheet(
                onDismissRequest = { showSheet = false },
                featureTitle = currentFeature ?: "",
                permissions = permissionItems
            )
        }
    }

    val scrollState = rememberScrollState()
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val allFeatures = remember {
        mutableStateListOf(
            FeatureItem("Screen off widget", R.drawable.rounded_settings_power_24, "Tools", "Invisible widget to turn the screen off"),
            FeatureItem("Statusbar icons", R.drawable.rounded_interests_24, "Visuals", "Control statusbar icons visibility"),
            FeatureItem("Caffeinate", R.drawable.rounded_coffee_24, "Tools", "Keep the screen awake"),
            FeatureItem(
                FEATURE_MAPS_POWER_SAVING,
                R.drawable.rounded_navigation_24,
                "Tools",
                "For any Android device"
            ),
            FeatureItem(
                "Edge lighting",
                R.drawable.rounded_magnify_fullscreen_24,
                "Visuals",
                "Flash screen for notifications"
            ),
            FeatureItem(
                "Sound mode tile",
                R.drawable.rounded_volume_up_24,
                "Tools",
                "QS tile to toggle sound mode"
            ),
            FeatureItem(
                "Link actions",
                R.drawable.rounded_link_24,
                "Tools",
                "Handle links with multiple apps"
            ),
            FeatureItem(
                "Snooze system notifications",
                R.drawable.rounded_snooze_24,
                "Tools",
                "Snooze persistent notifications"
            ),
            FeatureItem(
                "Quick settings tiles",
                R.drawable.rounded_tile_small_24,
                "System",
                "View all"
            ),
            FeatureItem(
                "Button remap",
                R.drawable.rounded_switch_access_3_24,
                "System",
                "Remap hardware button actions"
            ),
            FeatureItem(
                "Dynamic night light",
                R.drawable.rounded_nightlight_24,
                "Visuals",
                "Toggle night light based on app"
            ),
            FeatureItem(
                "Pixel IMS",
                R.drawable.rounded_wifi_calling_bar_3_24,
                "System",
                "Force enable IMS services on Pixels"
            ),
            FeatureItem(
                "Screen locked security",
                R.drawable.rounded_security_24,
                "System",
                "Prevent network controls"
            )
        )
    }

    var filtered by remember { mutableStateOf(allFeatures.toList()) }
    var isLoading by remember { mutableStateOf(false) }
    var debounceJob: Job? by remember { mutableStateOf(null) }

    LaunchedEffect(searchRequested) {
        if (searchRequested) {
            scrollState.animateScrollTo(0)
            delay(100)
            focusRequester.requestFocus()
            onSearchHandled()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { new ->
                query = new
                debounceJob?.cancel()
                isLoading = true
                debounceJob = kotlinx.coroutines.GlobalScope.launch {
                    delay(250)
                    val q = new.trim().lowercase()
                    filtered = if (q.isEmpty()) allFeatures.toList() else allFeatures.filter { it.title.lowercase().contains(q) }
                    isLoading = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            leadingIcon = { Icon(painter = painterResource(id = R.drawable.rounded_search_24), contentDescription = "Search", modifier = Modifier.size(24.dp)) },
            placeholder = { if (!isFocused && query.isEmpty()) Text("Search for Tools, Mods and Tweaks") },
            shape = RoundedCornerShape(64.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceBright
            )
        )

        // Loading indicator while filtering
        if (isLoading) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LoadingIndicator()
            }
        }

        // No results view
        if (!isLoading && filtered.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¯\\_(ツ)_/¯",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Render filtered features grouped by category
        val categories = filtered.map { it.category }.distinct()
        for (category in categories) {
            val categoryFeatures = filtered.filter { it.category == category }

            // Show category header if there are features in this category
            if (categoryFeatures.isNotEmpty()) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RoundedCardContainer(
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                for (feature in categoryFeatures) {
                    val isEnabled = when (feature.title) {
                        "Screen off widget" -> true // Always enabled since it's a widget
                        "Statusbar icons" -> isStatusBarIconControlEnabled
                        "Caffeinate" -> isCaffeinateActive
                        FEATURE_MAPS_POWER_SAVING -> isMapsPowerSavingEnabled
                        "Edge lighting" -> isEdgeLightingEnabled
                        "Sound mode tile" -> true // Always enabled since it's a tile
                        "Button remap" -> true
                        "Dynamic night light" -> isDynamicNightLightEnabled
                        "Pixel IMS" -> isPixelImsEnabled
                        "Screen locked security" -> isScreenLockedSecurityEnabled
                        else -> false
                    }

                    val isToggleEnabled = when (feature.title) {
                        "Screen off widget" -> false // No toggle for widget
                        "Statusbar icons" -> isWriteSecureSettingsEnabled
                        "Caffeinate" -> true
                        FEATURE_MAPS_POWER_SAVING -> isShizukuAvailable && isShizukuPermissionGranted && isNotificationListenerEnabled
                        "Edge lighting" -> isOverlayPermissionGranted && isEdgeLightingAccessibilityEnabled && isNotificationListenerEnabled
                        "Sound mode tile" -> false // No toggle for QS tile
                        "Button remap" -> isAccessibilityEnabled
                        "Snooze system notifications" -> isNotificationListenerEnabled
                        "Dynamic night light" -> isAccessibilityEnabled && isWriteSecureSettingsEnabled
                        "Pixel IMS" -> isShizukuAvailable && isShizukuPermissionGranted
                        "Screen locked security" -> isAccessibilityEnabled && isWriteSecureSettingsEnabled && viewModel.isDeviceAdminEnabled.value
                        else -> false
                    }

                    val featureOnClick = if (feature.title == FEATURE_MAPS_POWER_SAVING) {
                        {}
                    } else {
                        {
                            context.startActivity(
                                Intent(context, FeatureSettingsActivity::class.java).apply {
                                    putExtra("feature", feature.title)
                                }
                            )
                        }
                    }

                    FeatureCard(
                        title = feature.title,
                        isEnabled = isEnabled,
                        onToggle = { enabled ->
                            when (feature.title) {
                                "Screen off widget" -> {} // No toggle action needed for widget
                                "Statusbar icons" -> viewModel.setStatusBarIconControlEnabled(enabled, context)
                                "Caffeinate" -> if (enabled) viewModel.startCaffeinate(context) else viewModel.stopCaffeinate(context)
                                FEATURE_MAPS_POWER_SAVING -> viewModel.setMapsPowerSavingEnabled(enabled, context)
                                "Edge lighting" -> viewModel.setEdgeLightingEnabled(enabled, context)
                                "Sound mode tile" -> {} // No toggle action needed for tile
                                "Button remap" -> viewModel.setButtonRemapEnabled(enabled, context)
                                "Dynamic night light" -> viewModel.setDynamicNightLightEnabled(enabled, context)
                                "Pixel IMS" -> viewModel.setPixelImsEnabled(enabled, context)
                                "Screen locked security" -> viewModel.setScreenLockedSecurityEnabled(enabled, context)
                                else -> {}
                            }
                        },
                        onClick = featureOnClick,
                        iconRes = feature.iconRes,
                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
                        isToggleEnabled = isToggleEnabled,
                        showToggle = feature.title != "Sound mode tile" && feature.title != "Screen off widget" && feature.title != "Link actions" && feature.title != "Snooze system notifications" && feature.title != "Quick settings tiles" && feature.title != "Pixel IMS" && feature.title != "Button remap", // Hide toggle for Sound mode tile, Screen off widget, Link actions, Snooze notifications, QS Tiles, Pixel IMS, and Button remap
                        hasMoreSettings = feature.title != FEATURE_MAPS_POWER_SAVING,
                        onDisabledToggleClick = {
                            currentFeature = feature.title
                            showSheet = true
                        },
                        description = feature.description
                    )
                }
            }
        }
    }
}

private data class FeatureItem(val title: String, val iconRes: Int, val category: String, val description: String)

@Preview(showBackground = true)
@Composable
fun SetupFeaturesPreview() {
    EssentialsTheme {
        val mockViewModel = previewMainViewModel.apply {
            isAccessibilityEnabled.value = false
        }
        SetupFeatures(viewModel = mockViewModel)
    }
}