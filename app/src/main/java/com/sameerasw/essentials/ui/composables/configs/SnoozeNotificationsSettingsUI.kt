package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun SnoozeNotificationsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            viewModel.snoozeChannels.value.forEach { channel ->
                IconToggleItem(
                    iconRes = when (channel.id) {
                        "DEVELOPER_OPTIONS" -> R.drawable.rounded_adb_24
                        "USB_CONNECTION" -> R.drawable.rounded_usb_24
                        "BATTERY" -> R.drawable.rounded_charger_24
                        else -> R.drawable.rounded_notification_settings_24
                    },
                    title = channel.name,
                    isChecked = channel.isBlocked,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setSnoozeChannelBlocked(channel.id, checked, context)
                    },
                    modifier = Modifier.highlight(highlightSetting == channel.id)
                )
            }

            if (viewModel.snoozeChannels.value.isEmpty()) {
                androidx.compose.material3.Text(
                    text = stringResource(R.string.snooze_no_channels_discovered),
                    modifier = Modifier.padding(16.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
