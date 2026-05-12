package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.ShutUpAppConfig
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShutUpPerAppSettingsSheet(
    onDismissRequest: () -> Unit,
    config: ShutUpAppConfig,
    onConfigChanged: (ShutUpAppConfig) -> Unit,
    onCreateShortcut: (ShutUpAppConfig) -> Unit,
    isFrozen: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentConfig by remember(config) { mutableStateOf(config) }
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.shut_up_per_app_settings),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            RoundedCardContainer(
                modifier = Modifier,
                spacing = 2.dp,
                cornerRadius = 24.dp
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_settings_24,
                    title = stringResource(R.string.shut_up_disable_dev_options),
                    isChecked = currentConfig.disableDevOptions,
                    onCheckedChange = {
                        val newConfig = currentConfig.copy(disableDevOptions = it)
                        currentConfig = newConfig
                        onConfigChanged(newConfig)
                    }
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_adb_24,
                    title = stringResource(R.string.shut_up_disable_usb_debugging),
                    isChecked = currentConfig.disableUsbDebugging,
                    onCheckedChange = {
                        val newConfig = currentConfig.copy(disableUsbDebugging = it)
                        currentConfig = newConfig
                        onConfigChanged(newConfig)
                    }
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_android_wifi_4_bar_plus_24,
                    title = stringResource(R.string.shut_up_disable_wireless_debugging),
                    isChecked = currentConfig.disableWirelessDebugging,
                    onCheckedChange = {
                        val newConfig = currentConfig.copy(disableWirelessDebugging = it)
                        currentConfig = newConfig
                        onConfigChanged(newConfig)
                    }
                )
//                if (currentConfig.disableWirelessDebugging){
//                    IconToggleItem(
//                        iconRes = R.drawable.rounded_adb_24,
//                        title = stringResource(R.string.shut_up_auto_archive_notif_title),
//                        isChecked = restartShizuku,
//                        onCheckedChange = {
//                            val newConfig = currentConfig.copy(autoArchive = it)
//                            currentConfig = newConfig
//                            onConfigChanged(newConfig)
//                        }
//                    )
//                }
                IconToggleItem(
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    title = stringResource(R.string.shut_up_disable_accessibility),
                    isChecked = currentConfig.disableAccessibility,
                    onCheckedChange = {
                        val newConfig = currentConfig.copy(disableAccessibility = it)
                        currentConfig = newConfig
                        onConfigChanged(newConfig)
                    }
                )
            }

            RoundedCardContainer(
                modifier = Modifier,
                spacing = 2.dp,
                cornerRadius = 24.dp
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_snowflake_24,
                    title = stringResource(R.string.shut_up_auto_archive_notif_title),
                    isChecked = currentConfig.autoArchive,
                    onCheckedChange = { 
                        val newConfig = currentConfig.copy(autoArchive = it)
                        currentConfig = newConfig
                        onConfigChanged(newConfig)
                    }
                )
            }

            Button(
                onClick = {
                    onCreateShortcut(currentConfig)
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.rounded_open_in_new_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.action_create_shortcut))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
