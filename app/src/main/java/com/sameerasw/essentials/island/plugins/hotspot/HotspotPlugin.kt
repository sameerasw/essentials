package com.sameerasw.essentials.island.plugins.hotspot

import com.sameerasw.essentials.island.ui.components.islandAccent

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lsposed.hiddenapibypass.HiddenApiBypass

class HotspotPlugin : BaseIslandPlugin() {
    override val id = "hotspot"

    override val settingKeys = setOf(SettingsRepository.KEY_ISLAND_SHOW_HOTSPOT)

    private val stats by lazy { context.getSystemService(NetworkStatsManager::class.java) }
    private val wifi by lazy { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private var enabled = false
    private var windowStart = 0L
    private var baseline: Long? = null
    private var bytes: Long? = null
    private var clients: Int? = null
    private var pollJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) =
            setEnabled(intent.getIntExtra(EXTRA_AP_STATE, 0) == AP_STATE_ENABLED)
    }

    override fun onStart() {
        try {
            // Not sticky on recent Android, so this only catches changes; the initial state is read below.
            context.registerReceiver(receiver, IntentFilter(ACTION_AP_STATE))
        } catch (_: Exception) {
        }
        setEnabled(readApEnabled())
    }

    override fun onStop() {
        pollJob?.cancel()
        pollJob = null
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        enabled = false
        bytes = null
        clients = null
    }

    override fun refresh() {
        syncPolling()
        render()
    }

    private fun setEnabled(on: Boolean) {
        if (ctx == null || on == enabled) return
        enabled = on
        syncPolling()
        render()
    }

    private fun syncPolling() {
        val c = ctx ?: return
        if (!enabled || !settings.isIslandShowHotspotEnabled()) {
            pollJob?.cancel()
            pollJob = null
            bytes = null
            clients = null
            baseline = null
            return
        }
        if (pollJob?.isActive == true) return
        windowStart = System.currentTimeMillis() - WINDOW_MS
        pollJob = c.scope.launch { poll() }
    }

    private suspend fun poll() {
        while (true) {
            val total = withContext(Dispatchers.IO) { readTetheredBytes() }
            val count = withContext(Dispatchers.IO) { readClientCount() }
            if (ctx == null || !enabled) return
            if (total != null && baseline == null) baseline = total
            bytes = total?.let { (it - (baseline ?: it)).coerceAtLeast(0L) }
            clients = count
            render()
            delay(POLL_MS)
        }
    }

    private fun readApEnabled(): Boolean = try {
        HiddenApiBypass.invoke(WifiManager::class.java, wifi, "isWifiApEnabled") as? Boolean ?: false
    } catch (_: Exception) {
        false
    }

    private fun readClientCount(): Int? {
        val c = ctx?.service ?: return null
        if (!ShellUtils.isAvailable(c) || !ShellUtils.hasPermission(c)) return null
        val dump = ShellUtils.runCommandWithOutput(c, "dumpsys tethering", notifyOnError = false) ?: return null
        return TetheringDump.clientCount(dump)
    }

    private fun readTetheredBytes(): Long? {
        if (!PermissionUtils.hasUsageStatsPermission(context)) return null
        return try {
            val now = System.currentTimeMillis()
            // Mobile covers the usual case; Wi-Fi covers Pixels sharing a Wi-Fi connection.
            listOf(ConnectivityManager.TYPE_MOBILE, ConnectivityManager.TYPE_WIFI).sumOf { type ->
                stats.queryDetailsForUid(type, null, windowStart, now, NetworkStats.Bucket.UID_TETHERING).use { s ->
                    val bucket = NetworkStats.Bucket()
                    var total = 0L
                    while (s.hasNextBucket()) {
                        s.getNextBucket(bucket)
                        total += bucket.rxBytes + bucket.txBytes
                    }
                    total
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Tethered bytes query failed", e)
            null
        }
    }

    private fun render() {
        if (ctx == null || !enabled || !settings.isIslandShowHotspotEnabled()) {
            publish(null)
            return
        }
        val count = clients?.takeIf { it > 0 }
        val end = listOfNotNull(
            count?.let { context.resources.getQuantityString(R.plurals.island_hotspot_clients, it, it) },
            bytes?.let { Formatter.formatShortFileSize(context, it) },
        ).joinToString(" · ")
        publish(
            IslandItem(
                key = KEY,
                priority = IslandPriority.HOTSPOT,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("hotspot.icon") { HotspotIcon(count) },
                ),
                line = LineContent(
                    icon = { IslandIcon(R.drawable.rounded_wifi_tethering_24, tint = islandAccent()) },
                    start = context.getString(R.string.island_show_hotspot_title),
                    end = end,
                ),
            )
        )
    }

    companion object {
        const val KEY = "hotspot"
        private const val TAG = "HotspotPlugin"
        private const val ACTION_AP_STATE = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        private const val EXTRA_AP_STATE = "wifi_state"
        private const val AP_STATE_ENABLED = 13
        private const val POLL_MS = 10_000L
        private const val WINDOW_MS = 24 * 60 * 60 * 1000L
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun HotspotIcon(count: Int?) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        IslandIcon(
            R.drawable.rounded_wifi_tethering_24,
            size = 20.dp,
            tint = islandAccent(),
            modifier = Modifier.align(Alignment.TopStart),
        )
        if (count != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(11.dp)
                    .drawBehind { drawCircle(Color.Black, blendMode = BlendMode.Clear) },
            )
            Text(
                text = if (count > 9) "9+" else count.toString(),
                color = islandAccent(),
                fontSize = if (count > 9) 6.sp else 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(11.dp),
                style = LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
            )
        }
    }
}

object TetheringDump {
    fun clientCount(dump: String): Int? {
        val lines = dump.lineSequence().toList()
        val header = lines.indexOfFirst { it.trim() == "Client Information:" }
        if (header < 0) return null
        val body = lines.drop(header + 1).takeWhile { it.trim().startsWith("{") || it.trim() == "<empty>" }
        return body.sumOf { Regex("client: /").findAll(it).count() }
    }
}
