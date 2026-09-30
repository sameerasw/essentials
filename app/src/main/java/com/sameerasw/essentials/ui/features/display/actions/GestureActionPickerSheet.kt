package com.sameerasw.essentials.ui.features.display.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionRegistry
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.ui.components.CategoryExpandableSection
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.CustomSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.DeviceEffectsSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.DimWallpaperSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.FreezeTagSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.ScreenOffSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.SometimesEssentialsSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.SoundModeSettingsSheet
import com.sameerasw.essentials.ui.features.apps.sheets.KeyboardSelectionSheet
import com.sameerasw.essentials.ui.features.audio.sheets.SetVolumeSettingsSheet
import com.sameerasw.essentials.ui.features.system.LikeSongSettingsSheet
import com.sameerasw.essentials.ui.features.system.RemapActionItem
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun GestureActionPickerSheet(
    viewModel: MainViewModel,
    currentAction: Action?,
    onActionSelected: (Action?) -> Unit,
    onPickerClosed: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var showPicker by remember { mutableStateOf(true) }
    var showFlashlightOptions by remember { mutableStateOf(false) }
    val showLikeSongOptions = remember { mutableStateOf(false) }
    var showDimSettings by remember { mutableStateOf(false) }
    var showScreenOffSettings by remember { mutableStateOf(false) }
    var showDeviceEffectsSettings by remember { mutableStateOf(false) }
    var showSoundModeSettings by remember { mutableStateOf(false) }
    var showSometimesEssentialsSettings by remember { mutableStateOf(false) }
    var showFreezeTagSettings by remember { mutableStateOf(false) }
    var showOpenAppSettings by remember { mutableStateOf(false) }
    var showFreezeAppsSettings by remember { mutableStateOf(false) }
    var temporarySelectedAppsForAction by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSetKeyboardSheet by remember { mutableStateOf(false) }
    var showCustomSettingsSettings by remember { mutableStateOf(false) }
    var showSetVolumeSettings by remember { mutableStateOf(false) }
    var configAction by remember { mutableStateOf<Action?>(null) }
    var showPermissionSheet by remember { mutableStateOf(false) }
    var permissionKeysToShow by remember { mutableStateOf<List<String>>(emptyList()) }
    var permissionFeatureTitle by remember { mutableStateOf<Any>("") }

    val anyOpen = showPicker || showFlashlightOptions || showLikeSongOptions.value || showDimSettings ||
        showScreenOffSettings || showDeviceEffectsSettings || showSoundModeSettings ||
        showSometimesEssentialsSettings || showFreezeTagSettings || showOpenAppSettings ||
        showFreezeAppsSettings || showSetKeyboardSheet || showCustomSettingsSettings ||
        showSetVolumeSettings || showPermissionSheet
    LaunchedEffect(anyOpen) {
        if (!anyOpen) onPickerClosed()
    }
    if (!anyOpen) return

    fun getMissingPermissionsHelper(action: Action?): List<String> {
        if (action == null) return emptyList()
        val resolvedPermissions = action.permissions.map { permKey ->
            if (permKey == "SHIZUKU" || permKey == "ROOT") {
                if (ShellUtils.isRootEnabled(context)) "ROOT" else "SHIZUKU"
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

    if (showPicker) {
        EssentialsBottomSheet(
            onDismissRequest = { showPicker = false },
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.duo_action_pick_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                RoundedCardContainer(spacing = 2.dp) {
                    RemapActionItem(
                        title = stringResource(R.string.duo_action_none),
                        isSelected = currentAction == null,
                        onClick = {
                            onActionSelected(null)
                            showPicker = false
                        },
                        iconRes = R.drawable.rounded_do_not_disturb_on_24,
                    )
                }

                val actionCategories = remember {
                    ActionRegistry.getCategories()
                }

                var expandedActionCategory by remember {
                    mutableStateOf<Int?>(
                        actionCategories.firstOrNull { category ->
                            category.actions.any { currentAction != null && it::class == currentAction::class }
                        }?.titleRes ?: actionCategories.firstOrNull()?.titleRes
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
                                hasSettings = resolvedAction.isConfigurable ||
                                    resolvedAction is Action.ToggleFlashlight ||
                                    resolvedAction is Action.LikeCurrentSong,
                                onClick = {
                                    onActionSelected(resolvedAction)
                                    showPicker = false
                                    if (missing.isNotEmpty()) {
                                        showMissingPermissionSheet()
                                    }
                                },
                                onSettingsClick = {
                                    showPicker = false
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showLikeSongOptions.value) {
        LikeSongSettingsSheet(
            onDismiss = { showLikeSongOptions.value = false },
            viewModel = viewModel,
            context = context,
        )
    }

    if (showFlashlightOptions) {
        EssentialsBottomSheet(
            onDismissRequest = { showFlashlightOptions = false },
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.flashlight_options_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                    modifier = Modifier
                        .fillMaxWidth(),
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
        DeviceEffectsSettingsSheet(
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
        SometimesEssentialsSettingsSheet(
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
        val availableTags = remember {
            SettingsRepository(context).getFreezeTags()
        }
        FreezeTagSettingsSheet(
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
                val finalAction = when (val action = configAction) {
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
                temporarySelectedAppsForAction = selections.filter { it.isEnabled }.map { it.packageName }
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
        val permissionItems = PermissionUIHelper.getPermissionItems(
            permissionKeysToShow,
            context,
            viewModel,
        )
        if (permissionItems.isNotEmpty()) {
            PermissionsBottomSheet(
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
