package com.sameerasw.essentials.services

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.sameerasw.essentials.utils.FlashlightUtil

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
                    val prefs =
                        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
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
    private var prefChangeListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var aodContentObserver: android.database.ContentObserver? = null
    private var tapToWakeContentObserver: android.database.ContentObserver? = null

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

        // Sync on ringer mode change
        context.registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                syncDeviceInfo(context)
            }
        }, IntentFilter(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION))

        // Sync on flashlight change
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.registerTorchCallback(torchCallback, handler)

        // Sync on AOD change
        val aodUri = android.provider.Settings.Secure.getUriFor("doze_always_on")
        aodContentObserver = object : android.database.ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                syncDeviceInfo(context)
            }
        }
        context.contentResolver.registerContentObserver(aodUri, true, aodContentObserver!!)

        // Sync on Tap to wake change
        val tapToWakeUri = android.provider.Settings.Secure.getUriFor("doze_tap_gesture")
        tapToWakeContentObserver = object : android.database.ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                syncDeviceInfo(context)
            }
        }
        context.contentResolver.registerContentObserver(tapToWakeUri, true, tapToWakeContentObserver!!)

        // Sync on preference change (flashlight pulse, glance, watch controls)
        val p = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "flashlight_pulse_enabled" || key == "notification_glance_enabled" || key == "watch_controls_layout" || key == "watch_sync_sound_mode_enabled") {
                syncDeviceInfo(context)
            }
        }
        p.registerOnSharedPreferenceChangeListener(prefChangeListener)
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
        val batteryPct =
            if (level != -1 && scale != -1) (level / scale.toFloat() * 100).toInt() else -1

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS

        val putDataMapReq = PutDataMapRequest.create(SYNC_PATH)
        val ringerMode =
            (context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager)?.ringerMode
                ?: 2
        val deviceName = android.provider.Settings.Global.getString(
            context.contentResolver,
            android.provider.Settings.Global.DEVICE_NAME
        )
            ?: android.os.Build.MODEL

        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val travelActive = prefs.getBoolean("travel_active", false)
        val travelName = prefs.getString("travel_name", "") ?: ""
        val travelProgress = prefs.getFloat("travel_progress", 0f)
        val travelRemainingTime = prefs.getString("travel_remaining_time", "") ?: ""
        val travelRemainingDistance = prefs.getString("travel_remaining_distance", "") ?: ""
        val travelIconName = prefs.getString("travel_icon_name", "") ?: ""
        val travelIsPaused = prefs.getBoolean("travel_is_paused", false)
        val travelArrived = prefs.getBoolean("travel_arrived", false)

        val flashlightPulseEnabled = prefs.getBoolean("flashlight_pulse_enabled", false)
        val aodState = when {
            prefs.getBoolean("notification_glance_enabled", false) -> 2
            android.provider.Settings.Secure.getInt(context.contentResolver, "doze_always_on", 0) == 1 -> 1
            else -> 0
        }

        val watchControlsLayout = prefs.getString("watch_controls_layout", "LOCK,SOUND,FLASHLIGHT,FLASHLIGHT_PULSE,AOD") ?: "LOCK,SOUND,FLASHLIGHT,FLASHLIGHT_PULSE,AOD"

        val tapToWakeEnabled = android.provider.Settings.Secure.getInt(context.contentResolver, "doze_tap_gesture", 1) == 1
        val watchSyncSoundModeEnabled = prefs.getBoolean("watch_sync_sound_mode_enabled", false)

        val dataMap = putDataMapReq.dataMap
        dataMap.putInt("battery_level", batteryPct)
        dataMap.putBoolean("is_charging", isCharging)
        dataMap.putBoolean("flashlight_on", isTorchOn)
        dataMap.putInt("flashlight_level", torchLevel)
        dataMap.putInt("flashlight_max_level", maxTorchLevel)
        dataMap.putBoolean("flashlight_intensity_supported", isIntensitySupported)
        dataMap.putInt("ringer_mode", ringerMode)
        dataMap.putString("device_name", deviceName)
        dataMap.putBoolean("flashlight_pulse_enabled", flashlightPulseEnabled)
        dataMap.putInt("aod_state", aodState)
        dataMap.putBoolean("tap_to_wake_enabled", tapToWakeEnabled)
        dataMap.putString("watch_controls_layout", watchControlsLayout)
        dataMap.putBoolean("watch_sync_sound_mode_enabled", watchSyncSoundModeEnabled)
        
        dataMap.putBoolean("travel_active", travelActive)
        dataMap.putString("travel_name", travelName)
        dataMap.putFloat("travel_progress", travelProgress)
        dataMap.putString("travel_remaining_time", travelRemainingTime)
        dataMap.putString("travel_remaining_distance", travelRemainingDistance)
        dataMap.putString("travel_icon_name", travelIconName)
        dataMap.putBoolean("travel_is_paused", travelIsPaused)
        dataMap.putBoolean("travel_arrived", travelArrived)

        dataMap.putLong("timestamp", System.currentTimeMillis())

        val putDataReq = putDataMapReq.asPutDataRequest()
        putDataReq.setUrgent()

        Wearable.getDataClient(context).putDataItem(putDataReq)
    }
}
