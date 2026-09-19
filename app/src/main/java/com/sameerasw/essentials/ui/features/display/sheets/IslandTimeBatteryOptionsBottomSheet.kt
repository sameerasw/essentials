/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandTimeBatteryOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring the Island time and battery indicator style.
 */

package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandTimeBatteryOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current
    val styleLabel = if (viewModel.islandBatteryStyle.value == SettingsRepository.ISLAND_BATTERY_STYLE_ICON) {
        stringResource(R.string.island_battery_style_icon)
    } else {
        stringResource(R.string.island_battery_style_ring)
    }

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RoundedCardContainer {
                ConfigPickerItem(
                    title = stringResource(R.string.island_battery_style_title),
                    selectedValue = styleLabel,
                    iconRes = R.drawable.rounded_battery_android_frame_3_24,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SegmentedDropdownMenuItem(
                        text = { Text(stringResource(R.string.island_battery_style_ring)) },
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.setIslandBatteryStyle(SettingsRepository.ISLAND_BATTERY_STYLE_RING)
                        },
                    )
                    SegmentedDropdownMenuItem(
                        text = { Text(stringResource(R.string.island_battery_style_icon)) },
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.setIslandBatteryStyle(SettingsRepository.ISLAND_BATTERY_STYLE_ICON)
                        },
                    )
                }
            }

            BatteryColorOptions(viewModel)
        }
    }
}
