package com.sameerasw.essentials.weather.effects

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// Each particle is a pure function of its seed values and elapsed time, so there's no per-frame state to update.
private class ParticleField(val x: FloatArray, val phase: FloatArray, val speed: FloatArray, val size: FloatArray) {
    val count: Int get() = x.size

    companion object {
        fun create(count: Int, seed: Int): ParticleField {
            val random = Random(seed)
            fun values() = FloatArray(count) { random.nextFloat() }
            return ParticleField(values(), values(), values(), values())
        }
    }
}

@Composable
fun WeatherEffects(
    spec: WeatherEffectSpec,
    modifier: Modifier = Modifier,
    strength: Float = 1f,
    clearTop: Dp = 0.dp,
) {
    if (spec.isEmpty) return
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { time.floatValue = (it - start) / 1_000_000_000f }
        }
    }
    val fields = remember(spec) {
        spec.layers.mapIndexed { index, layer -> ParticleField.create(particleCount(layer), seed = index * 7919 + 17) }
    }
    Canvas(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val top = clearTop.toPx().coerceAtMost(size.height)
                if (top <= 0f) return@drawWithContent
                // preserve camera cutout
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black,
                        startY = top,
                        endY = (top + CLEAR_FADE.toPx()).coerceAtMost(size.height),
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        val t = time.floatValue
        spec.layers.forEachIndexed { index, layer ->
            val field = fields[index]
            when (layer) {
                is WeatherEffectLayer.Rain -> drawRain(field, t, layer, strength)
                is WeatherEffectLayer.Snow -> drawSnow(field, t, layer.intensity, strength)
                is WeatherEffectLayer.Hail -> drawHail(field, t, layer.intensity, strength)
                is WeatherEffectLayer.Clouds -> drawClouds(field, t, layer.intensity, strength, fog = false)
                is WeatherEffectLayer.Fog -> drawClouds(field, t, layer.intensity, strength, fog = true)
                is WeatherEffectLayer.SunGlow -> drawSunGlow(t, layer.intensity, strength)
                is WeatherEffectLayer.Stars -> drawStars(field, t, layer.intensity, strength)
                is WeatherEffectLayer.Lightning -> drawLightning(t, layer.intensity, strength)
            }
        }
    }
}

private fun particleCount(layer: WeatherEffectLayer): Int = when (layer) {
    is WeatherEffectLayer.Rain -> (40 + 110 * layer.intensity).toInt()
    is WeatherEffectLayer.Snow -> (20 + 60 * layer.intensity).toInt()
    is WeatherEffectLayer.Hail -> (20 + 50 * layer.intensity).toInt()
    is WeatherEffectLayer.Stars -> (12 + 28 * layer.intensity).toInt()
    is WeatherEffectLayer.Clouds, is WeatherEffectLayer.Fog -> 5
    is WeatherEffectLayer.SunGlow, is WeatherEffectLayer.Lightning -> 0
}

private fun DrawScope.drawRain(field: ParticleField, t: Float, layer: WeatherEffectLayer.Rain, strength: Float) {
    val w = size.width
    val h = size.height
    val baseLength = 12.dp.toPx() + 14.dp.toPx() * layer.intensity
    val stroke = 1.2.dp.toPx()
    for (i in 0 until field.count) {
        val length = baseLength * (0.6f + 0.4f * field.size[i])
        val span = h + length
        val cyclesPerSecond = (1.4f + 0.9f * field.speed[i]) * (0.8f + 0.4f * layer.intensity) * h / span
        val progress = (field.phase[i] + t * cyclesPerSecond) % 1f
        val y = progress * span - length
        val x = wrap(field.x[i] * w + layer.slant * y, w)
        val alpha = (0.12f + 0.2f * layer.intensity) * (0.6f + 0.4f * field.size[i]) * strength
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(x, y),
            end = Offset(x + layer.slant * length, y + length),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSnow(field: ParticleField, t: Float, intensity: Float, strength: Float) {
    val w = size.width
    val h = size.height
    val sway = 10.dp.toPx()
    for (i in 0 until field.count) {
        val radius = 1.4.dp.toPx() + 1.8.dp.toPx() * field.size[i]
        val span = h + radius * 2
        val progress = (field.phase[i] + t * (0.08f + 0.1f * field.speed[i])) % 1f
        val y = progress * span - radius
        val x = wrap(field.x[i] * w + sin(t * 0.8f + field.phase[i] * TWO_PI) * sway, w)
        drawCircle(Color.White.copy(alpha = (0.25f + 0.35f * intensity) * strength), radius, Offset(x, y))
    }
}

private fun DrawScope.drawHail(field: ParticleField, t: Float, intensity: Float, strength: Float) {
    val w = size.width
    val h = size.height
    val radius = 1.6.dp.toPx()
    for (i in 0 until field.count) {
        val progress = (field.phase[i] + t * (1.8f + field.speed[i])) % 1f
        val y = progress * (h + radius * 2) - radius
        drawCircle(Color.White.copy(alpha = (0.3f + 0.3f * intensity) * strength), radius * (0.8f + 0.5f * field.size[i]), Offset(field.x[i] * w, y))
    }
}

private fun DrawScope.drawClouds(field: ParticleField, t: Float, intensity: Float, strength: Float, fog: Boolean) {
    val w = size.width
    val h = size.height
    for (i in 0 until field.count) {
        val drift = (field.x[i] + t * (0.008f + 0.012f * field.speed[i])) % 1f
        val cx = (drift * 1.6f - 0.3f) * w
        val cy = if (fog) (0.15f + 0.8f * field.phase[i]) * h else (0.02f + 0.35f * field.phase[i]) * h
        val radius = (if (fog) 0.55f else 0.35f + 0.25f * field.size[i]) * w
        val alpha = (if (fog) 0.14f else 0.1f) * intensity * strength
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = alpha), Color.Transparent),
                center = Offset(cx, cy),
                radius = radius,
            ),
            radius = radius,
            center = Offset(cx, cy),
        )
    }
}

private fun DrawScope.drawSunGlow(t: Float, intensity: Float, strength: Float) {
    val center = Offset(size.width * 0.85f, 0f)
    val radius = size.width * 0.9f
    val pulse = 0.85f + 0.15f * sin(t * 0.6f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SUN_COLOR.copy(alpha = 0.22f * intensity * pulse * strength), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawStars(field: ParticleField, t: Float, intensity: Float, strength: Float) {
    for (i in 0 until field.count) {
        val twinkle = 0.5f + 0.5f * sin(t * (1f + field.speed[i]) + field.phase[i] * TWO_PI)
        val alpha = (0.15f + 0.45f * twinkle) * intensity * strength
        val radius = 0.7.dp.toPx() + 0.9.dp.toPx() * field.size[i]
        drawCircle(Color.White.copy(alpha = alpha), radius, Offset(field.x[i] * size.width, field.phase[i] * size.height * 0.6f))
    }
}

private fun DrawScope.drawLightning(t: Float, intensity: Float, strength: Float) {
    val cycle = floor(t / LIGHTNING_CYCLE_S).toInt()
    val offset = 1f + hash(cycle) * (LIGHTNING_CYCLE_S - 2f)
    val local = t - cycle * LIGHTNING_CYCLE_S - offset
    if (local !in 0f..0.45f) return
    // Two quick pulses, the second softer, like a real strike.
    val first = (1f - abs(local - 0.05f) / 0.07f).coerceAtLeast(0f)
    val second = (1f - abs(local - 0.25f) / 0.1f).coerceAtLeast(0f) * 0.6f
    val flash = maxOf(first, second)
    if (flash <= 0f) return
    drawRect(Color.White.copy(alpha = 0.28f * flash * intensity * strength))
}

private fun wrap(value: Float, max: Float): Float = ((value % max) + max) % max

private fun hash(n: Int): Float {
    var x = n * 374761393 + 668265263
    x = (x xor (x ushr 13)) * 1274126177
    return ((x xor (x ushr 16)) and 0x7fffffff) / Int.MAX_VALUE.toFloat()
}

private const val TWO_PI = (2 * PI).toFloat()
private const val LIGHTNING_CYCLE_S = 6f
private val SUN_COLOR = Color(0xFFFFD27A)
private val CLEAR_FADE = 32.dp
