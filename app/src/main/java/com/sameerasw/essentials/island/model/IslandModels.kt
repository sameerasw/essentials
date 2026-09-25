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

    const val TIMER_OVERRIDE = 5
    const val CALENDAR_OVERRIDE = 6

    const val NOTIFICATION = 10
    const val PROGRESS = 15
    const val FLASHLIGHT = 20
    const val TIMER = 25
    const val MEDIA = 30
    const val CONSCIOUS_GATE = 40
    const val CAFFEINATE = 44
    const val TRAVEL = 45
    const val WEATHER_ALERT = 48
    const val CALENDAR = 50
    const val SOUND_MODE = 55
    const val WEATHER = 58
    const val NETWORK = 60
    const val DEVICES = 62
    const val ALARM = 90
    const val DEFAULT = 100
}

class CompactCell(
    val key: String,
    // Shown only when this item is alone in a centred compact island, on the side opposite its cells.
    val soloOnly: Boolean = false,
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

class QueueInfo(
    val next: IslandItem,
    val onAdvance: () -> Unit,
)

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
    val queue: QueueInfo? = null,
    // Owning app; used to hide the item while that app is in the foreground.
    val sourcePackage: String? = null,
    // Temporary priority while the item's state is urgent; null falls back to `priority`.
    val priorityOverride: Int? = null,
    
    val compactVisible: Boolean = true,
    val bypassLauncherOnly: Boolean = false,
) {
    val effectivePriority: Int get() = priorityOverride ?: priority

    init {
        require(compact.size in 1..2) { "IslandItem $key must have 1..2 compact cells" }
    }
}

sealed interface PluginRequest {
    data class Peek(val itemKey: String, val durationMs: Long, val sticky: Boolean = false) : PluginRequest
    data class Expand(val itemKey: String, val sticky: Boolean = false) : PluginRequest
    data class Collapse(val itemKey: String) : PluginRequest
}
