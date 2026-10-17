/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Hardware Features
 * File: QuickTapSettingsUI.kt
 * Description: Settings screen for Pixel Quick Tap+ action sequences.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun QuickTapSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val isEnabled = viewModel.isQuickTapEnabled.value

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_touch_app_24,
                title = stringResource(R.string.quick_tap_enable_title),
                isChecked = isEnabled,
                onCheckedChange = { viewModel.setQuickTapEnabled(it, context) },
                modifier = Modifier.highlight(highlightSetting == "enable_quick_tap"),
            )
        }

        AnimatedVisibility(
            visible = isEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            ActionSequenceEditor(
                viewModel = viewModel,
                actions = viewModel.quickTapActions.value,
                onActionsChange = viewModel::setQuickTapActions,
                screenOnOnly = true,
                listKey = "quick_tap",
                emptyText = stringResource(R.string.quick_tap_no_actions),
            )
        }

        RoundedCardContainer {
            Text(
                text = stringResource(R.string.quick_tap_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
