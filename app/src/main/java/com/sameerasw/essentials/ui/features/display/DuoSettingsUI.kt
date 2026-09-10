/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: DuoSettingsUI.kt
 * Description: UI settings composable for Duo ambient camera indicator feature.
 */

package com.sameerasw.essentials.ui.features.display

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DuoSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var requestingPermissionsFor by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    if (requestingPermissionsFor != null) {
        val (featureTitle, permKeys) = requestingPermissionsFor!!
        val permissionItems = PermissionUIHelper.getPermissionItems(permKeys, context, viewModel)
        PermissionsBottomSheet(
            onDismissRequest = {
                requestingPermissionsFor = null
                viewModel.check(context)
            },
            featureTitle = featureTitle,
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
                iconRes = R.drawable.rounded_motion_play_24,
                title = stringResource(R.string.duo_enable_title),
                description = stringResource(R.string.duo_enable_desc),
                isChecked = viewModel.isDuoEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !viewModel.isAccessibilityEnabled.value) {
                        requestingPermissionsFor = Pair(R.string.duo_title, listOf("ACCESSIBILITY"))
                    } else {
                        viewModel.setDuoEnabled(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "duo_enabled"),
            )
        }

        Text(
            text = stringResource(R.string.duo_section_position),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_center_focus_strong_24,
                title = stringResource(R.string.duo_auto_detect_title),
                description = stringResource(R.string.duo_auto_detect_desc),
                isChecked = viewModel.isDuoAutoDetect.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setDuoAutoDetect(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "duo_use_auto_detect"),
            )

            AnimatedVisibility(
                visible = !viewModel.isDuoAutoDetect.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ConfigSliderItem(
                        title = stringResource(R.string.duo_camera_offset_x_title),
                        value = viewModel.duoCameraOffsetX.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setDuoCameraOffsetX(it)
                        },
                        valueRange = 0f..100f,
                        increment = 1f,
                        iconRes = R.drawable.rounded_border_left_24,
                        valueFormatter = { "${it.toInt()}%" },
                    )
                    ConfigSliderItem(
                        title = stringResource(R.string.duo_camera_offset_y_title),
                        value = viewModel.duoCameraOffsetY.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setDuoCameraOffsetY(it)
                        },
                        valueRange = 0f..20f,
                        increment = 0.5f,
                        iconRes = R.drawable.rounded_border_top_24,
                        valueFormatter = { "%.1f%%".format(it) },
                    )
                }
            }

            ConfigSliderItem(
                title = stringResource(R.string.duo_camera_size_title),
                value = viewModel.duoCameraSize.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setDuoCameraSize(it)
                },
                valueRange = 0.2f..2.0f,
                increment = 0.05f,
                iconRes = R.drawable.rounded_arrows_outward_24,
                valueFormatter = { "%.2fx".format(it) },
            )
        }

        Text(
            text = stringResource(R.string.duo_section_style),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            ConfigSliderItem(
                title = stringResource(R.string.duo_ring_radius_title),
                value = viewModel.duoRingRadius.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setDuoRingRadius(it)
                },
                valueRange = 0.4f..1.6f,
                increment = 0.05f,
                iconRes = R.drawable.rounded_circle_24,
                valueFormatter = { "%.2fx".format(it) },
            )
            ConfigSliderItem(
                title = stringResource(R.string.duo_arc_thickness_title),
                value = viewModel.duoArcThickness.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setDuoArcThickness(it)
                },
                valueRange = 2f..10f,
                increment = 0.5f,
                iconRes = R.drawable.rounded_line_weight_24,
                valueFormatter = { "${it.toInt()} dp" },
            )
            ConfigSliderItem(
                title = stringResource(R.string.duo_dot_size_title),
                value = viewModel.duoDotSize.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setDuoDotSize(it)
                },
                valueRange = 2f..8f,
                increment = 0.5f,
                iconRes = R.drawable.rounded_circles_24,
                valueFormatter = { "${it.toInt()} dp" },
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
                iconRes = R.drawable.rounded_signal_cellular_alt_24,
                title = stringResource(R.string.duo_show_networks_title),
                description = stringResource(R.string.duo_show_networks_desc),
                isChecked = viewModel.isDuoShowNetworks.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setDuoShowNetworks(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "duo_show_networks"),
            )
        }

        Text(
            text = stringResource(R.string.duo_section_behavior),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_palette_24,
                title = stringResource(R.string.duo_material_you_title),
                description = stringResource(R.string.duo_material_you_desc),
                isChecked = viewModel.isDuoUseMaterialYou.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setDuoUseMaterialYou(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "duo_use_material_you"),
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_off_24,
                title = stringResource(R.string.duo_hide_when_screen_off_title),
                description = stringResource(R.string.duo_hide_when_screen_off_desc),
                isChecked = viewModel.isDuoHideWhenScreenOff.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setDuoHideWhenScreenOff(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "duo_hide_when_screen_off"),
            )
        }
    }
}
