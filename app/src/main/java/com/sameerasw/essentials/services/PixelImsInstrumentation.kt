package com.sameerasw.essentials.services

import android.annotation.SuppressLint
import android.app.Instrumentation
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.BuildConfig
import rikka.shizuku.ShizukuProvider.METHOD_GET_BINDER

class PixelImsInstrumentation : Instrumentation() {
    companion object {
        const val TAG = "PixelIms"
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(arguments: Bundle?) {
        val binder = object : Binder() {
            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                if (code == 1) {
                    try {
                        val context = context
                        val persistent = canPersistent(context)
                        overrideConfig(context, persistent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in instrumentation transaction: ${Log.getStackTraceString(e)}")
                    }
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({ finish(0, Bundle()) }, 1000)
                    return true
                }
                return super.onTransact(code, data, reply, flags)
            }
        }
        val extras = Bundle()
        extras.putBinder("binder", binder)
        val cr = context.contentResolver
        cr.call("${BuildConfig.APPLICATION_ID}.shizuku", METHOD_GET_BINDER, null, extras)
    }

    @SuppressLint("PrivateApi")
    private fun canPersistent(context: Context): Boolean {
        return try {
            val phoneContext = context.createPackageContext("com.android.phone",
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY)
            val clazz = phoneContext.classLoader.loadClass("com.android.phone.CarrierConfigLoader")
            try {
                clazz.getDeclaredMethod("isSystemApp")
            } catch (_: NoSuchMethodException) {
                return true
            }
            clazz.getDeclaredMethod("secureOverrideConfig", PersistableBundle::class.java, Boolean::class.java)
            true
        } catch (_: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private fun overrideConfig(context: Context, persistent: Boolean) {
        try {
            val cm = context.getSystemService(CarrierConfigManager::class.java)
            val sm = context.getSystemService(SubscriptionManager::class.java)
            val values = getConfig()
            
            // Use reflection for overrideConfig as it is a hidden API
            val overrideConfigMethod = cm.javaClass.getDeclaredMethod("overrideConfig", Int::class.java, PersistableBundle::class.java, Boolean::class.java)

            val getActiveSubscriptionIdListMethod = sm.javaClass.getDeclaredMethod("getActiveSubscriptionIdList")
            val subIds = getActiveSubscriptionIdListMethod.invoke(sm) as IntArray

            for (subId in subIds) {
                val bundle = cm.getConfigForSubId(subId)
                if (bundle == null || bundle.getInt("vvb2060_config_version", 0) != 5) { // Using 5 as in original BuildConfig.VERSION_CODE
                    values.putInt("vvb2060_config_version", 5)
                    overrideConfigMethod.invoke(cm, subId, values, persistent)
                    Log.d(TAG, "Config overridden for subId: $subId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error overriding config: ${Log.getStackTraceString(e)}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getConfig(): PersistableBundle {
        val bundle = PersistableBundle()
        
        // Use generic keys to avoid dependency on specific SDK versions if possible, 
        // but these are fairly standard in CarrierConfigManager
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true)
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true)
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true)

        // KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL might not be available on older SDKs
        // bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL, true)
        bundle.putBoolean("carrier_cross_sim_ims_available_bool", true)
        bundle.putBoolean("enable_cross_sim_calling_on_opportunistic_data_bool", true)

        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true)
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true)
        bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true)
        bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true)
        bundle.putBoolean("show_wifi_calling_icon_in_status_bar_bool", true)
        bundle.putInt("wfc_spn_format_idx_int", 6)

        bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true)
        bundle.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false)
        bundle.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false)

        // vonr_enabled_bool
        bundle.putBoolean("vonr_enabled_bool", true)
        bundle.putBoolean("vonr_setting_visibility_bool", true)
        
        // KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY
        bundle.putIntArray("carrier_nr_availabilities_int_array", intArrayOf(1, 2)) // NSA, SA
        bundle.putIntArray("5g_nr_ssrsrp_thresholds_int_array", intArrayOf(-128, -118, -108, -98))
        
        return bundle
    }
}
