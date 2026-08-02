package com.sameerasw.essentials.utils

import android.content.Context
import android.net.wifi.WifiManager

object WifiUtil {
    private const val UNKNOWN_SSID = "<unknown ssid>"
    private val SSID_REGEX = Regex("\"([^\"]*)\"")

    @Suppress("DEPRECATION")
    fun getCurrentSsid(context: Context): String? {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return null
        val ssid = wifiManager.connectionInfo?.ssid ?: return null
        if (ssid.isEmpty() || ssid == UNKNOWN_SSID) return null
        return ssid.removeSurrounding("\"")
    }

    /**
     * Shizuku/root grant shell-level privileges, which can run `cmd wifi list-networks`
     * (normally restricted to system apps) to read the device's saved Wi-Fi networks.
     */
    fun canReadSavedNetworks(context: Context): Boolean {
        return ShellUtils.isAvailable(context) && ShellUtils.hasPermission(context)
    }

    fun getSavedNetworkSsids(context: Context): List<String> {
        val output = ShellUtils.runCommandWithOutput(context, "cmd wifi list-networks")
            ?: return emptyList()
        return output.lineSequence()
            .drop(1) // header row: "Network Id      SSID      Security type"
            .mapNotNull { line -> SSID_REGEX.find(line)?.groupValues?.get(1) }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }
}
