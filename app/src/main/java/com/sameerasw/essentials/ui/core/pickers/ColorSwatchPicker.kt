/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Pickers
 * File: ColorSwatchPicker.kt
 * Description: Reusable solid color swatch picker row with preset palettes and haptic feedback.
 */

package com.sameerasw.essentials.ui.core.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sameerasw.essentials.utils.HapticUtil

val DUO_PRESET_COLORS =
    listOf(
        "#00E676", // Vibrant Spring Green (Default Charging)
        "#4CAF50", // Standard Green
        "#00BCD4", // Cyan
        "#00E5FF", // Electric Cyan
        "#2196F3", // Blue
        "#3F51B5", // Indigo
        "#9C27B0", // Purple
        "#E91E63", // Pink
        "#FFEB3B", // Vivid Yellow (Default Low)
        "#FFD600", // Bright Gold
        "#FF9800", // Orange
        "#FF5722", // Deep Orange
        "#F44336", // Vivid Red (Default Critical)
        "#FF1744", // Bright Red
        "#FFFFFF", // Pure White
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSwatchPicker(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<String> = DUO_PRESET_COLORS,
) {
    val view = LocalView.current
    val carouselState = rememberCarouselState { colors.size }

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 46.dp,
        minSmallItemWidth = 10.dp,
        maxSmallItemWidth = 42.dp,
        itemSpacing = 6.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(vertical = 4.dp),
    ) { index ->
        val colorHex = colors[index]
        val isSelected = colorHex.equals(selectedColorHex, ignoreCase = true)
        val parsedColor =
            try {
                Color(android.graphics.Color.parseColor(colorHex))
            } catch (_: Exception) {
                MaterialTheme.colorScheme.primary
            }

        val isLightColor =
            try {
                val c = android.graphics.Color.parseColor(colorHex)
                val r = android.graphics.Color.red(c) / 255.0
                val g = android.graphics.Color.green(c) / 255.0
                val b = android.graphics.Color.blue(c) / 255.0
                (0.299 * r + 0.587 * g + 0.114 * b) > 0.5
            } catch (_: Exception) {
                false
            }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .maskClip(RoundedCornerShape(16.dp))
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
                    tint = if (isLightColor) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
