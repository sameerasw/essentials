package com.sameerasw.essentials.island.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize

internal data class IslandVisualFrame(
    val bounds: Rect,
    val cornerRadius: Float,
    val alpha: Float,
    val pulse: Float,
    val cameraCenter: Offset,
    val cameraRadius: Float,
)

internal fun visualExpansionAmount(live: IntSize, compact: IntSize, expanded: IntSize): Float {
    val heightRange = expanded.height - compact.height
    val widthRange = expanded.width - compact.width
    return when {
        heightRange > 0 -> (live.height - compact.height).toFloat() / heightRange
        widthRange > 0 -> (live.width - compact.width).toFloat() / widthRange
        else -> 0f
    }.coerceIn(0f, 1f)
}

val LocalIslandSurfaceAccent = staticCompositionLocalOf<(Color) -> Unit> { {} }

@Composable
fun IslandSurfaceAccent(color: Color) {
    val report = LocalIslandSurfaceAccent.current
    SideEffect { report(color) }
}
