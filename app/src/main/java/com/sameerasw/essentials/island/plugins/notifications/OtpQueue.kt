package com.sameerasw.essentials.island.plugins.notifications

internal fun <T> MutableList<T>.removeKeepingCurrent(
    index: Int,
    currentIndex: Int,
): Int {
    if (index !in indices) return currentIndex
    removeAt(index)
    val shifted = if (index < currentIndex) currentIndex - 1 else currentIndex
    return shifted.coerceIn(0, (size - 1).coerceAtLeast(0))
}
