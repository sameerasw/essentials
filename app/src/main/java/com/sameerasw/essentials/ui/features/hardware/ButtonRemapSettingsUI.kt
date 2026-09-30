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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sameerasw.essentials.ui.core.sheets.ScreenOffSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.ChargingModeSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.SoundModeSettingsSheet
import com.sameerasw.essentials.ui.features.apps.sheets.KeyboardSelectionSheet
import com.sameerasw.essentials.ui.features.audio.sheets.SetVolumeSettingsSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

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
    var showSometimesEssentialsSettings by remember { mutableStateOf(false) }
    var showFreezeTagSettings by remember { mutableStateOf(false) }
    var showOpenAppSettings by remember { mutableStateOf(false) }
    var showFreezeAppsSettings by remember { mutableStateOf(false) }
    var temporarySelectedAppsForAction by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSetKeyboardSheet by remember { mutableStateOf(false) }
    var showCustomSettingsSettings by remember { mutableStateOf(false) }
    var showSetVolumeSettings by remember { mutableStateOf(false) }
    var configAction by remember { mutableStateOf<Action?>(null) }

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
                else -> false
            }
        }
    }

    val currentAction: Action? =
        when (selectedScreenTab) {
            0 if selectedButtonTab == 0 -> viewModel.volumeUpActionOff.value
            0 if selectedButtonTab == 1 -> viewModel.volumeDownActionOff.value
            1 if selectedButtonTab == 0 -> viewModel.volumeUpActionOn.value
            else -> viewModel.volumeDownActionOn.value
        }

    val onActionSelected: (Action?) -> Unit = { action ->
        when (selectedScreenTab) {
            0 if selectedButtonTab == 0 -> viewModel.setVolumeUpActionOff(action, context)
            0 if selectedButtonTab == 1 -> viewModel.setVolumeDownActionOff(action, context)
            1 if selectedButtonTab == 0 -> viewModel.setVolumeUpActionOn(action, context)
            else -> viewModel.setVolumeDownActionOn(action, context)
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

                // None Option
                RoundedCardContainer(spacing = 2.dp) {
                    RemapActionItem(
                        title = stringResource(R.string.haptic_none),
                        isSelected = currentAction == null,
                        onClick = { onActionSelected(null) },
                        iconRes = R.drawable.rounded_do_not_disturb_on_24,
                    )
                }

                // Categorized Actions List
                val actionCategories =
                    remember(selectedScreenTab) {
                        ActionRegistry.getCategories(screenOnOnly = selectedScreenTab == 1)
                    }

                var expandedActionCategory by remember(selectedScreenTab, selectedButtonTab) {
                    mutableStateOf<Int?>(
                        actionCategories
                            .firstOrNull { category ->
                                category.actions.any { currentAction != null && it::class == currentAction::class }
                            }?.titleRes ?: actionCategories.firstOrNull()?.titleRes,
                    )
                }

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
                            val resolvedAction =
                                if (currentAction != null && currentAction::class == action::class) currentAction else action
                            val isSelected =
                                currentAction != null && currentAction::class == resolvedAction::class
                            val missing = getMissingPermissionsHelper(resolvedAction)

                            fun showMissingPermissionSheet() {
                                permissionKeysToShow = missing
                                permissionFeatureTitle = resolvedAction.title
                                showPermissionSheet = true
                            }

                            RemapActionItem(
                                title = stringResource(resolvedAction.title),
                                iconRes = resolvedAction.icon,
                                isSelected = isSelected,
                                hasSettings =
                                    resolvedAction.isConfigurable ||
                                        resolvedAction is Action.ToggleFlashlight ||
                                        resolvedAction is Action.LikeCurrentSong,
                                onClick = {
                                    onActionSelected(resolvedAction)
                                    if (missing.isNotEmpty()) {
                                        showMissingPermissionSheet()
                                    }
                                },
                                onSettingsClick = {
                                    if (resolvedAction is Action.ToggleFlashlight) {
                                        showFlashlightOptions = true
                                        return@RemapActionItem
                                    }
                                    if (resolvedAction is Action.LikeCurrentSong) {
                                        showLikeSongOptions.value = true
                                        return@RemapActionItem
                                    }
                                    if (missing.isNotEmpty()) {
                                        showMissingPermissionSheet()
                                        return@RemapActionItem
                                    }

                                    configAction = resolvedAction
                                    when (resolvedAction) {
                                        is Action.DimWallpaper -> showDimSettings = true
                                        is Action.ScreenOff -> showScreenOffSettings = true
                                        is Action.DeviceEffects -> showDeviceEffectsSettings = true
                                        is Action.SoundMode -> showSoundModeSettings = true
                                        is Action.SetChargingMode -> showChargingModeSettings = true
                                        is Action.SometimesEssentials -> showSometimesEssentialsSettings = true
                                        is Action.FreezeTag -> showFreezeTagSettings = true
                                        is Action.OpenApp -> showOpenAppSettings = true
                                        is Action.FreezeApps -> {
                                            temporarySelectedAppsForAction = resolvedAction.packageNames
                                            showFreezeAppsSettings = true
                                        }
                                        is Action.UnfreezeApps -> {
                                            temporarySelectedAppsForAction = resolvedAction.packageNames
                                            showFreezeAppsSettings = true
                                        }
                                        is Action.Keyboard -> showSetKeyboardSheet = true
                                        is Action.SetVolume -> showSetVolumeSettings = true
                                        is Action.CustomSettings -> showCustomSettingsSettings = true
                                        else -> {}
                                    }
                                },
                            )
                        }
                    }
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
