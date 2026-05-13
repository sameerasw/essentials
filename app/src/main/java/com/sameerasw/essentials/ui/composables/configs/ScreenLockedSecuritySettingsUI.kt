package com.sameerasw.essentials.ui.composables.configs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
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
    var showPermissionSheet by remember { mutableStateOf(false) }

    if (showPermissionSheet) {
        val isShizukuAvailable = viewModel.isShizukuAvailable.value
        val isShizukuGranted = viewModel.isShizukuPermissionGranted.value
        val isRootAvailable = viewModel.isRootAvailable.value
        val isRootGranted = viewModel.isRootPermissionGranted.value
        val isShellGranted =
            (isShizukuAvailable && isShizukuGranted) || (isRootAvailable && isRootGranted)

        PermissionsBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            featureTitle = R.string.screen_locked_security_title,
            permissions = listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_adb_24,
                    title = if (!isShizukuAvailable) R.string.perm_shizuku_title else R.string.perm_shizuku_grant_title,
                    description = if (!isShizukuAvailable) R.string.perm_shizuku_desc else R.string.perm_shizuku_grant_desc,
                    dependentFeatures = listOf(R.string.screen_locked_security_title),
                    actionLabel = if (!isShizukuAvailable) R.string.perm_shizuku_install_action else if (isShellGranted) R.string.perm_action_granted else R.string.perm_action_grant,
                    action = {
                        if (!isShizukuAvailable) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
                            )
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } else {
                            viewModel.requestShizukuPermission()
                        }
                    },
                    isGranted = isShellGranted
                )
            )
        )
    }

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
            val isShizukuGranted =
                viewModel.isShizukuAvailable.value && viewModel.isShizukuPermissionGranted.value
            val isRootGranted =
                viewModel.isRootAvailable.value && viewModel.isRootPermissionGranted.value
            val isShellGranted = isShizukuGranted || isRootGranted

            IconToggleItem(
                title = stringResource(R.string.screen_locked_security_title),
                description = stringResource(R.string.screen_locked_security_desc),
                isChecked = viewModel.isScreenLockedSecurityEnabled.value,
                onCheckedChange = { isChecked ->
                    if (!isShellGranted) {
                        showPermissionSheet = true
                    } else if (context is FragmentActivity) {
                        BiometricHelper.showBiometricPrompt(
                            activity = context,
                            title = context.getString(R.string.screen_locked_security_dialog_title),
                            subtitle = if (isChecked) context.getString(R.string.screen_locked_security_auth_enable) else context.getString(
                                R.string.screen_locked_security_auth_disable
                            ),
                            onSuccess = {
                                viewModel.setScreenLockedSecurityEnabled(
                                    isChecked,
                                    context
                                )
                            }
                        )
                    } else {
                        viewModel.setScreenLockedSecurityEnabled(isChecked, context)
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!isShellGranted) {
                        showPermissionSheet = true
                    }
                },
                iconRes = R.drawable.rounded_security_24,
                modifier = Modifier.highlight(highlightSetting == "screen_locked_security_toggle")
            )
        }
    }
}
