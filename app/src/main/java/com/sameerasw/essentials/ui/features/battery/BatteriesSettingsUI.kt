/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Battery Settings
 * File: BatteriesSettingsUI.kt
 * Description: UI settings composable screen for battery stats, rings, and charging options.
 */

package com.sameerasw.essentials.ui.features.battery

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteriesSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
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
        val notificationPermission =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    viewModel.isPostNotificationsEnabled.value = true
                    viewModel.setBatteryNotificationEnabled(true, context)
                }
            }

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_battery_charging_60_24,
                title = stringResource(R.string.batteries_notification_title),
                isChecked = viewModel.isBatteryNotificationEnabled.value,
                onCheckedChange = { enabled ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (enabled &&
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                        !viewModel.isPostNotificationsEnabled.value
                    ) {
                        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setBatteryNotificationEnabled(enabled, context)
                    }
                },
            )

            var showPercentage by remember {
                mutableStateOf(
                    com.sameerasw.essentials.data.repository
                        .SettingsRepository(context)
                        .isBatteryShowPercentageEnabled(),
                )
            }
            IconToggleItem(
                iconRes = R.drawable.rounded_percent_24,
                title = stringResource(R.string.battery_show_percentage_title),
                isChecked = showPercentage,
                onCheckedChange = { enabled ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    showPercentage = enabled
                    com.sameerasw.essentials.data.repository
                        .SettingsRepository(context)
                        .setBatteryShowPercentageEnabled(enabled)
                    BatteriesPreviewTrigger.bump()
                    viewModel.setBluetoothDevicesEnabled(viewModel.isBluetoothDevicesEnabled.value, context)
                },
            )
        }

        RoundedCardContainer {
            // Bluetooth Devices
            val isBluetoothEnabled = viewModel.isBluetoothDevicesEnabled.value
            val isPermissionGranted = viewModel.isBluetoothPermissionGranted.value

            val launcher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) { permissions ->
                    val allGranted = permissions.values.all { it }
                    if (allGranted) {
                        viewModel.isBluetoothPermissionGranted.value = true
                        viewModel.setBluetoothDevicesEnabled(true, context)
                    }
                }

            IconToggleItem(
                iconRes = R.drawable.rounded_bluetooth_24,
                title = stringResource(R.string.show_bluetooth_devices),
                isChecked = isBluetoothEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (isPermissionGranted) {
                            viewModel.setBluetoothDevicesEnabled(true, context)
                        } else {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                launcher.launch(
                                    arrayOf(
                                        android.Manifest.permission.BLUETOOTH_CONNECT,
                                        android.Manifest.permission.BLUETOOTH_SCAN,
                                    ),
                                )
                            } else {
                                viewModel.setBluetoothDevicesEnabled(true, context)
                            }
                        }
                    } else {
                        viewModel.setBluetoothDevicesEnabled(false, context)
                    }
                },
            )

            if (isBluetoothEnabled && isPermissionGranted) {
                var separateChannels by remember {
                    mutableStateOf(
                        com.sameerasw.essentials.data.repository
                            .SettingsRepository(context)
                            .isBatterySeparateChannelsEnabled(),
                    )
                }
                IconToggleItem(
                    iconRes = R.drawable.rounded_earbuds_2_24,
                    title = stringResource(R.string.battery_separate_channels_title),
                    description = stringResource(R.string.battery_separate_channels_desc),
                    isChecked = separateChannels,
                    onCheckedChange = { enabled ->
                        separateChannels = enabled
                        com.sameerasw.essentials.data.repository
                            .SettingsRepository(context)
                            .setBatterySeparateChannelsEnabled(enabled)
                        if (enabled && !com.sameerasw.essentials.utils.ShellUtils.hasPermission(context)) {
                            if (com.sameerasw.essentials.utils.ShizukuUtils.isShizukuAvailable()) {
                                com.sameerasw.essentials.utils.ShizukuUtils.requestPermission()
                            } else {
                                android.widget.Toast
                                    .makeText(context, R.string.shizuku_not_running_desc, android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        viewModel.setBluetoothDevicesEnabled(true, context)
                        BatteriesPreviewTrigger.bump()
                    },
                )
            }
        }

        run {
            Text(
                text = stringResource(R.string.bt_device_icons_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, bottom = 0.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            com.sameerasw.essentials.ui.features.battery.BluetoothDeviceIconsSection(viewModel, viewModel.isBluetoothDevicesEnabled.value && viewModel.isBluetoothPermissionGranted.value)
        }

        // Max Devices Slider
        Text(
            text = stringResource(R.string.limit_max_devices),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 0.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer {
            ListItem(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                contentPadding =
                    PaddingValues(
                        horizontal = 16.dp,
                        vertical = 16.dp,
                    ),
                colors =
                    ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                    ),
                content = {
                    Text(
                        text = stringResource(R.string.limit_max_devices_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                supportingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Slider(
                            value = viewModel.batteryWidgetMaxDevices.intValue.toFloat(),
                            onValueChange = {
                                val newInt = it.toInt()
                                if (newInt != viewModel.batteryWidgetMaxDevices.intValue) {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.setBatteryWidgetMaxDevices(newInt, context)
                                }
                            },
                            valueRange = 1f..16f,
                            steps = 14,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = viewModel.batteryWidgetMaxDevices.intValue.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        }
    }
}
