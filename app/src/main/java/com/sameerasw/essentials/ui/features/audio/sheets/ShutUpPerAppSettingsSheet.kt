/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Audio
 * File: ShutUpPerAppSettingsSheet.kt
 * Description: UI component and settings composable for Audio feature domain.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.ShutUpCustomSetting
import com.sameerasw.essentials.utils.HapticUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.ShutUpAppConfig
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShutUpPerAppSettingsSheet(
    onDismissRequest: () -> Unit,
    config: ShutUpAppConfig,
    onConfigChanged: (ShutUpAppConfig) -> Unit,
    onCreateShortcut: (ShutUpAppConfig) -> Unit,
    isFrozen: Boolean,
    viewModel: MainViewModel = viewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentConfig by remember(config) { mutableStateOf(config) }
    var showShizukuRestartWarning by remember { mutableStateOf(false) }

    val isAttemptShizukuRestart by viewModel.isShutUpAttemptShizukuRestart
    val hiddenDebugging by viewModel.isHiddenDebuggingSupport

    if (showShizukuRestartWarning) {
        AlertDialog(
            onDismissRequest = { showShizukuRestartWarning = false },
            title = { TranslatableText(R.string.shut_up_shizuku_restart_warning_title) },
            text = { TranslatableText(R.string.shut_up_shizuku_restart_warning_message) },
            confirmButton = {
                TextButton(onClick = {
                    showShizukuRestartWarning = false
                    val newConfig = currentConfig.copy(autoArchive = true)
                    currentConfig = newConfig
                    onConfigChanged(newConfig)
                }) {
                    TranslatableText(R.string.action_enable)
                }
            },
            dismissButton = {
                TextButton(onClick = { showShizukuRestartWarning = false }) {
                    TranslatableText(R.string.action_cancel)
                }
            },
        )
    }

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.shut_up_per_app_settings,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            RoundedCardContainer(
                modifier = Modifier,
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_settings_24,
                    title = stringResource(R.string.shut_up_disable_dev_options),
                    isChecked = currentConfig.disableDevOptions && !hiddenDebugging,
                    enabled = !hiddenDebugging,
                    onCheckedChange = {
                        val newConfig = currentConfig.copy(disableDevOptions = it)
                        currentConfig = newConfig
                        onConfigChanged(newConfig)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_adb_24,
                    title = stringResource(R.string.shut_up_disable_usb_debugging),
                    isChecked = currentConfig.disableUsbDebugging && !hiddenDebugging,
                    enabled = !hiddenDebugging,
                    onCheckedChange = {
                        val newConfig = currentConfig.copy(disableUsbDebugging = it)
                        currentConfig = newConfig
                        onConfigChanged(newConfig)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_android_wifi_4_bar_plus_24,
                    title = stringResource(R.string.shut_up_disable_wireless_debugging),
                    isChecked = currentConfig.disableWirelessDebugging,
                    onCheckedChange = {
                        val newConfig = currentConfig.copy(disableWirelessDebugging = it)
                        currentConfig = newConfig
                        onConfigChanged(newConfig)
                    },
                )
                if (currentConfig.disableWirelessDebugging) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_adb_24,
                        title = stringResource(R.string.shut_up_attempt_shizuku_restart),
                        isChecked = isAttemptShizukuRestart,
                        onCheckedChange = {
                            viewModel.setShutUpAttemptShizukuRestartEnabled(it)
                            if (it && viewModel.shizukuAuthToken.value.isEmpty()) {
                                android.widget.Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.shut_up_auth_token_missing),
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                            }
                        },
                    )
                }
                IconToggleItem(
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    title = stringResource(R.string.shut_up_disable_accessibility),
                    isChecked = currentConfig.disableAccessibility,
                    onCheckedChange = {
                        val newConfig = currentConfig.copy(disableAccessibility = it)
                        currentConfig = newConfig
                        onConfigChanged(newConfig)
                    },
                )
            }

            RoundedCardContainer(
                modifier = Modifier,
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_keyboard_24,
                    title = stringResource(R.string.shut_up_restore_keyboard),
                    isChecked = currentConfig.restoreKeyboard,
                    onCheckedChange = {
                        val newConfig = currentConfig.copy(restoreKeyboard = it)
                        currentConfig = newConfig
                        onConfigChanged(newConfig)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_snowflake_24,
                    title = stringResource(R.string.shut_up_auto_archive_notif_title),
                    isChecked = currentConfig.autoArchive,
                    onCheckedChange = {
                        if (it && !isAttemptShizukuRestart && currentConfig.disableWirelessDebugging) {
                            showShizukuRestartWarning = true
                        } else {
                            val newConfig = currentConfig.copy(autoArchive = it)
                            currentConfig = newConfig
                            onConfigChanged(newConfig)
                        }
                    },
                )
            }

            ShutUpCustomSettingsSection(
                settings = currentConfig.customSettings.orEmpty(),
                onSettingsChanged = { updated ->
                    val newConfig = currentConfig.copy(customSettings = updated)
                    currentConfig = newConfig
                    onConfigChanged(newConfig)
                },
            )

            Button(
                onClick = {
                    onCreateShortcut(currentConfig)
                    onDismissRequest()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(
                    painter =
                        androidx.compose.ui.res
                            .painterResource(id = R.drawable.rounded_open_in_new_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                TranslatableText(stringResId = R.string.action_create_shortcut)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ShutUpCustomSettingsSection(
    settings: List<ShutUpCustomSetting>,
    onSettingsChanged: (List<ShutUpCustomSetting>) -> Unit,
) {
    val view = LocalView.current
    val count = settings.size + 1

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TranslatableText(
            stringResId = R.string.shut_up_custom_settings,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        settings.forEachIndexed { index, setting ->
            ShutUpCustomSettingRow(
                setting = setting,
                shape = groupedShape(index, count),
                onChange = { updated ->
                    onSettingsChanged(settings.toMutableList().also { it[index] = updated })
                },
                onDelete = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onSettingsChanged(settings.toMutableList().also { it.removeAt(index) })
                },
            )
        }
        IconToggleItem(
            iconRes = R.drawable.rounded_add_24,
            title = stringResource(R.string.shut_up_custom_setting_add),
            showToggle = false,
            onClick = { onSettingsChanged(settings + ShutUpCustomSetting()) },
            modifier = Modifier.clip(groupedShape(count - 1, count)),
        )
    }
}

@Composable
private fun ShutUpCustomSettingRow(
    setting: ShutUpCustomSetting,
    shape: Shape,
    onChange: (ShutUpCustomSetting) -> Unit,
    onDelete: () -> Unit,
) {
    val view = LocalView.current
    val tables =
        listOf(
            Action.SettingsTable.GLOBAL to R.string.diy_custom_settings_table_global,
            Action.SettingsTable.SECURE to R.string.diy_custom_settings_table_secure,
            Action.SettingsTable.SYSTEM to R.string.diy_custom_settings_table_system,
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            tables.forEachIndexed { index, (table, labelRes) ->
                SegmentedButton(
                    selected = setting.table == table,
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onChange(setting.copy(table = table))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, tables.size),
                    label = { Text(stringResource(labelRes), maxLines = 1) },
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = setting.key,
                onValueChange = { onChange(setting.copy(key = it.trim())) },
                label = { TranslatableText(R.string.diy_custom_settings_key_label) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = setting.isEnabled,
                onCheckedChange = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onChange(setting.copy(isEnabled = it))
                },
            )
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.rounded_delete_24),
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = setting.valueOnLaunch,
                onValueChange = { onChange(setting.copy(valueOnLaunch = it)) },
                label = { TranslatableText(R.string.shut_up_custom_setting_launch) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = setting.valueOnRestore,
                onValueChange = { onChange(setting.copy(valueOnRestore = it)) },
                label = { TranslatableText(R.string.shut_up_custom_setting_restore) },
                placeholder = { TranslatableText(R.string.shut_up_custom_setting_original) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun groupedShape(
    index: Int,
    count: Int,
): Shape {
    val top = if (index == 0) 24.dp else 4.dp
    val bottom = if (index == count - 1) 24.dp else 4.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}
