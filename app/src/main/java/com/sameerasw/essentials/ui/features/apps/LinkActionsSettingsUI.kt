package com.sameerasw.essentials.ui.features.apps

/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Link Action Features
 * File: LinkActionsSettingsUI.kt
 * Description: Composable screen for Link actions settings.
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.linkActions.LinkPickerScreen
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun LinkActionsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    var linkActionVisible by remember { mutableStateOf(false) }

    if (linkActionVisible) {
        LinkPickerScreen(
            uri = "https://sameerasw.com".toUri(),
            disableLinkPreview = viewModel.disableLinkPreview.value,
            onFinish = { linkActionVisible = !linkActionVisible },
            modifier = Modifier.fillMaxSize(),
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Link action options",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer(spacing = 2.dp) {
            FeatureCard(
                modifier = Modifier.highlight(highlightSetting == "Link action"),
                title = R.string.link_action_title_open,
                description = R.string.link_action_desc_open,
                iconRes = R.drawable.rounded_link_24,
                isEnabled = true,
                isToggleEnabled = false,
                showToggle = false,
                onDisabledToggleClick = null,
                hasMoreSettings = false,
                isBeta = false,
                onToggle = {},
                onClick = { linkActionVisible = !linkActionVisible },
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_window_open_24,
                title = stringResource(R.string.disable_link_preview_title),
                description = stringResource(R.string.disable_link_preview_desc),
                isChecked = viewModel.disableLinkPreview.value,
                onCheckedChange = { viewModel.toggleDisableLinkPreview() },
                modifier = Modifier.highlight(highlightSetting == "disable_link_preview"),
            )
        }
    }
}
