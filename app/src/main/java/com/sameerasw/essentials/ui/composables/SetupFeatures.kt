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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.domain.registry.PermissionRegistry
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.delay

private const val FEATURE_MAPS_POWER_SAVING = R.string.feat_maps_power_saving_title

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SetupFeatures(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    searchRequested: Boolean = false,
    onSearchHandled: () -> Unit = {},
    onHelpClick: () -> Unit = {}
) {
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
    val isShizukuAvailable by viewModel.isShizukuAvailable
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
    val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted
    val isNotificationLightingAccessibilityEnabled by viewModel.isNotificationLightingAccessibilityEnabled
    val isRootEnabled by viewModel.isRootEnabled
    val isRootPermissionGranted by viewModel.isRootPermissionGranted
    val isRootAvailable by viewModel.isRootAvailable
    viewModel.isButtonRemapEnabled.value
    viewModel.isDynamicNightLightEnabled.value

    viewModel.isScreenLockedSecurityEnabled.value
    val context = LocalContext.current

    fun buildMapsPowerSavingPermissionItems(): List<PermissionItem> {
        val items = mutableListOf<PermissionItem>()

        if (isRootEnabled) {
            if (!isRootPermissionGranted) {
                items.add(
                    PermissionItem(
                        iconRes = R.drawable.rounded_security_24,
                        title = R.string.perm_root_title,
                        description = R.string.perm_root_desc,
                        dependentFeatures = PermissionRegistry.getFeatures("ROOT"),
                        actionLabel = R.string.perm_action_grant,
                        action = { 
                            viewModel.isRootPermissionGranted.value = com.sameerasw.essentials.utils.RootUtils.isRootPermissionGranted()
                        },
                        isGranted = isRootPermissionGranted
                    )
                )
            }
        } else {
            if (!isShizukuAvailable) {
                items.add(
                    PermissionItem(
                        iconRes = R.drawable.rounded_adb_24,
                        title = R.string.perm_shizuku_title,
                        description = R.string.perm_shizuku_desc,
                        dependentFeatures = PermissionRegistry.getFeatures("SHIZUKU"),
                        actionLabel = R.string.perm_shizuku_install_action,
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
                        title = R.string.perm_shizuku_grant_title,
                        description = R.string.perm_shizuku_grant_desc,
                        dependentFeatures = PermissionRegistry.getFeatures("SHIZUKU"),
                        actionLabel = R.string.perm_action_grant,
                        action = { viewModel.requestShizukuPermission() },
                        isGranted = isShizukuPermissionGranted
                    )
                )
            }
        }

        if (!isNotificationListenerEnabled) {
            items.add(
                PermissionItem(
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    title = R.string.perm_notif_listener_title,
                    description = R.string.perm_notif_listener_desc_maps,
                    dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                    actionLabel = R.string.perm_action_grant,
                    action = { viewModel.requestNotificationListenerPermission(context) },
                    isGranted = isNotificationListenerEnabled
                )
            )
        }

        return items
    }

    var showSheet by remember { mutableStateOf(false) }
    var currentFeature by remember { mutableStateOf<Int?>(null) }

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
        isNotificationLightingAccessibilityEnabled,
        currentFeature
    ) {
        if (showSheet && currentFeature != null) {
            val missing = mutableListOf<PermissionItem>()
            when (currentFeature) {
                R.string.feat_screen_off_widget_title -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = R.string.perm_accessibility_title,
                                description = R.string.perm_accessibility_desc_common,
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                action = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                isGranted = isAccessibilityEnabled
                            )
                        )
                    }
                }
                R.string.feat_statusbar_icons_title -> {
                    if (!isWriteSecureSettingsEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = R.string.perm_write_secure_title,
                                description = R.string.perm_write_secure_desc_common,
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = R.string.perm_action_copy_adb,
                                action = {
                                    val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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
                    }
                }
                FEATURE_MAPS_POWER_SAVING -> {
                    missing.addAll(buildMapsPowerSavingPermissionItems())
                }
                R.string.feat_notification_lighting_title -> {
                    if (!isOverlayPermissionGranted) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_magnify_fullscreen_24,
                                title = R.string.perm_overlay_title,
                                description = R.string.perm_overlay_desc,
                                dependentFeatures = PermissionRegistry.getFeatures("DRAW_OVERLAYS"),
                                actionLabel = R.string.perm_action_grant,
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
                    if (!isNotificationLightingAccessibilityEnabled) {
                        missing.add(
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
                            )
                        )
                    }
                    if (!isNotificationListenerEnabled) {
                        missing.add(
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
                    }
                }
                R.string.feat_button_remap_title -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = R.string.perm_accessibility_title,
                                description = R.string.perm_accessibility_desc_remap,
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = R.string.perm_action_enable,
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
                R.string.feat_dynamic_night_light_title -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
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
                            )
                        )
                    }
                    if (!isWriteSecureSettingsEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = R.string.perm_write_secure_title,
                                description = R.string.perm_write_secure_desc_night_light,
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = R.string.perm_action_how_to,
                                action = {
                                    // instructions
                                },
                                isGranted = isWriteSecureSettingsEnabled
                            )
                        )
                    }
                }

                R.string.feat_screen_locked_security_title -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = R.string.perm_accessibility_title,
                                description = R.string.perm_accessibility_desc_common,
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = R.string.perm_action_enable,
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
                                title = R.string.perm_write_secure_title,
                                description = R.string.perm_write_secure_desc_common,
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = R.string.perm_action_copy_adb,
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
                    }
                }
                R.string.feat_app_lock_title -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = R.string.perm_accessibility_title,
                                description = R.string.perm_accessibility_desc_common,
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = R.string.perm_action_enable,
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
            R.string.feat_screen_off_widget_title -> listOf(
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
            R.string.feat_statusbar_icons_title -> listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_security_24,
                    title = R.string.perm_write_secure_title,
                    description = R.string.perm_write_secure_desc_common,
                    dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                    actionLabel = R.string.perm_action_copy_adb,
                    action = {
                        val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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
            FEATURE_MAPS_POWER_SAVING -> buildMapsPowerSavingPermissionItems()
            R.string.feat_notification_lighting_title -> listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_magnify_fullscreen_24,
                    title = R.string.perm_overlay_title,
                    description = R.string.perm_overlay_desc,
                    dependentFeatures = PermissionRegistry.getFeatures("DRAW_OVERLAYS"),
                    actionLabel = R.string.perm_action_grant,
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
            R.string.feat_button_remap_title -> listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    title = R.string.perm_accessibility_title,
                    description = R.string.perm_accessibility_desc_remap,
                    dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                    actionLabel = R.string.perm_action_enable,
                    action = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    },
                    isGranted = isAccessibilityEnabled
                )
            )
            R.string.feat_snooze_notifications_title -> listOf(
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
            R.string.feat_dynamic_night_light_title -> listOf(
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
                    actionLabel = R.string.perm_action_how_to,
                    action = { /* instructions */ },
                    isGranted = isWriteSecureSettingsEnabled
                )
            )
            R.string.feat_screen_locked_security_title -> listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    title = R.string.perm_accessibility_title,
                    description = R.string.perm_accessibility_desc_common,
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
                    description = R.string.perm_write_secure_desc_common,
                    dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                    actionLabel = R.string.perm_action_copy_adb,
                    action = {
                        val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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

            R.string.feat_app_lock_title -> listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    title = R.string.perm_accessibility_title,
                    description = R.string.perm_accessibility_desc_common,
                    dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                    actionLabel = R.string.perm_action_enable,
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

        if (showSheet && permissionItems.isNotEmpty() && currentFeature != null) {
            PermissionsBottomSheet(
                onDismissRequest = { showSheet = false },
                featureTitle = currentFeature!!,
                permissions = permissionItems,
                onHelpClick = {
                    showSheet = false
                    onHelpClick()
                }
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
                viewModel.onSearchQueryChanged(new, context)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_search_24),
                    contentDescription = stringResource(R.string.label_search_content_description),
                    modifier = Modifier.size(24.dp)
                )
            },
            placeholder = { if (!isFocused && viewModel.searchQuery.value.isEmpty()) Text(stringResource(R.string.search_placeholder)) },
            shape = MaterialTheme.shapes.extraExtraLarge,
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
                    text = stringResource(id = R.string.search_no_results, searchQuery),
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
                    text = stringResource(R.string.search_results_title),
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
                                        title = context.getString(R.string.biometric_title_settings_format, result.title),
                                        subtitle = context.getString(R.string.biometric_subtitle_access_settings),
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
                            isBeta = result.isBeta, // Added isBeta
                            descriptionOverride = if (result.parentFeature != null) "${result.parentFeature} > ${result.description}" else result.description
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
                        text = stringResource(id = category),
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
                                if (feature.category == R.string.cat_security && context is FragmentActivity) {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = context,
                                        title = context.getString(R.string.biometric_title_settings_format, context.getString(feature.title)),
                                        subtitle = if (enabled) context.getString(R.string.biometric_subtitle_enable_feature) else context.getString(R.string.biometric_subtitle_disable_feature),
                                        onSuccess = { feature.onToggle(viewModel, context, enabled) }
                                    )
                                } else {
                                    feature.onToggle(viewModel, context, enabled)
                                }
                            },
                            onClick = {
                                if (feature.category == R.string.cat_security && context is FragmentActivity) {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = context,
                                        title = context.getString(R.string.biometric_title_settings_format, context.getString(feature.title)),
                                        subtitle = context.getString(R.string.biometric_subtitle_access_settings),
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
                            description = feature.description,
                            isBeta = feature.isBeta
                        )
                    }
                }
            }
        }
    }
}
