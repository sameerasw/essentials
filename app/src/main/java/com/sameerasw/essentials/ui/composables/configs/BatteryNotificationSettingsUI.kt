package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun BatteryNotificationSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        RoundedCardContainer {
            ListItem(
                leadingContent = {
                    androidx.compose.material3.Icon(
                        painter = painterResource(id = R.drawable.rounded_battery_charging_60_24),
                        contentDescription = null
                    )
                },
                headlineContent = { Text(stringResource(R.string.feat_battery_notification_title)) },
                supportingContent = { Text(stringResource(R.string.feat_battery_notification_desc)) },
                trailingContent = {
                    Switch(
                        checked = viewModel.isBatteryNotificationEnabled.value,
                        onCheckedChange = { enabled ->
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.setBatteryNotificationEnabled(enabled, context)
                        }
                    )
                }
            )
        }

        Text(
            text = "This notification displays battery levels for your connected Mac and Bluetooth devices. You can configure which devices to show in the Battery Widget settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
