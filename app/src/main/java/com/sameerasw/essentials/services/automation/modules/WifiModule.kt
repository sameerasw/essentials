package com.sameerasw.essentials.services.automation.modules

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WifiModule : AutomationModule {
    companion object {
        const val ID = "wifi_module"
        private const val UNKNOWN_SSID = "<unknown ssid>"
    }

    override val id: String = ID
    private var automations: List<Automation> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var connectivityManager: ConnectivityManager? = null
    private var appContext: Context? = null
    private val activeNetworkSsids = java.util.concurrent.ConcurrentHashMap<Network, String>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return
            val ssid = currentWifiSsid()
            if (ssid == null) {
                Log.d(ID, "Wi-Fi capabilities changed but SSID unavailable (check location permission/services)")
                return
            }
            if (activeNetworkSsids[network] == ssid) return
            Log.d(ID, "Wi-Fi connected: $ssid")
            activeNetworkSsids[network] = ssid
            handleTrigger { it is Trigger.WifiConnected && it.ssid == ssid }
        }

        override fun onLost(network: Network) {
            val ssid = activeNetworkSsids.remove(network) ?: return
            Log.d(ID, "Wi-Fi disconnected: $ssid")
            handleTrigger { it is Trigger.WifiDisconnected && it.ssid == ssid }
        }
    }

    override fun start(context: Context) {
        appContext = context.applicationContext
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager = manager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            // Default builder requires validated internet access; many Wi-Fi networks
            // (captive portals, offline IoT/local networks) never satisfy that, so the
            // callback would otherwise never fire for them.
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            manager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e(ID, "Failed to register Wi-Fi network callback", e)
        }
    }

    // NetworkCapabilities.getTransportInfo() only exists from API 31 onward, and querying it
    // on older OS versions throws NoSuchMethodError inside the (system-swallowed) callback.
    // WifiManager.connectionInfo works on every API level and respects the same location
    // permission gating, so it's used unconditionally instead of branching by SDK level.
    @Suppress("DEPRECATION")
    private fun currentWifiSsid(): String? {
        val wifiManager =
            appContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val ssid = try {
            wifiManager.connectionInfo?.ssid
        } catch (e: Exception) {
            Log.w(ID, "Failed to read current SSID", e)
            null
        }
        if (ssid.isNullOrEmpty() || ssid == UNKNOWN_SSID) return null
        return ssid.removeSurrounding("\"")
    }

    override fun stop(context: Context) {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        connectivityManager = null
        appContext = null
        activeNetworkSsids.clear()
    }

    override fun updateAutomations(automations: List<Automation>) {
        this.automations = automations
    }

    private fun handleTrigger(matches: (Trigger) -> Boolean) {
        val context = appContext ?: return
        scope.launch {
            automations.filter { it.type == Automation.Type.TRIGGER && it.trigger?.let(matches) == true }
                .forEach { automation ->
                    automation.actions.forEach { action ->
                        CombinedActionExecutor.execute(context, action)
                    }
                }
        }
    }
}
