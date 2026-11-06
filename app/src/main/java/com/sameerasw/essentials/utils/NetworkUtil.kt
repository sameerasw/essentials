package com.sameerasw.essentials.utils

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtil {
    private val nonVpnTransports =
        intArrayOf(
            NetworkCapabilities.TRANSPORT_WIFI,
            NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.TRANSPORT_ETHERNET,
            NetworkCapabilities.TRANSPORT_BLUETOOTH,
        )

    fun underlyingWifi(manager: ConnectivityManager?): NetworkCapabilities? {
        manager ?: return null
        val active = manager.activeNetwork ?: return null
        val activeCaps = manager.getNetworkCapabilities(active) ?: return null
        if (!activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            return activeCaps.takeIf { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }
        }
        val declaresOtherTransport = nonVpnTransports.any { it != NetworkCapabilities.TRANSPORT_WIFI && activeCaps.hasTransport(it) }
        if (declaresOtherTransport && !activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        return manager.allNetworks
            .mapNotNull { manager.getNetworkCapabilities(it) }
            .firstOrNull { it.isPlainWifi() }
    }

    fun isWifiConnected(manager: ConnectivityManager?): Boolean = underlyingWifi(manager) != null

    private fun NetworkCapabilities.isPlainWifi() =
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            !hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
