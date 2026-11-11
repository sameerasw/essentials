/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Audio
 * File: SetVolumeSettingsSheet.kt
 * Description: Configuration bottom sheet for the SetVolume automation action.
 *              Lets the user pick a sound channel (icon-only segmented picker)
 *              and set the target level (0–100%) with a slider.
 */

package com.sameerasw.essentials.ui.features.audio.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetVolumeSettingsSheet(
    initialAction: Action.SetVolume,
    onDismiss: () -> Unit,
    onSave: (Action.SetVolume) -> Unit,
) {
    val view = LocalView.current

    var selectedChannel by remember { mutableStateOf(initialAction.channel) }
    var selectedLevel by remember { mutableIntStateOf(initialAction.level) }

    val channels = Action.VolumeChannel.entries

    EssentialsBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.diy_action_set_volume,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            RoundedCardContainer(spacing = 2.dp) {
                SegmentedPicker(
                    items = channels,
                    selectedItem = selectedChannel,
                    onItemSelected = {
                        HapticUtil.performUIHaptic(view)
                        selectedChannel = it
                    },
                    labelProvider = { "" },
                    iconProvider = { channel ->
                        val iconRes =
                            when (channel) {
                                Action.VolumeChannel.MUSIC -> R.drawable.rounded_music_note_24
                                Action.VolumeChannel.RING -> R.drawable.rounded_ring_volume_24
                                Action.VolumeChannel.ALARM -> R.drawable.rounded_alarm_24
                                Action.VolumeChannel.CALL -> R.drawable.rounded_call_24
                                Action.VolumeChannel.NOTIFICATION -> R.drawable.rounded_notifications_unread_24
                                Action.VolumeChannel.SYSTEM -> R.drawable.rounded_android_24
                            }
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription =
                                when (channel) {
                                    Action.VolumeChannel.MUSIC -> stringResource(R.string.diy_volume_channel_music)
                                    Action.VolumeChannel.RING -> stringResource(R.string.diy_volume_channel_ring)
                                    Action.VolumeChannel.ALARM -> stringResource(R.string.diy_volume_channel_alarm)
                                    Action.VolumeChannel.CALL -> stringResource(R.string.diy_volume_channel_call)
                                    Action.VolumeChannel.NOTIFICATION -> stringResource(R.string.diy_volume_channel_notification)
                                    Action.VolumeChannel.SYSTEM -> stringResource(R.string.diy_volume_channel_system)
                                },
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                val channelTitleRes =
                    when (selectedChannel) {
                        Action.VolumeChannel.MUSIC -> R.string.diy_volume_channel_music
                        Action.VolumeChannel.RING -> R.string.diy_volume_channel_ring
                        Action.VolumeChannel.ALARM -> R.string.diy_volume_channel_alarm
                        Action.VolumeChannel.CALL -> R.string.diy_volume_channel_call
                        Action.VolumeChannel.NOTIFICATION -> R.string.diy_volume_channel_notification
                        Action.VolumeChannel.SYSTEM -> R.string.diy_volume_channel_system
                    }
                val channelIconRes =
                    when (selectedChannel) {
                        Action.VolumeChannel.MUSIC -> R.drawable.rounded_music_note_24
                        Action.VolumeChannel.RING -> R.drawable.rounded_ring_volume_24
                        Action.VolumeChannel.ALARM -> R.drawable.rounded_alarm_24
                        Action.VolumeChannel.CALL -> R.drawable.rounded_call_24
                        Action.VolumeChannel.NOTIFICATION -> R.drawable.rounded_notifications_unread_24
                        Action.VolumeChannel.SYSTEM -> R.drawable.rounded_android_24
                    }

                ConfigSliderItem(
                    title = stringResource(channelTitleRes),
                    value = selectedLevel.toFloat(),
                    onValueChange = { selectedLevel = it.toInt() },
                    valueRange = 0f..100f,
                    increment = 5f,
                    steps = 19,
                    valueFormatter = { "${it.toInt()}%" },
                    iconRes = channelIconRes,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    TranslatableText(R.string.action_cancel)
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSave(initialAction.copy(channel = selectedChannel, level = selectedLevel))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    TranslatableText(R.string.action_save)
                }
            }
        }
    }
}
