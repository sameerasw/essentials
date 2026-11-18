package com.sameerasw.essentials.island.plugins.starlink

enum class StarlinkState { Unknown, Connected, Searching, Booting }

enum class StarlinkAlert { MotorsStuck, ThermalShutdown, ThermalThrottle, UnexpectedLocation, MastNotNearVertical, SlowEthernet }

data class StarlinkStatus(
    val state: StarlinkState = StarlinkState.Unknown,
    val downlinkBps: Float? = null,
    val uplinkBps: Float? = null,
    val pingMs: Float? = null,
    val dropRate: Float? = null,
    val currentlyObstructed: Boolean = false,
    val obstructionFraction: Float? = null,
    val uptimeS: Long? = null,
    val hardwareVersion: String? = null,
    val softwareVersion: String? = null,
    val alerts: Set<StarlinkAlert> = emptySet(),
) {
    val degraded: Boolean get() = currentlyObstructed || alerts.isNotEmpty() || (dropRate ?: 0f) >= 0.05f
}
