package com.sameerasw.essentials.island.state

import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandStage
import com.sameerasw.essentials.island.model.PluginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class IslandUiState(
    val stage: IslandStage = IslandStage.Hidden,
    val items: Map<String, IslandItem> = emptyMap(),
    val arrangement: CompactArrangement = CompactArrangement.Empty,
    val focusedKey: String? = null,
) {
    val focused: IslandItem? get() = focusedKey?.let { items[it] }
}

fun interface Cancellable {
    fun cancel()
}

fun interface DelayScheduler {
    fun schedule(delayMs: Long, action: () -> Unit): Cancellable
}

class IslandController(
    private val scheduler: DelayScheduler,
    private val anchorProvider: () -> CameraAnchor = { CameraAnchor.Center },
) {
    private val _state = MutableStateFlow(IslandUiState())
    val state: StateFlow<IslandUiState> = _state.asStateFlow()

    var lineStageEnabled: Boolean = true
    var expandedTimeoutMs: Long = 0L
    var onStageChanged: ((IslandStage) -> Unit)? = null

    var collapseAnimator: ((commit: () -> Unit) -> Unit)? = null
    private var collapsing = false

    private val itemsBySource = LinkedHashMap<String, List<IslandItem>>()
    private var suppressed = false
    private var expandedKey: String? = null
    private var peekKey: String? = null
    private var peekTimer: Cancellable? = null
    private var peekDurationMs: Long = 0L
    private var expandedTimer: Cancellable? = null

    fun setItems(sourceId: String, items: List<IslandItem>) {
        itemsBySource[sourceId] = items
        recompute()
    }

    fun setSuppressed(value: Boolean) {
        if (suppressed == value) return
        suppressed = value
        if (value) clearFocus()
        recompute()
    }

    fun handle(request: PluginRequest) {
        when (request) {
            is PluginRequest.Peek -> peek(request.itemKey, request.durationMs)
            is PluginRequest.Expand -> expand(request.itemKey)
            is PluginRequest.Collapse -> if (expandedKey == request.itemKey || peekKey == request.itemKey) collapse()
        }
    }

    fun onTap(itemKey: String?) {
        val current = _state.value
        when (current.stage) {
            IslandStage.Hidden -> Unit
            IslandStage.Expanded -> collapse()
            IslandStage.Line -> current.focusedKey?.let { expand(it) }
            IslandStage.Compact -> {
                val target = itemKey?.let { current.items[it] }?.takeIf { it.expanded != null }
                    ?: defaultTapTarget(current)
                    ?: return
                if (target.interactions.onTap?.invoke() == true) return
                expand(target.key)
            }
        }
    }

    fun onLongPress(itemKey: String?) {
        val current = _state.value
        val item = (if (current.stage == IslandStage.Compact) itemKey?.let { current.items[it] } else current.focused)
            ?: defaultTapTarget(current)
            ?: return
        val action = item.interactions.onLongPress ?: item.onOpen ?: return
        action()
        collapse()
    }

    fun dismissFocused(): Boolean {
        val item = _state.value.focused ?: return false
        if (!item.dismissible) return false
        clearFocus()
        item.onDismiss?.invoke()
        recompute()
        return true
    }

    fun collapse() {
        val animator = collapseAnimator
        val stage = _state.value.stage
        if (animator == null || (stage != IslandStage.Expanded && stage != IslandStage.Line)) {
            collapseNow()
            return
        }
        if (collapsing) return
        collapsing = true
        animator {
            collapsing = false
            collapseNow()
        }
    }

    private fun collapseNow() {
        clearFocus()
        recompute()
    }

    fun expand(itemKey: String) {
        val item = allItems()[itemKey] ?: return
        if (item.expanded == null || suppressed) return
        cancelPeek()
        expandedKey = itemKey
        restartExpandedTimer()
        recompute()
    }

    fun peek(itemKey: String, durationMs: Long) {
        if (!lineStageEnabled || suppressed || expandedKey != null) return
        val item = allItems()[itemKey] ?: return
        if (item.line == null) return
        cancelPeek()
        peekKey = itemKey
        peekDurationMs = durationMs
        schedulePeekEnd(itemKey)
        recompute()
    }

    fun onUserInteraction() {
        if (expandedKey != null) restartExpandedTimer()
        peekKey?.let { key ->
            peekTimer?.cancel()
            schedulePeekEnd(key)
        }
    }

    private fun schedulePeekEnd(itemKey: String) {
        peekTimer = scheduler.schedule(peekDurationMs) {
            if (peekKey == itemKey) {
                peekKey = null
                recompute()
            }
        }
    }

    private fun defaultTapTarget(state: IslandUiState): IslandItem? =
        state.arrangement.visibleItems
            .mapNotNull { state.items[it] }
            .filter { it.placement == CompactPlacement.Dynamic && it.expanded != null }
            .minByOrNull { it.priority }

    private fun restartExpandedTimer() {
        expandedTimer?.cancel()
        expandedTimer = null
        val timeout = expandedTimeoutMs
        val key = expandedKey ?: return
        if (timeout <= 0L) return
        expandedTimer = scheduler.schedule(timeout) { if (expandedKey == key) collapse() }
    }

    private fun cancelPeek() {
        peekTimer?.cancel()
        peekTimer = null
        peekKey = null
    }

    private fun clearFocus() {
        cancelPeek()
        expandedTimer?.cancel()
        expandedTimer = null
        expandedKey = null
    }

    private fun allItems(): Map<String, IslandItem> =
        itemsBySource.values.flatten().associateBy { it.key }

    private fun recompute() {
        val items = if (suppressed) emptyMap() else allItems()
        if (expandedKey != null && items[expandedKey]?.expanded == null) {
            expandedTimer?.cancel()
            expandedKey = null
        }
        if (peekKey != null && items[peekKey]?.line == null) cancelPeek()

        val arrangement = CompactLayoutEngine.arrange(
            items.values.map { item ->
                CompactEntry(item.key, item.priority, item.placement == CompactPlacement.Pinned, item.compact.map { it.key })
            },
            anchorProvider(),
        )
        val focusedKey = expandedKey ?: peekKey
        val stage = when {
            items.isEmpty() -> IslandStage.Hidden
            expandedKey != null -> IslandStage.Expanded
            peekKey != null -> IslandStage.Line
            arrangement.visibleItems.isEmpty() -> IslandStage.Hidden
            else -> IslandStage.Compact
        }
        val previous = _state.value.stage
        _state.value = IslandUiState(stage, items, arrangement, focusedKey)
        if (previous != stage) onStageChanged?.invoke(stage)
    }
}
