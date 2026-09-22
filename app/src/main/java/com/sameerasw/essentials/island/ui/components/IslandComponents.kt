package com.sameerasw.essentials.island.ui.components

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
    val shape = if (circle) CircleShape else RoundedCornerShape(size * 0.28f)
    AnimatedContent(
        targetState = bitmap,
        transitionSpec = { fadeIn(IslandMotion.contentIn()) togetherWith fadeOut(IslandMotion.contentOut()) },
        label = "islandBitmap",
        modifier = modifier.size(size).clip(shape),
    ) { bmp ->
        when {
            bmp != null -> Image(
                bitmap = remember(bmp) { bmp.asImageBitmap() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
            fallbackRes != null -> Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                Icon(painterResource(fallbackRes), null, tint = Color.White, modifier = Modifier.size(size * 0.8f))
            }
            else -> Box(Modifier.size(size))
        }
    }
}

@Composable
fun IslandIcon(res: Int, tint: Color = Color.White, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Icon(painterResource(res), contentDescription = null, tint = tint, modifier = modifier.size(size))
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
    Canvas(modifier.size(size)) {
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
fun BatteryRing(level: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    val sweep by animateFloatAsState(360f * level.coerceIn(0, 100) / 100f, IslandMotion.float(), label = "batterySweep")
    val ringColor by animateColorAsState(color, label = "batteryColor")
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 0.16f
        val inset = stroke / 2f
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(Color.White.copy(alpha = 0.25f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke * 0.5f))
        if (sweep > 0.5f) {
            drawArc(ringColor, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}
