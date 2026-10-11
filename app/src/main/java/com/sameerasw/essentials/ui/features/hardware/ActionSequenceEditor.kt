/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Hardware Features
 * File: ActionSequenceEditor.kt
 * Description: Reusable editor for an ordered sequence of DIY actions (add, configure, reorder, remove).
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionRegistry
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.ui.components.CategoryExpandableSection
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.CustomSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.DimWallpaperSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.HilightEffectSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.ScreenOffSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.OpenActivityPicker
import com.sameerasw.essentials.ui.core.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.ChargingModeSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.NotificationLightingActionSheet
import com.sameerasw.essentials.ui.core.sheets.OverlayControlSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.SoundModeSettingsSheet
import com.sameerasw.essentials.ui.features.apps.sheets.KeyboardSelectionSheet
import com.sameerasw.essentials.ui.features.audio.sheets.SetVolumeSettingsSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import java.util.concurrent.atomic.AtomicLong
import sh.calvin.reorderable.ReorderableColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSequenceEditor(
    viewModel: MainViewModel,
    actions: List<Action>,
    onActionsChange: (List<Action>) -> Unit,
    screenOnOnly: Boolean,
    listKey: Any,
    emptyText: String,
    modifier: Modifier = Modifier,
    maxActions: Int = Int.MAX_VALUE,
    categories: List<ActionRegistry.ActionCategory>? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var showFlashlightOptions by remember { mutableStateOf(false) }
    val showLikeSongOptions = remember { mutableStateOf(false) }

    // Action Config Sheets State
    var showDimSettings by remember { mutableStateOf(false) }
    var showScreenOffSettings by remember { mutableStateOf(false) }
    var showDeviceEffectsSettings by remember { mutableStateOf(false) }
    var showSoundModeSettings by remember { mutableStateOf(false) }
    var showChargingModeSettings by remember { mutableStateOf(false) }
    var showNotificationLightingSettings by remember { mutableStateOf(false) }
    var showOverlayControlSettings by remember { mutableStateOf(false) }
    var showSometimesEssentialsSettings by remember { mutableStateOf(false) }
    var showFreezeTagSettings by remember { mutableStateOf(false) }
    var showOpenAppSettings by remember { mutableStateOf(false) }
    var showOpenActivitySettings by remember { mutableStateOf(false) }
    var showFreezeAppsSettings by remember { mutableStateOf(false) }
    var temporarySelectedAppsForAction by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSetKeyboardSheet by remember { mutableStateOf(false) }
    var showCustomSettingsSettings by remember { mutableStateOf(false) }
    var showSetVolumeSettings by remember { mutableStateOf(false) }
    var showHilightSettings by remember { mutableStateOf(false) }
    var configAction by remember { mutableStateOf<Action?>(null) }
    var configIndex by remember { mutableStateOf<Int?>(null) }
    var showAddActionSheet by remember { mutableStateOf(false) }

    // Missing permission handling sheet
    var showPermissionSheet by remember { mutableStateOf(false) }
    var permissionKeysToShow by remember { mutableStateOf<List<String>>(emptyList()) }
    var permissionFeatureTitle by remember { mutableStateOf<Any>("") }

    fun getMissingPermissionsHelper(action: Action?): List<String> {
        if (action == null) return emptyList()
        val resolvedPermissions =
            action.permissions
                .map { permKey ->
                    if (permKey == "SHIZUKU" || permKey == "ROOT") {
                        if (com.sameerasw.essentials.utils.ShellUtils
                                .isRootEnabled(context)
                        ) {
                            "ROOT"
                        } else {
                            "SHIZUKU"
                        }
                    } else {
                        permKey
                    }
                }.distinct()

        return resolvedPermissions.filter { permKey ->
            when (permKey) {
                "SHIZUKU" -> !viewModel.isShizukuPermissionGranted.value
                "ROOT" -> !viewModel.isRootPermissionGranted.value
                "WRITE_SETTINGS" -> !viewModel.isWriteSettingsEnabled.value
                "NOTIFICATION_POLICY" -> !viewModel.isNotificationPolicyAccessGranted.value
                "WRITE_SECURE_SETTINGS" -> !viewModel.isWriteSecureSettingsEnabled.value
                "DRAW_OVERLAYS" -> !viewModel.isOverlayPermissionGranted.value
                "ACCESSIBILITY" -> !viewModel.isAccessibilityEnabled.value
                else -> false
            }
        }
    }

    val rowIdCounter = remember { AtomicLong() }
    val rowIds = remember(listKey) { mutableListOf<Long>() }
    if (rowIds.size != actions.size) {
        rowIds.clear()
        actions.forEach { _ -> rowIds.add(rowIdCounter.getAndIncrement()) }
    }

    fun updateActions(newActions: List<Action>) = onActionsChange(newActions)

    val onActionSelected: (Action) -> Unit = { action ->
        val index = configIndex
        if (index != null && index in actions.indices) {
            updateActions(actions.toMutableList().apply { this[index] = action })
        }
    }

    fun showMissingPermissionSheet(
        action: Action,
        missing: List<String>,
    ) {
        permissionKeysToShow = missing
        permissionFeatureTitle = action.title
        showPermissionSheet = true
    }

    fun openActionSettings(
        index: Int,
        action: Action,
    ) {
        if (action is Action.ToggleFlashlight) {
            showFlashlightOptions = true
            return
        }
        if (action is Action.LikeCurrentSong) {
            showLikeSongOptions.value = true
            return
        }
        val missing = getMissingPermissionsHelper(action)
        if (missing.isNotEmpty()) {
            showMissingPermissionSheet(action, missing)
            return
        }

        configIndex = index
        configAction = action
        when (action) {
            is Action.DimWallpaper -> showDimSettings = true
            is Action.ScreenOff -> showScreenOffSettings = true
            is Action.DeviceEffects -> showDeviceEffectsSettings = true
            is Action.SoundMode -> showSoundModeSettings = true
            is Action.SetChargingMode -> showChargingModeSettings = true
            is Action.TriggerNotificationLighting -> showNotificationLightingSettings = true
            is Action.OverlayControl -> showOverlayControlSettings = true
            is Action.SometimesEssentials -> showSometimesEssentialsSettings = true
            is Action.FreezeTag -> showFreezeTagSettings = true
            is Action.OpenApp -> showOpenAppSettings = true
            is Action.OpenActivity -> showOpenActivitySettings = true
            is Action.FreezeApps -> {
                temporarySelectedAppsForAction = action.packageNames
                showFreezeAppsSettings = true
            }
            is Action.UnfreezeApps -> {
                temporarySelectedAppsForAction = action.packageNames
                showFreezeAppsSettings = true
            }
            is Action.Keyboard -> showSetKeyboardSheet = true
            is Action.SetVolume -> showSetVolumeSettings = true
            is Action.Hilight -> showHilightSettings = true
            is Action.CustomSettings -> showCustomSettingsSettings = true
            else -> {}
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RoundedCardContainer(spacing = 2.dp) {
            if (actions.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                            ).padding(16.dp),
                )
            } else if (maxActions == 1) {
                val action = actions.first()
                RemapSequenceItem(
                    position = 1,
                    action = action,
                    isDragging = false,
                    hasSettings = hasActionSettings(action),
                    dragHandleModifier = Modifier,
                    showOrder = false,
                    onSettingsClick = { openActionSettings(0, action) },
                    onRemove = {
                        rowIds.clear()
                        updateActions(emptyList())
                    },
                )
            } else {
                ReorderableColumn(
                    list = actions,
                    onSettle = { fromIndex, toIndex ->
                        rowIds.add(toIndex, rowIds.removeAt(fromIndex))
                        updateActions(
                            actions.toMutableList().apply { add(toIndex, removeAt(fromIndex)) },
                        )
                    },
                    onMove = { HapticUtil.performUIHaptic(view) },
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) { index, action, isDragging ->
                    key(rowIds.getOrNull(index) ?: index) {
                        ReorderableItem {
                            RemapSequenceItem(
                                position = index + 1,
                                action = action,
                                isDragging = isDragging,
                                hasSettings = hasActionSettings(action),
                                dragHandleModifier = Modifier.draggableHandle(),
                                showOrder = true,
                                onSettingsClick = { openActionSettings(index, action) },
                                onRemove = {
                                    if (index in rowIds.indices) rowIds.removeAt(index)
                                    updateActions(actions.toMutableList().apply { removeAt(index) })
                                },
                            )
                        }
                    }
                }
            }

            if (actions.size < maxActions) {
                RemapAddActionItem(onClick = { showAddActionSheet = true })
            }
        }

        if (actions.size > 1) {
            Text(
                text = stringResource(R.string.button_remap_sequence_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            )
        }
    }

    if (showAddActionSheet) {
        RemapAddActionSheet(
            screenOnOnly = screenOnOnly,
            categories = categories,
            onDismiss = { showAddActionSheet = false },
            onActionPicked = { action ->
                showAddActionSheet = false
                val newIndex = actions.size
                rowIds.add(rowIdCounter.getAndIncrement())
                updateActions(actions + action)
                val missing = getMissingPermissionsHelper(action)
                if (missing.isNotEmpty()) {
                    showMissingPermissionSheet(action, missing)
                } else if (action.isConfigurable) {
                    openActionSettings(newIndex, action)
                }
            },
        )
    }

    // Config Bottom Sheets
    if (showLikeSongOptions.value) {
        LikeSongSettingsSheet(
            onDismiss = { showLikeSongOptions.value = false },
            viewModel = viewModel,
            context = context,
        )
    }

    if (showFlashlightOptions) {
        ModalBottomSheet(
            onDismissRequest = { showFlashlightOptions = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.flashlight_options_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                RoundedCardContainer(spacing = 2.dp) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_blur_on_24,
                        title = stringResource(R.string.flashlight_fade_title),
                        description = stringResource(R.string.flashlight_fade_desc),
                        isChecked = viewModel.isFlashlightFadeEnabled.value,
                        onCheckedChange = { viewModel.setFlashlightFadeEnabled(it, context) },
                    )

                    IconToggleItem(
                        iconRes = R.drawable.rounded_flashlight_on_24,
                        title = stringResource(R.string.flashlight_always_off_title),
                        description = stringResource(R.string.flashlight_always_off_desc),
                        isChecked = viewModel.isFlashlightAlwaysTurnOffEnabled.value,
                        onCheckedChange = {
                            viewModel.setFlashlightAlwaysTurnOffEnabled(it, context)
                        },
                    )
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showFlashlightOptions = false
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(stringResource(R.string.action_done))
                }
            }
        }
    }

    if (showDimSettings && configAction is Action.DimWallpaper) {
        DimWallpaperSettingsSheet(
            initialAction = configAction as Action.DimWallpaper,
            onDismiss = { showDimSettings = false },
            onSave = { newAction ->
                showDimSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showScreenOffSettings && configAction is Action.ScreenOff) {
        ScreenOffSettingsSheet(
            initialAction = configAction as Action.ScreenOff,
            onDismiss = { showScreenOffSettings = false },
            onSave = { newAction ->
                showScreenOffSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showDeviceEffectsSettings && configAction is Action.DeviceEffects) {
        com.sameerasw.essentials.ui.core.sheets.DeviceEffectsSettingsSheet(
            initialAction = configAction as Action.DeviceEffects,
            onDismiss = { showDeviceEffectsSettings = false },
            onSave = { newAction ->
                showDeviceEffectsSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showSoundModeSettings && configAction is Action.SoundMode) {
        SoundModeSettingsSheet(
            initialAction = configAction as Action.SoundMode,
            onDismiss = { showSoundModeSettings = false },
            onSave = { newAction ->
                showSoundModeSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showChargingModeSettings && configAction is Action.SetChargingMode) {
        ChargingModeSettingsSheet(
            initialAction = configAction as Action.SetChargingMode,
            onDismiss = { showChargingModeSettings = false },
            onSave = { newAction ->
                showChargingModeSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showNotificationLightingSettings && configAction is Action.TriggerNotificationLighting) {
        NotificationLightingActionSheet(
            initialAction = configAction as Action.TriggerNotificationLighting,
            onDismiss = { showNotificationLightingSettings = false },
            onSave = { newAction ->
                showNotificationLightingSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showOverlayControlSettings && configAction is Action.OverlayControl) {
        OverlayControlSettingsSheet(
            initialAction = configAction as Action.OverlayControl,
            onDismiss = { showOverlayControlSettings = false },
            onSave = { newAction ->
                showOverlayControlSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showSetVolumeSettings && configAction is Action.SetVolume) {
        SetVolumeSettingsSheet(
            initialAction = configAction as Action.SetVolume,
            onDismiss = { showSetVolumeSettings = false },
            onSave = { newAction ->
                showSetVolumeSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showHilightSettings && configAction is Action.Hilight) {
        HilightEffectSettingsSheet(
            initialAction = configAction as Action.Hilight,
            onDismiss = { showHilightSettings = false },
            onSave = { newAction ->
                showHilightSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showSometimesEssentialsSettings && configAction is Action.SometimesEssentials) {
        com.sameerasw.essentials.ui.core.sheets.SometimesEssentialsSettingsSheet(
            initialAction = configAction as Action.SometimesEssentials,
            onDismiss = { showSometimesEssentialsSettings = false },
            onSave = { newAction ->
                showSometimesEssentialsSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showFreezeTagSettings && configAction is Action.FreezeTag) {
        val availableTags =
            remember {
                com.sameerasw.essentials.data.repository
                    .SettingsRepository(context)
                    .getFreezeTags()
            }
        com.sameerasw.essentials.ui.core.sheets.FreezeTagSettingsSheet(
            initialAction = configAction as Action.FreezeTag,
            availableTags = availableTags,
            onDismiss = { showFreezeTagSettings = false },
            onSave = { newAction ->
                showFreezeTagSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showOpenAppSettings) {
        SingleAppSelectionSheet(
            includeSelf = true,
            onDismissRequest = { showOpenAppSettings = false },
            onAppSelected = { app ->
                val newAction = Action.OpenApp(packageName = app.packageName)
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showOpenActivitySettings) {
        OpenActivityPicker(
            onDismiss = { showOpenActivitySettings = false },
            onActivitySelected = { newAction ->
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showFreezeAppsSettings && (configAction is Action.FreezeApps || configAction is Action.UnfreezeApps)) {
        AppSelectionSheet(
            restrictSystemApps = !viewModel.isEnableUnsupportedFeatures.value,
            showInvertSelection = false,
            onDismissRequest = {
                val finalAction =
                    when (val action = configAction) {
                        is Action.FreezeApps -> action.copy(packageNames = temporarySelectedAppsForAction)
                        is Action.UnfreezeApps -> action.copy(packageNames = temporarySelectedAppsForAction)
                        else -> configAction
                    }
                if (finalAction != null) {
                    onActionSelected(finalAction)
                }
                showFreezeAppsSettings = false
                configAction = null
            },
            onLoadApps = {
                temporarySelectedAppsForAction.map { AppSelection(it, true) }
            },
            onSaveApps = { _, selections ->
                temporarySelectedAppsForAction =
                    selections.filter { it.isEnabled }.map { it.packageName }
            },
        )
    }

    if (showSetKeyboardSheet && configAction is Action.Keyboard) {
        KeyboardSelectionSheet(
            onDismissRequest = { newIme ->
                showSetKeyboardSheet = false
                onActionSelected(Action.Keyboard(newIme))
                configAction = null
            },
            selectedIme = (configAction as? Action.Keyboard)?.inputMethodId,
        )
    }

    if (showCustomSettingsSettings && configAction is Action.CustomSettings) {
        CustomSettingsSheet(
            initialAction = configAction as Action.CustomSettings,
            onDismiss = { showCustomSettingsSettings = false },
            onSave = { newAction ->
                showCustomSettingsSettings = false
                onActionSelected(newAction)
                configAction = null
            },
        )
    }

    if (showPermissionSheet) {
        val permissionItems =
            com.sameerasw.essentials.utils.PermissionUIHelper.getPermissionItems(
                permissionKeysToShow,
                context,
                viewModel,
            )
        if (permissionItems.isNotEmpty()) {
            com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet(
                onDismissRequest = {
                    showPermissionSheet = false
                    permissionKeysToShow = emptyList()
                },
                featureTitle = permissionFeatureTitle,
                permissions = permissionItems,
            )
        }
    }
}

@Composable
private fun RemapSequenceItem(
    position: Int,
    action: Action,
    isDragging: Boolean,
    hasSettings: Boolean,
    dragHandleModifier: Modifier,
    showOrder: Boolean,
    onSettingsClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val view = LocalView.current
    val title = stringResource(action.title)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color =
                        if (isDragging) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            MaterialTheme.colorScheme.surfaceBright
                        },
                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                ).padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showOrder) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_drag_handle_24),
                contentDescription = stringResource(R.string.content_desc_drag_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    dragHandleModifier
                        .padding(8.dp)
                        .size(24.dp),
            )

            Text(
                text = position.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Spacer(modifier = Modifier.size(8.dp))
        }

        Icon(
            painter = painterResource(id = action.icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (hasSettings) {
            IconButton(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    onSettingsClick()
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_settings_24),
                    contentDescription = stringResource(R.string.content_desc_settings),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        IconButton(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                onRemove()
            },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_close_24),
                contentDescription = stringResource(R.string.action_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RemapAddActionItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable {
                    HapticUtil.performUIHaptic(view)
                    onClick()
                }.background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                ).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.rounded_add_24),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.button_remap_add_action),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun RemapAddActionSheet(
    screenOnOnly: Boolean,
    categories: List<ActionRegistry.ActionCategory>?,
    onDismiss: () -> Unit,
    onActionPicked: (Action) -> Unit,
) {
    val view = LocalView.current
    val actionCategories =
        remember(screenOnOnly, categories) { categories ?: ActionRegistry.getCategories(screenOnOnly = screenOnOnly) }
    var expandedActionCategory by remember(screenOnOnly) {
        mutableStateOf(actionCategories.firstOrNull()?.titleRes)
    }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.button_remap_add_action),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp),
            )

            actionCategories.forEach { category ->
                CategoryExpandableSection(
                    title = stringResource(category.titleRes),
                    itemCount = category.actions.size,
                    isExpanded = expandedActionCategory == category.titleRes,
                    onToggleExpand = {
                        expandedActionCategory =
                            if (expandedActionCategory == category.titleRes) null else category.titleRes
                    },
                ) {
                    category.actions.forEach { action ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HapticUtil.performUIHaptic(view)
                                        onActionPicked(action)
                                    }.background(
                                        color = MaterialTheme.colorScheme.surfaceBright,
                                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                                    ).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = action.icon),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(action.title),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun hasActionSettings(action: Action): Boolean =
    action.isConfigurable || action is Action.ToggleFlashlight || action is Action.LikeCurrentSong
