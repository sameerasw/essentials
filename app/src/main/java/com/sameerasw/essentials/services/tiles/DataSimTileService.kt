package com.sameerasw.essentials.services.tiles

import android.content.Context
import com.sameerasw.essentials.utils.TelephonyParser
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
class DataSimTileService : BaseTileService() {

    private val TAG = "DataSimTile"

    private val subscriptionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            Log.d(TAG, "Broadcast received: ${intent?.action}. Updating tile.")
            updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "onStartListening called")
        val filter = android.content.IntentFilter().apply {
            addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")
            addAction("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED")
        }
        registerReceiver(subscriptionReceiver, filter)
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.d(TAG, "onStopListening called")
        try {
            unregisterReceiver(subscriptionReceiver)
        } catch (_: Exception) {}
    }

    override fun onTileClick() {
        Log.d(TAG, "onTileClick initiated")
        if (!hasFeaturePermission()) {
            Log.w(TAG, "No shell/root permission")
            return
        }

        val activeSubs = TelephonyParser.parseActiveSims(ShellUtils.runCommandWithOutput(this, "dumpsys isub") ?: "")
        if (activeSubs.isEmpty()) {
            Log.w(TAG, "No active subscriptions found from shell")
            return
        }

        if (activeSubs.size < 2) {
            Log.w(TAG, "Only ${activeSubs.size} active SIMs. Toggle requires at least 2.")
            return
        }

        val defaultDataSubId = getDefaultDataSubIdFromShell()
        Log.d(TAG, "Current default data subId: $defaultDataSubId")

        val nextSub = activeSubs.firstOrNull { it.id != defaultDataSubId }
        if (nextSub != null) {
            val nextSubId = nextSub.id
            Log.d(TAG, "Targeting next data SIM: subId=$nextSubId, slotIndex=${nextSub.slotIndex}")
            setDefaultDataSubId(nextSubId)
            enableMobileData(nextSubId)
        } else {
            Log.e(TAG, "Could not find next subscription")
        }
    }

    private fun setDefaultDataSubId(subId: Int) {
        try {
            val binder = SystemServiceHelper.getSystemService("isub")
            if (binder != null) {
                val wrappedBinder = ShizukuBinderWrapper(binder)
                val stubClass = Class.forName("com.android.internal.telephony.ISub\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
                val iSubInstance = asInterfaceMethod.invoke(null, wrappedBinder)
                
                val setDefaultDataSubIdMethod = iSubInstance.javaClass.getMethod(
                    "setDefaultDataSubId",
                    Int::class.javaPrimitiveType
                )
                setDefaultDataSubIdMethod.invoke(iSubInstance, subId)
                Log.d(TAG, "Successfully set default data subscription to $subId via standard Java reflection")
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting default data sub via standard Java reflection", e)
        }
        
        // Fallback for Root/other cases: write setting
        Log.w(TAG, "Shizuku binder reflection failed. Attempting settings write.")
        ShellUtils.runCommand(this, "settings put global multi_sim_data_call $subId")
    }

    private fun enableMobileData(subId: Int) {
        try {
            val binder = SystemServiceHelper.getSystemService("phone")
            if (binder != null) {
                val wrappedBinder = ShizukuBinderWrapper(binder)
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
                val iTelephonyInstance = asInterfaceMethod.invoke(null, wrappedBinder)
                
                var setDataEnabledMethod: java.lang.reflect.Method? = null
                var args = arrayOf<Any>()

                // Try setDataEnabledForReason(int subId, int reason, boolean enable, String callingPackage) - Android 14+
                try {
                    setDataEnabledMethod = iTelephonyInstance.javaClass.getMethod(
                        "setDataEnabledForReason",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType,
                        String::class.java
                    )
                    args = arrayOf(subId, 0, true, packageName)
                } catch (e: NoSuchMethodException) {
                    // Try setDataEnabled(int subId, boolean enable) - older versions
                    try {
                        setDataEnabledMethod = iTelephonyInstance.javaClass.getMethod(
                            "setDataEnabled",
                            Int::class.javaPrimitiveType,
                            Boolean::class.javaPrimitiveType
                        )
                        args = arrayOf(subId, true)
                    } catch (ex: NoSuchMethodException) {
                        Log.e(TAG, "Could not find setDataEnabled or setDataEnabledForReason method", ex)
                    }
                }

                if (setDataEnabledMethod != null) {
                    setDataEnabledMethod.invoke(iTelephonyInstance, *args)
                    Log.d(TAG, "Successfully enabled mobile data for subId=$subId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling mobile data for subId=$subId", e)
        }
    }

    override fun getTileLabel(): String = getString(R.string.tile_data_sim)

    override fun getTileSubtitle(): String {
        if (!hasFeaturePermission()) return getString(R.string.permission_missing)

        val activeSubs = TelephonyParser.parseActiveSims(ShellUtils.runCommandWithOutput(this, "dumpsys isub") ?: "")
        if (activeSubs.isEmpty()) {
            return getString(R.string.off)
        }

        val defaultDataSubId = getDefaultDataSubIdFromShell()
        val currentSub = activeSubs.find { it.id == defaultDataSubId }

        val subtitle = if (currentSub != null) {
            val displayName = currentSub.displayName
            if (displayName.isNotBlank()) {
                "SIM ${currentSub.slotIndex + 1}: $displayName"
            } else {
                "SIM ${currentSub.slotIndex + 1}"
            }
        } else {
            getString(R.string.off)
        }
        Log.d(TAG, "getTileSubtitle returning: $subtitle")
        return subtitle
    }

    override fun hasFeaturePermission(): Boolean {
        val hasShell = ShellUtils.isAvailable(this) && ShellUtils.hasPermission(this)
        Log.d(TAG, "hasFeaturePermission check: hasShell=$hasShell")
        return hasShell
    }

    override fun getTileIcon(): Icon {
        return Icon.createWithResource(this, R.drawable.rounded_android_cell_dual_4_bar_24)
    }

    override fun getTileState(): Int {
        if (!hasFeaturePermission()) return Tile.STATE_INACTIVE

        val activeSubs = TelephonyParser.parseActiveSims(ShellUtils.runCommandWithOutput(this, "dumpsys isub") ?: "")
        if (activeSubs.isEmpty()) return Tile.STATE_INACTIVE

        val defaultDataSubId = getDefaultDataSubIdFromShell()
        val currentSub = activeSubs.find { it.id == defaultDataSubId }

        val state = if (currentSub?.slotIndex == 0) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        Log.d(TAG, "getTileState returning: $state")
        return state
    }

    private fun getDefaultDataSubIdFromShell(): Int {
        val output = ShellUtils.runCommandWithOutput(this, "settings get global multi_sim_data_call")
        return output?.trim()?.toIntOrNull() ?: -1
    }
}
