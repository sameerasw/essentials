package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.cards.PermissionCard
import com.sameerasw.essentials.ui.components.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.BiometricHelper
import androidx.fragment.app.FragmentActivity
import com.sameerasw.essentials.ui.modifiers.highlight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }
    
    val isAppLockEnabled by viewModel.isAppLockEnabled
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Security",
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
                iconRes = R.drawable.rounded_shield_lock_24,
                title = "Enable app lock",
                isChecked = isAppLockEnabled,
                onCheckedChange = { enabled ->
                    if (context is FragmentActivity) {
                        BiometricHelper.showBiometricPrompt(
                            activity = context,
                            title = "App Lock Security",
                            subtitle = if (enabled) "Authenticate to enable app lock" else "Authenticate to disable app lock",
                            onSuccess = { viewModel.setAppLockEnabled(enabled, context) }
                        )
                    } else {
                        viewModel.setAppLockEnabled(enabled, context)
                    }
                },
                enabled = isAccessibilityEnabled,
                onDisabledClick = {},
                modifier = Modifier.highlight(highlightKey == "app_lock_enabled")
            )

            FeatureCard(
                title = "Select locked apps",
                description = "Choose which apps require authentication",
                iconRes = R.drawable.rounded_app_registration_24,
                isEnabled = isAppLockEnabled,
                showToggle = false,
                hasMoreSettings = true,
                onToggle = {},
                onClick = { isAppSelectionSheetOpen = true },
                modifier = Modifier.highlight(highlightKey == "app_lock_selected_apps")
            )
        }

        Text(
            text = "Secure your apps with biometric authentication. Locked apps will require authentication when launching, Stays unlocked until the screen turns off.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Beware that this is not a robust solution as this is only a 3rd party application. If you need strong security, consider using Private Space or other such features.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Another not, the biometric authentication prompt only lets you use STRONG secure class methods. Face unlock security methods in WEAK class in devices such as Pixel 7 will only be able to utilize the available other STRONG auth methods such as fingerprint or pin.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isAppSelectionSheetOpen) {
            AppSelectionSheet(
                onDismissRequest = { isAppSelectionSheetOpen = false },
                onLoadApps = { viewModel.loadAppLockSelectedApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveAppLockSelectedApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled -> viewModel.updateAppLockAppEnabled(ctx, pkg, enabled) }
            )
        }
    }
}
