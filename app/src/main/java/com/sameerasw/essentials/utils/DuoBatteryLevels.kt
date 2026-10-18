package com.sameerasw.essentials.utils

object DuoBatteryLevels {
    const val DEFAULT_LOW = 20
    const val DEFAULT_CRITICAL = 10
    const val MIN = 1
    const val MAX = 99

    fun withLow(
        low: Int,
        critical: Int,
    ): Pair<Int, Int> {
        val newLow = low.coerceIn(MIN + 1, MAX)
        return newLow to critical.coerceIn(MIN, newLow - 1)
    }

    fun withCritical(
        critical: Int,
        low: Int,
    ): Pair<Int, Int> {
        val newCritical = critical.coerceIn(MIN, MAX - 1)
        return low.coerceIn(newCritical + 1, MAX) to newCritical
    }
}
