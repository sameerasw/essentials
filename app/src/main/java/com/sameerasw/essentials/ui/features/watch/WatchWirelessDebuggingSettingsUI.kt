/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: WearOS Companion
 * File: WatchWirelessDebuggingSettingsUI.kt
 * Description: Settings screen displaying watch IP, available ADB port, tap-to-copy, and manual refresh for Wireless Debugging.
 */

package com.sameerasw.essentials.ui.features.watch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.wearable.Wearable
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun WatchWirelessDebuggingSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember {
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
    }

    var isAdbWifiEnabled by remember {
        mutableStateOf(prefs.getBoolean("watch_adb_wifi_enabled", false))
    }
    var watchSsid by remember {
        mutableStateOf(prefs.getString("watch_wifi_ssid", "") ?: "")
    }
    var watchIp by remember {
        mutableStateOf(prefs.getString("watch_adb_wifi_ip", "") ?: "")
    }
    var watchPort by remember {
        mutableIntStateOf(prefs.getInt("watch_adb_wifi_port", -1))
    }

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            when (key) {
                "watch_adb_wifi_enabled" -> isAdbWifiEnabled = p.getBoolean(key, false)
                "watch_wifi_ssid" -> watchSsid = p.getString(key, "") ?: ""
                "watch_adb_wifi_ip" -> watchIp = p.getString(key, "") ?: ""
                "watch_adb_wifi_port" -> watchPort = p.getInt(key, -1)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun copyToClipboard(text: String) {
        if (text.isBlank()) return
        HapticUtil.performUIHaptic(view)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ADB Info", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.watch_wireless_debugging_copied), Toast.LENGTH_SHORT).show()
    }

    fun requestStatusRefresh() {
        HapticUtil.performUIHaptic(view)
        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/request_watch_status", byteArrayOf())
            }
        }
    }

    val isSsidAvailable = watchSsid.isNotBlank()
    val isIpAvailable = watchIp.isNotBlank()
    val isPortAvailable = watchPort > 0
    val adbConnectCommand = if (isIpAvailable && isPortAvailable) "adb connect $watchIp:$watchPort" else ""

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_adb_24,
                title = stringResource(R.string.feat_watch_wireless_debugging_title),
                description = stringResource(R.string.feat_watch_wireless_debugging_desc),
                isChecked = isAdbWifiEnabled,
                onCheckedChange = { enabled ->
                    HapticUtil.performUIHaptic(view)
                    val nodeClient = Wearable.getNodeClient(context)
                    val messageClient = Wearable.getMessageClient(context)
                    nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                        for (node in nodes) {
                            messageClient.sendMessage(node.id, "/toggle_watch_adb_wifi", byteArrayOf())
                        }
                    }
                }
            )
        }

        Text(
            text = stringResource(R.string.watch_wireless_debugging_info_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp)
        )

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_android_wifi_4_bar_plus_24,
                title = stringResource(R.string.watch_wireless_debugging_wifi_title),
                description = if (isSsidAvailable) watchSsid else stringResource(R.string.watch_wireless_debugging_unavailable),
                enabled = isSsidAvailable,
                showToggle = false,
                onClick = { copyToClipboard(watchSsid) }
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_android_wifi_4_bar_plus_24,
                title = stringResource(R.string.watch_wireless_debugging_ip_title),
                description = if (isIpAvailable) watchIp else stringResource(R.string.watch_wireless_debugging_unavailable),
                enabled = isIpAvailable,
                showToggle = false,
                onClick = { copyToClipboard(watchIp) }
            )
            IconToggleItem(
                iconRes = R.drawable.router_24px,
                title = stringResource(R.string.watch_wireless_debugging_port_title),
                description = if (isPortAvailable) watchPort.toString() else stringResource(R.string.watch_wireless_debugging_unavailable),
                enabled = isPortAvailable,
                showToggle = false,
                onClick = { copyToClipboard(watchPort.toString()) }
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_code_24,
                title = stringResource(R.string.watch_wireless_debugging_command_title),
                description = if (adbConnectCommand.isNotBlank()) adbConnectCommand else stringResource(R.string.watch_wireless_debugging_unavailable),
                enabled = adbConnectCommand.isNotBlank(),
                showToggle = false,
                onClick = { copyToClipboard(adbConnectCommand) }
            )
        }

        Button(
            onClick = { requestStatusRefresh() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_refresh_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.watch_wireless_debugging_refresh))
        }
    }
}
