package com.sameerasw.essentials.island.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.state.IslandStillness
import com.sameerasw.essentials.island.ui.IslandFontFamily
import com.sameerasw.essentials.island.ui.IslandMotion
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.motionTween
import com.sameerasw.essentials.island.ui.squareFit
import com.sameerasw.essentials.utils.AppIconUtil
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun NotificationGlyph(
    themedIcon: Bitmap?,
    smallIcon: Bitmap?,
    color: Color?,
    size: Dp,
    modifier: Modifier = Modifier,
    fallbackRes: Int,
) {
    val background = color ?: MaterialTheme.colorScheme.primary
    val glyph = lerp(background, if (background.luminance() > 0.179f) Color.Black else Color.White, 0.8f)
    Box(
        modifier
            .sizeIn(maxWidth = size, maxHeight = size)
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        val icon = themedIcon ?: smallIcon
        if (icon != null) {
            Image(
                bitmap = remember(icon) { icon.asImageBitmap() },
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(glyph),
                modifier = Modifier.fillMaxSize(if (themedIcon != null) 1f else 0.6f),
            )
        } else {
            Icon(painterResource(fallbackRes), null, tint = glyph, modifier = Modifier.fillMaxSize(0.6f))
        }
    }
}

@Composable
fun IslandBitmap(
    bitmap: Bitmap?,
    size: Dp,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    fallbackRes: Int? = null,
    tint: Color? = null,
) {
    val shape = when {
        LocalIslandBitmapFlat.current -> RectangleShape
        circle -> CircleShape
        else -> RoundedCornerShape(percent = 28)
    }
    AnimatedContent(
        targetState = bitmap,
        transitionSpec = { fadeIn(IslandMotion.contentIn()) togetherWith fadeOut(IslandMotion.contentOut()) },
        label = "islandBitmap",
        modifier = modifier
            .sizeIn(maxWidth = size, maxHeight = size)
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .clip(shape),
    ) { bmp ->
        when {
            bmp != null -> {
                val iconColor = LocalIslandIconColor.current ?: MaterialTheme.colorScheme.primary
                val themedMode = remember(bmp) { AppIconUtil.themedMode(bmp) }
                Image(
                    bitmap = remember(bmp) { bmp.asImageBitmap() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = if (tint != null) ColorFilter.tint(tint, BlendMode.SrcIn) else when (themedMode) {
                        AppIconUtil.ThemedMode.MONO -> ColorFilter.tint(iconColor, BlendMode.SrcIn)
                        AppIconUtil.ThemedMode.DUO -> duotoneFilter(iconColor)
                        null -> null
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            fallbackRes != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(painterResource(fallbackRes), null, tint = LocalIslandChipInk.current ?: Color.White, modifier = Modifier.fillMaxSize(0.8f))
            }
            else -> Box(Modifier.fillMaxSize())
        }
    }
}

private fun duotoneFilter(accent: Color): ColorFilter {
    val dark = lerp(accent, Color.Black, 0.6f)
    val light = lerp(accent, Color.White, 0.45f)
    fun row(low: Float, high: Float) = floatArrayOf((high - low), (high - low), (high - low), 0f, low * 255f)
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                *row(dark.red, light.red).also { it[1] = 0f; it[2] = 0f },
                *row(dark.green, light.green).also { it[0] = 0f; it[2] = 0f },
                *row(dark.blue, light.blue).also { it[0] = 0f; it[1] = 0f },
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}

val LocalIslandBitmapFlat = compositionLocalOf { false }

val LocalIslandChipInk = compositionLocalOf<Color?> { null }

val LocalIslandIconColor = compositionLocalOf<Color?> { null }

@Composable
fun islandAccent(): Color = LocalIslandIconColor.current ?: MaterialTheme.colorScheme.primary

@Composable
fun resolveIconTint(tint: Color): Color {
    val override = LocalIslandIconColor.current ?: return tint
    if (tint.red == 0f && tint.green == 0f && tint.blue == 0f) return tint
    return override.copy(alpha = override.alpha * tint.alpha)
}

@Composable
fun IslandIcon(res: Int, tint: Color = Color.White, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Icon(painterResource(res), contentDescription = null, tint = resolveIconTint(tint), modifier = modifier.squareFit(size))
}

@Composable
fun islandStill(): Boolean = IslandStillness.still.collectAsState().value

@Composable
fun rememberLoopValue(active: Boolean, target: Float, durationMs: Int): Float {
    val value = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        while (true) {
            value.animateTo(target, motionTween(((1f - value.value / target) * durationMs).toInt().coerceAtLeast(1), easing = LinearEasing))
            value.snapTo(0f)
        }
    }
    return value.value
}

@Composable
fun EqualizerBars(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    outline: Color? = null,
) {
    val barColor = if (LocalIslandChipInk.current != null) Color.Black else color
    val levels = remember { mutableStateListOf(0.16f, 0.16f, 0.16f) }
    val still = islandStill()
    LaunchedEffect(playing, still) {
        if (!playing) {
            for (i in levels.indices) levels[i] = 0.16f
            return@LaunchedEffect
        }
        if (still) {
            levels[0] = 0.5f
            levels[1] = 0.85f
            levels[2] = 0.65f
            return@LaunchedEffect
        }
        val random = Random(System.nanoTime())
        while (true) {
            for (i in levels.indices) levels[i] = 0.15f + random.nextFloat() * 0.85f
            delay(180L + random.nextLong(160L))
        }
    }
    val animated = levels.mapIndexed { i, level ->
        animateFloatAsState(level, IslandMotion.float(), label = "eq$i").value
    }
    Canvas(modifier.squareFit(size)) {
        val barWidth = this.size.width / 5f
        animated.forEachIndexed { i, level ->
            val h = this.size.height * level
            if (outline != null) {
                val pad = barWidth * 0.3f
                drawRoundRect(
                    color = outline,
                    topLeft = Offset(barWidth * (i * 2f) - pad, (this.size.height - h) / 2f - pad),
                    size = Size(barWidth + pad * 2f, h + pad * 2f),
                    cornerRadius = CornerRadius(barWidth / 2f + pad),
                )
            }
            drawRoundRect(
                color = barColor,
                topLeft = Offset(barWidth * (i * 2f), (this.size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
fun BatteryRing(level: Int, color: Color, modifier: Modifier = Modifier, showLevel: Boolean = false, size: Dp = if (showLevel) 24.dp else 18.dp) {
    val sweep by animateFloatAsState(360f * level.coerceIn(0, 100) / 100f, IslandMotion.float(), label = "batterySweep")
    val ringColor by animateColorAsState(color, label = "batteryColor")
    var ringPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Box(modifier.squareFit(size).onSizeChanged { ringPx = minOf(it.width, it.height) }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val diameter = this.size.minDimension
            val stroke = diameter * (if (showLevel) 0.12f else 0.16f)
            val topLeft = Offset((this.size.width - diameter + stroke) / 2f, (this.size.height - diameter + stroke) / 2f)
            val arcSize = Size(diameter - stroke, diameter - stroke)
            drawArc(Color.White.copy(alpha = 0.25f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke * 0.5f))
            if (sweep > 0.5f) {
                drawArc(ringColor, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        if (showLevel && ringPx > 0) {
            val label = level.coerceIn(0, 100).toString()
            val innerPx = ringPx * (1f - 2f * 0.12f) * 0.8f
            val fontPx = minOf(innerPx / (label.length * 0.62f), innerPx * 0.75f)
            val fontSp = with(density) { fontPx.toSp() }
            Text(
                text = label,
                style = IslandTextStyles.compact.copy(fontSize = fontSp, lineHeight = fontSp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}


@Composable
fun BatteryGlyph(
    level: Int,
    color: Color,
    modifier: Modifier = Modifier,
    showLevel: Boolean = true,
    charging: Boolean = false,
    powerSave: Boolean = false,
) {
    val fraction by animateFloatAsState(level.coerceIn(0, 100) / 100f, IslandMotion.float(), label = "batteryFill")
    val fillColor by animateColorAsState(color, label = "batteryGlyphColor")
    val labelColor = if (fillColor.luminance() > 0.4f) lerp(fillColor, Color.Black, 0.82f) else Color.White
    val label = level.coerceIn(0, 100).toString()
    val fontResolver = LocalFontFamilyResolver.current
    val typeface = remember(fontResolver) {
        fontResolver.resolve(IslandFontFamily, FontWeight.Bold).value as? android.graphics.Typeface
    }
    val paint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
    val bounds = remember { android.graphics.Rect() }
    val symbol = when {
        charging -> 1
        powerSave -> 2
        else -> 0
    }
    Row(
        modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .sizeIn(maxWidth = 28.dp, maxHeight = 15.dp)
                .aspectRatio(28f / 15f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(32))
                .drawBehind {
                    drawRect(Color.White.copy(alpha = 0.45f))
                    drawRect(fillColor, size = Size(size.width * fraction, size.height))
                    if (!showLevel) return@drawBehind
                    paint.typeface = typeface
                    paint.color = labelColor.toArgb()
                    paint.textSize = size.height * (if (label.length > 2) 0.82f else 0.94f)
                    paint.letterSpacing = -0.04f
                    paint.getTextBounds(label, 0, label.length, bounds)
                    val textWidth = paint.measureText(label)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        size.width / 2f - textWidth / 2f,
                        size.height / 2f - bounds.exactCenterY(),
                        paint,
                    )
                },
        )
        Canvas(Modifier.size(width = 2.5.dp, height = 6.dp)) {
            drawRoundRect(
                color = if (fraction >= 0.99f) fillColor else Color.White.copy(alpha = 0.45f),
                topLeft = Offset(size.width * 0.2f, 0f),
                size = Size(size.width * 0.8f, size.height),
                cornerRadius = CornerRadius(size.width, size.width),
            )
        }
        if (symbol != 0) {
            Canvas(
                Modifier
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val overlap = 7.dp.roundToPx()
                        layout((placeable.width - overlap).coerceAtLeast(0), placeable.height) { placeable.place(-overlap, 0) }
                    }
                    .size(11.dp, 13.dp),
            ) {
                val stroke = 1.8.dp.toPx()
                val gap = 0.8.dp.toPx()
                if (symbol == 2) {
                    val arm = size.width * 0.42f
                    val c = center
                    for ((width, paintColor, mode) in listOf(
                        Triple(stroke + gap * 2, Color.Black, BlendMode.Clear),
                        Triple(stroke, Color.White, BlendMode.SrcOver),
                    )) {
                        drawLine(paintColor, Offset(c.x - arm, c.y), Offset(c.x + arm, c.y), width, StrokeCap.Round, blendMode = mode)
                        drawLine(paintColor, Offset(c.x, c.y - arm), Offset(c.x, c.y + arm), width, StrokeCap.Round, blendMode = mode)
                    }
                } else {
                    val w = size.width
                    val h = size.height
                    val bolt = Path().apply {
                        moveTo(w * 0.62f, 0f)
                        lineTo(w * 0.08f, h * 0.58f)
                        lineTo(w * 0.46f, h * 0.58f)
                        lineTo(w * 0.36f, h)
                        lineTo(w * 0.92f, h * 0.40f)
                        lineTo(w * 0.54f, h * 0.40f)
                        close()
                    }
                    drawPath(bolt, Color.Black, style = Stroke(gap * 2, join = StrokeJoin.Round), blendMode = BlendMode.Clear)
                    drawPath(bolt, Color.White, style = Stroke(stroke * 0.3f, join = StrokeJoin.Round))
                    drawPath(bolt, Color.White)
                }
            }
        }
    }
}
