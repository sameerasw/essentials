package com.sameerasw.essentials.island.state

import com.sameerasw.essentials.island.model.SideBubble
import com.sameerasw.essentials.island.model.SideBubblePriority
import com.sameerasw.essentials.island.model.QueueOverflow
import com.sameerasw.essentials.island.ui.OVERLAP_MAX
import com.sameerasw.essentials.island.ui.overlapSlots
import kotlin.math.ceil
import com.sameerasw.essentials.island.ui.inlineBubbleCell
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
    val sideBubble: SideBubble? = null,
    val sideBubbleKey: String? = null,
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

    var maxCells: Int = CompactLayoutEngine.MAX_CELLS
    var compactFits: ((before: List<String>, after: List<String>, leftExtra: Int, growing: Boolean) -> Int?)? = null
    var compactCellWidth: (String) -> Float = { 1f }
    var compactBubbleWidth: (Int) -> Float = { it.toFloat() }
    var balanceCompact: Boolean = true
    var chipsEnabled: Boolean = false
    var secondaryBubbleEnabled: Boolean = true
    var secondaryBubbleMax: Int = 0
    var bubbleOnRight: Boolean = false
    private var feedbackActive = false

    fun setFeedbackActive(active: Boolean) {
        if (feedbackActive == active) return
        feedbackActive = active
        recompute()
    }
    var lineStageEnabled: Boolean = true
    var expandedTimeoutMs: Long = 0L
    var holdFocus: Boolean = false
    var onStageChanged: ((IslandStage) -> Unit)? = null
    var onAutoExpand: (() -> Unit)? = null

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
    private var hiddenSources: Set<String>? = null
    private var launcherOnlySources: Set<String> = emptySet()
    private var launcherOnlyProviders: Set<String> = emptySet()
    private var onLauncher = true
    private var updateOnlySources: Set<String> = emptySet()
    private var updateOnlyProviders: Set<String> = emptySet()
    private var updateRevealMs: Long = 0L
    private val seenUpdates = mutableMapOf<String, String?>()
    private val revealTimers = mutableMapOf<String, Cancellable>()

    fun setItems(sourceId: String, items: List<IslandItem>) {
        val previous = itemsBySource[sourceId].orEmpty()
        itemsBySource[sourceId] = items
        val updated = trackUpdates(sourceId, previous, items)
        recompute()
        updated.firstOrNull()?.let { peek(it, updateRevealMs) }
    }

    fun setUpdateOnly(sources: Set<String>, providers: Set<String>, revealMs: Long) {
        if (updateOnlySources == sources && updateOnlyProviders == providers && updateRevealMs == revealMs) return
        updateOnlySources = sources
        updateOnlyProviders = providers
        updateRevealMs = revealMs
        recompute()
    }

    private fun isUpdateOnly(sourceId: String, item: IslandItem): Boolean =
        sourceId in updateOnlySources || item.key.substringBefore(':') in updateOnlyProviders

    private fun trackUpdates(sourceId: String, previous: List<IslandItem>, items: List<IslandItem>): List<String> {
        val currentKeys = items.mapTo(mutableSetOf()) { it.key }
        previous.filter { it.key !in currentKeys }.forEach {
            seenUpdates.remove(it.key)
            revealTimers.remove(it.key)?.cancel()
        }
        return items.filter { isUpdateOnly(sourceId, it) }.mapNotNull { item ->
            val signature = item.updateKey ?: item.contentId
            if (item.key in seenUpdates && seenUpdates[item.key] == signature) return@mapNotNull null
            seenUpdates[item.key] = signature
            revealTimers.remove(item.key)?.cancel()
            revealTimers[item.key] = scheduler.schedule(updateRevealMs) {
                revealTimers.remove(item.key)
                recompute()
            }
            item.key
        }
    }

    fun setHiddenPackage(packageName: String?, onlySources: Set<String>? = null) {
        if (hiddenPackage == packageName && hiddenSources == onlySources) return
        hiddenPackage = packageName
        hiddenSources = onlySources
        recompute()
    }

    fun setLauncherState(onLauncher: Boolean, launcherOnlySources: Set<String>, launcherOnlyProviders: Set<String> = emptySet()) {
        if (this.onLauncher == onLauncher && this.launcherOnlySources == launcherOnlySources &&
            this.launcherOnlyProviders == launcherOnlyProviders
        ) {
            return
        }
        this.onLauncher = onLauncher
        this.launcherOnlySources = launcherOnlySources
        this.launcherOnlyProviders = launcherOnlyProviders
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
            is PluginRequest.Expand -> {
                expand(request.itemKey, request.sticky)
                if (expandedKey == request.itemKey) onAutoExpand?.invoke()
            }
            is PluginRequest.Collapse -> if (expandedKey == request.itemKey || peekKey == request.itemKey) collapse()
        }
    }

    var fallbackTapKey: (() -> String?)? = null
    var opensOnExpandedTap: (IslandItem) -> Boolean = { false }

    fun onTap(itemKey: String?): Boolean {
        val current = _state.value
        return when (current.stage) {
            IslandStage.Hidden -> false
            IslandStage.Expanded -> {
                val focused = current.focused
                val handled = focused?.interactions?.onExpandedTap?.invoke() == true
                if (!handled && focused != null && opensOnExpandedTap(focused)) focused.onOpen?.invoke()
                collapse()
                true
            }
            IslandStage.Line -> current.focusedKey?.let {
                if (current.focused?.interactions?.onLineTap?.invoke() != true) expand(it)
                true
            } ?: false
            IslandStage.Compact -> {
                val tapped = itemKey?.let { current.items[it] }
                if (tapped?.expanded == null && tapped?.interactions?.onTap?.invoke() == true) return true
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

    private var userHold = false

    fun setUserHold(held: Boolean) {
        if (userHold == held) return
        userHold = held
        if (held) {
            expandedTimer?.cancel()
            expandedTimer = null
        } else {
            restartExpandedTimer()
        }
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
        if (timeout <= 0L || holdFocus || stickyFocus || userHold) return
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
                val visible = when {
                    onLauncher -> items
                    source in launcherOnlySources -> items.filter { it.bypassLauncherOnly }
                    else -> items.filter { it.bypassLauncherOnly || it.key.substringBefore(':') !in launcherOnlyProviders }
                }.filter { !isUpdateOnly(source, it) || it.key in revealTimers }
                val hideHere = hiddenPackage != null && hiddenSources?.contains(source) != false
                if (hideHere) visible.filter { it.sourcePackage != hiddenPackage } else visible
            }
            .map(::inlineBubbleIfDisabled)
            .associateBy { it.key }

    private fun inlineBubbleIfDisabled(item: IslandItem): IslandItem {
        val bubble = item.sideBubble
        if (secondaryBubbleEnabled || bubble == null || bubble.whenOccupiedOnly) return item
        return item.withInlineBubble(inlineBubbleCell(bubble))
    }

    private fun recompute() {
        val items = if (suppressed) emptyMap() else allItems()
        if (expandedKey != null && items[expandedKey]?.expanded == null) {
            expandedTimer?.cancel()
            expandedKey = null
        }
        if (peekKey != null && items[peekKey]?.line == null) cancelPeek()

        val liveProgress = items.values.any { item ->
            item.sideBubble?.let { it.priority == SideBubblePriority.PROGRESS && !it.whenOccupiedOnly && it.icons.isNotEmpty() } == true
        }
        val mediaItem = items.values.firstOrNull { it.sideBubble?.priority == SideBubblePriority.MEDIA }
        val mediaBubble = mediaItem?.sideBubble?.takeIf { secondaryBubbleEnabled && !liveProgress }
        val bubbleOwner = items.values
            .filter { secondaryBubbleEnabled && it.sideBubble != null && it.sideBubble.priority != SideBubblePriority.MEDIA }
            .sortedBy { it.sideBubble!!.priority }
            .firstOrNull { candidate ->
                !candidate.sideBubble!!.whenOccupiedOnly || items.values.any {
                    it.key != candidate.key &&
                        it.compactVisible &&
                        it.placement == CompactPlacement.Dynamic &&
                        it.sideBubble?.whenOccupiedOnly != true
                }
            }
        val merged = mergedBubble(items, bubbleOwner, mediaBubble)
        val usesBubbleOnly = merged == null && bubbleOwner?.sideBubble?.whenOccupiedOnly == true
        val anchor = anchorProvider()
        val mirrored = bubbleOnRight && anchor == CameraAnchor.Center
        fun CompactArrangement.flipped() = if (mirrored) CompactArrangement(after, before, visibleItems) else this
        val arrangement = CompactLayoutEngine.arrange(
            items.values.filter {
                it.compactVisible && !(usesBubbleOnly && it.key == bubbleOwner?.key) && !(mediaBubble != null && it.key == mediaItem?.key)
            }.map { item ->
                CompactEntry(
                    item.key,
                    item.effectivePriority,
                    item.placement == CompactPlacement.Pinned,
                    item.compact.filterNot { it.soloOnly }.map { it.key },
                    item.compact.filter { it.soloOnly }.map { it.key },
                    item.needsCompanyAtCenter,
                    item.companionOnly,
                    chipsEnabled && item.chip,
                )
            },
            anchor,
            maxCells = maxCells,
            leftExtra = (merged ?: bubbleOwner?.sideBubble)?.cells?.coerceAtLeast(1) ?: 0,
            balance = balanceCompact,
            cellWidth = compactCellWidth,
            leftExtraWidth = (merged ?: bubbleOwner?.sideBubble)?.cells?.coerceAtLeast(1)?.let(compactBubbleWidth) ?: 0f,
            previous = _state.value.arrangement.flipped(),
            fits = compactFits?.let { fits ->
                val extra = (merged ?: bubbleOwner?.sideBubble)?.cells?.coerceAtLeast(1) ?: 0
                val shown = (_state.value.arrangement.before + _state.value.arrangement.after).toSet()
                val wrapped: (List<String>, List<String>) -> Int? = { before, after ->
                    val growing = (before + after).any { it !in shown }
                    if (mirrored) fits(after, before, extra, growing) else fits(before, after, extra, growing)
                }
                wrapped
            },
        ).flipped()
        val focusedKey = expandedKey ?: peekKey
        val stage = when {
            feedbackActive && !suppressed && expandedKey == null && peekKey == null -> IslandStage.Compact
            items.isEmpty() -> IslandStage.Hidden
            expandedKey != null -> IslandStage.Expanded
            peekKey != null -> IslandStage.Line
            arrangement.visibleItems.isEmpty() && bubbleOwner == null && merged == null -> IslandStage.Hidden
            else -> IslandStage.Compact
        }
        val previous = _state.value.stage
        val bubble = merged ?: bubbleOwner?.sideBubble
        val bubbleKey = if (merged != null) MERGED_BUBBLE_KEY else bubbleOwner?.key
        _state.value = IslandUiState(stage, items, arrangement, focusedKey, bubble, bubbleKey)
        if (previous != stage) onStageChanged?.invoke(stage)
    }

    private fun mergedBubble(items: Map<String, IslandItem>, owner: IslandItem?, media: SideBubble?): SideBubble? {
        if (!secondaryBubbleEnabled) return null
        val progress = items.values
            .mapNotNull { it.sideBubble }
            .filter { it.priority == SideBubblePriority.PROGRESS && !it.whenOccupiedOnly && it.icons.isNotEmpty() }
        if (progress.isEmpty() && media == null) return null
        val notifications = owner?.sideBubble?.takeIf { it.priority != SideBubblePriority.PROGRESS }
        val lead = media?.let { it.soloIcons ?: it.icons }.orEmpty()
        val icons = lead + progress.flatMap { it.icons } + notifications?.icons.orEmpty()
        val cap = overlapSlots(icons, secondaryBubbleMax.takeIf { it > 0 } ?: OVERLAP_MAX)
        val shown = icons.size.coerceIn(1, cap)
        val overflow = if (icons.size > cap && notifications?.overflow != QueueOverflow.None) 0.3f else 0f
        val wide = icons.take(shown).sumOf { (it.widthFactor - 1f).coerceAtLeast(0f).toDouble() }.toFloat()
        val cells = maxOf(ceil(1f + 0.55f * (shown - 1) + overflow + wide).toInt(), notifications?.cells ?: 1)
        return SideBubble(
            priority = SideBubblePriority.PROGRESS,
            onOpen = notifications?.onOpen ?: progress.firstOrNull()?.onOpen ?: media?.onOpen ?: {},
            icons = icons,
            onDismiss = notifications?.onDismiss,
            maxIcons = secondaryBubbleMax,
            overflow = notifications?.overflow ?: QueueOverflow.Number,
            cells = cells,
        )
    }

    companion object {
        const val MERGED_BUBBLE_KEY = "bubbles.merged"
    }
}
