/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - System
 * File: SometimesEssentialsSettingsSheet.kt
 * Description: UI component and settings composable for System feature domain.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.ui.core.cards.FeatureDropdownRow
import com.sameerasw.essentials.ui.core.cards.FeatureToggleRow
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SometimesEssentialsSettingsSheet(
    initialAction: Action.SometimesEssentials,
    onDismiss: () -> Unit,
    onSave: (Action.SometimesEssentials) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var changeNotificationLighting by remember { mutableStateOf(initialAction.changeNotificationLighting) }
    var notificationLightingEnabled by remember { mutableStateOf(initialAction.notificationLightingEnabled) }

    var changeFlashlightPulse by remember { mutableStateOf(initialAction.changeFlashlightPulse) }
    var flashlightPulseEnabled by remember { mutableStateOf(initialAction.flashlightPulseEnabled) }

    var changeBatteryNotification by remember { mutableStateOf(initialAction.changeBatteryNotification) }
    var batteryNotificationEnabled by remember { mutableStateOf(initialAction.batteryNotificationEnabled) }

    var changeEssentialsOnDisplay by remember { mutableStateOf(initialAction.changeEssentialsOnDisplay) }
    var essentialsOnDisplayMode by remember { mutableStateOf(initialAction.essentialsOnDisplayMode) }

    var changeAlwaysOnDisplay by remember { mutableStateOf(initialAction.changeAlwaysOnDisplay) }
    var alwaysOnDisplayMode by remember { mutableStateOf(initialAction.alwaysOnDisplayMode) }

    var changeGloveMode by remember { mutableStateOf(initialAction.changeGloveMode) }
    var gloveModeEnabled by remember { mutableStateOf(initialAction.gloveModeEnabled) }

    var changeLockScreenClock by remember { mutableStateOf(initialAction.changeLockScreenClock) }
    var lockScreenClockStyle by remember { mutableStateOf(initialAction.lockScreenClockStyle) }

    var changeSyncSoundModeWatch by remember { mutableStateOf(initialAction.changeSyncSoundModeWatch) }
    var syncSoundModeWatchEnabled by remember { mutableStateOf(initialAction.syncSoundModeWatchEnabled) }

    var changeSmartPixels by remember { mutableStateOf(initialAction.changeSmartPixels) }
    var smartPixelsEnabled by remember { mutableStateOf(initialAction.smartPixelsEnabled) }

    val clockOptions =
        remember {
            listOf(
                "DEFAULT" to R.string.lock_screen_clock_default,
                "ANALOG_CLOCK_BIGNUM" to R.string.lock_screen_clock_bignum,
                "DIGITAL_CLOCK_CALLIGRAPHY" to R.string.lock_screen_clock_calligraphy,
                "DIGITAL_CLOCK_GROWTH" to R.string.lock_screen_clock_growth,
                "DIGITAL_CLOCK_HANDWRITTEN" to R.string.lock_screen_clock_handwritten,
                "DIGITAL_CLOCK_INFLATE" to R.string.lock_screen_clock_inflate,
                "DIGITAL_CLOCK_METRO" to R.string.lock_screen_clock_metro,
                "DIGITAL_CLOCK_NUMBEROVERLAP" to R.string.lock_screen_clock_numoverlap,
                "DIGITAL_CLOCK_WEATHER" to R.string.lock_screen_clock_weather,
            )
        }

    EssentialsBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.diy_action_sometimes_essentials),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
            ) {
                RoundedCardContainer(spacing = 2.dp) {
                    FeatureToggleRow(
                        title = stringResource(R.string.feat_notification_lighting_title),
                        iconRes = R.drawable.rounded_magnify_fullscreen_24,
                        isChecked = changeNotificationLighting,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            changeNotificationLighting = it
                        },
                        switchValue = notificationLightingEnabled,
                        onSwitchChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            notificationLightingEnabled = it
                        },
                    )

                    FeatureToggleRow(
                        title = stringResource(R.string.flashlight_pulse_title),
                        iconRes = R.drawable.rounded_flashlight_on_24,
                        isChecked = changeFlashlightPulse,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            changeFlashlightPulse = it
                        },
                        switchValue = flashlightPulseEnabled,
                        onSwitchChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            flashlightPulseEnabled = it
                        },
                    )

                    FeatureToggleRow(
                        title = stringResource(R.string.feat_battery_notification_title),
                        iconRes = R.drawable.rounded_battery_android_frame_shield_24,
                        isChecked = changeBatteryNotification,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            changeBatteryNotification = it
                        },
                        switchValue = batteryNotificationEnabled,
                        onSwitchChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            batteryNotificationEnabled = it
                        },
                    )

                    FeatureDropdownRow(
                        title = stringResource(R.string.feat_essentials_on_display_title),
                        iconRes = R.drawable.rounded_music_note_24,
                        isChecked = changeEssentialsOnDisplay,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            changeEssentialsOnDisplay = it
                        },
                        selectedValue = essentialsOnDisplayMode,
                        options = listOf("Off", "On", "Docked"),
                        labelProvider = {
                            when (it) {
                                "Off" -> context.getString(R.string.off)
                                "On" -> context.getString(R.string.on)
                                "Docked" -> context.getString(R.string.tile_docked)
                                else -> it
                            }
                        },
                        onOptionSelected = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            essentialsOnDisplayMode = it
                        },
                    )

                    FeatureDropdownRow(
                        title = stringResource(R.string.feat_always_on_display_title),
                        iconRes = R.drawable.rounded_mobile_text_2_24,
                        isChecked = changeAlwaysOnDisplay,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            changeAlwaysOnDisplay = it
                        },
                        selectedValue = alwaysOnDisplayMode,
                        options = listOf("Off", "Dynamic", "On"),
                        labelProvider = {
                            when (it) {
                                "Off" -> context.getString(R.string.off)
                                "On" -> context.getString(R.string.on)
                                "Dynamic" -> context.getString(R.string.tile_dynamic)
                                else -> it
                            }
                        },
                        onOptionSelected = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            alwaysOnDisplayMode = it
                        },
                    )

                    FeatureToggleRow(
                        title = stringResource(R.string.label_mode_glove),
                        iconRes = R.drawable.round_front_hand_24,
                        isChecked = changeGloveMode,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            changeGloveMode = it
                        },
                        switchValue = gloveModeEnabled,
                        onSwitchChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            gloveModeEnabled = it
                        },
                    )

                    val currentClockLabel =
                        clockOptions.find { it.first == lockScreenClockStyle }?.second?.let {
                            stringResource(it)
                        } ?: lockScreenClockStyle
                    FeatureDropdownRow(
                        title = stringResource(R.string.feat_lock_screen_clock_title),
                        iconRes = R.drawable.rounded_nest_clock_farsight_analog_24,
                        isChecked = changeLockScreenClock,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            changeLockScreenClock = it
                        },
                        selectedValue = currentClockLabel,
                        options = clockOptions.map { it.first },
                        labelProvider = { key ->
                            val stringRes = clockOptions.find { it.first == key }?.second
                            if (stringRes != null) context.getString(stringRes) else key
                        },
                        onOptionSelected = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            lockScreenClockStyle = it
                        },
                    )

                    FeatureToggleRow(
                        title = stringResource(R.string.feat_sync_sound_mode_title),
                        iconRes = R.drawable.rounded_watch_24,
                        isChecked = changeSyncSoundModeWatch,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            changeSyncSoundModeWatch = it
                        },
                        switchValue = syncSoundModeWatchEnabled,
                        onSwitchChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            syncSoundModeWatchEnabled = it
                        },
                    )

                    FeatureToggleRow(
                        title = stringResource(R.string.feat_smart_pixels_title),
                        iconRes = R.drawable.rounded_grain_24,
                        isChecked = changeSmartPixels,
                        onCheckedChange = {
                            HapticUtil.performUIHaptic(view)
                            changeSmartPixels = it
                        },
                        switchValue = smartPixelsEnabled,
                        onSwitchChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            smartPixelsEnabled = it
                        },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onSave(
                            Action.SometimesEssentials(
                                changeNotificationLighting = changeNotificationLighting,
                                notificationLightingEnabled = notificationLightingEnabled,
                                changeFlashlightPulse = changeFlashlightPulse,
                                flashlightPulseEnabled = flashlightPulseEnabled,
                                changeBatteryNotification = changeBatteryNotification,
                                batteryNotificationEnabled = batteryNotificationEnabled,
                                changeEssentialsOnDisplay = changeEssentialsOnDisplay,
                                essentialsOnDisplayMode = essentialsOnDisplayMode,
                                changeAlwaysOnDisplay = changeAlwaysOnDisplay,
                                alwaysOnDisplayMode = alwaysOnDisplayMode,
                                changeGloveMode = changeGloveMode,
                                gloveModeEnabled = gloveModeEnabled,
                                changeLockScreenClock = changeLockScreenClock,
                                lockScreenClockStyle = lockScreenClockStyle,
                                changeSyncSoundModeWatch = changeSyncSoundModeWatch,
                                syncSoundModeWatchEnabled = syncSoundModeWatchEnabled,
                                changeSmartPixels = changeSmartPixels,
                                smartPixelsEnabled = smartPixelsEnabled,
                            ),
                        )
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
