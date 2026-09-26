package com.sameerasw.essentials.island.ui.components

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
    val shape = if (circle) CircleShape else RoundedCornerShape(percent = 28)
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
            bmp != null -> Image(
                bitmap = remember(bmp) { bmp.asImageBitmap() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            fallbackRes != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(painterResource(fallbackRes), null, tint = Color.White, modifier = Modifier.fillMaxSize(0.8f))
            }
            else -> Box(Modifier.fillMaxSize())
        }
    }
}

@Composable
fun IslandIcon(res: Int, tint: Color = Color.White, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Icon(painterResource(res), contentDescription = null, tint = tint, modifier = modifier.squareFit(size))
}

@Composable
fun EqualizerBars(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
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
                color = color,
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
    Box(modifier.squareFit(size), contentAlignment = Alignment.Center) {
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
        if (showLevel) {
            Text(
                text = level.coerceIn(0, 100).toString(),
                style = IslandTextStyles.compact.copy(fontSize = 9.sp, lineHeight = 9.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
    }
}

@Composable
fun BatteryGlyph(level: Int, color: Color, modifier: Modifier = Modifier, showLevel: Boolean = true) {
    val fraction by animateFloatAsState(level.coerceIn(0, 100) / 100f, IslandMotion.float(), label = "batteryFill")
    val fillColor by animateColorAsState(color, label = "batteryGlyphColor")
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
