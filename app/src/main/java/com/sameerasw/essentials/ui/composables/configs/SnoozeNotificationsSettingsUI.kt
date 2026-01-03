package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.ui.modifiers.highlight

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
            // Debugging
            IconToggleItem(
                iconRes = R.drawable.rounded_adb_24,
                title = "Disable debugging notifications",
                isChecked = viewModel.isSnoozeDebuggingEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setSnoozeDebuggingEnabled(checked, context)
                },
                modifier = Modifier.highlight(highlightSetting == "snooze_debugging")
            )

            // File Transfer
            IconToggleItem(
                iconRes = R.drawable.rounded_usb_24,
                title = "Disable file transfer notification",
                isChecked = viewModel.isSnoozeFileTransferEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setSnoozeFileTransferEnabled(checked, context)
                },
                modifier = Modifier.highlight(highlightSetting == "snooze_file_transfer")
            )

            // Charging
            IconToggleItem(
                iconRes = R.drawable.rounded_charger_24,
                title = "Disable charging notification",
                isChecked = viewModel.isSnoozeChargingEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setSnoozeChargingEnabled(checked, context)
                },
                modifier = Modifier.highlight(highlightSetting == "snooze_charging")
            )
        }
    }
}
