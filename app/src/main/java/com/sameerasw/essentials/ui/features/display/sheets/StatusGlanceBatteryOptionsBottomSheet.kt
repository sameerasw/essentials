/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: StatusGlanceBatteryOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring Status Glance battery display mode, icon size, and dynamic visibility conditions.
 */

package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusGlanceBatteryOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.status_glance_battery_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            Text(
                text = stringResource(R.string.status_glance_battery_display_mode_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )

            val modes = listOf("icon", "percentage", "both")
            val icons = listOf(
                R.drawable.battery_android_frame_full_24px,
                R.drawable.rounded_numbers_24,
                R.drawable.battery_android_frame_plus_24px,
            )
            val labels = listOf(
                stringResource(R.string.status_glance_battery_mode_icon),
                stringResource(R.string.status_glance_battery_mode_percentage),
                stringResource(R.string.status_glance_battery_mode_both),
            )

            RoundedCardContainer {
                Row(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                        )
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    modes.forEachIndexed { index, mode ->
                        ToggleButton(
                            checked = viewModel.statusGlanceBatteryDisplayMode.value == mode,
                            onCheckedChange = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.setStatusGlanceBatteryDisplayMode(mode)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = icons[index]),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = labels[index],
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = viewModel.statusGlanceBatteryDisplayMode.value != "percentage",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                RoundedCardContainer(
                    spacing = 2.dp,
                    cornerRadius = 24.dp,
                ) {
                    ConfigSliderItem(
                        title = stringResource(R.string.status_glance_battery_icon_size_title),
                        value = viewModel.statusGlanceBatteryIconSize.floatValue,
                        onValueChange = { viewModel.setStatusGlanceBatteryIconSize(it) },
                        valueRange = 10f..24f,
                        increment = 1f,
                        iconRes = R.drawable.battery_android_frame_full_24px,
                        valueFormatter = { "${it.toInt()} dp" },
                    )
                }
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
                    iconRes = R.drawable.battery_android_frame_alert_24px,
                    title = stringResource(R.string.status_glance_battery_show_low_title),
                    isChecked = viewModel.isStatusGlanceBatteryShowLow.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setStatusGlanceBatteryShowLow(checked)
                    },
                )

                IconToggleItem(
                    iconRes = R.drawable.battery_android_frame_bolt_24px,
                    title = stringResource(R.string.status_glance_battery_show_charging_title),
                    isChecked = viewModel.isStatusGlanceBatteryShowCharging.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setStatusGlanceBatteryShowCharging(checked)
                    },
                )

                IconToggleItem(
                    iconRes = R.drawable.battery_android_frame_full_24px,
                    title = stringResource(R.string.status_glance_battery_show_full_title),
                    isChecked = viewModel.isStatusGlanceBatteryShowFull.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setStatusGlanceBatteryShowFull(checked)
                    },
                )

                IconToggleItem(
                    iconRes = R.drawable.battery_android_frame_4_24px,
                    title = stringResource(R.string.status_glance_battery_show_otherwise_title),
                    isChecked = viewModel.isStatusGlanceBatteryShowOtherwise.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setStatusGlanceBatteryShowOtherwise(checked)
                    },
                )
            }
        }
    }
}

