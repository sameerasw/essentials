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
fun AlwaysOnDisplaySettingsUI(
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_text_2_24,
                title = stringResource(R.string.feat_always_on_display_title),
                isChecked = viewModel.isAodEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setAodEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "aod_toggle")
            )
        }

        Text(
            text = stringResource(R.string.feat_notification_glance_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_notification_settings_24,
                title = stringResource(R.string.feat_notification_glance_title),
                isChecked = viewModel.isNotificationGlanceEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.toggleNotificationGlanceEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "notification_glance_enabled")
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_apps_24,
                title = stringResource(R.string.notification_glance_same_as_lighting_title),
                isChecked = viewModel.isNotificationGlanceSameAsLightingEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setNotificationGlanceSameAsLightingEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "notification_glance_same_apps")
            )

            val isAccessibilityEnabled = viewModel.isAccessibilityEnabled.value
            IconToggleItem(
                iconRes = R.drawable.rounded_power_settings_new_24,
                title = stringResource(R.string.feat_aod_force_turn_off_title),
                isChecked = viewModel.isAodForceTurnOffEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    // Check latest snapshot inside lambda
                    val currentlyEnabled = com.sameerasw.essentials.utils.PermissionUtils.isAccessibilityServiceEnabled(context)
                    if (checked && !currentlyEnabled) {
                        com.sameerasw.essentials.utils.PermissionUtils.openAccessibilitySettings(context)
                    } else {
                        viewModel.toggleAodForceTurnOffEnabled(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "aod_force_turn_off")
            )
        }

        Text(
            text = stringResource(R.string.notification_glance_desc),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

            Text(
                text = stringResource(R.string.feat_aod_force_turn_off_desc),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        if (!viewModel.isNotificationGlanceSameAsLightingEnabled.value) {
            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    showAppSelectionSheet = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.isNotificationGlanceEnabled.value
            ) {
                Text(stringResource(R.string.action_select_apps))
            }
        }

        Spacer(modifier = Modifier.height(80.dp))

        if (showAppSelectionSheet) {
            AppSelectionSheet(
                onDismissRequest = { showAppSelectionSheet = false },
                onLoadApps = { viewModel.loadNotificationGlanceSelectedApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveNotificationGlanceSelectedApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled ->
                    viewModel.updateNotificationGlanceAppEnabled(
                        ctx,
                        pkg,
                        enabled
                    )
                },
                context = context
            )
        }
    }
}
