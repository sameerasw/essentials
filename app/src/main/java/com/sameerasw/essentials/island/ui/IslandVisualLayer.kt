package com.sameerasw.essentials.island.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

internal data class IslandVisualFrame(val bounds: Rect, val cornerRadius: Float, val alpha: Float, val pulse: Float,
    val cameraCenter: Offset, val cameraRadius: Float)

internal class IslandVisualEntry(val owner: String, val content: @Composable (() -> IslandVisualFrame) -> Unit) {
    var registered by mutableStateOf(false)
}

internal class IslandVisualSlot {
    var entry by mutableStateOf<IslandVisualEntry?>(null)
}

internal class IslandVisualRegistration(val slot: IslandVisualSlot, val owner: String, val publish: Boolean)

internal val LocalIslandVisualRegistration = staticCompositionLocalOf<IslandVisualRegistration?> { null }

@Composable
internal fun DetachedSurfaceVisuals(content: @Composable (() -> IslandVisualFrame) -> Unit): Boolean {
    val registration = LocalIslandVisualRegistration.current ?: return false
    val latest by rememberUpdatedState(content)
    val entry = remember(registration.owner) { IslandVisualEntry(registration.owner) { frame -> latest(frame) } }
    DisposableEffect(registration.slot, registration.publish, entry) {
        if (registration.publish) {
            entry.registered = true
            registration.slot.entry = entry
        }
        onDispose { if (registration.publish) entry.registered = false }
    }
    return true
}

internal fun visualExpansionAmount(live: IntSize, compact: IntSize, expanded: IntSize): Float {
    val heightRange = expanded.height - compact.height
    val widthRange = expanded.width - compact.width
    return when {
        heightRange > 0 -> (live.height - compact.height).toFloat() / heightRange
        widthRange > 0 -> (live.width - compact.width).toFloat() / widthRange
        else -> 0f
    }.coerceIn(0f, 1f)
}
