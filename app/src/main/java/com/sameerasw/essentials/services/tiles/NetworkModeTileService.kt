package com.sameerasw.essentials.services.tiles

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.util.Log
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.ShellUtils
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

@RequiresApi(Build.VERSION_CODES.N)
class NetworkModeTileService : BaseTileService() {

    private val TAG = "NetworkModeTile"

    private val subscriptionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            Log.d(TAG, "Broadcast received: ${intent?.action}. Updating network mode tile.")
            updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val filter = android.content.IntentFilter().apply {
            addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")
            addAction("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED")
            addAction("android.telephony.action.CARRIER_CONFIG_CHANGED")
        }
        registerReceiver(subscriptionReceiver, filter)
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        try {
            unregisterReceiver(subscriptionReceiver)
        } catch (_: Exception) {}
    }

    override fun onTileClick() {
        if (!hasFeaturePermission()) return

        val defaultDataSubId = getDefaultDataSubIdFromShell()
        val slotId = getDefaultDataSimSlotFromShell()
        
        val currentMode = getCurrentNetworkMode(defaultDataSubId, slotId)
        val nextMode = when (currentMode) {
            NetworkMode.MODE_NR_ONLY -> NetworkMode.MODE_LTE_ONLY
            else -> NetworkMode.MODE_NR_ONLY
        }

        val bitmask = getBitmaskForMode(nextMode)
        setAllowedNetworkTypes(defaultDataSubId, slotId, bitmask)
        updateTile()
    }

    override fun getTileLabel(): String = getString(R.string.tile_network_mode)

    override fun getTileSubtitle(): String {
        if (!hasFeaturePermission()) return getString(R.string.permission_missing)

        val defaultDataSubId = getDefaultDataSubIdFromShell()
        val slotId = getDefaultDataSimSlotFromShell()
        
        return when (getCurrentNetworkMode(defaultDataSubId, slotId)) {
            NetworkMode.MODE_NR_ONLY -> getString(R.string.network_mode_nr_only)
            NetworkMode.MODE_LTE_ONLY -> getString(R.string.network_mode_lte_only)
            NetworkMode.MODE_5G_PREF -> getString(R.string.network_mode_5g_pref)
            NetworkMode.MODE_3G_ONLY -> getString(R.string.network_mode_3g_only)
            NetworkMode.MODE_2G_ONLY -> getString(R.string.network_mode_2g_only)
        }
    }

    override fun hasFeaturePermission(): Boolean {
        return ShellUtils.isAvailable(this) && ShellUtils.hasPermission(this)
    }

    override fun getTileIcon(): Icon {
        return Icon.createWithResource(this, R.drawable.rounded_signal_cellular_alt_24)
    }

    override fun getTileState(): Int {
        if (!hasFeaturePermission()) return Tile.STATE_INACTIVE
        
        val defaultDataSubId = getDefaultDataSubIdFromShell()
        val slotId = getDefaultDataSimSlotFromShell()
        
        return when (getCurrentNetworkMode(defaultDataSubId, slotId)) {
            NetworkMode.MODE_NR_ONLY, NetworkMode.MODE_LTE_ONLY -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
    }

    private enum class NetworkMode {
        MODE_5G_PREF, MODE_NR_ONLY, MODE_LTE_ONLY, MODE_3G_ONLY, MODE_2G_ONLY
    }

    private fun getDefaultDataSimSlotFromShell(): Int {
        val defaultDataSubId = getDefaultDataSubIdFromShell()
        val output = ShellUtils.runCommandWithOutput(this, "dumpsys isub") ?: return 0
        
        output.lineSequence().forEach { line ->
            if (line.contains("SubscriptionInfoInternal:")) {
                val id = parseValue(line, "id=")?.toIntOrNull()
                val simSlotIndex = parseValue(line, "simSlotIndex=")?.toIntOrNull()
                if (id == defaultDataSubId && simSlotIndex != null && simSlotIndex >= 0) {
                    return simSlotIndex
                }
            }
        }
        return 0
    }

    private fun getDefaultDataSubIdFromShell(): Int {
        val output = ShellUtils.runCommandWithOutput(this, "settings get global multi_sim_data_call")
        return output?.trim()?.toIntOrNull() ?: -1
    }

    private fun parseValue(line: String, key: String): String? {
        val startIndex = line.indexOf(key)
        if (startIndex == -1) return null
        val valStart = startIndex + key.length
        
        var valEnd = valStart
        while (valEnd < line.length) {
            val c = line[valEnd]
            if (c == ' ' || c == ',' || c == ']') {
                break
            }
            valEnd++
        }
        if (valEnd > valStart) {
            return line.substring(valStart, valEnd).trim()
        }
        return null
    }

    private fun getCurrentNetworkMode(subId: Int, slotId: Int): NetworkMode {
        var currentAllowed: Long = -1L
        
        // Try getting via Shizuku binder wrapper reflection first
        try {
            val binder = SystemServiceHelper.getSystemService("phone")
            if (binder != null) {
                val wrappedBinder = ShizukuBinderWrapper(binder)
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
                val iTelephonyInstance = asInterfaceMethod.invoke(null, wrappedBinder)
                
                val getMethod = iTelephonyInstance.javaClass.getMethod(
                    "getAllowedNetworkTypesForReason",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                currentAllowed = getMethod.invoke(iTelephonyInstance, subId, 0) as Long
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting allowed network types via standard reflection", e)
        }

        // If reflection failed or binder is null, fallback to shell
        if (currentAllowed == -1L) {
            val output = ShellUtils.runCommandWithOutput(this, "cmd phone get-allowed-network-types-for-users -s $slotId")
            if (output.isNullOrBlank()) {
                return NetworkMode.MODE_5G_PREF
            }
            val hasNR = output.contains("NR", ignoreCase = true)
            val hasLTE = output.contains("LTE", ignoreCase = true)
            val has3G = output.contains("UMTS", ignoreCase = true) || output.contains("HSPA", ignoreCase = true) || output.contains("HSDPA", ignoreCase = true)
            val has2G = output.contains("GSM", ignoreCase = true) || output.contains("GPRS", ignoreCase = true) || output.contains("EDGE", ignoreCase = true)

            return when {
                hasNR && !hasLTE && !has3G && !has2G -> NetworkMode.MODE_NR_ONLY
                !hasNR && hasLTE && !has3G && !has2G -> NetworkMode.MODE_LTE_ONLY
                !hasNR && !hasLTE && has3G && !has2G -> NetworkMode.MODE_3G_ONLY
                !hasNR && !hasLTE && !has3G && has2G -> NetworkMode.MODE_2G_ONLY
                else -> NetworkMode.MODE_5G_PREF
            }
        }

        val bitmask2G = getBitmaskForTech(16) or getBitmaskForTech(1) or getBitmaskForTech(2) or getBitmaskForTech(4) or getBitmaskForTech(7) or getBitmaskForTech(11)
        val bitmask3G = getBitmaskForTech(3) or getBitmaskForTech(5) or getBitmaskForTech(6) or getBitmaskForTech(12) or getBitmaskForTech(8) or getBitmaskForTech(9) or getBitmaskForTech(10) or getBitmaskForTech(14) or getBitmaskForTech(15)
        val bitmask4G = getBitmaskForTech(13) or getBitmaskForTech(19)
        val bitmask5G = getBitmaskForTech(20)

        val hasNR = (currentAllowed and bitmask5G) != 0L
        val hasLTE = (currentAllowed and bitmask4G) != 0L
        val has3G = (currentAllowed and bitmask3G) != 0L
        val has2G = (currentAllowed and bitmask2G) != 0L

        return when {
            hasNR && !hasLTE && !has3G && !has2G -> NetworkMode.MODE_NR_ONLY
            !hasNR && hasLTE && !has3G && !has2G -> NetworkMode.MODE_LTE_ONLY
            !hasNR && !hasLTE && has3G && !has2G -> NetworkMode.MODE_3G_ONLY
            !hasNR && !hasLTE && !has3G && has2G -> NetworkMode.MODE_2G_ONLY
            else -> NetworkMode.MODE_5G_PREF
        }
    }

    private fun setAllowedNetworkTypes(subId: Int, slotId: Int, bitmask: Long) {
        // Try setting via Shizuku binder wrapper reflection first
        try {
            val binder = SystemServiceHelper.getSystemService("phone")
            if (binder != null) {
                val wrappedBinder = ShizukuBinderWrapper(binder)
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
                val iTelephonyInstance = asInterfaceMethod.invoke(null, wrappedBinder)
                
                var setMethod: java.lang.reflect.Method? = null
                var args = arrayOf<Any>()

                // Try 4-argument signature (Android 14/15/16/17+)
                try {
                    setMethod = iTelephonyInstance.javaClass.getMethod(
                        "setAllowedNetworkTypesForReason",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Long::class.javaPrimitiveType,
                        String::class.java
                    )
                    args = arrayOf(subId, 0, bitmask, packageName)
                } catch (e: NoSuchMethodException) {
                    // Fall back to 3-argument signature (Android 11/12/13)
                    try {
                        setMethod = iTelephonyInstance.javaClass.getMethod(
                            "setAllowedNetworkTypesForReason",
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                            Long::class.javaPrimitiveType
                        )
                        args = arrayOf(subId, 0, bitmask)
                    } catch (ex: NoSuchMethodException) {
                        Log.e(TAG, "Both 4-argument and 3-argument setAllowedNetworkTypesForReason methods not found", ex)
                    }
                }

                if (setMethod != null) {
                    setMethod.invoke(iTelephonyInstance, *args)
                    Log.d(TAG, "Successfully set allowed network types to $bitmask via standard reflection")
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting allowed network types via standard reflection", e)
        }

        // Fallback for Root/other cases: write via shell
        val bitmaskString = longToBinaryString20(bitmask)
        Log.w(TAG, "Shizuku binder reflection failed. Falling back to shell command.")
        ShellUtils.runCommand(this, "cmd phone set-allowed-network-types-for-users -s $slotId $bitmaskString")
    }

    private fun getBitmaskForMode(mode: NetworkMode): Long {
        val bitmask2G = getBitmaskForTech(16) or
                getBitmaskForTech(1) or
                getBitmaskForTech(2) or
                getBitmaskForTech(4) or
                getBitmaskForTech(7) or
                getBitmaskForTech(11)

        val bitmask3G = bitmask2G or
                getBitmaskForTech(3) or
                getBitmaskForTech(5) or
                getBitmaskForTech(6) or
                getBitmaskForTech(12) or
                getBitmaskForTech(8) or
                getBitmaskForTech(9) or
                getBitmaskForTech(10) or
                getBitmaskForTech(14) or
                getBitmaskForTech(15)

        val bitmask4G = getBitmaskForTech(13) or getBitmaskForTech(19)
        val bitmask5G = getBitmaskForTech(20)

        return when (mode) {
            NetworkMode.MODE_5G_PREF -> bitmask2G or bitmask3G or bitmask4G or bitmask5G
            NetworkMode.MODE_NR_ONLY -> bitmask5G
            NetworkMode.MODE_LTE_ONLY -> bitmask4G
            NetworkMode.MODE_3G_ONLY -> bitmask3G
            NetworkMode.MODE_2G_ONLY -> bitmask2G
        }
    }

    private fun getBitmaskForTech(tech: Int): Long {
        return if (tech > 0) (1L shl (tech - 1)) else 0L
    }

    private fun longToBinaryString20(mask: Long): String {
        val sb = java.lang.StringBuilder()
        for (i in 20 downTo 1) {
            val bit = if ((mask and (1L shl (i - 1))) != 0L) '1' else '0'
            sb.append(bit)
        }
        return sb.toString()
    }
}
