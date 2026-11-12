/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Battery
 * File: ChargingModeSettingsSheet.kt
 * Description: Config sheet for the "Set Charging Mode" DIY automation action.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.battery.ChargingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingModeSettingsSheet(
    initialAction: Action.SetChargingMode,
    onDismiss: () -> Unit,
    onSave: (Action.SetChargingMode) -> Unit,
) {
    val view = LocalView.current

    var selectedMode by remember { mutableStateOf(initialAction.mode) }

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
                stringResId = R.string.diy_action_set_charging_mode,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            TranslatableText(
                stringResId = R.string.diy_charging_mode_desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RoundedCardContainer(spacing = 2.dp) {
                ChargingMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    val title =
                        when (mode) {
                            ChargingMode.OFF -> stringResource(R.string.label_charging_mode_off)
                            ChargingMode.ADAPTIVE -> stringResource(R.string.adaptive_charging)
                            ChargingMode.LIMITED -> stringResource(R.string.label_charging_mode_limited)
                        }
                    val icon =
                        when (mode) {
                            ChargingMode.OFF -> R.drawable.outline_battery_android_frame_bolt_24
                            ChargingMode.ADAPTIVE -> R.drawable.rounded_battery_android_frame_plus_24
                            ChargingMode.LIMITED -> R.drawable.rounded_battery_android_frame_shield_24
                        }

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    HapticUtil.performUIHaptic(view)
                                    selectedMode = mode
                                }.background(MaterialTheme.colorScheme.surfaceBright)
                                .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                selectedMode = mode
                            },
                        )
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
                        painter = painterResource(id = R.drawable.rounded_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    TranslatableText(R.string.action_cancel)
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSave(initialAction.copy(mode = selectedMode))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
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
