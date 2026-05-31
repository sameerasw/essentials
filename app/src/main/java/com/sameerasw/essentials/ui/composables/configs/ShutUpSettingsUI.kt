package com.sameerasw.essentials.ui.composables.configs

import android.Manifest
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.*
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.components.sheets.ShutUpPerAppSettingsSheet
import com.sameerasw.essentials.ui.components.pickers.RestoreModePicker
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.components.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShutUpSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    var showPermissionSheet by remember { mutableStateOf(false) }
    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }
    var isEditSheetOpen by remember { mutableStateOf(false) }
    var editingPackageName by remember { mutableStateOf("") }
    var editingConfig by remember { mutableStateOf<ShutUpAppConfig?>(null) }

    // Permission states checked on composition and changes
    var hasSecureSettings by remember { mutableStateOf(PermissionUtils.canWriteSecureSettings(context)) }
    var hasWriteSettings by remember { mutableStateOf(PermissionUtils.canWriteSystemSettings(context)) }
    var hasUsageStats by remember { mutableStateOf(PermissionUtils.hasUsageStatsPermission(context)) }
    var hasNotifications by remember { mutableStateOf(PermissionUtils.isPostNotificationsEnabled(context)) }

    val hasAllPermissions = hasSecureSettings && hasWriteSettings && hasUsageStats && hasNotifications

    // System Permission Launcher for Post Notifications
    val requestNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotifications = isGranted
    }

    LaunchedEffect(Unit) {
        hasSecureSettings = PermissionUtils.canWriteSecureSettings(context)
        hasWriteSettings = PermissionUtils.canWriteSystemSettings(context)
        hasUsageStats = PermissionUtils.hasUsageStatsPermission(context)
        hasNotifications = PermissionUtils.isPostNotificationsEnabled(context)
    }

    if (showPermissionSheet) {
        PermissionsBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            featureTitle = R.string.feat_shut_up_title,
            permissions = listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_admin_panel_settings_24,
                    title = "Write Secure Settings",
                    description = "Required to toggle USB and Wireless Debugging settings",
                    isGranted = hasSecureSettings,
                    action = {
                        // Secure settings cannot be requested directly by activity intent.
                        // We show a toast instructions.
                        Toast.makeText(context, "Requires ADB or Root to grant WRITE_SECURE_SETTINGS", Toast.LENGTH_LONG).show()
                    }
                ),
                PermissionItem(
                    iconRes = R.drawable.rounded_settings_24,
                    title = "Write System Settings",
                    description = "Required to toggle standard settings",
                    isGranted = hasWriteSettings,
                    action = {
                        PermissionUtils.openWriteSettings(context)
                    }
                ),
                PermissionItem(
                    iconRes = R.drawable.rounded_data_usage_24,
                    title = "Usage Access",
                    description = "Required to detect when target apps are launched or exited",
                    isGranted = hasUsageStats,
                    action = {
                        com.sameerasw.essentials.utils.PermissionUtils.openUsageStatsSettings(context)
                    }
                ),
                PermissionItem(
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    title = "Post Notifications",
                    description = "Required to display background service status and auto-freeze countdowns",
                    isGranted = hasNotifications,
                    action = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Monitoring Service",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val onToggleShutUpService: (Boolean) -> Unit = { enabled ->
            HapticUtil.performVirtualKeyHaptic(view)
            if (enabled) {
                // Recheck permissions
                hasSecureSettings = PermissionUtils.canWriteSecureSettings(context)
                hasWriteSettings = PermissionUtils.canWriteSystemSettings(context)
                hasUsageStats = PermissionUtils.hasUsageStatsPermission(context)
                hasNotifications = PermissionUtils.isPostNotificationsEnabled(context)

                if (hasSecureSettings && hasWriteSettings && hasUsageStats && hasNotifications) {
                    viewModel.setShutUpServiceEnabled(true, context)
                } else {
                    showPermissionSheet = true
                }
            } else {
                viewModel.setShutUpServiceEnabled(false, context)
            }
        }

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            FeatureCard(
                title = "Enable Shut-Up! Service",
                description = "Runs in the background and applies security rules on target app launch",
                iconRes = R.drawable.rounded_security_24,
                isEnabled = viewModel.isShutUpServiceEnabled.value,
                showToggle = true,
                hasMoreSettings = false,
                onToggle = onToggleShutUpService,
                onClick = { onToggleShutUpService(!viewModel.isShutUpServiceEnabled.value) }
            )
        }

        Text(
            text = "App Configurations",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            ConfigSliderItem(
                title = stringResource(R.string.shut_up_restore_delay_title),
                value = viewModel.shutUpRestoreDelay.intValue.toFloat(),
                onValueChange = { viewModel.setShutUpRestoreDelay(it.toInt()) },
                valueRange = 2f..60f,
                increment = 1f,
                valueFormatter = { "${it.toInt()}s" },
                iconRes = R.drawable.rounded_timer_24,
                subtitle = stringResource(R.string.shut_up_restore_delay_desc)
            )

            RestoreModePicker(
                selectedMode = viewModel.shutUpRestoreMode.value,
                onModeSelected = { viewModel.setShutUpRestoreMode(it) }
            )

            FeatureCard(
                title = stringResource(R.string.shut_up_select_apps_title),
                description = stringResource(R.string.shut_up_select_apps_desc),
                iconRes = R.drawable.rounded_app_registration_24,
                isEnabled = true,
                showToggle = false,
                hasMoreSettings = false,
                onToggle = {},
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    isAppSelectionSheetOpen = true
                }
            )
        }

        val configs by viewModel.shutUpConfigs
        if (configs.isNotEmpty()) {
            RoundedCardContainer(
                modifier = Modifier,
                spacing = 2.dp,
                cornerRadius = 24.dp
            ) {
                configs.forEach { config ->
                    ShutUpAppItem(
                        config = config,
                        viewModel = viewModel,
                        onEditClick = { packageName, cfg ->
                            editingPackageName = packageName
                            editingConfig = cfg
                            isEditSheetOpen = true
                        }
                    )
                }
            }
        }

        if (isAppSelectionSheetOpen) {
            SingleAppSelectionSheet(
                onDismissRequest = { isAppSelectionSheetOpen = false },
                onAppSelected = { app ->
                    isAppSelectionSheetOpen = false
                    editingPackageName = app.packageName
                    editingConfig = configs.find { it.packageName == app.packageName }
                    isEditSheetOpen = true
                }
            )
        }

        if (isEditSheetOpen) {
            val isFrozen = remember(editingPackageName) {
                com.sameerasw.essentials.utils.FreezeManager.isAppFrozen(context, editingPackageName)
            }
            ShutUpPerAppSettingsSheet(
                onDismissRequest = { isEditSheetOpen = false },
                config = editingConfig ?: ShutUpAppConfig(packageName = editingPackageName),
                onConfigChanged = { updatedConfig ->
                    viewModel.updateShutUpConfig(updatedConfig)
                    editingConfig = updatedConfig
                },
                onCreateShortcut = { config ->
                    viewModel.createShutUpShortcut(context, config)
                },
                isFrozen = isFrozen,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun ShutUpAppItem(
    config: ShutUpAppConfig,
    viewModel: MainViewModel,
    onEditClick: (String, ShutUpAppConfig) -> Unit
) {
    val context = LocalContext.current
    val appName = remember(config.packageName) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(config.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            config.packageName
        }
    }

    val appIconPainter = remember(config.packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(config.packageName)
            androidx.compose.ui.graphics.painter.BitmapPainter(
                AppUtil.drawableToBitmap(drawable).asImageBitmap()
            )
        } catch (e: Exception) {
            null
        }
    }

    val enabledCount = config.settings.count { it.enabled }
    val descText = "${enabledCount} settings configured" +
            (if (config.autoArchive) " • Auto-Freeze" else "") +
            (if (config.attemptShizukuRestart) " • Shizuku restart" else "")

    FeatureCard(
        title = appName,
        description = descText,
        isEnabled = config.isEnabled,
        showToggle = true,
        onToggle = { isChecked ->
            viewModel.updateShutUpConfig(config.copy(isEnabled = isChecked))
        },
        onClick = {
            onEditClick(config.packageName, config)
        },
        iconPainter = appIconPainter,
        hasMoreSettings = true,
        additionalMenuItems = { onDismiss ->
            SegmentedDropdownMenuItem(
                text = { Text("Create Shortcut") },
                onClick = {
                    onDismiss()
                    viewModel.createShutUpShortcut(context, config)
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_link_24),
                        contentDescription = null
                    )
                }
            )
            SegmentedDropdownMenuItem(
                text = { Text(stringResource(R.string.action_remove)) },
                onClick = {
                    onDismiss()
                    viewModel.removeShutUpConfig(config.packageName)
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_delete_24),
                        contentDescription = null
                    )
                }
            )
        }
    )
}
