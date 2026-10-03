/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandSettingsUI.kt
 * Description: UI settings composable for Island dynamic notifications feature.
 */

package com.sameerasw.essentials.ui.features.display

import android.content.Intent
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.features.consciousgate.CONSCIOUS_GATE_FEATURE_ID
import com.sameerasw.essentials.ui.features.display.actions.GestureActionPickerSheet
import com.sameerasw.essentials.ui.features.display.actions.HorizontalSlideModeSheet
import com.sameerasw.essentials.ui.features.display.actions.horizontalSlideDescription
import com.sameerasw.essentials.ui.features.display.sheets.IslandAlarmOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandBriefOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandCallOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandDevicesBatteryBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandNotificationOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandSoundModeOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandTimeBatteryOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandTimerOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandWeatherOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.StatusGlanceCalendarOptionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.weather.overcast.OvercastWeather
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

private val notificationSheetSettings =
    setOf(
        "island_notif_compact_heads_up",
        "island_notif_compact_only",
        "island_notif_keep_progress",
        "island_notif_queue",
        "island_notif_tap_to_open",
        "island_catch_up",
        "island_catch_up_timeout",
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    IslandSettingsUI(
        viewModel = viewModel,
        isIslandEnabled = viewModel.isIslandEnabled.value,
        isAccessibilityEnabled = viewModel.isAccessibilityEnabled.value,
        isNotificationListenerEnabled = viewModel.isNotificationListenerEnabled.value,
        isIslandShowCalls = viewModel.isIslandShowCalls.value,
        isIslandShowNotifications = viewModel.isIslandShowNotifications.value,
        isIslandShowTimeBattery = viewModel.isIslandShowTimeBattery.value,
        isIslandShowFlashlight = viewModel.isIslandShowFlashlight.value,
        isIslandShowTimers = viewModel.isIslandShowTimers.value,
        isIslandShowWeather = viewModel.isIslandShowWeather.value,
        isOvercastWeatherPermissionGranted = viewModel.isOvercastWeatherPermissionGranted.value,
        isIslandShowMedia = viewModel.isIslandShowMedia.value,
        isIslandShowConsciousGate = viewModel.isIslandShowConsciousGate.value,
        isIslandShowCaffeinate = viewModel.isIslandShowCaffeinate.value,
        isIslandShowTravel = viewModel.isIslandShowTravel.value,
        isIslandShowCalendar = viewModel.isIslandShowCalendar.value,
        isCalendarPermissionGranted = viewModel.isCalendarPermissionGranted.value,
        isIslandShowSoundMode = viewModel.isIslandShowSoundMode.value,
        isIslandShowAlarm = viewModel.isIslandShowAlarm.value,
        isIslandShowNetwork = viewModel.isIslandShowNetwork.value,
        isIslandShowDevices = viewModel.isIslandShowDevices.value,
        isIslandBriefEnabled = viewModel.isIslandBriefEnabled.value,
        islandLongPressAction = viewModel.islandLongPressAction.value,
        isIslandLikeWhilePlaying = viewModel.isIslandLikeWhilePlaying.value,
        islandSlideMode = viewModel.islandSlideMode.value,
        isIslandSlideTrack = viewModel.isIslandSlideTrack.value,
        isIslandSlideInvertDirection = viewModel.isIslandSlideInvertDirection.value,
        isDuoEnabled = viewModel.isDuoEnabled.value,
        isDuoIslandCombined = viewModel.isDuoIslandCombined.value,
        isIslandLineStageEnabled = viewModel.isIslandLineStageEnabled.value,
        isIslandMediaPeekSongChange = viewModel.isIslandMediaPeekSongChange.value,
        isIslandMediaKeepWhenPaused = viewModel.isIslandMediaKeepWhenPaused.value,
        isIslandMediaShowPrevious = viewModel.isIslandMediaShowPrevious.value,
        modifier = modifier,
        highlightSetting = highlightSetting,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IslandSettingsUI(
    viewModel: MainViewModel?,
    isIslandEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    isNotificationListenerEnabled: Boolean,
    isIslandShowCalls: Boolean,
    isIslandShowNotifications: Boolean,
    isIslandShowTimeBattery: Boolean,
    isIslandShowFlashlight: Boolean,
    isIslandShowTimers: Boolean,
    isIslandShowWeather: Boolean,
    isOvercastWeatherPermissionGranted: Boolean,
    isIslandShowMedia: Boolean,
    isIslandShowConsciousGate: Boolean,
    isIslandShowCaffeinate: Boolean,
    isIslandShowTravel: Boolean,
    isIslandShowCalendar: Boolean,
    isCalendarPermissionGranted: Boolean,
    isIslandShowSoundMode: Boolean,
    isIslandShowAlarm: Boolean,
    isIslandShowNetwork: Boolean,
    isIslandShowDevices: Boolean,
    isIslandBriefEnabled: Boolean,
    islandLongPressAction: Action?,
    isIslandLikeWhilePlaying: Boolean,
    islandSlideMode: String,
    isIslandSlideTrack: Boolean,
    isIslandSlideInvertDirection: Boolean,
    isDuoEnabled: Boolean,
    isDuoIslandCombined: Boolean,
    isIslandLineStageEnabled: Boolean,
    isIslandMediaPeekSongChange: Boolean,
    isIslandMediaKeepWhenPaused: Boolean,
    isIslandMediaShowPrevious: Boolean,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var requestingPermissionsFor by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }

    LaunchedEffect(Unit) {
        if (isIslandShowWeather && !OvercastWeather.isAvailable(context)) {
            requestingPermissionsFor = Pair(R.string.lock_screen_clock_weather, listOf("OVERCAST_WEATHER"))
        }
    }
    var showMediaAppSelectionSheet by remember { mutableStateOf(false) }
    var showCalendarOptionsSheet by remember { mutableStateOf(false) }
    var showSoundModeOptionsSheet by remember { mutableStateOf(false) }
    var showDevicesBatterySheet by remember { mutableStateOf(false) }
    var showTimeBatteryOptionsSheet by remember { mutableStateOf(false) }
    var showTimerOptionsSheet by remember { mutableStateOf(false) }
    var showCallOptionsSheet by remember { mutableStateOf(false) }
    var showAlarmOptionsSheet by remember { mutableStateOf(false) }
    var showBriefOptionsSheet by remember { mutableStateOf(highlightSetting == "island_brief_show_alarm") }
    var showNotificationOptionsSheet by remember {
        mutableStateOf(highlightSetting in notificationSheetSettings)
    }
    var showWeatherOptionsSheet by remember { mutableStateOf(false) }
    var pickingGesture by remember { mutableStateOf<String?>(null) }
    var showSlideModeSheet by remember { mutableStateOf(false) }

    if (requestingPermissionsFor != null) {
        val (titleRes, permKeys) = requestingPermissionsFor!!
        val permissionItems = viewModel?.let { PermissionUIHelper.getPermissionItems(permKeys, context, it) } ?: emptyList()
        PermissionsBottomSheet(
            onDismissRequest = {
                requestingPermissionsFor = null
                viewModel?.check(context)
            },
            featureTitle = stringResource(titleRes),
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
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = stringResource(R.string.island_enable_title),
                isChecked = isIslandEnabled,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked) {
                        val missingPermissions = mutableListOf<String>()
                        if (!isAccessibilityEnabled) {
                            missingPermissions.add("ACCESSIBILITY")
                        }
                        if (!isNotificationListenerEnabled) {
                            missingPermissions.add("NOTIFICATION_LISTENER")
                        }

                        if (missingPermissions.isNotEmpty()) {
                            requestingPermissionsFor = Pair(R.string.island_title, missingPermissions)
                        } else {
                            viewModel?.setIslandEnabled(true)
                        }
                    } else {
                        viewModel?.setIslandEnabled(false)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "island_enabled"),
            )
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            ISLAND_SUB_PAGES.forEach { (featureId, titleRes, iconRes) ->
                FeatureCard(
                    title = titleRes,
                    iconRes = iconRes,
                    isEnabled = true,
                    showToggle = false,
                    onToggle = {},
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        context.startActivity(
                            Intent(context, FeatureSettingsActivity::class.java).apply {
                                putExtra("feature", featureId)
                            },
                        )
                    },
                )
            }
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
                iconRes = R.drawable.rounded_call_24,
                title = stringResource(R.string.island_show_calls_title),
                isChecked = isIslandShowCalls && PermissionUtils.hasCallPermissions(context),
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !PermissionUtils.hasCallPermissions(context)) {
                        requestingPermissionsFor = Pair(R.string.island_title, listOf("READ_PHONE_STATE", "ANSWER_PHONE_CALLS", "READ_CONTACTS"))
                    } else {
                        viewModel?.setIslandShowCalls(checked)
                    }
                },
                onSettingsClick = { showCallOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_calls"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = stringResource(R.string.island_section_notifications),
                isChecked = isIslandShowNotifications,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowNotifications(checked)
                },
                onSettingsClick = { showNotificationOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_notifications"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_schedule_24,
                title = stringResource(R.string.island_show_time_battery_title),
                isChecked = isIslandShowTimeBattery,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowTimeBattery(checked)
                },
                onSettingsClick = { showTimeBatteryOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_time_battery"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_flashlight_on_24,
                title = stringResource(R.string.feat_flashlight_title),
                isChecked = isIslandShowFlashlight,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowFlashlight(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_flashlight"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_timer_24,
                title = stringResource(R.string.island_show_timers_title),
                isChecked = isIslandShowTimers,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowTimers(checked)
                },
                onSettingsClick = { showTimerOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_timers"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_partly_cloudy_day_24,
                title = stringResource(R.string.lock_screen_clock_weather),
                description =
                    if (isIslandShowWeather && !isOvercastWeatherPermissionGranted) {
                        stringResource(R.string.weather_error_overcast_permission)
                    } else {
                        null
                    },
                isChecked = isIslandShowWeather,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowWeather(checked)
                    if (checked && !OvercastWeather.isAvailable(context)) {
                        requestingPermissionsFor = Pair(R.string.lock_screen_clock_weather, listOf("OVERCAST_WEATHER"))
                    }
                },
                onSettingsClick = { showWeatherOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_weather"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_motion_play_24,
                title = stringResource(R.string.duo_show_media_title),
                isChecked = isIslandShowMedia,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowMedia(checked)
                },
                onSettingsClick = { showMediaAppSelectionSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_media"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_pause_24,
                title = stringResource(R.string.feat_conscious_gate_title),
                isChecked = isIslandShowConsciousGate,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowConsciousGate(checked)
                },
                onSettingsClick = {
                    val intent = Intent(context, FeatureSettingsActivity::class.java).apply {
                        putExtra("feature", CONSCIOUS_GATE_FEATURE_ID)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_conscious_gate"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_coffee_24,
                title = stringResource(R.string.feat_caffeinate_title),
                isChecked = isIslandShowCaffeinate,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowCaffeinate(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_caffeinate"),
            )

            IconToggleItem(
                iconRes = R.drawable.round_navigation_24,
                title = stringResource(R.string.feat_location_reached_title),
                isChecked = isIslandShowTravel,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowTravel(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_travel"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_calendar_today_24,
                title = stringResource(R.string.status_glance_show_calendar_title),
                isChecked = isIslandShowCalendar,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !isCalendarPermissionGranted) {
                        requestingPermissionsFor = Pair(R.string.island_title, listOf("READ_CALENDAR"))
                    } else {
                        viewModel?.setIslandShowCalendar(checked)
                    }
                },
                onSettingsClick = { showCalendarOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_calendar"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_volume_up_24,
                title = stringResource(R.string.island_show_sound_mode_title),
                isChecked = isIslandShowSoundMode,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowSoundMode(checked)
                },
                onSettingsClick = { showSoundModeOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_sound_mode"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_alarm_24,
                title = stringResource(R.string.island_show_alarm_title),
                isChecked = isIslandShowAlarm,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowAlarm(checked)
                },
                onSettingsClick = { showAlarmOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_alarm"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_android_wifi_3_bar_24,
                title = stringResource(R.string.island_show_network_title),
                isChecked = isIslandShowNetwork,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandShowNetwork(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_network"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_bluetooth_24,
                title = stringResource(R.string.island_show_devices_title),
                isChecked = isIslandShowDevices && PermissionUtils.hasBluetoothPermission(context),
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !PermissionUtils.hasBluetoothPermission(context)) {
                        requestingPermissionsFor = Pair(R.string.island_title, listOf("BLUETOOTH_CONNECT", "BLUETOOTH_SCAN"))
                    } else {
                        viewModel?.setIslandShowDevices(checked)
                    }
                },
                onSettingsClick = { showDevicesBatterySheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_show_devices"),
            )
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_calendar_today_24,
                title = stringResource(R.string.island_brief_title),
                description = stringResource(R.string.island_brief_desc),
                isChecked = isIslandBriefEnabled,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandBriefEnabled(checked)
                },
                onSettingsClick = { showBriefOptionsSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_brief_enabled"),
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
            IconToggleItem(
                iconRes = R.drawable.rounded_front_hand_24,
                title = stringResource(R.string.duo_action_long_press_title),
                description = if (isIslandBriefEnabled) {
                    stringResource(R.string.island_brief_title)
                } else {
                    islandLongPressAction?.let { stringResource(it.title) }
                        ?: stringResource(R.string.duo_action_none)
                },
                showToggle = false,
                enabled = !isIslandBriefEnabled,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    pickingGesture = "long_press"
                },
                modifier = Modifier.highlight(highlightSetting == "island_long_press_action"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_favorite_24,
                title = stringResource(R.string.island_like_while_playing_title),
                isChecked = isIslandLikeWhilePlaying,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandLikeWhilePlayingEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_like_while_playing"),
            )
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_compare_arrows_24,
                title = stringResource(R.string.duo_action_horizontal_slide_title),
                description = horizontalSlideDescription(islandSlideMode, isIslandSlideTrack),
                showToggle = false,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    showSlideModeSheet = true
                },
                modifier = Modifier.highlight(highlightSetting == "island_slide_mode"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_skip_next_24,
                title = stringResource(R.string.duo_action_horizontal_slide_track),
                description = stringResource(R.string.duo_action_horizontal_slide_track_desc),
                isChecked = isIslandSlideTrack,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel?.setIslandSlideTrackEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_slide_track"),
            )

            AnimatedVisibility(
                visible = isIslandSlideTrack || islandSlideMode == "sound_mode",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_compare_arrows_24,
                    title = stringResource(R.string.duo_slide_mirror_direction_title),
                    isChecked = isIslandSlideInvertDirection,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel?.setIslandSlideInvertDirection(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_slide_invert_direction"),
                )
            }
        }

        AnimatedVisibility(
            visible = !isIslandShowTimeBattery,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                text = stringResource(R.string.island_gesture_visuals_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        AnimatedVisibility(
            visible = isDuoEnabled && isIslandEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_link_24,
                    title = stringResource(R.string.island_combine_with_duo),
                    isChecked = isDuoIslandCombined,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel?.setDuoIslandCombined(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "duo_island_combined"),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (pickingGesture != null && viewModel != null) {
        GestureActionPickerSheet(
            viewModel = viewModel,
            currentAction = islandLongPressAction,
            onActionSelected = viewModel::setIslandLongPressAction,
            onPickerClosed = { pickingGesture = null },
        )
    }

    if (showSlideModeSheet) {
        HorizontalSlideModeSheet(
            mode = islandSlideMode,
            onModeSelected = { viewModel?.setIslandSlideMode(it) },
            onDismissRequest = { showSlideModeSheet = false },
        )
    }

    if (showMediaAppSelectionSheet && viewModel != null) {
        AppSelectionSheet(
            title = stringResource(R.string.island_media_config_title),
            onDismissRequest = { showMediaAppSelectionSheet = false },
            onLoadApps = { viewModel.loadIslandMediaApps(it) },
            onSaveApps = { ctx, apps -> viewModel.saveIslandMediaApps(ctx, apps) },
            onAppToggle = { ctx, pkg, enabled -> viewModel.updateIslandMediaAppEnabled(ctx, pkg, enabled) },
            context = context,
            headerContent = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                        if (isIslandLineStageEnabled) {
                            IconToggleItem(
                                iconRes = R.drawable.rounded_music_note_24,
                                title = stringResource(R.string.island_media_peek_song_change_title),
                                isChecked = isIslandMediaPeekSongChange,
                                onCheckedChange = { checked ->
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.setIslandMediaPeekSongChange(checked)
                                },
                                modifier = Modifier.highlight(highlightSetting == "island_media_peek_song_change"),
                            )
                        }
                        IconToggleItem(
                            iconRes = R.drawable.rounded_pause_24,
                            title = stringResource(R.string.island_media_keep_when_paused_title),
                            isChecked = isIslandMediaKeepWhenPaused,
                            onCheckedChange = { checked ->
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.setIslandMediaKeepWhenPaused(checked)
                            },
                            modifier = Modifier.highlight(highlightSetting == "island_media_keep_when_paused"),
                        )
                        IconToggleItem(
                            iconRes = R.drawable.rounded_skip_previous_24,
                            title = stringResource(R.string.island_media_show_previous_title),
                            isChecked = isIslandMediaShowPrevious,
                            onCheckedChange = { checked ->
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.setIslandMediaShowPrevious(checked)
                            },
                            modifier = Modifier.highlight(highlightSetting == "island_media_show_previous"),
                        )
                    }
                    Text(
                        text = stringResource(R.string.duo_media_skip_apps_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            },
        )
    }

    if (showWeatherOptionsSheet && viewModel != null) {
        IslandWeatherOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showWeatherOptionsSheet = false },
        )
    }

    if (showNotificationOptionsSheet && viewModel != null) {
        IslandNotificationOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showNotificationOptionsSheet = false },
            highlightSetting = highlightSetting,
        )
    }

    if (showBriefOptionsSheet && viewModel != null) {
        IslandBriefOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showBriefOptionsSheet = false },
            highlightSetting = highlightSetting,
        )
    }

    if (showSoundModeOptionsSheet && viewModel != null) {
        IslandSoundModeOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showSoundModeOptionsSheet = false },
        )
    }

    if (showAlarmOptionsSheet && viewModel != null) {
        IslandAlarmOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showAlarmOptionsSheet = false },
        )
    }

    if (showCallOptionsSheet) {
        IslandCallOptionsBottomSheet(onDismissRequest = { showCallOptionsSheet = false })
    }

    if (showTimerOptionsSheet && viewModel != null) {
        IslandTimerOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showTimerOptionsSheet = false },
        )
    }

    if (showTimeBatteryOptionsSheet && viewModel != null) {
        IslandTimeBatteryOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showTimeBatteryOptionsSheet = false },
        )
    }

    if (showDevicesBatterySheet && viewModel != null) {
        IslandDevicesBatteryBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showDevicesBatterySheet = false },
        )
    }

    if (showCalendarOptionsSheet && viewModel != null) {
        StatusGlanceCalendarOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showCalendarOptionsSheet = false },
            allowIconEdit = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IslandSettingsUIPreview() {
    EssentialsTheme {
        IslandSettingsUI(
            viewModel = null,
            isIslandEnabled = true,
            isAccessibilityEnabled = true,
            isNotificationListenerEnabled = true,
            isIslandShowCalls = true,
            isIslandShowNotifications = true,
            isIslandShowTimeBattery = true,
            isIslandShowFlashlight = true,
            isIslandShowTimers = true,
            isIslandShowWeather = false,
            isOvercastWeatherPermissionGranted = false,
            isIslandShowMedia = true,
            isIslandShowConsciousGate = false,
            isIslandShowCaffeinate = false,
            isIslandShowTravel = false,
            isIslandShowCalendar = false,
            isCalendarPermissionGranted = false,
            isIslandShowSoundMode = false,
            isIslandShowAlarm = false,
            isIslandShowNetwork = false,
            isIslandShowDevices = false,
            isIslandBriefEnabled = false,
            islandLongPressAction = null,
            isIslandLikeWhilePlaying = false,
            islandSlideMode = "none",
            isIslandSlideTrack = false,
            isIslandSlideInvertDirection = false,
            isDuoEnabled = false,
            isDuoIslandCombined = false,
            isIslandLineStageEnabled = true,
            isIslandMediaPeekSongChange = false,
            isIslandMediaKeepWhenPaused = false,
            isIslandMediaShowPrevious = false,
        )
    }
}
