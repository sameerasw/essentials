/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Power
 * File: MapsPowerSavingSettingsUI.kt
 * Description: UI component and settings composable for Power feature domain.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun MapsPowerSavingSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TranslatableText(
            stringResId = R.string.maps_power_saving_detection_channels,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            viewModel.mapsChannels.value.forEach { channel ->
                IconToggleItem(
                    iconRes = R.drawable.rounded_navigation_24,
                    title = channel.name,
                    isChecked = channel.isEnabled,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setMapsChannelDetected(channel.id, checked, context)
                    },
                    modifier = Modifier.highlight(highlightSetting == channel.id),
                )
            }

            if (viewModel.mapsChannels.value.isEmpty()) {
                TranslatableText(
                    stringResId = R.string.maps_power_saving_no_channels,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
