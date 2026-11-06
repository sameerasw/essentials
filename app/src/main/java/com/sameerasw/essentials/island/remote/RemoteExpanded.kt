package com.sameerasw.essentials.island.remote

import com.sameerasw.essentials.island.ui.motionTween

import android.os.Build
import android.graphics.Outline
import android.graphics.PixelFormat
import android.view.SurfaceView
import android.view.ViewOutlineProvider
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandLayoutSpec
import com.sameerasw.essentials.island.ui.LocalIslandCollapse
import kotlinx.coroutines.delay

@Composable
fun RemoteExpanded(
    session: RemoteSurfaceSession?,
    scope: IslandExpandedScope,
    heightDp: Int,
    reportedHeightPx: Int,
    snapshot: ImageBitmap?,
    onScope: (IslandExpandedScope?) -> Unit,
    onLayout: (IslandLayoutSpec, Float, Color) -> Unit,
) {
    DisposableEffect(scope) {
        onScope(scope)
        onDispose { onScope(null) }
    }
    val density = LocalDensity.current
    val accent = scope.accent
    LaunchedEffect(scope.spec, density.density, accent) { onLayout(scope.spec, density.density, accent) }
    val width = scope.spec.expandedWidth + scope.spec.expandedOutset * 2
    val heightPxResolved = if (reportedHeightPx > 0) reportedHeightPx else with(density) { heightDp.dp.roundToPx() }
    val size = Modifier.width(width).height(with(density) { heightPxResolved.toDp() })
    if (session == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        Spacer(size)
        return
    }
    val widthPx = with(density) { width.roundToPx() }
    val heightPx = heightPxResolved
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(scope.current) {
        if (scope.current) {
            delay(REVEAL_DELAY_MS)
            revealed = true
        } else {
            revealed = false
        }
    }
    val collapse = LocalIslandCollapse.current
    val paused = LocalRemotePaused.current
    val dragging by remember { derivedStateOf { collapse() > 0.02f || paused() } }
    val showLive = revealed && !dragging && session.hasSurface
    val fade by animateFloatAsState(if (showLive) 1f else 0f, if (showLive) motionTween(160) else snap(), label = "remoteFade")
    val cornerPx = with(density) { scope.spec.expandedCorner.toPx() }
    val current = remember { intArrayOf(0, 0) }
    val corner = remember { floatArrayOf(0f) }
    val interactive by rememberUpdatedState(revealed && !dragging)
    Box(
        size.pointerInput(session) {
            detectTapGestures { if (interactive) session.tap(it.x, it.y) }
        },
    ) {
    snapshot?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
    }
    AndroidView(
        modifier = Modifier.matchParentSize(),
        factory = { context ->
            SurfaceView(context).apply {
                setZOrderMediaOverlay(true)
                holder.setFormat(PixelFormat.TRANSLUCENT)
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, corner[0])
                    }
                }
                clipToOutline = true
                addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        current[0] = widthPx
                        current[1] = heightPx
                        session.request(this@apply, widthPx, heightPx)
                    }

                    override fun onViewDetachedFromWindow(v: View) = session.release()
                })
            }
        },
        update = {
            it.alpha = fade
            it.visibility = if (fade > 0f) View.VISIBLE else View.INVISIBLE
            if (corner[0] != cornerPx) {
                corner[0] = cornerPx
                it.invalidateOutline()
            }
            if (it.isAttachedToWindow && (current[0] != widthPx || current[1] != heightPx)) {
                current[0] = widthPx
                current[1] = heightPx
                session.resize(widthPx, heightPx)
            }
        },
    )
    }
}

val LocalRemotePaused = compositionLocalOf<() -> Boolean> { { false } }

private const val REVEAL_DELAY_MS = 280L
