package com.sameerasw.essentials.island.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import com.sameerasw.essentials.utils.AppIconUtil
import com.sameerasw.essentials.island.ui.squareFit
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.ui.IslandTextStyles
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import com.sameerasw.essentials.island.ui.IslandMotion
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun IslandBitmap(
    bitmap: Bitmap?,
    size: Dp,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    fallbackRes: Int? = null,
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
                    colorFilter = when (themedMode) {
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
fun EqualizerBars(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val barColor = if (LocalIslandChipInk.current != null) Color.Black else color
    val levels = remember { mutableStateListOf(0.16f, 0.16f, 0.16f) }
    LaunchedEffect(playing) {
        if (!playing) {
            for (i in levels.indices) levels[i] = 0.16f
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
    val ringColor by animateColorAsState(resolveIconTint(color), label = "batteryColor")
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
fun BatteryGlyph(level: Int, color: Color, modifier: Modifier = Modifier, showLevel: Boolean = true) {
    val fraction by animateFloatAsState(level.coerceIn(0, 100) / 100f, IslandMotion.float(), label = "batteryFill")
    val fillColor by animateColorAsState(resolveIconTint(color), label = "batteryGlyphColor")
    val onFill = if (fillColor.luminance() > 0.5f) Color.Black else Color.White
    val textStyle = IslandTextStyles.compact.copy(fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold)
    val label = level.coerceIn(0, 100).toString()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .sizeIn(maxWidth = 28.dp, maxHeight = 15.dp)
                .aspectRatio(28f / 15f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(30))
                .drawBehind {
                    drawRect(Color.White.copy(alpha = 0.3f))
                    drawRect(fillColor, size = Size(size.width * fraction, size.height))
                },
            contentAlignment = Alignment.Center,
        ) {
            if (showLevel) Text(
                label,
                style = textStyle.copy(color = Color.White),
                maxLines = 1,
                modifier = Modifier.drawWithContent {
                    val split = (size.width / 2f) + (fraction - 0.5f) * 28.dp.toPx()
                    clipRect(left = split) { this@drawWithContent.drawContent() }
                },
            )
            if (showLevel) Text(
                label,
                style = textStyle.copy(color = onFill),
                maxLines = 1,
                modifier = Modifier.drawWithContent {
                    val split = (size.width / 2f) + (fraction - 0.5f) * 28.dp.toPx()
                    clipRect(right = split) { this@drawWithContent.drawContent() }
                },
            )
        }
        Canvas(Modifier.size(width = 2.5.dp, height = 6.dp)) {
            drawRoundRect(
                color = if (fraction >= 0.99f) fillColor else Color.White.copy(alpha = 0.3f),
                topLeft = Offset(size.width * 0.2f, 0f),
                size = Size(size.width * 0.8f, size.height),
                cornerRadius = CornerRadius(size.width, size.width),
            )
        }
    }
}
