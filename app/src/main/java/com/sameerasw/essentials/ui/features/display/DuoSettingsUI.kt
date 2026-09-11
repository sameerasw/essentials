/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: DuoSettingsUI.kt
 * Description: UI settings composable for Duo ambient camera indicator feature.
 */

package com.sameerasw.essentials.ui.features.display

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
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
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DuoSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var requestingPermissionsFor by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }
    var showMediaAppSelectionSheet by remember { mutableStateOf(false) }

    var pickingActionForGesture by remember { mutableStateOf<String?>(null) }
    var activeConfigGesture by remember { mutableStateOf<String?>(null) }
    var showSlideModeSheet by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

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

    fun applyActionForGesture(gesture: String?, action: Action?) {
        when (gesture) {
            "tap" -> viewModel.setDuoTapAction(action)
            "double_tap" -> viewModel.setDuoDoubleTapAction(action)
            "long_press" -> viewModel.setDuoLongPressAction(action)
            "swipe_down" -> viewModel.setDuoSwipeDownAction(action)
        }
    }

    if (requestingPermissionsFor != null) {
        val (featureTitle, permKeys) = requestingPermissionsFor!!
        val permissionItems = PermissionUIHelper.getPermissionItems(permKeys, context, viewModel)
        PermissionsBottomSheet(
            onDismissRequest = {
                requestingPermissionsFor = null
                viewModel.check(context)
            },
            featureTitle = featureTitle,
            permissions = permissionItems,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_motion_play_24,
                title = stringResource(R.string.duo_enable_title),
                description = stringResource(R.string.duo_enable_desc),
                isChecked = viewModel.isDuoEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !viewModel.isAccessibilityEnabled.value) {
                        requestingPermissionsFor = Pair(R.string.duo_title, listOf("ACCESSIBILITY"))
                    } else {
                        viewModel.setDuoEnabled(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "duo_enabled"),
            )
        }

        Text(
            text = stringResource(R.string.duo_section_position),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_center_focus_strong_24,
                title = stringResource(R.string.duo_auto_detect_title),
                description = stringResource(R.string.duo_auto_detect_desc),
                isChecked = viewModel.isDuoAutoDetect.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setDuoAutoDetect(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "duo_use_auto_detect"),
            )

            AnimatedVisibility(
                visible = !viewModel.isDuoAutoDetect.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ConfigSliderItem(
                        title = stringResource(R.string.duo_camera_offset_x_title),
                        value = viewModel.duoCameraOffsetX.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setDuoCameraOffsetX(it)
                        },
                        valueRange = 0f..100f,
                        increment = 1f,
                        iconRes = R.drawable.rounded_border_left_24,
                        valueFormatter = { "${it.toInt()}%" },
                    )
                    ConfigSliderItem(
                        title = stringResource(R.string.duo_camera_offset_y_title),
                        value = viewModel.duoCameraOffsetY.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setDuoCameraOffsetY(it)
                        },
                        valueRange = 0f..20f,
                        increment = 0.5f,
                        iconRes = R.drawable.rounded_border_top_24,
                        valueFormatter = { "%.1f%%".format(it) },
                    )
                }
            }

            ConfigSliderItem(
                title = stringResource(R.string.duo_camera_size_title),
                value = viewModel.duoCameraSize.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setDuoCameraSize(it)
                },
                valueRange = 0.05f..2.0f,
                increment = 0.05f,
                iconRes = R.drawable.rounded_arrows_outward_24,
                valueFormatter = { "%.2fx".format(it) },
            )
        }

        Text(
            text = stringResource(R.string.duo_section_style),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            ConfigSliderItem(
                title = stringResource(R.string.duo_ring_radius_title),
                value = viewModel.duoRingRadius.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setDuoRingRadius(it)
                },
                valueRange = 0.1f..1.6f,
                increment = 0.05f,
                iconRes = R.drawable.rounded_circle_24,
                valueFormatter = { "%.2fx".format(it) },
            )
            ConfigSliderItem(
                title = stringResource(R.string.duo_arc_thickness_title),
                value = viewModel.duoArcThickness.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setDuoArcThickness(it)
                },
                valueRange = 0.5f..10f,
                increment = 0.5f,
                iconRes = R.drawable.rounded_line_weight_24,
                valueFormatter = { if (it % 1f == 0f) "${it.toInt()} dp" else "%.1f dp".format(it) },
            )
            ConfigSliderItem(
                title = stringResource(R.string.duo_dot_size_title),
                value = viewModel.duoDotSize.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setDuoDotSize(it)
                },
                valueRange = 0.5f..8f,
                increment = 0.5f,
                iconRes = R.drawable.rounded_circles_24,
                valueFormatter = { if (it % 1f == 0f) "${it.toInt()} dp" else "%.1f dp".format(it) },
            )
        }

        Text(
            text = stringResource(R.string.duo_section_what_to_show),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_battery_charging_60_24,
                title = stringResource(R.string.duo_show_battery_title),
                isChecked = viewModel.isDuoShowBattery.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setDuoShowBattery(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "duo_show_battery"),
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_signal_cellular_alt_24,
                title = stringResource(R.string.duo_show_networks_title),
                isChecked = viewModel.isDuoShowNetworks.value,
                enabled = viewModel.isDuoShowBattery.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setDuoShowNetworks(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "duo_show_networks"),
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_motion_play_24,
                title = stringResource(R.string.duo_show_media_title),
                isChecked = viewModel.isDuoShowMedia.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !viewModel.isNotificationListenerEnabled.value) {
                        requestingPermissionsFor = Pair(R.string.duo_title, listOf("NOTIFICATION_LISTENER"))
                    } else {
                        viewModel.setDuoShowMedia(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "duo_show_media"),
            )

            AnimatedVisibility(
                visible = viewModel.isDuoShowMedia.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_apps_24,
                    title = stringResource(R.string.feat_aod_wallpaper_media_apps),
                    showToggle = false,
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showMediaAppSelectionSheet = true
                    },
                )
            }
            IconToggleItem(
                iconRes = R.drawable.rounded_downloading_24,
                title = stringResource(R.string.duo_show_progress_title),
                isChecked = viewModel.isDuoShowProgress.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !viewModel.isNotificationListenerEnabled.value) {
                        requestingPermissionsFor = Pair(R.string.duo_title, listOf("NOTIFICATION_LISTENER"))
                    } else {
                        viewModel.setDuoShowProgress(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "duo_show_progress"),
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_flashlight_on_24,
                title = stringResource(R.string.duo_show_flashlight_title),
                isChecked = viewModel.isDuoShowFlashlight.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setDuoShowFlashlight(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "duo_show_flashlight"),
            )
        }

        Text(
            text = stringResource(R.string.duo_section_behavior),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_palette_24,
                title = stringResource(R.string.duo_material_you_title),
                isChecked = viewModel.isDuoUseMaterialYou.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setDuoUseMaterialYou(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "duo_use_material_you"),
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_off_24,
                title = stringResource(R.string.duo_hide_when_screen_off_title),
                isChecked = viewModel.isDuoHideWhenScreenOff.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setDuoHideWhenScreenOff(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "duo_hide_when_screen_off"),
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_nightlight_24,
                title = stringResource(R.string.duo_hide_when_screen_off_only_idle_title),
                isChecked = viewModel.isDuoHideWhenScreenOffOnlyIdle.value,
                enabled = viewModel.isDuoHideWhenScreenOff.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setDuoHideWhenScreenOffOnlyIdle(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "duo_hide_when_screen_off_only_idle"),
            )
        }

        Text(
            text = stringResource(R.string.duo_section_actions),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            val tapAction = viewModel.duoTapAction.value
            IconToggleItem(
                iconRes = R.drawable.rounded_pan_tool_alt_24,
                title = stringResource(R.string.duo_action_tap_title),
                description = tapAction?.let { stringResource(it.title) } ?: stringResource(R.string.duo_action_none),
                showToggle = false,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    pickingActionForGesture = "tap"
                },
                modifier = Modifier.highlight(highlightSetting == "duo_tap_action"),
            )

            val doubleTapAction = viewModel.duoDoubleTapAction.value
            IconToggleItem(
                iconRes = R.drawable.rounded_touch_app_24,
                title = stringResource(R.string.duo_action_double_tap_title),
                description = doubleTapAction?.let { stringResource(it.title) } ?: stringResource(R.string.duo_action_none),
                showToggle = false,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    pickingActionForGesture = "double_tap"
                },
                modifier = Modifier.highlight(highlightSetting == "duo_double_tap_action"),
            )

            val longPressAction = viewModel.duoLongPressAction.value
            IconToggleItem(
                iconRes = R.drawable.rounded_front_hand_24,
                title = stringResource(R.string.duo_action_long_press_title),
                description = longPressAction?.let { stringResource(it.title) } ?: stringResource(R.string.duo_action_none),
                showToggle = false,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    pickingActionForGesture = "long_press"
                },
                modifier = Modifier.highlight(highlightSetting == "duo_long_press_action"),
            )

            val swipeDownAction = viewModel.duoSwipeDownAction.value
            IconToggleItem(
                iconRes = R.drawable.rounded_south_24,
                title = stringResource(R.string.duo_action_swipe_down_title),
                description = swipeDownAction?.let { stringResource(it.title) } ?: stringResource(R.string.duo_action_none),
                showToggle = false,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    pickingActionForGesture = "swipe_down"
                },
                modifier = Modifier.highlight(highlightSetting == "duo_swipe_down_action"),
            )

            val slideModeDescription = when (viewModel.duoSlideMode.value) {
                "volume" -> stringResource(R.string.duo_action_horizontal_slide_volume)
                "brightness" -> stringResource(R.string.duo_action_horizontal_slide_brightness)
                "track" -> stringResource(R.string.duo_action_horizontal_slide_track)
                "sound_mode" -> stringResource(R.string.duo_action_horizontal_slide_sound_mode)
                else -> stringResource(R.string.duo_action_horizontal_slide_none)
            }
            IconToggleItem(
                iconRes = R.drawable.rounded_compare_arrows_24,
                title = stringResource(R.string.duo_action_horizontal_slide_title),
                description = slideModeDescription,
                showToggle = false,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    showSlideModeSheet = true
                },
                modifier = Modifier.highlight(highlightSetting == "duo_slide_mode"),
            )

            val isMirrorableMode = viewModel.duoSlideMode.value == "track" || viewModel.duoSlideMode.value == "sound_mode"
            AnimatedVisibility(
                visible = isMirrorableMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_compare_arrows_24,
                    title = stringResource(R.string.duo_slide_mirror_direction_title),
                    isChecked = viewModel.isDuoSlideInvertDirection.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setDuoSlideInvertDirection(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "duo_slide_invert_direction"),
                )
            }
        }
    }

    if (showSlideModeSheet) {
        EssentialsBottomSheet(
            onDismissRequest = { showSlideModeSheet = false },
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.duo_action_horizontal_slide_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                RoundedCardContainer(spacing = 2.dp) {
                    val modes = listOf(
                        Triple("none", R.string.duo_action_horizontal_slide_none, R.drawable.rounded_do_not_disturb_on_24),
                        Triple("volume", R.string.duo_action_horizontal_slide_volume, R.drawable.rounded_volume_up_24),
                        Triple("brightness", R.string.duo_action_horizontal_slide_brightness, R.drawable.rounded_brightness_6_24),
                        Triple("track", R.string.duo_action_horizontal_slide_track, R.drawable.rounded_skip_next_24),
                        Triple("sound_mode", R.string.duo_action_horizontal_slide_sound_mode, R.drawable.rounded_mobile_sound_24),
                    )

                    modes.forEach { (modeKey, titleRes, iconRes) ->
                        RemapActionItem(
                            title = stringResource(titleRes),
                            iconRes = iconRes,
                            isSelected = viewModel.duoSlideMode.value == modeKey,
                            onClick = {
                                viewModel.setDuoSlideMode(modeKey)
                                showSlideModeSheet = false
                            },
                        )
                    }
                }
            }
        }
    }

    if (pickingActionForGesture != null) {
        val currentGesture = pickingActionForGesture
        val currentAction = when (currentGesture) {
            "tap" -> viewModel.duoTapAction.value
            "double_tap" -> viewModel.duoDoubleTapAction.value
            "long_press" -> viewModel.duoLongPressAction.value
            "swipe_down" -> viewModel.duoSwipeDownAction.value
            else -> null
        }

        EssentialsBottomSheet(
            onDismissRequest = { pickingActionForGesture = null },
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
                            applyActionForGesture(currentGesture, null)
                            pickingActionForGesture = null
                        },
                        iconRes = R.drawable.rounded_do_not_disturb_on_24,
                    )
                }

                val actionCategories = remember {
                    ActionRegistry.getCategories(screenOnOnly = false)
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
                                    applyActionForGesture(currentGesture, resolvedAction)
                                    pickingActionForGesture = null
                                    if (missing.isNotEmpty()) {
                                        showMissingPermissionSheet()
                                    }
                                },
                                onSettingsClick = {
                                    activeConfigGesture = currentGesture
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
                applyActionForGesture(activeConfigGesture, newAction)
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
                applyActionForGesture(activeConfigGesture, newAction)
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
                applyActionForGesture(activeConfigGesture, newAction)
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
                applyActionForGesture(activeConfigGesture, newAction)
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
                applyActionForGesture(activeConfigGesture, newAction)
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
                applyActionForGesture(activeConfigGesture, newAction)
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
                applyActionForGesture(activeConfigGesture, newAction)
                configAction = null
            },
        )
    }

    if (showOpenAppSettings) {
        SingleAppSelectionSheet(
            onDismissRequest = { showOpenAppSettings = false },
            onAppSelected = { app ->
                val newAction = Action.OpenApp(packageName = app.packageName)
                applyActionForGesture(activeConfigGesture, newAction)
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
                    applyActionForGesture(activeConfigGesture, finalAction)
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
                applyActionForGesture(activeConfigGesture, Action.Keyboard(newIme))
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
                applyActionForGesture(activeConfigGesture, newAction)
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

    if (showMediaAppSelectionSheet) {
        AppSelectionSheet(
            onDismissRequest = { showMediaAppSelectionSheet = false },
            onLoadApps = { viewModel.loadAodWallpaperMediaApps(it) },
            onSaveApps = { ctx, apps ->
                viewModel.saveAodWallpaperMediaApps(
                    ctx,
                    apps,
                )
            },
            onAppToggle = { ctx, pkg, enabled ->
                viewModel.updateAodWallpaperMediaAppEnabled(
                    ctx,
                    pkg,
                    enabled,
                )
            },
            context = context,
        )
    }
}

