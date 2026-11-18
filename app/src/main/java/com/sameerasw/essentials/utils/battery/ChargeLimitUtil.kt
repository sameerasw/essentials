package com.sameerasw.essentials.utils.battery

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import com.sameerasw.essentials.utils.DeviceUtils
import java.util.concurrent.atomic.AtomicBoolean

object ChargeLimitUtil {
    private const val SHIELD_LEVEL = 78
    private const val CACHE_MS = 15_000L

    @Volatile
    private var cached = false

    @Volatile
    private var stamp = 0L

    private val refreshing = AtomicBoolean(false)

    private fun compute(context: Context): Boolean {
        val value = ChargingModeUtil.getMode(context) == ChargingMode.LIMITED
        cached = value
        stamp = SystemClock.elapsedRealtime()
        return value
    }

    fun isLimitActive(context: Context): Boolean {
        if (!DeviceUtils.isGoogleDevice()) return false
        val fresh = stamp != 0L && SystemClock.elapsedRealtime() - stamp < CACHE_MS
        if (fresh) return cached
        if (Looper.myLooper() != Looper.getMainLooper()) return compute(context)
        if (refreshing.compareAndSet(false, true)) {
            val app = context.applicationContext
            Thread {
                try {
                    compute(app)
                } finally {
                    refreshing.set(false)
                }
            }.start()
        }
        return cached
    }

    fun isShielded(
        context: Context,
        level: Int,
        charging: Boolean,
    ): Boolean = charging && level >= SHIELD_LEVEL && isLimitActive(context)
}
