package com.sameerasw.essentials.island.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class LiquidBond(
    val id: String,
    val radius: (Rect) -> Float = { it.height / 2f },
    val presence: () -> Float = { 1f },
)

val LocalLiquidField = staticCompositionLocalOf<LiquidField?> { null }

// Registered shapes that come within `reach` of each other get a spring-driven neck drawn between them, so they pinch together and tear apart like droplets
@Stable
class LiquidField {
    var reach = 0f

    private class Node(val bounds: Rect, val bond: LiquidBond)
    private class Link(var strength: Float = 0f, var velocity: Float = 0f, var gap: Float = Float.NaN, var rest: Float = 0f)

    private val nodes = mutableStateMapOf<String, Node>()
    private val links = HashMap<String, Link>()
    private var tick by mutableIntStateOf(0)
    private val wake = Channel<Unit>(Channel.CONFLATED)

    fun report(bounds: Rect, bond: LiquidBond) {
        nodes[bond.id] = Node(bounds, bond)
        wake.trySend(Unit)
    }

    fun remove(id: String) {
        nodes.remove(id)
        wake.trySend(Unit)
    }

    suspend fun run() {
        for (signal in wake) {
            var last = 0L
            do {
                val frame = withFrameNanos { it }
                val dt = if (last == 0L) FALLBACK_DT else ((frame - last) / NANOS).coerceAtMost(MAX_DT)
                last = frame
            } while (step(dt))
        }
    }

    private fun step(dt: Float): Boolean {
        var moving = false
        val live = HashSet<String>()
        forEachPair { a, b ->
            val key = a.bond.id + "|" + b.bond.id
            live += key
            val link = links.getOrPut(key) { Link() }
            val gap = gapBetween(a.bounds, b.bounds)
            link.rest = if (!link.gap.isNaN() && abs(gap - link.gap) < REST_SLOP) link.rest + dt else 0f
            link.gap = gap
            val settle = if (gap > 0f) 1f - smooth(link.rest / REST_S) else 1f
            val target = targetFor(gap) * settle
            var remaining = dt
            while (remaining > 0f) {
                val h = min(remaining, SUBSTEP)
                link.velocity += (STIFFNESS * (target - link.strength) - DAMPING * link.velocity) * h
                link.strength += link.velocity * h
                remaining -= h
            }
            if (abs(link.strength - target) > EPSILON || abs(link.velocity) > EPSILON || (gap > 0f && link.rest < REST_S && targetFor(gap) > 0f)) moving = true
        }
        links.keys.retainAll(live)
        tick++
        return moving
    }

    private fun targetFor(gap: Float): Float {
        if (reach <= 0f) return 0f
        return smooth(1f - gap.coerceAtLeast(0f) / reach)
    }

    private fun smooth(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return c * c * (3f - 2f * c)
    }

    private fun gapBetween(a: Rect, b: Rect): Float {
        val dx = max(a.left - b.right, b.left - a.right)
        val dy = max(a.top - b.bottom, b.top - a.bottom)
        return max(dx, dy)
    }

    private inline fun forEachPair(block: (Node, Node) -> Unit) {
        val ordered = nodes.values.sortedBy { it.bond.id }
        for (i in ordered.indices) for (j in i + 1 until ordered.size) block(ordered[i], ordered[j])
    }

    fun draw(scope: DrawScope, origin: Offset, color: Color) {
        tick
        forEachPair { a, b ->
            val link = links[a.bond.id + "|" + b.bond.id] ?: return@forEachPair
            val presence = min(a.bond.presence(), b.bond.presence())
            val strength = link.strength.coerceIn(0f, 1.1f) * presence * smooth(link.strength / FADE_IN)
            if (strength < MIN_DRAW) return@forEachPair
            val path = neck(a.bounds.translate(-origin), a.bond.radius(a.bounds), b.bounds.translate(-origin), b.bond.radius(b.bounds), strength)
            if (path != null) scope.drawPath(path, color)
        }
    }

    private fun neck(a: Rect, ra: Float, b: Rect, rb: Float, strength: Float): Path? {
        val dx = max(a.left - b.right, b.left - a.right)
        val dy = max(a.top - b.bottom, b.top - a.bottom)
        val horizontal = dx >= dy
        if (horizontal) return horizontalNeck(a, ra, b, rb, strength)
        val path = horizontalNeck(a.transposed(), ra, b.transposed(), rb, strength) ?: return null
        path.transform(Transpose)
        return path
    }

    private fun horizontalNeck(a: Rect, ra: Float, b: Rect, rb: Float, strength: Float): Path? {
        val (left, right) = if (a.center.x <= b.center.x) a to b else b to a
        val (rl, rr) = if (a.center.x <= b.center.x) ra to rb else rb to ra
        val top = max(left.top, right.top)
        val bottom = min(left.bottom, right.bottom)
        val overlap = bottom - top
        if (overlap <= 0f) return null
        val cy = (top + bottom) / 2f
        val half = overlap / 2f * NECK_FILL * strength.coerceAtMost(1f)
        if (half < 1f) return null
        val yTop = cy - half
        val yBottom = cy + half
        val midX = (left.right + right.left) / 2f
        return Path().apply {
            moveTo(left.right - inset(left, rl, yTop), yTop)
            quadraticTo(midX, cy - half * WAIST * strength.coerceAtMost(1f), right.left + inset(right, rr, yTop), yTop)
            lineTo(right.left + inset(right, rr, yBottom), yBottom)
            quadraticTo(midX, cy + half * WAIST * strength.coerceAtMost(1f), left.right - inset(left, rl, yBottom), yBottom)
            close()
        }
    }

    private fun inset(rect: Rect, radius: Float, y: Float): Float {
        val r = min(radius, rect.height / 2f)
        val past = abs(y - rect.center.y) - (rect.height / 2f - r)
        return if (past <= 0f) 0f else r - sqrt((r * r - past * past).coerceAtLeast(0f))
    }

    private fun Rect.transposed() = Rect(top, left, bottom, right)

    private companion object {
        const val NANOS = 1_000_000_000f
        const val FALLBACK_DT = 1f / 60f
        const val MAX_DT = 1f / 30f
        const val SUBSTEP = 1f / 120f
        const val STIFFNESS = 320f
        const val DAMPING = 2f * 0.5f * 17.9f
        const val EPSILON = 0.002f
        const val MIN_DRAW = 0.02f
        const val FADE_IN = 0.2f
        const val REST_SLOP = 0.5f
        const val REST_S = 0.3f
        const val NECK_FILL = 0.9f
        const val WAIST = 0.25f
        val Transpose = Matrix(
            floatArrayOf(
                0f, 1f, 0f, 0f,
                1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            ),
        )
    }
}

@Composable
fun LiquidBridges(field: LiquidField, color: Color = Color.Black, modifier: Modifier = Modifier) {
    LaunchedEffect(field) { field.run() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawBehind { field.draw(this, origin, color) },
    )
}
