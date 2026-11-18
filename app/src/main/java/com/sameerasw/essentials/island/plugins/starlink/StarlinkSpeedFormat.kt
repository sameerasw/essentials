package com.sameerasw.essentials.island.plugins.starlink

import kotlin.math.ceil

enum class SpeedUnit(val label: String) { Kbps("kbps"), Mbps("mbps"), Gbps("gbps") }

enum class SpeedDirection { Down, Up }

class CompactSpeed(val text: String, val unit: SpeedUnit, val direction: SpeedDirection? = null)

object StarlinkSpeedFormat {
    const val MODE_DOWN = "down"
    const val MODE_UP = "up"
    const val MODE_DYNAMIC = "dynamic"
    const val MODE_COMBINED = "combined"

    const val UNIT_AUTO = "auto"
    const val UNIT_MBPS = "mbps"

    fun forMode(mode: String?, downBps: Float?, upBps: Float?, fixedMbps: Boolean = false): CompactSpeed? = when (mode) {
        MODE_UP -> upBps?.let { compact(it, SpeedDirection.Up, fixedMbps) }
        MODE_DYNAMIC -> when {
            downBps != null && upBps != null ->
                if (upBps > downBps) compact(upBps, SpeedDirection.Up, fixedMbps) else compact(downBps, SpeedDirection.Down, fixedMbps)
            downBps != null -> compact(downBps, SpeedDirection.Down, fixedMbps)
            upBps != null -> compact(upBps, SpeedDirection.Up, fixedMbps)
            else -> null
        }
        MODE_COMBINED -> combined(downBps, upBps, fixedMbps)
        else -> downBps?.let { compact(it, SpeedDirection.Down, fixedMbps) }
    }

    private fun combined(downBps: Float?, upBps: Float?, fixedMbps: Boolean): CompactSpeed? {
        if (downBps == null || upBps == null) return (downBps ?: upBps)?.let { compact(it, null, fixedMbps) }
        val down = compact(downBps, null, fixedMbps)
        val up = compact(upBps, null, fixedMbps)
        return when {
            down.unit == up.unit -> CompactSpeed("${down.text}/${up.text}", down.unit)
            downBps >= upBps -> down
            else -> up
        }
    }

    // Two digits at most, except 100-500 mbps. Anything larger rolls into the next unit as 0.x
    fun compact(bps: Float, direction: SpeedDirection? = null, fixedMbps: Boolean = false): CompactSpeed {
        val kbps = bps.coerceAtLeast(0f) / 1_000f
        val mbps = kbps / 1_000f
        val gbps = mbps / 1_000f
        return when {
            fixedMbps -> CompactSpeed(number(mbps), SpeedUnit.Mbps, direction)
            mbps > 500f -> CompactSpeed(number(gbps), SpeedUnit.Gbps, direction)
            mbps >= 100f -> CompactSpeed(number(mbps), SpeedUnit.Mbps, direction)
            mbps >= 1f || kbps > 99f -> CompactSpeed(number(mbps), SpeedUnit.Mbps, direction)
            else -> CompactSpeed(number(kbps), SpeedUnit.Kbps, direction)
        }
    }

    private fun number(value: Float): String {
        if (value >= 10f) return ceil(value - 1e-3f).toInt().toString()
        val tenths = ceil(value * 10f - 1e-3f).toInt()
        if (tenths >= 100) return (tenths / 10).toString()
        return if (tenths <= 0) "0" else "${tenths / 10}.${tenths % 10}"
    }
}
