package com.sameerasw.essentials.island.state

import kotlin.math.abs
import kotlin.math.ceil

data class CompactEntry(
    val itemKey: String,
    val priority: Int,
    val pinned: Boolean,
    // [icon, value] — icon always ends up on the outer edge.
    val cellKeys: List<String>,
    val soloCellKeys: List<String> = emptyList(),
    val needsCompany: Boolean = false,
    val companionOnly: Boolean = false,
    val chip: Boolean = false,
)

// `before` / `after` are camera-relative and ordered inner (next to camera) → outer.
data class CompactArrangement(
    val before: List<String>,
    val after: List<String>,
    val visibleItems: Set<String>,
) {
    companion object {
        val Empty = CompactArrangement(emptyList(), emptyList(), emptySet())
    }
}

object CompactLayoutEngine {
    const val MAX_CELLS = 4

    private const val MAX_SEARCH = 10
    private const val WIDTH_EPSILON = 0.01f
    private const val STICKY_WIDTH = 0.5f

    fun arrange(
        entries: List<CompactEntry>,
        anchor: CameraAnchor = CameraAnchor.Center,
        maxCells: Int = MAX_CELLS,
        leftExtra: Int = 0,
        balance: Boolean = false,
        persistentOrder: Boolean = false,
        highFirst: Boolean = false,
        fits: ((before: List<String>, after: List<String>) -> Int?)? = null,
        cellWidth: (String) -> Float = { 1f },
        leftExtraWidth: Float = leftExtra.toFloat(),
        previous: CompactArrangement = CompactArrangement.Empty,
    ): CompactArrangement {
        fun attempt(pool: List<CompactEntry>): CompactArrangement? {
            val result = arrangeOnce(pool, anchor, maxCells, balance, cellWidth, leftExtraWidth, previous, persistentOrder, highFirst)
            if (fits == null || fits(result.before, result.after) != null) return result
            if (anchor != CameraAnchor.Center) return null
            val selected = pool.filter { it.itemKey in result.visibleItems }
            if (persistentOrder) return persistentFitting(selected, result.visibleItems, highFirst, fits)
            return bestSplit(selected, result, fits)
        }

        var pool = entries
        val dropped = mutableListOf<CompactEntry>()
        var result: CompactArrangement
        while (true) {
            val fitted = attempt(pool)
            if (fitted != null) {
                result = fitted
                break
            }
            val shown = arrangeOnce(pool, anchor, maxCells, balance, cellWidth, leftExtraWidth, previous, persistentOrder, highFirst)
            if (shown.visibleItems.size <= 1) return shown
            val drop = pool.filter { it.itemKey in shown.visibleItems }.maxByOrNull { it.priority } ?: return shown
            pool = pool - drop
            dropped += drop
        }

        for (entry in dropped.sortedBy { it.priority }) {
            val candidate = attempt(pool + entry) ?: continue
            if (candidate.visibleItems.size > result.visibleItems.size && result.visibleItems.all { it in candidate.visibleItems }) {
                pool = pool + entry
                result = candidate
            }
        }
        if (previous.visibleItems.size > result.visibleItems.size && previousStillValid(previous, entries, maxCells, fits)) return previous
        return result
    }

    private fun previousStillValid(
        previous: CompactArrangement,
        entries: List<CompactEntry>,
        maxCells: Int,
        fits: ((List<String>, List<String>) -> Int?)?,
    ): Boolean {
        val byKey = entries.associateBy { it.itemKey }
        val shown = previous.visibleItems.map { byKey[it] ?: return false }
        if (shown.sumOf { it.cellKeys.size } > maxCells) return false
        val cells = (previous.before + previous.after).toSet()
        if (cells != shown.flatMap { it.cellKeys }.toSet()) return false
        return fits == null || fits(previous.before, previous.after) != null
    }

    private fun bestSplit(
        selected: List<CompactEntry>,
        base: CompactArrangement,
        fits: (List<String>, List<String>) -> Int?,
    ): CompactArrangement? {
        val baseBefore = selected.filter { e -> e.cellKeys.isNotEmpty() && e.cellKeys.all { it in base.before } }.map { it.itemKey }.toSet()
        val pinned = selected.filter { it.pinned }.map { it.itemKey }.toSet()
        return splitSearch(selected, base.visibleItems, fits) { key -> if (key in pinned) key in baseBefore else null }
            ?: splitSearch(selected, base.visibleItems, fits) { null }
    }

    private fun splitSearch(
        entries: List<CompactEntry>,
        visible: Set<String>,
        fits: (List<String>, List<String>) -> Int?,
        fixedSide: (String) -> Boolean?,
    ): CompactArrangement? {
        val movable = entries.filter { fixedSide(it.itemKey) == null }
        if (movable.isEmpty() || movable.size > MAX_SEARCH) return null
        val fixedBefore = entries.filter { fixedSide(it.itemKey) == true }.map { it.itemKey }
        var best: CompactArrangement? = null
        var bestScore = Int.MAX_VALUE
        for (mask in 0 until (1 shl movable.size)) {
            val before = fixedBefore + movable.filterIndexed { i, _ -> (mask shr i) and 1 == 1 }.map { it.itemKey }
            val candidate = ordered(entries, before.toSet(), visible)
            val score = fits(candidate.before, candidate.after) ?: continue
            if (score < bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return best
    }

    private fun arrangeOnce(
        entries: List<CompactEntry>,
        anchor: CameraAnchor,
        maxCells: Int,
        balance: Boolean,
        cellWidth: (String) -> Float,
        leftExtraWidth: Float,
        previous: CompactArrangement,
        persistentOrder: Boolean,
        highFirst: Boolean,
    ): CompactArrangement {
        val base = entries.filterNot { it.companionOnly }
        val companions = entries.filter { it.companionOnly }
        var effective = base
        if (anchor == CameraAnchor.Center && companions.isNotEmpty()) {
            val alone = select(base, maxCells)
            if (alone.isNotEmpty() && alone.all { it.needsCompany }) effective = base + companions
        }
        val selected = select(effective, maxCells)
        if (selected.isEmpty()) return CompactArrangement.Empty

        val pinned = selected.filter { it.pinned }
        val dynamic = selected.filterNot { it.pinned }
        val visible = selected.map { it.itemKey }.toSet()

        if (anchor != CameraAnchor.Center && persistentOrder) {
            val byImportance = selected.sortedWith(compareBy({ it.priority }, { it.itemKey }))
            val sorted = if (anchor == CameraAnchor.Start) byImportance.reversed() else byImportance
            val cells = sorted.flatMap(::cellsOf)
            return if (anchor == CameraAnchor.Start) {
                CompactArrangement(emptyList(), cells, visible)
            } else {
                CompactArrangement(cells, emptyList(), visible)
            }
        }

        if (anchor != CameraAnchor.Center) {
            val cells = pinned.flatMap { it.cellKeys } + dynamic.flatMap { it.cellKeys.reversed() }
            return if (anchor == CameraAnchor.Start) {
                CompactArrangement(emptyList(), cells, visible)
            } else {
                CompactArrangement(cells, emptyList(), visible)
            }
        }

        val solo = selected.singleOrNull()
        if (solo != null && solo.soloCellKeys.isNotEmpty() && solo.cellKeys.size == 1) {
            if (solo.chip) return CompactArrangement(emptyList(), solo.soloCellKeys + solo.cellKeys, visible)
            return CompactArrangement(solo.soloCellKeys, solo.cellKeys, visible)
        }
        if (solo != null && !solo.pinned && solo.cellKeys.size == 2) {
            return CompactArrangement(listOf(solo.cellKeys[0]), listOf(solo.cellKeys[1]), visible)
        }

        val width = { e: CompactEntry -> e.cellKeys.sumOf { cellWidth(it).toDouble() }.toFloat() }
        if (persistentOrder) {
            val sequence = persistentSequence(selected, highFirst)
            val unit = { entry: CompactEntry -> if (balance) width(entry) else entry.cellKeys.size.toFloat() }
            val extra = if (balance) leftExtraWidth else ceil(leftExtraWidth.toDouble()).toFloat()
            return splitAt(sequence, persistentSplit(sequence, unit, extra), visible)
        }
        val defaultBefore = defaultBefore(pinned, dynamic, width, leftExtraWidth)
        val before = if (balance) balancedBefore(selected, defaultBefore, previous, width, leftExtraWidth) else defaultBefore
        return ordered(selected, before, visible)
    }

    private fun persistentSequence(
        entries: List<CompactEntry>,
        highFirst: Boolean,
    ): List<CompactEntry> {
        val byImportance = entries.sortedWith(compareBy({ it.priority }, { it.itemKey }))
        return if (highFirst) byImportance else byImportance.reversed()
    }

    private fun persistentSplit(
        sequence: List<CompactEntry>,
        width: (CompactEntry) -> Float,
        extra: Float,
    ): Int {
        var bestK = 0
        var bestGap = Float.MAX_VALUE
        var bestHeavy = true
        for (k in 0..sequence.size) {
            val left = extra + sequence.take(k).sumOf { width(it).toDouble() }.toFloat()
            val right = sequence.drop(k).sumOf { width(it).toDouble() }.toFloat()
            val gap = abs(left - right)
            val heavy = extra > 0f && left > right + WIDTH_EPSILON
            val better = when {
                gap < bestGap - WIDTH_EPSILON -> true
                gap > bestGap + WIDTH_EPSILON -> false
                else -> bestHeavy && !heavy
            }
            if (better) {
                bestK = k
                bestGap = gap
                bestHeavy = heavy
            }
        }
        return bestK
    }

    private fun persistentFitting(
        selected: List<CompactEntry>,
        visible: Set<String>,
        highFirst: Boolean,
        fits: (List<String>, List<String>) -> Int?,
    ): CompactArrangement? {
        val sequence = persistentSequence(selected, highFirst)
        var best: CompactArrangement? = null
        var bestScore = Int.MAX_VALUE
        for (k in 0..sequence.size) {
            val candidate = splitAt(sequence, k, visible)
            val score = fits(candidate.before, candidate.after) ?: continue
            if (score < bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return best
    }

    private fun splitAt(
        sequence: List<CompactEntry>,
        k: Int,
        visible: Set<String>,
    ): CompactArrangement =
        CompactArrangement(
            sequence.take(k).reversed().flatMap(::cellsOf),
            sequence.drop(k).flatMap(::cellsOf),
            visible,
        )

    private fun cellsOf(entry: CompactEntry): List<String> = if (entry.pinned) entry.cellKeys else entry.cellKeys.reversed()

    private fun defaultBefore(
        pinned: List<CompactEntry>,
        dynamic: List<CompactEntry>,
        width: (CompactEntry) -> Float,
        extra: Float,
    ): Set<String> {
        val before = mutableSetOf<String>()
        var left = extra
        var right = 0f
        fun put(entry: CompactEntry, toBefore: Boolean) {
            if (toBefore) {
                before += entry.itemKey
                left += width(entry)
            } else {
                right += width(entry)
            }
        }
        val multi = dynamic.filter { it.cellKeys.size > 1 }
        val single = dynamic.filter { it.cellKeys.size <= 1 }
        if (multi.isNotEmpty()) {
            put(multi[0], false)
            pinned.forEach { put(it, true) }
            multi.drop(1).forEach { put(it, true) }
            single.forEach { put(it, left <= right) }
        } else {
            pinned.forEachIndexed { i, entry -> put(entry, i != 1) }
            single.forEach { put(it, right > left) }
        }
        return before
    }

    private fun balancedBefore(
        entries: List<CompactEntry>,
        fallback: Set<String>,
        previous: CompactArrangement,
        width: (CompactEntry) -> Float,
        extra: Float,
    ): Set<String> {
        if (entries.size > MAX_SEARCH) return fallback
        val wasBefore = previous.before.toSet()
        val wasAfter = previous.after.toSet()
        val previousSide = entries.map { e ->
            when {
                e.cellKeys.isEmpty() -> null
                e.cellKeys.all { it in wasBefore } -> true
                e.cellKeys.all { it in wasAfter } -> false
                else -> null
            }
        }
        val reference = entries.mapIndexed { i, e -> previousSide[i] ?: (e.itemKey in fallback) }
        val referenceMask = reference.foldIndexed(0) { i, acc, b -> if (b) acc or (1 shl i) else acc }
        val widths = entries.map(width)
        val importance = entries.sortedByDescending { it.priority }.withIndex().associate { (i, e) -> e.itemKey to i + 1 }
        val weights = entries.map { importance.getValue(it.itemKey) }

        var bestMask = referenceMask
        var bestGap = Float.MAX_VALUE
        var bestHeavy = true
        var bestCost = Int.MAX_VALUE
        var referenceGap = Float.MAX_VALUE
        for (mask in 0 until (1 shl entries.size)) {
            var left = extra
            var right = 0f
            var cost = 0
            for (i in entries.indices) {
                val isBefore = (mask shr i) and 1 == 1
                if (isBefore) left += widths[i] else right += widths[i]
                if (isBefore != reference[i]) cost += weights[i]
            }
            val gap = abs(left - right)
            if (mask == referenceMask) referenceGap = gap
            val heavy = extra > 0f && left > right + WIDTH_EPSILON
            val better = when {
                gap < bestGap - WIDTH_EPSILON -> true
                gap > bestGap + WIDTH_EPSILON -> false
                heavy != bestHeavy -> !heavy
                else -> cost < bestCost
            }
            if (better) {
                bestMask = mask
                bestGap = gap
                bestHeavy = heavy
                bestCost = cost
            }
        }
        val mask = if (previousSide.all { it != null } && referenceGap <= bestGap + STICKY_WIDTH) referenceMask else bestMask
        return entries.filterIndexed { i, _ -> (mask shr i) and 1 == 1 }.map { it.itemKey }.toSet()
    }

    private fun ordered(
        entries: List<CompactEntry>,
        before: Set<String>,
        visible: Set<String>,
    ): CompactArrangement {
        val sorted = entries.sortedWith(compareBy<CompactEntry>({ it.chip }, { !it.pinned }, { it.priority }))
        fun cells(entry: CompactEntry) = if (entry.pinned) entry.cellKeys else entry.cellKeys.reversed()
        return CompactArrangement(
            sorted.filter { it.itemKey in before }.flatMap(::cells),
            sorted.filterNot { it.itemKey in before }.flatMap(::cells),
            visible,
        )
    }

    private fun select(entries: List<CompactEntry>, maxCells: Int): List<CompactEntry> {
        var used = 0
        val result = mutableListOf<CompactEntry>()
        for (entry in entries.sortedBy { it.priority }) {
            val size = entry.cellKeys.size
            if (used + size > maxCells) continue
            used += size
            result += entry
        }
        return result
    }
}
