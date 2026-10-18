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
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.ColorSwatchPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.DuoBatteryLevels
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun BatteryColorOptions(
    viewModel: MainViewModel,
    showIdleColor: Boolean = false,
    showLevels: Boolean = false,
) {
    val view = LocalView.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (showLevels) {
                        LevelSlider(
                            title = stringResource(R.string.duo_battery_low_level_title),
                            level = viewModel.duoBatteryLowLevel.intValue,
                            range = (DuoBatteryLevels.MIN + 1)..DuoBatteryLevels.MAX,
                            onLevelChange = viewModel::setDuoBatteryLowLevel,
                        )
                    }
                    ColorSwatchPicker(
                        selectedColorHex = viewModel.duoBatteryLowColor.value,
                        onColorSelected = { hex ->
                            viewModel.setDuoBatteryLowColor(hex)
                        },
                    )
                }
            }
        }

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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (showLevels) {
                        LevelSlider(
                            title = stringResource(R.string.duo_battery_critical_level_title),
                            level = viewModel.duoBatteryCriticalLevel.intValue,
                            range = DuoBatteryLevels.MIN..(DuoBatteryLevels.MAX - 1),
                            onLevelChange = viewModel::setDuoBatteryCriticalLevel,
                        )
                    }
                    ColorSwatchPicker(
                        selectedColorHex = viewModel.duoBatteryCriticalColor.value,
                        onColorSelected = { hex ->
                            viewModel.setDuoBatteryCriticalColor(hex)
                        },
                    )
                }
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
                    allowAuto = true,
                )
            }
        }

        if (showIdleColor) {
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_battery_android_frame_6_24,
                    title = stringResource(R.string.island_battery_idle_color_title),
                    isChecked = viewModel.isIslandBatteryIdleColorEnabled.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandBatteryIdleColorEnabled(checked)
                    },
                )
                AnimatedVisibility(
                    visible = viewModel.isIslandBatteryIdleColorEnabled.value,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ColorSwatchPicker(
                        selectedColorHex = viewModel.islandBatteryIdleColor.value,
                        onColorSelected = { hex ->
                            viewModel.setIslandBatteryIdleColor(hex)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelSlider(
    title: String,
    level: Int,
    range: IntRange,
    onLevelChange: (Int) -> Unit,
) {
    val view = LocalView.current
    ConfigSliderItem(
        title = title,
        value = level.toFloat(),
        onValueChange = {
            HapticUtil.performUIHaptic(view)
            onLevelChange(it.toInt().coerceIn(range))
        },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        increment = 1f,
        iconRes = R.drawable.rounded_percent_24,
        valueFormatter = { "${it.toInt()}%" },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoBatteryOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
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

            BatteryColorOptions(viewModel, showLevels = true)
        }
    }
}
