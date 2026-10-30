package com.sameerasw.essentials.island.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.sameerasw.essentials.island.ui.IslandLayoutSpec

enum class IslandStage { Hidden, Compact, Line, Expanded }

enum class QueueOverflow { Number, Dot, None }

enum class CompactPlacement { Pinned, Dynamic }

// Island item priority
object IslandPriority {
    // Calls outrank everything, including the pinned time/battery.
    const val CALL = -10
    const val TIME = 0
    const val BATTERY = 1
    const val BATTERY_ALERT = 2

    const val TIMER_OVERRIDE = 5
    const val CALENDAR_OVERRIDE = 6

    const val NOTIFICATION = 10
        const val PROGRESS = 15
    const val FLASHLIGHT = 20
    const val NAVIGATION = 22
    const val TIMER = 25
    const val MEDIA = 30
    const val CONSCIOUS_GATE = 40
    const val CAFFEINATE = 44
    const val TRAVEL = 45
    const val CALENDAR = 50
    const val SOUND_MODE = 55
    const val SIGNAL = 57
    const val NETWORK = 60
    const val HOTSPOT = 61
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
    val showLeftIcon: Boolean = true,
    // Icon + title only on line peek
    val condensed: Boolean = false,
)

interface IslandExpandedScope {
    val spec: IslandLayoutSpec
    val accent: Color
    val current: Boolean get() = true
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
    val onExpandedTap: (() -> Boolean)? = null,
    val onLineTap: (() -> Boolean)? = null,
) {
    companion object {
        val Default = InteractionOverrides()
    }
}

class StackIcon(
    val key: String,
    val current: Boolean = false,
    val onSelect: (() -> Unit)? = null,
    val highlight: Boolean = false,
    val content: @Composable (size: Dp) -> Unit,
)

class SideBubble(
    val priority: Int,
    val onOpen: () -> Unit,
    val icons: List<StackIcon> = emptyList(),
    val content: (@Composable (Dp) -> Unit)? = null,
    val whenOccupiedOnly: Boolean = false,
    val onDismiss: (() -> Unit)? = null,
    val maxIcons: Int? = null,
    val overflow: QueueOverflow = QueueOverflow.Number,
    // Compact cells' worth of room the bubble takes beside the camera
    val cells: Int = 1,
)

object SideBubblePriority {
    const val NOTIFICATION = 0
}
object IslandChips {
    val items = setOf(
        "media", "calendar", "timer", "alarm", "caffeinate", "flashlight", "sound_mode", "devices", "travel", "navigation", "call",
    )
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
    val outlineAccent: Color? = null,
    val dismissible: Boolean = false,
    val onDismiss: (() -> Unit)? = null,
    val onOpen: (() -> Unit)? = null,
    val interactions: InteractionOverrides = InteractionOverrides.Default,
    val queue: QueueInfo? = null,
    // Owning app; used to hide the item while that app is in the foreground.
    val sourcePackage: String? = null,
    // Temporary priority while the item's state is urgent; null falls back to `priority`.
    val priorityOverride: Int? = null,
    // Identifies the content currently shown, so a new one pulses even when the item key is unchanged.
    val contentId: String? = null,
    
    val compactVisible: Boolean = true,
    val needsCompanyAtCenter: Boolean = false,
    val sideBubble: SideBubble? = null,
    val companionOnly: Boolean = false,
    val bypassLauncherOnly: Boolean = false,
    
    val stack: List<StackIcon> = emptyList(),
    val queued: List<StackIcon>? = null,
    val priorityGroup: String? = null,
    // Rendered inside a colored chip when Island chips is enabled.
    val chip: Boolean = key in IslandChips.items,
    // Kept right of the camera, at the outer end of the compact island.
    val compactEndSide: Boolean = false,
) {
    var userPriority: Int? = null

    var inBubble: Boolean = false

    fun withInlineBubble(cell: CompactCell) = IslandItem(
        key = key,
        priority = priority,
        placement = placement,
        compact = listOf(cell) + compact.take(1),
        line = line,
        expanded = expanded,
        accent = accent,
        outlineAccent = outlineAccent,
        dismissible = dismissible,
        onDismiss = onDismiss,
        onOpen = onOpen,
        interactions = interactions,
        queue = queue,
        sourcePackage = sourcePackage,
        priorityOverride = priorityOverride,
        contentId = contentId,
        compactVisible = true,
        needsCompanyAtCenter = needsCompanyAtCenter,
        sideBubble = null,
        companionOnly = companionOnly,
        bypassLauncherOnly = bypassLauncherOnly,
        stack = stack,
        queued = queued,
        priorityGroup = priorityGroup,
        chip = chip,
        compactEndSide = compactEndSide,
    ).also {
        it.userPriority = userPriority
        it.inBubble = inBubble
    }

    val effectivePriority: Int get() = priorityOverride ?: userPriority ?: priority

    init {
        require(compact.size in 1..2) { "IslandItem $key must have 1..2 compact cells" }
    }
}

sealed interface PluginRequest {
    data class Peek(val itemKey: String, val durationMs: Long, val sticky: Boolean = false) : PluginRequest
    data class Expand(val itemKey: String, val sticky: Boolean = false) : PluginRequest
    data class Collapse(val itemKey: String) : PluginRequest
}
