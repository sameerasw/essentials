/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandNotificationOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring Island notification options.
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
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandNotificationOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
    highlightSetting: String? = null,
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
                text = stringResource(R.string.island_notification_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                if (rememberIslandShowsWhileLocked()) {
                    IslandPrefToggle(
                        settingKey = SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_LOCKED,
                        iconRes = R.drawable.rounded_visibility_off_24,
                        title = stringResource(R.string.island_notif_conceal_locked_title),
                    )
                }

                AnimatedVisibility(
                    visible = viewModel.isIslandLineStageEnabled.value,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_notification_sound_24,
                        title = stringResource(R.string.island_notif_compact_heads_up_title),
                        description = stringResource(R.string.island_notif_compact_heads_up_desc),
                        isChecked = viewModel.isIslandNotifCompactHeadsUp.value,
                        onCheckedChange = { checked ->
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.setIslandNotifCompactHeadsUp(checked)
                        },
                        modifier = Modifier.highlight(highlightSetting == "island_notif_compact_heads_up"),
                    )
                }

                IconToggleItem(
                    iconRes = R.drawable.rounded_downloading_24,
                    title = stringResource(R.string.island_notif_keep_progress_title),
                    isChecked = viewModel.isIslandNotifKeepProgress.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandNotifKeepProgress(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_notif_keep_progress"),
                )

                IconToggleItem(
                    iconRes = R.drawable.outline_circle_notifications_24,
                    title = stringResource(R.string.island_notif_queue_title),
                    isChecked = viewModel.isIslandNotifQueue.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandNotifQueue(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_notif_queue"),
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_touch_app_24,
                    title = stringResource(R.string.island_notif_tap_to_open_title),
                    isChecked = viewModel.isIslandNotifTapToOpen.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandNotifTapToOpen(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_notif_tap_to_open"),
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    title = stringResource(R.string.island_catch_up_title),
                    description = stringResource(R.string.island_catch_up_desc),
                    isChecked = viewModel.isIslandCatchUpEnabled.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandCatchUpEnabled(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_catch_up"),
                )

                AnimatedVisibility(
                    visible = viewModel.isIslandCatchUpEnabled.value,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    val infinityText = stringResource(R.string.island_timeout_infinity)
                    ConfigSliderItem(
                        title = stringResource(R.string.island_catch_up_timeout_title),
                        value = catchUpStepIndex(viewModel.islandCatchUpTimeoutMs.longValue).toFloat(),
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setIslandCatchUpTimeoutMs(CATCH_UP_STEPS_MS[it.toInt().coerceIn(CATCH_UP_STEPS_MS.indices)])
                        },
                        valueRange = 0f..CATCH_UP_STEPS_MS.lastIndex.toFloat(),
                        increment = 1f,
                        iconRes = R.drawable.rounded_timer_24,
                        valueFormatter = { formatCatchUp(CATCH_UP_STEPS_MS[it.toInt().coerceIn(CATCH_UP_STEPS_MS.indices)], infinityText) },
                        modifier = Modifier.highlight(highlightSetting == "island_catch_up_timeout"),
                    )
                }
            }
        }
    }
}

private val CATCH_UP_STEPS_MS =
    longArrayOf(5_000, 10_000, 15_000, 20_000, 30_000, 45_000, 60_000, 120_000, 180_000, 300_000, 600_000, 900_000, 0)

private fun catchUpStepIndex(ms: Long): Int =
    if (ms <= 0L) CATCH_UP_STEPS_MS.lastIndex
    else CATCH_UP_STEPS_MS.indices.filter { CATCH_UP_STEPS_MS[it] > 0 }.minBy { kotlin.math.abs(CATCH_UP_STEPS_MS[it] - ms) }

private fun formatCatchUp(ms: Long, infinityText: String): String =
    when {
        ms <= 0L -> infinityText
        ms < 60_000 -> "${ms / 1000}s"
        else -> "${ms / 60_000}m"
    }
