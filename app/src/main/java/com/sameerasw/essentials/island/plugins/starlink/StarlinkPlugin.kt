package com.sameerasw.essentials.island.plugins.starlink

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPluginContext
import com.sameerasw.essentials.island.model.IslandStage
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.state.IslandItemsState
import com.sameerasw.essentials.island.state.IslandPresence
import com.sameerasw.essentials.island.state.IslandStillness
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.islandTextColor
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.island.ui.components.islandAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class StarlinkPlugin : BaseIslandPlugin() {
    override val id = "starlink"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_STARLINK,
        SettingsRepository.KEY_ISLAND_STARLINK_COMPACT,
        SettingsRepository.KEY_ISLAND_STARLINK_SPEEDS,
        SettingsRepository.KEY_ISLAND_STARLINK_SPEED_MODE,
        SettingsRepository.KEY_ISLAND_STARLINK_SPEED_UNIT,
        SettingsRepository.KEY_ISLAND_STARLINK_HIDE_UNIT,
        SettingsRepository.KEY_ISLAND_STARLINK_HIDE_ARROW,
        SettingsRepository.KEY_ISLAND_STARLINK_CHIP,
        SettingsRepository.KEY_ISLAND_CHIPS,
        SettingsRepository.KEY_ISLAND_BRIEF_ENABLED,
    )

    private enum class Demand { Off, Idle, Live }

    private class RenderSignature(
        val compact: Boolean,
        val chip: Boolean,
        val warn: Boolean,
        val speed: String?,
        val hideUnit: Boolean,
        val hideArrow: Boolean,
        val line: String?,
        val details: StarlinkStatus?,
    ) {
        override fun equals(other: Any?) = other is RenderSignature && compact == other.compact && chip == other.chip &&
            warn == other.warn && speed == other.speed && hideUnit == other.hideUnit && hideArrow == other.hideArrow &&
            line == other.line && details == other.details

        override fun hashCode() = 0
    }

    private var status: StarlinkStatus? = null
    private var misses = 0
    private var failures = 0
    private var lastFetchAt = 0L
    private var force = false
    private var lastSignature: RenderSignature? = null
    private var pollJob: Job? = null
    private var presenceJob: Job? = null
    private val wake = Channel<Unit>(Channel.CONFLATED)

    @Volatile
    private var wifi: Network? = null
    private var wifiCallback: ConnectivityManager.NetworkCallback? = null

    override fun onStart() {
        registerWifi()
        presenceJob = ctx?.scope?.launch { IslandItemsState.presence.collect { wake.trySend(Unit) } }
    }

    override fun onStop() {
        pollJob?.cancel()
        pollJob = null
        presenceJob?.cancel()
        presenceJob = null
        unregisterWifi()
        status = null
        misses = 0
        failures = 0
        lastSignature = null
    }

    override fun refresh() {
        syncPolling()
        render()
        wake.trySend(Unit)
    }

    override fun onScreenStateChanged() {
        force = true
        refresh()
    }

    private fun registerWifi() {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                wifi = network
                failures = 0
                force = true
                wake.trySend(Unit)
            }

            override fun onLost(network: Network) {
                if (wifi == network) wifi = null
                wake.trySend(Unit)
            }
        }
        try {
            cm.registerNetworkCallback(NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), callback)
            wifiCallback = callback
        } catch (_: Exception) {
        }
    }

    private fun unregisterWifi() {
        val callback = wifiCallback ?: return
        try {
            context.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
        wifiCallback = null
        wifi = null
    }

    private fun syncPolling() {
        val c = ctx ?: return
        if (!settings.isIslandShowStarlinkEnabled()) {
            pollJob?.cancel()
            pollJob = null
            status = null
            misses = 0
            failures = 0
            return
        }
        if (pollJob?.isActive == true) return
        pollJob = c.scope.launch { poll() }
    }

    private fun compactOn() = settings.getBoolean(SettingsRepository.KEY_ISLAND_STARLINK_COMPACT, true)

    private fun speedsOn() = settings.getBoolean(SettingsRepository.KEY_ISLAND_STARLINK_SPEEDS, false)

    private fun screenOn() = context.getSystemService(PowerManager::class.java)?.isInteractive != false

    private fun expandedHere(c: IslandPluginContext): Boolean {
        if (c.currentStage() != IslandStage.Expanded) return false
        val focused = c.focusedKey()
        return focused == KEY || focused == BRIEF_KEY
    }

    // Live data is only worth fetching while something on screen shows it
    private fun demand(): Demand {
        val c = ctx ?: return Demand.Off
        if (!settings.isIslandShowStarlinkEnabled() || !screenOn() || c.isContentSuppressed()) return Demand.Off
        val compact = compactOn()
        val shown = IslandItemsState.presence.value[KEY] == IslandPresence.Shown
        return when {
            expandedHere(c) -> Demand.Live
            compact && shown && speedsOn() -> Demand.Live
            compact -> Demand.Idle
            else -> Demand.Off
        }
    }

    private fun intervalFor(demand: Demand): Long {
        val still = IslandStillness.isStill
        return when {
            status == null -> ABSENT_POLL_MS[failures.coerceIn(0, ABSENT_POLL_MS.lastIndex)]
            misses > 0 -> RETRY_POLL_MS
            demand == Demand.Live -> if (still) STILL_LIVE_POLL_MS else LIVE_POLL_MS
            else -> if (still) STILL_IDLE_POLL_MS else IDLE_POLL_MS
        }
    }

    private suspend fun poll() {
        while (true) {
            val demand = demand()
            val network = wifi
            if (demand == Demand.Off || network == null) {
                if (network == null && status != null) {
                    status = null
                    misses = 0
                    render()
                }
                wake.receive()
                continue
            }
            val now = SystemClock.elapsedRealtime()
            val due = if (force) now else lastFetchAt + intervalFor(demand)
            if (now < due) {
                withTimeoutOrNull(due - now) { wake.receive() }
                continue
            }
            force = false
            val fetched = withContext(Dispatchers.IO) { StarlinkClient.fetchStatus(connect = { network.openConnection(it) }) }
            lastFetchAt = SystemClock.elapsedRealtime()
            if (ctx == null) return
            if (fetched != null) {
                misses = 0
                failures = 0
                status = fetched
            } else if (status != null) {
                if (++misses >= MISSES_BEFORE_HIDE) {
                    status = null
                    misses = 0
                }
            } else {
                failures++
            }
            render()
        }
    }

    private fun render() {
        val current = status
        val c = ctx
        if (c == null || current == null || !settings.isIslandShowStarlinkEnabled()) {
            lastSignature = null
            publish(null)
            return
        }
        val compact = compactOn()
        if (!compact && !settings.isIslandBriefEnabled()) {
            lastSignature = null
            publish(null)
            return
        }
        val connected = current.state != StarlinkState.Searching && current.state != StarlinkState.Booting
        val fixedMbps = settings.getString(SettingsRepository.KEY_ISLAND_STARLINK_SPEED_UNIT, null) == StarlinkSpeedFormat.UNIT_MBPS
        val hideUnit = fixedMbps && settings.getBoolean(SettingsRepository.KEY_ISLAND_STARLINK_HIDE_UNIT, false)
        val speedMode = settings.getString(SettingsRepository.KEY_ISLAND_STARLINK_SPEED_MODE, null)
        val hideArrow = (speedMode == StarlinkSpeedFormat.MODE_UP || speedMode == StarlinkSpeedFormat.MODE_DOWN || speedMode == null) &&
            settings.getBoolean(SettingsRepository.KEY_ISLAND_STARLINK_HIDE_ARROW, false)
        val speed = if (compact && connected && speedsOn()) {
            StarlinkSpeedFormat.forMode(speedMode, current.downlinkBps, current.uplinkBps, fixedMbps = fixedMbps)
        } else {
            null
        }
        val warn = !connected || current.degraded
        val chip = compact && settings.getBoolean(SettingsRepository.KEY_ISLAND_STARLINK_CHIP, false)
        val stage = c.currentStage()
        val lineText = if (stage == IslandStage.Line || stage == IslandStage.Expanded) lineSummary(current) else null
        val signature = RenderSignature(
            compact, chip, warn,
            speed?.let { "${it.text}|${it.unit}|${it.direction}" },
            hideUnit, hideArrow, lineText,
            current.takeIf { expandedHere(c) },
        )
        if (signature == lastSignature) return
        lastSignature = signature
        publish(
            IslandItem(
                key = KEY,
                priority = IslandPriority.STARLINK,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("starlink.icon") {
                        IslandIcon(R.drawable.ic_starlink_24, size = 20.dp, tint = if (warn) WARN else islandAccent())
                    },
                ) + listOfNotNull(speed?.let { s -> CompactCell("starlink.speed") { SpeedCell(s, hideUnit, hideArrow) } }),
                line = LineContent(
                    icon = { IslandIcon(R.drawable.ic_starlink_24, tint = if (warn) WARN else islandAccent()) },
                    start = context.getString(R.string.island_show_starlink_title),
                    end = lineText ?: lineSummary(current),
                ),
                expanded = ExpandedContent { scope -> StarlinkExpanded(current, scope) },
                compactVisible = compact,
                chip = chip,
                accent = if (chip) chipAccent(warn) else null,
            ),
        )
    }

    // Chip ink is black, so the container always needs to be light
    private fun chipAccent(warn: Boolean): Color = when {
        warn -> WARN
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> Color(context.getColor(android.R.color.system_accent1_200))
        else -> Color(0xFFB3C8FF)
    }

    private fun lineSummary(s: StarlinkStatus): String = when (s.state) {
        StarlinkState.Searching -> context.getString(R.string.island_starlink_searching)
        StarlinkState.Booting -> context.getString(R.string.island_starlink_booting)
        else -> if (s.currentlyObstructed) {
            context.getString(R.string.island_starlink_obstructed)
        } else {
            listOfNotNull(s.downlinkBps?.let { "↓ ${formatRate(it)}" }, s.pingMs?.let { "${it.toInt()} ms" }).joinToString(" · ")
        }
    }

    @Composable
    private fun StarlinkExpanded(s: StarlinkStatus, scope: IslandExpandedScope) {
        val spec = scope.spec
        val side = spec.expandedPadding + spec.expandedCorner * 0.35f
        val alerts = s.alerts.map { stringResource(alertLabel(it)) }.joinToString(", ")
        Box(propagateMinConstraints = true) {
            Column(Modifier.fillMaxWidth().padding(spec.expandedOutset)) {
                Spacer(Modifier.height(spec.expandedTopPadding))
                scope.CameraRow(
                    title = stringResource(R.string.island_show_starlink_title),
                    start = { IslandIcon(R.drawable.ic_starlink_24, size = 20.dp, tint = islandAccent()) },
                    end = { Text(stateLabel(s), style = IslandTextStyles.title) },
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = side),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val down = s.downlinkBps?.let { rateParts(it) }
                    val up = s.uplinkBps?.let { rateParts(it) }
                    StatTile(R.drawable.rounded_arrow_upward_24, 180f, down?.first, down?.second, Modifier.weight(1f))
                    StatTile(R.drawable.rounded_arrow_upward_24, 0f, up?.first, up?.second, Modifier.weight(1f))
                    StatTile(R.drawable.rounded_timer_24, 0f, s.pingMs?.toInt()?.toString(), "ms", Modifier.weight(1f))
                }
                if (s.obstructionFraction != null || s.uptimeS != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = side),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        s.obstructionFraction?.let { InfoChip(R.drawable.rounded_visibility_off_24, formatPercent(it), Modifier.weight(1f)) }
                        s.uptimeS?.let { InfoChip(R.drawable.rounded_schedule_24, formatUptime(it), Modifier.weight(1f)) }
                    }
                }
                if (alerts.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(alerts, style = IslandTextStyles.body, color = WARN, modifier = Modifier.padding(horizontal = side))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    @Composable
    private fun SpeedCell(speed: CompactSpeed, hideUnit: Boolean, hideArrow: Boolean) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RollingText(speed.text)
            Spacer(Modifier.width(2.dp))
            Column(horizontalAlignment = Alignment.Start) {
                speed.direction?.takeIf { !hideArrow }?.let {
                    IslandIcon(
                        if (it == SpeedDirection.Up) R.drawable.ic_chevron_up_bold_24 else R.drawable.ic_chevron_down_bold_24,
                        size = 13.dp,
                        tint = islandAccent(),
                    )
                }
                if (!hideUnit) Text(speed.unit.label, style = IslandTextStyles.body.copy(fontSize = 7.sp, lineHeight = 8.sp))
            }
        }
    }

    @Composable
    private fun StatTile(icon: Int, rotation: Float, value: String?, unit: String?, modifier: Modifier) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(18.dp))
                .background(islandTextColor(0.1f))
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IslandIcon(icon, size = 20.dp, tint = islandAccent(), modifier = Modifier.rotate(rotation))
            Spacer(Modifier.height(6.dp))
            RollingText(
                text = value ?: "–",
                style = IslandTextStyles.title.copy(fontSize = 26.sp, fontFeatureSettings = "tnum"),
            )
            Text(unit.orEmpty().ifEmpty { " " }, style = IslandTextStyles.body)
        }
    }

    @Composable
    private fun InfoChip(icon: Int, value: String, modifier: Modifier) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(50))
                .background(islandTextColor(0.1f))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IslandIcon(icon, size = 16.dp, tint = islandAccent())
            Spacer(Modifier.width(6.dp))
            RollingText(value)
        }
    }

    @Composable
    private fun stateLabel(s: StarlinkStatus): String = when {
        s.state == StarlinkState.Searching -> stringResource(R.string.island_starlink_searching)
        s.state == StarlinkState.Booting -> stringResource(R.string.island_starlink_booting)
        s.currentlyObstructed -> stringResource(R.string.island_starlink_obstructed)
        else -> stringResource(R.string.island_devices_connected)
    }

    private fun alertLabel(alert: StarlinkAlert): Int = when (alert) {
        StarlinkAlert.MotorsStuck -> R.string.island_starlink_alert_motors
        StarlinkAlert.ThermalShutdown -> R.string.island_starlink_alert_thermal_shutdown
        StarlinkAlert.ThermalThrottle -> R.string.island_starlink_alert_thermal_throttle
        StarlinkAlert.UnexpectedLocation -> R.string.island_starlink_alert_location
        StarlinkAlert.MastNotNearVertical -> R.string.island_starlink_alert_mast
        StarlinkAlert.SlowEthernet -> R.string.island_starlink_alert_ethernet
    }

    companion object {
        const val KEY = "starlink"
        private const val BRIEF_KEY = "brief"
        private const val LIVE_POLL_MS = 2_000L
        private const val STILL_LIVE_POLL_MS = 5_000L
        private const val IDLE_POLL_MS = 30_000L
        private const val STILL_IDLE_POLL_MS = 60_000L
        private const val RETRY_POLL_MS = 3_000L
        private val ABSENT_POLL_MS = longArrayOf(20_000L, 60_000L, 300_000L, 600_000L)
        private const val MISSES_BEFORE_HIDE = 2
        private val WARN = Color(0xFFFFB74D)

        fun rateParts(bps: Float): Pair<String, String> = when {
            bps >= 1_000_000_000f -> "%.1f".format(bps / 1_000_000_000f) to "Gbps"
            bps >= 1_000_000f -> (bps / 1_000_000f).toInt().toString() to "Mbps"
            else -> (bps / 1_000f).toInt().toString() to "kbps"
        }

        fun formatRate(bps: Float): String = when {
            bps >= 1_000_000_000f -> "%.1f Gbps".format(bps / 1_000_000_000f)
            bps >= 1_000_000f -> "${(bps / 1_000_000f).toInt()} Mbps"
            else -> "${(bps / 1_000f).toInt()} kbps"
        }

        fun formatPercent(fraction: Float): String = "%.1f%%".format(fraction * 100f)

        fun formatUptime(seconds: Long): String {
            val d = seconds / 86_400
            val h = seconds % 86_400 / 3_600
            val m = seconds % 3_600 / 60
            return when {
                d > 0 -> "${d}d ${h}h"
                h > 0 -> "${h}h ${m}m"
                else -> "${m}m"
            }
        }
    }
}
