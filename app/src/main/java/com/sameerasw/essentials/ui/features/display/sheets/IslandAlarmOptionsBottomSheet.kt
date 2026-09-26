/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandAlarmOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring Island alarm options.
 */

package com.sameerasw.essentials.ui.features.display.sheets

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
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

private val ALARM_WINDOW_OPTIONS = listOf(4, 8, 12, 24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandAlarmOptionsBottomSheet(
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
                text = stringResource(R.string.island_alarm_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                ConfigPickerItem(
                    title = stringResource(R.string.island_alarm_window_title),
                    selectedValue = stringResource(R.string.island_alarm_window_hours, viewModel.islandAlarmWindowHours.intValue),
                    iconRes = R.drawable.rounded_schedule_24,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ALARM_WINDOW_OPTIONS.forEach { hours ->
                        SegmentedDropdownMenuItem(
                            text = { Text(stringResource(R.string.island_alarm_window_hours, hours)) },
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.setIslandAlarmWindowHours(hours)
                            },
                        )
                    }
                }
            }

            IslandLauncherOnlyToggle(SettingsRepository.KEY_ISLAND_ALARM_LAUNCHER_ONLY)
        }
    }
}
