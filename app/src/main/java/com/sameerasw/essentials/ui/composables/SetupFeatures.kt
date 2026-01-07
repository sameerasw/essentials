package com.sameerasw.essentials.ui.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.fragment.app.FragmentActivity
import com.sameerasw.essentials.utils.BiometricHelper
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.essentials.FeatureRegistry
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.PermissionRegistry
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.delay

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
    val isShizukuAvailable by viewModel.isShizukuAvailable
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
    val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted
    val isEdgeLightingAccessibilityEnabled by viewModel.isEdgeLightingAccessibilityEnabled
    viewModel.isButtonRemapEnabled.value
    viewModel.isDynamicNightLightEnabled.value

    viewModel.isScreenLockedSecurityEnabled.value
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
                        val intent = Intent(Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api".toUri())
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
                    actionLabel = "Grant listener",
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
                                description = "Required for App Lock, Screen off widget and other features to detect interactions",
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
                                description = "Required for Statusbar icons and Screen Locked Security",
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
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        "package:${context.packageName}".toUri())
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

                "Screen locked security" -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = "Accessibility Service",
                                description = "Required for App Lock, Screen Locked Security and other features to detect interactions",
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
                                description = "Required for Statusbar icons and Screen Locked Security",
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
                "App lock" -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = "Accessibility Service",
                                description = "Required for App Lock and other features to detect app launches",
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
                    iconRes = R.drawable.rounded_chevron_right_24,
                    title = "Write Secure Settings",
                    description = "Required for Statusbar icons and Screen Locked Security",
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
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri())
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
                        description = "Required for App Lock, Screen Locked Security and other features to detect interactions",
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

                "App lock" -> listOf(
                    PermissionItem(
                        iconRes = R.drawable.rounded_settings_accessibility_24,
                        title = "Accessibility Service",
                        description = "Required for App Lock and other features to detect app launches",
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
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val allFeatures = FeatureRegistry.ALL_FEATURES

    var filtered by remember { mutableStateOf(allFeatures.toList()) }

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
            value = viewModel.searchQuery.value,
            onValueChange = { new ->
                viewModel.onSearchQueryChanged(new)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_search_24),
                    contentDescription = "Search",
                    modifier = Modifier.size(24.dp)
                )
            },
            placeholder = { if (!isFocused && viewModel.searchQuery.value.isEmpty()) Text("Search for Tools, Mods and Tweaks") },
            shape = RoundedCornerShape(64.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceBright
            )
        )

        val searchQuery = viewModel.searchQuery.value
        val searchResults = viewModel.searchResults.value
        val isSearchingViewModel = viewModel.isSearching.value

        // Loading indicator while filtering
        if (isSearchingViewModel) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoadingIndicator()
            }
        }

        // No results view
        if (!isSearchingViewModel && searchQuery.isNotEmpty() && searchResults.isEmpty()) {
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
                Text(
                    text = "No results for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (searchQuery.isNotEmpty()) {
            // Render Search Results
            if (searchResults.isNotEmpty()) {
                Text(
                    text = "Search Results",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RoundedCardContainer(
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    for (result in searchResults) {
                        FeatureCard(
                            title = result.title,
                            isEnabled = true,
                            onToggle = {},
                            onClick = {
                                val action = {
                                    context.startActivity(
                                        Intent(context, FeatureSettingsActivity::class.java).apply {
                                            putExtra("feature", result.featureKey)
                                            result.targetSettingHighlightKey?.let {
                                                putExtra("highlight_setting", it)
                                            }
                                        }
                                    )
                                }
                                if (result.category == "Security and Privacy" && context is FragmentActivity) {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = context,
                                        title = "${result.title} Settings",
                                        subtitle = "Authenticate to access settings",
                                        onSuccess = action
                                    )
                                } else {
                                    action()
                                }
                            },
                            iconRes = result.icon ?: R.drawable.rounded_settings_24,
                            modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
                            showToggle = false,
                            hasMoreSettings = true,
                            description = if (result.parentFeature != null) "${result.parentFeature} > ${result.description}" else result.description
                        )
                    }
                }
            }
        } else {
            // Render filtered features grouped by category (Original View)
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
                        FeatureCard(
                            title = feature.title,
                            isEnabled = feature.isEnabled(viewModel),
                            onToggle = { enabled ->
                                if (feature.category == "Security and Privacy" && context is FragmentActivity) {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = context,
                                        title = "${feature.title} Security",
                                        subtitle = if (enabled) "Authenticate to enable this feature" else "Authenticate to disable this feature",
                                        onSuccess = { feature.onToggle(viewModel, context, enabled) }
                                    )
                                } else {
                                    feature.onToggle(viewModel, context, enabled)
                                }
                            },
                            onClick = {
                                if (feature.category == "Security and Privacy" && context is FragmentActivity) {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = context,
                                        title = "${feature.title} Settings",
                                        subtitle = "Authenticate to access settings",
                                        onSuccess = { feature.onClick(context, viewModel) }
                                    )
                                } else {
                                    feature.onClick(context, viewModel)
                                }
                            },
                            iconRes = feature.iconRes,
                            modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
                            isToggleEnabled = feature.isToggleEnabled(viewModel, context),
                            showToggle = feature.showToggle,
                            hasMoreSettings = feature.hasMoreSettings,
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
}
