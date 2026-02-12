package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.BiometricHelper
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun ScreenLockedSecuritySettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val isAccessibilityEnabled = viewModel.isAccessibilityEnabled.value

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_section_security),
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
                title = stringResource(R.string.screen_locked_security_title),
                isChecked = viewModel.isScreenLockedSecurityEnabled.value,
                onCheckedChange = { isChecked ->
                    if (context is FragmentActivity) {
                        BiometricHelper.showBiometricPrompt(
                            activity = context,
                            title = context.getString(R.string.screen_locked_security_dialog_title),
                            subtitle = if (isChecked) context.getString(R.string.screen_locked_security_auth_enable) else context.getString(R.string.screen_locked_security_auth_disable),
                            onSuccess = { viewModel.setScreenLockedSecurityEnabled(isChecked, context) }
                        )
                    } else {
                        viewModel.setScreenLockedSecurityEnabled(isChecked, context)
                    }
                },
                enabled = isAccessibilityEnabled && viewModel.isWriteSecureSettingsEnabled.value && viewModel.isDeviceAdminEnabled.value,
                onDisabledClick = {
                    // Handled by parent
                },
                iconRes = R.drawable.rounded_security_24,
                modifier = Modifier.highlight(highlightSetting == "screen_locked_security_toggle")
            )
        }


        // Warning Section
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.warning_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.screen_locked_security_warning_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.screen_locked_security_airplane_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )

            }
        }

        Text(
            text = stringResource(R.string.screen_locked_security_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
