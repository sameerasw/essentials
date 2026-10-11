package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandBatteryOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings = remember { SettingsRepository(context) }
    var iconPosition by remember { mutableStateOf(settings.getIslandBatteryAlertIconPosition()) }

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IslandBatteryOptions(
                viewModel = viewModel,
                onlyLow = viewModel.isIslandBatteryOnlyLow.value,
                onOnlyLowChange = viewModel::setIslandBatteryOnlyLow,
                showIconConditional = true,
            )

            Text(
                text = stringResource(R.string.island_battery_alerts_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_battery_alert_24,
                    title = stringResource(R.string.island_battery_alerts_title),
                    isChecked = viewModel.isIslandShowBatteryAlerts.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandShowBatteryAlerts(checked)
                    },
                )
                IslandPrefToggle(
                    settingKey = SettingsRepository.KEY_ISLAND_BATTERY_ALERT_CHARGING,
                    iconRes = R.drawable.rounded_bolt_24,
                    title = stringResource(R.string.island_battery_alert_charging_title),
                    defaultValue = true,
                )
                IslandPrefToggle(
                    settingKey = SettingsRepository.KEY_ISLAND_BATTERY_ALERT_LOW,
                    iconRes = R.drawable.rounded_battery_alert_24,
                    title = stringResource(R.string.island_battery_alert_low_title),
                    defaultValue = true,
                )
                IslandPrefToggle(
                    settingKey = SettingsRepository.KEY_ISLAND_BATTERY_ALERT_CRITICAL,
                    iconRes = R.drawable.rounded_battery_alert_24,
                    title = stringResource(R.string.island_battery_alert_critical_title),
                    defaultValue = true,
                )
                ConfigPickerItem(
                    title = stringResource(R.string.island_battery_alert_icon_position_title),
                    selectedValue = if (iconPosition == SettingsRepository.ISLAND_BATTERY_ALERT_ICON_POSITION_LEFT) {
                        stringResource(R.string.island_battery_alert_icon_position_left)
                    } else {
                        stringResource(R.string.island_battery_alert_icon_position_right)
                    },
                    iconRes = R.drawable.rounded_center_focus_strong_24,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SegmentedDropdownMenuItem(
                        text = { Text(stringResource(R.string.island_battery_alert_icon_position_right)) },
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            iconPosition = SettingsRepository.ISLAND_BATTERY_ALERT_ICON_POSITION_RIGHT
                            settings.setIslandBatteryAlertIconPosition(SettingsRepository.ISLAND_BATTERY_ALERT_ICON_POSITION_RIGHT)
                        },
                    )
                    SegmentedDropdownMenuItem(
                        text = { Text(stringResource(R.string.island_battery_alert_icon_position_left)) },
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            iconPosition = SettingsRepository.ISLAND_BATTERY_ALERT_ICON_POSITION_LEFT
                            settings.setIslandBatteryAlertIconPosition(SettingsRepository.ISLAND_BATTERY_ALERT_ICON_POSITION_LEFT)
                        },
                    )
                }
            }
        }
    }
}
