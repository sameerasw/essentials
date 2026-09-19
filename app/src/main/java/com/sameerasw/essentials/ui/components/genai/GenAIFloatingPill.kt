/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: GenAIFloatingPill.kt
 * Description: UI layout element for GenAIFloatingPill.kt.
 */

package com.sameerasw.essentials.ui.components.genai

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.genai.AutomationSuggestion
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GenAIFloatingPill(
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (AutomationSuggestion) -> Unit,
    onReset: () -> Unit = {},
    isLoading: Boolean = false,
    suggestion: AutomationSuggestion? = null,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var promptText by remember { mutableStateOf("") }
    val isPromptValid = promptText.isNotBlank()

    LaunchedEffect(isLoading) {
        if (isLoading) {
            while (true) {
                HapticUtil.performLightHaptic(view)
                delay(300)
            }
        }
    }

    val pulseDuration = if (isLoading) 500 else 1750
    val infiniteTransition = rememberInfiniteTransition(label = "blurPulse")
    val animatedBlurRadius by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 80f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = pulseDuration),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "blurRadius",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { window ->
                window.setGravity(Gravity.BOTTOM)
                window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                )
                window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE,
                )
                window.setDimAmount(0.32f)
                window.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }

        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
        ) {
            val bgModifier =
                if (suggestion != null) {
                    Modifier
                        .matchParentSize()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(32.dp),
                        )
                } else {
                    Modifier
                        .matchParentSize()
                        .progressiveBlur(
                            blurRadius = animatedBlurRadius,
                            height = 150f,
                            direction = BlurDirection.TOP,
                            showGradientOverlay = false,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded,
                        ).background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(32.dp),
                        )
                }

            Box(modifier = bgModifier)

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (suggestion != null) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 4.dp),
                    ) {
                        if (!suggestion.explanation.isNullOrEmpty()) {
                            Text(
                                text = suggestion.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                modifier =
                                    Modifier.padding(
                                        bottom = 12.dp,
                                        start = 4.dp,
                                        end = 4.dp,
                                    ),
                            )
                        }

                        val triggerOrStateTitle =
                            when {
                                suggestion.type.equals("APP", ignoreCase = true) -> {
                                    val appsCount = suggestion.selectedApps.size
                                    if (appsCount > 0) "Apps ($appsCount selected)" else "App Automation"
                                }

                                suggestion.triggerType == "Schedule" && suggestion.hour != null -> {
                                    String.format(
                                        "Schedule (%02d:%02d)",
                                        suggestion.hour,
                                        suggestion.minute ?: 0,
                                    )
                                }

                                suggestion.stateType == "TimePeriod" && suggestion.hour != null && suggestion.endHour != null -> {
                                    String.format(
                                        "Time Period (%02d:%02d - %02d:%02d)",
                                        suggestion.hour,
                                        suggestion.minute ?: 0,
                                        suggestion.endHour,
                                        suggestion.endMinute ?: 0,
                                    )
                                }

                                else ->
                                    suggestion.triggerType
                                        ?: suggestion.stateType
                                        ?: suggestion.title.ifEmpty { suggestion.type }
                            }

                        val iconRes =
                            when {
                                suggestion.type.equals(
                                    "APP",
                                    ignoreCase = true,
                                ) -> R.drawable.rounded_apps_24

                                suggestion.triggerType != null -> R.drawable.rounded_bolt_24
                                suggestion.stateType != null -> R.drawable.rounded_toggle_on_24
                                else -> R.drawable.rounded_apps_24
                            }

                        Surface(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = triggerOrStateTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_arrow_cool_down_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (suggestion.actionTypes.isNotEmpty()) {
                                suggestion.actionTypes.forEach { actionName ->
                                    val actionIconRes =
                                        when (actionName) {
                                            "FreezeTag" -> R.drawable.rounded_mode_cool_24
                                            "SometimesEssentials" -> R.drawable.rounded_settings_24
                                            "DimWallpaper" -> R.drawable.rounded_mobile_screensaver_24
                                            "DeviceEffects" -> R.drawable.rounded_bed_24
                                            "SoundMode" -> R.drawable.rounded_volume_up_24
                                            "TurnOnFlashlight", "ToggleFlashlight" -> R.drawable.rounded_flashlight_on_24
                                            "TurnOffFlashlight" -> R.drawable.rounded_flashlight_on_24
                                            "HapticVibration" -> R.drawable.rounded_mobile_vibrate_24
                                            "ShowNotification" -> R.drawable.rounded_notifications_unread_24
                                            "RemoveNotification" -> R.drawable.rounded_notifications_off_24
                                            "TurnOnLowPower", "TurnOffLowPower" -> R.drawable.rounded_battery_android_frame_shield_24
                                            "ScreenOff" -> R.drawable.rounded_mobile_off_24
                                            "MediaPlayPause" -> R.drawable.round_play_arrow_24
                                            "MediaNext" -> R.drawable.rounded_skip_next_24
                                            "MediaPrevious" -> R.drawable.rounded_skip_previous_24
                                            "AIAssistant" -> R.drawable.google
                                            "TakeScreenshot" -> R.drawable.rounded_screenshot_region_24
                                            "ToggleMediaVolume" -> R.drawable.rounded_mobile_sound_24
                                            "LikeCurrentSong" -> R.drawable.rounded_favorite_24
                                            "CircleToSearch" -> R.drawable.rounded_search_24
                                            "PinApp" -> R.drawable.rounded_shield_lock_24
                                            else -> R.drawable.rounded_play_arrow_24
                                        }

                                    val actionDetail =
                                        when (actionName) {
                                            "FreezeTag" -> {
                                                val mode = suggestion.freezeTagMode ?: "Freeze"
                                                val tags = suggestion.freezeTagIds.joinToString()
                                                if (tags.isNotBlank()) "$mode ($tags)" else mode
                                            }

                                            "SoundMode" -> suggestion.soundMode ?: "SOUND"
                                            "SometimesEssentials" -> {
                                                val details = mutableListOf<String>()
                                                if (suggestion.lockScreenClockStyle != null) {
                                                    details.add(
                                                        "Clock: ${suggestion.lockScreenClockStyle}",
                                                    )
                                                }
                                                if (suggestion.alwaysOnDisplayMode != null) {
                                                    details.add(
                                                        "AOD: ${suggestion.alwaysOnDisplayMode}",
                                                    )
                                                }
                                                if (suggestion.essentialsOnDisplayMode != null) {
                                                    details.add(
                                                        "EOD: ${suggestion.essentialsOnDisplayMode}",
                                                    )
                                                }
                                                if (details.isNotEmpty()) details.joinToString(" • ") else null
                                            }

                                            else -> null
                                        }

                                    Surface(
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                painter = painterResource(actionIconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = actionName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                if (actionDetail != null) {
                                                    Text(
                                                        text = actionDetail,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                            MaterialTheme.colorScheme.onPrimary.copy(
                                                                alpha = 0.8f,
                                                            ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "No actions defined",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(14.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.diy_genai_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }

                // Prompt
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.diy_genai_describe_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            )
                        },
                        singleLine = true,
                        enabled = !isLoading && suggestion == null,
                        shape = RoundedCornerShape(24.dp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                disabledTextColor = MaterialTheme.colorScheme.onPrimary,
                                disabledPlaceholderColor =
                                    MaterialTheme.colorScheme.onPrimary.copy(
                                        alpha = 0.6f,
                                    ),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        modifier = Modifier.weight(1f),
                    )

                    AnimatedVisibility(
                        visible = isPromptValid || isLoading || suggestion != null,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(8.dp))
                            if (suggestion != null) {
                                IconButton(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        promptText = ""
                                        onReset()
                                    },
                                    modifier =
                                        Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_refresh_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        onConfirm(suggestion)
                                    },
                                    modifier =
                                        Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onPrimary),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_check_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (isPromptValid && !isLoading) {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            onSend(promptText.trim())
                                        }
                                    },
                                    enabled = isPromptValid && !isLoading,
                                    modifier =
                                        Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onPrimary),
                                ) {
                                    if (isLoading) {
                                        LoadingIndicator(
                                            modifier = Modifier.size(48.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_arrow_upward_24),
                                            contentDescription = stringResource(R.string.diy_genai_describe_send),
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
