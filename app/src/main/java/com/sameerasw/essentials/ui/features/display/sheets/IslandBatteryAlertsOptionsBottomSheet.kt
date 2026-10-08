package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandBatteryAlertsOptionsBottomSheet(
    onDismissRequest: () -> Unit,
) {
    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.island_battery_alerts_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
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
            }
        }
    }
}
