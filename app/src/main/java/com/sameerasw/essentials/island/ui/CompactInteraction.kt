package com.sameerasw.essentials.island.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.gestures.CompactGestures
import com.sameerasw.essentials.island.gestures.IslandSlideFeedback
import com.sameerasw.essentials.island.gestures.SlideFeedback
import com.sameerasw.essentials.island.gestures.SlideMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign

class CompactJellyState {
    val press = Animatable(0f)
    val stretchX = Animatable(0f)
    val stretchY = Animatable(0f)

    suspend fun pressDown() = press.animateTo(1f, spring(0.6f, 900f))
    suspend fun pressUp() = press.animateTo(0f, JellySpring)

    suspend fun releaseStretch(velocityX: Float, velocityY: Float) = coroutineScope {
        launch { stretchX.animateTo(0f, JellySpring, velocityX) }
        launch { stretchY.animateTo(0f, JellySpring, velocityY) }
    }

    companion object {
        const val PRESS_SCALE = 0.035f
        const val MAX_STRETCH_X = 0.10f
        const val MAX_STRETCH_Y = 0.28f
        val JellySpring = spring<Float>(dampingRatio = 0.42f, stiffness = 520f)
    }
}

@Composable
fun rememberCompactJellyState() = remember { CompactJellyState() }

private fun rubber(distance: Float, range: Float, max: Float) = max * (1f - exp(-abs(distance) / range))

fun Modifier.compactJelly(state: CompactJellyState, rangePx: Float): Modifier = graphicsLayer {
    val press = 1f + CompactJellyState.PRESS_SCALE * state.press.value
    val sx = state.stretchX.value
    val sy = state.stretchY.value.coerceAtLeast(0f)
    scaleX = press * (1f + rubber(sx, rangePx, CompactJellyState.MAX_STRETCH_X))
    scaleY = press * (1f + rubber(sy, rangePx, CompactJellyState.MAX_STRETCH_Y))
    val pull = (abs(sx) / rangePx).coerceIn(0f, 1f)
    transformOrigin = TransformOrigin(0.5f - 0.5f * sign(sx) * pull, 0f)
}

suspend fun PointerInputScope.detectCompactGestures(
    gestures: () -> CompactGestures,
    jelly: CompactJellyState,
    scope: CoroutineScope,
    context: Context,
    isBlocked: () -> Boolean,
) {
    val commitThreshold = 56.dp.toPx()
    val slideStep = 18.dp.toPx()
    val tickStep = 12.dp.toPx()
    val brightnessRange = 240.dp.toPx()
    var lastBrightnessPercent = -1
    val tracker = VelocityTracker()
    var config: CompactGestures = CompactGestures.None
    var mode = SlideMode.None
    var horizontal: Boolean? = null
    var dx = 0f
    var dy = 0f
    var lastStepX = 0f
    var lastTick = 0f
    var crossed = false

    fun reset() {
        horizontal = null
        dx = 0f
        dy = 0f
        lastStepX = 0f
        lastTick = 0f
        crossed = false
    }

    detectDragGestures(
        onDragStart = {
            reset()
            tracker.resetTracking()
            config = gestures()
            mode = config.slideMode
            config.slideBegin()
            lastBrightnessPercent = config.levelPercent()
            publish(config, mode, 0f, armed = false)
        },
        onDrag = { change, amount ->
            if (isBlocked()) return@detectDragGestures
            change.consume()
            tracker.addPosition(change.uptimeMillis, change.position)
            dx += amount.x
            dy += amount.y
            val isHorizontal = horizontal ?: (abs(dx) > abs(dy)).also { horizontal = it }

            if (isHorizontal) {
                val active = mode != SlideMode.None
                scope.launch { jelly.stretchX.snapTo(if (active) dx else dx * 0.35f) }
                when (mode) {
                    SlideMode.Brightness -> {
                        val percent = config.slideTo(dx, brightnessRange)
                        if (percent != lastBrightnessPercent) {
                            if (percent / 5 != lastBrightnessPercent / 5) IslandHaptics.sliderStep(context)
                            lastBrightnessPercent = percent
                            publish(config, mode, dx, armed = false)
                        }
                    }
                    SlideMode.Volume -> if (abs(dx - lastStepX) >= slideStep) {
                        config.slideStep(forward = dx > lastStepX)
                        lastStepX = dx
                        IslandHaptics.sliderStep(context)
                        publish(config, mode, dx, armed = false)
                    }
                    SlideMode.SoundMode, SlideMode.Track -> {
                        val now = abs(dx) >= commitThreshold
                        publish(config, mode, dx, armed = now)
                        if (now != crossed) {
                            crossed = now
                            if (now) IslandHaptics.thresholdReached(context) else IslandHaptics.thresholdLeft(context)
                        } else if (!now && abs(abs(dx) - lastTick) >= tickStep) {
                            lastTick = abs(dx)
                            IslandHaptics.dragStep(context)
                        }
                    }
                    SlideMode.None -> {}
                }
            } else {
                scope.launch { jelly.stretchY.snapTo(dy.coerceAtLeast(0f) * 0.35f) }
            }
        },
        onDragEnd = {
            val v = tracker.calculateVelocity()
            if (!isBlocked() && crossed && horizontal == true) {
                IslandHaptics.commit(context)
                config.slideCommit(dx)
                publish(config, mode, dx, armed = mode == SlideMode.Track)
            }
            val settled = mode
            scope.launch {
                delay(if (settled == SlideMode.None) 0L else FEEDBACK_LINGER_MS)
                IslandSlideFeedback.publish(null)
            }
            scope.launch { jelly.releaseStretch(v.x, v.y) }
            reset()
        },
        onDragCancel = {
            IslandSlideFeedback.publish(null)
            scope.launch { jelly.releaseStretch(0f, 0f) }
            reset()
        },
    )
}

private const val FEEDBACK_LINGER_MS = 700L

private fun publish(config: CompactGestures, mode: SlideMode, dx: Float, armed: Boolean) {
    IslandSlideFeedback.publish(
        when (mode) {
            SlideMode.Volume -> SlideFeedback.Level(brightness = false, percent = config.levelPercent())
            SlideMode.Brightness -> SlideFeedback.Level(brightness = true, percent = config.levelPercent())
            SlideMode.SoundMode -> SlideFeedback.Sound(if (armed) config.soundModeAfter(dx) else config.soundMode())
            SlideMode.Track -> SlideFeedback.Track(next = config.trackForward(dx), armed = armed)
            SlideMode.None -> null
        },
    )
}
