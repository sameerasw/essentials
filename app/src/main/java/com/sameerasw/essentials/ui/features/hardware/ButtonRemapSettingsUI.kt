/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Hardware Features
 * File: ButtonRemapSettingsUI.kt
 * Description: Composable screen for remapping volume and hardware keys with unified DIY actions.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionRegistry
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.RemapInput
import com.sameerasw.essentials.domain.model.RemapScreenState
import com.sameerasw.essentials.domain.model.RemapSlot
import com.sameerasw.essentials.shizuku.ShizukuPermissionHelper
import com.sameerasw.essentials.shizuku.ShizukuStatus
import com.sameerasw.essentials.ui.components.CategoryExpandableSection
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.HapticFeedbackPicker
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.CustomSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.DimWallpaperSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.ScreenOffSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.ChargingModeSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.NotificationLightingActionSheet
import com.sameerasw.essentials.ui.core.sheets.OverlayControlSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.SoundModeSettingsSheet
import com.sameerasw.essentials.ui.features.apps.sheets.KeyboardSelectionSheet
import com.sameerasw.essentials.ui.features.audio.sheets.SetVolumeSettingsSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import java.util.concurrent.atomic.AtomicLong
import sh.calvin.reorderable.ReorderableColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonRemapSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var selectedScreenTab by remember { mutableIntStateOf(0) } // 0: Off, 1: On
    var selectedButtonTab by remember { mutableIntStateOf(0) } // 0: Up, 1: Down
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
    var showFreezeAppsSettings by remember { mutableStateOf(false) }
    var temporarySelectedAppsForAction by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSetKeyboardSheet by remember { mutableStateOf(false) }
    var showCustomSettingsSettings by remember { mutableStateOf(false) }
    var showSetVolumeSettings by remember { mutableStateOf(false) }
    var configAction by remember { mutableStateOf<Action?>(null) }
    var configIndex by remember { mutableStateOf<Int?>(null) }
    var showAddActionSheet by remember { mutableStateOf(false) }

    // Missing permission handling sheet
    var showPermissionSheet by remember { mutableStateOf(false) }
    var permissionKeysToShow by remember { mutableStateOf<List<String>>(emptyList()) }
    var permissionFeatureTitle by remember { mutableStateOf<Any>("") }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var shizukuStatus by remember { mutableStateOf(ShizukuStatus.NOT_RUNNING) }
    val shizukuHelper = remember { ShizukuPermissionHelper(context) }

    // Check Shizuku status on resume
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    shizukuStatus = shizukuHelper.getStatus()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    val currentSlot =
        RemapSlot(
            input = if (selectedButtonTab == 0) RemapInput.VOLUME_UP else RemapInput.VOLUME_DOWN,
            screenState = if (selectedScreenTab == 0) RemapScreenState.OFF else RemapScreenState.ON,
        )
    val currentActions: List<Action> = viewModel.remapActions[currentSlot].orEmpty()

    val rowIdCounter = remember { AtomicLong() }
    val rowIds = remember(currentSlot) { mutableListOf<Long>() }
    if (rowIds.size != currentActions.size) {
        rowIds.clear()
        currentActions.forEach { _ -> rowIds.add(rowIdCounter.getAndIncrement()) }
    }

    fun updateActions(actions: List<Action>) = viewModel.setRemapActions(currentSlot, actions)

    val onActionSelected: (Action) -> Unit = { action ->
        val index = configIndex
        if (index != null && index in currentActions.indices) {
            updateActions(currentActions.toMutableList().apply { this[index] = action })
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
            is Action.CustomSettings -> showCustomSettingsSettings = true
            else -> {}
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Master Toggle
        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                iconRes = R.drawable.rounded_switch_access_3_24,
                title = stringResource(R.string.button_remap_enable_title),
                isChecked = viewModel.isButtonRemapEnabled.value,
                onCheckedChange = { viewModel.setButtonRemapEnabled(it, context) },
                modifier = Modifier.highlight(highlightSetting == "enable_remap"),
            )

            AnimatedVisibility(
                visible = viewModel.isButtonRemapEnabled.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_volume_up_24,
                        title = stringResource(R.string.button_remap_pause_on_volume_dialog_title),
                        isChecked = viewModel.isButtonRemapPauseOnVolumeDialog.value,
                        onCheckedChange = { viewModel.setButtonRemapPauseOnVolumeDialog(it, context) },
                        modifier = Modifier.highlight(highlightSetting == "pause_on_volume_dialog"),
                    )

                    val isRootEnabled =
                        com.sameerasw.essentials.utils.ShellUtils
                            .isRootEnabled(context)
                    IconToggleItem(
                        iconRes = if (isRootEnabled) R.drawable.rounded_numbers_24 else R.drawable.rounded_adb_24,
                        title = stringResource(R.string.button_remap_use_shizuku_title),
                        description = stringResource(R.string.button_remap_use_shizuku_desc),
                        isChecked = viewModel.isButtonRemapUseShizuku.value,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val shellHasPermission =
                                    com.sameerasw.essentials.utils.ShellUtils
                                        .hasPermission(context)
                                val shellIsAvailable =
                                    com.sameerasw.essentials.utils.ShellUtils
                                        .isAvailable(context)

                                if (shellHasPermission) {
                                    viewModel.setButtonRemapUseShizuku(true, context)
                                } else if (shellIsAvailable && !isRootEnabled) {
                                    shizukuHelper.requestPermission { _, grantResult ->
                                        if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            viewModel.setButtonRemapUseShizuku(true, context)
                                        }
                                    }
                                } else if (isRootEnabled && !shellHasPermission) {
                                    viewModel.setButtonRemapUseShizuku(true, context)
                                    com.sameerasw.essentials.utils.ShellUtils.runCommand(
                                        context,
                                        "id",
                                    )
                                } else {
                                    viewModel.setButtonRemapUseShizuku(true, context)
                                    val toastRes =
                                        if (isRootEnabled) R.string.root_not_available_toast else R.string.shizuku_not_running_toast
                                    android.widget.Toast
                                        .makeText(
                                            context,
                                            context.getString(toastRes),
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            } else {
                                viewModel.setButtonRemapUseShizuku(false, context)
                            }
                        },
                        modifier = Modifier.highlight(highlightSetting == "shizuku_remap"),
                    )

                    AnimatedVisibility(
                        visible = viewModel.isButtonRemapUseShizuku.value,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceBright,
                                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                                    ).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val shellAvailable =
                                com.sameerasw.essentials.utils.ShellUtils
                                    .isAvailable(context)
                            val shellPermission =
                                com.sameerasw.essentials.utils.ShellUtils
                                    .hasPermission(context)

                            val statusText =
                                if (shellPermission && viewModel.shizukuDetectedDevicePath.value != null) {
                                    stringResource(
                                        R.string.shizuku_detected_prefix,
                                        viewModel.shizukuDetectedDevicePath.value ?: "",
                                    )
                                } else if (isRootEnabled) {
                                    if (shellPermission) {
                                        "Root Access: Granted"
                                    } else if (shellAvailable) {
                                        "Root Access: Found"
                                    } else {
                                        "Root Access: Not Found"
                                    }
                                } else {
                                    stringResource(
                                        R.string.shizuku_status_prefix,
                                        shizukuStatus.name,
                                    )
                                }

                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (shellPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )

                            if (!shellPermission &&
                                !isRootEnabled &&
                                shizukuStatus != ShizukuStatus.READY &&
                                shizukuStatus != ShizukuStatus.PERMISSION_NEEDED
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val shizukuPackage =
                                                com.sameerasw.essentials.utils.ShizukuUtils
                                                    .getShizukuPackageName(context)
                                            val intent =
                                                context.packageManager.getLaunchIntentForPackage(shizukuPackage)
                                            if (intent != null) context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding =
                                        androidx.compose.foundation.layout.PaddingValues(
                                            horizontal = 12.dp,
                                            vertical = 0.dp,
                                        ),
                                ) {
                                    Text(
                                        stringResource(R.string.shizuku_open_button),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = viewModel.isButtonRemapEnabled.value,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Haptic Feedback (Common)
                Text(
                    text = stringResource(R.string.settings_section_haptic),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RoundedCardContainer(spacing = 0.dp) {
                    HapticFeedbackPicker(
                        selectedFeedback = viewModel.remapHapticType.value,
                        onFeedbackSelected = { viewModel.setRemapHapticType(it, context) },
                        options =
                            listOf(
                                R.string.haptic_none to HapticFeedbackType.NONE,
                                R.string.haptic_tick to HapticFeedbackType.TICK,
                                R.string.haptic_double to HapticFeedbackType.DOUBLE,
                            ),
                        modifier = Modifier.highlight(highlightSetting == "remap_haptic"),
                    )
                }

                Text(
                    text = stringResource(R.string.button_remap_section_long_press),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Button Picker & Tabs
                RoundedCardContainer(spacing = 2.dp) {
                    val screenOptions =
                        listOf(
                            stringResource(R.string.screen_off),
                            stringResource(R.string.screen_on),
                        )
                    SegmentedPicker(
                        items = screenOptions,
                        selectedItem = if (selectedScreenTab == 0) screenOptions[0] else screenOptions[1],
                        onItemSelected = {
                            HapticUtil.performUIHaptic(view)
                            selectedScreenTab = screenOptions.indexOf(it)
                        },
                        labelProvider = { it },
                    )
                    val buttonOptions =
                        listOf(
                            stringResource(R.string.volume_up),
                            stringResource(R.string.volume_down),
                        )
                    SegmentedPicker(
                        items = buttonOptions,
                        selectedItem = if (selectedButtonTab == 0) buttonOptions[0] else buttonOptions[1],
                        onItemSelected = {
                            HapticUtil.performUIHaptic(view)
                            selectedButtonTab = buttonOptions.indexOf(it)
                        },
                        labelProvider = { it },
                    )
                }

                RoundedCardContainer(spacing = 2.dp) {
                    if (currentActions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.button_remap_no_actions),
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
                    } else {
                        ReorderableColumn(
                            list = currentActions,
                            onSettle = { fromIndex, toIndex ->
                                rowIds.add(toIndex, rowIds.removeAt(fromIndex))
                                updateActions(
                                    currentActions.toMutableList().apply { add(toIndex, removeAt(fromIndex)) },
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
                                        hasSettings =
                                            action.isConfigurable ||
                                                action is Action.ToggleFlashlight ||
                                                action is Action.LikeCurrentSong,
                                        dragHandleModifier = Modifier.draggableHandle(),
                                        onSettingsClick = { openActionSettings(index, action) },
                                        onRemove = {
                                            if (index in rowIds.indices) rowIds.removeAt(index)
                                            updateActions(currentActions.toMutableList().apply { removeAt(index) })
                                        },
                                    )
                                }
                            }
                        }
                    }

                    RemapAddActionItem(onClick = { showAddActionSheet = true })
                }

                if (currentActions.size > 1) {
                    Text(
                        text = stringResource(R.string.button_remap_sequence_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    )
                }
            }
        }

        // Hint
        RoundedCardContainer {
            Text(
                text =
                    if (selectedScreenTab == 0) {
                        stringResource(R.string.button_remap_screen_off_hint)
                    } else {
                        stringResource(R.string.button_remap_screen_on_hint)
                    },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showAddActionSheet) {
        RemapAddActionSheet(
            screenOnOnly = selectedScreenTab == 1,
            onDismiss = { showAddActionSheet = false },
            onActionPicked = { action ->
                showAddActionSheet = false
                val newIndex = currentActions.size
                rowIds.add(rowIdCounter.getAndIncrement())
                updateActions(currentActions + action)
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
fun RemapActionItem(
    title: Any, // Can be Int or String
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasSettings: Boolean = false,
    onSettingsClick: () -> Unit = {},
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
                ).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
        )

        val resolvedTitle =
            when (title) {
                is Int -> stringResource(id = title)
                is String -> title
                else -> ""
            }

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = resolvedTitle,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = resolvedTitle,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasSettings && isSelected) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_settings_24),
                    contentDescription = stringResource(R.string.content_desc_settings),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
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
    onDismiss: () -> Unit,
    onActionPicked: (Action) -> Unit,
) {
    val view = LocalView.current
    val actionCategories =
        remember(screenOnOnly) { ActionRegistry.getCategories(screenOnOnly = screenOnOnly) }
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
