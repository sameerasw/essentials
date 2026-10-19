package com.sameerasw.essentials.island.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.channels.Channel
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh

@Immutable
data class LiquidSpec(
    val stiffness: Float = 420f,
    val damping: Float = 0.38f,
    val sizeGain: Float = 0.025f,
    val travelGain: Float = 0.03f,
    val maxStretch: Float = 0.16f,
    val volume: Float = 0.85f,
) {
    companion object {
        val Surface = LiquidSpec()
        val Droplet = LiquidSpec(stiffness = 520f, damping = 0.34f, sizeGain = 0.035f, travelGain = 0.05f, maxStretch = 0.22f)
    }
}

// Reads how fast a node resizes or travels and lets a damped spring wobble the stretch, so any pill turns liquid without per-animation code
@Stable
class LiquidBody(private val spec: LiquidSpec, private val field: LiquidField? = null, private val bond: LiquidBond? = null) {
    private var stretchX by mutableFloatStateOf(0f)
    private var stretchY by mutableFloatStateOf(0f)
    private var velocityX = 0f
    private var velocityY = 0f
    private var targetX = 0f
    private var targetY = 0f
    private var lastWidth = 0f
    private var lastHeight = 0f
    private var lastPosition = Offset.Zero
    private var lastSample = 0L
    private val wake = Channel<Unit>(Channel.CONFLATED)

    fun observe(size: IntSize, position: Offset) {
        if (field != null && bond != null) field.report(Rect(position, size.toSize()), bond)
        val now = System.nanoTime()
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        if (lastSample == 0L || lastWidth <= 0f || lastHeight <= 0f) {
            store(w, h, position, now)
            return
        }
        val dt = (now - lastSample) / NANOS
        if (dt < MIN_SAMPLE_S) return
        val growX = (w - lastWidth) / lastWidth / dt
        val growY = (h - lastHeight) / lastHeight / dt
        val travelX = abs(position.x - lastPosition.x) / lastWidth / dt
        val travelY = abs(position.y - lastPosition.y) / lastHeight / dt
        targetX = soft(spec.sizeGain * growX + spec.travelGain * travelX)
        targetY = soft(spec.sizeGain * growY + spec.travelGain * travelY)
        store(w, h, position, now)
        wake.trySend(Unit)
    }

    private fun soft(raw: Float, max: Float = spec.maxStretch) = max * tanh(raw / max)

    fun release() {
        if (field != null && bond != null) field.remove(bond.id)
        lastSample = 0L
    }

    private fun store(w: Float, h: Float, position: Offset, now: Long) {
        lastWidth = w
        lastHeight = h
        lastPosition = position
        lastSample = now
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
        val idle = System.nanoTime() - lastSample > IDLE_NS
        if (idle) {
            targetX = 0f
            targetY = 0f
        }
        val damping = 2f * spec.damping * sqrt(spec.stiffness)
        val limit = spec.maxStretch * OVERSHOOT
        var remaining = dt
        while (remaining > 0f) {
            val h = minOf(remaining, SUBSTEP)
            velocityX += (spec.stiffness * (targetX - stretchX) - damping * velocityX) * h
            velocityY += (spec.stiffness * (targetY - stretchY) - damping * velocityY) * h
            stretchX = soft(stretchX + velocityX * h, limit)
            stretchY = soft(stretchY + velocityY * h, limit)
            remaining -= h
        }
        val settled = abs(stretchX) < EPSILON && abs(stretchY) < EPSILON &&
            abs(velocityX) < EPSILON && abs(velocityY) < EPSILON
        if (idle && settled) {
            stretchX = 0f
            stretchY = 0f
            velocityX = 0f
            velocityY = 0f
        }
        return !(idle && settled)
    }

    internal fun applyTo(scope: GraphicsLayerScope, origin: TransformOrigin) {
        var sx = 1f + stretchX
        var sy = 1f + stretchY
        val volume = (sx * sy).pow(0.5f * spec.volume)
        sx /= volume
        sy /= volume
        scope.scaleX = sx
        scope.scaleY = sy
        scope.transformOrigin = origin
    }

    private companion object {
        const val NANOS = 1_000_000_000f
        const val IDLE_NS = 60_000_000L
        const val MIN_SAMPLE_S = 0.006f
        const val FALLBACK_DT = 1f / 60f
        const val MAX_DT = 1f / 30f
        const val SUBSTEP = 1f / 120f
        const val OVERSHOOT = 1.5f
        const val EPSILON = 0.0008f
    }
}

@Composable
fun rememberLiquidBody(
    spec: LiquidSpec = LiquidSpec.Surface,
    bond: LiquidBond? = null,
    field: LiquidField? = LocalLiquidField.current,
): LiquidBody {
    val body = remember(spec, field, bond?.id) { LiquidBody(spec, field, bond) }
    LaunchedEffect(body) { body.run() }
    return body
}

fun Modifier.liquid(body: LiquidBody, origin: TransformOrigin = TransformOrigin.Center): Modifier =
    this.then(LiquidTrackElement(body)).graphicsLayer { body.applyTo(this, origin) }

private class LiquidTrackElement(val body: LiquidBody) : ModifierNodeElement<LiquidTrackNode>() {
    override fun create() = LiquidTrackNode(body)
    override fun update(node: LiquidTrackNode) {
        node.body = body
    }
    override fun equals(other: Any?) = other is LiquidTrackElement && other.body === body
    override fun hashCode() = body.hashCode()
}

private class LiquidTrackNode(var body: LiquidBody) : Modifier.Node(), GlobalPositionAwareModifierNode {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        body.observe(coordinates.size, coordinates.positionInRoot())
    }

    override fun onDetach() {
        body.release()
    }
}
