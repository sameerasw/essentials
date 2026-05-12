package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.ShutUpAppConfig
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.components.sheets.ShutUpPerAppSettingsSheet
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun ShutUpSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }
    var selectedConfigForEditing by remember { mutableStateOf<ShutUpAppConfig?>(null) }
    
    val configs by viewModel.shutUpConfigs

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
            FeatureCard(
                title = stringResource(R.string.shut_up_select_apps_title),
                description = stringResource(R.string.shut_up_select_apps_desc),
                iconRes = R.drawable.rounded_app_registration_24,
                isEnabled = true,
                showToggle = false,
                hasMoreSettings = true,
                onToggle = {},
                onClick = { isAppSelectionSheetOpen = true }
            )
        }


        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            configs.forEach { config ->
                val appName = remember(config.packageName) {
                    try {
                        val appInfo = context.packageManager.getApplicationInfo(config.packageName, 0)
                        context.packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        config.packageName
                    }
                }

                val appIconPainter = remember(config.packageName) {
                    try {
                        val drawable = context.packageManager.getApplicationIcon(config.packageName)
                        androidx.compose.ui.graphics.painter.BitmapPainter(
                            AppUtil.drawableToBitmap(drawable).asImageBitmap()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                FeatureCard(
                    title = appName,
                    description = config.packageName,
                    isEnabled = true,
                    onToggle = {},
                    onClick = { selectedConfigForEditing = config },
                    iconPainter = appIconPainter,
                    showToggle = false,
                    hasMoreSettings = true,
                    customTrailingContent = {
                        IconButton(
                            onClick = {
                                viewModel.createShutUpShortcut(context, config)
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_add_24),
                                contentDescription = stringResource(R.string.action_create_shortcut),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    additionalMenuItems = { onDismiss ->
                        SegmentedDropdownMenuItem(
                            text = { Text(stringResource(R.string.action_remove)) },
                            onClick = {
                                onDismiss()
                                viewModel.removeShutUpConfig(config.packageName)
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_delete_24),
                                    contentDescription = null
                                )
                            }
                        )
                    }
                )
            }
        }

        Text(
            text = stringResource(R.string.shut_up_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isAppSelectionSheetOpen) {
            AppSelectionSheet(
                onDismissRequest = { isAppSelectionSheetOpen = false },
                onLoadApps = { ctx -> 
                    viewModel.shutUpConfigs.value.map { AppSelection(it.packageName, true) }
                },
                onSaveApps = { ctx, apps -> viewModel.saveShutUpSelectedApps(ctx, apps) }
            )
        }

        if (selectedConfigForEditing != null) {
            val frozenApps = remember { viewModel.loadFreezeSelectedApps(context) }
            val isFrozen = remember(selectedConfigForEditing) {
                frozenApps.any { it.packageName == selectedConfigForEditing?.packageName }
            }
            
            ShutUpPerAppSettingsSheet(
                onDismissRequest = { selectedConfigForEditing = null },
                config = configs.find { it.packageName == selectedConfigForEditing?.packageName } ?: selectedConfigForEditing!!,
                onConfigChanged = { viewModel.updateShutUpConfig(it) },
                onCreateShortcut = { viewModel.createShutUpShortcut(context, it) },
                isFrozen = isFrozen,
                viewModel = viewModel
            )
        }
    }
}
