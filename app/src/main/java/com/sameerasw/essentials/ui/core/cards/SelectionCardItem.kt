/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: SelectionCardItem.kt
 * Description: Reusable core UI component for SelectionCardItem.kt.
 */

package com.sameerasw.essentials.ui.core.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun SelectionCardItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    iconRes: Int? = null,
) {
    val view = LocalView.current

    ListItem(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable {
                    HapticUtil.performUIHaptic(view)
                    onClick()
                },
        headlineContent = { TranslatableText(text = title) },
        supportingContent =
            if (description != null) {
                { TranslatableText(text = description) }
            } else {
                null
            },
        leadingContent =
            if (iconRes != null && iconRes != 0) {
                {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                null
            },
        colors =
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
            ),
    )
}
