/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - System
 * File: OtherCustomizationsSettingsUI.kt
 * Description: UI component and settings composable for System feature domain.
 */

package com.sameerasw.essentials.ui.features.system

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PermissionItem
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.viewmodels.MainViewModel

enum class PermissionModule {
    HIDE_GESTURE_BAR,
    SHOW_ON_LAUNCHER,
    CIRCLE_TO_SEARCH,
    DISABLE_ROTATION_SUGGESTION,
    PIXEL_SEARCHBAR,
    PREFER_GPU_COMPOSING,
    ALLOW_OVERLAYS_IN_SETTINGS,
    TRANSPARENT_NAVIGATION_BAR,
    NONE,
}

@Composable
fun OtherCustomizationsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    var requestingPermissionFor by remember { mutableStateOf(PermissionModule.NONE) }

    if (requestingPermissionFor != PermissionModule.NONE) {
        val isShizukuAvailable = viewModel.isShizukuAvailable.value
        val isShizukuGranted = viewModel.isShizukuPermissionGranted.value
        val isRootAvailable = viewModel.isRootAvailable.value
        val isRootGranted = viewModel.isRootPermissionGranted.value
        val isShellGranted =
            (isShizukuAvailable && isShizukuGranted) || (isRootAvailable && isRootGranted)
        val isAccessibilityEnabled = viewModel.isAccessibilityEnabled.value
        val isUsageStatsGranted = viewModel.isUsageStatsPermissionGranted.value
        val useUsageAccess = viewModel.isUseUsageAccess.value

        val shizukuPermission =
            PermissionItem(
                iconRes = R.drawable.rounded_adb_24,
                title = if (!isShizukuAvailable) R.string.perm_shizuku_title else R.string.perm_shizuku_grant_title,
                description = if (!isShizukuAvailable) R.string.perm_shizuku_desc else R.string.perm_shizuku_grant_desc,
                dependentFeatures =
                    listOf(
                        R.string.feat_hide_gesture_bar_title,
                        R.string.feat_hide_gesture_bar_on_launcher_title,
                        R.string.feat_circle_to_search_gesture_title,
                        R.string.feat_prefer_gpu_composing_title,
                    ),
                actionLabel =
                    if (!isShizukuAvailable) {
                        R.string.perm_shizuku_install_action
                    } else if (isShellGranted) {
                        R.string.perm_action_granted
                    } else {
                        R.string.perm_action_grant
                    },
                action = {
                    if (!isShizukuAvailable) {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/thedjchi/Shizuku"),
                            )
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    } else {
                        viewModel.requestShizukuPermission()
                    }
                },
                isGranted = isShellGranted,
            )

        val accessibilityPermission =
            PermissionItem(
                iconRes = R.drawable.rounded_accessibility_new_24,
                title = R.string.perm_accessibility_title,
                description = R.string.perm_accessibility_desc_common,
                dependentFeatures =
                    listOf(
                        R.string.feat_hide_gesture_bar_on_launcher_title,
                        R.string.feat_circle_to_search_gesture_title,
                    ),
                actionLabel = if (isAccessibilityEnabled) R.string.label_enabled else R.string.perm_action_enable,
                action = {
                    com.sameerasw.essentials.utils.PermissionUtils.openAccessibilitySettings(
                        context,
                    )
                },
                isGranted = isAccessibilityEnabled,
            )

        val usageStatsPermission =
            PermissionItem(
                iconRes = R.drawable.rounded_data_usage_24,
                title = R.string.perm_usage_stats_title,
                description = R.string.perm_usage_stats_desc_app_lock,
                dependentFeatures = listOf(R.string.feat_hide_gesture_bar_on_launcher_title),
                actionLabel = if (isUsageStatsGranted) R.string.perm_action_granted else R.string.perm_action_enable,
                action = {
                    com.sameerasw.essentials.utils.PermissionUtils
                        .openUsageStatsSettings(context)
                },
                isGranted = isUsageStatsGranted,
            )

        val permissionsToShow =
            when (requestingPermissionFor) {
                PermissionModule.HIDE_GESTURE_BAR -> listOf(shizukuPermission)
                PermissionModule.SHOW_ON_LAUNCHER -> {
                    val appDetectionPermission =
                        if (useUsageAccess) usageStatsPermission else accessibilityPermission
                    listOf(shizukuPermission, appDetectionPermission)
                }

                PermissionModule.CIRCLE_TO_SEARCH -> listOf(shizukuPermission, accessibilityPermission)
                PermissionModule.DISABLE_ROTATION_SUGGESTION -> listOf(shizukuPermission)
                PermissionModule.PIXEL_SEARCHBAR -> listOf(shizukuPermission)
                PermissionModule.PREFER_GPU_COMPOSING -> listOf(shizukuPermission)
                PermissionModule.ALLOW_OVERLAYS_IN_SETTINGS -> listOf(shizukuPermission)
                PermissionModule.TRANSPARENT_NAVIGATION_BAR -> listOf(shizukuPermission)
                else -> emptyList()
            }

        PermissionsBottomSheet(
            onDismissRequest = { requestingPermissionFor = PermissionModule.NONE },
            featureTitle = R.string.feat_other_customizations_title,
            permissions = permissionsToShow,
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val isShizukuGranted =
            viewModel.isShizukuAvailable.value && viewModel.isShizukuPermissionGranted.value
        val isRootGranted =
            viewModel.isRootAvailable.value && viewModel.isRootPermissionGranted.value
        val isShellGranted = isShizukuGranted || isRootGranted
        val isAccessibilityEnabled = viewModel.isAccessibilityEnabled.value
        val isUsageStatsGranted = viewModel.isUsageStatsPermissionGranted.value
        val isAppDetectionGranted =
            if (viewModel.isUseUsageAccess.value) isUsageStatsGranted else isAccessibilityEnabled

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, viewModel.isCircleToSearchGestureEnabled.value) {
                val observer =
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.setCircleToSearchPreviewEnabled(viewModel.isCircleToSearchGestureEnabled.value)
                            viewModel.refreshPreferGpuComposingState(context)
                            viewModel.refreshAllowOverlaysInSettingsState(context)
                            viewModel.refreshTransparentNavigationBarState(context)
                        } else if (event == Lifecycle.Event.ON_PAUSE) {
                            viewModel.setCircleToSearchPreviewEnabled(false)
                        }
                    }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    viewModel.setCircleToSearchPreviewEnabled(false)
                }
            }

            IconToggleItem(
                title = stringResource(R.string.feat_disable_rotation_suggestion_title),
                description = stringResource(R.string.feat_disable_rotation_suggestion_desc),
                isChecked = viewModel.isDisableRotationSuggestionEnabled.value,
                onCheckedChange = { enabled ->
                    if (viewModel.isWriteSecureSettingsEnabled.value ||
                        viewModel.isShizukuPermissionGranted.value ||
                        viewModel.isRootPermissionGranted.value
                    ) {
                        viewModel.setDisableRotationSuggestionEnabled(enabled, context)
                    } else {
                        requestingPermissionFor = PermissionModule.DISABLE_ROTATION_SUGGESTION
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!viewModel.isWriteSecureSettingsEnabled.value &&
                        !viewModel.isShizukuPermissionGranted.value &&
                        !viewModel.isRootPermissionGranted.value
                    ) {
                        requestingPermissionFor = PermissionModule.DISABLE_ROTATION_SUGGESTION
                    }
                },
                iconRes = R.drawable.rounded_mobile_rotate_24,
                modifier = Modifier.highlight(highlightSetting == "disable_rotation_suggestion_toggle"),
            )

            IconToggleItem(
                title = stringResource(R.string.feat_allow_overlays_in_settings_title),
                description = stringResource(R.string.feat_allow_overlays_in_settings_desc),
                isChecked = viewModel.isAllowOverlaysInSettingsEnabled.value,
                onCheckedChange = { enabled ->
                    if (viewModel.isWriteSecureSettingsEnabled.value ||
                        viewModel.isShizukuPermissionGranted.value ||
                        viewModel.isRootPermissionGranted.value
                    ) {
                        viewModel.setAllowOverlaysInSettingsEnabled(enabled, context)
                    } else {
                        requestingPermissionFor = PermissionModule.ALLOW_OVERLAYS_IN_SETTINGS
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!viewModel.isWriteSecureSettingsEnabled.value &&
                        !viewModel.isShizukuPermissionGranted.value &&
                        !viewModel.isRootPermissionGranted.value
                    ) {
                        requestingPermissionFor = PermissionModule.ALLOW_OVERLAYS_IN_SETTINGS
                    }
                },
                iconRes = R.drawable.rounded_security_24,
                modifier = Modifier.highlight(highlightSetting == "allow_overlays_in_settings_toggle"),
            )
        }

        Text(
            text = stringResource(R.string.section_graphics),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                title = stringResource(R.string.feat_prefer_gpu_composing_title),
                description = stringResource(R.string.feat_prefer_gpu_composing_desc),
                isChecked = viewModel.isPreferGpuComposingEnabled.value,
                onCheckedChange = { enabled ->
                    if (isShellGranted) {
                        viewModel.setPreferGpuComposingEnabled(enabled, context)
                    } else {
                        requestingPermissionFor = PermissionModule.PREFER_GPU_COMPOSING
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!isShellGranted) {
                        requestingPermissionFor = PermissionModule.PREFER_GPU_COMPOSING
                    }
                },
                iconRes = R.drawable.rounded_memory_alt_24,
                modifier = Modifier.highlight(highlightSetting == "prefer_gpu_composing_toggle"),
            )
        }
    }
}
