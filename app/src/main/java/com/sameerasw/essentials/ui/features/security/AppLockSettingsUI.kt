/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Security & Device Protection
 * File: AppLockSettingsUI.kt
 * Description: Composable UI for configuring AppLock authentication, target applications,
 * usage access permissions, and auto-lock delay intervals.
 */

package com.sameerasw.essentials.ui.features.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionItem
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.BiometricHelper
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.viewmodels.PermissionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSettingsUI(
    viewModel: MainViewModel,
    permissionViewModel: PermissionViewModel = viewModel(),
    modifier: Modifier = Modifier,
    highlightKey: String? = null,
) {
    val context = LocalContext.current
    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }
    var showPermissionSheet by remember { mutableStateOf(false) }

    val isAppLockEnabled by viewModel.isAppLockEnabled
    val isUseUsageAccess by viewModel.isUseUsageAccess
    val isAccessibilityEnabled by permissionViewModel.isAccessibilityEnabled
    val isUsageStatsPermissionGranted by viewModel.isUsageStatsPermissionGranted

    val canEnableAppLock =
        if (isUseUsageAccess) isUsageStatsPermissionGranted else isAccessibilityEnabled

    val delayLabels =
        listOf(
            stringResource(R.string.app_lock_auto_lock_delay_none),
            stringResource(R.string.app_lock_auto_lock_delay_1min),
            stringResource(R.string.app_lock_auto_lock_delay_5min),
            stringResource(R.string.app_lock_auto_lock_delay_10min),
            stringResource(R.string.app_lock_auto_lock_delay_20min),
            stringResource(R.string.app_lock_auto_lock_delay_30min),
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_section_security),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_shield_lock_24,
                title = stringResource(R.string.app_lock_enable_title),
                isChecked = isAppLockEnabled,
                onCheckedChange = { enabled ->
                    if (context is FragmentActivity) {
                        BiometricHelper.showBiometricPrompt(
                            activity = context,
                            title = context.getString(R.string.app_lock_auth_title),
                            subtitle =
                                if (enabled) {
                                    context.getString(R.string.app_lock_enable_auth_subtitle)
                                } else {
                                    context.getString(
                                        R.string.app_lock_disable_auth_subtitle,
                                    )
                                },
                            onSuccess = { viewModel.setAppLockEnabled(enabled, context) },
                        )
                    } else {
                        viewModel.setAppLockEnabled(enabled, context)
                    }
                },
                enabled = canEnableAppLock,
                onDisabledClick = { showPermissionSheet = true },
                modifier = Modifier.highlight(highlightKey == "app_lock_enabled"),
            )

            FeatureCard(
                title = stringResource(R.string.app_lock_select_apps_title),
                description = stringResource(R.string.app_lock_select_apps_desc),
                iconRes = R.drawable.rounded_app_registration_24,
                isEnabled = isAppLockEnabled,
                showToggle = false,
                hasMoreSettings = true,
                onToggle = {},
                onClick = { isAppSelectionSheetOpen = true },
                modifier = Modifier.highlight(highlightKey == "app_lock_selected_apps"),
            )

            ConfigPickerItem(
                title = stringResource(R.string.app_lock_auto_lock_delay_title),
                description = stringResource(R.string.app_lock_auto_lock_delay_desc),
                iconRes = R.drawable.rounded_lock_clock_24,
                isEnabled = isAppLockEnabled,
                selectedValue = delayLabels[viewModel.appLockAutoLockDelayIndex.intValue],
                modifier = Modifier.highlight(highlightKey == "app_lock_auto_lock_delay"),
            ) {
                delayLabels.forEachIndexed { index, label ->
                    SegmentedDropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            viewModel.setAppLockAutoLockDelayIndex(index)
                        },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.app_lock_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.app_lock_warning),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.app_lock_biometric_note),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isAppSelectionSheetOpen) {
            AppSelectionSheet(
                onDismissRequest = { isAppSelectionSheetOpen = false },
                onLoadApps = { viewModel.loadAppLockSelectedApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveAppLockSelectedApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled ->
                    viewModel.updateAppLockAppEnabled(
                        ctx,
                        pkg,
                        enabled,
                    )
                },
            )
        }

        if (showPermissionSheet) {
            val permissionItem =
                if (isUseUsageAccess) {
                    PermissionItem(
                        iconRes = R.drawable.rounded_data_usage_24,
                        title = R.string.perm_usage_stats_title,
                        description = R.string.perm_usage_stats_desc_app_lock,
                        dependentFeatures = listOf(R.string.feat_app_lock_title),
                        actionLabel = if (isUsageStatsPermissionGranted) R.string.perm_action_granted else R.string.perm_action_grant,
                        action = { PermissionUtils.openUsageStatsSettings(context) },
                        isGranted = isUsageStatsPermissionGranted,
                    )
                } else {
                    PermissionItem(
                        iconRes = R.drawable.rounded_settings_accessibility_24,
                        title = R.string.perm_accessibility_title,
                        description = R.string.perm_accessibility_desc_common,
                        dependentFeatures = listOf(R.string.feat_app_lock_title),
                        actionLabel = if (isAccessibilityEnabled) R.string.perm_action_granted else R.string.perm_action_grant,
                        action = { PermissionUtils.openAccessibilitySettings(context) },
                        isGranted = isAccessibilityEnabled,
                    )
                }

            PermissionsBottomSheet(
                onDismissRequest = {
                    showPermissionSheet = false
                    permissionViewModel.refreshPermissions(context)
                    viewModel.check(context)
                },
                featureTitle = R.string.feat_app_lock_title,
                permissions = listOf(permissionItem),
            )
        }
    }
}
