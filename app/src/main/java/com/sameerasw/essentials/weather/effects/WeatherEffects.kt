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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
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
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
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

    companion object {
        fun create(count: Int, seed: Int): ParticleField {
            val random = Random(seed)
            fun values() = FloatArray(count) { random.nextFloat() }
            return ParticleField(values(), values(), values(), values())
        }
    }
}

private class LayerState(val layer: WeatherEffectLayer, val field: ParticleField)

@Composable
fun WeatherEffects(
    spec: WeatherEffectSpec,
    modifier: Modifier = Modifier,
    strength: Float = 1f,
    clearTop: Dp = 0.dp,
    haptics: WeatherEffectHaptics? = null,
    surfaces: () -> List<RainSurface> = { emptyList() },
    scrollTick: () -> Int = { 0 },
) {
    if (spec.isEmpty) return
    val density = LocalDensity.current
    val time = remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val currentHaptics by rememberUpdatedState(haptics)
    val currentSurfaces by rememberUpdatedState(surfaces)
    val currentScrollTick by rememberUpdatedState(scrollTick)
    val snowState = remember(spec) { SnowState() }
    val layers = remember(spec) {
        spec.layers.mapIndexed { index, layer ->
            val field = ParticleField.create(particleCount(layer), seed = index * 7919 + 17)
            LayerState(layer, field)
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
            val all = currentSurfaces()
            val tick = currentScrollTick()
            if (tick != snowState.tick) {
                snowState.tick = tick
                snowState.lastMove = now
                dismissPiles(snowState, all, now, density.density)
            }
            snowState.settled = now - snowState.lastMove >= SURFACE_SETTLE_S
            snowState.falling.removeAll { now - it.t0 > FALL_S }
            val snow = layers.firstOrNull { it.layer is WeatherEffectLayer.Snow }
            if (snow != null && snowState.settled && now > previous && canvasSize.width > 0) {
                settleSnow(snowState.piles, all, snow.field, previous, now, canvasSize.width.toFloat(), h, density.density)
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
                is WeatherEffectLayer.Rain -> drawRain(state, layer, t, strength, if (snowState.settled) surfaces() else emptyList())
                is WeatherEffectLayer.Snow -> drawSnow(state.field, t, layer.intensity, strength, if (snowState.settled) surfaces() else emptyList(), snowState)
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

// Every pass of a drop gets its own column and its own roll for landing on a surface, splashing or ticking, so nothing repeats.
private fun cycleOf(field: ParticleField, i: Int, fall: Fall, t: Float): Int = floor(field.phase[i] + t * fall.cyclesPerSecond).toInt()

private fun cycleX(i: Int, cycle: Int): Float = hash(i * 7919 + cycle * 104729 + 3)

private fun roll(i: Int, cycle: Int, salt: Int): Float = hash(i * 31 + cycle * 977 + salt)

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
            is WeatherEffectLayer.Rain -> for (i in 0 until state.field.count) {
                val fall = rainFall(state.field, i, layer.intensity, h, density)
                val before = progress(state.field, i, fall, previous)
                val after = progress(state.field, i, fall, now)
                if (crossed(before, after, fall.impact)) {
                    val cycle = cycleOf(state.field, i, fall, now) - if (after >= fall.impact) 0 else 1
                    if (roll(i, cycle, HERO_SALT) < RAIN_HAPTIC_SHARE) sink.onDrop(layer.intensity, state.field.size[i])
                }
            }
            is WeatherEffectLayer.Hail -> for (i in 0 until state.field.count) {
                val fall = hailFall(state.field, i, h, density)
                val before = progress(state.field, i, fall, previous)
                val after = progress(state.field, i, fall, now)
                if (crossed(before, after, fall.impact)) {
                    val cycle = cycleOf(state.field, i, fall, now) - if (after >= fall.impact) 0 else 1
                    if (roll(i, cycle, HERO_SALT) < HAIL_HAPTIC_SHARE) sink.onHail(layer.intensity, state.field.size[i])
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

class RainSurface(val key: String, val rect: Rect, val cornerRadius: Float, val anchorLeft: Float = rect.left, val anchorWidth: Float = rect.width) {
    fun topAt(x: Float): Float {
        if (x < rect.left || x > rect.right) return Float.MAX_VALUE
        val r = min(cornerRadius, min(rect.width, rect.height) / 2f)
        val dx = if (x < rect.left + r) rect.left + r - x else if (x > rect.right - r) x - (rect.right - r) else 0f
        return if (dx <= 0f) rect.top else rect.top + r - sqrt(maxOf(r * r - dx * dx, 0f))
    }
}

private class Landing(val ledge: Float, val x: Float)

private fun landingFor(
    surfaces: List<RainSurface>,
    i: Int,
    cycle: Int,
    length: Float,
    slant: Float,
    w: Float,
    h: Float,
): Landing? {
    if (surfaces.isEmpty() || roll(i, cycle, SURFACE_SALT) >= SURFACE_DROP_SHARE) return null
    val depth = surfaceDepth(i, cycle)
    val fx = cycleX(i, cycle)
    fun headX(at: Float) = wrap(fx * w + slant * (at - length), w) + slant * length
    var probe = h / 2f
    var ledge = Float.MAX_VALUE
    repeat(3) {
        ledge = ledgeFor(surfaces, headX(probe), depth)
        if (ledge == Float.MAX_VALUE) return null
        probe = ledge
    }
    val x = headX(ledge)
    ledge = ledgeFor(surfaces, x, depth)
    return if (ledge == Float.MAX_VALUE) null else Landing(ledge, x)
}

private fun DrawScope.drawRain(
    state: LayerState,
    layer: WeatherEffectLayer.Rain,
    t: Float,
    strength: Float,
    surfaces: List<RainSurface>,
) {
    val field = state.field
    val w = size.width
    val h = size.height
    val stroke = 1.2.dp.toPx()
    for (i in 0 until field.count) {
        val length = rainLength(field, i, layer.intensity, this)
        val fall = rainFall(field, i, layer.intensity, h, this)
        val p = progress(field, i, fall, t)
        val cycle = cycleOf(field, i, fall, t)
        val y = p * fall.span - length
        val x = wrap(cycleX(i, cycle) * w + layer.slant * y, w)
        var endY = y + length
        var endX = x + layer.slant * length
        val landing = landingFor(surfaces, i, cycle, length, layer.slant, w, h)
        var visible = true
        if (landing != null) {
            if (landing.ledge <= y) visible = false
            if (landing.ledge < endY) {
                endY = landing.ledge
                endX = x + layer.slant * (landing.ledge - y)
            }
        }
        val alpha = (0.12f + 0.2f * layer.intensity) * (0.6f + 0.4f * field.size[i]) * strength
        if (visible) {
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(x, y),
                end = Offset(endX, endY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        // The splash belongs to whichever pass last hit something: this one, or the one that just wrapped around.
        for (c in cycle downTo cycle - 1) {
            val hit = if (c == cycle) landing else landingFor(surfaces, i, c, length, layer.slant, w, h)
            val impact = hit?.let { it.ledge / fall.span } ?: fall.impact
            val since = ((cycle - c) + p - impact) / fall.cyclesPerSecond
            if (since < 0f || since > SPLASH_S) continue
            if (hit != null) {
                drawSplash(since, hit.x, hit.ledge, (0.3f + 0.3f * layer.intensity) * strength)
            } else if (roll(i, c, SPLASH_SALT) < SPLASH_SHARE) {
                val bx = wrap(cycleX(i, c) * w + layer.slant * (h - length), w) + layer.slant * length
                drawSplash(since, bx, h - 2.dp.toPx(), (0.2f + 0.25f * layer.intensity) * strength)
            }
        }
    }
}

private fun surfaceDepth(i: Int, cycle: Int): Int {
    val v = roll(i, cycle, DEPTH_SALT)
    return if (v < 0.6f) 0 else if (v < 0.88f) 1 else 2
}

// Top of the (depth + 1)th surface covering this column, counted from the top down.
private fun ledgeFor(surfaces: List<RainSurface>, x: Float, depth: Int): Float {
    var floor = -Float.MAX_VALUE
    var found = Float.MAX_VALUE
    for (n in 0..depth) {
        found = Float.MAX_VALUE
        for (r in surfaces) {
            val top = r.topAt(x)
            if (top > floor && top < found) found = top
        }
        if (found == Float.MAX_VALUE) return Float.MAX_VALUE
        floor = found
    }
    return found
}

private fun DrawScope.drawHail(state: LayerState, intensity: Float, t: Float, strength: Float) {
    val field = state.field
    val w = size.width
    val h = size.height
    for (i in 0 until field.count) {
        val radius = hailRadius(field, i, this)
        val fall = hailFall(field, i, h, this)
        val p = progress(field, i, fall, t)
        val cycle = cycleOf(field, i, fall, t)
        val y = p * fall.span - radius
        drawCircle(Color.White.copy(alpha = (0.3f + 0.3f * intensity) * strength), radius, Offset(cycleX(i, cycle) * w, y))
        for (c in cycle downTo cycle - 1) {
            val since = ((cycle - c) + p - fall.impact) / fall.cyclesPerSecond
            if (since < 0f || since > SPLASH_S || roll(i, c, SPLASH_SALT) >= HAIL_SPLASH_SHARE) continue
            drawSplash(since, cycleX(i, c) * w, h - 2.dp.toPx(), (0.3f + 0.3f * intensity) * strength)
        }
    }
}

private fun sinceImpact(progress: Float, fall: Fall, impact: Float = fall.impact): Float {
    val cycles = if (progress >= impact) progress - impact else progress + 1f - impact
    return cycles / fall.cyclesPerSecond
}

// Seen from the side: a few droplets thrown up off the surface that fall back down.
private fun DrawScope.drawSplash(sinceImpact: Float, x: Float, y: Float, alpha: Float) {
    if (sinceImpact > SPLASH_S) return
    val f = sinceImpact / SPLASH_S
    val fade = 1f - f
    val gravity = 420.dp.toPx()
    for (k in 0..2) {
        val sideways = (k - 1) * 26.dp.toPx() + (k - 1) * sinceImpact * 10.dp.toPx()
        val up = (46 + 14 * k).dp.toPx()
        val px = x + sideways * sinceImpact
        val py = y - up * sinceImpact + 0.5f * gravity * sinceImpact * sinceImpact
        if (py > y) continue
        drawCircle(Color.White.copy(alpha = alpha * 0.9f * fade), 0.9.dp.toPx(), Offset(px, py))
    }
}

private const val SURFACE_SETTLE_S = 0.5f
private const val FALL_S = 1.1f
private const val FALL_GRAVITY_DP = 1500f
private const val PILE_BUCKET_DP = 2f
private const val PILE_MAX_DP = 9f
private const val PILE_DEPOSIT_DP = 0.5f
private const val PILE_REPOSE = 0.7f

private class Falling(val t0: Float, val x: Float, val y0: Float, val r: Float, val vx: Float)

private class SnowState {
    val piles = HashMap<String, FloatArray>()
    val falling = ArrayList<Falling>()
    var tick = 0
    var lastMove = 0f
    var settled = false
}

private fun dismissPiles(state: SnowState, surfaces: List<RainSurface>, now: Float, density: Float) {
    if (state.piles.isEmpty()) return
    val bucket = PILE_BUCKET_DP * density
    for (surface in surfaces) {
        val pile = state.piles[surface.key] ?: continue
        for (b in pile.indices) {
            val height = pile[b]
            if (height < 0.15f * density || Random.nextFloat() < 0.4f) continue
            val x = (surface.anchorLeft + (b + 0.5f) * bucket).coerceIn(surface.rect.left, surface.rect.right)
            val r = (0.7f * density + height * 0.5f) * (0.8f + 0.4f * Random.nextFloat())
            state.falling.add(Falling(now + Random.nextFloat() * 0.18f, x, surface.topAt(x) - height * 0.5f, r, (Random.nextFloat() - 0.5f) * 36f * density))
        }
    }
    state.piles.clear()
}

private class Flake(val x: Float, val y: Float, val radius: Float, val cycle: Int)

private fun flakeAt(field: ParticleField, i: Int, t: Float, w: Float, h: Float, density: Float): Flake {
    val radius = (1.4f + 1.8f * field.size[i]) * density
    val span = h + radius * 2
    val rate = 0.08f + 0.1f * field.speed[i]
    val raw = field.phase[i] + t * rate
    val y = (raw % 1f) * span - radius
    val x = wrap(field.x[i] * w + sin(t * 0.8f + field.phase[i] * TWO_PI) * 10f * density, w)
    return Flake(x, y, radius, floor(raw).toInt())
}

private fun settleSnow(
    piles: HashMap<String, FloatArray>,
    surfaces: List<RainSurface>,
    field: ParticleField,
    from: Float,
    to: Float,
    w: Float,
    h: Float,
    density: Float,
) {
    if (surfaces.isEmpty()) return
    val bucket = PILE_BUCKET_DP * density
    val maxDiff = PILE_REPOSE * bucket
    val max = PILE_MAX_DP * density
    for (i in 0 until field.count) {
        val before = flakeAt(field, i, from, w, h, density)
        val after = flakeAt(field, i, to, w, h, density)
        if (before.cycle != after.cycle) continue
        val ledge = ledgeFor(surfaces, after.x, surfaceDepth(i, after.cycle))
        if (ledge == Float.MAX_VALUE) continue
        if (before.y + before.radius >= ledge || after.y + after.radius < ledge) continue
        val surface = surfaces.firstOrNull { abs(it.topAt(after.x) - ledge) < 0.75f } ?: continue
        val n = (surface.anchorWidth / bucket).toInt() + 2
        var pile = piles[surface.key]
        if (pile == null || pile.size != n) {
            pile = pile?.copyOf(n) ?: FloatArray(n)
            piles[surface.key] = pile
        }
        val at = ((after.x - surface.anchorLeft) / bucket).toInt().coerceIn(0, n - 1)
        pile[at] += PILE_DEPOSIT_DP * density * (0.5f + field.size[i]) * (1f - pile[at] / max).coerceAtLeast(0f)
        repeat(5) {
            for (j in 0 until n - 1) {
                val d = pile[j] - pile[j + 1]
                if (d > maxDiff) {
                    val move = (d - maxDiff) / 2f
                    pile[j] -= move
                    pile[j + 1] += move
                } else if (-d > maxDiff) {
                    val move = (-d - maxDiff) / 2f
                    pile[j] += move
                    pile[j + 1] -= move
                }
            }
        }
    }
}

private fun DrawScope.drawPile(surface: RainSurface, pile: FloatArray, alpha: Float) {
    val density = this.density
    val bucket = PILE_BUCKET_DP.dp.toPx()
    val rect = surface.rect
    val first = ((rect.left - surface.anchorLeft) / bucket).toInt().coerceIn(0, pile.size - 1)
    val last = ((rect.right - surface.anchorLeft) / bucket).toInt().coerceIn(0, pile.size - 1)
    val eps = 0.12f * density
    var b = first
    while (b <= last) {
        if (pile[b] <= eps) {
            b++
            continue
        }
        var end = b
        while (end + 1 <= last && pile[end + 1] > eps) end++
        val path = Path()
        val xs = ArrayList<Float>()
        for (k in (b - 1)..(end + 1)) {
            val x = (surface.anchorLeft + (k + 0.5f) * bucket).coerceIn(rect.left, rect.right)
            val height = if (k < b || k > end) 0f else pile[k]
            xs.add(x)
            val y = surface.topAt(x) - height
            if (k == b - 1) path.moveTo(x, y) else path.lineTo(x, y)
        }
        for (k in xs.indices.reversed()) path.lineTo(xs[k], surface.topAt(xs[k]))
        path.close()
        drawPath(path, Color.White.copy(alpha = alpha))
        b = end + 1
    }
}

private fun DrawScope.drawSnow(
    field: ParticleField,
    t: Float,
    intensity: Float,
    strength: Float,
    surfaces: List<RainSurface>,
    state: SnowState,
) {
    val density = this.density
    for (i in 0 until field.count) {
        val flake = flakeAt(field, i, t, size.width, size.height, density)
        if (surfaces.isNotEmpty()) {
            val ledge = ledgeFor(surfaces, flake.x, surfaceDepth(i, flake.cycle))
            if (ledge != Float.MAX_VALUE && flake.y + flake.radius > ledge) continue
        }
        drawCircle(Color.White.copy(alpha = (0.25f + 0.35f * intensity) * strength), flake.radius, Offset(flake.x, flake.y))
    }
    for (surface in surfaces) {
        val pile = state.piles[surface.key] ?: continue
        drawPile(surface, pile, 0.95f * strength.coerceAtMost(1f))
    }
    for (chunk in state.falling) {
        val dt = t - chunk.t0
        if (dt < 0f) continue
        val life = 1f - dt / FALL_S
        val cx = chunk.x + chunk.vx * dt
        val cy = chunk.y0 + 0.5f * FALL_GRAVITY_DP * density * dt * dt
        drawCircle(Color.White.copy(alpha = 0.9f * life * strength.coerceAtMost(1f)), chunk.r, Offset(cx, cy))
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
private const val SURFACE_DROP_SHARE = 0.035f
private const val SPLASH_SHARE = 0.22f
private const val HAIL_SPLASH_SHARE = 0.12f
private const val RAIN_HAPTIC_SHARE = 0.05f
private const val HAIL_HAPTIC_SHARE = 0.1f
private const val HERO_SALT = 101
private const val SPLASH_SALT = 211
private const val SURFACE_SALT = 307
private const val DEPTH_SALT = 401
private val SUN_COLOR = Color(0xFFFFD27A)
private val CLEAR_FADE = 32.dp
