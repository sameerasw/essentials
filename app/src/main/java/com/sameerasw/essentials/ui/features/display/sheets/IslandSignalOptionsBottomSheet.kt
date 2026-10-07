package com.sameerasw.essentials.ui.features.display.sheets

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.NetworkTypePicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionItem
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandSignalOptionsBottomSheet(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings = remember { SettingsRepository(context) }
    var showPermissionSheet by remember { mutableStateOf(false) }
    var activity by remember { mutableStateOf(settings.isIslandNetworkActivityEnabled()) }
    var enabled by remember { mutableStateOf(settings.isIslandShowSignalEnabled()) }
    var lowOnly by remember { mutableStateOf(settings.isIslandSignalLowOnlyEnabled()) }
    var wifi by remember { mutableStateOf(settings.isIslandSignalWifiEnabled()) }
    var showMode by remember { mutableStateOf(settings.isIslandSignalShowModeEnabled()) }
    var types by remember { mutableStateOf(settings.getIslandSignalNetworkTypes()) }

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.island_signal_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_android_wifi_3_bar_24,
                    title = stringResource(R.string.island_network_activity_title),
                    isChecked = activity,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        activity = it
                        settings.setIslandNetworkActivityEnabled(it)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_signal_cellular_alt_24,
                    title = stringResource(R.string.island_show_signal_title),
                    isChecked = enabled,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        enabled = it
                        settings.setIslandShowSignalEnabled(it)
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                        if (it && !granted) showPermissionSheet = true
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_android_wifi_3_bar_24,
                    title = stringResource(R.string.island_signal_wifi_title),
                    isChecked = wifi,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        wifi = it
                        settings.setIslandSignalWifiEnabled(it)
                    },
                )
                if (enabled || wifi) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_android_cell_dual_5_bar_alert_24,
                        title = stringResource(R.string.island_signal_low_only_title),
                        isChecked = lowOnly,
                        onCheckedChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            lowOnly = it
                            settings.setIslandSignalLowOnlyEnabled(it)
                        },
                    )
                }
            }

            if (enabled) {
                Text(
                    text = stringResource(R.string.island_signal_types_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
                RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                    NetworkTypePicker(
                        selectedTypes = types,
                        onTypesSelected = {
                            types = it
                            settings.setIslandSignalNetworkTypes(it)
                        },
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_signal_cellular_alt_24,
                        title = stringResource(R.string.island_signal_show_mode_title),
                        isChecked = showMode,
                        onCheckedChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            showMode = it
                            settings.setIslandSignalShowModeEnabled(it)
                        },
                    )
                }
            }
        }
    }

    if (showPermissionSheet) {
        PermissionsBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            featureTitle = R.string.island_show_signal_title,
            permissions = listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_android_cell_dual_4_bar_24,
                    title = R.string.permission_read_phone_state_title,
                    description = R.string.permission_read_phone_state_desc,
                    dependentFeatures = listOf(R.string.island_show_signal_title),
                    actionLabel = R.string.permission_grant_action,
                    action = {
                        (context as? Activity)?.let {
                            ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.READ_PHONE_STATE), 1001)
                        }
                    },
                    isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED,
                ),
            ),
        )
    }
}
