/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: StatusGlanceSettingsUI.kt
 * Description: UI settings composable for Status Glance ambient indicator feature.
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
import androidx.compose.material3.ModalBottomSheet
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
import com.sameerasw.essentials.domain.model.AppPermission
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
import com.sameerasw.essentials.ui.features.display.sheets.StatusGlanceBatteryOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.StatusGlanceCalendarOptionsBottomSheet
import com.sameerasw.essentials.ui.features.system.LikeSongSettingsSheet
import com.sameerasw.essentials.ui.features.system.RemapActionItem
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusGlanceSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var requestingPermissionsFor by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }
    var showMediaAppSelectionSheet by remember { mutableStateOf(false) }
    var showBatteryOptionsSheet by remember { mutableStateOf(false) }
    var showCalendarOptionsSheet by remember { mutableStateOf(false) }

    var pickingActionForGesture by remember { mutableStateOf<String?>(null) }
    var activeConfigGesture by remember { mutableStateOf<String?>(null) }

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
            "tap" -> viewModel.setStatusGlanceTapAction(action)
            "double_tap" -> viewModel.setStatusGlanceDoubleTapAction(action)
            "long_press" -> viewModel.setStatusGlanceLongPressAction(action)
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
                title = stringResource(R.string.status_glance_enable_title),
                description = stringResource(R.string.status_glance_enable_desc),
                iconRes = R.drawable.rounded_motion_play_24,
                isChecked = viewModel.isStatusGlanceEnabled.value,
                onCheckedChange = { isChecked ->
                    HapticUtil.performUIHaptic(view)
                    if (isChecked && !viewModel.isAccessibilityEnabled.value) {
                        requestingPermissionsFor = Pair(
                            R.string.feat_status_glance_title,
                            listOf(AppPermission.ACCESSIBILITY.key)
                        )
                    } else {
                        viewModel.setStatusGlanceEnabled(isChecked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "status_glance_enabled"),
            )
        }

        AnimatedVisibility(
            visible = viewModel.isStatusGlanceEnabled.value,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.status_glance_section_position),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )

                RoundedCardContainer(
                    spacing = 2.dp,
                    cornerRadius = 24.dp,
                ) {
                    IconToggleItem(
                        title = stringResource(R.string.status_glance_auto_detect_button_title),
                        description = stringResource(R.string.status_glance_auto_detect_button_desc),
                        iconRes = R.drawable.rounded_center_focus_strong_24,
                        showToggle = false,
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.autoDetectStatusGlancePosition(context)
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_auto_detect"),
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.status_glance_offset_x_title),
                        value = viewModel.statusGlanceOffsetX.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceOffsetX(it)
                        },
                        valueRange = 0f..100f,
                        increment = 1f,
                        iconRes = R.drawable.rounded_border_left_24,
                        valueFormatter = { "${it.toInt()}%" },
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.status_glance_offset_y_title),
                        value = viewModel.statusGlanceOffsetY.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceOffsetY(it)
                        },
                        valueRange = 0f..20f,
                        increment = 0.5f,
                        iconRes = R.drawable.rounded_border_top_24,
                        valueFormatter = { "%.1f%%".format(it) },
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.status_glance_max_width_title),
                        value = viewModel.statusGlanceMaxWidth.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceMaxWidth(it)
                        },
                        valueRange = 80f..350f,
                        increment = 10f,
                        iconRes = R.drawable.rounded_arrows_outward_24,
                        valueFormatter = { "${it.toInt()} dp" },
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.status_glance_font_size_title),
                        value = viewModel.statusGlanceFontSize.floatValue,
                        onValueChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceFontSize(it)
                        },
                        valueRange = 9f..22f,
                        increment = 1f,
                        iconRes = R.drawable.rounded_mobile_text_24,
                        valueFormatter = { "${it.toInt()} sp" },
                    )
                }

                Text(
                    text = stringResource(R.string.status_glance_section_what_to_show),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )

                RoundedCardContainer(
                    spacing = 2.dp,
                    cornerRadius = 24.dp,
                ) {
                    IconToggleItem(
                        title = stringResource(R.string.status_glance_show_flashlight_title),
                        iconRes = R.drawable.rounded_flashlight_on_24,
                        isChecked = viewModel.isStatusGlanceShowFlashlight.value,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceShowFlashlight(it)
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_show_flashlight"),
                    )

                    IconToggleItem(
                        title = stringResource(R.string.status_glance_show_calendar_title),
                        iconRes = R.drawable.rounded_calendar_today_24,
                        isChecked = viewModel.isStatusGlanceShowCalendar.value,
                        onCheckedChange = { isChecked ->
                            HapticUtil.performUIHaptic(view)
                            if (isChecked && !viewModel.isCalendarPermissionGranted.value) {
                                requestingPermissionsFor = Pair(
                                    R.string.status_glance_show_calendar_title,
                                    listOf(AppPermission.READ_CALENDAR.key)
                                )
                            }
                            viewModel.setStatusGlanceShowCalendar(isChecked)
                        },
                        onSettingsClick = {
                            showCalendarOptionsSheet = true
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_show_calendar"),
                    )

                    IconToggleItem(
                        title = stringResource(R.string.status_glance_show_media_title),
                        iconRes = R.drawable.rounded_motion_play_24,
                        isChecked = viewModel.isStatusGlanceShowMedia.value,
                        onCheckedChange = { isChecked ->
                            HapticUtil.performUIHaptic(view)
                            if (isChecked && !viewModel.isNotificationListenerEnabled.value) {
                                requestingPermissionsFor = Pair(
                                    R.string.feat_status_glance_title,
                                    listOf(AppPermission.NOTIFICATION_LISTENER.key)
                                )
                            }
                            viewModel.setStatusGlanceShowMedia(isChecked)
                        },
                        onSettingsClick = {
                            showMediaAppSelectionSheet = true
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_show_media"),
                    )

                    IconToggleItem(
                        title = stringResource(R.string.status_glance_show_time_title),
                        iconRes = R.drawable.rounded_schedule_24,
                        isChecked = viewModel.isStatusGlanceShowTime.value,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceShowTime(it)
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_show_time"),
                    )

                    IconToggleItem(
                        title = stringResource(R.string.status_glance_show_battery_title),
                        iconRes = R.drawable.battery_android_frame_full_24px,
                        isChecked = viewModel.isStatusGlanceShowBattery.value,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceShowBattery(it)
                        },
                        onSettingsClick = {
                            showBatteryOptionsSheet = true
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_show_battery"),
                    )
                }

                Text(
                    text = stringResource(R.string.status_glance_section_gestures),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )

                RoundedCardContainer(
                    spacing = 2.dp,
                    cornerRadius = 24.dp,
                ) {
                    val tapAction = viewModel.statusGlanceTapAction.value
                    val tapDescription = if (tapAction != null) {
                        "${stringResource(R.string.status_glance_action_tap_media_desc)} • ${stringResource(tapAction.title)}"
                    } else {
                        stringResource(R.string.status_glance_action_tap_media_desc)
                    }
                    IconToggleItem(
                        iconRes = R.drawable.rounded_touch_app_24,
                        title = stringResource(R.string.status_glance_action_tap_title),
                        description = tapDescription,
                        showToggle = false,
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            pickingActionForGesture = "tap"
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_tap_action"),
                    )

                    val doubleTapAction = viewModel.statusGlanceDoubleTapAction.value
                    IconToggleItem(
                        iconRes = R.drawable.rounded_touch_app_24,
                        title = stringResource(R.string.status_glance_action_double_tap_title),
                        description = doubleTapAction?.let { stringResource(it.title) } ?: stringResource(R.string.status_glance_action_none),
                        showToggle = false,
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            pickingActionForGesture = "double_tap"
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_double_tap_action"),
                    )

                    val longPressAction = viewModel.statusGlanceLongPressAction.value
                    IconToggleItem(
                        iconRes = R.drawable.rounded_front_hand_24,
                        title = stringResource(R.string.status_glance_action_long_press_title),
                        description = longPressAction?.let { stringResource(it.title) } ?: stringResource(R.string.status_glance_action_none),
                        showToggle = false,
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            pickingActionForGesture = "long_press"
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_long_press_action"),
                    )

                    IconToggleItem(
                        iconRes = R.drawable.rounded_skip_next_24,
                        title = stringResource(R.string.status_glance_action_swipe_right_title),
                        description = stringResource(R.string.status_glance_action_swipe_right_desc),
                        showToggle = false,
                    )
                }

                Text(
                    text = stringResource(R.string.status_glance_section_appearance),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )

                RoundedCardContainer(
                    spacing = 2.dp,
                    cornerRadius = 24.dp,
                ) {
                    IconToggleItem(
                        title = stringResource(R.string.status_glance_background_pill_title),
                        description = stringResource(R.string.status_glance_background_pill_desc),
                        iconRes = R.drawable.rounded_circle_24,
                        isChecked = viewModel.isStatusGlanceBackgroundPill.value,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceBackgroundPill(it)
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_background_pill"),
                    )

                    IconToggleItem(
                        title = stringResource(R.string.status_glance_hide_in_quick_settings_title),
                        description = stringResource(R.string.status_glance_hide_in_quick_settings_desc),
                        iconRes = R.drawable.rounded_top_panel_close_24,
                        isChecked = viewModel.isStatusGlanceHideInQuickSettings.value,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setStatusGlanceHideInQuickSettings(it)
                        },
                        modifier = Modifier.highlight(highlightSetting == "status_glance_hide_in_quick_settings"),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showMediaAppSelectionSheet) {
        AppSelectionSheet(
            title = stringResource(R.string.duo_media_skip_apps_title),
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

    if (showBatteryOptionsSheet) {
        StatusGlanceBatteryOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showBatteryOptionsSheet = false },
        )
    }

    if (showCalendarOptionsSheet) {
        StatusGlanceCalendarOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showCalendarOptionsSheet = false },
        )
    }

    if (pickingActionForGesture != null) {
        val currentGesture = pickingActionForGesture
        val currentAction = when (currentGesture) {
            "tap" -> viewModel.statusGlanceTapAction.value
            "double_tap" -> viewModel.statusGlanceDoubleTapAction.value
            "long_press" -> viewModel.statusGlanceLongPressAction.value
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
                    text = stringResource(R.string.status_glance_action_pick_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                RoundedCardContainer(spacing = 2.dp) {
                    RemapActionItem(
                        title = stringResource(R.string.status_glance_action_none),
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
        ModalBottomSheet(
            onDismissRequest = { showFlashlightOptions = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
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
                    modifier = Modifier.fillMaxWidth(),
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
}
