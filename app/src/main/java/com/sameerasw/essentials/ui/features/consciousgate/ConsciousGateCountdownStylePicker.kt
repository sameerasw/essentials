/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Conscious Gate
 * File: ConsciousGateCountdownStylePicker.kt
 * Description: UI component letting the user pick which Material 3 Expressive countdown
 * treatment the Conscious Gate pause screen uses, with a small live preview per option.
 */

package com.sameerasw.essentials.ui.features.consciousgate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.domain.model.ConsciousGateCountdownStyle
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConsciousGateCountdownStylePicker(
    selectedStyle: ConsciousGateCountdownStyle,
    onStyleSelected: (ConsciousGateCountdownStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val styles =
        listOf(
            ConsciousGateCountdownStyle.CIRCULAR_WAVY,
            ConsciousGateCountdownStyle.LOADING_BLOB,
            ConsciousGateCountdownStyle.BREATHING_DOT,
            ConsciousGateCountdownStyle.LINEAR_WAVY,
        )
    val view = LocalView.current
    val selectedIndex = styles.indexOf(selectedStyle).coerceAtLeast(0)

    RoundedCardContainer(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                    ).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val itemModifiers = List(styles.size) { Modifier.weight(1f) }

            styles.forEachIndexed { index, style ->
                ToggleButton(
                    checked = selectedIndex == index,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onStyleSelected(style)
                    },
                    modifier = itemModifiers[index].semantics { role = Role.RadioButton },
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            styles.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                ) {
                    val isSelected = selectedIndex == index
                    val previewColor =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ConsciousGateCountdownIndicator(
                        style = style,
                        progress = { 0.6f },
                        modifier = Modifier.size(28.dp),
                        indicatorColor = previewColor,
                        trackColor = previewColor.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}
