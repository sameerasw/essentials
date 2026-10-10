package com.sameerasw.essentials.island.ui

import com.sameerasw.essentials.island.ui.components.MarqueeText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.island.model.LineContent

@Composable
fun LineTemplate(line: LineContent, spec: IslandLayoutSpec, startInset: Dp = 0.dp, endInset: Dp = 0.dp) {
    if (line.condensed) {
        CondensedLine(line, spec, startInset, endInset)
        return
    }
    val textEndInset = if (line.endSlot == null && spec.growDirection >= 0) spec.compactHeight * OPTICAL_INSET_RATIO else 0.dp
    Row(
        modifier = Modifier
            .width(spec.lineWidth - startInset - endInset)
            .height(spec.compactHeight)
            .padding(start = spec.cameraGap, end = spec.cameraGap + textEndInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon: @Composable () -> Unit = {
            Box(Modifier.squareFit(spec.cellSize), contentAlignment = Alignment.Center) { line.icon() }
        }
        val endSlot: @Composable () -> Unit = {
            line.endSlot?.let { slot -> Box(Modifier.squareFit(spec.cellSize), contentAlignment = Alignment.Center) { slot() } }
        }
        if (spec.growDirection == 0) {
            Row(
                modifier = Modifier.width((spec.lineWidth - spec.cameraSlotWidth) / 2 - spec.cameraGap - startInset),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spec.cellSpacing),
            ) {
                icon()
                LineText(line.start, TextAlign.Start, FontWeight.SemiBold, Modifier.weight(1f))
            }
            Spacer(Modifier.width(spec.cameraSlotWidth))
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spec.cellSpacing),
            ) {
                LineText(line.end, TextAlign.End, FontWeight.Normal, Modifier.weight(1f))
                endSlot()
            }
        } else {
            if (spec.growDirection > 0) Spacer(Modifier.width(spec.cameraSlotWidth - spec.cameraGap))
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spec.cellSpacing),
            ) {
                icon()
                LineText(line.start, TextAlign.Start, FontWeight.SemiBold, Modifier.weight(1f))
                LineText(line.end, TextAlign.End, FontWeight.Normal, Modifier.weight(1f))
                endSlot()
            }
            if (spec.growDirection < 0) Spacer(Modifier.width(spec.cameraSlotWidth - spec.cameraGap))
        }
    }
}

internal fun condensedTitleWidth(
    natural: Dp,
    lineWidth: Dp,
    cameraSlotWidth: Dp,
    cellSize: Dp,
    edgePad: Dp,
    endInset: Dp,
): Dp {
    val roomBesideCamera = ((lineWidth - edgePad * 2 - cameraSlotWidth - endInset) / 2).coerceAtLeast(cellSize)
    return natural.coerceIn(cellSize, roomBesideCamera)
}

@Composable
private fun CondensedLine(line: LineContent, spec: IslandLayoutSpec, startInset: Dp, endInset: Dp) {
    val style = IslandTextStyles.line.copy(fontWeight = FontWeight.SemiBold)
    val measurer = rememberTextMeasurer()
    val naturalPx = remember(line.start, style, measurer) {
        measurer.measure(line.start, style, maxLines = 1, softWrap = false).size.width
    }
    val natural = with(LocalDensity.current) { (naturalPx + 1).toDp() }
    val icon: @Composable () -> Unit = {
        Box(Modifier.squareFit(spec.cellSize), contentAlignment = Alignment.Center) { line.icon() }
    }
    val title: @Composable (Dp) -> Unit = { width ->
        MarqueeText(text = line.start, style = style, textAlign = TextAlign.Start, modifier = Modifier.width(width))
    }

    if (spec.growDirection == 0) {
        val edgePad = spec.cameraGap + spec.compactHeight * OPTICAL_INSET_RATIO
        Row(
            modifier = Modifier
                .height(spec.compactHeight)
                .padding(horizontal = edgePad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(spec.cellSize), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(spec.cameraSlotWidth))
            title(
                condensedTitleWidth(
                    natural = natural,
                    lineWidth = spec.lineWidth,
                    cameraSlotWidth = spec.cameraSlotWidth,
                    cellSize = spec.cellSize,
                    edgePad = edgePad,
                    endInset = endInset,
                ),
            )
        }
    } else {
        val opticalInset = if (spec.growDirection > 0) spec.compactHeight * OPTICAL_INSET_RATIO else 0.dp
        val fixed = spec.cameraGap + opticalInset + spec.cameraSlotWidth + spec.cellSize + spec.cellSpacing * 2
        val maxText = (spec.lineWidth - startInset - endInset - fixed).coerceAtLeast(0.dp)
        Row(
            modifier = Modifier
                .height(spec.compactHeight)
                .padding(start = spec.cameraGap, end = spec.cameraGap + opticalInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spec.cellSpacing),
        ) {
            if (spec.growDirection > 0) Spacer(Modifier.width(spec.cameraSlotWidth - spec.cameraGap))
            icon()
            title(natural.coerceAtMost(maxText))
            if (spec.growDirection < 0) Spacer(Modifier.width(spec.cameraSlotWidth - spec.cameraGap))
        }
    }
}

@Composable
private fun LineText(text: String, align: TextAlign, weight: FontWeight, modifier: Modifier) {
    MarqueeText(text = text, style = IslandTextStyles.line.copy(fontWeight = weight), textAlign = align, modifier = modifier)
}

@OptIn(ExperimentalTextApi::class)
private fun flexFont(weight: Int) = Font(
    R.font.google_sans_flex,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("ROND", 100f)),
)

val IslandFontFamily = FontFamily(flexFont(400), flexFont(500), flexFont(600), flexFont(700))

object IslandTextStyles {
    val line = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = IslandFontFamily, fontWeight = FontWeight.Medium)
    val compact = TextStyle(
        color = Color.White,
        fontSize = 14.sp,
        fontFamily = IslandFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    )
    val title = TextStyle(color = Color.White, fontSize = 15.sp, fontFamily = IslandFontFamily, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp, fontFamily = IslandFontFamily)
}
