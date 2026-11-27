/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Components
 * File: Switch.kt
 * Description: Design aware switch that renders a One UI style switch in One UI mode.
 */

package com.sameerasw.essentials.ui.core.controls

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.theme.LocalUiDesign
import com.sameerasw.essentials.ui.theme.UiDesign
import androidx.compose.material3.Switch as MaterialSwitch

private val TrackWidth = 40.dp
private val TrackHeight = 22.dp
private val ThumbRadius = 8.5.dp
private val ThumbInset = 11.dp

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    if (LocalUiDesign.current != UiDesign.ONE_UI) {
        MaterialSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            thumbContent = thumbContent,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
        )
        return
    }
    OneUiSwitch(checked, onCheckedChange, modifier, enabled, interactionSource)
}

@Composable
private fun OneUiSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "one_ui_switch_track",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - ThumbInset else ThumbInset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "one_ui_switch_thumb",
    )
    val toggleModifier =
        if (onCheckedChange != null) {
            Modifier.toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = TrackWidth / 2 + 4.dp),
            )
        } else {
            Modifier
        }
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .then(toggleModifier)
                .alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(TrackWidth, TrackHeight)) {
            drawRoundRect(
                color = trackColor,
                cornerRadius = CornerRadius(size.height / 2f),
            )
            drawCircle(
                color = Color.White,
                radius = ThumbRadius.toPx(),
                center = Offset(thumbOffset.toPx(), size.height / 2f),
            )
        }
    }
}
