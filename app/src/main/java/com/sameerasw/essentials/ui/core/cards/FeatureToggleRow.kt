/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: FeatureToggleRow.kt
 * Description: Reusable core UI component for FeatureToggleRow.kt.
 */

package com.sameerasw.essentials.ui.core.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.utils.ColorUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureToggleRow(
    title: String,
    iconRes: Int,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchValue: Boolean,
    onSwitchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        onClick = { onCheckedChange(!isChecked) },
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                )
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ColorUtil.getPastelColorFor(title)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = ColorUtil.getVibrantColorFor(title),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        trailingContent = {
            Switch(
                checked = switchValue,
                onCheckedChange = onSwitchChange,
                enabled = isChecked,
            )
        },
        colors =
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        content = {
            TranslatableText(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}
