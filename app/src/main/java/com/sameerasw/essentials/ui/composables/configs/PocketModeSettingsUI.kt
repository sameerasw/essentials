package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.viewmodels.MainViewModel
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.sameerasw.essentials.ui.components.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun PocketModeSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showAppSelectionSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.feat_pocket_mode_title),
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
                title = stringResource(R.string.feat_pocket_mode_title),
                description = stringResource(R.string.feat_pocket_mode_desc),
                isChecked = viewModel.isPocketModeEnabled.value,
                onCheckedChange = { isChecked ->
                    viewModel.setPocketModeEnabled(isChecked)
                },
                enabled = true,
                iconRes = R.drawable.ic_pocket_mode,
                modifier = Modifier.highlight(highlightSetting == "pocket_mode_toggle")
            )

            IconToggleItem(
                title = stringResource(R.string.pocket_mode_use_light_title),
                description = stringResource(R.string.pocket_mode_use_light_desc),
                isChecked = viewModel.isPocketModeUseLightSensor.value,
                onCheckedChange = { isChecked ->
                    viewModel.setPocketModeUseLightSensor(isChecked)
                },
                enabled = viewModel.isPocketModeEnabled.value,
                iconRes = R.drawable.ic_light_sensor,
                modifier = Modifier.highlight(highlightSetting == "pocket_mode_use_light_sensor")
            )

            ConfigSliderItem(
                title = stringResource(R.string.pocket_mode_trigger_delay_title),
                value = viewModel.pocketModeTriggerDelay.floatValue,
                onValueChange = { viewModel.setPocketModeTriggerDelay(it) },
                valueRange = 0f..30f,
                steps = 29,
                increment = 1f,
                valueFormatter = { "${it.toInt()}s" },
                enabled = viewModel.isPocketModeEnabled.value,
                iconRes = R.drawable.rounded_timer_24
            )

            IconToggleItem(
                title = stringResource(R.string.pocket_mode_lock_screen_only_title),
                description = stringResource(R.string.pocket_mode_lock_screen_only_desc),
                isChecked = viewModel.isPocketModeLockScreenOnly.value,
                onCheckedChange = { viewModel.setPocketModeLockScreenOnly(it) },
                enabled = viewModel.isPocketModeEnabled.value,
                iconRes = R.drawable.rounded_lock_24
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

        Text(
            text = stringResource(R.string.pocket_mode_exclude_apps_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                showAppSelectionSheet = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_select_apps))
        }

        Text(
            text = stringResource(R.string.pocket_mode_exclude_apps_desc),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showAppSelectionSheet) {
            AppSelectionSheet(
                onDismissRequest = { showAppSelectionSheet = false },
                onLoadApps = { viewModel.loadPocketModeExcludedApps(it) },
                onSaveApps = { ctx, apps ->
                    viewModel.savePocketModeExcludedApps(ctx, apps)
                },
                onAppToggle = { ctx, pkg, enabled ->
                    viewModel.updatePocketModeExcludedAppEnabled(ctx, pkg, enabled)
                },
                context = context
            )
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))

        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            cornerRadius = 24.dp
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.rounded_info_24),
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.pocket_mode_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
