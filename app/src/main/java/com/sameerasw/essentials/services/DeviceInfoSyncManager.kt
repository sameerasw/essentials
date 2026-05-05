package com.sameerasw.essentials.services

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object DeviceInfoSyncManager {
    private const val TAG = "DeviceInfoSyncManager"
    private const val SYNC_PATH = "/device_info"
    private val handler = Handler(Looper.getMainLooper())
    private var isInitialized = false

    private val syncRunnable = object : Runnable {
        override fun run() {
            syncDeviceInfo(currentContext ?: return)
            handler.postDelayed(this, 5 * 60 * 1000) // Sync every 5 minutes
        }
    }

    private var currentContext: Context? = null

    fun init(context: Context) {
        if (isInitialized) return
        currentContext = context.applicationContext
        isInitialized = true
        
        // Initial sync
        syncDeviceInfo(context)
        
        // Start periodic sync
        handler.postDelayed(syncRunnable, 5 * 60 * 1000)
    }

    fun forceSync(context: Context) {
        Log.d(TAG, "forceSync: Manually triggering sync")
        syncDeviceInfo(context)
    }

    private fun syncDeviceInfo(context: Context) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level / scale.toFloat() * 100).toInt() else -1
        
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        Log.d(TAG, "Syncing device info: Battery=$batteryPct%, Charging=$isCharging")

        val putDataMapReq = PutDataMapRequest.create(SYNC_PATH)
        val dataMap = putDataMapReq.dataMap
        dataMap.putInt("battery_level", batteryPct)
        dataMap.putBoolean("is_charging", isCharging)
        dataMap.putLong("timestamp", System.currentTimeMillis())

        val putDataReq = putDataMapReq.asPutDataRequest()
        putDataReq.setUrgent()

        Wearable.getDataClient(context).putDataItem(putDataReq)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully synced device info to wearable")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync device info to wearable", e)
            }
    }
}
