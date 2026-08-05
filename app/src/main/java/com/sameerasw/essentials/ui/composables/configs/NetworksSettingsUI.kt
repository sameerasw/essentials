package com.sameerasw.essentials.ui.composables.configs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

private enum class NetworkPermissionModule {
    RATE_LIMIT,
    MOBILE_DATA_ALWAYS_ON,
    NONE
}

@Composable
fun NetworksSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    var requestingPermissionFor by remember { mutableStateOf(NetworkPermissionModule.NONE) }

    val presetValues = remember { intArrayOf(-1, 16000, 32000, 125000, 625000, 1875000) }
    val disabledLabel = stringResource(R.string.rate_limit_disabled)
    val presetLabels = remember(disabledLabel) {
        listOf(disabledLabel, "128 Kbps", "256 Kbps", "1 Mbps", "5 Mbps", "15 Mbps")
    }

    val isShizukuAvailable = viewModel.isShizukuAvailable.value
    val isShizukuGranted = viewModel.isShizukuPermissionGranted.value
    val isRootAvailable = viewModel.isRootAvailable.value
    val isRootGranted = viewModel.isRootPermissionGranted.value
    val isShellGranted = (isShizukuAvailable && isShizukuGranted) || (isRootAvailable && isRootGranted)
    val isHasWritePermission = viewModel.isWriteSecureSettingsEnabled.value || isShellGranted

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNetworksState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (requestingPermissionFor != NetworkPermissionModule.NONE) {
        val shizukuPermission = PermissionItem(
            iconRes = R.drawable.rounded_adb_24,
            title = if (!isShizukuAvailable) R.string.perm_shizuku_title else R.string.perm_shizuku_grant_title,
            description = if (!isShizukuAvailable) R.string.perm_shizuku_desc else R.string.perm_shizuku_grant_desc,
            dependentFeatures = listOf(
                R.string.feat_network_download_rate_limit_title,
                R.string.feat_mobile_data_always_on_title
            ),
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

        PermissionsBottomSheet(
            onDismissRequest = { requestingPermissionFor = NetworkPermissionModule.NONE },
            featureTitle = R.string.feat_networks_title,
            permissions = listOf(shizukuPermission)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            val currentRateLimit = viewModel.networkDownloadRateLimit.intValue
            val currentIndex = remember(currentRateLimit) {
                val idx = presetValues.indexOf(currentRateLimit)
                if (idx != -1) idx else 0
            }

            ConfigSliderItem(
                title = stringResource(R.string.feat_network_download_rate_limit_title),
                description = stringResource(R.string.feat_network_download_rate_limit_desc),
                value = currentIndex.toFloat(),
                onValueChange = { floatVal ->
                    val newIndex = floatVal.toInt().coerceIn(0, presetValues.lastIndex)
                    HapticUtil.performSliderHaptic(view)
                    if (isHasWritePermission) {
                        viewModel.setNetworkDownloadRateLimit(presetValues[newIndex], context)
                    } else {
                        requestingPermissionFor = NetworkPermissionModule.RATE_LIMIT
                    }
                },
                valueRange = 0f..5f,
                steps = 4,
                increment = 1f,
                valueFormatter = { floatVal ->
                    val idx = floatVal.toInt().coerceIn(0, presetLabels.lastIndex)
                    presetLabels[idx]
                },
                iconRes = R.drawable.rounded_cell_wifi_24,
                enabled = true,
                modifier = Modifier.highlight(highlightSetting == "network_download_rate_limit_slider")
            )

            IconToggleItem(
                title = stringResource(R.string.feat_mobile_data_always_on_title),
                description = stringResource(R.string.feat_mobile_data_always_on_desc),
                isChecked = viewModel.isMobileDataAlwaysOnEnabled.value,
                onCheckedChange = { enabled ->
                    if (isHasWritePermission) {
                        viewModel.setMobileDataAlwaysOnEnabled(enabled, context)
                    } else {
                        requestingPermissionFor = NetworkPermissionModule.MOBILE_DATA_ALWAYS_ON
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!isHasWritePermission) {
                        requestingPermissionFor = NetworkPermissionModule.MOBILE_DATA_ALWAYS_ON
                    }
                },
                iconRes = R.drawable.rounded_mobile_24,
                modifier = Modifier.highlight(highlightSetting == "mobile_data_always_on_toggle")
            )
        }
    }
}
