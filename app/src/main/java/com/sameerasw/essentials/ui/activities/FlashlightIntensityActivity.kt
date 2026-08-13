/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: FlashlightIntensityActivity.kt
 * Description: Activity component for FlashlightIntensityActivity.kt dialog overlay.
 */

package com.sameerasw.essentials.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.receivers.FlashlightActionReceiver
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.FlashlightUtil
import com.sameerasw.essentials.utils.HapticUtil

class FlashlightIntensityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val componentName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(
                "android.intent.extra.COMPONENT_NAME",
                android.content.ComponentName::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("android.intent.extra.COMPONENT_NAME")
        }
        if (componentName != null && componentName.className != "com.sameerasw.essentials.services.tiles.FlashlightTileService") {
            // Redirect to MainActivity for other tiles
            val mainIntent = Intent(this, com.sameerasw.essentials.MainActivity::class.java).apply {
                action = intent.action
                putExtra("android.intent.extra.COMPONENT_NAME", componentName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(mainIntent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.check(context)
            }
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                FlashlightIntensityOverlay(onDismiss = { finish() })
            }
        }
    }
}

enum class FlashlightSpecialMode {
    NONE, SOS, STROBE, FADE
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlashlightIntensityOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE) }

    // Get flashlight max level
    val maxLevel = remember {
        val cameraManager =
            context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        val cameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: "0"
        } catch (e: Exception) {
            "0"
        }
        FlashlightUtil.getMaxLevel(context, cameraId)
    }

    var intensity by remember {
        mutableFloatStateOf(
            prefs.getInt("flashlight_last_intensity", 1).toFloat()
        )
    }
    var lastSentLevel by remember { mutableIntStateOf(intensity.toInt()) }

    var isSpecialModesVisible by remember { mutableStateOf(false) }
    var selectedSpecialMode by remember { mutableStateOf(FlashlightSpecialMode.NONE) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        // Automatically turn on or update intensity on open
        val intent = Intent(context, FlashlightActionReceiver::class.java).apply {
            action = FlashlightActionReceiver.ACTION_SET_INTENSITY
            putExtra(FlashlightActionReceiver.EXTRA_INTENSITY, intensity.toInt())
        }
        context.sendBroadcast(intent)
    }

    fun updateSpecialMode(mode: FlashlightSpecialMode) {
        selectedSpecialMode = mode
        when (mode) {
            FlashlightSpecialMode.SOS -> {
                val intent = Intent(context, FlashlightActionReceiver::class.java).apply {
                    action = FlashlightActionReceiver.ACTION_START_SOS
                }
                context.sendBroadcast(intent)
            }
            FlashlightSpecialMode.STROBE -> {
                val intent = Intent(context, FlashlightActionReceiver::class.java).apply {
                    action = FlashlightActionReceiver.ACTION_START_STROBE
                    putExtra(FlashlightActionReceiver.EXTRA_STROBE_SPEED, 10f)
                    putExtra(FlashlightActionReceiver.EXTRA_STROBE_FADE, false)
                }
                context.sendBroadcast(intent)
            }
            FlashlightSpecialMode.FADE -> {
                val intent = Intent(context, FlashlightActionReceiver::class.java).apply {
                    action = FlashlightActionReceiver.ACTION_START_STROBE
                    putExtra(FlashlightActionReceiver.EXTRA_STROBE_SPEED, 10f)
                    putExtra(FlashlightActionReceiver.EXTRA_STROBE_FADE, true)
                }
                context.sendBroadcast(intent)
            }
            FlashlightSpecialMode.NONE -> {
                val intent = Intent(context, FlashlightActionReceiver::class.java).apply {
                    action = FlashlightActionReceiver.ACTION_SET_INTENSITY
                    putExtra(FlashlightActionReceiver.EXTRA_INTENSITY, intensity.toInt())
                }
                context.sendBroadcast(intent)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = true
                ) { /* Stop propagation */ },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_flashlight_on_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = stringResource(if (isSpecialModesVisible) R.string.feature_flashlight_effects_title else R.string.feature_flashlight_brightness_title),
                    style = MaterialTheme.typography.titleLarge
                )

                AnimatedContent(
                    targetState = isSpecialModesVisible,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "FlashlightControlSwap"
                ) { showModes ->
                    if (!showModes) {
                        Slider(
                            value = intensity,
                            onValueChange = { newVal ->
                                intensity = newVal
                                val level = newVal.toInt().coerceIn(1, maxLevel)

                                if (level != lastSentLevel) {
                                    lastSentLevel = level
                                    val intent =
                                        Intent(context, FlashlightActionReceiver::class.java).apply {
                                            action = FlashlightActionReceiver.ACTION_SET_INTENSITY
                                            putExtra(FlashlightActionReceiver.EXTRA_INTENSITY, level)
                                        }
                                    context.sendBroadcast(intent)
                                    prefs.edit().putInt("flashlight_last_intensity", level).apply()
                                    HapticUtil.performSliderHaptic(view)
                                }
                            },
                            valueRange = 1f..maxLevel.toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceBright,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ) {
                            // SOS
                            ToggleButton(
                                checked = selectedSpecialMode == FlashlightSpecialMode.SOS,
                                onCheckedChange = {
                                    HapticUtil.performUIHaptic(view)
                                    val nextMode = if (selectedSpecialMode == FlashlightSpecialMode.SOS) FlashlightSpecialMode.NONE else FlashlightSpecialMode.SOS
                                    updateSpecialMode(nextMode)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .semantics { role = Role.RadioButton },
                                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                            ) {
                                Text(stringResource(R.string.flashlight_mode_sos))
                            }

                            // Strobe
                            ToggleButton(
                                checked = selectedSpecialMode == FlashlightSpecialMode.STROBE,
                                onCheckedChange = {
                                    HapticUtil.performUIHaptic(view)
                                    val nextMode = if (selectedSpecialMode == FlashlightSpecialMode.STROBE) FlashlightSpecialMode.NONE else FlashlightSpecialMode.STROBE
                                    updateSpecialMode(nextMode)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .semantics { role = Role.RadioButton },
                                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                            ) {
                                Text(stringResource(R.string.flashlight_mode_strobe))
                            }

                            // Fade
                            ToggleButton(
                                checked = selectedSpecialMode == FlashlightSpecialMode.FADE,
                                onCheckedChange = {
                                    HapticUtil.performUIHaptic(view)
                                    val nextMode = if (selectedSpecialMode == FlashlightSpecialMode.FADE) FlashlightSpecialMode.NONE else FlashlightSpecialMode.FADE
                                    updateSpecialMode(nextMode)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .semantics { role = Role.RadioButton },
                                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                            ) {
                                Text(stringResource(R.string.flashlight_fade_label))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedToggleButton(
                        checked = isSpecialModesVisible,
                        onCheckedChange = { checked ->
                            HapticUtil.performUIHaptic(view)
                            isSpecialModesVisible = checked
                            if (!checked && selectedSpecialMode != FlashlightSpecialMode.NONE) {
                                updateSpecialMode(FlashlightSpecialMode.NONE)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_award_star_24),
                            contentDescription = "Toggle special modes",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.action_done))
                    }

                    Button(
                        onClick = {
                            val intent =
                                Intent(context, FlashlightActionReceiver::class.java).apply {
                                    action = FlashlightActionReceiver.ACTION_OFF
                                }
                            context.sendBroadcast(intent)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.action_turn_off))
                    }
                }
            }
        }
    }
}
