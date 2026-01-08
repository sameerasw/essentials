package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FreezeSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }
    
    val isShizukuAvailable by viewModel.isShizukuAvailable
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
    val pickedApps by viewModel.freezePickedApps

    LaunchedEffect(Unit) {
        viewModel.refreshFreezePickedApps(context)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "App Control",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_mode_cool_24,
                title = "Freeze all apps",
                description = "Immediately freeze all picked apps",
                isChecked = false,
                onCheckedChange = { 
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.freezeAllManual(context)
                },
                enabled = isShizukuAvailable && isShizukuPermissionGranted,
                onDisabledClick = {
                    viewModel.requestShizukuPermission()
                },
                showToggle = false,
                modifier = Modifier.highlight(highlightKey == "freeze_all_manual")
            )

            FeatureCard(
                title = "Pick apps to freeze",
                description = "Choose which apps can be frozen",
                iconRes = R.drawable.rounded_app_registration_24,
                isEnabled = true,
                showToggle = false,
                hasMoreSettings = true,
                onToggle = {},
                onClick = { isAppSelectionSheetOpen = true },
                modifier = Modifier.highlight(highlightKey == "freeze_selected_apps")
            )
        }

        Text(
            text = "Automation",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_lock_clock_24,
                title = "Freeze when locked",
                isChecked = viewModel.isFreezeWhenLockedEnabled.value,
                onCheckedChange = { enabled ->
                    viewModel.setFreezeWhenLockedEnabled(enabled, context)
                },
                enabled = true,
                modifier = Modifier.highlight(highlightKey == "freeze_when_locked_enabled")
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                    )
                    .padding(horizontal = 32.dp, vertical = 18.dp)
                    .highlight(highlightKey == "freeze_lock_delay_index"),
            ) {
                Text(
                    text = "Freeze delay",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (viewModel.isFreezeWhenLockedEnabled.value) 
                        MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                
                val labels = listOf("Immediate", "1m", "5m", "15m", "Manual")
                Slider(
                    value = viewModel.freezeLockDelayIndex.intValue.toFloat(),
                    onValueChange = { viewModel.setFreezeLockDelayIndex(it.toInt(), context) },
                    valueRange = 0f..4f,
                    steps = 3,
                    enabled = viewModel.isFreezeWhenLockedEnabled.value,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        val isFreezePickedAppsLoading by viewModel.isFreezePickedAppsLoading

        if (isFreezePickedAppsLoading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                LoadingIndicator()
            }
        } else if (pickedApps.isNotEmpty()) {
            Text(
                text = "Auto freeze apps",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RoundedCardContainer(
                modifier = Modifier.fillMaxWidth(),
                spacing = 1.dp,
                cornerRadius = 24.dp
            ) {
                pickedApps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.updateFreezeAppAutoFreeze(context, app.packageName, !app.isEnabled)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            bitmap = app.icon.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            contentScale = ContentScale.Fit
                        )
                        
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Switch(
                            checked = app.isEnabled,
                            onCheckedChange = { isChecked ->
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.updateFreezeAppAutoFreeze(context, app.packageName, isChecked)
                            }
                        )
                    }
                }
            }
        }

        Text(
            text = "Freeze selected apps when the device locks. Choose a delay to avoid freezing apps if you unlock the screen shortly after turning it off.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Freezing system apps might be dangerous and may cause unexpected behavior.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isAppSelectionSheetOpen) {
            AppSelectionSheet(
                onDismissRequest = { isAppSelectionSheetOpen = false },
                onLoadApps = { viewModel.loadFreezeSelectedApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveFreezeSelectedApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled -> viewModel.updateFreezeAppEnabled(ctx, pkg, enabled) }
            )
        }
    }
}
