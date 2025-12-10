package com.sameerasw.essentials.ui.composables.configs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.viewmodels.StatusBarIconViewModel
import com.sameerasw.essentials.ui.components.pickers.NetworkTypePicker
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer

@Composable
fun StatusBarIconSettingsUI(
    viewModel: StatusBarIconViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPermissionGranted = viewModel.isWriteSecureSettingsEnabled.value
    val isMobileDataVisible = viewModel.isMobileDataVisible.value
    val isWiFiVisible = viewModel.isWiFiVisible.value

    var showPermissionSheet by remember { mutableStateOf(false) }

    // Refresh permission state when composable is shown
    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    // Permission sheet for Smart Data
    if (showPermissionSheet) {
        PermissionsBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            featureTitle = "Smart Data",
            permissions = listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_android_cell_dual_4_bar_24,
                    title = "Read Phone State",
                    description = "Required to detect network type for Smart Data feature",
                    dependentFeatures = listOf("Smart Data"),
                    actionLabel = "Grant Permission",
                    action = {
                        ActivityCompat.requestPermissions(
                            context as ComponentActivity,
                            arrayOf(Manifest.permission.READ_PHONE_STATE),
                            1001
                        )
                    },
                    isGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED
                )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Icon Visibility Category
        Text(
            text = "Icon Visibility",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_android_cell_dual_4_bar_24,
                title = "Mobile Data",
                isChecked = isMobileDataVisible,
                onCheckedChange = { isChecked ->
                    viewModel.setMobileDataVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_android_wifi_3_bar_24,
                title = "WiFi",
                isChecked = isWiFiVisible,
                onCheckedChange = { isChecked ->
                    viewModel.setWiFiVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_vpn_key_24,
                title = "VPN",
                isChecked = viewModel.isVpnVisible.value,
                onCheckedChange = { isChecked ->
                    viewModel.setVpnVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_alarm_24,
                title = "Alarm Clock",
                isChecked = viewModel.isAlarmClockVisible.value,
                onCheckedChange = { isChecked ->
                    viewModel.setAlarmClockVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_wifi_tethering_24,
                title = "Hotspot",
                isChecked = viewModel.isHotspotVisible.value,
                onCheckedChange = { isChecked ->
                    viewModel.setHotspotVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_bluetooth_24,
                title = "Bluetooth",
                isChecked = viewModel.isBluetoothVisible.value,
                onCheckedChange = { isChecked ->
                    viewModel.setBluetoothVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_data_saver_on_24,
                title = "Data saver",
                isChecked = viewModel.isDataSaverVisible.value,
                onCheckedChange = { isChecked ->
                    viewModel.setDataSaverVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_headset_mic_24,
                title = "Headset",
                isChecked = viewModel.isHeadsetVisible.value,
                onCheckedChange = { isChecked ->
                    viewModel.setHeadsetVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_rotate_24,
                title = "Rotate",
                isChecked = viewModel.isRotateVisible.value,
                onCheckedChange = { isChecked ->
                    viewModel.setRotateVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_wifi_calling_bar_3_24,
                title = "voLTE/ voNR/ voWiFi",
                isChecked = viewModel.isVolteVisible.value,
                onCheckedChange = { isChecked ->
                    viewModel.setVolteVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_cast_24,
                title = "Cast",
                isChecked = viewModel.isCastVisible.value,
                onCheckedChange = { isChecked ->
                    viewModel.setCastVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_nest_clock_farsight_analog_24,
                title = "Clock",
                isChecked = viewModel.isClockVisible.value,
                onCheckedChange = { isChecked ->
                    viewModel.setClockVisible(isChecked, context)
                },
                enabled = isPermissionGranted
            )
        }

        // Smart Visibility Category
        Text(
            text = "Smart Visibility",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_cell_wifi_24,
                title = "Smart WiFi",
                description = "Hide mobile data when WiFi is connected",
                isChecked = viewModel.isSmartWiFiEnabled.value,
                onCheckedChange = { isChecked ->
                    viewModel.setSmartWiFiEnabled(isChecked, context)
                },
                enabled = isPermissionGranted && viewModel.isMobileDataVisible.value
            )

            Box(
                modifier = Modifier.clickable {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        showPermissionSheet = true
                    }
                }
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_android_cell_dual_5_bar_alert_24,
                    title = "Smart Data",
                    description = "Hide mobile data in certain modes",
                    isChecked = viewModel.isSmartDataEnabled.value,
                    onCheckedChange = { isChecked ->
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED

                        if (isChecked && !hasPermission) {
                            showPermissionSheet = true
                        } else {
                            viewModel.setSmartDataEnabled(isChecked, context)
                        }
                    },
                    enabled = isPermissionGranted && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED && viewModel.isMobileDataVisible.value,
                    onDisabledClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            showPermissionSheet = true
                        }
                    }
                )

                val isSwitchDisabled =
                    !(isPermissionGranted && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED)

                if (isSwitchDisabled) {
                    Box(modifier = Modifier.matchParentSize().clickable {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            showPermissionSheet = true
                        }
                    })
                }
            }

            // Network Type Picker (only show when Smart Data is enabled)
            if (viewModel.isSmartDataEnabled.value) {
                Column() {
                    NetworkTypePicker(
                        selectedTypes = viewModel.selectedNetworkTypes.value,
                        onTypesSelected = { selectedTypes ->
                            viewModel.selectedNetworkTypes.value = selectedTypes
                            // Save to preferences
                            val prefs = context.getSharedPreferences(
                                "essentials_prefs",
                                Context.MODE_PRIVATE
                            )
                            prefs.edit().putStringSet(
                                "selected_network_types",
                                selectedTypes.map { it.name }.toSet()
                            ).apply()
                        }
                    )
                }
            }
        }
    }
}
