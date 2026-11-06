package com.sameerasw.essentials.island.ui

import com.sameerasw.essentials.island.ui.components.MarqueeText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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

class LineLeading(val width: Dp, val content: @Composable () -> Unit)

@Composable
fun LineTemplate(
    line: LineContent,
    spec: IslandLayoutSpec,
    startInset: Dp = 0.dp,
    endInset: Dp = 0.dp,
    leading: LineLeading? = null,
) {
    if (line.condensed) {
        CondensedLine(line, spec, startInset, endInset, leading)
        return
    }
    val leadingSlot: @Composable () -> Unit = { LeadingSlot(leading, spec) }
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
                leadingSlot()
                if (line.showLeftIcon) icon()
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
            if (spec.growDirection > 0) Spacer(Modifier.width((spec.cameraSlotWidth - spec.cameraGap).coerceAtLeast(0.dp)))
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spec.cellSpacing),
            ) {
                leadingSlot()
                if (line.showLeftIcon) icon()
                MarqueeText(
                    text = mergedLineText(line),
                    style = IslandTextStyles.line,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )
                endSlot()
            }
            if (spec.growDirection < 0) Spacer(Modifier.width((spec.cameraSlotWidth - spec.cameraGap).coerceAtLeast(0.dp)))
        }
    }
}

internal fun condensedTitleWidth(
    natural: Dp,
    lineWidth: Dp,
    cameraSlotWidth: Dp,
    cellSize: Dp,
    edgePad: Dp,
    queueInset: Dp,
): Dp {
    val roomBesideCamera = ((lineWidth - edgePad * 2 - cameraSlotWidth) / 2 - queueInset).coerceAtLeast(cellSize)
    return natural.coerceIn(cellSize, roomBesideCamera)
}

@Composable
private fun CondensedLine(line: LineContent, spec: IslandLayoutSpec, startInset: Dp, endInset: Dp, leading: LineLeading?) {
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
    val leadingSlot: @Composable () -> Unit = { LeadingSlot(leading, spec) }
    val leadingWidth = leading?.let { it.width + spec.cellSpacing } ?: 0.dp

    if (spec.growDirection == 0) {
        val edgePad = spec.cameraGap + spec.compactHeight * OPTICAL_INSET_RATIO
        val titleWidth = condensedTitleWidth(
            natural = natural,
            lineWidth = spec.lineWidth,
            cameraSlotWidth = spec.cameraSlotWidth,
            cellSize = spec.cellSize,
            edgePad = edgePad,
            queueInset = maxOf(startInset, endInset),
        )
        val side = maxOf(titleWidth, spec.cellSize + leadingWidth) + edgePad
        Row(
            modifier = Modifier.height(spec.compactHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.width(side).padding(start = edgePad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spec.cellSpacing),
            ) {
                leadingSlot()
                icon()
            }
            Spacer(Modifier.width(spec.cameraSlotWidth))
            Box(
                modifier = Modifier.width(side).padding(end = edgePad),
                contentAlignment = Alignment.CenterEnd,
            ) { title(titleWidth) }
        }
    } else {
        val opticalInset = if (spec.growDirection > 0) spec.compactHeight * OPTICAL_INSET_RATIO else 0.dp
        val fixed = spec.cameraGap + opticalInset + spec.cameraSlotWidth + spec.cellSize + spec.cellSpacing * 2 + leadingWidth
        val maxText = (spec.lineWidth - startInset - endInset - fixed).coerceAtLeast(0.dp)
        Row(
            modifier = Modifier
                .height(spec.compactHeight)
                .padding(start = spec.cameraGap, end = spec.cameraGap + opticalInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spec.cellSpacing),
        ) {
            if (spec.growDirection > 0) Spacer(Modifier.width((spec.cameraSlotWidth - spec.cameraGap).coerceAtLeast(0.dp)))
            leadingSlot()
            icon()
            title(natural.coerceAtMost(maxText))
            if (spec.growDirection < 0) Spacer(Modifier.width((spec.cameraSlotWidth - spec.cameraGap).coerceAtLeast(0.dp)))
        }
    }
}

@Composable
private fun LeadingSlot(leading: LineLeading?, spec: IslandLayoutSpec) {
    var last by remember { mutableStateOf(leading) }
    if (leading != null) last = leading
    val width by animateDpAsState(last?.width ?: 0.dp, IslandMotion.smoothSpatial(), label = "lineLeadingWidth")
    AnimatedVisibility(
        visible = leading != null,
        enter = expandHorizontally(IslandMotion.smoothSpatial()) +
            scaleIn(IslandMotion.expressiveFastSpatial(), initialScale = 0.6f) +
            fadeIn(IslandMotion.expressiveDefaultEffects()),
        exit = shrinkHorizontally(IslandMotion.expressiveFastEffects()) +
            scaleOut(IslandMotion.expressiveFastEffects(), targetScale = 0.6f) +
            fadeOut(IslandMotion.expressiveFastEffects()),
    ) {
        Box(Modifier.width(width).height(spec.cellSize), contentAlignment = Alignment.Center) { last?.content?.invoke() }
    }
}

private fun mergedLineText(line: LineContent): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(line.start) }
    if (line.end.isNotBlank()) {
        if (line.start.isNotBlank()) append("  ·  ")
        withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append(line.end) }
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

val LocalIslandTextColor = compositionLocalOf<Color?> { null }

@Composable
fun islandTextColor(alpha: Float = 1f): Color = (LocalIslandTextColor.current ?: Color.White).let { it.copy(alpha = it.alpha * alpha) }

fun pastelOf(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = minOf(hsv[1], 0.4f)
    hsv[2] = 1f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

object IslandTextStyles {
    val line: TextStyle
        @Composable get() = TextStyle(color = islandTextColor(), fontSize = 13.sp, fontFamily = IslandFontFamily, fontWeight = FontWeight.Medium)
    val compact: TextStyle
        @Composable get() = TextStyle(
            color = islandTextColor(),
            fontSize = 14.sp,
            fontFamily = IslandFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum",
        )
    val title: TextStyle
        @Composable get() = TextStyle(color = islandTextColor(), fontSize = 15.sp, fontFamily = IslandFontFamily, fontWeight = FontWeight.SemiBold)
    val body: TextStyle
        @Composable get() = TextStyle(color = islandTextColor(0.72f), fontSize = 13.sp, fontFamily = IslandFontFamily)
}
