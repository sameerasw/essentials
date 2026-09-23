package com.sameerasw.essentials.island.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf


class IslandBackdropSlot {
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)
}

val LocalIslandBackdropSlot = staticCompositionLocalOf<IslandBackdropSlot?> { null }

@Composable
fun SurfaceBackdrop(content: @Composable () -> Unit): Boolean {
    val slot = LocalIslandBackdropSlot.current ?: return false
    val latest by rememberUpdatedState(content)
    val entry: @Composable () -> Unit = remember { { latest() } }
    DisposableEffect(slot) {
        slot.content = entry
        onDispose { if (slot.content === entry) slot.content = null }
    }
    return true
}
