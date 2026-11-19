package com.sameerasw.essentials.island.state

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

object MarqueeProgress {
    private const val MAX_WAIT_MS = 60_000L

    private val endings = ConcurrentHashMap<Any, Long>()

    fun start(owner: Any, durationMs: Long) {
        endings[owner] = SystemClock.uptimeMillis() + durationMs
    }

    fun clear(owner: Any) {
        endings.remove(owner)
    }

    fun remainingMs(): Long {
        val latest = endings.values.maxOrNull() ?: return 0L
        return (latest - SystemClock.uptimeMillis()).coerceIn(0L, MAX_WAIT_MS)
    }
}
