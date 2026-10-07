/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandTimerOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring Island timer and stopwatch options.
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandTimerOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    var showAppSelectionSheet by remember { mutableStateOf(false) }

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
                text = stringResource(R.string.island_timer_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_fiber_smart_record_24,
                    title = stringResource(R.string.island_timers_show_screen_recorder_title),
                    isChecked = viewModel.isIslandTimersShowScreenRecorder.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandTimersShowScreenRecorder(checked)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_apps_24,
                    title = stringResource(R.string.island_timers_filter_apps_title),
                    isChecked = viewModel.isIslandTimersFilterApps.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandTimersFilterApps(checked)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_apps_24,
                    title = stringResource(R.string.action_select_apps),
                    showToggle = false,
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showAppSelectionSheet = true
                    },
                )
            }
        }
    }

    if (showAppSelectionSheet) {
        AppSelectionSheet(
            onDismissRequest = { showAppSelectionSheet = false },
            onLoadApps = { viewModel.loadIslandTimersSelectedApps(it) },
            onSaveApps = { ctx, apps -> viewModel.saveIslandTimersSelectedApps(ctx, apps) },
            onAppToggle = { ctx, pkg, enabled -> viewModel.updateIslandTimersAppEnabled(ctx, pkg, enabled) },
            context = context,
        )
    }
}
