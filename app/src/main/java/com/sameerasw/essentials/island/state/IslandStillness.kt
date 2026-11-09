package com.sameerasw.essentials.island.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

object IslandStillness {
    const val IDLE_MS = 5_000L

    private val _still = MutableStateFlow(false)
    val still: StateFlow<Boolean> = _still

    val isStill: Boolean get() = _still.value

    internal fun set(value: Boolean): Boolean {
        if (_still.value == value) return false
        _still.value = value
        return true
    }

    fun msToNextMinute(now: Long = System.currentTimeMillis()): Long = 60_000L - now % 60_000L + 2L

    fun msToMinuteChange(millis: Long, countDown: Boolean): Long {
        val rem = millis.coerceAtLeast(0L) % 60_000L
        return (if (countDown) rem.takeIf { it > 0L } ?: 60_000L else 60_000L - rem) + 2L
    }

    suspend fun awaitTick(activeDelayMs: Long, stillDelayMs: Long = msToNextMinute()) {
        val current = _still.value
        val wait = if (current) stillDelayMs else activeDelayMs
        withTimeoutOrNull(wait.coerceAtLeast(1L)) { _still.first { it != current } }
    }

    fun coarse(millis: Long, countDown: Boolean): String {
        val minutes = if (countDown) (millis.coerceAtLeast(0L) + 59_999L) / 60_000L else millis.coerceAtLeast(0L) / 60_000L
        if (minutes <= 0L) return "<1m"
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
