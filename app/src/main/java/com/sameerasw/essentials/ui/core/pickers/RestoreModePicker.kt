/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: RestoreModePicker.kt
 * Description: Reusable core UI component for RestoreModePicker.kt.
 */

package com.sameerasw.essentials.ui.core.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RestoreModePicker(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<String> = listOf("Auto", "Notify"),
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceBright),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        ListItem(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_timer_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            contentPadding =
                PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                ),
            content = {
                Text(
                    text = stringResource(R.string.label_restore_mode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            options.forEachIndexed { index, mode ->
                val isChecked = selectedMode == mode

                ToggleButton(
                    checked = isChecked,
                    onCheckedChange = {
                        onModeSelected(mode)
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                    shapes =
                        when {
                            index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                ) {
                    Text(
                        text =
                            when (mode) {
                                "Auto" -> stringResource(R.string.app_colors_auto)
                                "Notify" -> stringResource(R.string.restore_mode_notify)
                                else -> mode
                            },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
