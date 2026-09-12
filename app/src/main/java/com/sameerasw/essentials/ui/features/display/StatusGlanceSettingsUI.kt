/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: StatusGlanceSettingsUI.kt
 * Description: UI settings composable for Status Glance ambient indicator feature.
 */

package com.sameerasw.essentials.ui.features.display

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sameerasw.essentials.domain.model.AppPermission
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusGlanceSettingsUI(
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
                title = stringResource(R.string.status_glance_enable_title),
                description = stringResource(R.string.status_glance_enable_desc),
                iconRes = R.drawable.rounded_motion_play_24,
                isChecked = viewModel.isStatusGlanceEnabled.value,
                onCheckedChange = { isChecked ->
                    HapticUtil.performUIHaptic(view)
                    if (isChecked && !viewModel.isAccessibilityEnabled.value) {
                        requestingPermissionsFor = Pair(
                            R.string.feat_status_glance_title,
                            listOf("ACCESSIBILITY")
                        )
                    } else {
                        viewModel.setStatusGlanceEnabled(isChecked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "status_glance_enabled"),
            )
        }

        AnimatedVisibility(
            visible = viewModel.isStatusGlanceEnabled.value,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.status_glance_section_position),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )

                RoundedCardContainer(
                    spacing = 2.dp,
                    cornerRadius = 24.dp,
                ) {
                    IconToggleItem(
                        title = stringResource(R.string.status_glance_auto_detect_button_title),
                        description = stringResource(R.string.status_glance_auto_detect_button_desc),
                        iconRes = R.drawable.rounded_center_focus_strong_24,
                        showToggle = false,
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.autoDetectStatusGlancePosition(context)
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_auto_detect"),
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.status_glance_offset_x_title),
                        value = viewModel.statusGlanceOffsetX.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceOffsetX(it)
                        },
                        valueRange = 0f..100f,
                        increment = 1f,
                        iconRes = R.drawable.rounded_border_left_24,
                        valueFormatter = { "${it.toInt()}%" },
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.status_glance_offset_y_title),
                        value = viewModel.statusGlanceOffsetY.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceOffsetY(it)
                        },
                        valueRange = 0f..20f,
                        increment = 0.5f,
                        iconRes = R.drawable.rounded_border_top_24,
                        valueFormatter = { "%.1f%%".format(it) },
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.status_glance_max_width_title),
                        value = viewModel.statusGlanceMaxWidth.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceMaxWidth(it)
                        },
                        valueRange = 80f..350f,
                        increment = 10f,
                        iconRes = R.drawable.rounded_arrows_outward_24,
                        valueFormatter = { "${it.toInt()} dp" },
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.status_glance_font_size_title),
                        value = viewModel.statusGlanceFontSize.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceFontSize(it)
                        },
                        valueRange = 9f..22f,
                        increment = 1f,
                        iconRes = R.drawable.rounded_mobile_text_24,
                        valueFormatter = { "${it.toInt()} sp" },
                    )
                }

                Text(
                    text = stringResource(R.string.status_glance_section_what_to_show),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )

                RoundedCardContainer(
                    spacing = 2.dp,
                    cornerRadius = 24.dp,
                ) {
                    IconToggleItem(
                        title = stringResource(R.string.status_glance_show_flashlight_title),
                        description = stringResource(R.string.status_glance_show_flashlight_desc),
                        iconRes = R.drawable.rounded_flashlight_on_24,
                        isChecked = viewModel.isStatusGlanceShowFlashlight.value,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceShowFlashlight(it)
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_show_flashlight"),
                    )

                    IconToggleItem(
                        title = stringResource(R.string.status_glance_show_calendar_title),
                        description = stringResource(R.string.status_glance_show_calendar_desc),
                        iconRes = R.drawable.rounded_calendar_today_24,
                        isChecked = viewModel.isStatusGlanceShowCalendar.value,
                        onCheckedChange = { isChecked ->
                            HapticUtil.performUIHaptic(view)
                            if (isChecked && !viewModel.isCalendarPermissionGranted.value) {
                                requestingPermissionsFor = Pair(
                                    R.string.status_glance_show_calendar_title,
                                    listOf(AppPermission.READ_CALENDAR.key)
                                )
                            }
                            viewModel.setStatusGlanceShowCalendar(isChecked)
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_show_calendar"),
                    )

                    IconToggleItem(
                        title = stringResource(R.string.status_glance_show_media_title),
                        description = stringResource(R.string.status_glance_show_media_desc),
                        iconRes = R.drawable.rounded_motion_play_24,
                        isChecked = viewModel.isStatusGlanceShowMedia.value,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceShowMedia(it)
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_show_media"),
                    )

                    IconToggleItem(
                        title = stringResource(R.string.status_glance_show_time_title),
                        description = stringResource(R.string.status_glance_show_time_desc),
                        iconRes = R.drawable.rounded_schedule_24,
                        isChecked = viewModel.isStatusGlanceShowTime.value,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceShowTime(it)
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_show_time"),
                    )
                }

                Text(
                    text = stringResource(R.string.status_glance_section_appearance),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )

                RoundedCardContainer(
                    spacing = 2.dp,
                    cornerRadius = 24.dp,
                ) {
                    IconToggleItem(
                        title = stringResource(R.string.status_glance_background_pill_title),
                        description = stringResource(R.string.status_glance_background_pill_desc),
                        iconRes = R.drawable.rounded_circle_24,
                        isChecked = viewModel.isStatusGlanceBackgroundPill.value,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceBackgroundPill(it)
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_background_pill"),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
