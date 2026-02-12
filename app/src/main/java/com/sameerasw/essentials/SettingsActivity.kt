package com.sameerasw.essentials

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.essentials.ui.components.pickers.HapticFeedbackPicker
import com.sameerasw.essentials.ui.components.pickers.DefaultTabPicker
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityCompat
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.cards.PermissionCard
import com.sameerasw.essentials.ui.components.dialogs.AboutSection
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.ui.components.sheets.UpdateBottomSheet
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.sameerasw.essentials.domain.DIYTabs
import com.sameerasw.essentials.domain.registry.PermissionRegistry
import com.sameerasw.essentials.ui.components.sheets.InstructionsBottomSheet
import com.sameerasw.essentials.utils.PermissionUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import rikka.shizuku.Shizuku

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val shizukuPermissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.check(this)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        // Register Shizuku permission listener
        Shizuku.addRequestPermissionResultListener(shizukuPermissionResultListener)
        setContent {
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val context = LocalContext.current
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

                var showBugReportSheet by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.check(context)
                }

                if (showBugReportSheet) {
                    com.sameerasw.essentials.ui.components.sheets.BugReportBottomSheet(
                        viewModel = viewModel,
                        onDismissRequest = { showBugReportSheet = false }
                    )
                }

                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = "Settings",
                            hasBack = true,
                            hasSearch = false,
                            onBackClick = { finish() },
                            scrollBehavior = scrollBehavior,
                            actions = {
                                androidx.compose.material3.IconButton(
                                    onClick = { showBugReportSheet = true },
                                    colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright
                                    ),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_bug_report_24),
                                        contentDescription = "Report Bug",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    SettingsContent(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.check(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionResultListener)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode in 1001..1006) {
            viewModel.check(this)
        }
    }
}

@Composable
fun SettingsContent(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
    val isPostNotificationsEnabled by viewModel.isPostNotificationsEnabled
    val isReadPhoneStateEnabled by viewModel.isReadPhoneStateEnabled
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
    val isShizukuAvailable by viewModel.isShizukuAvailable
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted
    val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled
    val isDefaultBrowserSet by viewModel.isDefaultBrowserSet
    val isWriteSettingsEnabled by viewModel.isWriteSettingsEnabled
    val isNotificationPolicyAccessGranted by viewModel.isNotificationPolicyAccessGranted
    val isLocationPermissionGranted by viewModel.isLocationPermissionGranted
    val isBackgroundLocationPermissionGranted by viewModel.isBackgroundLocationPermissionGranted
    val isDeviceAdminEnabled by viewModel.isDeviceAdminEnabled
    val isCalendarPermissionGranted by viewModel.isCalendarPermissionGranted
    val isUsageStatsPermissionGranted by viewModel.isUsageStatsPermissionGranted
    val context = LocalContext.current
    val isAppHapticsEnabled = remember { mutableStateOf(HapticUtil.loadAppHapticsEnabled(context)) }
    var isPermissionsExpanded by remember { mutableStateOf(false) }
    var showUpdateSheet by remember { mutableStateOf(false) }
    val updateInfo by viewModel.updateInfo
    val isAutoUpdateEnabled by viewModel.isAutoUpdateEnabled
    val isUpdateNotificationEnabled by viewModel.isUpdateNotificationEnabled
    val isPreReleaseCheckEnabled by viewModel.isPreReleaseCheckEnabled
    val isRootEnabled by viewModel.isRootEnabled
    val isRootAvailable by viewModel.isRootAvailable
    val isRootPermissionGranted by viewModel.isRootPermissionGranted
    val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled
    var showInstructionsSheet by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    viewModel.exportConfigs(context, outputStream)
                    Toast.makeText(context, "Config exported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export config", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    if (viewModel.importConfigs(context, inputStream)) {
                        Toast.makeText(context, "Config imported successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to import config", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import config", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    if (showUpdateSheet) {
        UpdateBottomSheet(
            updateInfo = updateInfo,
            isChecking = viewModel.isCheckingUpdate.value,
            onDismissRequest = { showUpdateSheet = false }
        )
    }

    if (showInstructionsSheet) {
        InstructionsBottomSheet(
            onDismissRequest = { showInstructionsSheet = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // App Settings Section
        androidx.compose.material3.Text(
            text = "App Settings",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer {
            com.sameerasw.essentials.ui.components.cards.IconToggleItem(
                iconRes = R.drawable.rounded_mobile_vibrate_24,
                title = "Haptic Feedback",
                isChecked = isAppHapticsEnabled.value,
                onCheckedChange = { isChecked ->
                    isAppHapticsEnabled.value = isChecked
                    HapticUtil.saveAppHapticsEnabled(context, isChecked)
                }
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_invert_colors_24,
                title = stringResource(R.string.setting_pitch_black_theme_title),
                description = stringResource(R.string.setting_pitch_black_theme_desc),
                isChecked = viewModel.isPitchBlackThemeEnabled.value,
                onCheckedChange = { viewModel.setPitchBlackThemeEnabled(it, context) }
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_numbers_24,
                title = stringResource(R.string.setting_use_root_title),
                description = stringResource(R.string.setting_use_root_desc),
                isChecked = viewModel.isRootEnabled.value,
                onCheckedChange = { viewModel.setRootEnabled(it, context) }
            )
        }

        Text(
            text = "Default tab",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val defaultTab by viewModel.defaultTab
        RoundedCardContainer {
            val availableTabs = remember { DIYTabs.entries }
            DefaultTabPicker(
                selectedTab = defaultTab,
                onTabSelected = { viewModel.setDefaultTab(it, context) },
                options = availableTabs
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permissions Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isPermissionsExpanded = !isPermissionsExpanded }
                .padding(start = 16.dp, top = 16.dp, bottom = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = painterResource(id = if (isPermissionsExpanded) R.drawable.rounded_keyboard_arrow_up_24 else R.drawable.rounded_keyboard_arrow_down_24),
                contentDescription = if (isPermissionsExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = isPermissionsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            RoundedCardContainer {
                PermissionCard(
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    title = "Accessibility",
                    dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                    actionLabel = if (isAccessibilityEnabled) "Granted" else "Grant Permission",
                    isGranted = isAccessibilityEnabled,
                    onActionClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_security_24,
                    title = "Write Secure Settings",
                    dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                    actionLabel = if (isWriteSecureSettingsEnabled) "Granted" else "Copy ADB Command",
                    isGranted = isWriteSecureSettingsEnabled,
                    onActionClick = {
                        val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("adb_command", adbCommand)
                        clipboard.setPrimaryClip(clip)
                    },
                    secondaryActionLabel = "Check",
                    onSecondaryActionClick = {
                        viewModel.check(context)
                    },
                )

                if (isRootEnabled) {
                    PermissionCard(
                        iconRes = R.drawable.rounded_numbers_24,
                        title = stringResource(R.string.perm_root_title),
                        dependentFeatures = PermissionRegistry.getFeatures("ROOT"),
                        actionLabel = if (isRootPermissionGranted) "Granted" else "Grant Access",
                        isGranted = isRootPermissionGranted,
                        onActionClick = {
                            viewModel.check(context)
                        }
                    )
                } else if (isShizukuAvailable) {
                    PermissionCard(
                        iconRes = R.drawable.rounded_adb_24,
                        title = "Shizuku",
                        dependentFeatures = PermissionRegistry.getFeatures("SHIZUKU"),
                        actionLabel = if (isShizukuPermissionGranted) "Granted" else "Request Permission",
                        isGranted = isShizukuPermissionGranted,
                        onActionClick = {
                            viewModel.requestShizukuPermission()
                        },
                        secondaryActionLabel = if (isShizukuPermissionGranted && !isWriteSecureSettingsEnabled) "Auto-Grant" else null,
                        onSecondaryActionClick = if (isShizukuPermissionGranted && !isWriteSecureSettingsEnabled) {
                            {
                                viewModel.grantWriteSecureSettingsWithShizuku(context)
                            }
                        } else null,
                    )
                }

                PermissionCard(
                    iconRes = R.drawable.rounded_android_cell_dual_4_bar_24,
                    title = "Read Phone State",
                    dependentFeatures = PermissionRegistry.getFeatures("READ_PHONE_STATE"),
                    actionLabel = if (isReadPhoneStateEnabled) "Granted" else "Grant Permission",
                    isGranted = isReadPhoneStateEnabled,
                    onActionClick = {
                        viewModel.requestReadPhoneStatePermission(context as ComponentActivity)
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    title = "Post Notifications",
                    dependentFeatures = PermissionRegistry.getFeatures("POST_NOTIFICATIONS"),
                    actionLabel = if (isPostNotificationsEnabled) "Granted" else "Grant Permission",
                    isGranted = isPostNotificationsEnabled,
                    onActionClick = {
                        // Request permission
                        ActivityCompat.requestPermissions(
                            context as ComponentActivity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            1002
                        )
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_magnify_fullscreen_24,
                    title = "Draw Overlays",
                    dependentFeatures = PermissionRegistry.getFeatures("DRAW_OVER_OTHER_APPS"),
                    actionLabel = if (isOverlayPermissionGranted) "Granted" else "Grant Permission",
                    isGranted = isOverlayPermissionGranted,
                    onActionClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_notification_settings_24,
                    title = "Notification Listener",
                    dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                    actionLabel = if (isNotificationListenerEnabled) "Granted" else "Enable listener",
                    isGranted = isNotificationListenerEnabled,
                    onActionClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_security_24,
                    title = stringResource(R.string.perm_write_settings_title),
                    dependentFeatures = PermissionRegistry.getFeatures("WRITE_SETTINGS"),
                    actionLabel = if (isWriteSettingsEnabled) "Granted" else "Grant Permission",
                    isGranted = isWriteSettingsEnabled,
                    onActionClick = {
                        PermissionUtils.openWriteSettings(context)
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_volume_up_24,
                    title = stringResource(R.string.perm_notif_policy_title),
                    dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_POLICY"),
                    actionLabel = if (isNotificationPolicyAccessGranted) "Granted" else "Grant Permission",
                    isGranted = isNotificationPolicyAccessGranted,
                    onActionClick = {
                        PermissionUtils.openNotificationPolicySettings(context)
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_open_in_browser_24,
                    title = stringResource(R.string.perm_default_browser_title),
                    dependentFeatures = PermissionRegistry.getFeatures("DEFAULT_BROWSER"),
                    actionLabel = if (isDefaultBrowserSet) "Granted" else "Set as Default",
                    isGranted = isDefaultBrowserSet,
                    onActionClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback for older Android versions
                            val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(settingsIntent)
                        }
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_settings_motion_mode_24,
                    title = stringResource(R.string.perm_write_settings_title),
                    dependentFeatures = PermissionRegistry.getFeatures("WRITE_SETTINGS"),
                    actionLabel = if (isWriteSettingsEnabled) "Granted" else "Grant Permission",
                    isGranted = isWriteSettingsEnabled,
                    onActionClick = {
                        PermissionUtils.openWriteSettings(context)
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_notifications_off_24,
                    title = stringResource(R.string.perm_notif_policy_title),
                    dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_POLICY"),
                    actionLabel = if (isNotificationPolicyAccessGranted) "Granted" else "Grant Permission",
                    isGranted = isNotificationPolicyAccessGranted,
                    onActionClick = {
                        PermissionUtils.openNotificationPolicySettings(context)
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_data_usage_24,
                    title = stringResource(R.string.perm_usage_stats_title),
                    dependentFeatures = PermissionRegistry.getFeatures("USAGE_STATS"),
                    actionLabel = if (isUsageStatsPermissionGranted) "Granted" else "Grant Permission",
                    isGranted = isUsageStatsPermissionGranted,
                    onActionClick = {
                        PermissionUtils.openUsageStatsSettings(context)
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_location_on_24,
                    title = "Location Access",
                    dependentFeatures = PermissionRegistry.getFeatures("LOCATION"),
                    actionLabel = if (isLocationPermissionGranted) "Granted" else "Grant Permission",
                    isGranted = isLocationPermissionGranted,
                    onActionClick = {
                        viewModel.requestLocationPermission(context as ComponentActivity)
                    },
                )

                if (isLocationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    PermissionCard(
                        iconRes = R.drawable.rounded_location_on_24,
                        title = "Background Location",
                        dependentFeatures = PermissionRegistry.getFeatures("BACKGROUND_LOCATION"),
                        actionLabel = if (isBackgroundLocationPermissionGranted) "Granted" else "Grant Permission",
                        isGranted = isBackgroundLocationPermissionGranted,
                        onActionClick = {
                            viewModel.requestBackgroundLocationPermission(context as ComponentActivity)
                        },
                    )
                }

                PermissionCard(
                    iconRes = R.drawable.rounded_admin_panel_settings_24,
                    title = "Device Admin",
                    dependentFeatures = PermissionRegistry.getFeatures("DEVICE_ADMIN"),
                    actionLabel = if (isDeviceAdminEnabled) "Granted" else "Enable Admin",
                    isGranted = isDeviceAdminEnabled,
                    onActionClick = {
                        viewModel.requestDeviceAdmin(context)
                    },
                )

                PermissionCard(
                    iconRes = R.drawable.rounded_calendar_today_24,
                    title = "Calendar",
                    dependentFeatures = PermissionRegistry.getFeatures("READ_CALENDAR"),
                    actionLabel = if (isCalendarPermissionGranted) "Granted" else "Grant Permission",
                    isGranted = isCalendarPermissionGranted,
                    onActionClick = {
                        viewModel.requestCalendarPermission(context as ComponentActivity)
                    },
                )
            }
        }

        // Updates Section
        Text(
            text = "Updates",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_check_24,
                title = "Auto check for updates",
                description = "Check for updates at app launch",
                isChecked = isAutoUpdateEnabled,
                onCheckedChange = { viewModel.setAutoUpdateEnabled(it, context) }
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_experiment_24,
                title = context.getString(R.string.check_pre_releases_label),
                description = context.getString(R.string.check_pre_releases_desc),
                isChecked = isPreReleaseCheckEnabled,
                onCheckedChange = { viewModel.setPreReleaseCheckEnabled(it, context) }
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = "Notify for new updates",
                description = "Show a notification when an update is found",
                isChecked = isUpdateNotificationEnabled,
                onCheckedChange = { viewModel.setUpdateNotificationEnabled(it, context) }
            )
        }

        // Check for updates button
        val view = LocalView.current
        Button(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                viewModel.checkForUpdates(context, manual = true)
                showUpdateSheet = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_mobile_arrow_down_24),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Check for updates", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        RoundedCardContainer {
            AboutSection(
                onAvatarLongClick = {
                    val newState = !isDeveloperModeEnabled
                    viewModel.setDeveloperModeEnabled(newState, context)
                    Toast.makeText(context, if (newState) "Developer options enabled" else "Developer options disabled", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (isDeveloperModeEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            // Updates Section
            Text(
                text = "Developer Options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RoundedCardContainer {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                exportLauncher.launch("essentials_config_$timeStamp.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export Config")
                        }
                        Button(
                            onClick = { 
                                importLauncher.launch(arrayOf("application/json"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import Config")
                        }
                    }

            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
