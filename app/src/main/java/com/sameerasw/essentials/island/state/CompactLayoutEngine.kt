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

    fun arrange(
        entries: List<CompactEntry>,
        anchor: CameraAnchor = CameraAnchor.Center,
        maxCells: Int = MAX_CELLS,
        leftExtra: Int = 0,
        balance: Boolean = false,
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
                if (before.size + leftExtra <= after.size) {
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
                if (after.size <= before.size + leftExtra) {
                    after += it.cellKeys
                } else {
                    before += it.cellKeys
                    movable += it
                }
            }
        }

        if (balance && leftExtra > 0) {
            for (entry in movable.sortedByDescending { it.priority }) {
                val size = entry.cellKeys.size
                val left = before.size + leftExtra
                val right = after.size
                val gap = kotlin.math.abs(left - right)
                val movedGap = kotlin.math.abs((left - size) - (right + size))
                if (movedGap > gap || (movedGap == gap && left <= right)) break
                before.removeAll(entry.cellKeys.toSet())
                after.addAll(0, if (size == 2) entry.cellKeys.reversed() else entry.cellKeys)
            }
        }

        if (balance && leftExtra >= WIDE_BUBBLE_CELLS) {
            for (entry in pinned.filter { e -> e.cellKeys.all { it in before } }.sortedByDescending { it.priority }) {
                val size = entry.cellKeys.size
                val left = before.size + leftExtra
                val right = after.size
                val gap = kotlin.math.abs(left - right)
                val movedGap = kotlin.math.abs((left - size) - (right + size))
                if (movedGap >= gap) break
                before.removeAll(entry.cellKeys.toSet())
                after.addAll(0, if (size == 2) entry.cellKeys.reversed() else entry.cellKeys)
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
