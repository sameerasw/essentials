/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Telephony Utilities
 * File: SimCarrierUtil.kt
 * Description: Utilities to query SIM cards, read current carrier configuration, and override carrier names via Shizuku.
 */

package com.sameerasw.essentials.utils

import android.annotation.SuppressLint
import android.app.IActivityManager
import android.app.IInstrumentationWatcher
import android.app.UiAutomationConnection
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import com.sameerasw.essentials.shizuku.CarrierConfigModifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

data class SimCarrierInfo(
    val subId: Int,
    val simSlotIndex: Int,
    val displayName: String,
    val currentCarrierName: String,
    val defaultCarrierName: String,
    val isOverridden: Boolean,
)

object SimCarrierUtil {
    private const val TAG = "SimCarrierUtil"

    @SuppressLint("MissingPermission")
    fun getSimCarrierInfoList(context: Context): List<SimCarrierInfo> {
        val subscriptionManager =
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                ?: return emptyList()
        val carrierConfigManager =
            context.getSystemService(Context.CARRIER_CONFIG_SERVICE) as? CarrierConfigManager
                ?: return emptyList()

        val activeSubscriptions: List<SubscriptionInfo> =
            try {
                subscriptionManager.activeSubscriptionInfoList ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get active subscription info list", e)
                emptyList()
            }

        return activeSubscriptions.map { subInfo ->
            val subId = subInfo.subscriptionId
            val defaultCarrierName = subInfo.carrierName?.toString().orEmpty()
            val bundle =
                try {
                    carrierConfigManager.getConfigForSubId(subId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get config for subId $subId", e)
                    null
                }

            val isOverridden =
                bundle?.getBoolean(
                    CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL,
                    false,
                ) ?: false

            val currentCarrierName =
                if (isOverridden) {
                    bundle?.getString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, defaultCarrierName)
                        ?: defaultCarrierName
                } else {
                    defaultCarrierName
                }

            SimCarrierInfo(
                subId = subId,
                simSlotIndex = subInfo.simSlotIndex,
                displayName = subInfo.displayName?.toString().orEmpty(),
                currentCarrierName = currentCarrierName,
                defaultCarrierName = defaultCarrierName,
                isOverridden = isOverridden,
            )
        }
    }

    suspend fun overrideCarrierName(
        context: Context,
        subId: Int,
        carrierName: String?,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val bundle = CarrierConfigModifier.buildOverrideBundle(subId, carrierName)
            val result = startInstrumentation(context, CarrierConfigModifier::class.java, bundle)
            result?.getBoolean(CarrierConfigModifier.BUNDLE_RESULT, false) ?: false
        }

    suspend fun resetCarrierName(
        context: Context,
        subId: Int,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val bundle = CarrierConfigModifier.buildResetBundle(subId)
            val result = startInstrumentation(context, CarrierConfigModifier::class.java, bundle)
            result?.getBoolean(CarrierConfigModifier.BUNDLE_RESULT, false) ?: false
        }

    private suspend fun startInstrumentation(
        context: Context,
        cls: Class<*>,
        args: Bundle,
    ): Bundle? {
        val deferredResult = CompletableDeferred<Bundle?>()

        return try {
            val watcher =
                object : IInstrumentationWatcher.Stub() {
                    override fun instrumentationStatus(
                        name: ComponentName?,
                        resultCode: Int,
                        results: Bundle?,
                    ) {
                    }

                    override fun instrumentationFinished(
                        name: ComponentName?,
                        resultCode: Int,
                        results: Bundle?,
                    ) {
                        deferredResult.complete(results)
                    }
                }

            val binder =
                SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)
                    ?: return null
            val am = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(binder))
            val name = ComponentName(context, cls)
            val flags = 8 // ActivityManager.INSTR_FLAG_NO_RESTART
            val connection = UiAutomationConnection()

            am.startInstrumentation(name, null, flags, args, watcher, connection, 0, null)

            withTimeoutOrNull(5000L) {
                deferredResult.await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start instrumentation for ${cls.name}", e)
            null
        }
    }

    fun saveCustomCarrierNames(context: Context, names: Map<Int, String>) {
        val prefs = context.getSharedPreferences("sim_names_prefs", Context.MODE_PRIVATE)
        val json = com.google.gson.Gson().toJson(names)
        prefs.edit().putString("saved_carrier_names", json).apply()
    }

    fun getSavedCustomCarrierNames(context: Context): Map<Int, String> {
        val prefs = context.getSharedPreferences("sim_names_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("saved_carrier_names", null) ?: return emptyMap()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<Int, String>>() {}.type
            com.google.gson.Gson().fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun applySavedCarrierNames(context: Context) {
        val saved = getSavedCustomCarrierNames(context)
        saved.forEach { (subId, name) ->
            if (name.isNotBlank()) {
                overrideCarrierName(context, subId, name)
            }
        }
    }
}
