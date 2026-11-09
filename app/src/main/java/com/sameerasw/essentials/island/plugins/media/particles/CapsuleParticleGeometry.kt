package com.sameerasw.essentials.island.plugins.media.particles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal data class CapsuleParticleGeometry(val viewport: Size, val rect: Rect, val radius: Float) {
    private val straightX = rect.width / 2f - radius
    private val straightY = rect.height / 2f - radius
    val perimeter: Float get() = 4f * (straightX + straightY) + (2 * PI * radius).toFloat()

    fun signedDistance(point: Offset): Float {
        val qx = abs(point.x - rect.center.x) - straightX
        val qy = abs(point.y - rect.center.y) - straightY
        return hypot(max(qx, 0f), max(qy, 0f)) + min(max(qx, qy), 0f) - radius
    }

    fun sample(distance: Float): Pair<Offset, Offset> {
        var s = ((distance % perimeter) + perimeter) % perimeter
        val x = straightX
        val y = straightY
        val quarter = (PI * radius / 2).toFloat()
        fun at(px: Float, py: Float, nx: Float, ny: Float) =
            (rect.center + Offset(px, py)) to Offset(nx, ny)
        fun arc(cx: Float, cy: Float, angle: Float): Pair<Offset, Offset> =
            at(cx + radius * cos(angle), cy + radius * sin(angle), cos(angle), sin(angle))
        if (s < 2 * x) return at(-x + s, -y - radius, 0f, -1f)
        s -= 2 * x
        if (s < quarter) return arc(x, -y, (-PI / 2).toFloat() + s / radius)
        s -= quarter
        if (s < 2 * y) return at(x + radius, -y + s, 1f, 0f)
        s -= 2 * y
        if (s < quarter) return arc(x, y, s / radius)
        s -= quarter
        if (s < 2 * x) return at(x - s, y + radius, 0f, 1f)
        s -= 2 * x
        if (s < quarter) return arc(-x, y, (PI / 2).toFloat() + s / radius)
        s -= quarter
        if (s < 2 * y) return at(-x - radius, y - s, -1f, 0f)
        s -= 2 * y
        return arc(-x, -y, PI.toFloat() + if (radius > 0f) s / radius else 0f)
    }

    companion object {
        fun create(viewport: Size, inset: Float, cornerRadius: Float, capsuleRect: Rect? = null): CapsuleParticleGeometry? {
            if (!viewport.width.isFinite() || !viewport.height.isFinite() || viewport.width <= 0f || viewport.height <= 0f) return null
            if (!inset.isFinite() || inset < 0f || !cornerRadius.isFinite()) return null
            val r = capsuleRect ?: Rect(inset, inset, viewport.width - inset, viewport.height - inset)
            if (!listOf(r.left, r.top, r.right, r.bottom).all { it.isFinite() } || r.width <= 0f || r.height <= 0f) return null
            return CapsuleParticleGeometry(viewport, r, cornerRadius.coerceIn(0f, min(r.width, r.height) / 2f))
        }
    }
}

internal fun particleActivation(value: Float): Float = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
