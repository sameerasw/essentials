package com.sameerasw.essentials.services.tiles

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.os.Vibrator
import android.view.accessibility.AccessibilityEvent
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.utils.performHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.sameerasw.essentials.services.handlers.*
import com.sameerasw.essentials.services.receivers.FlashlightActionReceiver
import com.sameerasw.essentials.utils.FreezeManager
import com.sameerasw.essentials.services.InputEventListenerService

class ScreenOffAccessibilityService : AccessibilityService(), SensorEventListener {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Handlers
    private lateinit var flashlightHandler: FlashlightHandler
    private lateinit var notificationLightingHandler: NotificationLightingHandler
    private lateinit var buttonRemapHandler: ButtonRemapHandler
    private lateinit var appFlowHandler: AppFlowHandler
    private lateinit var securityHandler: SecurityHandler
    private lateinit var ambientGlanceHandler: AmbientGlanceHandler

    private var screenReceiver: BroadcastReceiver? = null
    
    // Proximity
    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private var proximitySensor: Sensor? = null
    
    // Freeze Logic
    private val freezeHandler = Handler(Looper.getMainLooper())
    private val freezeRunnable = Runnable {
        FreezeManager.freezeAll(this)
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Handlers
        flashlightHandler = FlashlightHandler(this, serviceScope)
        notificationLightingHandler = NotificationLightingHandler(this)
        buttonRemapHandler = ButtonRemapHandler(this, flashlightHandler)
        appFlowHandler = AppFlowHandler(this)
        securityHandler = SecurityHandler(this)
        ambientGlanceHandler = AmbientGlanceHandler(this)
        
        flashlightHandler.register()
        
        // Screen Receiver
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        notificationLightingHandler.onScreenOn()
                        ambientGlanceHandler.dismissImmediately()
                        freezeHandler.removeCallbacks(freezeRunnable)
                        stopInputEventListener()
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        appFlowHandler.clearAuthenticated()
                        scheduleFreeze()
                        startInputEventListenerIfEnabled()
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        securityHandler.restoreAnimationScale()
                    }
                    InputEventListenerService.ACTION_VOLUME_LONG_PRESSED -> {
                        buttonRemapHandler.handleExternalVolumeLongPress(intent)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(InputEventListenerService.ACTION_VOLUME_LONG_PRESSED)
        }
        registerReceiver(screenReceiver, filter, RECEIVER_EXPORTED)

        // Proximity
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    
    private fun scheduleFreeze() {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isFreezeWhenLockedEnabled = prefs.getBoolean("freeze_when_locked_enabled", false)
        
        if (isFreezeWhenLockedEnabled) {
            val delayIndex = prefs.getInt("freeze_lock_delay_index", 1)
            val delayMs = when (delayIndex) {
                0 -> 0L // Immediately
                1 -> 60_000L // 1 minute
                2 -> 300_000L // 5 minutes
                3 -> 900_000L // 15 minutes
                else -> -1L // Never
            }
            
            if (delayMs >= 0) {
                freezeHandler.removeCallbacks(freezeRunnable)
                freezeHandler.postDelayed(freezeRunnable, delayMs)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        flashlightHandler.unregister()
        sensorManager.unregisterListener(this)
        securityHandler.restoreAnimationScale()
        notificationLightingHandler.removeOverlay()
        ambientGlanceHandler.removeOverlay()
        stopInputEventListener()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        securityHandler.onAccessibilityEvent(event)
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            appFlowHandler.onPackageChanged(packageName)
        }
    }

    override fun onInterrupt() { }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = event.sensor.maximumRange
            val isBlocked = distance < maxRange && distance < 5f
            
            flashlightHandler.isProximityBlocked = isBlocked
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isInteractive && event.action == KeyEvent.ACTION_DOWN) {
                triggerAmbientGlanceVolume(keyCode)
            }
        }
        return buttonRemapHandler.onKeyEvent(event) || super.onKeyEvent(event)
    }

    private fun triggerAmbientGlanceVolume(keyCode: Int) {
        val prefs = getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ENABLED, false)) {
            val title = prefs.getString("current_media_title", null)
            val artist = prefs.getString("current_media_artist", null)
            
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val percentage = (currentVolume.toFloat() / maxVolume.toFloat() * 100).toInt()

            val intent = Intent("SHOW_AMBIENT_GLANCE").apply {
                putExtra("event_type", "volume")
                putExtra("track_title", title)
                putExtra("artist_name", artist)
                putExtra("volume_percentage", percentage)
                putExtra("volume_key_code", keyCode)
            }
            ambientGlanceHandler.handleIntent(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return super.onStartCommand(intent, flags, startId)
        
        when (action) {
            "LOCK_SCREEN" -> {
                val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                val hapticTypeStr = prefs.getString("haptic_feedback_type", HapticFeedbackType.NONE.name)
                val hapticType = try {
                    HapticFeedbackType.valueOf(hapticTypeStr ?: HapticFeedbackType.NONE.name)
                } catch (e: Exception) {
                    HapticFeedbackType.NONE
                }

                if (hapticType != HapticFeedbackType.NONE) {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.let { performHapticFeedback(it, hapticType) }
                }
                securityHandler.lockDevice()
            }
            
            "SHOW_NOTIFICATION_LIGHTING" -> notificationLightingHandler.handleIntent(intent)
            "SHOW_AMBIENT_GLANCE" -> ambientGlanceHandler.handleIntent(intent)
            
            "APP_AUTHENTICATED" -> intent.getStringExtra("package_name")?.let { appFlowHandler.onAuthenticated(it) }
            "APP_AUTHENTICATION_FAILED" -> performGlobalAction(GLOBAL_ACTION_HOME)
            
            FlashlightActionReceiver.ACTION_INCREASE,
            FlashlightActionReceiver.ACTION_DECREASE,
            FlashlightActionReceiver.ACTION_OFF,
            FlashlightActionReceiver.ACTION_TOGGLE,
            FlashlightActionReceiver.ACTION_SET_INTENSITY,
            FlashlightActionReceiver.ACTION_PULSE_NOTIFICATION -> flashlightHandler.handleIntent(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startInputEventListenerIfEnabled() {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("button_remap_enabled", false)
        val useShizuku = prefs.getBoolean("button_remap_use_shizuku", false)
        
        if (isEnabled && useShizuku) {
            try {
                val intent = Intent(this, InputEventListenerService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun stopInputEventListener() {
        try {
            stopService(Intent(this, InputEventListenerService::class.java))
        } catch (e: Exception) {
            // Ignore
        }
    }
}