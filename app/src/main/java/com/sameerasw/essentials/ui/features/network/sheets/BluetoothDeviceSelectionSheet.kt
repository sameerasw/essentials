/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Network
 * File: BluetoothDeviceSelectionSheet.kt
 * Description: UI component and settings composable for Network feature domain.
 */

package com.sameerasw.essentials.ui.core.sheets

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.cards.SelectionCardItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.BluetoothPairedDevicesUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDeviceSelectionSheet(
    onDismiss: () -> Unit,
    onSave: (address: String, name: String) -> Unit,
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    val devices =
        remember(hasPermission) {
            if (hasPermission) BluetoothPairedDevicesUtil.getPairedDevices(context) else emptyList()
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.diy_select_bluetooth_device_title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (!hasPermission) {
                TranslatableText(
                    stringResId = R.string.diy_bluetooth_permission_required,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (devices.isEmpty()) {
                TranslatableText(
                    stringResId = R.string.diy_no_paired_bluetooth_devices,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RoundedCardContainer {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(devices) { device ->
                            SelectionCardItem(
                                title = device.name,
                                description = device.address,
                                iconRes = R.drawable.rounded_bluetooth_24,
                                onClick = { onSave(device.address, device.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}
