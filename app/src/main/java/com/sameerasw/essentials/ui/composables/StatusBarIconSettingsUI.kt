package com.sameerasw.essentials.ui.composables

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.StatusBarIconViewModel

@Composable
fun StatusBarIconSettingsUI(
    viewModel: StatusBarIconViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPermissionGranted = viewModel.isWriteSecureSettingsEnabled.value
    val isMobileDataVisible = viewModel.isMobileDataVisible.value
    val isWiFiVisible = viewModel.isWiFiVisible.value

    // Refresh permission state when composable is shown
    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon Visibility Settings Card
        SettingsCard(title = "Icon Visibility") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Mobile Data Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_android_cell_dual_4_bar_24),
                        contentDescription = "Mobile Data",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Mobile Data",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isMobileDataVisible,
                        onCheckedChange = { isChecked ->
                            viewModel.setMobileDataVisible(isChecked, context)
                        },
                        enabled = isPermissionGranted
                    )
                }

                // WiFi Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_android_wifi_3_bar_24),
                        contentDescription = "WiFi",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "WiFi",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isWiFiVisible,
                        onCheckedChange = { isChecked ->
                            viewModel.setWiFiVisible(isChecked, context)
                        },
                        enabled = isPermissionGranted
                    )
                }
            }
        }

        // Smart Visibility Settings Card
        SettingsCard(title = "Smart Visibility") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                // Smart WiFi Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_cell_wifi_24),
                        contentDescription = "Smart WiFi",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart WiFi",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Hide mobile data when WiFi is connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = viewModel.isSmartWiFiEnabled.value,
                        onCheckedChange = { isChecked ->
                            viewModel.setSmartWiFiEnabled(isChecked, context)
                        },
                        enabled = isPermissionGranted
                    )
                }
            }
        }
    }
}

