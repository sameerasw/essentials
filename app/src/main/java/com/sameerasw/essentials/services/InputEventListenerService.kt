package com.sameerasw.essentials.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.input.InputDeviceScanner
import com.sameerasw.essentials.input.VolumeLongPressDetector
import com.sameerasw.essentials.input.VolumePressEvent
import com.sameerasw.essentials.shizuku.ShizukuPermissionHelper
import com.sameerasw.essentials.shizuku.ShizukuStatus
import kotlinx.coroutines.*

class InputEventListenerService : Service() {
    companion object {
        const val ACTION_VOLUME_LONG_PRESSED = "com.sameerasw.essentials.VOLUME_LONG_PRESSED"
        const val EXTRA_DIRECTION = "direction"
        const val EXTRA_DURATION_MS = "duration_ms"
        private const val NOTIFICATION_ID = 4242
    }

    private var scope: CoroutineScope? = null
    private var detector: VolumeLongPressDetector? = null
    private lateinit var shizukuHelper: ShizukuPermissionHelper
    private var isTorchOn = false

    override fun onCreate() {
        super.onCreate()
        shizukuHelper = ShizukuPermissionHelper(this)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "service_channel",
                "Background Service",
                android.app.NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "service_channel")
            .setContentTitle("Volume Listener Running")
            .setContentText("Listening for volume button long presses")
            .setSmallIcon(android.R.drawable.star_on)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        
        val cameraManager = getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        cameraManager.registerTorchCallback(object : android.hardware.camera2.CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                super.onTorchModeChanged(cameraId, enabled)
                isTorchOn = enabled
            }
        }, null)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        scope?.launch {
            delay(500)
            startListening()
        }
    }

    private fun startListening() {
        scope?.launch {
            if (shizukuHelper.getStatus() != ShizukuStatus.READY) {
                Log.e("InputEventListener", "Shizuku not ready")
                return@launch
            }

            val devices = withContext(Dispatchers.IO) {
                // find device
                InputDeviceScanner().scanForVolumeDevices()
            }

            if (devices.isEmpty()) {
                Log.e("InputEventListener", "No devices found")
                // Clear prefs if no device found
                getSharedPreferences("essentials_prefs", android.content.Context.MODE_PRIVATE)
                    .edit().remove("shizuku_detected_device_path").apply()
                return@launch
            }

            // picked first item
            // IMPROVEMENT: Iterate and try to find the one that actually emits keys
            val devicePath = devices.first().path
            Log.d("InputEventListener", "Listening on device: $devicePath")

            // Save detected device to prefs for UI
            getSharedPreferences("essentials_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("shizuku_detected_device_path", devicePath).apply()
            
            detector = VolumeLongPressDetector(devicePath, 500)

            launch {
                detector?.events?.collect { event ->
                    if (event is VolumePressEvent.LongPress) {
                        Log.i("InputEventListener", "⭐ LONG PRESS: ${event.direction}")
                        sendBroadcast(Intent(ACTION_VOLUME_LONG_PRESSED).apply {
                            putExtra(EXTRA_DIRECTION, event.direction.name)
                            putExtra(EXTRA_DURATION_MS, event.durationMs)
                            setPackage(packageName)
                        })
                    } else if (event is VolumePressEvent.ShortPress) {
                        if (isTorchOn) {
                            val prefs = getSharedPreferences("essentials_prefs", android.content.Context.MODE_PRIVATE)
                            val isAdjustEnabled = prefs.getBoolean("flashlight_adjust_intensity_enabled", false)
                            val isGlobalEnabled = prefs.getBoolean("flashlight_global_enabled", false)
                            
                            // Only if adjustment is enabled
                            if (isAdjustEnabled || isGlobalEnabled) {
                                val action = if (event.direction == com.sameerasw.essentials.input.VolumeDirection.UP) 
                                    "com.sameerasw.essentials.ACTION_FLASHLIGHT_INCREASE" 
                                else 
                                    "com.sameerasw.essentials.ACTION_FLASHLIGHT_DECREASE"
                                sendBroadcast(Intent(action).setPackage(packageName))
                            } else {
                                val pm = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                                val isScreenOn = pm.isInteractive
                                val suffix = if (isScreenOn) "_on" else "_off"
                                val key = if (event.direction == com.sameerasw.essentials.input.VolumeDirection.UP) "button_remap_vol_up_action$suffix" else "button_remap_vol_down_action$suffix"
                                val actionStr = prefs.getString(key, "None")
                                if (actionStr != "None") {
                                    val am = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                    val direction = if (event.direction == com.sameerasw.essentials.input.VolumeDirection.UP) 
                                        android.media.AudioManager.ADJUST_RAISE 
                                    else 
                                        android.media.AudioManager.ADJUST_LOWER
                                    am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, android.media.AudioManager.FLAG_SHOW_UI)
                                }
                            }
                        } else {
                            val prefs = getSharedPreferences("essentials_prefs", android.content.Context.MODE_PRIVATE)
                            val pm = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                            val isScreenOn = pm.isInteractive
                            val suffix = if (isScreenOn) "_on" else "_off"
                            val key = if (event.direction == com.sameerasw.essentials.input.VolumeDirection.UP) "button_remap_vol_up_action$suffix" else "button_remap_vol_down_action$suffix"
                            val actionStr = prefs.getString(key, "None")
                            
                            if (actionStr != "None") {
                                val am = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                val dirKey = if (event.direction == com.sameerasw.essentials.input.VolumeDirection.UP) 
                                    android.media.AudioManager.ADJUST_RAISE 
                                else 
                                    android.media.AudioManager.ADJUST_LOWER
                                am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, dirKey, android.media.AudioManager.FLAG_SHOW_UI)
                            }
                        }
                    }
                }
            }

            detector?.startListening(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Reinforce foreground
        val notification = NotificationCompat.Builder(this, "service_channel")
            .setContentTitle("Volume Listener Running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        detector?.stopListening()
        scope?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
