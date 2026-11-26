package com.sameerasw.essentials.island.plugins.network

import com.sameerasw.essentials.island.ui.components.islandAccent

import com.sameerasw.essentials.utils.NetworkUtil
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.utils.WifiUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkPlugin : BaseIslandPlugin() {
    override val id = "network"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_NETWORK,
        SettingsRepository.KEY_ISLAND_NETWORK_ACTIVITY,
        SettingsRepository.KEY_ISLAND_NETWORK_VPN,
        SettingsRepository.KEY_ISLAND_LINE_STAGE_ENABLED,
    )

    private class WifiEvent(val connected: Boolean, val name: String)

    private val connectivity by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    private var wifiNetwork: Network? = null
    private var ssid: String? = null
    private var airplane = false
    private var event: WifiEvent? = null
    private var pendingLost: Runnable? = null
    private var lostSsid: String? = null
    private val clearEvent = Runnable {
        event = null
        render()
    }

    private val callback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = onWifi(network, caps)
            override fun onLost(network: Network) = onWifiLost(network)
        }

    private val vpnNetworks = mutableSetOf<Network>()
    private var vpnName: String? = null
    private var vpnLookup: Job? = null

    private val vpnCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val wasDisconnected = vpnNetworks.isEmpty()
                if (!vpnNetworks.add(network)) return
                render()
                if (wasDisconnected) resolveVpnName(network, announce = true)
            }

            override fun onLost(network: Network) {
                if (!vpnNetworks.remove(network)) return
                if (vpnNetworks.isEmpty()) {
                    vpnLookup?.cancel()
                    vpnName = null
                }
                render()
            }
        }

    private val airplaneReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            airplane = readAirplane()
            render()
        }
    }

    override fun onStart() {
        val c = ctx ?: return
        airplane = readAirplane()
        wifiNetwork = connectivity.allNetworks.firstOrNull { network ->
            connectivity.getNetworkCapabilities(network)?.let {
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !it.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            } == true
        }.takeIf { NetworkUtil.isWifiConnected(connectivity) }
        ssid = wifiNetwork?.let { WifiUtil.getCurrentSsid(context) }
        try {
            val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
            connectivity.registerNetworkCallback(request, callback, c.mainHandler)
        } catch (_: Exception) {
        }
        vpnNetworks.clear()
        connectivity.allNetworks
            .filter { connectivity.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true }
            .forEach { vpnNetworks.add(it) }
        vpnNetworks.firstOrNull()?.let { resolveVpnName(it, announce = false) }
        try {
            // Default requests carry NOT_VPN, which would filter out the very networks we want
            val request =
                NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .build()
            connectivity.registerNetworkCallback(request, vpnCallback, c.mainHandler)
        } catch (_: Exception) {
        }
        try {
            context.registerReceiver(airplaneReceiver, IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED))
        } catch (_: Exception) {
        }
    }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(clearEvent)
        pendingLost?.let { ctx?.mainHandler?.removeCallbacks(it) }
        pendingLost = null
        lostSsid = null
        try {
            connectivity.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
        try {
            connectivity.unregisterNetworkCallback(vpnCallback)
        } catch (_: Exception) {
        }
        vpnNetworks.clear()
        vpnLookup?.cancel()
        vpnLookup = null
        vpnName = null
        try {
            context.unregisterReceiver(airplaneReceiver)
        } catch (_: Exception) {
        }
        event = null
        wifiNetwork = null
        ssid = null
    }

    override fun refresh() = render()

    private fun onWifi(network: Network, caps: NetworkCapabilities) {
        if (ctx == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return
        if (network == wifiNetwork) {
            if (ssid == null) ssidFrom(caps)?.let { ssid = it }
            return
        }
        val name = ssidFrom(caps) ?: WifiUtil.getCurrentSsid(context)
        val pending = pendingLost
        if (pending != null) {
            ctx?.mainHandler?.removeCallbacks(pending)
            pendingLost = null
            val previous = lostSsid
            lostSsid = null
            if (previous == null || name == null || previous == name) {
                wifiNetwork = network
                ssid = name ?: previous
                return
            }
            announce(connected = false)
        }
        val sameNetwork = ssid != null && name == ssid
        wifiNetwork = network
        ssid = name
        if (!sameNetwork) announce(connected = true)
    }

    private fun onWifiLost(network: Network) {
        val c = ctx ?: return
        if (network != wifiNetwork) return
        wifiNetwork = null
        lostSsid = ssid
        val confirm = Runnable {
            pendingLost = null
            lostSsid = null
            announce(connected = false)
            ssid = null
        }
        pendingLost = confirm
        c.mainHandler.postDelayed(confirm, WIFI_LOST_SETTLE_MS)
    }

    private fun ssidFrom(caps: NetworkCapabilities): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val raw = (caps.transportInfo as? WifiInfo)?.ssid ?: return null
        if (raw.isEmpty() || raw == WifiManager.UNKNOWN_SSID) return null
        return raw.removeSurrounding("\"")
    }

    private fun announce(connected: Boolean) {
        val c = ctx ?: return
        if (!activityEnabled() || !settings.isIslandLineStageEnabled()) return
        val duration = settings.getIslandPeekDurationMs()
        event = WifiEvent(connected, ssid ?: context.getString(R.string.island_network_wifi))
        render()
        c.request(PluginRequest.Peek(WIFI_KEY, duration))
        c.mainHandler.removeCallbacks(clearEvent)
        c.mainHandler.postDelayed(clearEvent, duration + 500L)
    }

    private fun resolveVpnName(network: Network, announce: Boolean) {
        val c = ctx ?: return
        vpnLookup?.cancel()
        vpnLookup = c.scope.launch {
            val name = withContext(Dispatchers.IO) { runCatching { vpnOwnerLabel(network) }.getOrNull() }
            if (network !in vpnNetworks) return@launch
            vpnName = name
            render()
            if (announce) announceVpn()
        }
    }

    // ownerUid is redacted for apps that don't own the VPN, so fall back to always-on VPN, then a shell dump
    private fun vpnOwnerLabel(network: Network): String? {
        val pm = context.packageManager
        fun packageFor(uid: Int?): String? =
            uid?.takeIf { it > 0 }?.let { pm.getPackagesForUid(it)?.firstOrNull() }

        val ownerUid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) connectivity.getNetworkCapabilities(network)?.ownerUid else null
        val pkg =
            packageFor(ownerUid)
                ?: Settings.Secure.getString(context.contentResolver, "always_on_vpn_app")?.takeIf { it.isNotBlank() }
                ?: if (ShellUtils.isAvailable(context) && ShellUtils.hasPermission(context)) {
                    ShellUtils.runCommandWithOutput(context, "dumpsys connectivity", notifyOnError = false)
                        ?.let { VPN_OWNER_REGEX.find(it)?.groupValues?.get(1)?.toIntOrNull() }
                        ?.let { packageFor(it) }
                } else {
                    null
                }
        return pkg?.let { runCatching { pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString() }.getOrNull() }
    }

    private fun announceVpn() {
        val c = ctx ?: return
        if (!vpnEnabled() || !settings.isIslandLineStageEnabled()) return
        c.request(PluginRequest.Peek(VPN_KEY, settings.getIslandPeekDurationMs()))
    }

    private fun activityEnabled() = settings.isIslandShowNetworkEnabled() && settings.isIslandNetworkActivityEnabled()

    private fun vpnEnabled() = settings.isIslandShowNetworkEnabled() && settings.isIslandNetworkVpnEnabled()

    private fun readAirplane(): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

    private fun render() {
        val activity = activityEnabled()
        val vpn = vpnEnabled() && vpnNetworks.isNotEmpty()
        if (ctx == null || (!activity && !vpn)) {
            publish(null)
            return
        }
        val items = mutableListOf<IslandItem>()
        if (activity) event?.let { e ->
            val tint = if (e.connected) null else Color.White.copy(alpha = 0.6f)
            items += IslandItem(
                key = WIFI_KEY,
                priority = IslandPriority.NETWORK,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("network.wifi.icon") {
                        IslandIcon(R.drawable.rounded_android_wifi_3_bar_24, size = 18.dp, tint = tint ?: islandAccent())
                    },
                ),
                line = LineContent(
                    icon = { IslandIcon(R.drawable.rounded_android_wifi_3_bar_24, tint = tint ?: islandAccent()) },
                    start = context.getString(if (e.connected) R.string.island_network_connected_to else R.string.island_network_disconnected_from),
                    end = e.name,
                ),
            )
        }
        if (activity && airplane) {
            items += IslandItem(
                key = AIRPLANE_KEY,
                priority = IslandPriority.NETWORK,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("network.airplane.icon") {
                        IslandIcon(R.drawable.rounded_flight_24, size = 18.dp, tint = islandAccent())
                    },
                ),
            )
        }
        if (vpn) {
            items += IslandItem(
                key = VPN_KEY,
                priority = IslandPriority.NETWORK,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("network.vpn.icon") {
                        IslandIcon(R.drawable.rounded_vpn_key_24, size = 18.dp, tint = islandAccent())
                    },
                ),
                line = LineContent(
                    icon = { IslandIcon(R.drawable.rounded_vpn_key_24, tint = islandAccent()) },
                    start = context.getString(R.string.island_network_connected_to),
                    end = vpnName ?: context.getString(R.string.icon_vpn),
                ),
            )
        }
        publish(items)
    }

    companion object {
        const val WIFI_KEY = "network.wifi"
        const val AIRPLANE_KEY = "network.airplane"
        const val VPN_KEY = "network.vpn"
        private val VPN_OWNER_REGEX = Regex("""Transports: VPN[^\n]*?OwnerUid: (\d+)""")
        private const val WIFI_LOST_SETTLE_MS = 4000L
    }
}
