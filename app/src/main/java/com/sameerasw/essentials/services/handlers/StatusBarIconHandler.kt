package com.sameerasw.essentials.services.handlers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.Settings
import android.telephony.TelephonyManager
import com.sameerasw.essentials.domain.StatusBarIconRegistry
import com.sameerasw.essentials.ui.components.pickers.NetworkType
import com.sameerasw.essentials.utils.updateIconBlacklistSetting
import com.sameerasw.essentials.viewmodels.StatusBarIconViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handler for dynamic status bar icon management in the background.
 */
class StatusBarIconHandler(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var smartDataJob: Job? = null

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateAll()
        }

        override fun onLost(network: Network) {
            updateAll()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateAll()
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateBatteryPercentage()
        }
    }

    fun register() {
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Fallback for older versions or issues
            startPollingFallback()
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(batteryReceiver, filter)

        updateAll()
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {}
        
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {}
        
        smartDataJob?.cancel()
    }

    fun updateAll() {
        scope.launch {
            val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            val isSmartWiFiEnabled = prefs.getBoolean(StatusBarIconViewModel.PREF_SMART_WIFI_ENABLED, false)
            val isSmartDataEnabled = prefs.getBoolean(StatusBarIconViewModel.PREF_SMART_DATA_ENABLED, false)
            
            if (isSmartWiFiEnabled || isSmartDataEnabled) {
                updateNetworkIcons(isSmartWiFiEnabled, isSmartDataEnabled)
            }
            
            updateBatteryPercentage()
        }
    }

    private fun updateNetworkIcons(isSmartWiFiEnabled: Boolean, isSmartDataEnabled: Boolean) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        
        // 1. Get current states
        val isWifiConnected = isWifiConnected()
        val networkType = getCurrentNetworkType()
        
        // 2. Load user preferences for all icons
        val visibilities = StatusBarIconRegistry.ALL_ICONS.associate { icon ->
            icon.id to prefs.getBoolean(icon.preferencesKey, icon.defaultVisible)
        }.toMutableMap()

        // 3. Apply Smart WiFi logic
        if (isSmartWiFiEnabled) {
            val mobileDataVisible = visibilities["mobile_data"] ?: true
            visibilities["mobile_data"] = mobileDataVisible && !isWifiConnected
        }

        // 4. Apply Smart Data logic
        if (isSmartDataEnabled && !(isSmartWiFiEnabled && isWifiConnected)) {
            val selectedNetworkTypes = prefs.getStringSet(
                StatusBarIconViewModel.PREF_SELECTED_NETWORK_TYPES,
                setOf(NetworkType.NETWORK_4G.name, NetworkType.NETWORK_5G.name)
            )?.map { NetworkType.valueOf(it) }?.toSet() ?: emptySet()

            val shouldHideMobileData = selectedNetworkTypes.contains(networkType) ||
                (selectedNetworkTypes.contains(NetworkType.NETWORK_OTHER) &&
                        !setOf(
                            NetworkType.NETWORK_5G,
                            NetworkType.NETWORK_4G,
                            NetworkType.NETWORK_3G
                        ).contains(networkType))

            visibilities["mobile_data"] = visibilities["mobile_data"] == true && !shouldHideMobileData
        }

        // 5. Update system settings
        val blacklistNames = StatusBarIconRegistry.getBlacklistNames(visibilities)
        updateIconBlacklistSetting(context, blacklistNames)
    }

    private fun updateBatteryPercentage() {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val mode = prefs.getInt(StatusBarIconViewModel.PREF_BATTERY_PERCENT_MODE, 0)
        
        if (mode != 2) return // Only handle "Charging Only" mode here

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        
        updateSettingsValue("status_bar_show_battery_percent", if (isCharging) 1 else 0)
    }

    private fun updateSettingsValue(key: String, value: Int) {
        try {
            Settings.System.putInt(context.contentResolver, key, value)
        } catch (e: Exception) {
            try {
                Settings.Secure.putInt(context.contentResolver, key, value)
            } catch (e2: Exception) {
                // Fallback to Shizuku/Root if available (omitted for brevity in service, assuming permissions handled)
            }
        }
    }

    private fun isWifiConnected(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            false
        }
    }

    private fun getCurrentNetworkType(): NetworkType {
        return try {
            val network = connectivityManager.activeNetwork ?: return NetworkType.NETWORK_OTHER
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NETWORK_OTHER

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return NetworkType.NETWORK_OTHER
            }

            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val networkType = telephonyManager.networkType

            when (networkType) {
                TelephonyManager.NETWORK_TYPE_NR -> NetworkType.NETWORK_5G
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyManager.NETWORK_TYPE_HSPAP -> NetworkType.NETWORK_4G
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> NetworkType.NETWORK_3G
                else -> NetworkType.NETWORK_OTHER
            }
        } catch (e: Exception) {
            NetworkType.NETWORK_OTHER
        }
    }

    private fun startPollingFallback() {
        smartDataJob?.cancel()
        smartDataJob = scope.launch {
            while (true) {
                updateAll()
                delay(10000)
            }
        }
    }
}
