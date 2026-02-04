package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlashlightPulseSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    var showAppSelectionSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        
        Text(
            text = stringResource(R.string.settings_section_flashlight_pulse),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_flashlight_on_24,
                title = stringResource(R.string.flashlight_pulse_title),
                isChecked = viewModel.isFlashlightPulseEnabled.value,
                onCheckedChange = { checked ->
                    viewModel.setFlashlightPulseEnabled(checked, context)
                },
                modifier = Modifier.highlight(highlightSetting == "flashlight_pulse" || highlightSetting == "flashlight_pulse_enabled")
            )
            if (viewModel.isFlashlightPulseEnabled.value) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_mobile_text_24,
                    title = stringResource(R.string.flashlight_pulse_facedown_title),
                    isChecked = viewModel.isFlashlightPulseFacedownOnly.value,
                    onCheckedChange = { checked ->
                        viewModel.setFlashlightPulseFacedownOnly(checked, context)
                    },
                    modifier = Modifier.highlight(highlightSetting == "flashlight_pulse_facedown" || highlightSetting == "flashlight_pulse_facedown_only")
                )
            }
            IconToggleItem(
                iconRes = R.drawable.rounded_apps_24,
                title = stringResource(R.string.flashlight_pulse_same_as_lighting_title),
                isChecked = viewModel.isFlashlightPulseUseLightingApps.value,
                onCheckedChange = { checked ->
                    viewModel.setFlashlightPulseUseLightingApps(checked, context)
                },
                modifier = Modifier.highlight(highlightSetting == "flashlight_pulse_same_apps")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Selection Sheet Button
        Button(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                showAppSelectionSheet = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel.isFlashlightPulseEnabled.value && !viewModel.isFlashlightPulseUseLightingApps.value
        ) {
            Text(stringResource(R.string.action_select_apps))
        }

        Spacer(modifier = Modifier.height(80.dp))

        if (showAppSelectionSheet) {
            AppSelectionSheet(
                onDismissRequest = { showAppSelectionSheet = false },
                onLoadApps = { viewModel.loadFlashlightPulseSelectedApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveFlashlightPulseSelectedApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled -> viewModel.updateFlashlightPulseAppEnabled(ctx, pkg, enabled) },
                context = context
            )
        }
    }
}
