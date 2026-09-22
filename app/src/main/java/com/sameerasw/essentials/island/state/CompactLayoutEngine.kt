package com.sameerasw.essentials.island.state

data class CompactEntry(
    val itemKey: String,
    val priority: Int,
    val pinned: Boolean,
    // [icon, value] — icon always ends up on the outer edge.
    val cellKeys: List<String>,
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

    fun arrange(
        entries: List<CompactEntry>,
        anchor: CameraAnchor = CameraAnchor.Center,
        maxCells: Int = MAX_CELLS,
    ): CompactArrangement {
        val selected = select(entries, maxCells)
        if (selected.isEmpty()) return CompactArrangement.Empty

        val pinned = selected.filter { it.pinned }
        val dynamic = selected.filterNot { it.pinned }
        val visible = selected.map { it.itemKey }.toSet()

        if (anchor != CameraAnchor.Center) {
            val cells = pinned.flatMap { it.cellKeys } + dynamic.flatMap { it.cellKeys.reversed() }
            return CompactArrangement(emptyList(), cells, visible)
        }

        val before = mutableListOf<String>()
        val after = mutableListOf<String>()
        val twoCell = dynamic.filter { it.cellKeys.size == 2 }
        val oneCell = dynamic.filter { it.cellKeys.size == 1 }

        if (twoCell.isNotEmpty()) {
            after += twoCell[0].cellKeys.reversed()
            pinned.forEach { before += it.cellKeys }
            twoCell.drop(1).forEach { before += it.cellKeys.reversed() }
            oneCell.forEach { if (before.size <= after.size) before += it.cellKeys else after += it.cellKeys }
        } else {
            pinned.getOrNull(0)?.let { before += it.cellKeys }
            pinned.getOrNull(1)?.let { after += it.cellKeys }
            oneCell.forEach { if (after.size <= before.size) after += it.cellKeys else before += it.cellKeys }
        }

        if (before.isEmpty() && selected.size == 1 && after.size == 2) {
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
