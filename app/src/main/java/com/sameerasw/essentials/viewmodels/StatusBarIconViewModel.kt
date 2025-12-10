package com.sameerasw.essentials.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.sameerasw.essentials.ui.components.pickers.NetworkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StatusBarIconViewModel : ViewModel() {
    val isWriteSecureSettingsEnabled = mutableStateOf(false)
    val isMobileDataVisible = mutableStateOf(true)
    val isWiFiVisible = mutableStateOf(true)
    val isSmartWiFiEnabled = mutableStateOf(false)
    val isSmartDataEnabled = mutableStateOf(false)
    val selectedNetworkTypes = mutableStateOf(setOf(NetworkType.NETWORK_4G, NetworkType.NETWORK_5G))

    // New icon visibility states
    val isVpnVisible = mutableStateOf(true)
    val isAlarmClockVisible = mutableStateOf(true)
    val isHotspotVisible = mutableStateOf(true)
    val isBluetoothVisible = mutableStateOf(true)
    val isDataSaverVisible = mutableStateOf(true)
    val isHeadsetVisible = mutableStateOf(true)
    val isRotateVisible = mutableStateOf(true)
    val isVolteVisible = mutableStateOf(true)
    val isCastVisible = mutableStateOf(true)
    val isClockVisible = mutableStateOf(true)

    private var updateJob: Job? = null
    private var smartWifiJob: Job? = null
    private var smartDataJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val ICON_BLACKLIST_SETTING = "icon_blacklist"
        const val BASE_BLACKLIST = "rotate,vowifi,ims,nfc,volte,headset,ims_volte"
        const val PREF_SMART_WIFI_ENABLED = "smart_wifi_enabled"
        const val PREF_SMART_DATA_ENABLED = "smart_data_enabled"
        const val PREF_SELECTED_NETWORK_TYPES = "selected_network_types"
    }

    fun check(context: Context) {
        isWriteSecureSettingsEnabled.value = canWriteSecureSettings(context)
        loadIconVisibilityState(context)
        loadSmartWiFiPref(context)
        loadSmartDataPref(context)
        loadSelectedNetworkTypes(context)

        if (isSmartWiFiEnabled.value && isWriteSecureSettingsEnabled.value) {
            startSmartWiFiUpdates(context)
        }

        if (isSmartDataEnabled.value && isWriteSecureSettingsEnabled.value) {
            startSmartDataUpdates(context)
        }
    }

    fun setMobileDataVisible(visible: Boolean, context: Context) {
        isMobileDataVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_mobile_visible", visible)
        }

        updateIconBlacklist(context)
    }

    fun setWiFiVisible(visible: Boolean, context: Context) {
        isWiFiVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_wifi_visible", visible)
        }
        updateIconBlacklist(context)
    }

    fun setSmartWiFiEnabled(enabled: Boolean, context: Context) {
        isSmartWiFiEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean(PREF_SMART_WIFI_ENABLED, enabled)
        }

        if (enabled && isWriteSecureSettingsEnabled.value) {
            startSmartWiFiUpdates(context)
        } else {
            smartWifiJob?.cancel()
            // When disabling smart WiFi, restore manual settings
            updateIconBlacklist(context)
        }
    }

    fun setSmartDataEnabled(enabled: Boolean, context: Context) {
        isSmartDataEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean(PREF_SMART_DATA_ENABLED, enabled)
        }

        if (enabled && isWriteSecureSettingsEnabled.value) {
            startSmartDataUpdates(context)
        } else {
            smartDataJob?.cancel()
            updateIconBlacklist(context)
        }

        // Update the network types based on Smart Data setting
        updateSelectedNetworkTypes(context, enabled)
    }

    fun updateSelectedNetworkTypes(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val currentTypes = prefs.getStringSet(PREF_SELECTED_NETWORK_TYPES, setOf(NetworkType.NETWORK_4G.name, NetworkType.NETWORK_5G.name))?.toMutableSet() ?: mutableSetOf()

        if (enabled) {
            // Add 5G and 4G to selected types if not already present
            currentTypes.add(NetworkType.NETWORK_5G.name)
            currentTypes.add(NetworkType.NETWORK_4G.name)
        } else {
            // Remove 5G and 4G from selected types
            currentTypes.remove(NetworkType.NETWORK_5G.name)
            currentTypes.remove(NetworkType.NETWORK_4G.name)
        }

        selectedNetworkTypes.value = currentTypes.map { NetworkType.valueOf(it) }.toSet()

        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putStringSet(PREF_SELECTED_NETWORK_TYPES, currentTypes)
        }
    }

    private fun updateIconBlacklist(context: Context) {
        if (!isWriteSecureSettingsEnabled.value) return

        val blacklistItems = BASE_BLACKLIST.split(",").toMutableList()

        // Add or remove mobile from blacklist based on visibility
        if (!isMobileDataVisible.value && !blacklistItems.contains("mobile")) {
            blacklistItems.add("mobile")
        } else if (isMobileDataVisible.value) {
            blacklistItems.remove("mobile")
        }

        // Add or remove wifi from blacklist based on visibility
        if (!isWiFiVisible.value && !blacklistItems.contains("wifi")) {
            blacklistItems.add("wifi")
        } else if (isWiFiVisible.value) {
            blacklistItems.remove("wifi")
        }

        // Add or remove vpn from blacklist based on visibility
        if (!isVpnVisible.value && !blacklistItems.contains("vpn")) {
            blacklistItems.add("vpn")
        } else if (isVpnVisible.value) {
            blacklistItems.remove("vpn")
        }

        // Add or remove alarm_clock from blacklist based on visibility
        if (!isAlarmClockVisible.value && !blacklistItems.contains("alarm_clock")) {
            blacklistItems.add("alarm_clock")
        } else if (isAlarmClockVisible.value) {
            blacklistItems.remove("alarm_clock")
        }

        // Add or remove hotspot from blacklist based on visibility
        if (!isHotspotVisible.value && !blacklistItems.contains("hotspot")) {
            blacklistItems.add("hotspot")
        } else if (isHotspotVisible.value) {
            blacklistItems.remove("hotspot")
        }

        // Add or remove bluetooth from blacklist based on visibility
        if (!isBluetoothVisible.value && !blacklistItems.contains("bluetooth")) {
            blacklistItems.add("bluetooth")
        } else if (isBluetoothVisible.value) {
            blacklistItems.remove("bluetooth")
        }

        // Add or remove data_saver from blacklist based on visibility
        if (!isDataSaverVisible.value && !blacklistItems.contains("data_saver")) {
            blacklistItems.add("data_saver")
        } else if (isDataSaverVisible.value) {
            blacklistItems.remove("data_saver")
        }

        // Add or remove headset from blacklist based on visibility
        if (!isHeadsetVisible.value && !blacklistItems.contains("headset")) {
            blacklistItems.add("headset")
        } else if (isHeadsetVisible.value) {
            blacklistItems.remove("headset")
        }

        // Add or remove rotate from blacklist based on visibility
        if (!isRotateVisible.value && !blacklistItems.contains("rotate")) {
            blacklistItems.add("rotate")
        } else if (isRotateVisible.value) {
            blacklistItems.remove("rotate")
        }

        // Add or remove volte from blacklist based on visibility
        if (!isVolteVisible.value) {
            if (!blacklistItems.contains("volte")) blacklistItems.add("volte")
            if (!blacklistItems.contains("vowifi")) blacklistItems.add("vowifi")
            if (!blacklistItems.contains("ims_volte")) blacklistItems.add("ims_volte")
        } else {
            blacklistItems.remove("volte")
            blacklistItems.remove("vowifi")
            blacklistItems.remove("ims_volte")
        }

        // Add or remove cast from blacklist based on visibility
        if (!isCastVisible.value && !blacklistItems.contains("cast")) {
            blacklistItems.add("cast")
        } else if (isCastVisible.value) {
            blacklistItems.remove("cast")
        }

        // Add or remove clock from blacklist based on visibility
        if (!isClockVisible.value && !blacklistItems.contains("clock")) {
            blacklistItems.add("clock")
        } else if (isClockVisible.value) {
            blacklistItems.remove("clock")
        }

        val newBlacklist = blacklistItems.joinToString(",")

        try {
            Settings.Secure.putString(context.contentResolver, ICON_BLACKLIST_SETTING, newBlacklist)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startSmartWiFiUpdates(context: Context) {
        smartWifiJob?.cancel()
        smartWifiJob = scope.launch {
            while (true) {
                val isWifiConnected = isWifiConnected(context)
                updateSmartWiFiBlacklist(context, isWifiConnected)
                delay(1000) // Check every second
            }
        }
    }

    private fun updateSmartWiFiBlacklist(context: Context, wifiConnected: Boolean) {
        if (!isSmartWiFiEnabled.value || !isWriteSecureSettingsEnabled.value) return

        val blacklistItems = BASE_BLACKLIST.split(",").toMutableList()

        // Handle WiFi visibility
        if (!isWiFiVisible.value && !blacklistItems.contains("wifi")) {
            blacklistItems.add("wifi")
        } else if (isWiFiVisible.value) {
            blacklistItems.remove("wifi")
        }

        // Handle Mobile Data visibility with Smart WiFi logic
        if (wifiConnected) {
            // WiFi is connected - hide mobile data if Smart WiFi is enabled
            if (!blacklistItems.contains("mobile")) {
                blacklistItems.add("mobile")
            }
        } else {
            // WiFi is not connected - show mobile data only if it's enabled manually
            if (isMobileDataVisible.value) {
                blacklistItems.remove("mobile")
            } else if (!blacklistItems.contains("mobile")) {
                blacklistItems.add("mobile")
            }
        }

        // Handle other icon visibility (VPN, Alarm Clock, Hotspot, Bluetooth)
        if (!isVpnVisible.value && !blacklistItems.contains("vpn")) {
            blacklistItems.add("vpn")
        } else if (isVpnVisible.value) {
            blacklistItems.remove("vpn")
        }

        if (!isAlarmClockVisible.value && !blacklistItems.contains("alarm_clock")) {
            blacklistItems.add("alarm_clock")
        } else if (isAlarmClockVisible.value) {
            blacklistItems.remove("alarm_clock")
        }

        if (!isHotspotVisible.value && !blacklistItems.contains("hotspot")) {
            blacklistItems.add("hotspot")
        } else if (isHotspotVisible.value) {
            blacklistItems.remove("hotspot")
        }

        if (!isBluetoothVisible.value && !blacklistItems.contains("bluetooth")) {
            blacklistItems.add("bluetooth")
        } else if (isBluetoothVisible.value) {
            blacklistItems.remove("bluetooth")
        }

        // Handle Data Saver visibility
        if (!isDataSaverVisible.value && !blacklistItems.contains("data_saver")) {
            blacklistItems.add("data_saver")
        } else if (isDataSaverVisible.value) {
            blacklistItems.remove("data_saver")
        }

        // Handle Headset visibility
        if (!isHeadsetVisible.value && !blacklistItems.contains("headset")) {
            blacklistItems.add("headset")
        } else if (isHeadsetVisible.value) {
            blacklistItems.remove("headset")
        }

        // Handle Rotate visibility
        if (!isRotateVisible.value && !blacklistItems.contains("rotate")) {
            blacklistItems.add("rotate")
        } else if (isRotateVisible.value) {
            blacklistItems.remove("rotate")
        }

        // Handle VoLTE visibility
        if (!isVolteVisible.value) {
            if (!blacklistItems.contains("volte")) blacklistItems.add("volte")
            if (!blacklistItems.contains("vowifi")) blacklistItems.add("vowifi")
            if (!blacklistItems.contains("ims_volte")) blacklistItems.add("ims_volte")
        } else {
            blacklistItems.remove("volte")
            blacklistItems.remove("vowifi")
            blacklistItems.remove("ims_volte")
        }

        // Handle Cast visibility
        if (!isCastVisible.value && !blacklistItems.contains("cast")) {
            blacklistItems.add("cast")
        } else if (isCastVisible.value) {
            blacklistItems.remove("cast")
        }

        // Handle Clock visibility
        if (!isClockVisible.value && !blacklistItems.contains("clock")) {
            blacklistItems.add("clock")
        } else if (isClockVisible.value) {
            blacklistItems.remove("clock")
        }

        val newBlacklist = blacklistItems.joinToString(",")

        try {
            Settings.Secure.putString(context.contentResolver, ICON_BLACKLIST_SETTING, newBlacklist)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startSmartDataUpdates(context: Context) {
        smartDataJob?.cancel()
        smartDataJob = scope.launch {
            while (true) {
                val currentNetworkType = getCurrentNetworkType(context)
                updateSmartDataBlacklist(context, currentNetworkType)
                delay(10000) // Check every 10 seconds for network changes
            }
        }
    }

    private fun updateSmartDataBlacklist(context: Context, networkType: NetworkType) {
        if (!isSmartDataEnabled.value || !isWriteSecureSettingsEnabled.value || !isMobileDataVisible.value) {
            return
        }

        // If Smart WiFi is enabled and WiFi is connected, let Smart WiFi handle mobile data visibility
        if (isSmartWiFiEnabled.value && isWifiConnected(context)) {
            return
        }

        val blacklistItems = BASE_BLACKLIST.split(",").toMutableList()

        // Handle WiFi visibility
        if (!isWiFiVisible.value && !blacklistItems.contains("wifi")) {
            blacklistItems.add("wifi")
        } else if (isWiFiVisible.value) {
            blacklistItems.remove("wifi")
        }

        // Handle Mobile Data visibility with Smart Data logic
        val shouldHideMobileData = selectedNetworkTypes.value.contains(networkType) ||
            (selectedNetworkTypes.value.contains(NetworkType.NETWORK_OTHER) &&
             !setOf(NetworkType.NETWORK_5G, NetworkType.NETWORK_4G, NetworkType.NETWORK_3G).contains(networkType))

        if (shouldHideMobileData) {
            // Hide mobile data
            if (!blacklistItems.contains("mobile")) {
                blacklistItems.add("mobile")
            }
        } else {
            // Show mobile data (only if manually enabled)
            if (isMobileDataVisible.value) {
                blacklistItems.remove("mobile")
            }
        }

        // Handle other icon visibility (VPN, Alarm Clock, Hotspot, Bluetooth)
        if (!isVpnVisible.value && !blacklistItems.contains("vpn")) {
            blacklistItems.add("vpn")
        } else if (isVpnVisible.value) {
            blacklistItems.remove("vpn")
        }

        if (!isAlarmClockVisible.value && !blacklistItems.contains("alarm_clock")) {
            blacklistItems.add("alarm_clock")
        } else if (isAlarmClockVisible.value) {
            blacklistItems.remove("alarm_clock")
        }

        if (!isHotspotVisible.value && !blacklistItems.contains("hotspot")) {
            blacklistItems.add("hotspot")
        } else if (isHotspotVisible.value) {
            blacklistItems.remove("hotspot")
        }

        if (!isBluetoothVisible.value && !blacklistItems.contains("bluetooth")) {
            blacklistItems.add("bluetooth")
        } else if (isBluetoothVisible.value) {
            blacklistItems.remove("bluetooth")
        }

        // Handle Data Saver visibility
        if (!isDataSaverVisible.value && !blacklistItems.contains("data_saver")) {
            blacklistItems.add("data_saver")
        } else if (isDataSaverVisible.value) {
            blacklistItems.remove("data_saver")
        }

        // Handle Headset visibility
        if (!isHeadsetVisible.value && !blacklistItems.contains("headset")) {
            blacklistItems.add("headset")
        } else if (isHeadsetVisible.value) {
            blacklistItems.remove("headset")
        }

        // Handle Rotate visibility
        if (!isRotateVisible.value && !blacklistItems.contains("rotate")) {
            blacklistItems.add("rotate")
        } else if (isRotateVisible.value) {
            blacklistItems.remove("rotate")
        }

        // Handle VoLTE visibility
        if (!isVolteVisible.value) {
            if (!blacklistItems.contains("volte")) blacklistItems.add("volte")
            if (!blacklistItems.contains("vowifi")) blacklistItems.add("vowifi")
            if (!blacklistItems.contains("ims_volte")) blacklistItems.add("ims_volte")
        } else {
            blacklistItems.remove("volte")
            blacklistItems.remove("vowifi")
            blacklistItems.remove("ims_volte")
        }

        // Handle Cast visibility
        if (!isCastVisible.value && !blacklistItems.contains("cast")) {
            blacklistItems.add("cast")
        } else if (isCastVisible.value) {
            blacklistItems.remove("cast")
        }

        // Handle Clock visibility
        if (!isClockVisible.value && !blacklistItems.contains("clock")) {
            blacklistItems.add("clock")
        } else if (isClockVisible.value) {
            blacklistItems.remove("clock")
        }

        val newBlacklist = blacklistItems.joinToString(",")

        try {
            Settings.Secure.putString(context.contentResolver, ICON_BLACKLIST_SETTING, newBlacklist)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCurrentNetworkType(context: Context): NetworkType {
        return try {
            // Check if we have READ_PHONE_STATE permission
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return NetworkType.NETWORK_OTHER
            }

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return NetworkType.NETWORK_OTHER
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NETWORK_OTHER

            // If it's WiFi, return OTHER
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return NetworkType.NETWORK_OTHER
            }

            // For cellular networks, use TelephonyManager to get detailed network type
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            @Suppress("DEPRECATION")
            val networkType = telephonyManager.networkType

            when (networkType) {
                // 5G networks
                TelephonyManager.NETWORK_TYPE_NR -> NetworkType.NETWORK_5G

                // 4G networks
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyManager.NETWORK_TYPE_HSPAP -> NetworkType.NETWORK_4G

                // 3G networks
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> NetworkType.NETWORK_3G

                // Everything else is OTHER
                else -> NetworkType.NETWORK_OTHER
            }
        } catch (e: Exception) {
            NetworkType.NETWORK_OTHER
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            false
        }
    }

    private fun loadIconVisibilityState(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        isMobileDataVisible.value = prefs.getBoolean("icon_mobile_visible", true)
        isWiFiVisible.value = prefs.getBoolean("icon_wifi_visible", false)
        isVpnVisible.value = prefs.getBoolean("icon_vpn_visible", false)
        isAlarmClockVisible.value = prefs.getBoolean("icon_alarm_clock_visible", false)
        isHotspotVisible.value = prefs.getBoolean("icon_hotspot_visible", false)
        isBluetoothVisible.value = prefs.getBoolean("icon_bluetooth_visible", false)
        isDataSaverVisible.value = prefs.getBoolean("icon_data_saver_visible", false)
        isHeadsetVisible.value = prefs.getBoolean("icon_headset_visible", false)
        isRotateVisible.value = prefs.getBoolean("icon_rotate_visible", false)
        isVolteVisible.value = prefs.getBoolean("icon_volte_visible", false)
        isCastVisible.value = prefs.getBoolean("icon_cast_visible", false)
        isClockVisible.value = prefs.getBoolean("icon_clock_visible", false)
    }

    private fun loadSmartWiFiPref(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        isSmartWiFiEnabled.value = prefs.getBoolean(PREF_SMART_WIFI_ENABLED, false)
    }

    private fun loadSmartDataPref(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        isSmartDataEnabled.value = prefs.getBoolean(PREF_SMART_DATA_ENABLED, false)
    }

    private fun loadSelectedNetworkTypes(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val currentTypes = prefs.getStringSet(PREF_SELECTED_NETWORK_TYPES, setOf(NetworkType.NETWORK_4G.name, NetworkType.NETWORK_5G.name)) ?: setOf()

        selectedNetworkTypes.value = currentTypes.map { NetworkType.valueOf(it) }.toSet()
    }

    private fun canWriteSecureSettings(context: Context): Boolean {
        return try {
            val currentValue = Settings.Secure.getString(
                context.contentResolver,
                ICON_BLACKLIST_SETTING
            )
            // Try to write the same value back (no-op) to verify permission
            Settings.Secure.putString(
                context.contentResolver,
                ICON_BLACKLIST_SETTING,
                currentValue ?: "$BASE_BLACKLIST,mobile,wifi"
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getAdbCommand(): String {
        return "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
    }

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
        smartWifiJob?.cancel()
        smartDataJob?.cancel()
    }

    fun setVpnVisible(visible: Boolean, context: Context) {
        isVpnVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_vpn_visible", visible)
        }
        updateIconBlacklist(context)
    }

    fun setAlarmClockVisible(visible: Boolean, context: Context) {
        isAlarmClockVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_alarm_clock_visible", visible)
        }
        updateIconBlacklist(context)
    }

    fun setHotspotVisible(visible: Boolean, context: Context) {
        isHotspotVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_hotspot_visible", visible)
        }
        updateIconBlacklist(context)
    }

    fun setBluetoothVisible(visible: Boolean, context: Context) {
        isBluetoothVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_bluetooth_visible", visible)
        }
        updateIconBlacklist(context)
    }

    fun setDataSaverVisible(visible: Boolean, context: Context) {
        isDataSaverVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_data_saver_visible", visible)
        }
        updateIconBlacklist(context)
    }

    fun setHeadsetVisible(visible: Boolean, context: Context) {
        isHeadsetVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_headset_visible", visible)
        }
        updateIconBlacklist(context)
    }

    fun setRotateVisible(visible: Boolean, context: Context) {
        isRotateVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_rotate_visible", visible)
        }
        updateIconBlacklist(context)
    }

    fun setVolteVisible(visible: Boolean, context: Context) {
        isVolteVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_volte_visible", visible)
        }
        updateIconBlacklist(context)
    }

    fun setCastVisible(visible: Boolean, context: Context) {
        isCastVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_cast_visible", visible)
        }
        updateIconBlacklist(context)
    }

    fun setClockVisible(visible: Boolean, context: Context) {
        isClockVisible.value = visible
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("icon_clock_visible", visible)
        }
        updateIconBlacklist(context)
    }
}