/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandTimeOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring the Island time indicator.
 */

package com.sameerasw.essentials.ui.features.display.sheets

import com.sameerasw.essentials.island.IslandTimeFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.text.format.DateFormat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandTimeOptionsBottomSheet(
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings = remember { SettingsRepository(context) }
    var showSeconds by remember { mutableStateOf(settings.getBoolean(SettingsRepository.KEY_ISLAND_TIME_SHOW_SECONDS, false)) }
    var timeFormat by remember {
        mutableStateOf(settings.getString(SettingsRepository.KEY_ISLAND_TIME_FORMAT, null) ?: SettingsRepository.ISLAND_TIME_FORMAT_SYSTEM)
    }
    var period by remember {
        mutableStateOf(settings.getString(SettingsRepository.KEY_ISLAND_TIME_PERIOD, null) ?: SettingsRepository.ISLAND_TIME_PERIOD_NONE)
    }
    var leadingZero by remember { mutableStateOf(IslandTimeFormat.leadingZero(context, settings)) }
    val is12h = when (timeFormat) {
        SettingsRepository.ISLAND_TIME_FORMAT_12H -> true
        SettingsRepository.ISLAND_TIME_FORMAT_24H -> false
        else -> !DateFormat.is24HourFormat(context)
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
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                val formatLabels = mapOf(
                    SettingsRepository.ISLAND_TIME_FORMAT_SYSTEM to stringResource(R.string.island_time_format_system),
                    SettingsRepository.ISLAND_TIME_FORMAT_12H to stringResource(R.string.island_time_format_12h),
                    SettingsRepository.ISLAND_TIME_FORMAT_24H to stringResource(R.string.island_time_format_24h),
                )
                SegmentedPicker(
                    items = formatLabels.keys.toList(),
                    selectedItem = timeFormat,
                    onItemSelected = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        timeFormat = it
                        settings.putString(SettingsRepository.KEY_ISLAND_TIME_FORMAT, it)
                    },
                    labelProvider = { formatLabels[it].orEmpty() },
                    title = R.string.island_time_format_title,
                    modifier = Modifier.fillMaxWidth(),
                )
                AnimatedVisibility(
                    visible = is12h,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val periodLabels = mapOf(
                        SettingsRepository.ISLAND_TIME_PERIOD_NONE to stringResource(R.string.island_time_period_none),
                        SettingsRepository.ISLAND_TIME_PERIOD_AM_PM to stringResource(R.string.island_time_period_am_pm),
                    )
                    SegmentedPicker(
                        items = periodLabels.keys.toList(),
                        selectedItem = period,
                        onItemSelected = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            period = it
                            settings.putString(SettingsRepository.KEY_ISLAND_TIME_PERIOD, it)
                        },
                        labelProvider = { periodLabels[it].orEmpty() },
                        title = R.string.island_time_period_title,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_schedule_24,
                        title = stringResource(R.string.island_time_leading_zero_title),
                        isChecked = leadingZero,
                        onCheckedChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            leadingZero = it
                            settings.putBoolean(SettingsRepository.KEY_ISLAND_TIME_LEADING_ZERO, it)
                        },
                    )
                }
            }
            RoundedCardContainer {
                IconToggleItem(
                    iconRes = R.drawable.rounded_schedule_24,
                    title = stringResource(R.string.island_time_show_seconds_title),
                    isChecked = showSeconds,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showSeconds = it
                        settings.putBoolean(SettingsRepository.KEY_ISLAND_TIME_SHOW_SECONDS, it)
                    },
                )
                IslandPrefToggle(
                    settingKey = SettingsRepository.KEY_ISLAND_TIME_DAY_SUFFIX,
                    iconRes = R.drawable.rounded_schedule_24,
                    title = stringResource(R.string.island_time_day_suffix_title),
                )
                if (settings.getBoolean(SettingsRepository.KEY_ISLAND_CHIPS, false)) {
                    IslandPrefToggle(
                        settingKey = SettingsRepository.KEY_ISLAND_TIME_CHIP,
                        iconRes = R.drawable.rounded_palette_24,
                        title = stringResource(R.string.island_time_chip_title),
                    )
                }
            }
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                IslandPrefToggle(
                    settingKey = SettingsRepository.KEY_ISLAND_TIME_TAP_OPENS_CLOCK,
                    iconRes = R.drawable.rounded_alarm_24,
                    title = stringResource(R.string.island_time_tap_opens_clock_title),
                    description = stringResource(R.string.island_time_tap_opens_clock_desc),
                    defaultValue = true,
                )
            }
            IslandLauncherOnlyToggle(SettingsRepository.KEY_ISLAND_TIME_BATTERY_LAUNCHER_ONLY)
            if (rememberIslandShowsWhileLocked()) {
                RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                    IslandPrefToggle(
                        settingKey = SettingsRepository.KEY_ISLAND_TIME_HIDE_LOCKED,
                        iconRes = R.drawable.rounded_mobile_lock_portrait_24,
                        title = stringResource(R.string.island_hide_when_locked_title),
                    )
                }
            }
        }
    }
}


@Composable
fun IslandBatteryOptions(
    viewModel: MainViewModel,
    onlyLow: Boolean,
    onOnlyLowChange: (Boolean) -> Unit,
    showIconConditional: Boolean = false,
    devices: Boolean = false,
) {
    val view = LocalView.current
    val currentStyle = if (devices) viewModel.islandDevicesBatteryStyle.value else viewModel.islandBatteryStyle.value
    val setStyle: (String) -> Unit = if (devices) viewModel::setIslandDevicesBatteryStyle else viewModel::setIslandBatteryStyle
    val styleLabel = if (currentStyle == SettingsRepository.ISLAND_BATTERY_STYLE_ICON) {
        stringResource(R.string.island_battery_style_icon)
    } else {
        stringResource(R.string.island_battery_style_ring)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_battery_android_frame_alert_24,
                title = stringResource(R.string.island_battery_only_low_title),
                isChecked = onlyLow,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    onOnlyLowChange(checked)
                },
            )
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
                        setStyle(SettingsRepository.ISLAND_BATTERY_STYLE_RING)
                    },
                )
                SegmentedDropdownMenuItem(
                    text = { Text(stringResource(R.string.island_battery_style_icon)) },
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        setStyle(SettingsRepository.ISLAND_BATTERY_STYLE_ICON)
                    },
                )
            }
            if (showIconConditional) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_filter_alt_24,
                    title = stringResource(R.string.island_battery_icon_conditional_title),
                    isChecked = viewModel.isIslandBatteryIconConditional.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandBatteryIconConditional(checked)
                    },
                )
            }
        }

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_percent_24,
                title = stringResource(R.string.island_battery_percentage_title),
                isChecked = viewModel.isIslandBatteryPercentageEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandBatteryPercentageEnabled(checked)
                },
            )
            AnimatedVisibility(
                visible = viewModel.isIslandBatteryPercentageEnabled.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_filter_alt_24,
                    title = stringResource(R.string.island_battery_percentage_conditional_title),
                    isChecked = viewModel.isIslandBatteryPercentageConditional.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandBatteryPercentageConditional(checked)
                    },
                )
            }
        }

        BatteryColorOptions(viewModel, showIdleColor = true, showLevels = true)
    }
}
