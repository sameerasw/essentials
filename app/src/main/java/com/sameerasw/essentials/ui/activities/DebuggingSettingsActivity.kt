/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: DebuggingSettingsActivity.kt
 * Description: Bottom sheet dialog activity for toggling USB and Wireless Debugging, launched on long-pressing the Debugging QS Tile.
 */

package com.sameerasw.essentials.ui.activities

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

class DebuggingSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.check(context)
            }
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                DebuggingSettingsOverlay(onDismiss = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebuggingSettingsOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun isUsbDebuggingEnabled(): Boolean = com.sameerasw.essentials.utils.HiddenDebuggingUtils.isUsbDebuggingEnabled(context)

    fun isWifiDebuggingEnabled(): Boolean =
        try {
            Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
        } catch (_: Exception) {
            false
        }

    fun setUsbDebuggingEnabled(enabled: Boolean) {
        try {
            Settings.Global.putInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                if (enabled) 1 else 0,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setWifiDebuggingEnabled(enabled: Boolean) {
        try {
            Settings.Global.putInt(
                context.contentResolver,
                "adb_wifi_enabled",
                if (enabled) 1 else 0,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var isUsbEnabled by remember { mutableStateOf(isUsbDebuggingEnabled()) }
    var isWifiEnabled by remember { mutableStateOf(isWifiDebuggingEnabled()) }

    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        val observer =
            object : ContentObserver(handler) {
                override fun onChange(
                    selfChange: Boolean,
                    uri: Uri?,
                ) {
                    super.onChange(selfChange, uri)
                    isUsbEnabled = isUsbDebuggingEnabled()
                    isWifiEnabled = isWifiDebuggingEnabled()
                }
            }

        val adbUri = Settings.Global.getUriFor(Settings.Global.ADB_ENABLED)
        val adbWifiUri = Settings.Global.getUriFor("adb_wifi_enabled")

        if (adbUri != null) {
            context.contentResolver.registerContentObserver(adbUri, false, observer)
        }
        if (adbWifiUri != null) {
            context.contentResolver.registerContentObserver(adbWifiUri, false, observer)
        }

        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_adb_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = stringResource(R.string.tile_usb_debugging),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            RoundedCardContainer(spacing = 2.dp) {
                IconToggleItem(
                    iconRes = R.drawable.usb_debugging_24,
                    title = stringResource(R.string.usb_debugging_title),
                    description = stringResource(R.string.usb_debugging_desc),
                    isChecked = isUsbEnabled,
                    onCheckedChange = { enabled ->
                        HapticUtil.performUIHaptic(view)
                        isUsbEnabled = enabled
                        setUsbDebuggingEnabled(enabled)
                    },
                )

                IconToggleItem(
                    iconRes = R.drawable.wireless_debugging_24,
                    title = stringResource(R.string.wireless_debugging_title),
                    description = stringResource(R.string.wireless_debugging_desc),
                    isChecked = isWifiEnabled,
                    onCheckedChange = { enabled ->
                        HapticUtil.performUIHaptic(view)
                        isWifiEnabled = enabled
                        setWifiDebuggingEnabled(enabled)
                    },
                )
            }

            val settingsRepository =
                remember {
                    SettingsRepository(context)
                }
            var tapAction by remember {
                mutableStateOf(
                    settingsRepository.getString(
                        SettingsRepository.KEY_DEBUGGING_TILE_TAP_ACTION,
                        "both",
                    ) ?: "both",
                )
            }

            val tapActionOptions =
                listOf(
                    "both" to R.string.debugging_tap_action_both,
                    "usb" to R.string.debugging_tap_action_usb,
                    "wireless" to R.string.debugging_tap_action_wireless,
                )

            Text(
                text = stringResource(R.string.debugging_default_tap_action_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )

            RoundedCardContainer(spacing = 0.dp) {
                SegmentedPicker(
                    items = tapActionOptions.map { it.first },
                    selectedItem = tapAction,
                    onItemSelected = { selected ->
                        tapAction = selected
                        settingsRepository.putString(
                            SettingsRepository.KEY_DEBUGGING_TILE_TAP_ACTION,
                            selected,
                        )
                    },
                    labelProvider = { optionKey ->
                        val stringRes =
                            tapActionOptions.firstOrNull { it.first == optionKey }?.second
                                ?: R.string.debugging_tap_action_both
                        context.getString(stringRes)
                    },
                )
            }

            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    val devIntent =
                        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    context.startActivity(devIntent)
                    onDismiss()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_mobile_code_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.tile_developer_options),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
