package com.sameerasw.essentials.services

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.hardware.camera2.CameraManager
import com.sameerasw.essentials.utils.FlashlightUtil
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object DeviceInfoSyncManager {
    private const val TAG = "DeviceInfoSyncManager"
    private const val SYNC_PATH = "/device_info"
    private val handler = Handler(Looper.getMainLooper())
    private var isInitialized = false
    
    private var isTorchOn = false
    private var torchLevel = 1
    private var maxTorchLevel = 1
    private var isIntensitySupported = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            val context = currentContext ?: return
            val primaryId = FlashlightUtil.getCameraId(context)
            if (cameraId == primaryId) {
                isTorchOn = enabled
                
                var level = FlashlightUtil.getCurrentLevel(context, cameraId)
                // Fallback to last known intensity if system returns default level 1
                if (enabled && level <= 1) {
                    val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                    level = prefs.getInt("flashlight_last_intensity", level)
                }
                
                torchLevel = level
                maxTorchLevel = FlashlightUtil.getMaxLevel(context, cameraId)
                isIntensitySupported = FlashlightUtil.isIntensitySupported(context, cameraId)
                syncDeviceInfo(context)
            }
        }

        override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
            val context = currentContext ?: return
            val primaryId = FlashlightUtil.getCameraId(context)
            if (cameraId == primaryId) {
                torchLevel = newStrengthLevel
                syncDeviceInfo(context)
            }
        }
    }

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

        // Sync on battery change
        context.registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                syncDeviceInfo(context)
            }
        }, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Sync on flashlight change
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.registerTorchCallback(torchCallback, handler)
        
        // Get initial flashlight state
        val id = FlashlightUtil.getCameraId(context)
        if (id != null) {
            isIntensitySupported = FlashlightUtil.isIntensitySupported(context, id)
            maxTorchLevel = FlashlightUtil.getMaxLevel(context, id)
            torchLevel = FlashlightUtil.getCurrentLevel(context, id)
        }
    }

    private val syncDebouncer = Runnable {
        currentContext?.let { performSync(it) }
    }

    private fun syncDeviceInfo(context: Context) {
        handler.removeCallbacks(syncDebouncer)
        handler.postDelayed(syncDebouncer, 250L)
    }

    fun forceSync(context: Context) {
        handler.removeCallbacks(syncDebouncer)
        performSync(context)
    }

    private fun performSync(context: Context) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level / scale.toFloat() * 100).toInt() else -1
        
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS

        val putDataMapReq = PutDataMapRequest.create(SYNC_PATH)
        val dataMap = putDataMapReq.dataMap
        dataMap.putInt("battery_level", batteryPct)
        dataMap.putBoolean("is_charging", isCharging)
        dataMap.putBoolean("flashlight_on", isTorchOn)
        dataMap.putInt("flashlight_level", torchLevel)
        dataMap.putInt("flashlight_max_level", maxTorchLevel)
        dataMap.putBoolean("flashlight_intensity_supported", isIntensitySupported)
        dataMap.putLong("timestamp", System.currentTimeMillis())

        val putDataReq = putDataMapReq.asPutDataRequest()
        putDataReq.setUrgent()

        Wearable.getDataClient(context).putDataItem(putDataReq)
    }
}
