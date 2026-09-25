/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandSettingsUI.kt
 * Description: UI settings composable for Island dynamic notifications feature.
 */

package com.sameerasw.essentials.ui.features.display

import androidx.compose.foundation.background
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.data.repository.SettingsRepository
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.Button
import android.widget.Toast
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.features.consciousgate.CONSCIOUS_GATE_FEATURE_ID
import com.sameerasw.essentials.ui.features.display.actions.GestureActionPickerSheet
import com.sameerasw.essentials.ui.features.display.actions.HorizontalSlideModeSheet
import com.sameerasw.essentials.ui.features.display.actions.horizontalSlideDescription
import com.sameerasw.essentials.ui.features.display.sheets.IslandAlarmOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandBriefOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandDevicesBatteryBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandNotificationOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandTimeBatteryOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandTimerOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandWeatherOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.StatusGlanceCalendarOptionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

private val ISLAND_PLACEMENT_KEYS = setOf("island_camera_position", "island_use_auto_detect", "island_camera_size", "island_max_width", "island_expanded_width", "island_cutout_gap")
private val ISLAND_VISUALS_KEYS = setOf("island_expanded_scale", "island_font_scale", "island_expanded_roundness", "island_expanded_padding", "island_expanded_top_padding")

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

private val notificationSheetSettings =
    setOf(
        "island_notif_compact_heads_up",
        "island_notif_keep_progress",
        "island_notif_queue",
        "island_notif_tap_to_open",
        "island_catch_up",
        "island_catch_up_timeout",
    )

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
    var showDevicesBatterySheet by remember { mutableStateOf(false) }
    var showTimeBatteryOptionsSheet by remember { mutableStateOf(false) }
    var showTimerOptionsSheet by remember { mutableStateOf(false) }
    var showAlarmOptionsSheet by remember { mutableStateOf(false) }
    var showBriefOptionsSheet by remember { mutableStateOf(highlightSetting == "island_brief_show_alarm") }
    var showNotificationOptionsSheet by remember {
        mutableStateOf(highlightSetting in notificationSheetSettings)
    }
    var showWeatherOptionsSheet by remember { mutableStateOf(false) }
    var pickingGesture by remember { mutableStateOf<String?>(null) }
    var showSlideModeSheet by remember { mutableStateOf(false) }

    val hasShellPermission =
        if (ShellUtils.isRootEnabled(context)) {
            viewModel.isRootPermissionGranted.value
        } else {
            viewModel.isShizukuPermissionGranted.value
        }

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

        val previewSettings = remember { SettingsRepository(context) }
        var previewRing by remember { mutableStateOf(false) }
        var previewStage by remember { mutableStateOf(SettingsRepository.ISLAND_PREVIEW_STAGE_AUTO) }
        var showWhen by remember { mutableStateOf(previewSettings.getIslandShowWhen()) }
        DisposableEffect(Unit) {
            onDispose {
                previewSettings.setIslandPreviewRingEnabled(false)
                previewSettings.setIslandPreviewStage(SettingsRepository.ISLAND_PREVIEW_STAGE_AUTO)
            }
        }

        IslandExpandableSection(
            title = stringResource(R.string.island_section_placement),
            iconRes = R.drawable.rounded_center_focus_strong_24,
            initiallyExpanded = highlightSetting in ISLAND_PLACEMENT_KEYS,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_circle_24,
                title = stringResource(R.string.island_preview_ring_title),
                isChecked = previewRing,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    previewRing = checked
                    previewSettings.setIslandPreviewRingEnabled(checked)
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                    .padding(top = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.island_preview_stage_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                val stages = listOf(
                    SettingsRepository.ISLAND_PREVIEW_STAGE_AUTO,
                    SettingsRepository.ISLAND_PREVIEW_STAGE_PEEK,
                    SettingsRepository.ISLAND_PREVIEW_STAGE_EXPANDED,
                )
                val stageLabels = mapOf(
                    SettingsRepository.ISLAND_PREVIEW_STAGE_AUTO to stringResource(R.string.island_preview_stage_auto),
                    SettingsRepository.ISLAND_PREVIEW_STAGE_PEEK to stringResource(R.string.island_preview_stage_peek),
                    SettingsRepository.ISLAND_PREVIEW_STAGE_EXPANDED to stringResource(R.string.island_preview_stage_expanded),
                )
                SegmentedPicker(
                    items = stages,
                    selectedItem = previewStage,
                    onItemSelected = {
                        previewStage = it
                        previewSettings.setIslandPreviewStage(it)
                    },
                    labelProvider = { stageLabels[it].orEmpty() },
                    title = R.string.island_preview_stage_title,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                    .padding(top = 12.dp)
                    .highlight(highlightSetting == "island_camera_position"),
            ) {
                Text(
                    text = stringResource(R.string.island_camera_position_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                val positions = listOf(
                    SettingsRepository.ISLAND_CAMERA_POSITION_LEFT,
                    SettingsRepository.ISLAND_CAMERA_POSITION_CENTER,
                    SettingsRepository.ISLAND_CAMERA_POSITION_RIGHT,
                )
                val positionLabels = mapOf(
                    SettingsRepository.ISLAND_CAMERA_POSITION_LEFT to stringResource(R.string.island_camera_position_left),
                    SettingsRepository.ISLAND_CAMERA_POSITION_CENTER to stringResource(R.string.island_camera_position_center),
                    SettingsRepository.ISLAND_CAMERA_POSITION_RIGHT to stringResource(R.string.island_camera_position_right),
                )
                SegmentedPicker(
                    items = positions,
                    selectedItem = viewModel.islandCameraPosition.value,
                    onItemSelected = { viewModel.setIslandCameraPosition(it) },
                    labelProvider = { positionLabels[it].orEmpty() },
                    title = R.string.island_camera_position_title,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LaunchedEffect(Unit) {
                if (viewModel.isIslandAutoDetect.value) viewModel.autoAlignIslandWithCamera(context)
            }

            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.autoAlignIslandWithCamera(context)
                    Toast.makeText(context, R.string.island_auto_align_toast, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .highlight(highlightSetting == "island_use_auto_detect"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_center_focus_strong_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(R.string.island_auto_align_camera))
            }

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
                title = stringResource(R.string.island_expanded_scale_title),
                description = stringResource(R.string.island_expanded_scale_desc),
                value = viewModel.islandExpandedScale.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedScale(it)
                },
                valueRange = 1f..1.3f,
                increment = 0.02f,
                iconRes = R.drawable.rounded_magnify_fullscreen_24,
                valueFormatter = { "${(it * 100).toInt()}%" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_scale"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_font_scale_title),
                value = viewModel.islandFontScale.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandFontScale(it)
                },
                valueRange = 0.8f..1.3f,
                increment = 0.05f,
                iconRes = R.drawable.rounded_format_size_24,
                valueFormatter = { "${(it * 100).toInt()}%" },
                modifier = Modifier.highlight(highlightSetting == "island_font_scale"),
            )

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

            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_bottom_padding_title),
                value = viewModel.islandExpandedBottomPadding.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedBottomPadding(it)
                },
                valueRange = 0f..40f,
                increment = 2f,
                iconRes = R.drawable.rounded_vertical_align_bottom_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_bottom_padding"),
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
                valueRange = 2f..60f,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                    .padding(top = 12.dp)
                    .highlight(highlightSetting == "island_show_when" || highlightSetting == "island_hide_when_screen_off"),
            ) {
                Text(
                    text = stringResource(R.string.island_show_when_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                val showWhenOptions = listOf(
                    SettingsRepository.ISLAND_SHOW_WHEN_UNLOCKED,
                    SettingsRepository.ISLAND_SHOW_WHEN_SCREEN_ON,
                    SettingsRepository.ISLAND_SHOW_WHEN_ALWAYS,
                )
                val showWhenLabels = mapOf(
                    SettingsRepository.ISLAND_SHOW_WHEN_UNLOCKED to stringResource(R.string.island_show_when_unlocked),
                    SettingsRepository.ISLAND_SHOW_WHEN_SCREEN_ON to stringResource(R.string.island_show_when_screen_on),
                    SettingsRepository.ISLAND_SHOW_WHEN_ALWAYS to stringResource(R.string.island_show_when_always),
                )
                SegmentedPicker(
                    items = showWhenOptions,
                    selectedItem = showWhen,
                    onItemSelected = {
                        showWhen = it
                        previewSettings.setIslandShowWhen(it)
                    },
                    labelProvider = { showWhenLabels[it].orEmpty() },
                    title = R.string.island_show_when_title,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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

            IconToggleItem(
                iconRes = R.drawable.rounded_motion_play_24,
                title = stringResource(R.string.island_line_peek_title),
                description = stringResource(R.string.island_line_peek_desc),
                isChecked = viewModel.isIslandLineStageEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandLineStageEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_line_stage_enabled"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_visibility_off_24,
                title = stringResource(R.string.island_hide_in_owner_app_title),
                isChecked = viewModel.isIslandHideInOwnerApp.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandHideInOwnerApp(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_hide_in_owner_app"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_pinch_24,
                title = stringResource(R.string.island_hide_on_shade_title),
                isChecked = viewModel.isIslandHideOnShade.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandHideOnShade(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_hide_on_shade"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_touch_app_24,
                title = stringResource(R.string.island_dismiss_on_outside_title),
                isChecked = viewModel.isIslandDismissOnOutside.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandDismissOnOutside(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_dismiss_on_outside"),
            )

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
                iconRes = R.drawable.rounded_visibility_off_24,
                title = stringResource(R.string.island_dynamic_hide_status_bar_title),
                isChecked = viewModel.isIslandDynamicHideStatusBar.value && hasShellPermission,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !hasShellPermission) {
                        requestingPermissionsFor =
                            Pair(
                                R.string.island_dynamic_hide_status_bar_title,
                                listOf(if (ShellUtils.isRootEnabled(context)) "ROOT" else "SHIZUKU"),
                            )
                    } else {
                        viewModel.setIslandDynamicHideStatusBar(checked, context)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "island_dynamic_hide_status_bar"),
            )
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
                iconRes = R.drawable.rounded_call_24,
                title = stringResource(R.string.island_show_calls_title),
                isChecked = viewModel.isIslandShowCalls.value && PermissionUtils.hasCallPermissions(context),
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !PermissionUtils.hasCallPermissions(context)) {
                        requestingPermissionsFor = Pair(R.string.island_title, listOf("READ_PHONE_STATE", "ANSWER_PHONE_CALLS", "READ_CONTACTS"))
                    } else {
                        viewModel.setIslandShowCalls(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_calls"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = stringResource(R.string.island_section_notifications),
                isChecked = viewModel.isIslandShowNotifications.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowNotifications(checked)
                },
                onSettingsClick = { showNotificationOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_notifications"),
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
                iconRes = R.drawable.rounded_timer_24,
                title = stringResource(R.string.island_show_timers_title),
                isChecked = viewModel.isIslandShowTimers.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowTimers(checked)
                },
                onSettingsClick = { showTimerOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_timers"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_partly_cloudy_day_24,
                title = stringResource(R.string.lock_screen_clock_weather),
                isChecked = viewModel.isIslandShowWeather.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowWeather(checked)
                    if (checked && SettingsRepository(context).getWeatherApiKey() == null) showWeatherOptionsSheet = true
                },
                onSettingsClick = { showWeatherOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_weather"),
            )

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
                iconRes = R.drawable.rounded_coffee_24,
                title = stringResource(R.string.feat_caffeinate_title),
                isChecked = viewModel.isIslandShowCaffeinate.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowCaffeinate(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_caffeinate"),
            )

            IconToggleItem(
                iconRes = R.drawable.round_navigation_24,
                title = stringResource(R.string.feat_location_reached_title),
                isChecked = viewModel.isIslandShowTravel.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowTravel(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_travel"),
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
                iconRes = R.drawable.rounded_volume_up_24,
                title = stringResource(R.string.island_show_sound_mode_title),
                isChecked = viewModel.isIslandShowSoundMode.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowSoundMode(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_sound_mode"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_alarm_24,
                title = stringResource(R.string.island_show_alarm_title),
                isChecked = viewModel.isIslandShowAlarm.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowAlarm(checked)
                },
                onSettingsClick = { showAlarmOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_alarm"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_android_wifi_3_bar_24,
                title = stringResource(R.string.island_show_network_title),
                isChecked = viewModel.isIslandShowNetwork.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowNetwork(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_network"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_bluetooth_24,
                title = stringResource(R.string.island_show_devices_title),
                isChecked = viewModel.isIslandShowDevices.value && PermissionUtils.hasBluetoothPermission(context),
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !PermissionUtils.hasBluetoothPermission(context)) {
                        requestingPermissionsFor = Pair(R.string.island_title, listOf("BLUETOOTH_CONNECT", "BLUETOOTH_SCAN"))
                    } else {
                        viewModel.setIslandShowDevices(checked)
                    }
                },
                onSettingsClick = { showDevicesBatterySheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_devices"),
            )
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_calendar_today_24,
                title = stringResource(R.string.island_brief_title),
                description = stringResource(R.string.island_brief_desc),
                isChecked = viewModel.isIslandBriefEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandBriefEnabled(checked)
                },
                onSettingsClick = { showBriefOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_brief_enabled"),
            )
        }

        Text(
            text = stringResource(R.string.duo_section_actions),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_front_hand_24,
                title = stringResource(R.string.duo_action_long_press_title),
                description = if (viewModel.isIslandBriefEnabled.value) {
                    stringResource(R.string.island_brief_title)
                } else {
                    viewModel.islandLongPressAction.value?.let { stringResource(it.title) }
                        ?: stringResource(R.string.duo_action_none)
                },
                showToggle = false,
                enabled = !viewModel.isIslandBriefEnabled.value,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    pickingGesture = "long_press"
                },
                modifier = Modifier.highlight(highlightSetting == "island_long_press_action"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_favorite_24,
                title = stringResource(R.string.island_like_while_playing_title),
                isChecked = viewModel.isIslandLikeWhilePlaying.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandLikeWhilePlayingEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_like_while_playing"),
            )
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_compare_arrows_24,
                title = stringResource(R.string.duo_action_horizontal_slide_title),
                description = horizontalSlideDescription(viewModel.islandSlideMode.value, viewModel.isIslandSlideTrack.value),
                showToggle = false,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    showSlideModeSheet = true
                },
                modifier = Modifier.highlight(highlightSetting == "island_slide_mode"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_skip_next_24,
                title = stringResource(R.string.duo_action_horizontal_slide_track),
                description = stringResource(R.string.duo_action_horizontal_slide_track_desc),
                isChecked = viewModel.isIslandSlideTrack.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandSlideTrackEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_slide_track"),
            )

            AnimatedVisibility(
                visible = viewModel.isIslandSlideTrack.value || viewModel.islandSlideMode.value == "sound_mode",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_compare_arrows_24,
                    title = stringResource(R.string.duo_slide_mirror_direction_title),
                    isChecked = viewModel.isIslandSlideInvertDirection.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandSlideInvertDirection(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_slide_invert_direction"),
                )
            }
        }

        AnimatedVisibility(
            visible = !viewModel.isIslandShowTimeBattery.value,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                text = stringResource(R.string.island_gesture_visuals_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        AnimatedVisibility(
            visible = viewModel.isDuoEnabled.value && viewModel.isIslandEnabled.value,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_link_24,
                    title = stringResource(R.string.island_combine_with_duo),
                    isChecked = viewModel.isDuoIslandCombined.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setDuoIslandCombined(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "duo_island_combined"),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (pickingGesture != null) {
        GestureActionPickerSheet(
            viewModel = viewModel,
            currentAction = viewModel.islandLongPressAction.value,
            onActionSelected = viewModel::setIslandLongPressAction,
            onPickerClosed = { pickingGesture = null },
        )
    }

    if (showSlideModeSheet) {
        HorizontalSlideModeSheet(
            mode = viewModel.islandSlideMode.value,
            onModeSelected = viewModel::setIslandSlideMode,
            onDismissRequest = { showSlideModeSheet = false },
        )
    }

    if (showMediaAppSelectionSheet) {
        AppSelectionSheet(
            title = stringResource(R.string.island_media_config_title),
            onDismissRequest = { showMediaAppSelectionSheet = false },
            onLoadApps = { viewModel.loadIslandMediaApps(it) },
            onSaveApps = { ctx, apps -> viewModel.saveIslandMediaApps(ctx, apps) },
            onAppToggle = { ctx, pkg, enabled -> viewModel.updateIslandMediaAppEnabled(ctx, pkg, enabled) },
            context = context,
            headerContent = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                        if (viewModel.isIslandLineStageEnabled.value) {
                            IconToggleItem(
                                iconRes = R.drawable.rounded_music_note_24,
                                title = stringResource(R.string.island_media_peek_song_change_title),
                                isChecked = viewModel.isIslandMediaPeekSongChange.value,
                                onCheckedChange = { checked ->
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.setIslandMediaPeekSongChange(checked)
                                },
                                modifier = Modifier.highlight(highlightSetting == "island_media_peek_song_change"),
                            )
                        }
                        IconToggleItem(
                            iconRes = R.drawable.rounded_pause_24,
                            title = stringResource(R.string.island_media_keep_when_paused_title),
                            isChecked = viewModel.isIslandMediaKeepWhenPaused.value,
                            onCheckedChange = { checked ->
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.setIslandMediaKeepWhenPaused(checked)
                            },
                            modifier = Modifier.highlight(highlightSetting == "island_media_keep_when_paused"),
                        )
                        IconToggleItem(
                            iconRes = R.drawable.rounded_skip_previous_24,
                            title = stringResource(R.string.island_media_show_previous_title),
                            isChecked = viewModel.isIslandMediaShowPrevious.value,
                            onCheckedChange = { checked ->
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.setIslandMediaShowPrevious(checked)
                            },
                            modifier = Modifier.highlight(highlightSetting == "island_media_show_previous"),
                        )
                    }
                    Text(
                        text = stringResource(R.string.duo_media_skip_apps_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            },
        )
    }

    if (showWeatherOptionsSheet) {
        IslandWeatherOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showWeatherOptionsSheet = false },
        )
    }

    if (showNotificationOptionsSheet) {
        IslandNotificationOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showNotificationOptionsSheet = false },
            highlightSetting = highlightSetting,
        )
    }

    if (showBriefOptionsSheet) {
        IslandBriefOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showBriefOptionsSheet = false },
            highlightSetting = highlightSetting,
        )
    }

    if (showAlarmOptionsSheet) {
        IslandAlarmOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showAlarmOptionsSheet = false },
        )
    }

    if (showTimerOptionsSheet) {
        IslandTimerOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showTimerOptionsSheet = false },
        )
    }

    if (showTimeBatteryOptionsSheet) {
        IslandTimeBatteryOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showTimeBatteryOptionsSheet = false },
        )
    }

    if (showDevicesBatterySheet) {
        IslandDevicesBatteryBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showDevicesBatterySheet = false },
        )
    }

    if (showCalendarOptionsSheet) {
        StatusGlanceCalendarOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showCalendarOptionsSheet = false },
            allowIconEdit = true,
        )
    }
}
