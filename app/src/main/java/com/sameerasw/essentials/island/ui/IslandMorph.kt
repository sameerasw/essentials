package com.sameerasw.essentials.island.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

class IslandMorph {
    val progress = Animatable(1f)
    var from: Any? by mutableStateOf(null)
        private set
    var to: Any? by mutableStateOf(null)
        private set
    private val bounds = HashMap<Pair<Any, String>, Rect>()
    private val layers = HashMap<Pair<Any, String>, GraphicsLayer>()

    val running: Boolean get() = from != null && to != null

    fun begin(fromKey: Any, toKey: Any) {
        from = fromKey
        to = toKey
    }

    fun end() {
        from = null
        to = null
    }

    fun register(layerKey: Any, tag: String, layer: GraphicsLayer) {
        layers[layerKey to tag] = layer
    }

    fun unregister(layerKey: Any, tag: String, layer: GraphicsLayer) {
        if (layers[layerKey to tag] === layer) {
            layers.remove(layerKey to tag)
            bounds.remove(layerKey to tag)
        }
    }

    fun report(layerKey: Any, tag: String, rect: Rect) {
        bounds[layerKey to tag] = rect
    }

    private fun paired(tag: String): Boolean {
        val a = from ?: return false
        val b = to ?: return false
        val ra = bounds[a to tag] ?: return false
        val rb = bounds[b to tag] ?: return false
        return ra.width > 0f && rb.width > 0f && layers.containsKey(a to tag) && layers.containsKey(b to tag)
    }

    fun hides(layerKey: Any, tag: String): Boolean = when {
        !running -> false
        layerKey == from -> true
        layerKey == to -> paired(tag)
        else -> false
    }

    fun draw(scope: DrawScope, origin: Offset) {
        val a = from ?: return
        val b = to ?: return
        val p = progress.value.coerceIn(0f, 1f)
        for (tag in TAGS) {
            if (!paired(tag)) continue
            val ra = bounds.getValue(a to tag)
            val rb = bounds.getValue(b to tag)
            val left = lerp(ra.left, rb.left, p) - origin.x
            val centerY = lerp(ra.center.y, rb.center.y, p) - origin.y
            val height = lerp(ra.height, rb.height, p)
            scope.drawMorphed(layers.getValue(b to tag), rb, left, centerY, height)
        }
    }

    private fun DrawScope.drawMorphed(layer: GraphicsLayer, rect: Rect, left: Float, centerY: Float, height: Float) {
        if (rect.height <= 0f) return
        val scale = height / rect.height
        translate(left, centerY - rect.height * scale / 2f) {
            withTransform({ scale(scale, scale, Offset.Zero) }) { drawLayer(layer) }
        }
    }

    companion object {
        const val ICON = "icon"
        const val TITLE = "title"
        const val END = "end"
        private val TAGS = listOf(ICON, TITLE, END)

        private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    }
}

class IslandMorphLayer(val morph: IslandMorph, val layerKey: Any)

val LocalIslandMorph = staticCompositionLocalOf<IslandMorphLayer?> { null }

fun Modifier.morphTag(tag: String): Modifier = composed {
    val info = LocalIslandMorph.current ?: return@composed this
    val layer = rememberGraphicsLayer()
    DisposableEffect(info, tag, layer) {
        info.morph.register(info.layerKey, tag, layer)
        onDispose { info.morph.unregister(info.layerKey, tag, layer) }
    }
    this
        .onGloballyPositioned { info.morph.report(info.layerKey, tag, it.boundsInRoot()) }
        .drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            if (!info.morph.hides(info.layerKey, tag)) drawLayer(layer)
        }
}

object IslandLiveResize {
    var active by mutableStateOf(false)
}
