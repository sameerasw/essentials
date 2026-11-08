package com.sameerasw.essentials.island.state

import androidx.compose.runtime.mutableIntStateOf
import java.util.concurrent.ConcurrentHashMap

object CompactCellWidths {
    private val widths = ConcurrentHashMap<String, Int>()

    fun record(cellKeys: List<String>, unitWidth: Int) {
        if (cellKeys.isEmpty() || unitWidth <= 0) return
        val previous = widths[cellKeys[0]]
        if (previous != null && kotlin.math.abs(previous - unitWidth) <= JITTER_PX) return
        widths[cellKeys[0]] = unitWidth
        cellKeys.drop(1).forEach { widths[it] = 0 }
    }

    fun get(cellKey: String): Int? = widths[cellKey]

    @Volatile
    var bubbleExtent: Int = 0

    private const val JITTER_PX = 4
}

object CompactSideLimits {
    val left = mutableIntStateOf(Int.MAX_VALUE)
    val right = mutableIntStateOf(Int.MAX_VALUE)

    fun set(leftPx: Int, rightPx: Int) {
        if (left.intValue != leftPx) left.intValue = leftPx
        if (right.intValue != rightPx) right.intValue = rightPx
    }
}
