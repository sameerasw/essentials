package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.res.painterResource
import android.provider.Settings
import android.content.Intent
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.components.cards.AppToggleItem
import com.sameerasw.essentials.domain.registry.PermissionRegistry

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
    
    var isMenuExpanded by remember { mutableStateOf(false) }

    val freezeInteractionSource = remember { MutableInteractionSource() }
    val unfreezeInteractionSource = remember { MutableInteractionSource() }
    val moreInteractionSource = remember { MutableInteractionSource() }

    var showPermissionSheet by remember { mutableStateOf(false) }
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    var initialEnabledPackageNames by remember { mutableStateOf<Set<String>?>(null) }

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
            text = stringResource(R.string.settings_section_app_control),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .highlight(highlightKey == "freeze_all_manual")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Freeze Button
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.freezeAllAuto(context)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isShizukuAvailable && isShizukuPermissionGranted,
                    shape = ButtonDefaults.shape // Keep default look
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_mode_cool_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.action_freeze))
                }

                // Unfreeze Button
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.unfreezeAllAuto(context)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isShizukuAvailable && isShizukuPermissionGranted,
                    shape = ButtonDefaults.shape
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_mode_cool_off_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.action_unfreeze))
                }

                // More Menu Button
                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        isMenuExpanded = true
                    },
                    enabled = isShizukuAvailable && isShizukuPermissionGranted
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_more_vert_24),
                        contentDescription = stringResource(R.string.content_desc_more_options)
                    )

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_freeze_all)) },
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.freezeAllManual(context)
                                isMenuExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_mode_cool_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_unfreeze_all)) },
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.unfreezeAllManual(context)
                                isMenuExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_mode_cool_off_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

            FeatureCard(
                title = R.string.freeze_pick_apps_title,
                description = R.string.freeze_pick_apps_desc,
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
            text = stringResource(R.string.settings_section_automation),
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
                title = stringResource(R.string.freeze_when_locked_title),
                isChecked = viewModel.isFreezeWhenLockedEnabled.value,
                onCheckedChange = { enabled ->
                    if (enabled && !isAccessibilityEnabled) {
                        showPermissionSheet = true
                    } else {
                        viewModel.setFreezeWhenLockedEnabled(enabled, context)
                    }
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
                    text = stringResource(R.string.freeze_delay_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (viewModel.isFreezeWhenLockedEnabled.value) 
                        MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                
                val labels = listOf(
                    stringResource(R.string.delay_immediate),
                    stringResource(R.string.delay_1m),
                    stringResource(R.string.delay_5m),
                    stringResource(R.string.delay_15m),
                    stringResource(R.string.delay_manual)
                )
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
            if (initialEnabledPackageNames == null) {
                initialEnabledPackageNames = pickedApps.filter { it.isEnabled }.map { it.packageName }.toSet()
            }
            
            val sortedApps = remember(pickedApps, initialEnabledPackageNames) {
                val allowed = initialEnabledPackageNames ?: emptySet()
                pickedApps.sortedWith(
                    compareByDescending<com.sameerasw.essentials.domain.model.NotificationApp> { allowed.contains(it.packageName) }
                        .thenBy { it.appName.lowercase() }
                )
            }

            Text(
                text = stringResource(R.string.freeze_auto_freeze_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RoundedCardContainer(
                modifier = Modifier.fillMaxWidth()
            ) {
                sortedApps.forEach { app ->
                    AppToggleItem(
                        icon = app.icon,
                        title = app.appName,
                        isChecked = app.isEnabled,
                        onCheckedChange = { isChecked ->
                             viewModel.updateFreezeAppAutoFreeze(context, app.packageName, isChecked)
                        }
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.freeze_automation_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = stringResource(R.string.freeze_warning),
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

        if (showPermissionSheet) {
            PermissionsBottomSheet(
                onDismissRequest = { showPermissionSheet = false },
                featureTitle = R.string.permission_feature_freeze_locked,
                permissions = listOf(
                    PermissionItem(
                        iconRes = R.drawable.rounded_settings_accessibility_24,
                        title = R.string.permission_accessibility_title,
                        description = R.string.permission_accessibility_desc_freeze,
                        dependentFeatures = listOf(R.string.freeze_when_locked_title),
                        actionLabel = R.string.action_enable_in_settings,
                        action = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        isGranted = isAccessibilityEnabled
                    )
                )
            )
        }
    }
}
