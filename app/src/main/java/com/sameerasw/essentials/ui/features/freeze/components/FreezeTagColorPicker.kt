/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Freeze
 * File: FreezeTagColorPicker.kt
 * Description: UI component and settings composable for Freeze feature domain.
 */

package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.sheets.PRESET_PASTEL_COLORS
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreezeTagColorPicker(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val carouselState = rememberCarouselState { PRESET_PASTEL_COLORS.size }
    val view = LocalView.current

    Column(modifier = modifier.fillMaxWidth()) {
        TranslatableText(
            stringResId = R.string.freeze_tag_color_picker_label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 64.dp,
            minSmallItemWidth = 24.dp,
            maxSmallItemWidth = 36.dp,
            itemSpacing = 6.dp,
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp),
        ) { index ->
            val colorHex = PRESET_PASTEL_COLORS[index]
            val isSelected = colorHex.equals(selectedColorHex, ignoreCase = true)
            val parsedColor =
                try {
                    Color(android.graphics.Color.parseColor(colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.medium)
                        .background(parsedColor)
                        .clickable {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onColorSelected(colorHex)
                        },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
