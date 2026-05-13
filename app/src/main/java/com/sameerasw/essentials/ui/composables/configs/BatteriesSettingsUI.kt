package com.sameerasw.essentials.ui.composables.configs

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteriesSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AirSync connection
        val isAirSyncInstalled = try {
            context.packageManager.getPackageInfo("com.sameerasw.airsync", 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }

        RoundedCardContainer {
            if (isAirSyncInstalled) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_laptop_mac_24,
                    title = stringResource(R.string.connect_to_airsync),
                    description = stringResource(R.string.connect_to_airsync_summary),
                    isChecked = viewModel.isAirSyncConnectionEnabled.value,
                    onCheckedChange = {
                        viewModel.setAirSyncConnectionEnabled(it, context)
                    }
                )
            } else {
                ListItem(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_laptop_mac_24),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    trailingContent = {
                        Button(
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=com.sameerasw.airsync")
                                )
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text(stringResource(R.string.action_download))
                        }
                    },
                    content = {
                        Column {
                            Text(
                                text = stringResource(R.string.download_airsync),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.download_airsync_summary),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // Bluetooth Devices
            val isBluetoothEnabled = viewModel.isBluetoothDevicesEnabled.value
            val isPermissionGranted = viewModel.isBluetoothPermissionGranted.value

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
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
                description = stringResource(R.string.show_bluetooth_devices_summary),
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
                                        android.Manifest.permission.BLUETOOTH_SCAN
                                    )
                                )
                            } else {
                                viewModel.setBluetoothDevicesEnabled(true, context)
                            }
                        }
                    } else {
                        viewModel.setBluetoothDevicesEnabled(false, context)
                    }
                }
            )

            // Widget Background Toggle
            IconToggleItem(
                iconRes = R.drawable.rounded_circles_24,
                title = stringResource(R.string.widget_background_title),
                description = stringResource(R.string.widget_background_summary),
                isChecked = viewModel.isBatteryWidgetBackgroundEnabled.value,
                onCheckedChange = {
                    viewModel.setBatteryWidgetBackgroundEnabled(it, context)
                }
            )
        }

        // Max Devices Slider
        Text(
            text = stringResource(R.string.limit_max_devices),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 0.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer {
            ListItem(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ),
                content = {
                    Text(
                        text = stringResource(R.string.limit_max_devices_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                supportingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
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
                            valueRange = 1f..8f,
                            steps = 6,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = viewModel.batteryWidgetMaxDevices.intValue.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    }
}
