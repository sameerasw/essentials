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
    var holdFocus: Boolean = false
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
    private var stickyFocus = false

    private var hiddenPackage: String? = null
    private var launcherOnlySources: Set<String> = emptySet()
    private var onLauncher = true

    fun setItems(sourceId: String, items: List<IslandItem>) {
        itemsBySource[sourceId] = items
        recompute()
    }

    fun setHiddenPackage(packageName: String?) {
        if (hiddenPackage == packageName) return
        hiddenPackage = packageName
        recompute()
    }

    fun setLauncherState(onLauncher: Boolean, launcherOnlySources: Set<String>) {
        if (this.onLauncher == onLauncher && this.launcherOnlySources == launcherOnlySources) return
        this.onLauncher = onLauncher
        this.launcherOnlySources = launcherOnlySources
        recompute()
    }

    fun relayout() = recompute()

    fun setSuppressed(value: Boolean) {
        if (suppressed == value) return
        suppressed = value
        if (value) clearFocus()
        recompute()
    }

    fun handle(request: PluginRequest) {
        when (request) {
            is PluginRequest.Peek -> peek(request.itemKey, request.durationMs, sticky = request.sticky)
            is PluginRequest.Expand -> expand(request.itemKey, request.sticky)
            is PluginRequest.Collapse -> if (expandedKey == request.itemKey || peekKey == request.itemKey) collapse()
        }
    }

    var fallbackTapKey: (() -> String?)? = null

    fun onTap(itemKey: String?): Boolean {
        val current = _state.value
        return when (current.stage) {
            IslandStage.Hidden -> false
            IslandStage.Expanded -> {
                collapse()
                true
            }
            IslandStage.Line -> current.focusedKey?.let { expand(it); true } ?: false
            IslandStage.Compact -> {
                val target = itemKey?.let { current.items[it] }?.takeIf { it.expanded != null }
                    ?: defaultTapTarget(current)
                    ?: fallbackTapKey?.invoke()?.let { current.items[it] }?.takeIf { it.expanded != null }
                    ?: return false
                if (target.interactions.onTap?.invoke() != true) expand(target.key)
                true
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
        if (item.queue != null) {
            item.onDismiss?.invoke()
            restartExpandedTimer()
            return true
        }
        clearFocus()
        item.onDismiss?.invoke()
        recompute()
        return true
    }

    fun advanceFocused(): Boolean {
        val queue = _state.value.focused?.queue ?: return false
        queue.onAdvance()
        restartExpandedTimer()
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

    fun collapseImmediately() = collapseNow()

    private fun collapseNow() {
        clearFocus()
        recompute()
    }

    fun expand(itemKey: String, sticky: Boolean = false) {
        val item = allItems()[itemKey] ?: return
        if (item.expanded == null || suppressed) return
        cancelPeek()
        expandedKey = itemKey
        stickyFocus = sticky
        restartExpandedTimer()
        recompute()
    }

    fun peek(itemKey: String, durationMs: Long, force: Boolean = false, sticky: Boolean = false) {
        if ((!lineStageEnabled && !force) || suppressed || expandedKey != null) return
        val item = allItems()[itemKey] ?: return
        if (item.line == null) return
        cancelPeek()
        peekKey = itemKey
        stickyFocus = sticky
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
        if (holdFocus || stickyFocus) return
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
            .minByOrNull { it.effectivePriority }

    private fun restartExpandedTimer() {
        expandedTimer?.cancel()
        expandedTimer = null
        val timeout = expandedTimeoutMs
        val key = expandedKey ?: return
        if (timeout <= 0L || holdFocus || stickyFocus) return
        expandedTimer = scheduler.schedule(timeout) { if (expandedKey == key) collapse() }
    }

    private fun cancelPeek() {
        peekTimer?.cancel()
        peekTimer = null
        peekKey = null
        stickyFocus = false
    }

    private fun clearFocus() {
        cancelPeek()
        expandedTimer?.cancel()
        expandedTimer = null
        expandedKey = null
    }

    private fun allItems(): Map<String, IslandItem> =
        itemsBySource
            .flatMap { (source, items) ->
                if (onLauncher || source !in launcherOnlySources) items else items.filter { it.bypassLauncherOnly }
            }
            .filter { hiddenPackage == null || it.sourcePackage != hiddenPackage }
            .associateBy { it.key }

    private fun recompute() {
        val items = if (suppressed) emptyMap() else allItems()
        if (expandedKey != null && items[expandedKey]?.expanded == null) {
            expandedTimer?.cancel()
            expandedKey = null
        }
        if (peekKey != null && items[peekKey]?.line == null) cancelPeek()

        val arrangement = CompactLayoutEngine.arrange(
            items.values.filter { it.compactVisible }.map { item ->
                CompactEntry(
                    item.key,
                    item.effectivePriority,
                    item.placement == CompactPlacement.Pinned,
                    item.compact.filterNot { it.soloOnly }.map { it.key },
                    item.compact.filter { it.soloOnly }.map { it.key },
                )
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
