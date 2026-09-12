/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: DuoBatteryOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring Duo battery threshold and charging color options.
 */

package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.ColorSwatchPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoBatteryOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.duo_battery_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            // Low Battery (< 20%)
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_battery_alert_24,
                    title = stringResource(R.string.duo_battery_low_color_title),
                    isChecked = viewModel.isDuoBatteryLowColorEnabled.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setDuoBatteryLowColorEnabled(checked)
                    },
                )
                AnimatedVisibility(
                    visible = viewModel.isDuoBatteryLowColorEnabled.value,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ColorSwatchPicker(
                        selectedColorHex = viewModel.duoBatteryLowColor.value,
                        onColorSelected = { hex ->
                            viewModel.setDuoBatteryLowColor(hex)
                        },
                    )
                }
            }

            // Critically Low Battery (< 10%)
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_battery_android_frame_alert_24,
                    title = stringResource(R.string.duo_battery_critical_color_title),
                    isChecked = viewModel.isDuoBatteryCriticalColorEnabled.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setDuoBatteryCriticalColorEnabled(checked)
                    },
                )
                AnimatedVisibility(
                    visible = viewModel.isDuoBatteryCriticalColorEnabled.value,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ColorSwatchPicker(
                        selectedColorHex = viewModel.duoBatteryCriticalColor.value,
                        onColorSelected = { hex ->
                            viewModel.setDuoBatteryCriticalColor(hex)
                        },
                    )
                }
            }

            // Battery Saver
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_battery_android_frame_plus_24,
                    title = stringResource(R.string.duo_battery_power_save_color_title),
                    isChecked = viewModel.isDuoBatteryPowerSaveColorEnabled.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setDuoBatteryPowerSaveColorEnabled(checked)
                    },
                )
                AnimatedVisibility(
                    visible = viewModel.isDuoBatteryPowerSaveColorEnabled.value,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ColorSwatchPicker(
                        selectedColorHex = viewModel.duoBatteryPowerSaveColor.value,
                        onColorSelected = { hex ->
                            viewModel.setDuoBatteryPowerSaveColor(hex)
                        },
                    )
                }
            }

            // Charging
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_bolt_24,
                    title = stringResource(R.string.duo_battery_charging_color_title),
                    isChecked = viewModel.isDuoBatteryChargingColorEnabled.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setDuoBatteryChargingColorEnabled(checked)
                    },
                )
                AnimatedVisibility(
                    visible = viewModel.isDuoBatteryChargingColorEnabled.value,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ColorSwatchPicker(
                        selectedColorHex = viewModel.duoBatteryChargingColor.value,
                        onColorSelected = { hex ->
                            viewModel.setDuoBatteryChargingColor(hex)
                        },
                    )
                }
            }
        }
    }
}
