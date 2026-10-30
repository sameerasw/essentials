/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: SetupFeatures.kt
 * Description: UI layout element for SetupFeatures.kt.
 */

package com.sameerasw.essentials.ui.composables

import com.sameerasw.essentials.ui.navigation.LocalListPaneWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import com.sameerasw.essentials.ui.components.FavoriteGrid
import com.sameerasw.essentials.BuildConfig
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sameerasw.essentials.ui.components.rememberPullTitleHeaderState
import com.sameerasw.essentials.ui.components.PullTitleHeader
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.domain.registry.PermissionRegistry
import com.sameerasw.essentials.domain.registry.QSTileRegistry
import com.sameerasw.essentials.ui.activities.PixelSearchbarSettingsActivity
import com.sameerasw.essentials.ui.activities.WallpaperActivity
import com.sameerasw.essentials.ui.activities.YourAndroidActivity
import com.sameerasw.essentials.ui.components.FavoriteCarousel
import com.sameerasw.essentials.ui.components.buttons.ListExpandToggleButton
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PermissionItem
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.ReorderFavoritesBottomSheet
import com.sameerasw.essentials.ui.features.freeze.FrozenAppsSearchResultCard
import com.sameerasw.essentials.ui.features.permissions.PermissionsSearchResultCard
import com.sameerasw.essentials.ui.features.tiles.QSTilesSearchResultCard
import com.sameerasw.essentials.utils.FreezeManager
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.ui.modifiers.scrollMotionBlur
import com.sameerasw.essentials.utils.BiometricSecurityHelper
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun SetupFeatures(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    isSearchActive: Boolean = false,
    onHelpClick: () -> Unit = {},
    onNavigateToTab: ((com.sameerasw.essentials.domain.DIYTabs) -> Unit)? = null,
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

    var showSheet by remember { mutableStateOf(false) }
    var currentFeature by remember { mutableStateOf<Int?>(null) }
    var showReorderFavoritesSheet by remember { mutableStateOf(false) }

    // Help Sheet State
    var showHelpSheet by remember { mutableStateOf(false) }
    var selectedHelpFeature by remember {
        mutableStateOf<com.sameerasw.essentials.domain.model.Feature?>(
            null,
        )
    }

    val watchViewModel: com.sameerasw.essentials.viewmodels.WatchViewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel()
    val isWearUpdateRequired = watchViewModel.isWearUpdateRequired.value

    LaunchedEffect(Unit) {
        watchViewModel.check(context)
        viewModel.refreshFreezePickedApps(context, silent = true)
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
        isRootEnabled,
        isRootPermissionGranted,
        isNotificationListenerEnabled,
        isOverlayPermissionGranted,
        isNotificationLightingAccessibilityEnabled,
        isReadPhoneStateEnabled,
        currentFeature,
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
                                    PermissionUtils.openAccessibilitySettings(context)
                                },
                                isGranted = isAccessibilityEnabled,
                            ),
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
                                isGranted = isWriteSecureSettingsEnabled,
                            ),
                        )
                    }
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
                                    val intent =
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            "package:${context.packageName}".toUri(),
                                        )
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                isGranted = isOverlayPermissionGranted,
                            ),
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
                                    PermissionUtils.openAccessibilitySettings(context)
                                },
                                isGranted = isNotificationLightingAccessibilityEnabled,
                            ),
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
                                isGranted = isNotificationListenerEnabled,
                            ),
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
                                    PermissionUtils.openAccessibilitySettings(context)
                                },
                                isGranted = isAccessibilityEnabled,
                            ),
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
                                    PermissionUtils.openAccessibilitySettings(context)
                                },
                                isGranted = isAccessibilityEnabled,
                            ),
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
                                isGranted = isWriteSecureSettingsEnabled,
                            ),
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
                                isGranted = isWriteSecureSettingsEnabled,
                            ),
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
                                isGranted = isWriteSettingsEnabled,
                            ),
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
                                isGranted = viewModel.isUsageStatsPermissionGranted.value,
                            ),
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
                                            com.sameerasw.essentials.utils.RootUtils
                                                .isRootPermissionGranted()
                                    },
                                    isGranted = isRootPermissionGranted,
                                ),
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
                                        val intent =
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                "https://github.com/rushiranpise/Shizuku-Next".toUri(),
                                            )
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    },
                                    secondaryActionLabel = R.string.perm_porter_install_action,
                                    secondaryAction = {
                                        val intent =
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                "https://github.com/d4rken-org/porter".toUri(),
                                            )
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    },
                                    isGranted = isShizukuAvailable,
                                ),
                            )
                        } else if (!isShizukuPermissionGranted) {
                            missing.add(
                                PermissionItem(
                                    iconRes = R.drawable.rounded_adb_24,
                                    title = R.string.perm_shizuku_grant_title,
                                    description = R.string.perm_shizuku_grant_desc,
                                    dependentFeatures = listOf(R.string.feat_screen_locked_security_title),
                                    action = { viewModel.requestShizukuPermission() },
                                    isGranted = isShizukuPermissionGranted,
                                ),
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
                                    PermissionUtils.openAccessibilitySettings(context)
                                },
                                isGranted = isAccessibilityEnabled,
                            ),
                        )
                    }
                }

                R.string.feat_smart_pixels_title -> {
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
                                isGranted = isAccessibilityEnabled,
                            ),
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
                                isGranted = isReadPhoneStateEnabled,
                            ),
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
                                isGranted = isNotificationListenerEnabled,
                            ),
                        )
                    }
                }

                R.string.flashlight_pulse_title -> {
                    if (!isNotificationListenerEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_notifications_unread_24,
                                title = R.string.perm_notif_listener_title,
                                description = R.string.perm_notif_listener_desc_lighting,
                                dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                                actionLabel = R.string.perm_action_grant,
                                action = { viewModel.requestNotificationListenerPermission(context) },
                                isGranted = isNotificationListenerEnabled,
                            ),
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
                                    PermissionUtils.openAccessibilitySettings(context)
                                },
                                isGranted = isAccessibilityEnabled,
                            ),
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
                                isGranted = isNotificationListenerEnabled,
                            ),
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
        val permissionItems =
            when (currentFeature) {
                R.string.feat_screen_off_widget_title ->
                    listOf(
                        PermissionItem(
                            iconRes = R.drawable.rounded_settings_accessibility_24,
                            title = R.string.perm_accessibility_title,
                            description = R.string.perm_accessibility_desc_common,
                            dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                            actionLabel = R.string.perm_action_grant,
                            action = {
                                PermissionUtils.openAccessibilitySettings(context)
                            },
                            isGranted = isAccessibilityEnabled,
                        ),
                    )

                R.string.feat_statusbar_icons_title ->
                    listOf(
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
                            isGranted = isWriteSecureSettingsEnabled,
                        ),
                    )

                R.string.feat_notification_lighting_title ->
                    listOf(
                        PermissionItem(
                            iconRes = R.drawable.rounded_magnify_fullscreen_24,
                            title = R.string.perm_overlay_title,
                            description = R.string.perm_overlay_desc,
                            dependentFeatures = PermissionRegistry.getFeatures("DRAW_OVERLAYS"),
                            actionLabel = R.string.perm_action_grant,
                            action = {
                                val intent =
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        "package:${context.packageName}".toUri(),
                                    )
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            },
                            isGranted = isOverlayPermissionGranted,
                        ),
                        PermissionItem(
                            iconRes = R.drawable.rounded_settings_accessibility_24,
                            title = R.string.perm_accessibility_title,
                            description = R.string.perm_accessibility_desc_lighting,
                            dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                            actionLabel = R.string.perm_action_enable,
                            action = {
                                PermissionUtils.openAccessibilitySettings(context)
                            },
                            isGranted = isNotificationLightingAccessibilityEnabled,
                        ),
                        PermissionItem(
                            iconRes = R.drawable.rounded_notifications_unread_24,
                            title = R.string.perm_notif_listener_title,
                            description = R.string.perm_notif_listener_desc_lighting,
                            dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                            actionLabel = R.string.perm_action_grant,
                            action = { viewModel.requestNotificationListenerPermission(context) },
                            isGranted = isNotificationListenerEnabled,
                        ),
                    )

                R.string.feat_button_remap_title ->
                    listOf(
                        PermissionItem(
                            iconRes = R.drawable.rounded_settings_accessibility_24,
                            title = R.string.perm_accessibility_title,
                            description = R.string.perm_accessibility_desc_remap,
                            dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                            actionLabel = R.string.perm_action_enable,
                            action = {
                                PermissionUtils.openAccessibilitySettings(context)
                            },
                            isGranted = isAccessibilityEnabled,
                        ),
                    )

                R.string.feat_snooze_notifications_title ->
                    listOf(
                        PermissionItem(
                            iconRes = R.drawable.rounded_snooze_24,
                            title = R.string.perm_notif_listener_title,
                            description = R.string.perm_notif_listener_desc_snooze,
                            dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                            actionLabel = R.string.perm_action_grant,
                            action = { viewModel.requestNotificationListenerPermission(context) },
                            isGranted = isNotificationListenerEnabled,
                        ),
                    )

                R.string.feat_dynamic_night_light_title ->
                    listOf(
                        PermissionItem(
                            iconRes = R.drawable.rounded_settings_accessibility_24,
                            title = R.string.perm_accessibility_title,
                            description = R.string.perm_accessibility_desc_night_light,
                            dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                            actionLabel = R.string.perm_action_enable,
                            action = {
                                PermissionUtils.openAccessibilitySettings(context)
                            },
                            isGranted = isAccessibilityEnabled,
                        ),
                        PermissionItem(
                            iconRes = R.drawable.rounded_security_24,
                            title = R.string.perm_write_secure_title,
                            description = R.string.perm_write_secure_desc_night_light,
                            dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                            actionLabel = R.string.perm_action_how_to,
                            action = { /* instructions */ },
                            isGranted = isWriteSecureSettingsEnabled,
                        ),
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
                                            com.sameerasw.essentials.utils.RootUtils
                                                .isRootPermissionGranted()
                                    },
                                    isGranted = isRootPermissionGranted,
                                ),
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
                                        val intent =
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                "https://github.com/rushiranpise/Shizuku-Next".toUri(),
                                            )
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    },
                                    secondaryActionLabel = R.string.perm_porter_install_action,
                                    secondaryAction = {
                                        val intent =
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                "https://github.com/d4rken-org/porter".toUri(),
                                            )
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    },
                                    isGranted = isShizukuAvailable,
                                ),
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
                                    isGranted = isShizukuPermissionGranted,
                                ),
                            )
                        }
                    }
                    shellItems
                }

                R.string.feat_app_lock_title ->
                    listOf(
                        PermissionItem(
                            iconRes = R.drawable.rounded_settings_accessibility_24,
                            title = R.string.perm_accessibility_title,
                            description = R.string.perm_accessibility_desc_common,
                            dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                            actionLabel = R.string.perm_action_enable,
                            action = {
                                PermissionUtils.openAccessibilitySettings(context)
                            },
                            isGranted = isAccessibilityEnabled,
                        ),
                    )

                R.string.feat_smart_pixels_title ->
                    listOf(
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
                            isGranted = isAccessibilityEnabled,
                        ),
                    )

                R.string.feat_call_vibrations_title ->
                    listOf(
                        PermissionItem(
                            iconRes = R.drawable.rounded_mobile_24,
                            title = R.string.permission_read_phone_state_title,
                            description = R.string.permission_read_phone_state_desc_call_vibrations,
                            dependentFeatures = PermissionRegistry.getFeatures("READ_PHONE_STATE"),
                            actionLabel = R.string.perm_action_grant,
                            action = { viewModel.requestReadPhoneStatePermission(context as Activity) },
                            isGranted = isReadPhoneStateEnabled,
                        ),
                        PermissionItem(
                            iconRes = R.drawable.rounded_notifications_unread_24,
                            title = R.string.perm_notif_listener_title,
                            description = R.string.perm_notif_listener_desc_lighting,
                            dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                            actionLabel = R.string.perm_action_grant,
                            action = { viewModel.requestNotificationListenerPermission(context) },
                            isGranted = isNotificationListenerEnabled,
                        ),
                    )

                R.string.flashlight_pulse_title ->
                    listOf(
                        PermissionItem(
                            iconRes = R.drawable.rounded_notifications_unread_24,
                            title = R.string.perm_notif_listener_title,
                            description = R.string.perm_notif_listener_desc_lighting,
                            dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                            actionLabel = R.string.perm_action_grant,
                            action = { viewModel.requestNotificationListenerPermission(context) },
                            isGranted = isNotificationListenerEnabled,
                        ),
                    )

                R.string.feat_essentials_on_display_title ->
                    listOf(
                        PermissionItem(
                            iconRes = R.drawable.rounded_settings_accessibility_24,
                            title = R.string.perm_accessibility_title,
                            description = R.string.perm_accessibility_desc_essentials_on_display,
                            dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                            actionLabel = R.string.perm_action_enable,
                            action = {
                                PermissionUtils.openAccessibilitySettings(context)
                            },
                            isGranted = isAccessibilityEnabled,
                        ),
                        PermissionItem(
                            iconRes = R.drawable.rounded_notifications_unread_24,
                            title = R.string.perm_notif_listener_title,
                            description = R.string.perm_notif_listener_desc_lighting,
                            dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                            actionLabel = R.string.perm_action_grant,
                            action = { viewModel.requestNotificationListenerPermission(context) },
                            isGranted = isNotificationListenerEnabled,
                        ),
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
                },
            )
        }
    }

    var showInstructionsSheet by rememberSaveable { mutableStateOf(false) }

    if (showInstructionsSheet) {
        com.sameerasw.essentials.ui.core.sheets.InstructionsBottomSheet(
            onDismissRequest = { showInstructionsSheet = false },
        )
    }

    if (showHelpSheet && selectedHelpFeature != null) {
        com.sameerasw.essentials.ui.core.sheets.FeatureHelpBottomSheet(
            onDismissRequest = {
                showHelpSheet = false
                selectedHelpFeature = null
            },
            feature = selectedHelpFeature!!,
        )
    }

    if (showReorderFavoritesSheet) {
        ReorderFavoritesBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showReorderFavoritesSheet = false },
        )
    }

    val lazyListState = rememberLazyListState()
    val view = LocalView.current
    val searchQuery = viewModel.searchQuery.value

    val allFeatures =
        FeatureRegistry.getFilteredFeatures(
            context,
            viewModel.isEnableUnsupportedFeatures.value,
            viewModel.isShowLegacyFeatures.value,
        )

    val searchResults = viewModel.searchResults.value
    val isSearchingViewModel = viewModel.isSearching.value
    val recentSearches by viewModel.recentSearches
    val matchingTiles =
        remember(searchQuery, viewModel.isEnableUnsupportedFeatures.value, viewModel.isUseUsageAccess.value) {
            QSTileRegistry.searchTiles(
                context = context,
                query = searchQuery,
                includeUnsupported = viewModel.isEnableUnsupportedFeatures.value,
                isUseUsageStats = viewModel.isUseUsageAccess.value,
            )
        }
    val matchingPermissions =
        remember(searchQuery, viewModel) {
            PermissionUIHelper.searchPermissions(
                context = context,
                query = searchQuery,
                viewModel = viewModel,
                activity = context as? Activity,
            )
        }
    val freezePickedApps by viewModel.freezePickedApps
    val matchingFrozenApps =
        remember(searchQuery, freezePickedApps) {
            if (searchQuery.isBlank()) {
                emptyList()
            } else {
                freezePickedApps.filter { app ->
                    (app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)) &&
                        FreezeManager.isAppFrozen(context, app.packageName)
                }
            }
        }

    val isMotionBlurEnabled by viewModel.isMotionBlurEnabled
    val appVersion = remember { "v${BuildConfig.VERSION_NAME}" }
    val headerState = rememberPullTitleHeaderState()

    val listPaneWidth = LocalListPaneWidth.current
    val hasValidFavorites =
        remember(pinnedFeatureKeys) {
            val ids = FeatureRegistry.ALL_FEATURES.mapTo(HashSet()) { it.id }
            pinnedFeatureKeys.any { it in ids }
        }
    val gridEligible = listPaneWidth != null && hasValidFavorites && !isSearchActive && searchQuery.isEmpty()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val showFavoritesGrid = gridEligible && maxWidth >= listPaneWidth!! + 280.dp
        val listWidth = if (gridEligible) listPaneWidth!!.coerceAtMost(maxWidth) else maxWidth
        LazyColumn(
            state = lazyListState,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width(listWidth)
                    .nestedScroll(headerState.nestedScrollConnection)
                    .scrollMotionBlur(lazyListState, enabled = isMotionBlurEnabled),
            contentPadding =
                PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 64.dp,
                ),
        ) {
                item {
                    PullTitleHeader(
                        title = stringResource(R.string.tab_essentials),
                        caption = appVersion,
                        state = headerState,
                    )
                }
                item {
                    // Favorites
                    AnimatedVisibility(
                        visible = !showFavoritesGrid && !isSearchActive && pinnedFeatureKeys.isNotEmpty() && searchQuery.isEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        FavoriteCarousel(
                            pinnedKeys = pinnedFeatureKeys,
                            onFeatureClick = { feature ->
                                BiometricSecurityHelper.runWithAuth(
                                    activity = context as FragmentActivity,
                                    feature = feature,
                                    action = {
                                        feature.onClick(context, viewModel)
                                    },
                                )
                            },
                            onFeatureLongClick = { feature ->
                                viewModel.togglePinFeature(feature.id)
                            },
                            onReorderClick = {
                                showReorderFavoritesSheet = true
                            },
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                }

                if (isSearchActive && searchQuery.isEmpty()) {
                    item {
                        RecentSearchesSection(
                            recentSearches = recentSearches,
                            allFeatures = allFeatures,
                            pinnedFeatureKeys = pinnedFeatureKeys,
                            context = context,
                            viewModel = viewModel,
                        )
                    }
                } else if (searchQuery.isNotEmpty()) {
                    if (!isSearchingViewModel && searchResults.isEmpty() && matchingTiles.isEmpty() && matchingPermissions.isEmpty() && matchingFrozenApps.isEmpty()) {
                        item {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 64.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "¯\\_(ツ)_/¯",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(id = R.string.search_no_results, searchQuery),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    maxLines = 2,
                                    modifier =
                                        Modifier
                                            .padding(top = 8.dp)
                                            .padding(horizontal = 32.dp),
                                )
                            }
                        }
                    }

                    if (!isSearchingViewModel) {
                        item {
                            SearchResultsSection(
                                searchResults = searchResults,
                                allFeatures = allFeatures,
                                pinnedFeatureKeys = pinnedFeatureKeys,
                                context = context,
                                viewModel = viewModel,
                                onNavigateToTab = onNavigateToTab,
                            )
                        }
                    }
                } else if (!isSearchActive) {
                    val topLevelFeatures =
                        allFeatures.filter { it.parentFeatureId == null && it.isVisibleInMain }
                    val featureMap = topLevelFeatures.associateBy { it.id }

                    val sectionFeatureIdsList =
                        listOf(
                            listOf(
                                "Notifications",
                                "Sound",
                                "Display",
                                "Maps power saving mode",
                                "Daily Wallpaper",
                                "Widgets",
                            ),
                            listOf(
                                "Input",
                                "Power and battery",
                                "Quick settings tiles",
                                "Watch",
                            ),
                            listOf(
                                "Security",
                                "Networks",
                            ),
                            listOf(
                                "Location reached",
                                "Watermark",
                            ),
                        )

                    val assignedIds = sectionFeatureIdsList.flatten().toSet()
                    val unassignedFeatures = topLevelFeatures.filter { it.id !in assignedIds }

                    val sections =
                        sectionFeatureIdsList
                            .map { ids ->
                                ids.mapNotNull { featureMap[it] }
                            }.filter { it.isNotEmpty() } +
                            if (unassignedFeatures.isNotEmpty()) {
                                listOf(
                                    unassignedFeatures,
                                )
                            } else {
                                emptyList()
                            }

                    sections.forEachIndexed { sectionIndex, sectionFeatures ->
                        item {
                            RoundedCardContainer(
                                modifier =
                                    Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 12.dp),
                            ) {
                                sectionFeatures.forEach { feature ->
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
                                                },
                                            )
                                        },
                                        onClick = {
                                            BiometricSecurityHelper.runWithAuth(
                                                activity = context as FragmentActivity,
                                                feature = feature,
                                                action = {
                                                    feature.onClick(context, viewModel)
                                                },
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
                                        isLegacy = feature.isLegacy,
                                        isUnsupported = !feature.isDeviceSupported(context),
                                        isPinned = pinnedFeatureKeys.contains(feature.id),
                                        onPinToggle = {
                                            viewModel.togglePinFeature(feature.id)
                                        },
                                        onHelpClick =
                                            if (feature.aboutDescription != null) {
                                                {
                                                    selectedHelpFeature = feature
                                                    showHelpSheet = true
                                                }
                                            } else {
                                                null
                                            },
                                        hasBadge = if (feature.id == "Watch") isWearUpdateRequired else false,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ListExpandToggleButton(
                            isExpanded = false,
                            onToggle = {
                                showInstructionsSheet = true
                            },
                            title = R.string.label_help_guide,
                            description = R.string.label_help_guide,
                            iconRes = R.drawable.rounded_help_24,
                            modifier =
                                Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp, bottom = 12.dp),
                        )
                    }
                }
        }
        if (showFavoritesGrid) {
            FavoriteGrid(
                pinnedKeys = pinnedFeatureKeys,
                onFeatureClick = { feature ->
                    BiometricSecurityHelper.runWithAuth(
                        activity = context as FragmentActivity,
                        feature = feature,
                        action = {
                            feature.onClick(context, viewModel)
                        },
                    )
                },
                onFeatureLongClick = { feature ->
                    viewModel.togglePinFeature(feature.id)
                },
                onReorderClick = {
                    showReorderFavoritesSheet = true
                },
                modifier =
                    Modifier
                        .offset(x = listWidth)
                        .width(maxWidth - listWidth)
                        .fillMaxHeight(),
                contentPadding =
                    PaddingValues(
                        top = contentPadding.calculateTopPadding() + 140.dp,
                        bottom = contentPadding.calculateBottomPadding() + 64.dp,
                        end = 16.dp,
                    ),
            )
        }
    }
}

@Composable
private fun RecentSearchesSection(
    recentSearches: List<com.sameerasw.essentials.domain.model.SearchableItem>,
    allFeatures: List<com.sameerasw.essentials.domain.model.Feature>,
    pinnedFeatureKeys: List<String>,
    context: Context,
    viewModel: MainViewModel,
) {
    if (recentSearches.isEmpty()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(
                text = stringResource(R.string.label_no_recent_searches),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    } else {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.label_recent_searches),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.label_clear_all),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { viewModel.clearRecentSearches() },
            )
        }

        val validSearches =
            remember(recentSearches, allFeatures) {
                recentSearches.filter { item ->
                    item.title.isNotBlank() &&
                        allFeatures.any { it.id == item.featureKey }
                }
            }

        LaunchedEffect(recentSearches.size, validSearches.size) {
            if (recentSearches.isNotEmpty() && validSearches.size != recentSearches.size) {
                // If any corrupted/invalid feature IDs were found in history, clean them up
                viewModel.clearRecentSearches()
            }
        }

        RoundedCardContainer(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            validSearches.forEach { result ->
                FeatureCard(
                    title = result.title,
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        viewModel.addRecentSearch(result)
                        val feature = allFeatures.find { it.id == result.featureKey }
                        if (feature != null) {
                            val targetFeatureKey =
                                if (feature.searchTargetFeatureId != null) {
                                    feature.searchTargetFeatureId
                                } else if (!feature.hasMoreSettings && feature.parentFeatureId != null) {
                                    feature.parentFeatureId
                                } else {
                                    feature.id
                                }
                            val highlightKey =
                                if (!feature.hasMoreSettings && feature.parentFeatureId != null) {
                                    feature.id
                                } else {
                                    result.targetSettingHighlightKey
                                }
                            BiometricSecurityHelper.runWithAuth(
                                activity = context as FragmentActivity,
                                feature = feature,
                                action = {
                                    val intent =
                                        if (targetFeatureKey == "LiveWallpaper" || targetFeatureKey == "Daily Wallpaper") {
                                            Intent(
                                                context,
                                                com.sameerasw.essentials.ui.activities.WallpaperActivity::class.java,
                                            ).apply {
                                                putExtra(
                                                    "tab",
                                                    if (targetFeatureKey == "LiveWallpaper") "live" else "daily",
                                                )
                                            }
                                        } else {
                                            Intent(
                                                context,
                                                FeatureSettingsActivity::class.java,
                                            ).apply {
                                                putExtra("feature", targetFeatureKey)
                                                highlightKey?.let {
                                                    putExtra("highlight_setting", it)
                                                }
                                            }
                                        }
                                    context.startActivity(intent)
                                },
                            )
                        }
                    },
                    iconRes = result.icon ?: R.drawable.rounded_settings_24,
                    showToggle = false,
                    hasMoreSettings = true,
                    description = result.description,
                    isBeta = result.isBeta,
                    isLegacy = result.isLegacy,
                    isUnsupported = result.isUnsupported,
                    isPinned = pinnedFeatureKeys.contains(result.featureKey),
                    onPinToggle = {
                        viewModel.togglePinFeature(result.featureKey)
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchResultsSection(
    searchResults: List<com.sameerasw.essentials.domain.model.SearchableItem>,
    allFeatures: List<com.sameerasw.essentials.domain.model.Feature>,
    pinnedFeatureKeys: List<String>,
    context: Context,
    viewModel: MainViewModel,
    onNavigateToTab: ((com.sameerasw.essentials.domain.DIYTabs) -> Unit)? = null,
) {
    val query = viewModel.searchQuery.value
    val matchingTiles =
        remember(query, viewModel.isEnableUnsupportedFeatures.value, viewModel.isUseUsageAccess.value) {
            QSTileRegistry.searchTiles(
                context = context,
                query = query,
                includeUnsupported = viewModel.isEnableUnsupportedFeatures.value,
                isUseUsageStats = viewModel.isUseUsageAccess.value,
            )
        }
    val matchingPermissions =
        remember(query, viewModel) {
            PermissionUIHelper.searchPermissions(
                context = context,
                query = query,
                viewModel = viewModel,
                activity = context as? Activity,
            )
        }
    val freezePickedApps by viewModel.freezePickedApps
    val matchingFrozenApps =
        remember(query, freezePickedApps) {
            if (query.isBlank()) {
                emptyList()
            } else {
                freezePickedApps.filter { app ->
                    app.appName.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
                }
            }
        }

    var isExpanded by rememberSaveable(query) { mutableStateOf(false) }

    val nonQSSearchResults =
        remember(searchResults) {
            searchResults.filter { it.featureKey != "Quick settings tiles" }
        }

    val visibleResults =
        if (!isExpanded && nonQSSearchResults.size > 7) {
            nonQSSearchResults.take(7)
        } else {
            nonQSSearchResults
        }

    if (matchingTiles.isNotEmpty() || nonQSSearchResults.isNotEmpty() || matchingPermissions.isNotEmpty() || matchingFrozenApps.isNotEmpty()) {
        if (nonQSSearchResults.isNotEmpty()) {
            Text(
                text = stringResource(R.string.search_results_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RoundedCardContainer(
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = tween(durationMillis = 300)),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    for (result in visibleResults) {
                        FeatureCard(
                            title = result.title,
                            isEnabled = true,
                            onToggle = {},
                            onClick = {
                                viewModel.addRecentSearch(result)
                                val feature = allFeatures.find { it.id == result.featureKey }
                                if (feature != null) {
                                    val targetFeatureKey =
                                        if (feature.searchTargetFeatureId != null) {
                                            feature.searchTargetFeatureId
                                        } else if (!feature.hasMoreSettings && feature.parentFeatureId != null) {
                                            feature.parentFeatureId
                                        } else {
                                            feature.id
                                        }
                                    val highlightKey =
                                        if (!feature.hasMoreSettings && feature.parentFeatureId != null) {
                                            feature.id
                                        } else {
                                            result.targetSettingHighlightKey
                                        }
                                    BiometricSecurityHelper.runWithAuth(
                                        activity = context as FragmentActivity,
                                        feature = feature,
                                        action = {
                                            val intent =
                                                if (targetFeatureKey == "Pixel Searchbar") {
                                                    Intent(
                                                        context,
                                                        PixelSearchbarSettingsActivity::class.java,
                                                    )
                                                } else if (targetFeatureKey == "LiveWallpaper" || targetFeatureKey == "Daily Wallpaper") {
                                                    Intent(
                                                        context,
                                                        WallpaperActivity::class.java,
                                                    ).apply {
                                                        putExtra(
                                                            "tab",
                                                            if (targetFeatureKey == "LiveWallpaper") "live" else "daily",
                                                        )
                                                    }
                                                } else if (targetFeatureKey == "App updates") {
                                                    Intent(
                                                        context,
                                                        YourAndroidActivity::class.java,
                                                    )
                                                } else {
                                                    Intent(
                                                        context,
                                                        FeatureSettingsActivity::class.java,
                                                    ).apply {
                                                        putExtra("feature", targetFeatureKey)
                                                        highlightKey?.let {
                                                            putExtra("highlight_setting", it)
                                                        }
                                                    }
                                                }
                                            context.startActivity(intent)
                                        },
                                    )
                                } else {
                                    val intent =
                                        if (result.featureKey == "Pixel Searchbar") {
                                            Intent(
                                                context,
                                                PixelSearchbarSettingsActivity::class.java,
                                            )
                                        } else if (result.featureKey == "LiveWallpaper" || result.featureKey == "Daily Wallpaper") {
                                            Intent(
                                                context,
                                                WallpaperActivity::class.java,
                                            ).apply {
                                                putExtra(
                                                    "tab",
                                                    if (result.featureKey == "LiveWallpaper") "live" else "daily",
                                                )
                                            }
                                        } else if (result.featureKey == "App updates") {
                                            Intent(
                                                context,
                                                YourAndroidActivity::class.java,
                                            )
                                        } else {
                                            Intent(context, FeatureSettingsActivity::class.java).apply {
                                                putExtra("feature", result.featureKey)
                                                result.targetSettingHighlightKey?.let {
                                                    putExtra("highlight_setting", it)
                                                }
                                            }
                                        }
                                    context.startActivity(intent)
                                }
                            },
                            iconRes = result.icon ?: R.drawable.rounded_settings_24,
                            modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
                            showToggle = false,
                            hasMoreSettings = true,
                            isBeta = result.isBeta,
                            isLegacy = result.isLegacy,
                            isUnsupported = result.isUnsupported,
                            descriptionOverride =
                                if (result.parentFeature !=
                                    null
                                ) {
                                    "${result.parentFeature} > ${result.description}"
                                } else {
                                    result.description
                                },
                            isPinned = pinnedFeatureKeys.contains(result.featureKey),
                            onPinToggle = {
                                viewModel.togglePinFeature(result.featureKey)
                            },
                        )
                    }
                }
            }

            if (nonQSSearchResults.size > 7) {
                ListExpandToggleButton(
                    isExpanded = isExpanded,
                    onToggle = { isExpanded = !isExpanded },
                    title = R.string.action_show_more,
                    description = R.string.action_show_less,
                    expandedText = stringResource(R.string.action_show_less),
                    collapsedText = stringResource(R.string.action_show_more),
                    modifier =
                        Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 4.dp),
                )
            }
        }

        if (matchingFrozenApps.isNotEmpty()) {
            FrozenAppsSearchResultCard(
                apps = matchingFrozenApps,
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onNavigateToFreezeTab = onNavigateToTab?.let { nav -> { nav(com.sameerasw.essentials.domain.DIYTabs.FREEZE) } },
            )
        }

        if (matchingTiles.isNotEmpty()) {
            QSTilesSearchResultCard(
                tiles = matchingTiles,
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (matchingPermissions.isNotEmpty()) {
            PermissionsSearchResultCard(
                permissions = matchingPermissions,
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}
