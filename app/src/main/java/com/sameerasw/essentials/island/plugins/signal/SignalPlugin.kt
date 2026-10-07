package com.sameerasw.essentials.island.plugins.signal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.ui.core.pickers.NetworkType

class SignalPlugin : BaseIslandPlugin() {
    override val id = "signal"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_NETWORK,
        SettingsRepository.KEY_ISLAND_SHOW_SIGNAL,
        SettingsRepository.KEY_ISLAND_SIGNAL_NETWORK_TYPES,
        SettingsRepository.KEY_ISLAND_SIGNAL_SHOW_MODE,
        SettingsRepository.KEY_ISLAND_SIGNAL_WIFI,
        SettingsRepository.KEY_ISLAND_SIGNAL_LOW_ONLY,
    )

    private val connectivity by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    private val telephony by lazy { context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager }
    private var telephonyCallback: Any? = null
    private var level = 0
    private var airplane = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = render()
        override fun onLost(network: Network) = render()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = render()
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
        try {
            connectivity.registerDefaultNetworkCallback(networkCallback, c.mainHandler)
        } catch (_: Exception) {
        }
        try {
            context.registerReceiver(airplaneReceiver, IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED))
        } catch (_: Exception) {
        }
        registerTelephony()
    }

    override fun onStop() {
        try {
            connectivity.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
        try {
            context.unregisterReceiver(airplaneReceiver)
        } catch (_: Exception) {
        }
        unregisterTelephony()
    }

    override fun refresh() = render()

    private fun registerTelephony() {
        val manager = telephony ?: return
        val c = ctx ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        level = signalStrength.level.coerceIn(0, 4)
                        render()
                    }
                }
                manager.registerTelephonyCallback(context.mainExecutor, callback)
                telephonyCallback = callback
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        level = signalStrength?.level?.coerceIn(0, 4) ?: 0
                        c.mainHandler.post { render() }
                    }
                }
                @Suppress("DEPRECATION")
                manager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                telephonyCallback = listener
            }
        } catch (_: Exception) {
        }
    }

    private fun unregisterTelephony() {
        val manager = telephony
        val callback = telephonyCallback ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (callback as? TelephonyCallback)?.let { manager?.unregisterTelephonyCallback(it) }
            } else {
                @Suppress("DEPRECATION")
                (callback as? PhoneStateListener)?.let { manager?.listen(it, PhoneStateListener.LISTEN_NONE) }
            }
        } catch (_: Exception) {
        }
        telephonyCallback = null
    }

    private fun readAirplane(): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

    private fun rawDataType(): Int = try {
        val manager = telephony
        if (manager == null) {
            TelephonyManager.NETWORK_TYPE_UNKNOWN
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager.dataNetworkType
        } else {
            @Suppress("DEPRECATION")
            manager.networkType
        }
    } catch (_: Exception) {
        TelephonyManager.NETWORK_TYPE_UNKNOWN
    }

    private fun modeLabel(type: Int): String? = when (type) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_TD_SCDMA,
        TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A,
        TelephonyManager.NETWORK_TYPE_EVDO_B,
        TelephonyManager.NETWORK_TYPE_EHRPD,
        -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE -> "E"
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_GSM,
        TelephonyManager.NETWORK_TYPE_CDMA,
        TelephonyManager.NETWORK_TYPE_1xRTT,
        TelephonyManager.NETWORK_TYPE_IDEN,
        -> "2G"
        else -> null
    }

    private fun category(label: String?): NetworkType = when (label) {
        "5G" -> NetworkType.NETWORK_5G
        "4G" -> NetworkType.NETWORK_4G
        "3G" -> NetworkType.NETWORK_3G
        else -> NetworkType.NETWORK_OTHER
    }

    private fun readLevel(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val live = runCatching { telephony?.signalStrength?.level }.getOrNull()
            if (live != null) level = live.coerceIn(0, 4)
        }
        return level
    }

    private fun render() {
        if (ctx == null || !settings.isIslandShowNetworkEnabled() || !(settings.isIslandShowSignalEnabled() || settings.isIslandSignalWifiEnabled())) {
            publish(null)
            return
        }
        if (airplane) {
            publish(null)
            return
        }
        if (connectivity.activeNetwork == null) {
            publish(
                IslandItem(
                    key = KEY,
                    priority = IslandPriority.SIGNAL,
                    placement = CompactPlacement.Dynamic,
                    compact = listOf(
                        CompactCell("signal.offline") {
                            IslandIcon(R.drawable.rounded_android_cell_dual_5_bar_alert_24, size = 18.dp, tint = MaterialTheme.colorScheme.error)
                        },
                    ),
                ),
            )
            return
        }
        val caps = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        val wifiLevel = if (settings.isIslandSignalWifiEnabled() && caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            readWifiLevel(caps)
        } else {
            null
        }
        val label = modeLabel(rawDataType())
        if (wifiLevel == null && (!settings.isIslandShowSignalEnabled() || category(label) !in settings.getIslandSignalNetworkTypes())) {
            publish(null)
            return
        }
        val bars = readLevel()
        if (settings.isIslandSignalLowOnlyEnabled() && (wifiLevel ?: bars) > LOW_LEVEL) {
            publish(null)
            return
        }
        val shownLabel = if (settings.isIslandSignalShowModeEnabled()) label else null
        publish(
            IslandItem(
                key = KEY,
                priority = IslandPriority.SIGNAL,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("signal.bars") {
                        SignalIndicator(wifiLevel, bars, MaterialTheme.colorScheme.primary, shownLabel)
                    },
                ),
            ),
        )
    }

    private fun readWifiLevel(caps: NetworkCapabilities): Int {
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        var rssi: Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) rssi = (caps.transportInfo as? WifiInfo)?.rssi
        if (rssi == null) {
            @Suppress("DEPRECATION")
            rssi = wifi?.connectionInfo?.rssi
        }
        if (rssi == null || rssi <= -127) return 0
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifi != null) {
            wifi.calculateSignalLevel(rssi).coerceIn(0, 4)
        } else {
            @Suppress("DEPRECATION")
            WifiManager.calculateSignalLevel(rssi, 5).coerceIn(0, 4)
        }
    }

    companion object {
        const val KEY = "signal"
        private const val LOW_LEVEL = 1
    }
}
