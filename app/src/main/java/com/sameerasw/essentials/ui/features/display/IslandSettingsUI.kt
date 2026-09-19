/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandSettingsUI.kt
 * Description: UI settings composable for Island dynamic notifications feature.
 */

package com.sameerasw.essentials.ui.features.display

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Intent
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.features.consciousgate.CONSCIOUS_GATE_FEATURE_ID
import com.sameerasw.essentials.ui.features.display.sheets.IslandTimeBatteryOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.StatusGlanceCalendarOptionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

private val ISLAND_PLACEMENT_KEYS = setOf("island_use_auto_detect", "island_camera_size", "island_max_width", "island_expanded_width", "island_cutout_gap")
private val ISLAND_VISUALS_KEYS = setOf("island_expanded_roundness", "island_expanded_padding", "island_expanded_top_padding")

@Composable
private fun IslandExpandableSection(
    title: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val view = LocalView.current
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "island_section_arrow")

    RoundedCardContainer(
        modifier = modifier,
        spacing = 2.dp,
        cornerRadius = 24.dp,
    ) {
        ListItem(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                expanded = !expanded
            },
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                    contentDescription = if (expanded) stringResource(R.string.action_collapse) else stringResource(R.string.action_expand),
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
        )

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var requestingPermissionsFor by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }
    var showMediaAppSelectionSheet by remember { mutableStateOf(false) }
    var showCalendarOptionsSheet by remember { mutableStateOf(false) }
    var showTimeBatteryOptionsSheet by remember { mutableStateOf(false) }

    if (requestingPermissionsFor != null) {
        val (titleRes, permKeys) = requestingPermissionsFor!!
        val permissionItems = PermissionUIHelper.getPermissionItems(permKeys, context, viewModel)
        PermissionsBottomSheet(
            onDismissRequest = {
                requestingPermissionsFor = null
                viewModel.check(context)
            },
            featureTitle = stringResource(titleRes),
            permissions = permissionItems,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = stringResource(R.string.island_enable_title),
                description = stringResource(R.string.island_enable_desc),
                isChecked = viewModel.isIslandEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked) {
                        val missingPermissions = mutableListOf<String>()
                        if (!viewModel.isAccessibilityEnabled.value) {
                            missingPermissions.add("ACCESSIBILITY")
                        }
                        if (!viewModel.isNotificationListenerEnabled.value) {
                            missingPermissions.add("NOTIFICATION_LISTENER")
                        }

                        if (missingPermissions.isNotEmpty()) {
                            requestingPermissionsFor = Pair(R.string.island_title, missingPermissions)
                        } else {
                            viewModel.setIslandEnabled(true)
                        }
                    } else {
                        viewModel.setIslandEnabled(false)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "island_enabled"),
            )
        }

        IslandExpandableSection(
            title = stringResource(R.string.island_section_placement),
            iconRes = R.drawable.rounded_center_focus_strong_24,
            initiallyExpanded = highlightSetting in ISLAND_PLACEMENT_KEYS,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_center_focus_strong_24,
                title = stringResource(R.string.island_auto_detect_title),
                description = stringResource(R.string.island_auto_detect_desc),
                isChecked = viewModel.isIslandAutoDetect.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandAutoDetect(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_use_auto_detect"),
            )

            AnimatedVisibility(
                visible = !viewModel.isIslandAutoDetect.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ConfigSliderItem(
                        title = stringResource(R.string.island_camera_offset_x_title),
                        value = viewModel.islandCameraOffsetX.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setIslandCameraOffsetX(it)
                        },
                        valueRange = 0f..100f,
                        increment = 1f,
                        iconRes = R.drawable.rounded_border_left_24,
                        valueFormatter = { "${it.toInt()}%" },
                    )
                    ConfigSliderItem(
                        title = stringResource(R.string.island_camera_offset_y_title),
                        value = viewModel.islandCameraOffsetY.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setIslandCameraOffsetY(it)
                        },
                        valueRange = 0f..20f,
                        increment = 0.5f,
                        iconRes = R.drawable.rounded_border_top_24,
                        valueFormatter = { "%.1f%%".format(it) },
                    )
                }
            }

            ConfigSliderItem(
                title = stringResource(R.string.island_camera_size_title),
                value = viewModel.islandCameraSize.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandCameraSize(it)
                },
                valueRange = 0.05f..2.0f,
                increment = 0.05f,
                iconRes = R.drawable.rounded_arrows_outward_24,
                valueFormatter = { "%.2fx".format(it) },
                modifier = Modifier.highlight(highlightSetting == "island_camera_size"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_max_width_title),
                value = viewModel.islandMaxWidth.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandMaxWidth(it)
                },
                valueRange = 150f..500f,
                increment = 10f,
                iconRes = R.drawable.rounded_arrows_outward_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_max_width"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_width_title),
                value = viewModel.islandExpandedWidth.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedWidth(it)
                },
                valueRange = 200f..500f,
                increment = 10f,
                iconRes = R.drawable.rounded_arrows_outward_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_width"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_cutout_gap_title),
                value = viewModel.islandCutoutGap.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandCutoutGap(it)
                },
                valueRange = 0f..16f,
                increment = 1f,
                iconRes = R.drawable.rounded_arrows_outward_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_cutout_gap"),
            )
        }

        IslandExpandableSection(
            title = stringResource(R.string.island_section_visuals),
            iconRes = R.drawable.rounded_rounded_corner_24,
            initiallyExpanded = highlightSetting in ISLAND_VISUALS_KEYS,
        ) {
            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_roundness_title),
                value = viewModel.islandExpandedRoundness.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedRoundness(it)
                },
                valueRange = 0f..80f,
                increment = 2f,
                iconRes = R.drawable.rounded_rounded_corner_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_roundness"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_padding_title),
                value = viewModel.islandExpandedPadding.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedPadding(it)
                },
                valueRange = 8f..48f,
                increment = 2f,
                iconRes = R.drawable.rounded_screenshot_region_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_padding"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_top_padding_title),
                value = viewModel.islandExpandedTopPadding.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedTopPadding(it)
                },
                valueRange = 0f..40f,
                increment = 2f,
                iconRes = R.drawable.rounded_vertical_align_top_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_top_padding"),
            )
        }

        IslandExpandableSection(
            title = stringResource(R.string.island_section_duration),
            iconRes = R.drawable.rounded_timer_24,
            initiallyExpanded = highlightSetting == "island_expanded_timeout_ms",
        ) {
            ConfigSliderItem(
                title = stringResource(R.string.island_timeout_title),
                value = (viewModel.islandTimeoutMs.longValue / 1000f),
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandTimeoutMs((it * 1000).toLong())
                },
                valueRange = 2f..10f,
                increment = 0.5f,
                iconRes = R.drawable.rounded_timer_24,
                valueFormatter = { "%.1fs".format(it) },
            )

            val infinityText = stringResource(R.string.island_timeout_infinity)
            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_timeout_title),
                description = stringResource(R.string.island_expanded_timeout_desc),
                value = (viewModel.islandExpandedTimeoutMs.longValue / 1000f),
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedTimeoutMs((it * 1000).toLong())
                },
                valueRange = 0f..30f,
                increment = 1f,
                iconRes = R.drawable.rounded_schedule_24,
                valueFormatter = { if (it <= 0f) infinityText else "${it.toInt()}s" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_timeout_ms"),
            )
        }

        Text(
            text = stringResource(R.string.island_section_behavior),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_lock_portrait_24,
                title = stringResource(R.string.island_hide_when_screen_off_title),
                isChecked = viewModel.isIslandHideWhenScreenOff.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandHideWhenScreenOff(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_hide_when_screen_off"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_blur_on_24,
                title = stringResource(R.string.island_show_glow_title),
                isChecked = viewModel.isIslandShowGlow.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowGlow(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_glow"),
            )

            val tapActionLabel = if (viewModel.islandTapAction.value == SettingsRepository.ISLAND_TAP_ACTION_EXPAND) {
                stringResource(R.string.island_tap_action_expand)
            } else {
                stringResource(R.string.island_tap_action_open)
            }
            ConfigPickerItem(
                title = stringResource(R.string.island_tap_action_title),
                selectedValue = tapActionLabel,
                iconRes = R.drawable.rounded_touch_app_24,
                modifier = Modifier.fillMaxWidth().highlight(highlightSetting == "island_tap_action"),
            ) {
                SegmentedDropdownMenuItem(
                    text = { Text(stringResource(R.string.island_tap_action_open)) },
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandTapAction(SettingsRepository.ISLAND_TAP_ACTION_OPEN)
                    },
                )
                SegmentedDropdownMenuItem(
                    text = { Text(stringResource(R.string.island_tap_action_expand)) },
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandTapAction(SettingsRepository.ISLAND_TAP_ACTION_EXPAND)
                    },
                )
            }
        }

        Text(
            text = stringResource(R.string.island_section_notifications),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_off_24,
                title = stringResource(R.string.island_suppress_system_heads_up_title),
                isChecked = viewModel.isIslandSuppressSystemHeadsUp.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !PermissionUtils.canWriteSecureSettings(context) && !ShellUtils.isAvailable(context)) {
                        requestingPermissionsFor = Pair(R.string.island_suppress_system_heads_up_title, listOf("WRITE_SECURE_SETTINGS"))
                    } else {
                        viewModel.setIslandSuppressSystemHeadsUp(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "island_suppress_system_heads_up"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = stringResource(R.string.island_catch_up_title),
                description = stringResource(R.string.island_catch_up_desc),
                isChecked = viewModel.isIslandCatchUpEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandCatchUpEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_catch_up"),
            )

            AnimatedVisibility(
                visible = viewModel.isIslandCatchUpEnabled.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                ConfigSliderItem(
                    title = stringResource(R.string.island_catch_up_timeout_title),
                    value = (viewModel.islandCatchUpTimeoutMs.longValue / 1000f),
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandCatchUpTimeoutMs((it * 1000).toLong())
                    },
                    valueRange = 5f..60f,
                    increment = 5f,
                    iconRes = R.drawable.rounded_timer_24,
                    valueFormatter = { "${it.toInt()}s" },
                    modifier = Modifier.highlight(highlightSetting == "island_catch_up_timeout"),
                )
            }
        }

        Text(
            text = stringResource(R.string.duo_section_what_to_show),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_motion_play_24,
                title = stringResource(R.string.duo_show_media_title),
                isChecked = viewModel.isIslandShowMedia.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowMedia(checked)
                },
                onSettingsClick = { showMediaAppSelectionSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_media"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_calendar_today_24,
                title = stringResource(R.string.status_glance_show_calendar_title),
                isChecked = viewModel.isIslandShowCalendar.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !viewModel.isCalendarPermissionGranted.value) {
                        requestingPermissionsFor = Pair(R.string.island_title, listOf("READ_CALENDAR"))
                    } else {
                        viewModel.setIslandShowCalendar(checked)
                    }
                },
                onSettingsClick = { showCalendarOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_calendar"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_pause_24,
                title = stringResource(R.string.feat_conscious_gate_title),
                isChecked = viewModel.isIslandShowConsciousGate.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowConsciousGate(checked)
                },
                onSettingsClick = {
                    val intent = Intent(context, FeatureSettingsActivity::class.java).apply {
                        putExtra("feature", CONSCIOUS_GATE_FEATURE_ID)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_conscious_gate"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_flashlight_on_24,
                title = stringResource(R.string.feat_flashlight_title),
                isChecked = viewModel.isIslandShowFlashlight.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowFlashlight(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_flashlight"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_schedule_24,
                title = stringResource(R.string.island_show_time_battery_title),
                isChecked = viewModel.isIslandShowTimeBattery.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowTimeBattery(checked)
                },
                onSettingsClick = { showTimeBatteryOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_time_battery"),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showMediaAppSelectionSheet) {
        AppSelectionSheet(
            title = stringResource(R.string.duo_media_skip_apps_title),
            onDismissRequest = { showMediaAppSelectionSheet = false },
            onLoadApps = { viewModel.loadIslandMediaApps(it) },
            onSaveApps = { ctx, apps -> viewModel.saveIslandMediaApps(ctx, apps) },
            onAppToggle = { ctx, pkg, enabled -> viewModel.updateIslandMediaAppEnabled(ctx, pkg, enabled) },
            context = context,
        )
    }

    if (showTimeBatteryOptionsSheet) {
        IslandTimeBatteryOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showTimeBatteryOptionsSheet = false },
        )
    }

    if (showCalendarOptionsSheet) {
        StatusGlanceCalendarOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showCalendarOptionsSheet = false },
        )
    }
}
