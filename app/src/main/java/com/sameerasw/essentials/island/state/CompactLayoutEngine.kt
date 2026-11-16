package com.sameerasw.essentials.island.state

data class CompactEntry(
    val itemKey: String,
    val priority: Int,
    val pinned: Boolean,
    // [icon, value] — icon always ends up on the outer edge.
    val cellKeys: List<String>,
    val soloCellKeys: List<String> = emptyList(),
    val needsCompany: Boolean = false,
    val companionOnly: Boolean = false,
    val keepTogether: Boolean = false,
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

    // A bubble this wide is worth moving pinned cells across the camera for
    private const val WIDE_BUBBLE_CELLS = 2
    private const val WIDTH_EPSILON = 0.25f
    private const val STICKY_WIDTH = 1f

    fun arrange(
        entries: List<CompactEntry>,
        anchor: CameraAnchor = CameraAnchor.Center,
        maxCells: Int = MAX_CELLS,
        leftExtra: Int = 0,
        balance: Boolean = false,
        fits: ((before: List<String>, after: List<String>) -> Int?)? = null,
        cellWidth: (String) -> Float = { 1f },
        leftExtraWidth: Float = leftExtra.toFloat(),
        previous: CompactArrangement = CompactArrangement.Empty,
    ): CompactArrangement {
        fun attempt(pool: List<CompactEntry>): CompactArrangement? {
            val result = arrangeOnce(pool, anchor, maxCells, leftExtra, balance, cellWidth, leftExtraWidth, previous)
            if (fits == null || fits(result.before, result.after) != null) return result
            if (anchor != CameraAnchor.Center) return null
            val selected = pool.filter { it.itemKey in result.visibleItems }
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
            val shown = arrangeOnce(pool, anchor, maxCells, leftExtra, balance, cellWidth, leftExtraWidth, previous)
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
        val pinned = selected.filter { it.pinned }
        val dynamic = selected.filterNot { it.pinned }.sortedBy { it.priority }
        val pinnedCells = pinned.flatMap { it.cellKeys }.toSet()
        return splitSearch(base.before.filter { it in pinnedCells }, base.after.filter { it in pinnedCells }, dynamic, base, fits)
            ?: splitSearch(emptyList(), emptyList(), pinned + dynamic, base, fits)
    }

    private fun splitSearch(
        fixedBefore: List<String>,
        fixedAfter: List<String>,
        movable: List<CompactEntry>,
        base: CompactArrangement,
        fits: (List<String>, List<String>) -> Int?,
    ): CompactArrangement? {
        if (movable.isEmpty() || movable.size > 6) return null
        var best: CompactArrangement? = null
        var bestScore = Int.MAX_VALUE
        for (mask in 0 until (1 shl movable.size)) {
            val before = fixedBefore.toMutableList()
            val after = fixedAfter.toMutableList()
            movable.forEachIndexed { i, entry ->
                val cells = if (entry.cellKeys.size == 2) entry.cellKeys.reversed() else entry.cellKeys
                if ((mask shr i) and 1 == 1) before += cells else after += cells
            }
            val score = fits(before, after) ?: continue
            if (score < bestScore) {
                bestScore = score
                best = CompactArrangement(before, after, base.visibleItems)
            }
        }
        return best
    }

    private fun arrangeOnce(
        entries: List<CompactEntry>,
        anchor: CameraAnchor,
        maxCells: Int,
        leftExtra: Int,
        balance: Boolean,
        cellWidth: (String) -> Float,
        leftExtraWidth: Float,
        previous: CompactArrangement,
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

        if (anchor != CameraAnchor.Center) {
            val cells = pinned.flatMap { it.cellKeys } + dynamic.flatMap { it.cellKeys.reversed() }
            return if (anchor == CameraAnchor.Start) {
                CompactArrangement(emptyList(), cells, visible)
            } else {
                CompactArrangement(cells, emptyList(), visible)
            }
        }

        val before = mutableListOf<String>()
        val after = mutableListOf<String>()
        fun width(cells: List<String>) = cells.sumOf { cellWidth(it).toDouble() }.toFloat()
        fun left() = width(before) + leftExtraWidth
        fun right() = width(after)
        val twoCell = dynamic.filter { it.cellKeys.size == 2 }
        val oneCell = dynamic.filter { it.cellKeys.size == 1 }

        val movable = mutableListOf<CompactEntry>()
        if (twoCell.isNotEmpty()) {
            after += twoCell[0].cellKeys.reversed()
            pinned.forEach { before += it.cellKeys }
            twoCell.drop(1).forEach {
                before += it.cellKeys.reversed()
                movable += it
            }
            oneCell.forEach {
                if (left() <= right()) {
                    before += it.cellKeys
                    movable += it
                } else {
                    after += it.cellKeys
                }
            }
        } else {
            pinned.getOrNull(0)?.let { before += it.cellKeys }
            pinned.getOrNull(1)?.let { after += it.cellKeys }
            oneCell.forEach {
                if (right() <= left()) {
                    after += it.cellKeys
                } else {
                    before += it.cellKeys
                    movable += it
                }
            }
        }

        if (balance && leftExtraWidth > 0f) {
            for (entry in movable.sortedByDescending { it.priority }) {
                val size = entry.cellKeys.size
                val w = width(entry.cellKeys)
                val left = left()
                val right = right()
                val gap = kotlin.math.abs(left - right)
                val movedGap = kotlin.math.abs((left - w) - (right + w))
                if (movedGap > gap + WIDTH_EPSILON || (movedGap >= gap - WIDTH_EPSILON && left <= right)) break
                before.removeAll(entry.cellKeys.toSet())
                after.addAll(0, if (size == 2) entry.cellKeys.reversed() else entry.cellKeys)
            }
        }

        if (balance && leftExtraWidth >= WIDE_BUBBLE_CELLS) {
            for (entry in pinned.filter { e -> e.cellKeys.all { it in before } }.sortedByDescending { it.priority }) {
                val size = entry.cellKeys.size
                val w = width(entry.cellKeys)
                val left = left()
                val right = right()
                val gap = kotlin.math.abs(left - right)
                val movedGap = kotlin.math.abs((left - w) - (right + w))
                if (movedGap >= gap - WIDTH_EPSILON) break
                before.removeAll(entry.cellKeys.toSet())
                after.addAll(0, if (size == 2) entry.cellKeys.reversed() else entry.cellKeys)
            }
        }

        if (balance && (previous.before.isNotEmpty() || previous.after.isNotEmpty())) {
            val wasBefore = previous.before.toSet()
            val wasAfter = previous.after.toSet()
            for (entry in dynamic) {
                val cells = if (entry.cellKeys.size == 2) entry.cellKeys.reversed() else entry.cellKeys
                val nowBefore = entry.cellKeys.all { it in before }
                val stickBefore = entry.cellKeys.all { it in wasBefore }
                val stickAfter = entry.cellKeys.all { it in wasAfter }
                if (!(nowBefore && stickAfter) && !(!nowBefore && stickBefore)) continue
                val w = width(entry.cellKeys)
                val gap = kotlin.math.abs(left() - right())
                val sign = if (nowBefore) -1 else 1
                val revertedGap = kotlin.math.abs((left() + sign * w) - (right() - sign * w))
                if (revertedGap > gap + STICKY_WIDTH) continue
                if (nowBefore) {
                    before.removeAll(entry.cellKeys.toSet())
                    after.addAll(0, cells)
                } else {
                    after.removeAll(entry.cellKeys.toSet())
                    before += cells
                }
            }
        }

        val solo = selected.singleOrNull()
        if (solo != null && solo.soloCellKeys.isNotEmpty() && solo.cellKeys.size == 1) {
            if (solo.keepTogether) return CompactArrangement(emptyList(), solo.soloCellKeys + solo.cellKeys, visible)
            return CompactArrangement(solo.soloCellKeys, solo.cellKeys, visible)
        }

        if (before.isEmpty() && selected.size == 1 && after.size == 2 && selected.first().cellKeys.size == 2) {
            val item = selected.first()
            return CompactArrangement(listOf(item.cellKeys[0]), listOf(item.cellKeys[1]), visible)
        }
        return CompactArrangement(before, after, visible)
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
