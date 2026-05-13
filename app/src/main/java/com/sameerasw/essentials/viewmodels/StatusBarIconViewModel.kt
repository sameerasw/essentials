package com.sameerasw.essentials.viewmodels

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.sameerasw.essentials.domain.StatusBarIconRegistry
import com.sameerasw.essentials.ui.components.pickers.NetworkType
import com.sameerasw.essentials.utils.resetAllIconVisibilities
import com.sameerasw.essentials.utils.updateIconBlacklistSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class StatusBarIconViewModel : ViewModel() {
    val isWriteSecureSettingsEnabled = mutableStateOf(false)
    val isSmartWiFiEnabled = mutableStateOf(false)
    val isSmartDataEnabled = mutableStateOf(false)
    val selectedNetworkTypes = mutableStateOf(setOf(NetworkType.NETWORK_4G, NetworkType.NETWORK_5G))
    val isClockSecondsEnabled = mutableStateOf(false)
    val batteryPercentageMode = mutableStateOf(0) // 0: Hide, 1: Always, 2: Charging
    val isPrivacyChipEnabled = mutableStateOf(true)
    val isWriteSettingsEnabled = mutableStateOf(false)

    val isHideSystemIconsEnabled = mutableStateOf(false)
    val isHideSystemIconsLockedOnlyEnabled = mutableStateOf(false)
    val isHideClockEnabled = mutableStateOf(false)
    val isHideNotificationIconsEnabled = mutableStateOf(false)


    // Dynamic icon visibility states based on registry
    private val iconVisibilities =
        mutableMapOf<String, androidx.compose.runtime.MutableState<Boolean>>()

    private var updateJob: Job? = null
    private var smartWifiJob: Job? = null
    private var smartDataJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var batteryReceiver: android.content.BroadcastReceiver? = null

    companion object {
        const val ICON_BLACKLIST_SETTING = "icon_blacklist"
        const val PREF_SMART_WIFI_ENABLED = "smart_wifi_enabled"
        const val PREF_SMART_DATA_ENABLED = "smart_data_enabled"
        const val PREF_SELECTED_NETWORK_TYPES = "selected_network_types"
        const val PREF_BATTERY_PERCENT_MODE = "battery_percent_mode"
        const val PREF_HIDE_SYSTEM_ICONS = "hide_system_icons"
        const val PREF_HIDE_SYSTEM_ICONS_LOCKED_ONLY = "hide_system_icons_locked_only"
        const val PREF_HIDE_CLOCK = "hide_clock"
        const val PREF_HIDE_NOTIFICATION_ICONS = "hide_notification_icons"
        private const val ADVANCED_FLAGS_REQUESTER_ID = "StatusBarIconAdvanced"
    }

    init {
        // Initialize visibility states for all icons
        for (icon in StatusBarIconRegistry.ALL_ICONS) {
            iconVisibilities[icon.id] = mutableStateOf(icon.defaultVisible)
        }
    }

    /**
     * Get visibility state for a specific icon by ID
     */
    fun getIconVisibility(iconId: String): androidx.compose.runtime.MutableState<Boolean>? {
        return iconVisibilities[iconId]
    }

    /**
     * Get all visibility states as a map
     */
    fun getIconVisibilities(): Map<String, Boolean> {
        return iconVisibilities.mapValues { it.value.value }
    }

    // Backward compatibility properties for UI
    val isMobileDataVisible get() = iconVisibilities["mobile_data"] ?: mutableStateOf(true)
    val isWiFiVisible get() = iconVisibilities["wifi"] ?: mutableStateOf(true)
    val isVpnVisible get() = iconVisibilities["vpn"] ?: mutableStateOf(true)
    val isAlarmClockVisible get() = iconVisibilities["alarm"] ?: mutableStateOf(false)
    val isHotspotVisible get() = iconVisibilities["hotspot"] ?: mutableStateOf(true)
    val isBluetoothVisible get() = iconVisibilities["bluetooth"] ?: mutableStateOf(true)
    val isDataSaverVisible get() = iconVisibilities["data_saver"] ?: mutableStateOf(true)
    val isHeadsetVisible get() = iconVisibilities["headset"] ?: mutableStateOf(false)
    val isRotateVisible get() = iconVisibilities["rotate"] ?: mutableStateOf(false)
    val isVolteVisible get() = iconVisibilities["volte"] ?: mutableStateOf(true)
    val isCastVisible get() = iconVisibilities["cast"] ?: mutableStateOf(true)
    val isClockVisible get() = iconVisibilities["clock"] ?: mutableStateOf(true)
    val isAirplaneVisible get() = iconVisibilities["airplane_mode"] ?: mutableStateOf(true)

    val isShizukuAvailable = mutableStateOf(false)
    val isRootAvailable = mutableStateOf(false)

    fun check(context: Context) {
        isWriteSecureSettingsEnabled.value = canWriteSecureSettings(context)
        isShizukuAvailable.value = com.sameerasw.essentials.utils.ShizukuUtils.hasPermission()
        isRootAvailable.value = com.sameerasw.essentials.utils.RootUtils.isRootPermissionGranted()
        loadIconVisibilityState(context)
        loadSmartWiFiPref(context)
        loadSmartDataPref(context)
        loadSelectedNetworkTypes(context)
        isWriteSettingsEnabled.value =
            com.sameerasw.essentials.utils.PermissionUtils.canWriteSystemSettings(context)
        loadStatusBarSettings(context)
        loadAdvancedFlags(context)

        loadStatusBarSettings(context)
        loadAdvancedFlags(context)

        // Initial update for UI consistency
        updateIconBlacklist(context)
    }

    /**
     * Generic method to set icon visibility
     * Reduces code duplication by handling all icon types
     */
    fun setIconVisibility(iconId: String, visible: Boolean, context: Context) {
        val iconState = iconVisibilities[iconId] ?: return
        iconState.value = visible

        // Save to preferences
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean(
                StatusBarIconRegistry.getIconById(iconId)?.preferencesKey
                    ?: "icon_${iconId}_visible", visible
            )
        }

        updateIconBlacklist(context)
    }

    // Backward compatibility methods
    fun setMobileDataVisible(visible: Boolean, context: Context) {
        setIconVisibility("mobile_data", visible, context)
    }

    fun setWiFiVisible(visible: Boolean, context: Context) {
        setIconVisibility("wifi", visible, context)
    }

    fun setVpnVisible(visible: Boolean, context: Context) {
        setIconVisibility("vpn", visible, context)
    }

    fun setAlarmClockVisible(visible: Boolean, context: Context) {
        setIconVisibility("alarm", visible, context)
    }

    fun setHotspotVisible(visible: Boolean, context: Context) {
        setIconVisibility("hotspot", visible, context)
    }

    fun setBluetoothVisible(visible: Boolean, context: Context) {
        setIconVisibility("bluetooth", visible, context)
    }

    fun setDataSaverVisible(visible: Boolean, context: Context) {
        setIconVisibility("data_saver", visible, context)
    }

    fun setHeadsetVisible(visible: Boolean, context: Context) {
        setIconVisibility("headset", visible, context)
    }

    fun setRotateVisible(visible: Boolean, context: Context) {
        setIconVisibility("rotate", visible, context)
    }

    fun setVolteVisible(visible: Boolean, context: Context) {
        setIconVisibility("volte", visible, context)
    }

    fun setCastVisible(visible: Boolean, context: Context) {
        setIconVisibility("cast", visible, context)
    }

    fun setClockVisible(visible: Boolean, context: Context) {
        setIconVisibility("clock", visible, context)
    }

    fun setAirplaneVisible(visible: Boolean, context: Context) {
        setIconVisibility("airplane_mode", visible, context)
    }

    fun setSmartWiFiEnabled(enabled: Boolean, context: Context) {
        isSmartWiFiEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean(PREF_SMART_WIFI_ENABLED, enabled)
        }
        updateIconBlacklist(context)
    }

    fun setSmartDataEnabled(enabled: Boolean, context: Context) {
        isSmartDataEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean(PREF_SMART_DATA_ENABLED, enabled)
        }
        updateIconBlacklist(context)
        updateSelectedNetworkTypes(context, enabled)
    }

    fun updateSelectedNetworkTypes(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val currentTypes = prefs.getStringSet(
            PREF_SELECTED_NETWORK_TYPES,
            setOf(NetworkType.NETWORK_4G.name, NetworkType.NETWORK_5G.name)
        )?.toMutableSet() ?: mutableSetOf()

        if (enabled) {
            currentTypes.add(NetworkType.NETWORK_5G.name)
            currentTypes.add(NetworkType.NETWORK_4G.name)
        } else {
            currentTypes.remove(NetworkType.NETWORK_5G.name)
            currentTypes.remove(NetworkType.NETWORK_4G.name)
        }

        selectedNetworkTypes.value = currentTypes.map { NetworkType.valueOf(it) }.toSet()

        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putStringSet(PREF_SELECTED_NETWORK_TYPES, currentTypes)
        }
    }

    /**
     * Update the icon blacklist setting based on current visibility states
     * Uses efficient Set-based operations through StatusBarIconRegistry
     */
    private fun updateIconBlacklist(context: Context) {
        if (!isWriteSecureSettingsEnabled.value) return

        val blacklistNames = StatusBarIconRegistry.getBlacklistNames(getIconVisibilities())
        updateIconBlacklistSetting(context, blacklistNames)
    }

    private fun isWifiConnected(context: Context): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            false
        }
    }

    private fun loadIconVisibilityState(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        for (icon in StatusBarIconRegistry.ALL_ICONS) {
            val visibility = prefs.getBoolean(icon.preferencesKey, icon.defaultVisible)
            iconVisibilities[icon.id]?.value = visibility
        }
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
        val currentTypes = prefs.getStringSet(
            PREF_SELECTED_NETWORK_TYPES,
            setOf(NetworkType.NETWORK_4G.name, NetworkType.NETWORK_5G.name)
        ) ?: setOf()
        selectedNetworkTypes.value = currentTypes.map { NetworkType.valueOf(it) }.toSet()
    }

    private fun canWriteSecureSettings(context: Context): Boolean {
        return com.sameerasw.essentials.utils.PermissionUtils.canWriteSecureSettings(context)
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

    /**
     * Reset all icons to their default visibility states
     */
    fun resetAllIcons(context: Context) {
        // Reset blacklist setting
        try {
            Settings.Secure.putString(context.contentResolver, ICON_BLACKLIST_SETTING, null)
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            e.printStackTrace()
        }

        // Reset UI state to defaults
        for (icon in StatusBarIconRegistry.ALL_ICONS) {
            iconVisibilities[icon.id]?.value = icon.defaultVisible
        }

        // Turn off smart features
        isSmartWiFiEnabled.value = false
        isSmartDataEnabled.value = false

        // Reset advanced settings
        if (isWriteSecureSettingsEnabled.value) {
            setClockSecondsEnabled(false, context)
            setPrivacyChipEnabled(true, context)
        }
        setAdvancedFlagEnabled(context, PREF_HIDE_SYSTEM_ICONS, false)
        setAdvancedFlagEnabled(context, PREF_HIDE_CLOCK, false)
        setAdvancedFlagEnabled(context, PREF_HIDE_NOTIFICATION_ICONS, false)
        setBatteryPercentageMode(0, context)

        // Build default visibility map
        val defaultVisibilities =
            StatusBarIconRegistry.ALL_ICONS.associate { it.id to it.defaultVisible }

        // Clear preferences
        resetAllIconVisibilities(context, defaultVisibilities)
    }

    private fun loadStatusBarSettings(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        batteryPercentageMode.value = prefs.getInt(PREF_BATTERY_PERCENT_MODE, 0)

        // Load Clock Seconds
        isClockSecondsEnabled.value =
            (Settings.Secure.getInt(context.contentResolver, "clock_seconds", 0) == 1 ||
                    Settings.System.getInt(context.contentResolver, "clock_seconds", 0) == 1)

        // Load Privacy Chip
        isPrivacyChipEnabled.value =
            (Settings.Secure.getInt(context.contentResolver, "privacy_chip_2447_enabled", 1) == 1 ||
                    Settings.System.getInt(
                        context.contentResolver,
                        "privacy_chip_2447_enabled",
                        1
                    ) == 1)
    }

    fun setClockSecondsEnabled(enabled: Boolean, context: Context) {
        isClockSecondsEnabled.value = enabled
        updateSettingsValue(context, "clock_seconds", if (enabled) 1 else 0)
    }

    fun setPrivacyChipEnabled(enabled: Boolean, context: Context) {
        isPrivacyChipEnabled.value = enabled
        updateSettingsValue(context, "privacy_chip_2447_enabled", if (enabled) 1 else 0)
    }

    fun setBatteryPercentageMode(mode: Int, context: Context) {
        batteryPercentageMode.value = mode
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putInt(PREF_BATTERY_PERCENT_MODE, mode)
        }

        val systemValue = when (mode) {
            1 -> 1 // Always
            2 -> { // Charging
                val batteryStatus: Intent? =
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                        context.registerReceiver(null, ifilter)
                    }
                val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                if (isCharging) 1 else 0
            }

            else -> 0 // Never
        }

        updateSettingsValue(context, "status_bar_show_battery_percent", systemValue)
    }

    private fun updateSettingsValue(context: Context, key: String, value: Int) {
        var success = false
        // Try System first
        try {
            success = Settings.System.putInt(context.contentResolver, key, value)
        } catch (e: Exception) {
            // Some keys are mirrored in Secure
            try {
                success = Settings.Secure.putInt(context.contentResolver, key, value)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }

        val currentValue = try {
            Settings.System.getInt(context.contentResolver, key, -1)
        } catch (e: Exception) {
            -1
        }

        // If standard API failed, fallback to Shizuku OR Root
        if (!success || currentValue != value) {
            if (com.sameerasw.essentials.utils.ShizukuUtils.hasPermission()) {
                com.sameerasw.essentials.utils.ShizukuUtils.runCommand("settings put system $key $value")
                com.sameerasw.essentials.utils.ShizukuUtils.runCommand("settings put secure $key $value")
            } else if (com.sameerasw.essentials.utils.RootUtils.isRootPermissionGranted()) {
                com.sameerasw.essentials.utils.RootUtils.runCommand("settings put system $key $value")
                com.sameerasw.essentials.utils.RootUtils.runCommand("settings put secure $key $value")
            }
        }
    }

    private fun loadAdvancedFlags(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        isHideSystemIconsEnabled.value = prefs.getBoolean(PREF_HIDE_SYSTEM_ICONS, false)
        isHideSystemIconsLockedOnlyEnabled.value =
            prefs.getBoolean(PREF_HIDE_SYSTEM_ICONS_LOCKED_ONLY, false)
        isHideClockEnabled.value = prefs.getBoolean(PREF_HIDE_CLOCK, false)
        isHideNotificationIconsEnabled.value = prefs.getBoolean(PREF_HIDE_NOTIFICATION_ICONS, false)
        applyAdvancedFlags(context)
    }

    fun setAdvancedFlagEnabled(context: Context, flagKey: String, enabled: Boolean) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        when (flagKey) {
            PREF_HIDE_SYSTEM_ICONS -> isHideSystemIconsEnabled.value = enabled
            PREF_HIDE_SYSTEM_ICONS_LOCKED_ONLY -> isHideSystemIconsLockedOnlyEnabled.value = enabled
            PREF_HIDE_CLOCK -> isHideClockEnabled.value = enabled
            PREF_HIDE_NOTIFICATION_ICONS -> isHideNotificationIconsEnabled.value = enabled
        }
        prefs.edit { putBoolean(flagKey, enabled) }
        applyAdvancedFlags(context)
    }

    private fun applyAdvancedFlags(context: Context) {
        val flags = mutableSetOf<String>()
        if (isHideSystemIconsEnabled.value && !isHideSystemIconsLockedOnlyEnabled.value) {
            flags.add(com.sameerasw.essentials.utils.StatusBarManager.FLAG_SYSTEM_ICONS)
        }
        if (isHideClockEnabled.value) flags.add(com.sameerasw.essentials.utils.StatusBarManager.FLAG_CLOCK)
        if (isHideNotificationIconsEnabled.value) flags.add(com.sameerasw.essentials.utils.StatusBarManager.FLAG_NOTIFICATION_ICONS)

        if (flags.isNotEmpty()) {
            com.sameerasw.essentials.utils.StatusBarManager.requestDisable(
                context,
                ADVANCED_FLAGS_REQUESTER_ID,
                flags
            )
        } else {
            com.sameerasw.essentials.utils.StatusBarManager.requestRestore(
                context,
                ADVANCED_FLAGS_REQUESTER_ID
            )
        }
    }
}

