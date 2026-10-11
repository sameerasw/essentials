/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - System
 * File: FaceUnlockBrightnessSettingsUI.kt
 * Description: Settings for the Face unlock brightness feature.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.animation.AnimatedVisibility
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

private val MAX_BRIGHTNESS_OPTIONS = listOf(50, 75, 100)

private const val MIN_AMBIENT_LUX = 0.1f
private const val MAX_AMBIENT_LUX = 50f

// Log scale so the dim end, where it matters most, gets finer steps than the bright end
private fun luxToPosition(lux: Float): Float =
    (ln(lux.coerceIn(MIN_AMBIENT_LUX, MAX_AMBIENT_LUX) / MIN_AMBIENT_LUX) / ln(MAX_AMBIENT_LUX / MIN_AMBIENT_LUX)) * 100f

private fun positionToLux(position: Float): Float {
    val lux = MIN_AMBIENT_LUX * (MAX_AMBIENT_LUX / MIN_AMBIENT_LUX).pow(position.coerceIn(0f, 100f) / 100f)
    return when {
        lux < 1f -> (lux * 20f).roundToInt() / 20f
        lux < 10f -> (lux * 10f).roundToInt() / 10f
        else -> lux.roundToInt().toFloat()
    }
}

private fun formatLux(lux: Float): String =
    when {
        lux < 1f -> "%.2f lx".format(lux)
        lux < 10f -> "%.1f lx".format(lux)
        else -> "${lux.roundToInt()} lx"
    }

@Composable
fun FaceUnlockBrightnessSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var requestingPermissionsFor by remember { mutableStateOf<List<String>?>(null) }
    var currentLux by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    currentLux = event.values[0]
                }

                override fun onAccuracyChanged(
                    sensor: Sensor?,
                    accuracy: Int,
                ) {}
            }
        if (lightSensor != null) sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    requestingPermissionsFor?.let { permKeys ->
        PermissionsBottomSheet(
            onDismissRequest = {
                requestingPermissionsFor = null
                viewModel.check(context)
            },
            featureTitle = stringResource(R.string.feat_face_unlock_brightness_title),
            permissions = PermissionUIHelper.getPermissionItems(permKeys, context, viewModel),
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_face_24,
                title = stringResource(R.string.feat_face_unlock_brightness_title),
                isChecked = viewModel.isFaceUnlockBrightnessEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !viewModel.isAccessibilityEnabled.value) {
                        requestingPermissionsFor = listOf("ACCESSIBILITY")
                    } else {
                        viewModel.setFaceUnlockBrightnessEnabled(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "face_unlock_brightness_enabled"),
            )
        }

        AnimatedVisibility(
            visible = viewModel.isFaceUnlockBrightnessEnabled.value,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                            .padding(top = 12.dp)
                            .highlight(highlightSetting == "face_unlock_max_brightness"),
                ) {
                    Text(
                        text = stringResource(R.string.flashlight_pulse_max_brightness),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SegmentedPicker(
                        items = MAX_BRIGHTNESS_OPTIONS,
                        selectedItem = viewModel.faceUnlockMaxBrightness.intValue,
                        onItemSelected = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.setFaceUnlockMaxBrightness(it)
                        },
                        labelProvider = { "$it%" },
                        title = R.string.flashlight_pulse_max_brightness,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ConfigSliderItem(
                    title = stringResource(R.string.face_unlock_ambient_threshold_title),
                    value = luxToPosition(viewModel.faceUnlockAmbientThreshold.floatValue),
                    onValueChange = { viewModel.setFaceUnlockAmbientThreshold(positionToLux(it)) },
                    valueRange = 0f..100f,
                    increment = 2f,
                    valueFormatter = { formatLux(positionToLux(it)) },
                    iconRes = R.drawable.rounded_brightness_auto_24,
                    description = currentLux?.let { stringResource(R.string.face_unlock_ambient_now, formatLux(it)) },
                    trailingContent =
                        currentLux?.let { lux ->
                            { AmbientMatchChip(matches = lux < viewModel.faceUnlockAmbientThreshold.floatValue) }
                        },
                    modifier = Modifier.highlight(highlightSetting == "face_unlock_ambient_threshold"),
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_brightness_auto_24,
                    title = stringResource(R.string.face_unlock_auto_illuminate_title),
                    isChecked = viewModel.isFaceUnlockAutoIlluminate.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setFaceUnlockAutoIlluminate(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "face_unlock_auto_illuminate"),
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_lock_24,
                    title = stringResource(R.string.face_unlock_trigger_unlock_title),
                    isChecked = viewModel.isFaceUnlockTriggerUnlock.value && ShellUtils.hasPermission(context),
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        if (checked && !ShellUtils.hasPermission(context)) {
                            requestingPermissionsFor = listOf(if (ShellUtils.isRootEnabled(context)) "ROOT" else "SHIZUKU")
                        } else {
                            viewModel.setFaceUnlockTriggerUnlock(checked)
                        }
                    },
                    modifier = Modifier.highlight(highlightSetting == "face_unlock_trigger_unlock"),
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_sunny_24,
                    title = stringResource(R.string.face_unlock_light_tint_title),
                    isChecked = viewModel.isFaceUnlockLightTint.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setFaceUnlockLightTint(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "face_unlock_light_tint"),
                )
            }
        }
    }
}

@Composable
private fun AmbientMatchChip(matches: Boolean) {
    val containerColor by animateColorAsState(
        if (matches) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "ambientChipContainer",
    )
    val dotColor by animateColorAsState(
        if (matches) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "ambientChipDot",
    )
    Surface(shape = CircleShape, color = containerColor) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape),
            )
            Text(
                text =
                    stringResource(
                        if (matches) R.string.face_unlock_ambient_would_trigger else R.string.face_unlock_ambient_too_bright,
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
