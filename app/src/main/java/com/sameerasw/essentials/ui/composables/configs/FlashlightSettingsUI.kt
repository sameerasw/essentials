package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun FlashlightSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.flashlight_options_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                iconRes = R.drawable.rounded_globe_24,
                title = stringResource(R.string.flashlight_global_title),
                description = stringResource(R.string.flashlight_global_desc),
                isChecked = viewModel.isFlashlightGlobalEnabled.value,
                onCheckedChange = { viewModel.setFlashlightGlobalEnabled(it, context) },
                modifier = Modifier.highlight(highlightSetting == "flashlight_global")
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_upcoming_24,
                title = stringResource(R.string.flashlight_adjust_intensity_title),
                description = stringResource(R.string.flashlight_adjust_intensity_desc),
                isChecked = viewModel.isFlashlightAdjustEnabled.value,
                onCheckedChange = { viewModel.setFlashlightAdjustEnabled(it, context) },
                modifier = Modifier.highlight(highlightSetting == "flashlight_adjust")
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_flashlight_on_24,
                title = stringResource(R.string.flashlight_live_update_title),
                description = stringResource(R.string.flashlight_live_update_desc),
                isChecked = viewModel.isFlashlightLiveUpdateEnabled.value,
                onCheckedChange = { viewModel.setFlashlightLiveUpdateEnabled(it, context) },
                modifier = Modifier.highlight(highlightSetting == "flashlight_live_update")
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_front_hand_24,
                title = stringResource(R.string.flashlight_pocket_title),
                description = stringResource(R.string.flashlight_pocket_desc),
                isChecked = viewModel.isFlashlightPocketTurnOffEnabled.value,
                onCheckedChange = { viewModel.setFlashlightPocketTurnOffEnabled(it, context) }
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_thermometer_alert_24,
                title = stringResource(R.string.flashlight_overheat_title),
                description = stringResource(R.string.flashlight_overheat_desc),
                isChecked = viewModel.isFlashlightOverheatEnabled.value,
                onCheckedChange = { viewModel.setFlashlightOverheatEnabled(it, context) }
            )

        }
    }
}
