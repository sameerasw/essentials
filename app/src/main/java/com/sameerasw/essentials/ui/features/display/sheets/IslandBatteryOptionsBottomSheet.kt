package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
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
    val view = LocalView.current

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_battery_alert_24,
                    title = stringResource(R.string.island_battery_peek_alerts_title),
                    isChecked = viewModel.isIslandBatteryPeekAlerts.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandBatteryPeekAlerts(checked)
                    },
                )
            }

            if (rememberIslandShowsWhileLocked()) {
                RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                    IslandPrefToggle(
                        settingKey = SettingsRepository.KEY_ISLAND_BATTERY_HIDE_LOCKED,
                        iconRes = R.drawable.rounded_mobile_lock_portrait_24,
                        title = stringResource(R.string.island_hide_when_locked_title),
                    )
                }
            }

            IslandBatteryOptions(
                viewModel = viewModel,
                onlyLow = viewModel.isIslandBatteryOnlyLow.value,
                onOnlyLowChange = viewModel::setIslandBatteryOnlyLow,
                showIconConditional = true,
            )
        }
    }
}
