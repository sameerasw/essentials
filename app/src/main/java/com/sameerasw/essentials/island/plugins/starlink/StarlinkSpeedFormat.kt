package com.sameerasw.essentials.island.plugins.starlink

import kotlin.math.floor

enum class SpeedUnit(val label: String) { Kbps("kbps"), Mbps("mbps"), Gbps("gbps") }

enum class SpeedDirection { Down, Up }

class CompactSpeed(val text: String, val unit: SpeedUnit, val direction: SpeedDirection? = null)

object StarlinkSpeedFormat {
    const val MODE_DOWN = "down"
    const val MODE_UP = "up"
    const val MODE_DYNAMIC = "dynamic"
    const val MODE_COMBINED = "combined"

    fun forMode(mode: String?, downBps: Float?, upBps: Float?): CompactSpeed? = when (mode) {
        MODE_UP -> upBps?.let { compact(it, SpeedDirection.Up) }
        MODE_DYNAMIC -> when {
            downBps != null && upBps != null -> if (upBps > downBps) compact(upBps, SpeedDirection.Up) else compact(downBps, SpeedDirection.Down)
            downBps != null -> compact(downBps, SpeedDirection.Down)
            upBps != null -> compact(upBps, SpeedDirection.Up)
            else -> null
        }
        MODE_COMBINED -> combined(downBps, upBps)
        else -> downBps?.let { compact(it, SpeedDirection.Down) }
    }

    private fun combined(downBps: Float?, upBps: Float?): CompactSpeed? {
        if (downBps == null || upBps == null) return (downBps ?: upBps)?.let { compact(it) }
        val down = compact(downBps)
        val up = compact(upBps)
        return when {
            down.unit == up.unit -> CompactSpeed("${down.text}/${up.text}", down.unit)
            downBps >= upBps -> down
            else -> up
        }
    }

    // Two digits at most, except 100-500 mbps. Anything larger rolls into the next unit as 0.x
    fun compact(bps: Float, direction: SpeedDirection? = null): CompactSpeed {
        val kbps = bps.coerceAtLeast(0f) / 1_000f
        val mbps = kbps / 1_000f
        val gbps = mbps / 1_000f
        return when {
            mbps > 500f -> CompactSpeed(number(gbps), SpeedUnit.Gbps, direction)
            mbps >= 100f -> CompactSpeed(number(mbps), SpeedUnit.Mbps, direction)
            mbps >= 1f || kbps >= 100f -> CompactSpeed(number(mbps), SpeedUnit.Mbps, direction)
            else -> CompactSpeed(number(kbps), SpeedUnit.Kbps, direction)
        }
    }

    private fun number(value: Float): String {
        if (value >= 10f) return floor(value).toInt().toString()
        val tenths = floor(value * 10f + 1e-4f).toInt()
        return if (tenths == 0) "0" else "${tenths / 10}.${tenths % 10}"
    }
}
