package com.sameerasw.essentials.island.plugins.starlink

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.state.IslandStillness
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.islandTextColor
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.island.ui.components.islandAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StarlinkPlugin : BaseIslandPlugin() {
    override val id = "starlink"

    override val settingKeys = setOf(SettingsRepository.KEY_ISLAND_SHOW_STARLINK)

    private var status: StarlinkStatus? = null
    private var misses = 0
    private var pollJob: Job? = null

    override fun refresh() {
        syncPolling()
        render()
    }

    override fun onStop() {
        pollJob?.cancel()
        pollJob = null
        status = null
        misses = 0
    }

    private fun syncPolling() {
        val c = ctx ?: return
        if (!settings.isIslandShowStarlinkEnabled()) {
            pollJob?.cancel()
            pollJob = null
            status = null
            misses = 0
            return
        }
        if (pollJob?.isActive == true) return
        pollJob = c.scope.launch { poll() }
    }

    private suspend fun poll() {
        while (true) {
            val fetched = if (hasWifi()) withContext(Dispatchers.IO) { StarlinkClient.fetchStatus() } else null
            if (ctx == null) return
            if (fetched != null) {
                misses = 0
                status = fetched
            } else if (++misses >= MISSES_BEFORE_HIDE) {
                status = null
            }
            render()
            delay(
                when {
                    status == null -> ABSENT_POLL_MS
                    IslandStillness.isStill -> STILL_POLL_MS
                    else -> ACTIVE_POLL_MS
                },
            )
        }
    }

    private fun hasWifi(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.allNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
    }

    private fun render() {
        val current = status
        if (ctx == null || current == null || !settings.isIslandShowStarlinkEnabled()) {
            publish(null)
            return
        }
        val connected = current.state == StarlinkState.Connected
        val warn = !connected || current.degraded
        publish(
            IslandItem(
                key = KEY,
                priority = IslandPriority.STARLINK,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("starlink.icon") {
                        IslandIcon(R.drawable.ic_starlink_24, size = 20.dp, tint = if (warn) WARN else islandAccent())
                    },
                ),
                line = LineContent(
                    icon = { IslandIcon(R.drawable.ic_starlink_24, tint = if (warn) WARN else islandAccent()) },
                    start = context.getString(R.string.island_show_starlink_title),
                    end = lineSummary(current),
                ),
                expanded = ExpandedContent { scope -> StarlinkExpanded(current, scope) },
            ),
        )
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
        private const val ACTIVE_POLL_MS = 2_000L
        private const val STILL_POLL_MS = 10_000L
        private const val ABSENT_POLL_MS = 15_000L
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
