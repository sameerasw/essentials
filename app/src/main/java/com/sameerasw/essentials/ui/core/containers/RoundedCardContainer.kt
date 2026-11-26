/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Components
 * File: RoundedCardContainer.kt
 * Description: Material 3 Expressive 24.dp rounded card container wrapper for grouping settings items.
 */

package com.sameerasw.essentials.ui.core.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.theme.LocalUiDesign
import com.sameerasw.essentials.ui.theme.UiDesign

private val OneUiDividerThickness = 1.dp
private val OneUiDividerInset = 20.dp

@Composable
fun RoundedCardContainer(
    modifier: Modifier = Modifier,
    spacing: Dp = 2.dp,
    cornerRadius: Dp = 24.dp,
    containerColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUiDesign.current == UiDesign.ONE_UI && spacing <= 2.dp) {
        OneUiCardContainer(
            modifier = modifier,
            cornerRadius = cornerRadius,
            containerColor =
                if (containerColor == Color.Transparent) MaterialTheme.colorScheme.surfaceBright else containerColor,
            content = content,
        )
        return
    }
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(containerColor),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

@Composable
private fun OneUiCardContainer(
    modifier: Modifier,
    cornerRadius: Dp,
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val arrangement = remember { DividerArrangement() }
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(containerColor)
                .drawWithContent {
                    drawContent()
                    val inset = OneUiDividerInset.toPx()
                    val stroke = OneUiDividerThickness.toPx()
                    arrangement.dividers.forEach { y ->
                        drawLine(
                            color = dividerColor,
                            start = Offset(inset, y.toFloat()),
                            end = Offset(size.width - inset, y.toFloat()),
                            strokeWidth = stroke,
                        )
                    }
                },
        verticalArrangement = arrangement,
        content = content,
    )
}

private class DividerArrangement : Arrangement.Vertical {
    var dividers by mutableStateOf(IntArray(0))
        private set

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray,
    ) {
        val found = ArrayList<Int>()
        var y = 0
        var placedAny = false
        sizes.forEachIndexed { index, size ->
            if (size == 0) {
                outPositions[index] = y
                return@forEachIndexed
            }
            if (placedAny) found.add(y)
            outPositions[index] = y
            y += size
            placedAny = true
        }
        val next = found.toIntArray()
        if (!next.contentEquals(dividers)) dividers = next
    }
}
