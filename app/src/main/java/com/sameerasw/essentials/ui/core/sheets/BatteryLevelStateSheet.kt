/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Sheets
 * File: BatteryLevelStateSheet.kt
 * Description: Configures the battery level DIY state range.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil
import kotlin.math.roundToInt
import com.sameerasw.essentials.domain.diy.State as DIYState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryLevelStateSheet(
    initialState: DIYState.BatteryLevel?,
    onDismiss: () -> Unit,
    onStateChange: (DIYState.BatteryLevel) -> Unit,
) {
    val view = LocalView.current
    var state by remember { mutableStateOf(initialState ?: DIYState.BatteryLevel()) }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.diy_state_battery_level),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
            Text(
                text = stringResource(R.string.diy_battery_level_range, state.minLevel, state.maxLevel),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
            RangeSlider(
                value = state.minLevel.toFloat()..state.maxLevel.toFloat(),
                onValueChange = { range ->
                    val next = DIYState.BatteryLevel(range.start.roundToInt(), range.endInclusive.roundToInt())
                    if (next != state) {
                        HapticUtil.performSliderHaptic(view)
                        state = next
                        onStateChange(next)
                    }
                },
                valueRange = 0f..100f,
                steps = 99,
            )
        }
    }
}
