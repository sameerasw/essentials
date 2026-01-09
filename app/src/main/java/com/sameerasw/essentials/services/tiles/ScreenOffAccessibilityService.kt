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
import android.view.accessibility.AccessibilityEvent
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
        
        flashlightHandler.register()
        
        // Screen Receiver
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        notificationLightingHandler.onScreenOn()
                        freezeHandler.removeCallbacks(freezeRunnable)
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        appFlowHandler.clearAuthenticated()
                        scheduleFreeze()
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
        return buttonRemapHandler.onKeyEvent(event) || super.onKeyEvent(event)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return super.onStartCommand(intent, flags, startId)
        
        when (action) {
            "LOCK_SCREEN" -> securityHandler.lockDevice()
            
            "SHOW_NOTIFICATION_LIGHTING" -> notificationLightingHandler.handleIntent(intent)
            
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
}