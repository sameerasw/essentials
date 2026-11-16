package com.sameerasw.essentials.island.plugins.media.particles

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.roundToInt

val MediaParticleOutset = ParticleFieldStyle().outerBand + 8.dp

private val tileCache = ConcurrentHashMap<Float, PoissonTile>()
private val sharedPainter: ParticlePainter by lazy {
    if (Build.VERSION.SDK_INT >= 33) createRuntimeParticlePainter() ?: StaticParticlePainter() else StaticParticlePainter()
}

fun prewarmParticles(density: Density, style: ParticleFieldStyle = ParticleFieldStyle()) {
    sharedPainter
    val d = style.resolve(density).minDistance
    if (!tileCache.containsKey(d)) tileCache[d] = generatePoissonTile(d)
}

@Composable
fun GeminiParticlePill(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    cornerRadius: Dp = 24.dp,
    activation: () -> Float = { 1f },
    capsuleRect: Rect? = null,
    capsuleInset: Dp = MediaParticleOutset,
    enabled: Boolean = true,
    timeSeconds: Float? = null,
    forceFallback: Boolean = false,
    style: ParticleFieldStyle = ParticleFieldStyle(),
    geometry: (() -> MediaVisualGeometry)? = null,
    protectedCamera: (() -> MediaCameraGeometry)? = null,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val pixels = remember(style, density) { style.resolve(density) }
    val tile by produceState(tileCache[pixels.minDistance], pixels.minDistance) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.Default) {
            val running = currentCoroutineContext()
            generatePoissonTile(pixels.minDistance, checkCancelled = { running.ensureActive() })
                .also { tileCache[pixels.minDistance] = it }
        }
    }
    val latestActivation by rememberUpdatedState(activation)
    val latestColor by rememberUpdatedState(color)
    var started by remember(view) { mutableStateOf(false) }
    DisposableEffect(view) {
        val lifecycle = view.findViewTreeLifecycleOwner()?.lifecycle
        fun update() { started = lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: view.isShown }
        val observer = LifecycleEventObserver { _, _ -> update() }
        lifecycle?.addObserver(observer)
        update()
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val painter = remember(forceFallback) { if (forceFallback) StaticParticlePainter() else sharedPainter }
    var hasArea by remember { mutableStateOf(false) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var screenOrigin by remember { mutableStateOf(Offset.Zero) }
    val cameraClip = remember { Path().apply { fillType = PathFillType.EvenOdd } }
    val visible = enabled && started && style.opacity > 0f
    LaunchedEffect(visible, hasArea, painter, timeSeconds, tile) {
        if (!visible || !hasArea || tile == null || !painter.animated || timeSeconds != null) return@LaunchedEffect
        snapshotFlow { particleActivation(latestActivation()) > 0f }.collectLatest { active ->
            if (active) {
                var previous = 0L
                while (true) {
                    withFrameNanos { now ->
                        if (previous != 0L) elapsed += ((now - previous) / 1_000_000_000.0).toFloat()
                        previous = now
                    }
                }
            }
        }
    }
    Box(modifier.onSizeChanged { hasArea = it.width > 0 && it.height > 0 }
        .onGloballyPositioned {
            val origin = it.positionOnScreen()
            if (origin.x.isFinite() && origin.y.isFinite()) screenOrigin = origin
        }
        .graphicsLayer()
        .drawBehind {
            val cameraInset = if (geometry == null && capsuleRect == null) capsuleInset.toPx() else 0f
            val camera = protectedCamera?.invoke()?.takeIf { it.radius > 0f }
            val clip = camera?.let {
                cameraClip.rewind()
                cameraClip.addRect(Rect(Offset.Zero, size))
                cameraClip.addOval(Rect(it.center + Offset(cameraInset, cameraInset), it.radius + MediaCameraClearance.toPx()))
                cameraClip
            }
            val frame = geometry?.invoke()
            val capsule = CapsuleParticleGeometry.create(size, if (frame != null) 0f else capsuleInset.toPx(),
                frame?.cornerRadius ?: cornerRadius.toPx(), frame?.bounds ?: capsuleRect)
            val alpha = if (visible) particleActivation(latestActivation())*style.opacity else 0f
            val samples = tile
            if (capsule != null && samples != null && alpha > 0f) {
                val drawParticles: DrawScope.() -> Unit = {
                    with(painter) {
                        paint(capsule, timeSeconds?.takeIf { it.isFinite() } ?: elapsed, alpha, latestColor, pixels, screenOrigin, samples)
                    }
                }
                if (clip != null) clipPath(clip) { drawParticles() } else drawParticles()
            }
        })
}

internal interface ParticlePainter {
    val animated: Boolean
    fun DrawScope.paint(geometry: CapsuleParticleGeometry, time: Float, alpha: Float, color: Color,
                       style: ParticleFieldPixels, screenOrigin: Offset, tile: PoissonTile)
}

private class StaticParticlePainter : ParticlePainter {
    override val animated = false
    private var key: Triple<CapsuleParticleGeometry, ParticleFieldPixels, Offset>? = null
    private val paths = Array(256) { Path() }
    private var lastTile: PoissonTile? = null
    override fun DrawScope.paint(geometry: CapsuleParticleGeometry, time: Float, alpha: Float, color: Color,
                                style: ParticleFieldPixels, screenOrigin: Offset, tile: PoissonTile) {
        val next = Triple(geometry, style, screenOrigin)
        if (key != next || lastTile !== tile) {
            paths.forEach { it.rewind() }
            visitParticles(tile, geometry, style, screenOrigin, 0f) { x,y,radius,opacity ->
                val path = paths[(opacity*255f).roundToInt().coerceIn(0,255)]
                if (style.shape == ParticleShape.Circle) {
                    path.addOval(Rect(x-radius, y-radius, x+radius, y+radius))
                } else {
                    val extent = radius*1.25f
                    path.moveTo(x, y-extent)
                    path.lineTo(x+extent, y)
                    path.lineTo(x, y+extent)
                    path.lineTo(x-extent, y)
                    path.close()
                }
            }
            key = next
            lastTile = tile
        }
        for (i in 1..255) if (!paths[i].isEmpty) {
            drawPath(paths[i],color.copy(alpha = color.alpha*alpha*i/255f))
        }
    }
}
