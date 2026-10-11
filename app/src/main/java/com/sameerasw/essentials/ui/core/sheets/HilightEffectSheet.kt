/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Components
 * File: HilightEffectSheet.kt
 * Description: Pattern, colour and duration editor for Hilight effects, with a live try-out.
 */

package com.sameerasw.essentials.ui.core.sheets

import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.controller.HilightController
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.HilightEffect
import com.sameerasw.essentials.domain.model.HilightPattern
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.ColorSwatchPicker
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun HilightEffectEditor(
    effect: HilightEffect,
    onEffectChange: (HilightEffect) -> Unit,
    modifier: Modifier = Modifier,
    canTry: Boolean = true,
) {
    val context = LocalContext.current
    val view = LocalView.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RoundedCardContainer {
            FlowRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HilightPattern.entries.forEach { pattern ->
                    FilterChip(
                        selected = effect.pattern == pattern,
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            onEffectChange(effect.copy(pattern = pattern))
                        },
                        label = { Text(stringResource(pattern.title)) },
                    )
                }
            }
        }

        // Rainbow cycles through every hue, so a colour choice would have no effect
        if (effect.pattern != HilightPattern.RAINBOW) {
            ColorSwatchPicker(
                selectedColorHex = toHex(effect.color),
                onColorSelected = { hex -> onEffectChange(effect.copy(color = Color.parseColor(hex))) },
            )
        }

        RoundedCardContainer {
            ConfigSliderItem(
                title = stringResource(R.string.hilight_duration_title),
                value = effect.durationMs / 1000f,
                onValueChange = { onEffectChange(effect.copy(durationMs = (it * 1000).toLong())) },
                valueRange = HilightEffect.MIN_DURATION_MS / 1000f..HilightEffect.MAX_DURATION_MS / 1000f,
                increment = 1f,
                steps = ((HilightEffect.MAX_DURATION_MS - HilightEffect.MIN_DURATION_MS) / 1000 - 1).toInt(),
                valueFormatter = { context.getString(R.string.hilight_duration_value, it.toInt()) },
                iconRes = R.drawable.rounded_timer_24,
            )
        }

        OutlinedButton(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                if (!HilightController.play(effect)) {
                    Toast.makeText(context, R.string.hilight_unavailable_toast, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canTry,
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_auto_awesome_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.hilight_try_it))
        }
    }
}

@Composable
fun HilightEffectSettingsSheet(
    initialAction: Action.Hilight,
    onDismiss: () -> Unit,
    onSave: (Action.Hilight) -> Unit,
) {
    HilightEffectSheet(
        title = stringResource(R.string.diy_action_hilight),
        initialEffect = initialAction.toEffect(),
        onDismiss = onDismiss,
        onSave = { onSave(Action.Hilight(it.pattern, it.color, it.durationMs)) },
    )
}

@Composable
fun HilightEffectSheet(
    title: String,
    initialEffect: HilightEffect,
    onDismiss: () -> Unit,
    onSave: (HilightEffect) -> Unit,
    canTry: Boolean = true,
) {
    val view = LocalView.current
    var effect by remember { mutableStateOf(initialEffect) }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            HilightEffectEditor(effect = effect, onEffectChange = { effect = it }, canTry = canTry)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        HilightController.stop()
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
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        HilightController.stop()
                        onSave(effect)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

private fun toHex(color: Int) = String.format("#%06X", color and 0xFFFFFF)
