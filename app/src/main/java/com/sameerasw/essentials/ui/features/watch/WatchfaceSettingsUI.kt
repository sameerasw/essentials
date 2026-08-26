/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: WearOS Companion
 * File: WatchfaceSettingsUI.kt
 * Description: Composable settings screen for configuring Essentials Watchface preferences.
 */

package com.sameerasw.essentials.ui.features.watch

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.DeviceInfoSyncManager
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import java.util.Calendar

@Composable
fun WatchfaceSettingsUI(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
    }

    var hideBattery by remember {
        mutableStateOf(prefs.getBoolean("watchface_hide_battery", false))
    }
    var hideDeviceIcons by remember {
        mutableStateOf(prefs.getBoolean("watchface_hide_device_icons", false))
    }
    var showComplications by remember {
        mutableStateOf(prefs.getBoolean("watchface_show_complications", true))
    }
    var complicationsOnAod by remember {
        mutableStateOf(prefs.getBoolean("watchface_complications_on_aod", true))
    }
    var complicationOutline by remember {
        mutableStateOf(prefs.getBoolean("watchface_complication_outline", true))
    }
    var leftComplication by remember {
        mutableStateOf(prefs.getString("watchface_left_complication", "HEART_RATE") ?: "HEART_RATE")
    }
    var rightComplication by remember {
        mutableStateOf(prefs.getString("watchface_right_complication", "STEPS") ?: "STEPS")
    }
    var showUpcomingEvents by remember {
        mutableStateOf(prefs.getBoolean("watchface_show_glance", prefs.getBoolean("watchface_show_upcoming_events", true)))
    }
    var glanceBatteryAlerts by remember {
        mutableStateOf(prefs.getBoolean("watchface_glance_battery_alerts", true))
    }
    var glanceFlashlight by remember {
        mutableStateOf(prefs.getBoolean("watchface_glance_flashlight", true))
    }
    var glanceTravel by remember {
        mutableStateOf(prefs.getBoolean("watchface_glance_travel", true))
    }
    var glanceEvents by remember {
        mutableStateOf(prefs.getBoolean("watchface_glance_events", true))
    }
    var glanceAlarm by remember {
        mutableStateOf(prefs.getBoolean("watchface_glance_alarm", true))
    }
    var showGlow by remember {
        mutableStateOf(prefs.getBoolean("watchface_show_glow", true))
    }
    var showGlanceComplicationsSheet by remember {
        mutableStateOf(false)
    }

    var clockFont by remember {
        mutableStateOf(prefs.getString("watchface_clock_font", "FLEX") ?: "FLEX")
    }
    var clockFontSize by remember {
        mutableIntStateOf(prefs.getInt("watchface_clock_font_size", 100))
    }
    var clockDualTone by remember {
        mutableStateOf(prefs.getBoolean("watchface_clock_dual_tone", false))
    }
    var clockOutline by remember {
        mutableStateOf(prefs.getBoolean("watchface_clock_outline", false))
    }
    var clockOutlineThickness by remember {
        mutableIntStateOf(prefs.getInt("watchface_clock_outline_thickness", 2))
    }

    val clockFontOptions = listOf("FLEX", "GROWTH", "INFLATE", "ROTATION")
    val calendar = remember { Calendar.getInstance() }
    val is24Hour = DateFormat.is24HourFormat(context)
    val rawHour = calendar.get(Calendar.HOUR)
    val hour = if (is24Hour) calendar.get(Calendar.HOUR_OF_DAY) else if (rawHour == 0) 12 else rawHour
    val minute = calendar.get(Calendar.MINUTE)
    val timePreview = String.format("%02d:%02d", hour, minute)

    val fontFamilyMap = remember {
        mapOf(
            "FLEX" to FontFamily(Font(R.font.google_sans_flex)),
            "GROWTH" to FontFamily(Font(R.font.growth)),
            "INFLATE" to FontFamily(Font(R.font.inflate_vf)),
            "ROTATION" to FontFamily(Font(R.font.rotation_gx)),
        )
    }

    val complicationOptions = listOf(
        "DYNAMIC",
        "HEART_RATE",
        "STEPS",
        "DISTANCE",
        "CALORIES",
        "NOTIFICATIONS",
        "NOW_PLAYING",
        "SOUND_MODE",
        "WATCH_BATTERY",
        "PHONE_BATTERY",
        "NONE"
    )
    val complicationLabels = mapOf(
        "DYNAMIC" to stringResource(R.string.watchface_comp_dynamic),
        "HEART_RATE" to stringResource(R.string.watchface_comp_heart_rate),
        "STEPS" to stringResource(R.string.watchface_comp_steps),
        "DISTANCE" to stringResource(R.string.watchface_comp_distance),
        "CALORIES" to stringResource(R.string.watchface_comp_calories),
        "NOTIFICATIONS" to stringResource(R.string.watchface_comp_notifications),
        "NOW_PLAYING" to stringResource(R.string.watchface_comp_now_playing),
        "SOUND_MODE" to stringResource(R.string.watchface_comp_sound_mode),
        "WATCH_BATTERY" to stringResource(R.string.watchface_comp_watch_battery),
        "PHONE_BATTERY" to stringResource(R.string.watchface_comp_phone_battery),
        "NONE" to stringResource(R.string.watchface_comp_none),
    )

    if (showGlanceComplicationsSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showGlanceComplicationsSheet = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.watchface_glance_complications_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
                RoundedCardContainer(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                ) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_battery_alert_24,
                        title = stringResource(R.string.watchface_glance_comp_battery_alerts_title),
                        description = stringResource(R.string.watchface_glance_comp_battery_alerts_desc),
                        isChecked = glanceBatteryAlerts,
                        onCheckedChange = {
                            glanceBatteryAlerts = it
                            prefs.edit().putBoolean("watchface_glance_battery_alerts", it).apply()
                            DeviceInfoSyncManager.forceSync(context)
                        },
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_flashlight_on_24,
                        title = stringResource(R.string.watchface_glance_comp_flashlight_title),
                        description = stringResource(R.string.watchface_glance_comp_flashlight_desc),
                        isChecked = glanceFlashlight,
                        onCheckedChange = {
                            glanceFlashlight = it
                            prefs.edit().putBoolean("watchface_glance_flashlight", it).apply()
                            DeviceInfoSyncManager.forceSync(context)
                        },
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_distance_24,
                        title = stringResource(R.string.watchface_glance_comp_travel_title),
                        description = stringResource(R.string.watchface_glance_comp_travel_desc),
                        isChecked = glanceTravel,
                        onCheckedChange = {
                            glanceTravel = it
                            prefs.edit().putBoolean("watchface_glance_travel", it).apply()
                            DeviceInfoSyncManager.forceSync(context)
                        },
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_calendar_today_24,
                        title = stringResource(R.string.watchface_glance_comp_events_title),
                        description = stringResource(R.string.watchface_glance_comp_events_desc),
                        isChecked = glanceEvents,
                        onCheckedChange = {
                            glanceEvents = it
                            prefs.edit().putBoolean("watchface_glance_events", it).apply()
                            DeviceInfoSyncManager.forceSync(context)
                        },
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_alarm_24,
                        title = stringResource(R.string.watchface_glance_comp_alarm_title),
                        description = stringResource(R.string.watchface_glance_comp_alarm_desc),
                        isChecked = glanceAlarm,
                        onCheckedChange = {
                            glanceAlarm = it
                            prefs.edit().putBoolean("watchface_glance_alarm", it).apply()
                            DeviceInfoSyncManager.forceSync(context)
                        },
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Section 0: Clock
        Text(
            text = stringResource(R.string.watchface_category_clock),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
        ) {
            SegmentedPicker(
                items = clockFontOptions,
                selectedItem = clockFont,
                onItemSelected = { font ->
                    clockFont = font
                    prefs.edit().putString("watchface_clock_font", font).apply()
                    DeviceInfoSyncManager.forceSync(context)
                },
                labelProvider = { font ->
                    timePreview
                },
                iconProvider = null,
                textStyleProvider = { font ->
                    TextStyle(
                        fontFamily = fontFamilyMap[font],
                        fontWeight = FontWeight.W400,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            ConfigSliderItem(
                title = stringResource(R.string.watchface_clock_font_size),
                value = clockFontSize.toFloat(),
                onValueChange = {
                    clockFontSize = it.toInt()
                    prefs.edit().putInt("watchface_clock_font_size", it.toInt()).apply()
                },
                onValueChangeFinished = {
                    DeviceInfoSyncManager.forceSync(context)
                },
                valueRange = 70f..130f,
                increment = 5f,
                iconRes = R.drawable.rounded_draw_24,
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_palette_24,
                title = stringResource(R.string.watchface_clock_dual_tone_title),
                description = stringResource(R.string.watchface_clock_dual_tone_desc),
                isChecked = clockDualTone,
                onCheckedChange = {
                    clockDualTone = it
                    prefs.edit().putBoolean("watchface_clock_dual_tone", it).apply()
                    DeviceInfoSyncManager.forceSync(context)
                },
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_circles_24,
                title = stringResource(R.string.watchface_clock_outline_title),
                description = stringResource(R.string.watchface_clock_outline_desc),
                isChecked = clockOutline,
                onCheckedChange = {
                    clockOutline = it
                    prefs.edit().putBoolean("watchface_clock_outline", it).apply()
                    DeviceInfoSyncManager.forceSync(context)
                },
            )

            if (clockOutline) {
                ConfigSliderItem(
                    title = stringResource(R.string.watchface_clock_outline_thickness),
                    value = clockOutlineThickness.toFloat(),
                    onValueChange = {
                        clockOutlineThickness = it.toInt()
                        prefs.edit().putInt("watchface_clock_outline_thickness", it.toInt()).apply()
                    },
                    onValueChangeFinished = {
                        DeviceInfoSyncManager.forceSync(context)
                    },
                    valueRange = 1f..10f,
                    increment = 1f,
                    iconRes = R.drawable.rounded_line_weight_24,
                )
            }
        }

        // Section 1: Battery & Icons
        Text(
            text = stringResource(R.string.watchface_category_battery),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_battery_android_frame_6_24,
                title = stringResource(R.string.watchface_hide_battery_title),
                description = stringResource(R.string.watchface_hide_battery_desc),
                isChecked = hideBattery,
                onCheckedChange = {
                    hideBattery = it
                    prefs.edit().putBoolean("watchface_hide_battery", it).apply()
                    DeviceInfoSyncManager.forceSync(context)
                },
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_24,
                title = stringResource(R.string.watchface_hide_device_icons_title),
                description = stringResource(R.string.watchface_hide_device_icons_desc),
                isChecked = hideDeviceIcons,
                onCheckedChange = {
                    hideDeviceIcons = it
                    prefs.edit().putBoolean("watchface_hide_device_icons", it).apply()
                    DeviceInfoSyncManager.forceSync(context)
                },
            )
        }

        // Section 2: Complications
        Text(
            text = stringResource(R.string.watchface_category_complications),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_widgets_24,
                title = stringResource(R.string.watchface_show_complications_title),
                description = stringResource(R.string.watchface_show_complications_desc),
                isChecked = showComplications,
                onCheckedChange = {
                    showComplications = it
                    prefs.edit().putBoolean("watchface_show_complications", it).apply()
                    DeviceInfoSyncManager.forceSync(context)
                },
            )
            if (showComplications) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_dark_mode_24,
                    title = stringResource(R.string.watchface_complications_on_aod_title),
                    description = stringResource(R.string.watchface_complications_on_aod_desc),
                    isChecked = complicationsOnAod,
                    onCheckedChange = {
                        complicationsOnAod = it
                        prefs.edit().putBoolean("watchface_complications_on_aod", it).apply()
                        DeviceInfoSyncManager.forceSync(context)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_circles_24,
                    title = stringResource(R.string.watchface_complication_outline_title),
                    description = stringResource(R.string.watchface_complication_outline_desc),
                    isChecked = complicationOutline,
                    onCheckedChange = {
                        complicationOutline = it
                        prefs.edit().putBoolean("watchface_complication_outline", it).apply()
                        DeviceInfoSyncManager.forceSync(context)
                    },
                )
                ConfigPickerItem(
                    title = stringResource(R.string.watchface_left_complication_title),
                    selectedValue = complicationLabels[leftComplication] ?: leftComplication,
                    iconRes = when (leftComplication) {
                        "DYNAMIC" -> R.drawable.rounded_auto_awesome_24
                        "HEART_RATE" -> R.drawable.rounded_favorite_24
                        "STEPS" -> R.drawable.rounded_steps_24
                        "DISTANCE" -> R.drawable.rounded_distance_24
                        "CALORIES" -> R.drawable.rounded_local_fire_department_24
                        "NOTIFICATIONS" -> R.drawable.rounded_notifications_unread_24
                        "NOW_PLAYING" -> R.drawable.rounded_music_note_24
                        "SOUND_MODE" -> R.drawable.rounded_volume_up_24
                        "WATCH_BATTERY" -> R.drawable.rounded_watch_24
                        "PHONE_BATTERY" -> R.drawable.rounded_mobile_24
                        else -> R.drawable.rounded_widgets_24
                    },
                ) {
                    complicationOptions.forEach { option ->
                        val label = complicationLabels[option] ?: option
                        SegmentedDropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                leftComplication = option
                                prefs.edit().putString("watchface_left_complication", option).apply()
                                DeviceInfoSyncManager.forceSync(context)
                            },
                        )
                    }
                }
                ConfigPickerItem(
                    title = stringResource(R.string.watchface_right_complication_title),
                    selectedValue = complicationLabels[rightComplication] ?: rightComplication,
                    iconRes = when (rightComplication) {
                        "DYNAMIC" -> R.drawable.rounded_auto_awesome_24
                        "HEART_RATE" -> R.drawable.rounded_favorite_24
                        "STEPS" -> R.drawable.rounded_steps_24
                        "DISTANCE" -> R.drawable.rounded_distance_24
                        "CALORIES" -> R.drawable.rounded_local_fire_department_24
                        "NOTIFICATIONS" -> R.drawable.rounded_notifications_unread_24
                        "NOW_PLAYING" -> R.drawable.rounded_music_note_24
                        "SOUND_MODE" -> R.drawable.rounded_volume_up_24
                        "WATCH_BATTERY" -> R.drawable.rounded_watch_24
                        "PHONE_BATTERY" -> R.drawable.rounded_mobile_24
                        else -> R.drawable.rounded_widgets_24
                    },
                ) {
                    complicationOptions.forEach { option ->
                        val label = complicationLabels[option] ?: option
                        SegmentedDropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                rightComplication = option
                                prefs.edit().putString("watchface_right_complication", option).apply()
                                DeviceInfoSyncManager.forceSync(context)
                            },
                        )
                    }
                }
            }
        }

        // Section 3: At a Glance & Glow
        Text(
            text = stringResource(R.string.watchface_category_glance),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_upcoming_24,
                title = stringResource(R.string.watchface_show_glance_title),
                description = stringResource(R.string.watchface_show_glance_desc),
                isChecked = showUpcomingEvents,
                onCheckedChange = {
                    showUpcomingEvents = it
                    prefs.edit()
                        .putBoolean("watchface_show_upcoming_events", it)
                        .putBoolean("watchface_show_glance", it)
                        .apply()
                    DeviceInfoSyncManager.forceSync(context)
                },
            )
            if (showUpcomingEvents) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_widgets_24,
                    title = stringResource(R.string.watchface_glance_complications_title),
                    description = stringResource(R.string.watchface_glance_complications_desc),
                    showToggle = false,
                    onClick = {
                        showGlanceComplicationsSheet = true
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_blur_on_24,
                    title = stringResource(R.string.watchface_show_glow_title),
                    description = stringResource(R.string.watchface_show_glow_desc),
                    isChecked = showGlow,
                    onCheckedChange = {
                        showGlow = it
                        prefs.edit().putBoolean("watchface_show_glow", it).apply()
                        DeviceInfoSyncManager.forceSync(context)
                    },
                )
            }
        }
    }
}
