package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.viewmodels.WatchViewModel

@Composable
fun RemoteLockSettingsUI(
    mainViewModel: MainViewModel,
    watchViewModel: WatchViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    var showPermissionSheet by remember { mutableStateOf(false) }

    val isAccessibilityEnabled by mainViewModel.isAccessibilityEnabled
    val isDeviceAdminEnabled by mainViewModel.isDeviceAdminEnabled
    val remoteLockMode by watchViewModel.remoteLockMode

    LaunchedEffect(Unit) {
        watchViewModel.load(settingsRepository)
    }

    if (showPermissionSheet) {
        com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            featureTitle = R.string.feat_lock_from_watch_title,
            permissions = listOf(
                com.sameerasw.essentials.ui.components.sheets.PermissionItem(
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    title = R.string.perm_accessibility_title,
                    description = R.string.perm_accessibility_desc_common,
                    dependentFeatures = listOf(R.string.feat_lock_from_watch_title),
                    actionLabel = R.string.perm_action_enable,
                    action = { PermissionUtils.openAccessibilitySettings(context) },
                    isGranted = isAccessibilityEnabled
                ),
                com.sameerasw.essentials.ui.components.sheets.PermissionItem(
                    iconRes = R.drawable.rounded_security_24,
                    title = R.string.perm_device_admin_title,
                    description = R.string.perm_device_admin_desc,
                    dependentFeatures = listOf(R.string.feat_lock_from_watch_title),
                    actionLabel = R.string.perm_action_grant,
                    action = { mainViewModel.requestDeviceAdmin(context) },
                    isGranted = isDeviceAdminEnabled
                )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.remote_lock_mode_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer {
            com.sameerasw.essentials.ui.components.pickers.SegmentedPicker(
                items = listOf(0, 1),
                selectedItem = remoteLockMode,
                onItemSelected = { mode ->
                    val hasPermission = when (mode) {
                        0 -> isAccessibilityEnabled
                        1 -> isDeviceAdminEnabled
                        else -> false
                    }
                    if (!hasPermission) {
                        showPermissionSheet = true
                    } else {
                        watchViewModel.setRemoteLockMode(mode, settingsRepository)
                    }
                },
                labelProvider = { mode ->
                    when (mode) {
                        0 -> context.getString(R.string.remote_lock_mode_screen_off)
                        1 -> context.getString(R.string.remote_lock_mode_lock)
                        else -> ""
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "remote_lock_mode")
            )
        }

        Text(
            text = stringResource(R.string.remote_lock_mode_admin_note),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
        )
    }
}
