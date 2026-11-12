package com.sameerasw.essentials.island.state

import androidx.compose.runtime.mutableIntStateOf
import java.util.concurrent.ConcurrentHashMap

object CompactCellWidths {
    private val widths = ConcurrentHashMap<String, Int>()

    @Volatile
    var onChange: (() -> Unit)? = null

    fun record(cellKeys: List<String>, unitWidth: Int) {
        if (cellKeys.isEmpty() || unitWidth <= 0) return
        val n = cellKeys.size
        val share = unitWidth / n
        val remainder = unitWidth - share * n
        val sorted = cellKeys.sorted()
        var changed = false
        sorted.forEachIndexed { i, key ->
            val value = share + if (i == 0) remainder else 0
            val previous = widths[key]
            if (previous == null || kotlin.math.abs(previous - value) > JITTER_PX / n) {
                widths[key] = value
                changed = true
            }
        }
        if (changed) onChange?.invoke()
    }

    fun get(cellKey: String): Int? = widths[cellKey]

    @Volatile
    var bubbleExtent: Int = 0
        set(value) {
            val changed = kotlin.math.abs(field - value) > JITTER_PX
            field = value
            if (changed) onChange?.invoke()
        }

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
