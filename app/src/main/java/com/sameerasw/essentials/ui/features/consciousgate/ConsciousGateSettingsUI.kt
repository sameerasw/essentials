/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Conscious Gate
 * File: ConsciousGateSettingsUI.kt
 * Description: Composable UI for configuring Conscious Gate's target applications, pause
 * delay, reappear-after-usage timer, countdown style, pause-screen icon/text, and a live
 * preview of the resulting pause screen.
 */

package com.sameerasw.essentials.ui.features.consciousgate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsciousGateSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightKey: String? = null,
) {
    val context = LocalContext.current
    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }
    var appsReloadTrigger by remember { mutableStateOf(0) }
    var selectedAppLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isPreviewOpen by remember { mutableStateOf(false) }

    val isConsciousGateEnabled by viewModel.isConsciousGateEnabled
    val delaySeconds by viewModel.consciousGateDelaySeconds
    val reappearMinutes by viewModel.consciousGateReappearMinutes
    val iconName by viewModel.consciousGateIconName
    val title by viewModel.consciousGateTitle
    val message by viewModel.consciousGateMessage
    val countdownStyle by viewModel.consciousGateCountdownStyle

    LaunchedEffect(appsReloadTrigger) {
        withContext(Dispatchers.IO) {
            val labels =
                viewModel
                    .loadConsciousGateSelectedApps(context)
                    .filter { it.isEnabled }
                    .map { AppUtil.getAppLabel(context, it.packageName) }
            withContext(Dispatchers.Main) {
                selectedAppLabels = labels
            }
        }
    }

    val selectedAppsDescription =
        if (selectedAppLabels.isEmpty()) {
            stringResource(R.string.conscious_gate_select_apps_desc)
        } else {
            val shown = selectedAppLabels.take(3)
            val extra = selectedAppLabels.size - shown.size
            if (extra > 0) {
                shown.joinToString(", ") + " " + stringResource(R.string.conscious_gate_selected_apps_more_suffix, extra)
            } else {
                shown.joinToString(", ")
            }
        }

    val delaySecondsOptions = listOf(3, 5, 10)
    val reappearPresetMinutes = listOf(0, 5, 10, 15)
    val reappearPresetLabels =
        listOf(
            stringResource(R.string.conscious_gate_reappear_off),
            stringResource(R.string.conscious_gate_reappear_5min),
            stringResource(R.string.conscious_gate_reappear_10min),
            stringResource(R.string.conscious_gate_reappear_15min),
        )
    val customOptionLabel = stringResource(R.string.conscious_gate_custom_option_label)

    var isDelayCustom by remember { mutableStateOf(delaySeconds !in delaySecondsOptions) }
    var isReappearCustom by remember { mutableStateOf(reappearMinutes !in reappearPresetMinutes) }

    val delaySelectedLabel = stringResource(R.string.conscious_gate_delay_seconds_value, delaySeconds)
    val reappearSelectedLabel =
        if (isReappearCustom) {
            stringResource(R.string.conscious_gate_reappear_minutes_value, reappearMinutes)
        } else {
            reappearPresetMinutes.indexOf(reappearMinutes).let { presetIndex ->
                if (presetIndex >= 0) reappearPresetLabels[presetIndex] else stringResource(R.string.conscious_gate_reappear_minutes_value, reappearMinutes)
            }
        }

    var customDelayText by remember(delaySeconds) { mutableStateOf(delaySeconds.toString()) }
    var customReappearText by remember(reappearMinutes) { mutableStateOf(reappearMinutes.toString()) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.feat_conscious_gate_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_pause_24,
                title = stringResource(R.string.conscious_gate_enable_title),
                isChecked = isConsciousGateEnabled,
                onCheckedChange = { enabled -> viewModel.setConsciousGateEnabled(enabled, context) },
                modifier = Modifier.highlight(highlightKey == "conscious_gate_enabled"),
            )

            FeatureCard(
                title = stringResource(R.string.conscious_gate_select_apps_title),
                description = selectedAppsDescription,
                iconRes = R.drawable.rounded_apps_24,
                isEnabled = isConsciousGateEnabled,
                showToggle = false,
                hasMoreSettings = true,
                onToggle = {},
                onClick = { isAppSelectionSheetOpen = true },
                modifier = Modifier.highlight(highlightKey == "conscious_gate_selected_apps"),
            )

            ConfigPickerItem(
                title = stringResource(R.string.conscious_gate_delay_title),
                description = stringResource(R.string.conscious_gate_delay_desc),
                iconRes = R.drawable.rounded_timer_24,
                isEnabled = isConsciousGateEnabled,
                selectedValue = delaySelectedLabel,
                modifier = Modifier.highlight(highlightKey == "conscious_gate_delay_seconds"),
            ) {
                delaySecondsOptions.forEach { seconds ->
                    SegmentedDropdownMenuItem(
                        text = { Text(stringResource(R.string.conscious_gate_delay_seconds_value, seconds)) },
                        onClick = {
                            isDelayCustom = false
                            viewModel.setConsciousGateDelaySeconds(seconds)
                        },
                    )
                }
                SegmentedDropdownMenuItem(
                    text = { Text(customOptionLabel) },
                    onClick = { isDelayCustom = true },
                )
            }

            if (isDelayCustom) {
                OutlinedTextField(
                    value = customDelayText,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }.take(3)
                        customDelayText = filtered
                        filtered.toIntOrNull()?.let { seconds ->
                            if (seconds in 1..999) viewModel.setConsciousGateDelaySeconds(seconds)
                        }
                    },
                    label = { Text(stringResource(R.string.conscious_gate_delay_custom_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = isConsciousGateEnabled,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                )
            }

            ConfigPickerItem(
                title = stringResource(R.string.conscious_gate_reappear_title),
                description = stringResource(R.string.conscious_gate_reappear_desc),
                iconRes = R.drawable.rounded_lock_clock_24,
                isEnabled = isConsciousGateEnabled,
                selectedValue = reappearSelectedLabel,
                modifier = Modifier.highlight(highlightKey == "conscious_gate_reappear_minutes"),
            ) {
                reappearPresetMinutes.forEachIndexed { index, minutes ->
                    SegmentedDropdownMenuItem(
                        text = { Text(reappearPresetLabels[index]) },
                        onClick = {
                            isReappearCustom = false
                            viewModel.setConsciousGateReappearMinutes(minutes)
                        },
                    )
                }
                SegmentedDropdownMenuItem(
                    text = { Text(customOptionLabel) },
                    onClick = { isReappearCustom = true },
                )
            }

            if (isReappearCustom) {
                OutlinedTextField(
                    value = customReappearText,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }.take(4)
                        customReappearText = filtered
                        filtered.toIntOrNull()?.let { minutes ->
                            if (minutes in 0..1440) viewModel.setConsciousGateReappearMinutes(minutes)
                        }
                    },
                    label = { Text(stringResource(R.string.conscious_gate_reappear_custom_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = isConsciousGateEnabled,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                )
            }
        }

        Text(
            text = stringResource(R.string.conscious_gate_appearance_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer(modifier = Modifier) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.conscious_gate_countdown_style_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ConsciousGateCountdownStylePicker(
                    selectedStyle = countdownStyle,
                    onStyleSelected = { viewModel.setConsciousGateCountdownStyle(it) },
                    modifier = Modifier.highlight(highlightKey == "conscious_gate_countdown_style"),
                )

                ConsciousGateIconPicker(
                    selectedIconName = iconName,
                    onIconSelected = { viewModel.setConsciousGateIconName(it) },
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.setConsciousGateTitle(it) },
                    label = { Text(stringResource(R.string.conscious_gate_title_label)) },
                    placeholder = { Text(stringResource(R.string.conscious_gate_default_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { viewModel.setConsciousGateMessage(it) },
                    label = { Text(stringResource(R.string.conscious_gate_message_label)) },
                    placeholder = { Text(stringResource(R.string.conscious_gate_default_message)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                )
            }
        }

        OutlinedButton(
            onClick = { isPreviewOpen = true },
            enabled = isConsciousGateEnabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.round_play_arrow_24),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(R.string.conscious_gate_preview_button))
        }

        Text(
            text = stringResource(R.string.conscious_gate_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isPreviewOpen) {
            Dialog(
                onDismissRequest = { isPreviewOpen = false },
                properties =
                    DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                    ),
            ) {
                ConsciousGatePreview(
                    iconName = iconName,
                    title = title.takeIf { it.isNotBlank() } ?: stringResource(R.string.conscious_gate_default_title),
                    message = message.takeIf { it.isNotBlank() } ?: stringResource(R.string.conscious_gate_default_message),
                    targetAppLabel = selectedAppLabels.firstOrNull() ?: stringResource(R.string.conscious_gate_preview_placeholder_app),
                    countdownStyle = countdownStyle,
                    delaySeconds = delaySeconds,
                    onExit = { isPreviewOpen = false },
                )
            }
        }

        if (isAppSelectionSheetOpen) {
            AppSelectionSheet(
                onDismissRequest = {
                    isAppSelectionSheetOpen = false
                    appsReloadTrigger++
                },
                onLoadApps = { viewModel.loadConsciousGateSelectedApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveConsciousGateSelectedApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled ->
                    viewModel.updateConsciousGateAppEnabled(ctx, pkg, enabled)
                },
            )
        }
    }
}
