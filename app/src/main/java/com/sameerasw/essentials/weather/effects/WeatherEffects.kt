package com.sameerasw.essentials.weather.effects

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

interface WeatherEffectHaptics {
    // weight is 0..1 per drop so the pattern doesn't feel like a metronome.
    fun onDrop(intensity: Float, weight: Float)
    fun onHail(intensity: Float, weight: Float)
    fun onStrike(intensity: Float)
}

// Each particle is a pure function of its seed values and elapsed time, so there's no per-frame state to update.
private class ParticleField(val x: FloatArray, val phase: FloatArray, val speed: FloatArray, val size: FloatArray) {
    val count: Int get() = x.size

    // A random spread of particles that land with a splash and a haptic tick; mixed speeds keep the rhythm irregular.
    fun heroes(count: Int, seed: Int): IntArray = size.indices.shuffled(Random(seed)).take(count).toIntArray()

    companion object {
        fun create(count: Int, seed: Int): ParticleField {
            val random = Random(seed)
            fun values() = FloatArray(count) { random.nextFloat() }
            return ParticleField(values(), values(), values(), values())
        }
    }
}

private class LayerState(val layer: WeatherEffectLayer, val field: ParticleField, val heroes: IntArray)

@Composable
fun WeatherEffects(
    spec: WeatherEffectSpec,
    modifier: Modifier = Modifier,
    strength: Float = 1f,
    clearTop: Dp = 0.dp,
    haptics: WeatherEffectHaptics? = null,
) {
    if (spec.isEmpty) return
    val density = LocalDensity.current
    val time = remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val currentHaptics by rememberUpdatedState(haptics)
    val layers = remember(spec) {
        spec.layers.mapIndexed { index, layer ->
            val field = ParticleField.create(particleCount(layer), seed = index * 7919 + 17)
            LayerState(layer, field, field.heroes(heroCount(layer), seed = index * 31 + 5))
        }
    }

    LaunchedEffect(layers) {
        val start = withFrameNanos { it }
        var previous = 0f
        while (true) {
            val now = withFrameNanos { (it - start) / 1_000_000_000f }
            time.floatValue = now
            val h = canvasSize.height.toFloat()
            currentHaptics?.let { sink ->
                if (h > 0f && now > previous) emitHaptics(sink, layers, previous, now, h, density)
            }
            previous = now
        }
    }

    Canvas(
        modifier
            .onSizeChanged { canvasSize = it }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val top = clearTop.toPx().coerceAtMost(size.height)
                if (top <= 0f) return@drawWithContent
                // Nothing may show above the clearance line, so the camera cutout never gets lit.
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
        layers.forEach { state ->
            when (val layer = state.layer) {
                is WeatherEffectLayer.Rain -> drawRain(state, layer, t, strength)
                is WeatherEffectLayer.Snow -> drawSnow(state.field, t, layer.intensity, strength)
                is WeatherEffectLayer.Hail -> drawHail(state, layer.intensity, t, strength)
                is WeatherEffectLayer.Clouds -> drawClouds(state.field, t, layer.intensity, strength, fog = false)
                is WeatherEffectLayer.Fog -> drawClouds(state.field, t, layer.intensity, strength, fog = true)
                is WeatherEffectLayer.SunGlow -> drawSunGlow(t, layer.intensity, strength)
                is WeatherEffectLayer.Stars -> drawStars(state.field, t, layer.intensity, strength)
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

private fun heroCount(layer: WeatherEffectLayer): Int = when (layer) {
    is WeatherEffectLayer.Rain -> 2 + (6 * layer.intensity).roundToInt()
    is WeatherEffectLayer.Hail -> 2 + (3 * layer.intensity).roundToInt()
    else -> 0
}

// Shared geometry: the draw pass and the haptic pass must agree on exactly when a drop lands.
private class Fall(val cyclesPerSecond: Float, val span: Float, val impact: Float)

private fun rainLength(field: ParticleField, i: Int, intensity: Float, density: Density): Float =
    with(density) { (12.dp.toPx() + 14.dp.toPx() * intensity) * (0.6f + 0.4f * field.size[i]) }

private fun rainFall(field: ParticleField, i: Int, intensity: Float, h: Float, density: Density): Fall {
    val length = rainLength(field, i, intensity, density)
    val span = h + length
    val cycles = (1.4f + 0.9f * field.speed[i]) * (0.8f + 0.4f * intensity) * h / span
    return Fall(cycles, span, h / span)
}

private fun hailRadius(field: ParticleField, i: Int, density: Density): Float =
    with(density) { 1.6.dp.toPx() * (0.8f + 0.5f * field.size[i]) }

private fun hailFall(field: ParticleField, i: Int, h: Float, density: Density): Fall {
    val radius = hailRadius(field, i, density)
    val span = h + radius * 2
    return Fall(1.8f + field.speed[i], span, h / span)
}

private fun progress(field: ParticleField, i: Int, fall: Fall, t: Float): Float = (field.phase[i] + t * fall.cyclesPerSecond) % 1f

private fun crossed(previous: Float, current: Float, threshold: Float): Boolean =
    if (current >= previous) threshold in previous..current && threshold != previous
    else previous < threshold || current >= threshold

private fun lightningStrike(cycle: Int): Float = cycle * LIGHTNING_CYCLE_S + 1f + hash(cycle) * (LIGHTNING_CYCLE_S - 2f)

private fun emitHaptics(
    sink: WeatherEffectHaptics,
    layers: List<LayerState>,
    previous: Float,
    now: Float,
    h: Float,
    density: Density,
) {
    layers.forEach { state ->
        when (val layer = state.layer) {
            is WeatherEffectLayer.Rain -> state.heroes.forEach { i ->
                val fall = rainFall(state.field, i, layer.intensity, h, density)
                if (crossed(progress(state.field, i, fall, previous), progress(state.field, i, fall, now), fall.impact)) {
                    sink.onDrop(layer.intensity, state.field.size[i])
                }
            }
            is WeatherEffectLayer.Hail -> state.heroes.forEach { i ->
                val fall = hailFall(state.field, i, h, density)
                if (crossed(progress(state.field, i, fall, previous), progress(state.field, i, fall, now), fall.impact)) {
                    sink.onHail(layer.intensity, state.field.size[i])
                }
            }
            is WeatherEffectLayer.Lightning -> {
                val first = floor(previous / LIGHTNING_CYCLE_S).toInt()
                val last = floor(now / LIGHTNING_CYCLE_S).toInt()
                for (cycle in first..last) {
                    val strike = lightningStrike(cycle)
                    if (strike > previous && strike <= now) sink.onStrike(layer.intensity)
                }
            }
            else -> Unit
        }
    }
}

private fun DrawScope.drawRain(state: LayerState, layer: WeatherEffectLayer.Rain, t: Float, strength: Float) {
    val field = state.field
    val w = size.width
    val h = size.height
    val stroke = 1.2.dp.toPx()
    for (i in 0 until field.count) {
        val length = rainLength(field, i, layer.intensity, this)
        val fall = rainFall(field, i, layer.intensity, h, this)
        val y = progress(field, i, fall, t) * fall.span - length
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
    state.heroes.forEach { i ->
        val fall = rainFall(field, i, layer.intensity, h, this)
        val length = rainLength(field, i, layer.intensity, this)
        val impactX = wrap(field.x[i] * w + layer.slant * (h - length), w) + layer.slant * length
        drawSplash(sinceImpact(progress(field, i, fall, t), fall), impactX, h, (0.2f + 0.25f * layer.intensity) * strength)
    }
}

private fun DrawScope.drawHail(state: LayerState, intensity: Float, t: Float, strength: Float) {
    val field = state.field
    val w = size.width
    val h = size.height
    for (i in 0 until field.count) {
        val radius = hailRadius(field, i, this)
        val fall = hailFall(field, i, h, this)
        val y = progress(field, i, fall, t) * fall.span - radius
        drawCircle(Color.White.copy(alpha = (0.3f + 0.3f * intensity) * strength), radius, Offset(field.x[i] * w, y))
    }
    state.heroes.forEach { i ->
        val fall = hailFall(field, i, h, this)
        drawSplash(sinceImpact(progress(field, i, fall, t), fall), field.x[i] * w, h, (0.3f + 0.3f * intensity) * strength)
    }
}

private fun sinceImpact(progress: Float, fall: Fall): Float {
    val cycles = if (progress >= fall.impact) progress - fall.impact else progress + 1f - fall.impact
    return cycles / fall.cyclesPerSecond
}

private fun DrawScope.drawSplash(sinceImpact: Float, x: Float, h: Float, alpha: Float) {
    if (sinceImpact > SPLASH_S) return
    val f = sinceImpact / SPLASH_S
    drawCircle(
        color = Color.White.copy(alpha = alpha * (1f - f)),
        radius = 1.dp.toPx() + 6.dp.toPx() * f,
        center = Offset(x, h - 2.dp.toPx()),
        style = Stroke(width = 1.dp.toPx()),
    )
}

private fun DrawScope.drawSnow(field: ParticleField, t: Float, intensity: Float, strength: Float) {
    val w = size.width
    val h = size.height
    val sway = 10.dp.toPx()
    for (i in 0 until field.count) {
        val radius = 1.4.dp.toPx() + 1.8.dp.toPx() * field.size[i]
        val span = h + radius * 2
        val p = (field.phase[i] + t * (0.08f + 0.1f * field.speed[i])) % 1f
        val y = p * span - radius
        val x = wrap(field.x[i] * w + sin(t * 0.8f + field.phase[i] * TWO_PI) * sway, w)
        drawCircle(Color.White.copy(alpha = (0.25f + 0.35f * intensity) * strength), radius, Offset(x, y))
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
    val local = t - lightningStrike(cycle)
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
private const val SPLASH_S = 0.28f
private val SUN_COLOR = Color(0xFFFFD27A)
private val CLEAR_FADE = 32.dp
