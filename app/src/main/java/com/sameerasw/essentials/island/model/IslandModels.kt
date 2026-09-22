package com.sameerasw.essentials.island.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sameerasw.essentials.island.ui.IslandLayoutSpec

enum class IslandStage { Hidden, Compact, Line, Expanded }

enum class CompactPlacement { Pinned, Dynamic }

// Island item priority
object IslandPriority {
    // Calls outrank everything, including the pinned time/battery.
    const val CALL = -10
    const val TIME = 0
    const val BATTERY = 1
    const val NOTIFICATION = 10
    const val FLASHLIGHT = 20
    const val MEDIA = 30
    const val CONSCIOUS_GATE = 40
    const val CALENDAR = 50
    const val DEFAULT = 100
}

class CompactCell(
    val key: String,
    val content: @Composable () -> Unit,
)

class LineContent(
    val icon: @Composable () -> Unit,
    val start: String,
    val end: String,
    val endSlot: (@Composable () -> Unit)? = null,
)

interface IslandExpandedScope {
    val spec: IslandLayoutSpec
    val accent: Color
    fun collapse()
    fun dismiss()
    fun openApp()

    // Makes the island window focusable so a text field can take the keyboard
    fun setTextInput(active: Boolean)

    // Call while the user is doing something
    fun keepAlive()
}

class ExpandedContent(val content: @Composable (IslandExpandedScope) -> Unit)

class InteractionOverrides(
    val onTap: (() -> Boolean)? = null,
    val onLongPress: (() -> Unit)? = null,
) {
    companion object {
        val Default = InteractionOverrides()
    }
}

class IslandItem(
    val key: String,
    val priority: Int,
    val placement: CompactPlacement,
    val compact: List<CompactCell>,
    val line: LineContent? = null,
    val expanded: ExpandedContent? = null,
    val accent: Color? = null,
    val dismissible: Boolean = false,
    val onDismiss: (() -> Unit)? = null,
    val onOpen: (() -> Unit)? = null,
    val interactions: InteractionOverrides = InteractionOverrides.Default,
) {
    init {
        require(compact.size in 1..2) { "IslandItem $key must have 1..2 compact cells" }
    }
}

sealed interface PluginRequest {
    data class Peek(val itemKey: String, val durationMs: Long) : PluginRequest
    data class Expand(val itemKey: String) : PluginRequest
    data class Collapse(val itemKey: String) : PluginRequest
}
