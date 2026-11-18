/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandStarlinkOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring the Island Starlink plugin.
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
import androidx.compose.runtime.Composable
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
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.island.plugins.starlink.StarlinkSpeedFormat
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandStarlinkOptionsBottomSheet(
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings = remember { SettingsRepository(context) }
    var showInCompact by remember { mutableStateOf(settings.getBoolean(SettingsRepository.KEY_ISLAND_STARLINK_COMPACT, true)) }
    var chip by remember { mutableStateOf(settings.getBoolean(SettingsRepository.KEY_ISLAND_STARLINK_CHIP, false)) }
    val chipsAvailable = remember { settings.getBoolean(SettingsRepository.KEY_ISLAND_CHIPS, false) }
    var showSpeeds by remember { mutableStateOf(settings.getBoolean(SettingsRepository.KEY_ISLAND_STARLINK_SPEEDS, false)) }
    var speedMode by remember {
        mutableStateOf(settings.getString(SettingsRepository.KEY_ISLAND_STARLINK_SPEED_MODE, null) ?: StarlinkSpeedFormat.MODE_DOWN)
    }
    var hideArrow by remember { mutableStateOf(settings.getBoolean(SettingsRepository.KEY_ISLAND_STARLINK_HIDE_ARROW, false)) }
    var speedUnit by remember {
        mutableStateOf(settings.getString(SettingsRepository.KEY_ISLAND_STARLINK_SPEED_UNIT, null) ?: StarlinkSpeedFormat.UNIT_AUTO)
    }
    var hideUnit by remember { mutableStateOf(settings.getBoolean(SettingsRepository.KEY_ISLAND_STARLINK_HIDE_UNIT, false)) }
    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_dock_24,
                    title = stringResource(R.string.island_starlink_compact_title),
                    isChecked = showInCompact,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showInCompact = it
                        settings.putBoolean(SettingsRepository.KEY_ISLAND_STARLINK_COMPACT, it)
                    },
                )
                AnimatedVisibility(
                    visible = showInCompact && chipsAvailable,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_palette_24,
                        title = stringResource(R.string.island_time_chip_title),
                        isChecked = chip,
                        onCheckedChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            chip = it
                            settings.putBoolean(SettingsRepository.KEY_ISLAND_STARLINK_CHIP, it)
                        },
                    )
                }
                AnimatedVisibility(
                    visible = showInCompact,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_speed_24,
                        title = stringResource(R.string.island_starlink_speeds_title),
                        isChecked = showSpeeds,
                        onCheckedChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            showSpeeds = it
                            settings.putBoolean(SettingsRepository.KEY_ISLAND_STARLINK_SPEEDS, it)
                        },
                    )
                }
            }
            AnimatedVisibility(
                visible = showInCompact && showSpeeds,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                    val labels = mapOf(
                        StarlinkSpeedFormat.MODE_DOWN to stringResource(R.string.island_dismiss_direction_down),
                        StarlinkSpeedFormat.MODE_UP to stringResource(R.string.island_starlink_speed_up),
                        StarlinkSpeedFormat.MODE_DYNAMIC to stringResource(R.string.island_icon_color_dynamic),
                        StarlinkSpeedFormat.MODE_COMBINED to stringResource(R.string.island_starlink_speed_combined),
                    )
                    SegmentedPicker(
                        items = labels.keys.toList(),
                        selectedItem = speedMode,
                        onItemSelected = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            speedMode = it
                            settings.putString(SettingsRepository.KEY_ISLAND_STARLINK_SPEED_MODE, it)
                        },
                        labelProvider = { labels[it].orEmpty() },
                        title = R.string.island_starlink_speed_mode_title,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AnimatedVisibility(
                        visible = speedMode == StarlinkSpeedFormat.MODE_DOWN || speedMode == StarlinkSpeedFormat.MODE_UP,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        IconToggleItem(
                            iconRes = R.drawable.rounded_visibility_off_24,
                            title = stringResource(R.string.island_starlink_hide_arrow_title),
                            isChecked = hideArrow,
                            onCheckedChange = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                hideArrow = it
                                settings.putBoolean(SettingsRepository.KEY_ISLAND_STARLINK_HIDE_ARROW, it)
                            },
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = showInCompact && showSpeeds,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                    val unitLabels = mapOf(
                        StarlinkSpeedFormat.UNIT_MBPS to "Mbps",
                        StarlinkSpeedFormat.UNIT_AUTO to stringResource(R.string.label_auto),
                    )
                    SegmentedPicker(
                        items = unitLabels.keys.toList(),
                        selectedItem = speedUnit,
                        onItemSelected = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            speedUnit = it
                            settings.putString(SettingsRepository.KEY_ISLAND_STARLINK_SPEED_UNIT, it)
                        },
                        labelProvider = { unitLabels[it].orEmpty() },
                        title = R.string.island_starlink_unit_title,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AnimatedVisibility(
                        visible = speedUnit == StarlinkSpeedFormat.UNIT_MBPS,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        IconToggleItem(
                            iconRes = R.drawable.rounded_visibility_off_24,
                            title = stringResource(R.string.island_starlink_hide_unit_title),
                            isChecked = hideUnit,
                            onCheckedChange = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                hideUnit = it
                                settings.putBoolean(SettingsRepository.KEY_ISLAND_STARLINK_HIDE_UNIT, it)
                            },
                        )
                    }
                }
            }
        }
    }
}
