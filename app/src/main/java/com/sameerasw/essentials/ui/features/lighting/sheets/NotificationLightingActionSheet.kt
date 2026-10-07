/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Lighting
 * File: NotificationLightingActionSheet.kt
 * Description: Config sheet for the "Trigger notification lighting" DIY automation action.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.GlowSidesPicker
import com.sameerasw.essentials.ui.core.pickers.NotificationLightingColorModePicker
import com.sameerasw.essentials.ui.core.pickers.NotificationLightingStylePicker
import com.sameerasw.essentials.ui.core.pickers.NotificationLightingSystemModePicker
import com.sameerasw.essentials.ui.features.system.ColorCircle
import com.sameerasw.essentials.utils.HapticUtil

private val presetColors =
    listOf(
        0xFFF44336, 0xFFFF9800, 0xFFFFEB3B, 0xFF4CAF50,
        0xFF00BCD4, 0xFF2196F3, 0xFF6200EE, 0xFFE91E63, 0xFFFFFFFF,
    ).map { it.toInt() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationLightingActionSheet(
    initialAction: Action.TriggerNotificationLighting,
    onDismiss: () -> Unit,
    onSave: (Action.TriggerNotificationLighting) -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    var draft by remember { mutableStateOf(initialAction) }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.diy_action_notification_lighting),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            RoundedCardContainer(spacing = 2.dp) {
                NotificationLightingStylePicker(
                    selectedStyle = draft.style,
                    onStyleSelected = { draft = draft.copy(style = it) },
                )

                if (draft.style == NotificationLightingStyle.GLOW) {
                    GlowSidesPicker(
                        selectedSides = draft.glowSides,
                        onSideToggled = { side, checked ->
                            draft =
                                draft.copy(
                                    glowSides = if (checked) draft.glowSides + side else draft.glowSides - side,
                                )
                        },
                    )
                }

                if (draft.style == NotificationLightingStyle.SYSTEM) {
                    NotificationLightingSystemModePicker(
                        selectedMode = draft.systemMode,
                        onModeSelected = { draft = draft.copy(systemMode = it) },
                    )
                }
            }

            if (draft.style != NotificationLightingStyle.SYSTEM) {
                RoundedCardContainer(spacing = 2.dp) {
                    NotificationLightingColorModePicker(
                        selectedMode = draft.colorMode,
                        onModeSelected = { draft = draft.copy(colorMode = it) },
                        options =
                            listOf(
                                R.string.color_mode_system to NotificationLightingColorMode.SYSTEM,
                                R.string.color_mode_custom to NotificationLightingColorMode.CUSTOM,
                            ),
                    )

                    if (draft.colorMode == NotificationLightingColorMode.CUSTOM) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            presetColors.forEach { colorInt ->
                                ColorCircle(
                                    color = Color(colorInt),
                                    isSelected = draft.customColor == colorInt,
                                    size = 32.dp,
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        draft = draft.copy(customColor = colorInt)
                                    },
                                )
                            }
                        }
                    }
                }

                RoundedCardContainer(spacing = 2.dp) {
                    ConfigSliderItem(
                        title = stringResource(R.string.notification_lighting_pulse_count_title),
                        value = draft.pulseCount.toFloat(),
                        onValueChange = { draft = draft.copy(pulseCount = it.toInt()) },
                        valueRange = 1f..5f,
                        steps = 3,
                        increment = 1f,
                        valueFormatter = { "${it.toInt()}" },
                    )
                    ConfigSliderItem(
                        title = stringResource(R.string.notification_lighting_pulse_duration_title),
                        value = draft.pulseDuration.toFloat(),
                        onValueChange = { draft = draft.copy(pulseDuration = it.toLong()) },
                        valueRange = 100f..10000f,
                        increment = 100f,
                        valueFormatter = { "%.1fs".format(it / 1000f) },
                    )
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
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSave(draft)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = draft.style != NotificationLightingStyle.GLOW || draft.glowSides.isNotEmpty(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_save))
                }
            }

            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    CombinedActionExecutor.triggerNotificationLighting(context, draft)
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_magnify_fullscreen_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.action_preview))
            }
        }
    }
}
