/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Audio & Sound Controls
 * File: ShutUpSettingsUI.kt
 * Description: Composable screen for media mute and ShutUp per-app rules.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.ShutUpAppConfig
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.RestoreModePicker
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.ShutUpPerAppSettingsSheet
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShutUpSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightKey: String? = null,
) {
    val context = LocalContext.current
    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }
    var selectedConfigForEditing by remember { mutableStateOf<ShutUpAppConfig?>(null) }

    val configs by viewModel.shutUpConfigs

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            ConfigSliderItem(
                title = stringResource(R.string.shut_up_restore_delay_title),
                value = viewModel.shutUpRestoreDelay.intValue.toFloat(),
                onValueChange = { viewModel.setShutUpRestoreDelay(it.toInt()) },
                valueRange = 2f..60f,
                increment = 1f,
                valueFormatter = { "${it.toInt()}s" },
                iconRes = R.drawable.rounded_timer_24,
                subtitle = stringResource(R.string.shut_up_restore_delay_desc),
            )

            RestoreModePicker(
                selectedMode = viewModel.shutUpRestoreMode.value,
                onModeSelected = { viewModel.setShutUpRestoreMode(it) },
            )

            val keyboards = remember { loadEnabledKeyboards(context) }
            var keyboardMenuOpen by remember { mutableStateOf(false) }
            val selectedKeyboard = viewModel.shutUpKeyboard.value
            Box {
                IconToggleItem(
                    iconRes = R.drawable.rounded_keyboard_24,
                    title = stringResource(R.string.shut_up_keyboard_title),
                    description = keyboards.firstOrNull { it.first == selectedKeyboard }?.second
                        ?: stringResource(R.string.shut_up_keyboard_skip),
                    showToggle = false,
                    onClick = { keyboardMenuOpen = true },
                )
                SegmentedDropdownMenu(
                    expanded = keyboardMenuOpen,
                    onDismissRequest = { keyboardMenuOpen = false },
                ) {
                    SegmentedDropdownMenuItem(
                        text = { Text(stringResource(R.string.shut_up_keyboard_skip)) },
                        onClick = {
                            keyboardMenuOpen = false
                            viewModel.setShutUpKeyboard("")
                        },
                    )
                    keyboards.forEach { (id, label) ->
                        SegmentedDropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                keyboardMenuOpen = false
                                viewModel.setShutUpKeyboard(id)
                            },
                        )
                    }
                }
            }

            FeatureCard(
                title = stringResource(R.string.shut_up_select_apps_title),
                description = stringResource(R.string.shut_up_select_apps_desc),
                iconRes = R.drawable.rounded_app_registration_24,
                isEnabled = true,
                showToggle = false,
                hasMoreSettings = true,
                onToggle = {},
                onClick = { isAppSelectionSheetOpen = true },
            )
        }

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            configs.forEach { config ->
                val appName =
                    remember(config.packageName) {
                        try {
                            val appInfo =
                                context.packageManager.getApplicationInfo(config.packageName, 0)
                            context.packageManager.getApplicationLabel(appInfo).toString()
                        } catch (e: Exception) {
                            config.packageName
                        }
                    }

                val appIconPainter =
                    remember(config.packageName) {
                        try {
                            val drawable = context.packageManager.getApplicationIcon(config.packageName)
                            androidx.compose.ui.graphics.painter.BitmapPainter(
                                AppUtil.drawableToBitmap(drawable).asImageBitmap(),
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                FeatureCard(
                    title = appName,
                    description = config.packageName,
                    isEnabled = true,
                    onToggle = {},
                    onClick = { selectedConfigForEditing = config },
                    iconPainter = appIconPainter,
                    showToggle = false,
                    hasMoreSettings = true,
                    customTrailingContent = {
                        IconButton(
                            onClick = {
                                viewModel.createShutUpShortcut(context, config)
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_add_24),
                                contentDescription = stringResource(R.string.action_create_shortcut),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    additionalMenuItems = { onDismiss ->
                        SegmentedDropdownMenuItem(
                            text = { Text(stringResource(R.string.action_remove)) },
                            onClick = {
                                onDismiss()
                                viewModel.removeShutUpConfig(config.packageName)
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_delete_24),
                                    contentDescription = null,
                                )
                            },
                        )
                    },
                )
            }
        }

        Text(
            text = stringResource(R.string.shut_up_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isAppSelectionSheetOpen) {
            AppSelectionSheet(
                onDismissRequest = { isAppSelectionSheetOpen = false },
                onLoadApps = { ctx ->
                    viewModel.shutUpConfigs.value.map { AppSelection(it.packageName, true) }
                },
                onSaveApps = { ctx, apps -> viewModel.saveShutUpSelectedApps(ctx, apps) },
            )
        }

        if (selectedConfigForEditing != null) {
            val frozenApps = remember { viewModel.loadFreezeSelectedApps(context) }
            val isFrozen =
                remember(selectedConfigForEditing) {
                    frozenApps.any { it.packageName == selectedConfigForEditing?.packageName }
                }

            ShutUpPerAppSettingsSheet(
                onDismissRequest = { selectedConfigForEditing = null },
                config =
                    configs.find { it.packageName == selectedConfigForEditing?.packageName }
                        ?: selectedConfigForEditing!!,
                onConfigChanged = { viewModel.updateShutUpConfig(it) },
                onCreateShortcut = { viewModel.createShutUpShortcut(context, it) },
                isFrozen = isFrozen,
                viewModel = viewModel,
            )
        }
    }
}

private fun loadEnabledKeyboards(context: android.content.Context): List<Pair<String, String>> =
    try {
        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.enabledInputMethodList.map { it.id to it.loadLabel(context.packageManager).toString() }
    } catch (_: Exception) {
        emptyList()
    }
