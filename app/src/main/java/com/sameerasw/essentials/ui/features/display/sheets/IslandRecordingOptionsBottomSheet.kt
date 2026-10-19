/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandRecordingOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring Island recording options.
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
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandRecordingOptionsBottomSheet(
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
                text = stringResource(R.string.island_recording_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_visibility_24,
                    title = stringResource(R.string.island_recording_peek_title),
                    isChecked = viewModel.isIslandRecordingPeek.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandRecordingPeek(checked)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_fiber_smart_record_24,
                    title = stringResource(R.string.island_recording_focus_title),
                    description = stringResource(R.string.island_recording_focus_desc),
                    isChecked = viewModel.isIslandRecordingFocus.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandRecordingFocus(checked)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_av_timer_24,
                    title = stringResource(R.string.island_recording_show_time_title),
                    isChecked = viewModel.isIslandRecordingShowTime.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandRecordingShowTime(checked)
                    },
                )
            }

            Text(
                text = stringResource(R.string.island_recording_sources_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_mobile_24,
                    title = stringResource(R.string.island_recording_source_screen),
                    isChecked = viewModel.isIslandRecordingScreen.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandRecordingScreen(checked)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_videocam_24,
                    title = stringResource(R.string.island_recording_source_camera),
                    isChecked = viewModel.isIslandRecordingCamera.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandRecordingCamera(checked)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_mic_24,
                    title = stringResource(R.string.island_recording_source_microphone),
                    isChecked = viewModel.isIslandRecordingMicrophone.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandRecordingMicrophone(checked)
                    },
                )
            }
        }
    }
}
