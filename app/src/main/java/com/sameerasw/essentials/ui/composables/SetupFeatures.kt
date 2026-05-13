package com.sameerasw.essentials.ui.composables

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.domain.registry.PermissionRegistry
import com.sameerasw.essentials.ui.activities.YourAndroidActivity
import com.sameerasw.essentials.ui.components.FavoriteCarousel
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.utils.BiometricSecurityHelper
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FEATURE_MAPS_POWER_SAVING = R.string.feat_maps_power_saving_title

@Composable
fun SetupFeatures(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    searchRequested: Boolean = false,
    onSearchHandled: () -> Unit = {},
    onHelpClick: () -> Unit = {}
) {
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
    val isWriteSettingsEnabled by viewModel.isWriteSettingsEnabled
    val isShizukuAvailable by viewModel.isShizukuAvailable
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
    val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted
    val isNotificationLightingAccessibilityEnabled by viewModel.isNotificationLightingAccessibilityEnabled
    val isRootEnabled by viewModel.isRootEnabled
    val isRootPermissionGranted by viewModel.isRootPermissionGranted
    val isReadPhoneStateEnabled by viewModel.isReadPhoneStateEnabled
    viewModel.isButtonRemapEnabled.value
    viewModel.isDynamicNightLightEnabled.value

    viewModel.isScreenLockedSecurityEnabled.value
    val pinnedFeatureKeys by viewModel.pinnedFeatureKeys
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
                            viewModel.isRootPermissionGranted.value =
                                com.sameerasw.essentials.utils.RootUtils.isRootPermissionGranted()
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
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api".toUri()
                            )
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

    // Help Sheet State
    var showHelpSheet by remember { mutableStateOf(false) }
    var selectedHelpFeature by remember {
        mutableStateOf<com.sameerasw.essentials.domain.model.Feature?>(
            null
        )
    }

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
        isWriteSettingsEnabled,
        isShizukuAvailable,
        isShizukuPermissionGranted,
        isNotificationListenerEnabled,
        isOverlayPermissionGranted,
        isNotificationLightingAccessibilityEnabled,
        isReadPhoneStateEnabled,
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
                                    val adbCommand =
                                        "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("adb_command", adbCommand)
                                    clipboard.setPrimaryClip(clip)
                                },
                                secondaryActionLabel = R.string.perm_action_check,
                                secondaryAction = {
                                    viewModel.isWriteSecureSettingsEnabled.value =
                                        viewModel.canWriteSecureSettings(context)
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
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        "package:${context.packageName}".toUri()
                                    )
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

                R.string.feat_shut_up_title -> {
                    if (!isWriteSecureSettingsEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = R.string.perm_write_secure_title,
                                description = R.string.perm_write_secure_desc_common,
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = R.string.perm_action_grant,
                                action = { viewModel.requestWriteSecureSettingsPermission(context) },
                                isGranted = isWriteSecureSettingsEnabled
                            )
                        )
                    }
                    if (!isWriteSettingsEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_24,
                                title = R.string.perm_write_settings_title,
                                description = R.string.perm_write_settings_desc,
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SETTINGS"),
                                actionLabel = R.string.perm_action_grant,
                                action = { viewModel.requestWriteSettingsPermission(context) },
                                isGranted = isWriteSettingsEnabled
                            )
                        )
                    }
                    if (!viewModel.isUsageStatsPermissionGranted.value) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_app_registration_24,
                                title = R.string.perm_usage_stats_title,
                                description = R.string.perm_usage_stats_desc,
                                dependentFeatures = PermissionRegistry.getFeatures("USAGE_STATS"),
                                actionLabel = R.string.perm_action_grant,
                                action = { viewModel.requestUsageStatsPermission(context) },
                                isGranted = viewModel.isUsageStatsPermissionGranted.value
                            )
                        )
                    }
                }

                R.string.feat_screen_locked_security_title -> {
                    if (isRootEnabled) {
                        if (!isRootPermissionGranted) {
                            missing.add(
                                PermissionItem(
                                    iconRes = R.drawable.rounded_security_24,
                                    title = R.string.perm_root_title,
                                    description = R.string.perm_root_desc,
                                    dependentFeatures = listOf(R.string.feat_screen_locked_security_title),
                                    action = {
                                        viewModel.isRootPermissionGranted.value =
                                            com.sameerasw.essentials.utils.RootUtils.isRootPermissionGranted()
                                    },
                                    isGranted = isRootPermissionGranted
                                )
                            )
                        }
                    } else {
                        if (!isShizukuAvailable) {
                            missing.add(
                                PermissionItem(
                                    iconRes = R.drawable.rounded_adb_24,
                                    title = R.string.perm_shizuku_title,
                                    description = R.string.perm_shizuku_desc,
                                    dependentFeatures = listOf(R.string.feat_screen_locked_security_title),
                                    action = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api".toUri()
                                        )
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
                                    title = R.string.perm_shizuku_grant_title,
                                    description = R.string.perm_shizuku_grant_desc,
                                    dependentFeatures = listOf(R.string.feat_screen_locked_security_title),
                                    action = { viewModel.requestShizukuPermission() },
                                    isGranted = isShizukuPermissionGranted
                                )
                            )
                        }
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
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                },
                                isGranted = isAccessibilityEnabled
                            )
                        )
                    }
                }

                R.string.feat_call_vibrations_title -> {
                    if (!viewModel.isReadPhoneStateEnabled.value) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_mobile_24,
                                title = R.string.permission_read_phone_state_title,
                                description = R.string.permission_read_phone_state_desc_call_vibrations,
                                dependentFeatures = PermissionRegistry.getFeatures("READ_PHONE_STATE"),
                                actionLabel = R.string.perm_action_grant,
                                action = { viewModel.requestReadPhoneStatePermission(context as Activity) },
                                isGranted = isReadPhoneStateEnabled
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

                R.string.feat_essentials_on_display_title -> {
                    if (!isAccessibilityEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_settings_accessibility_24,
                                title = R.string.perm_accessibility_title,
                                description = R.string.perm_accessibility_desc_essentials_on_display,
                                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                                actionLabel = R.string.perm_action_enable,
                                action = {
                                    val intent =
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isAccessibilityEnabled
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
                        val adbCommand =
                            "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("adb_command", adbCommand)
                        clipboard.setPrimaryClip(clip)
                    },
                    secondaryActionLabel = R.string.perm_action_check,
                    secondaryAction = {
                        viewModel.isWriteSecureSettingsEnabled.value =
                            viewModel.canWriteSecureSettings(context)
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
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri()
                        )
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

            R.string.feat_screen_locked_security_title -> {
                val shellItems = mutableListOf<PermissionItem>()
                if (isRootEnabled) {
                    if (!isRootPermissionGranted) {
                        shellItems.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = R.string.perm_root_title,
                                description = R.string.perm_root_desc,
                                dependentFeatures = listOf(R.string.feat_screen_locked_security_title),
                                action = {
                                    viewModel.isRootPermissionGranted.value =
                                        com.sameerasw.essentials.utils.RootUtils.isRootPermissionGranted()
                                },
                                isGranted = isRootPermissionGranted
                            )
                        )
                    }
                } else {
                    if (!isShizukuAvailable) {
                        shellItems.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_adb_24,
                                title = R.string.perm_shizuku_title,
                                description = R.string.perm_shizuku_desc,
                                dependentFeatures = listOf(R.string.feat_screen_locked_security_title),
                                actionLabel = R.string.perm_shizuku_install_action,
                                action = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api".toUri()
                                    )
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isShizukuAvailable
                            )
                        )
                    } else if (!isShizukuPermissionGranted) {
                        shellItems.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_adb_24,
                                title = R.string.perm_shizuku_grant_title,
                                description = R.string.perm_shizuku_grant_desc,
                                dependentFeatures = listOf(R.string.feat_screen_locked_security_title),
                                actionLabel = R.string.perm_action_grant,
                                action = { viewModel.requestShizukuPermission() },
                                isGranted = isShizukuPermissionGranted
                            )
                        )
                    }
                }
                shellItems
            }

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

            R.string.feat_call_vibrations_title -> listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_mobile_24,
                    title = R.string.permission_read_phone_state_title,
                    description = R.string.permission_read_phone_state_desc_call_vibrations,
                    dependentFeatures = PermissionRegistry.getFeatures("READ_PHONE_STATE"),
                    actionLabel = R.string.perm_action_grant,
                    action = { viewModel.requestReadPhoneStatePermission(context as Activity) },
                    isGranted = isReadPhoneStateEnabled
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

            R.string.feat_essentials_on_display_title -> listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    title = R.string.perm_accessibility_title,
                    description = R.string.perm_accessibility_desc_essentials_on_display,
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
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    title = R.string.perm_notif_listener_title,
                    description = R.string.perm_notif_listener_desc_lighting,
                    dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                    actionLabel = R.string.perm_action_grant,
                    action = { viewModel.requestNotificationListenerPermission(context) },
                    isGranted = isNotificationListenerEnabled
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

    if (showHelpSheet && selectedHelpFeature != null) {
        com.sameerasw.essentials.ui.components.sheets.FeatureHelpBottomSheet(
            onDismissRequest = {
                showHelpSheet = false
                selectedHelpFeature = null
            },
            feature = selectedHelpFeature!!
        )
    }

    val scrollState = rememberScrollState()
    val view = LocalView.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val allFeatures = FeatureRegistry.ALL_FEATURES

    LaunchedEffect(searchRequested) {
        if (searchRequested) {
            scrollState.animateScrollTo(0)
            delay(100)
            focusRequester.requestFocus()
            onSearchHandled()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var shouldResetRefreshing by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (shouldResetRefreshing) {
                    scope.launch {
                        delay(200)
                        isRefreshing = false
                        shouldResetRefreshing = false
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            HapticUtil.performUIHaptic(view)
            context.startActivity(Intent(context, YourAndroidActivity::class.java))
            shouldResetRefreshing = true
        }
    }

    var lastHapticBucket by remember { mutableStateOf(0) }
    LaunchedEffect(pullRefreshState.distanceFraction) {
        val fraction = pullRefreshState.distanceFraction
        val currentBucket = (fraction * 10).toInt()

        if (fraction >= 1f && lastHapticBucket < 10) {
            HapticUtil.performUIHaptic(view)
            lastHapticBucket = 10
        } else if (fraction < 1f && currentBucket != lastHapticBucket) {
            if (currentBucket > lastHapticBucket) {
                HapticUtil.performSliderHaptic(view)
            }
            lastHapticBucket = currentBucket
        }

        if (fraction == 0f) {
            lastHapticBucket = 0
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true },
        state = pullRefreshState,
        indicator = { },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            val deviceInfo = remember { DeviceUtils.getDeviceInfo(context) }
            val displayFraction = if (isRefreshing) 1f else pullRefreshState.distanceFraction
            val thresholdPassed = displayFraction >= 1f
            val statusBarPadding = contentPadding.calculateTopPadding()

            val cardExpansion by androidx.compose.animation.core.animateDpAsState(
                targetValue = 120.dp * displayFraction.coerceIn(0f, 1f),
                animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                label = "cardExpansion"
            )

            val containerColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (thresholdPassed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
                label = "containerColor"
            )

            val contentColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (thresholdPassed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
                label = "contentColor"
            )

            val borderColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (thresholdPassed) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
                label = "borderColor"
            )

            val chevronAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (thresholdPassed) 0f else 1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
                label = "chevronAlpha"
            )

            val chevronWidth by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (thresholdPassed) 0.dp else 24.dp + 8.dp, // size + spacer
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
                label = "chevronWidth"
            )

            val fontWeight by androidx.compose.animation.core.animateIntAsState(
                targetValue = if (thresholdPassed) 700 else 500,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
                label = "fontWeight"
            )

            val textScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (thresholdPassed) 1.5f else 1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
                label = "textScale"
            )

            // My Android
            OutlinedCard(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    context.startActivity(Intent(context, YourAndroidActivity::class.java))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 0.dp, bottom = 0.dp)
                    .height(64.dp + statusBarPadding + cardExpansion),
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = containerColor,
                    contentColor = if (thresholdPassed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(statusBarPadding))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(chevronWidth)
                                    .graphicsLayer { alpha = chevronAlpha }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                rotationZ =
                                                    (displayFraction * 180f).coerceIn(0f, 180f)
                                            },
                                        tint = contentColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                            Text(
                                text = deviceInfo.deviceName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight(fontWeight)
                                ),
                                modifier = Modifier.graphicsLayer {
                                    scaleX = textScale
                                    scaleY = textScale
                                },
                                color = if (thresholdPassed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = viewModel.searchQuery.value,
                onValueChange = { new ->
                    viewModel.onSearchQueryChanged(new, context)
                },
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_search_24),
                        contentDescription = stringResource(R.string.label_search_content_description),
                        modifier = Modifier.size(24.dp)
                    )
                },
                placeholder = {
                    if (!isFocused && viewModel.searchQuery.value.isEmpty())
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                },
                shape = MaterialTheme.shapes.extraExtraLarge,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceBright
                )
            )

            if (pinnedFeatureKeys.isNotEmpty() && viewModel.searchQuery.value.isEmpty()) {
                FavoriteCarousel(
                    pinnedKeys = pinnedFeatureKeys,
                    onFeatureClick = { feature ->
                        BiometricSecurityHelper.runWithAuth(
                            activity = context as FragmentActivity,
                            feature = feature,
                            action = {
                                feature.onClick(context, viewModel)
                            }
                        )
                    },
                    onFeatureLongClick = { feature ->
                        viewModel.togglePinFeature(feature.id)
                    },
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                )
            }

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
                                    val feature = allFeatures.find { it.id == result.featureKey }
                                    if (feature != null) {
                                        BiometricSecurityHelper.runWithAuth(
                                            activity = context as FragmentActivity,
                                            feature = feature,
                                            action = {
                                                context.startActivity(
                                                    Intent(
                                                        context,
                                                        FeatureSettingsActivity::class.java
                                                    ).apply {
                                                        putExtra("feature", result.featureKey)
                                                        result.targetSettingHighlightKey?.let {
                                                            putExtra("highlight_setting", it)
                                                        }
                                                    }
                                                )
                                            }
                                        )
                                    } else {
                                        context.startActivity(
                                            Intent(
                                                context,
                                                FeatureSettingsActivity::class.java
                                            ).apply {
                                                putExtra("feature", result.featureKey)
                                                result.targetSettingHighlightKey?.let {
                                                    putExtra("highlight_setting", it)
                                                }
                                            }
                                        )
                                    }
                                },
                                iconRes = result.icon ?: R.drawable.rounded_settings_24,
                                modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
                                showToggle = false,
                                hasMoreSettings = true,
                                isBeta = result.isBeta,
                                descriptionOverride = if (result.parentFeature != null) "${result.parentFeature} > ${result.description}" else result.description,
                                isPinned = pinnedFeatureKeys.contains(result.featureKey),
                                onPinToggle = {
                                    viewModel.togglePinFeature(result.featureKey)
                                },
                                onHelpClick = if (allFeatures.find { it.id == result.featureKey }?.aboutDescription != null) {
                                    {
                                        val feature =
                                            allFeatures.find { it.id == result.featureKey }
                                        if (feature != null) {
                                            selectedHelpFeature = feature
                                            showHelpSheet = true
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                }
            } else {
                // Render Top Level Features (No Categories)
                val topLevelFeatures =
                    allFeatures.filter { it.parentFeatureId == null && it.isVisibleInMain }

                if (topLevelFeatures.isNotEmpty()) {
                    RoundedCardContainer(
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        topLevelFeatures.forEachIndexed { index, feature ->
                            FeatureCard(
                                title = feature.title,
                                isEnabled = feature.isEnabled(viewModel),
                                onToggle = { enabled ->
                                    BiometricSecurityHelper.runWithAuth(
                                        activity = context as FragmentActivity,
                                        feature = feature,
                                        isToggle = true,
                                        action = {
                                            feature.onToggle(viewModel, context, enabled)
                                        }
                                    )
                                },
                                onClick = {
                                    BiometricSecurityHelper.runWithAuth(
                                        activity = context as FragmentActivity,
                                        feature = feature,
                                        action = {
                                            feature.onClick(context, viewModel)
                                        }
                                    )
                                },
                                iconRes = feature.iconRes,
                                modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
                                isToggleEnabled = feature.isToggleEnabled(viewModel, context),
                                showToggle = feature.showToggle,
                                hasMoreSettings = feature.hasMoreSettings,
                                onDisabledToggleClick = {
                                    if (feature.id == "Screen locked security") {
                                        feature.onClick(context, viewModel)
                                    } else {
                                        currentFeature = feature.title
                                        showSheet = true
                                    }
                                },
                                description = feature.description,
                                isBeta = feature.isBeta,
                                isPinned = pinnedFeatureKeys.contains(feature.id),
                                onPinToggle = {
                                    viewModel.togglePinFeature(feature.id)
                                },
                                onHelpClick = if (feature.aboutDescription != null) {
                                    {
                                        selectedHelpFeature = feature
                                        showHelpSheet = true
                                    }
                                } else null
                            )
                        }
                    }

                } else {
                    val topLevelFeatures =
                        allFeatures.filter { it.parentFeatureId == null && it.isVisibleInMain }

                    if (topLevelFeatures.isNotEmpty()) {
                        RoundedCardContainer(
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            topLevelFeatures.forEachIndexed { index, feature ->
                                FeatureCard(
                                    title = feature.title,
                                    isEnabled = feature.isEnabled(viewModel),
                                    onToggle = { enabled ->
                                        BiometricSecurityHelper.runWithAuth(
                                            activity = context as FragmentActivity,
                                            feature = feature,
                                            isToggle = true,
                                            action = {
                                                feature.onToggle(viewModel, context, enabled)
                                            }
                                        )
                                    },
                                    onClick = {
                                        BiometricSecurityHelper.runWithAuth(
                                            activity = context as FragmentActivity,
                                            feature = feature,
                                            action = {
                                                feature.onClick(context, viewModel)
                                            }
                                        )
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
                                    isBeta = feature.isBeta,
                                    isPinned = pinnedFeatureKeys.contains(feature.id),
                                    onPinToggle = {
                                        viewModel.togglePinFeature(feature.id)
                                    },
                                    onHelpClick = if (feature.aboutDescription != null) {
                                        {
                                            selectedHelpFeature = feature
                                            showHelpSheet = true
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
            }
        }
    }
}