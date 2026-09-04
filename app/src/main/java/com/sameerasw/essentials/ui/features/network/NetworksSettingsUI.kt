/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Network & Connectivity
 * File: NetworksSettingsUI.kt
 * Description: Composable screen for DNS presets and network tiles.
 */

package com.sameerasw.essentials.ui.features.system

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PermissionItem
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.features.network.sheets.SimNamesBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlin.math.roundToInt

private enum class NetworkPermissionModule {
    RATE_LIMIT,
    MOBILE_DATA_ALWAYS_ON,
    WIRELESS_DISPLAY_CERTIFICATION,
    SIM_NAMES,
    NONE,
}

@Composable
fun NetworksSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var requestingPermissionFor by remember { mutableStateOf(NetworkPermissionModule.NONE) }

    val presetValues =
        remember {
            intArrayOf(
                -1, // Disabled
                16000, // 128 Kbps
                32000, // 256 Kbps
                64000, // 512 Kbps
                125000, // 1 Mbps
                250000, // 2 Mbps
                625000, // 5 Mbps
                1250000, // 10 Mbps
                1875000, // 15 Mbps
                3125000, // 25 Mbps
                6250000, // 50 Mbps
                12500000, // 100 Mbps
                18750000, // 150 Mbps
                25000000, // 200 Mbps
                31250000, // 250 Mbps
            )
        }
    val disabledLabel = stringResource(R.string.rate_limit_disabled)
    val presetLabels =
        remember(disabledLabel) {
            listOf(
                disabledLabel,
                "128 Kbps",
                "256 Kbps",
                "512 Kbps",
                "1 Mbps",
                "2 Mbps",
                "5 Mbps",
                "10 Mbps",
                "15 Mbps",
                "25 Mbps",
                "50 Mbps",
                "100 Mbps",
                "150 Mbps",
                "200 Mbps",
                "250 Mbps",
            )
        }

    val isShizukuAvailable = viewModel.isShizukuAvailable.value
    val isShizukuGranted = viewModel.isShizukuPermissionGranted.value
    val isRootAvailable = viewModel.isRootAvailable.value
    val isRootGranted = viewModel.isRootPermissionGranted.value
    val isShellGranted =
        (isShizukuAvailable && isShizukuGranted) || (isRootAvailable && isRootGranted)
    val isHasWritePermission = viewModel.isWriteSecureSettingsEnabled.value || isShellGranted

    LaunchedEffect(Unit) {
        viewModel.refreshNetworksState(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshNetworksState(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val hasReadPhoneState =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED
    val hasSimNamesPermission = isShellGranted && hasReadPhoneState

    var showSimNamesSheet by remember { mutableStateOf(false) }

    if (showSimNamesSheet) {
        SimNamesBottomSheet(
            onDismissRequest = { showSimNamesSheet = false },
        )
    }

    if (requestingPermissionFor != NetworkPermissionModule.NONE) {
        val permissionsList = mutableListOf<PermissionItem>()

        val shizukuPermission =
            PermissionItem(
                iconRes = R.drawable.rounded_adb_24,
                title = if (!isShizukuAvailable) R.string.perm_shizuku_title else R.string.perm_shizuku_grant_title,
                description = if (!isShizukuAvailable) R.string.perm_shizuku_desc else R.string.perm_shizuku_grant_desc,
                dependentFeatures =
                    listOf(
                        R.string.feat_network_download_rate_limit_title,
                        R.string.feat_mobile_data_always_on_title,
                        R.string.feat_wireless_display_certification_title,
                        R.string.feat_sim_names_title,
                    ),
                actionLabel =
                    if (!isShizukuAvailable) {
                        R.string.perm_shizuku_install_action
                    } else if (isShellGranted) {
                        R.string.perm_action_granted
                    } else {
                        R.string.perm_action_grant
                    },
                action = {
                    if (!isShizukuAvailable) {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/thedjchi/Shizuku"),
                            )
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    } else {
                        viewModel.requestShizukuPermission()
                    }
                },
                isGranted = isShellGranted,
            )
        permissionsList.add(shizukuPermission)

        if (requestingPermissionFor == NetworkPermissionModule.SIM_NAMES) {
            val phoneStatePermission =
                PermissionItem(
                    iconRes = R.drawable.rounded_mobile_vibrate_24,
                    title = R.string.permission_read_phone_state_title,
                    description = R.string.permission_read_phone_state_desc_call_vibrations,
                    dependentFeatures = listOf(R.string.feat_sim_names_title),
                    actionLabel = if (hasReadPhoneState) R.string.perm_action_granted else R.string.perm_action_grant,
                    action = {
                        (context as? Activity)?.let {
                            ActivityCompat.requestPermissions(
                                it,
                                arrayOf(Manifest.permission.READ_PHONE_STATE),
                                102,
                            )
                        }
                    },
                    isGranted = hasReadPhoneState,
                )
            permissionsList.add(phoneStatePermission)
        }

        PermissionsBottomSheet(
            onDismissRequest = { requestingPermissionFor = NetworkPermissionModule.NONE },
            featureTitle =
                if (requestingPermissionFor ==
                    NetworkPermissionModule.SIM_NAMES
                ) {
                    R.string.feat_sim_names_title
                } else {
                    R.string.feat_networks_title
                },
            permissions = permissionsList,
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            val currentRateLimit = viewModel.networkDownloadRateLimit.intValue
            val currentIndex =
                remember(currentRateLimit) {
                    val idx = presetValues.indexOf(currentRateLimit)
                    if (idx != -1) {
                        idx
                    } else {
                        presetValues.indices.minByOrNull { kotlin.math.abs(presetValues[it] - currentRateLimit) }
                            ?: 0
                    }
                }
            var sliderValue by remember(currentIndex) { mutableFloatStateOf(currentIndex.toFloat()) }

            ConfigSliderItem(
                title = stringResource(R.string.feat_network_download_rate_limit_title),
                description = stringResource(R.string.feat_network_download_rate_limit_desc),
                value = sliderValue,
                onValueChange = { floatVal ->
                    sliderValue = floatVal
                    val newIndex = floatVal.roundToInt().coerceIn(0, presetValues.lastIndex)
                    if (presetValues[newIndex] != currentRateLimit) {
                        HapticUtil.performSliderHaptic(view)
                        if (isHasWritePermission) {
                            viewModel.setNetworkDownloadRateLimit(presetValues[newIndex], context)
                        } else {
                            requestingPermissionFor = NetworkPermissionModule.RATE_LIMIT
                        }
                    }
                },
                valueRange = 0f..(presetValues.lastIndex.toFloat()),
                steps = presetValues.size - 2,
                increment = 1f,
                valueFormatter = { floatVal ->
                    val idx = floatVal.roundToInt().coerceIn(0, presetLabels.lastIndex)
                    presetLabels[idx]
                },
                iconRes = R.drawable.rounded_cell_wifi_24,
                enabled = true,
                modifier = Modifier.highlight(highlightSetting == "network_download_rate_limit_slider"),
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
                modifier = Modifier.highlight(highlightSetting == "mobile_data_always_on_toggle"),
            )

            IconToggleItem(
                title = stringResource(R.string.feat_wireless_display_certification_title),
                description = stringResource(R.string.feat_wireless_display_certification_desc),
                isChecked = viewModel.isWirelessDisplayCertificationEnabled.value,
                onCheckedChange = { enabled ->
                    if (isHasWritePermission) {
                        viewModel.setWirelessDisplayCertificationEnabled(enabled, context)
                    } else {
                        requestingPermissionFor =
                            NetworkPermissionModule.WIRELESS_DISPLAY_CERTIFICATION
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!isHasWritePermission) {
                        requestingPermissionFor =
                            NetworkPermissionModule.WIRELESS_DISPLAY_CERTIFICATION
                    }
                },
                iconRes = R.drawable.rounded_cast_24,
                modifier = Modifier.highlight(highlightSetting == "wireless_display_certification_toggle"),
            )

            IconToggleItem(
                title = stringResource(R.string.feat_sim_names_title),
                description = stringResource(R.string.feat_sim_names_desc),
                iconRes = R.drawable.rounded_android_cell_dual_4_bar_24,
                showToggle = false,
                onClick = {
                    if (hasSimNamesPermission) {
                        showSimNamesSheet = true
                    } else {
                        requestingPermissionFor = NetworkPermissionModule.SIM_NAMES
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "sim_names_item"),
            )
        }
    }
}
