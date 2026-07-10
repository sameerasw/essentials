package com.sameerasw.essentials.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class TelephonyActionReceiver : BroadcastReceiver() {

    private val TAG = "TelephonyActionReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received action: $action")

        try {
            when (action) {
                "com.sameerasw.essentials.SET_DATA_SIM" -> {
                    val subId = intent.getIntExtra("subId", -1)
                    if (subId != -1) {
                        Log.d(TAG, "Setting default data SIM to subId=$subId")
                        setDefaultDataSubId(subId)
                        enableMobileData(context, subId)
                    } else {
                        Log.w(TAG, "Invalid subId extra in SET_DATA_SIM broadcast")
                    }
                }
                "com.sameerasw.essentials.SET_NETWORK_MODE" -> {
                    val subId = intent.getIntExtra("subId", -1)
                    val bitmask = intent.getLongExtra("bitmask", -1L)
                    if (subId != -1 && bitmask != -1L) {
                        Log.d(TAG, "Setting network mode for subId=$subId to bitmask=$bitmask")
                        setAllowedNetworkTypes(context, subId, bitmask)
                    } else {
                        Log.w(TAG, "Invalid subId or bitmask extra in SET_NETWORK_MODE broadcast")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling telephony action broadcast", e)
        }
    }

    private fun setDefaultDataSubId(subId: Int) {
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
            Log.d(TAG, "Successfully set default data subscription to $subId via standard reflection")
        } else {
            Log.e(TAG, "Shizuku isub binder not available")
        }
    }

    private fun setAllowedNetworkTypes(context: Context, subId: Int, bitmask: Long) {
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
                args = arrayOf(subId, 0, bitmask, context.packageName)
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
            }
        } else {
            Log.e(TAG, "Shizuku phone binder not available")
        }
    }

    private fun enableMobileData(context: Context, subId: Int) {
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
                    args = arrayOf(subId, 0, true, context.packageName)
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
}
