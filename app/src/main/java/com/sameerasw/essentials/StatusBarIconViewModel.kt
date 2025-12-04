package com.sameerasw.essentials

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StatusBarIconViewModel : ViewModel() {
    val isWriteSecureSettingsEnabled = mutableStateOf(false)
    val isMobileDataVisible = mutableStateOf(true)
    val isWiFiVisible = mutableStateOf(false)
    val isSmartWiFiEnabled = mutableStateOf(false)

    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val ICON_BLACKLIST_SETTING = "icon_blacklist"
        const val BASE_BLACKLIST = "rotate,vowifi,battery,ims,nfc,vpn,volte,alarm_clock,headset,hotspot,bluetooth,ims_volte,vpn"
        const val PREF_SMART_WIFI_ENABLED = "smart_wifi_enabled"
    }

    fun check(context: Context) {
        isWriteSecureSettingsEnabled.value = canWriteSecureSettings(context)
        loadIconVisibilityState(context)
        loadSmartWiFiPref(context)

        if (isSmartWiFiEnabled.value && isWriteSecureSettingsEnabled.value) {
            startSmartWiFiUpdates(context)
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
            updateJob?.cancel()
            // When disabling smart WiFi, restore manual settings
            updateIconBlacklist(context)
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

        val newBlacklist = blacklistItems.joinToString(",")

        try {
            Settings.Secure.putString(context.contentResolver, ICON_BLACKLIST_SETTING, newBlacklist)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startSmartWiFiUpdates(context: Context) {
        updateJob?.cancel()
        updateJob = scope.launch {
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

        val newBlacklist = blacklistItems.joinToString(",")

        try {
            Settings.Secure.putString(context.contentResolver, ICON_BLACKLIST_SETTING, newBlacklist)
        } catch (e: Exception) {
            e.printStackTrace()
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
    }

    private fun loadSmartWiFiPref(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        isSmartWiFiEnabled.value = prefs.getBoolean(PREF_SMART_WIFI_ENABLED, false)
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
    }
}
