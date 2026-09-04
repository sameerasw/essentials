/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Conscious Gate
 * File: SettingsRowSurface.kt
 * Description: Shared row background (surfaceBright, small rounded corners) used to group
 * multiple composables under one continuous surface within a Conscious Gate settings card.
 */

package com.sameerasw.essentials.ui.features.consciousgate.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsRowSurface(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                ).padding(contentPadding),
        content = content,
    )
}
