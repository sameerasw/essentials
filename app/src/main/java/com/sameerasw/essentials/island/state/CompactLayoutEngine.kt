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
    // Always placed right of the camera, at the outer end
    val endSide: Boolean = false,
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
        maxItems: Int = Int.MAX_VALUE,
        mirror: Boolean = false,
        maxWidth: Float = Float.MAX_VALUE,
        widthOf: (CompactEntry) -> Float = { it.cellKeys.size.toFloat() },
        // Widest each side of the camera may get, bubbles included
        sideMax: Float = Float.MAX_VALUE,
        // Cells of a bubble right of the island, already netted out of leftExtra
        endExtra: Int = 0,
    ): CompactArrangement {
        val base = entries.filterNot { it.companionOnly }
        val companions = entries.filter { it.companionOnly }
        var effective = base
        if (anchor == CameraAnchor.Center && companions.isNotEmpty()) {
            val alone = select(base, maxCells, maxItems, maxWidth, widthOf)
            if (alone.isNotEmpty() && alone.all { it.needsCompany }) effective = base + companions
        }
        val selected = select(effective, maxCells, maxItems, maxWidth, widthOf)
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
        val movableAfter = mutableListOf<CompactEntry>()
        if (twoCell.isNotEmpty()) {
            after += twoCell[0].cellKeys.reversed()
            movableAfter += twoCell[0]
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
                    movableAfter += it
                }
            }
        } else {
            pinned.getOrNull(0)?.let { before += it.cellKeys }
            pinned.getOrNull(1)?.let { after += it.cellKeys }
            oneCell.forEach {
                if (after.size <= before.size + leftExtra) {
                    after += it.cellKeys
                    movableAfter += it
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

        val rightExtra = -leftExtra
        if (balance && mirror && rightExtra > 0) {
            for (entry in movableAfter.sortedByDescending { it.priority }) {
                val size = entry.cellKeys.size
                val left = before.size
                val right = after.size + rightExtra
                val gap = kotlin.math.abs(left - right)
                val movedGap = kotlin.math.abs((left + size) - (right - size))
                if (movedGap > gap || (movedGap == gap && right <= left)) break
                after.removeAll(entry.cellKeys.toSet())
                before.addAll(0, if (size == 2) entry.cellKeys.reversed() else entry.cellKeys)
            }
        }

        if (balance && mirror && rightExtra >= WIDE_BUBBLE_CELLS) {
            for (entry in pinned.filter { e -> e.cellKeys.all { it in after } }.sortedByDescending { it.priority }) {
                val size = entry.cellKeys.size
                val left = before.size
                val right = after.size + rightExtra
                val gap = kotlin.math.abs(left - right)
                val movedGap = kotlin.math.abs((left + size) - (right - size))
                if (movedGap >= gap) break
                after.removeAll(entry.cellKeys.toSet())
                before.addAll(0, if (size == 2) entry.cellKeys.reversed() else entry.cellKeys)
            }
        }

        if (selected.any { it.keepTogether }) groupChips(before, after, selected, leftExtra)

        val dropped = arrangeByWidth(before, after, selected, leftExtra + endExtra, endExtra, sideMax, widthOf)
        return finish(CompactArrangement(before, after, visible - dropped), selected.filterNot { it.itemKey in dropped })
    }

    private fun finish(arrangement: CompactArrangement, selected: List<CompactEntry>): CompactArrangement {
        val (before, after, visible) = arrangement
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

    internal fun arrangeByWidth(
        before: MutableList<String>,
        after: MutableList<String>,
        entries: List<CompactEntry>,
        leftExtra: Int,
        rightExtra: Int,
        sideMax: Float,
        widthOf: (CompactEntry) -> Float,
    ): Set<String> {
        val byCell = entries.flatMap { e -> e.cellKeys.map { it to e } }.toMap()
        fun blocks(side: List<String>) = side.mapNotNull { byCell[it] }.distinct()
        fun width(side: List<String>, extra: Int) = extra + blocks(side).sumOf { widthOf(it).toDouble() }.toFloat()
        fun narrows(l: Float, r: Float, shift: Float) = kotlin.math.abs((l + shift) - (r - shift)) < kotlin.math.abs(l - r)
        fun move(e: CompactEntry, from: MutableList<String>, to: MutableList<String>) {
            val cells = from.filter { byCell[it] == e }
            from.removeAll(cells.toSet())
            when {
                to === after -> to.addAll(after.count { byCell[it]?.endSide != true }, cells)
                e.keepTogether -> to.addAll(before.indexOfLast { byCell[it]?.keepTogether == true } + 1, cells)
                else -> to += cells
            }
        }
        blocks(before).filter { it.endSide && !it.pinned }.forEach { move(it, before, after) }
        blocks(after).filter { it.endSide && !it.pinned }.forEach {
            val cells = after.filter { c -> byCell[c] == it }
            after.removeAll(cells.toSet())
            after += cells
        }
        if (entries.any { it.keepTogether && !it.endSide && !it.pinned }) {
            blocks(before).filter { it.keepTogether && !it.pinned }.forEach { move(it, before, before) }
            blocks(after).filter { it.keepTogether && !it.endSide && !it.pinned }.forEach { move(it, after, before) }
            blocks(before).filter { !it.keepTogether && !it.pinned }.forEach { move(it, before, after) }
            for (e in blocks(after).filter { !it.keepTogether && !it.endSide && !it.pinned }.sortedByDescending { it.priority }) {
                if (!narrows(width(before, leftExtra), width(after, rightExtra), widthOf(e))) break
                move(e, after, before)
            }
        }
        for (e in blocks(before).filter { it.pinned && !it.keepTogether }.sortedByDescending { it.priority }) {
            val r = width(after, rightExtra)
            if (r + widthOf(e) > sideMax || !narrows(width(before, leftExtra), r, -widthOf(e))) continue
            move(e, before, after)
        }
        val dropped = mutableSetOf<String>()
        for (fromBefore in listOf(true, false)) {
            val from = if (fromBefore) before else after
            val to = if (fromBefore) after else before
            val fromExtra = if (fromBefore) leftExtra else rightExtra
            val toExtra = if (fromBefore) rightExtra else leftExtra
            while (width(from, fromExtra) > sideMax) {
                val candidates = blocks(from).filterNot { it.pinned }
                val e = (candidates.filterNot { it.endSide }.ifEmpty { candidates }).maxByOrNull { it.priority } ?: break
                if (!e.endSide && width(to, toExtra) + widthOf(e) <= sideMax) {
                    move(e, from, to)
                } else {
                    from.removeAll(e.cellKeys.toSet())
                    dropped += e.itemKey
                }
            }
        }
        return dropped
    }

    internal fun groupChips(before: MutableList<String>, after: MutableList<String>, entries: List<CompactEntry>, leftExtra: Int) {
        val byCell = entries.flatMap { e -> e.cellKeys.map { it to e } }.toMap()
        fun blocks(side: List<String>) = side.mapNotNull { byCell[it] }.distinct()
        fun cellsOf(side: List<String>, entry: CompactEntry) = side.filter { byCell[it] == entry }
        fun chipCells(side: List<String>) = side.count { byCell[it]?.keepTogether == true }
        fun pinnedChip(side: List<String>) = blocks(side).any { it.keepTogether && it.pinned }
        fun chipRunEnd(side: List<String>) = side.indexOfLast { byCell[it]?.keepTogether == true } + 1

        val gatherBefore = if (pinnedChip(before) != pinnedChip(after)) pinnedChip(before) else chipCells(before) >= chipCells(after)
        val target = if (gatherBefore) before else after
        val source = if (gatherBefore) after else before
        if (chipCells(target) > 0) {
            for (chip in blocks(source).filter { it.keepTogether && !it.pinned }) {
                val chipCellKeys = cellsOf(source, chip)
                val swap = blocks(target).firstOrNull { !it.keepTogether && !it.pinned && it.cellKeys.size == chip.cellKeys.size }
                if (swap != null) {
                    val plain = cellsOf(target, swap)
                    val at = source.indexOf(chipCellKeys.first())
                    source.removeAll(chipCellKeys.toSet())
                    source.addAll(at, plain)
                    target.removeAll(plain.toSet())
                } else {
                    source.removeAll(chipCellKeys.toSet())
                }
                target.addAll(chipRunEnd(target), chipCellKeys)
            }
        }

        fun breaker(side: List<String>): CompactEntry? {
            val order = blocks(side)
            val first = order.indexOfFirst { it.keepTogether }
            val last = order.indexOfLast { it.keepTogether }
            if (first < 0 || last <= first) return null
            return order.subList(first + 1, last).firstOrNull { !it.keepTogether && !it.pinned }
        }
        fun regroup(side: MutableList<String>) {
            val order = blocks(side)
            val firstChip = order.indexOfFirst { it.keepTogether }
            if (firstChip < 0) return
            val rest = order.drop(firstChip)
            if (rest.any { !it.keepTogether && it.pinned }) return
            val arranged = order.take(firstChip) + rest.filter { it.keepTogether } + rest.filterNot { it.keepTogether }
            val cells = arranged.flatMap { cellsOf(side, it) }
            side.clear()
            side += cells
        }
        repeat(entries.size) {
            for (fromBefore in listOf(true, false)) {
                val from = if (fromBefore) before else after
                val to = if (fromBefore) after else before
                val entry = breaker(from) ?: continue
                val size = entry.cellKeys.size
                val left = before.size + leftExtra
                val right = after.size
                val gap = kotlin.math.abs(left - right)
                val movedGap = if (fromBefore) kotlin.math.abs((left - size) - (right + size)) else kotlin.math.abs((left + size) - (right - size))
                if (movedGap <= gap) {
                    val cells = cellsOf(from, entry)
                    from.removeAll(cells.toSet())
                    to += cells
                } else {
                    regroup(from)
                }
            }
        }
        regroup(before)
        regroup(after)
    }

    private fun select(
        entries: List<CompactEntry>,
        maxCells: Int,
        maxItems: Int,
        maxWidth: Float = Float.MAX_VALUE,
        widthOf: (CompactEntry) -> Float = { it.cellKeys.size.toFloat() },
    ): List<CompactEntry> {
        var used = 0
        var dynamic = 0
        var width = entries.filter { it.pinned }.sumOf { widthOf(it).toDouble() }.toFloat()
        val result = mutableListOf<CompactEntry>()
        for (entry in entries.sortedBy { it.priority }) {
            val size = entry.cellKeys.size
            if (used + size > maxCells) continue
            if (!entry.pinned && dynamic >= maxItems) continue
            val w = if (entry.pinned) 0f else widthOf(entry)
            if (!entry.pinned && width + w > maxWidth) continue
            used += size
            width += w
            if (!entry.pinned) dynamic++
            result += entry
        }
        return result
    }
}
