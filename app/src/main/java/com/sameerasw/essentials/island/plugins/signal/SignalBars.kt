package com.sameerasw.essentials.island.plugins.signal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.island.ui.IslandTextStyles

private const val LABEL_SHIFT_X = 0.15f
private const val LABEL_SHIFT_Y = 0.15f
private const val WIFI_HALF_ANGLE = 45f
private const val WIFI_HEIGHT = 400f
private const val WIFI_DOT_Y = 349f
private const val WIFI_DOT_RADIUS = 50.5f
private const val WIFI_STROKE = 59f
private val WIFI_ARC_RADII = floatArrayOf(126f, 223f, 320f)

@Composable
fun SignalBars(level: Int, color: Color, label: String?, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = IslandTextStyles.compact.copy(
        fontSize = 7.sp,
        lineHeight = 7.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    Canvas(modifier.size(width = 16.dp, height = 14.dp)) {
        val bars = 4
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val barHeight = size.height * (i + 1) / bars
            drawRoundRect(
                color = if (i < level) color else Color.White.copy(alpha = 0.3f),
                topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
        if (label != null) {
            drawText(measurer, label, topLeft = Offset(-size.width * LABEL_SHIFT_X, -size.height * LABEL_SHIFT_Y), style = labelStyle)
        }
    }
}

@Composable
fun SignalIndicator(wifiLevel: Int?, mobileLevel: Int, color: Color, label: String?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = wifiLevel,
        transitionSpec = {
            (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.6f)) togetherWith
                (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.6f))
        },
        modifier = modifier,
        label = "signalIndicator",
    ) { wifi ->
        if (wifi != null) WifiBars(wifi, color) else SignalBars(mobileLevel, color, label)
    }
}

@Composable
fun WifiBars(level: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 18.dp, height = 14.dp)) {
        val unit = size.height / WIFI_HEIGHT
        val center = Offset(size.width / 2f, WIFI_DOT_Y * unit)
        val dim = Color.White.copy(alpha = 0.3f)
        drawCircle(if (level >= 1) color else dim, WIFI_DOT_RADIUS * unit, center)
        WIFI_ARC_RADII.forEachIndexed { index, arcRadius ->
            val radius = arcRadius * unit
            drawArc(
                color = if (level >= index + 2) color else dim,
                startAngle = -90f - WIFI_HALF_ANGLE,
                sweepAngle = WIFI_HALF_ANGLE * 2f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = WIFI_STROKE * unit, cap = StrokeCap.Round),
            )
        }
    }
}
