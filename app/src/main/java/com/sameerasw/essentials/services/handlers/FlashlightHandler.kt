package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.receivers.FlashlightActionReceiver
import com.sameerasw.essentials.utils.FlashlightUtil
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.utils.performHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FlashlightHandler(
    private val service: AccessibilityService,
    private val scope: CoroutineScope
) {
    private val cameraManager by lazy { service.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val handler = Handler(Looper.getMainLooper())
    
    var isTorchOn = false
        private set
        
    private var currentTorchId: String? = null
    private var currentIntensityLevel: Int = 1
    private var flashlightJob: Job? = null
    private var isInternalToggle = false

    private val NOTIFICATION_ID_FLASHLIGHT = 1001
    private val CHANNEL_ID_FLASHLIGHT = "flashlight_live_update"

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            isTorchOn = enabled
            currentTorchId = cameraId
            
            val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            val isGlobalEnabled = prefs.getBoolean("flashlight_global_enabled", false)
            val lastIntensity = prefs.getInt("flashlight_last_intensity", 1)

            if (enabled) {
                currentTorchId = cameraId
                
                if (isGlobalEnabled && !isInternalToggle) {
                    // External trigger - smoothly fade in to last known intensity
                    Log.d("Flashlight", "Global control detected external ON. Fading in to $lastIntensity")
                    flashlightJob?.cancel()
                    flashlightJob = scope.launch {
                        FlashlightUtil.fadeFlashlight(
                            service,
                            cameraId,
                            fromLevel = 1,
                            toLevel = lastIntensity,
                            durationMs = 400L,
                            steps = 20
                        )
                        updateFlashlightNotification(lastIntensity)
                    }
                } else if (isInternalToggle) {
                    // Internal trigger - we already handled the job
                    isInternalToggle = false
                } else {
                    // Normal mode or no global - sync level
                    currentIntensityLevel = FlashlightUtil.getDefaultLevel(service, cameraId)
                    updateFlashlightNotification(currentIntensityLevel)
                }
            } else {
                // Flashlight turned OFF
                flashlightJob?.cancel() // Stop any ongoing fade-in or fade-out
                isInternalToggle = false // Reset
                cancelFlashlightNotification()
            }
        }
    }

    fun register() {
        torchCallback.let { cameraManager.registerTorchCallback(it, handler) }
    }

    fun unregister() {
        torchCallback.let { cameraManager.unregisterTorchCallback(it) }
        currentTorchId = null
    }

    fun handleIntent(intent: Intent) {
        when (intent.action) {
            FlashlightActionReceiver.ACTION_INCREASE -> adjustFlashlightIntensity(true)
            FlashlightActionReceiver.ACTION_DECREASE -> adjustFlashlightIntensity(false)
            FlashlightActionReceiver.ACTION_OFF -> if (isTorchOn) toggleFlashlight()
            FlashlightActionReceiver.ACTION_TOGGLE -> toggleFlashlight()
            FlashlightActionReceiver.ACTION_SET_INTENSITY -> {
                val level = intent.getIntExtra(FlashlightActionReceiver.EXTRA_INTENSITY, 1)
                currentIntensityLevel = level
                if (!isTorchOn) {
                    toggleFlashlight(overrideIntensity = level)
                } else {
                    currentTorchId?.let { id ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                            FlashlightUtil.isIntensitySupported(service, id)) {
                            try {
                                cameraManager.turnOnTorchWithStrengthLevel(id, level)
                                updateFlashlightNotification(level)
                            } catch (e: Exception) {
                                Log.e("Flashlight", "Error setting intensity level", e)
                            }
                        }
                    }
                }
            }
            FlashlightActionReceiver.ACTION_PULSE_NOTIFICATION -> pulseFlashlightForNotificationWithCheck()
        }
    }

    private fun updateFlashlightNotification(intensity: Int) {
        val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("flashlight_live_update_enabled", true)) {
            cancelFlashlightNotification()
            return
        }
        
        val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_FLASHLIGHT,
                "Flashlight Controls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Controls for active flashlight"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val cameraId = currentTorchId ?: return
        val maxLevel = FlashlightUtil.getMaxLevel(service, cameraId)
        val percentage = (intensity * 100) / maxOf(1, maxLevel)
        
        val decreaseIntent = PendingIntent.getBroadcast(service, 1, 
            Intent(service, FlashlightActionReceiver::class.java).apply { action = FlashlightActionReceiver.ACTION_DECREASE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            
        val increaseIntent = PendingIntent.getBroadcast(service, 2, 
            Intent(service, FlashlightActionReceiver::class.java).apply { action = FlashlightActionReceiver.ACTION_INCREASE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            
        val offIntent = PendingIntent.getBroadcast(service, 3, 
            Intent(service, FlashlightActionReceiver::class.java).apply { action = FlashlightActionReceiver.ACTION_OFF },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= 35) {
            try {
                val builder = Notification.Builder(service, CHANNEL_ID_FLASHLIGHT)
                    .setSmallIcon(R.drawable.rounded_flashlight_on_24)
                    .setContentTitle("Flashlight active")
                    .setContentText("Brightness: $percentage%")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setColorized(false)
                    .setShowWhen(false)
                    .addAction(Notification.Action.Builder(
                        Icon.createWithResource(service, R.drawable.rounded_keyboard_arrow_down_24),
                        "-", decreaseIntent).build())
                    .addAction(Notification.Action.Builder(
                        Icon.createWithResource(service, R.drawable.rounded_power_settings_new_24),
                        "Off", offIntent).build())
                    .addAction(Notification.Action.Builder(
                        Icon.createWithResource(service, R.drawable.rounded_keyboard_arrow_up_24),
                        "+", increaseIntent).build())

                if (Build.VERSION.SDK_INT >= 36) {
                    try {
                        val progressStyle = Notification.ProgressStyle()
                            .setStyledByProgress(true)
                            .setProgress(intensity)
                            .setProgressTrackerIcon(Icon.createWithResource(service, R.drawable.rounded_flashlight_on_24))
                        
                        progressStyle.addProgressSegment(
                            Notification.ProgressStyle.Segment(maxLevel)
                                .setColor(Color.YELLOW)
                        )
                        builder.setStyle(progressStyle)
                    } catch (e: Throwable) {
                        Log.e("FlashlightNotification", "ProgressStyle error", e)
                    }
                }

                try {
                    builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                        .invoke(builder, true)
                    builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java)
                        .invoke(builder, "$percentage%")
                } catch (_: Throwable) {}
                
                val extras = android.os.Bundle()
                extras.putBoolean("android.requestPromotedOngoing", true)
                extras.putString("android.shortCriticalText", "$percentage%")
                builder.addExtras(extras)

                notificationManager.notify(NOTIFICATION_ID_FLASHLIGHT, builder.build())
                return
            } catch (e: Exception) {
                Log.e("FlashlightNotification", "Native builder failed, falling back to compat", e)
            }
        }

        val builder = NotificationCompat.Builder(service, CHANNEL_ID_FLASHLIGHT)
            .setSmallIcon(R.drawable.rounded_flashlight_on_24)
            .setContentTitle("Flashlight active")
            .setContentText("Brightness: $percentage%")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(R.drawable.rounded_keyboard_arrow_down_24, "-", decreaseIntent)
            .addAction(R.drawable.rounded_power_settings_new_24, "Off", offIntent)
            .addAction(R.drawable.rounded_keyboard_arrow_up_24, "+", increaseIntent)
            .addExtras(android.os.Bundle().apply {
                putBoolean("android.requestPromotedOngoing", true)
                putString("android.shortCriticalText", "$percentage%")
            })

        notificationManager.notify(NOTIFICATION_ID_FLASHLIGHT, builder.build())
    }

    private fun getCameraId(): String? {
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash) return id
            }
        } catch (e: Exception) {
            Log.e("Flashlight", "Error getting camera ID", e)
        }
        return null
    }

    fun pulseFlashlightForNotification() {
        if (isTorchOn) return
        
        val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val pulseEnabled = prefs.getBoolean("flashlight_pulse_enabled", false)
        if (!pulseEnabled) return
        
        // Face down check usually happens outside, but if we need it here we need proximity state.
        // For separation of concerns, Proximity check should happen in Service or passed in.
        // But current implementation checks isProximityBlocked inside pulseFlashlightForNotification.
        // Refactoring opportunity: make this method blocking status agnostic or pass it in.
        // I will assume for now we can skip the facedown check OR I should add a way to set proximity status.
        // Let's pass 'isProximityBlocked' to this method or main service sets it. 
        // For cleaner API, I'll add a property `isProximityBlocked` to the handler or pass it to this method.
        // Let's add a public var isProximityBlocked to the handler.
    }
    
    var isProximityBlocked = false

    fun pulseFlashlightForNotificationWithCheck() {
         if (isTorchOn) return
        
        val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val pulseEnabled = prefs.getBoolean("flashlight_pulse_enabled", false)
        if (!pulseEnabled) return
        
        val faceDownOnly = prefs.getBoolean("flashlight_pulse_facedown_only", true)
        if (faceDownOnly && !isProximityBlocked) return

        val cameraId = currentTorchId ?: getCameraId() ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
            FlashlightUtil.isIntensitySupported(service, cameraId)) {
                
                flashlightJob?.cancel()
                flashlightJob = scope.launch {
                    val maxLevel = FlashlightUtil.getMaxLevel(service, cameraId)
                    val targetPulseLevel = (maxLevel * 0.2f).toInt().coerceAtLeast(1)
                    
                    isInternalToggle = true 
                    
                    FlashlightUtil.fadeFlashlight(
                        service,
                        cameraId,
                        fromLevel = 0,
                        toLevel = targetPulseLevel,
                        durationMs = 600L,
                        steps = 40
                    )
                    
                    kotlinx.coroutines.delay(800L) 
                    
                    FlashlightUtil.fadeFlashlight(
                        service,
                        cameraId,
                        fromLevel = targetPulseLevel,
                        toLevel = 0,
                        durationMs = 600L,
                        steps = 40
                    )
                    
                    isInternalToggle = false
                }
            }
    }

    private fun cancelFlashlightNotification() {
        val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_FLASHLIGHT)
    }

    fun adjustFlashlightIntensity(increase: Boolean) {
        val cameraId = currentTorchId ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        try {
            val maxLevel = FlashlightUtil.getMaxLevel(service, cameraId)
            val currentSystemLevel = FlashlightUtil.getCurrentLevel(service, cameraId)
            
            val step = maxOf(1, maxLevel / 5)
            val isAtLimit = if (increase) currentSystemLevel >= maxLevel else currentSystemLevel <= 1
            
            if (isAtLimit) {
                triggerHapticFeedback(HapticFeedbackType.DOUBLE)
                return
            }

            val targetLevel = if (increase) {
                (currentSystemLevel + step).coerceAtMost(maxLevel)
            } else {
                (currentSystemLevel - step).coerceAtLeast(1)
            }
            
            currentIntensityLevel = targetLevel
            updateFlashlightNotification(targetLevel)
            
            val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("flashlight_global_enabled", false)) {
                prefs.edit().putInt("flashlight_last_intensity", targetLevel).apply()
            }
            
            flashlightJob?.cancel()
            flashlightJob = scope.launch {
                FlashlightUtil.fadeFlashlight(
                    service,
                    cameraId,
                    fromLevel = currentSystemLevel,
                    toLevel = targetLevel,
                    durationMs = 150L,
                    steps = 5
                )
            }

            if (targetLevel == maxLevel || targetLevel == 1) {
                triggerHapticFeedback(HapticFeedbackType.DOUBLE)
            } else {
                triggerHapticFeedback(HapticFeedbackType.SUBTLE)
            }
        } catch (e: Exception) {
            Log.e("Flashlight", "Error adjusting intensity", e)
        }
    }

    fun toggleFlashlight(overrideIntensity: Int? = null) {
        val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val isFadeEnabled = prefs.getBoolean("flashlight_fade_enabled", false)

        try {
            var targetCameraId: String? = null
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
                if (flashAvailable && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    targetCameraId = id
                    break
                }
            }
            if (targetCameraId == null) {
                for (id in cameraManager.cameraIdList) {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                        targetCameraId = id
                        break
                    }
                }
            }

            if (targetCameraId != null) {
                val finalCameraId = targetCameraId
                currentTorchId = finalCameraId
                val maxLevel = FlashlightUtil.getMaxLevel(service, finalCameraId)
                val defaultLevel = FlashlightUtil.getDefaultLevel(service, finalCameraId)
                
                if (isFadeEnabled && FlashlightUtil.isIntensitySupported(service, finalCameraId)) {
                    val targetState = !isTorchOn
                    if (targetState) {
                        currentIntensityLevel = overrideIntensity ?: if (prefs.getBoolean("flashlight_global_enabled", false)) {
                            prefs.getInt("flashlight_last_intensity", defaultLevel)
                        } else {
                            defaultLevel
                        }
                    }
                    isInternalToggle = true
                    flashlightJob?.cancel()
                    flashlightJob = scope.launch {
                        FlashlightUtil.fadeFlashlight(
                            service,
                            finalCameraId,
                            targetState,
                            maxLevel = currentIntensityLevel
                        )
                    }
                    if (targetState) {
                        updateFlashlightNotification(currentIntensityLevel)
                    } else {
                        cancelFlashlightNotification()
                    }
                } else {
                    isInternalToggle = true
                    flashlightJob?.cancel()
                    cameraManager.setTorchMode(finalCameraId, !isTorchOn)
                    currentIntensityLevel = overrideIntensity ?: if (prefs.getBoolean("flashlight_global_enabled", false)) {
                        prefs.getInt("flashlight_last_intensity", defaultLevel)
                    } else {
                        defaultLevel
                    }
                    if (!isTorchOn) {
                        updateFlashlightNotification(currentIntensityLevel)
                    } else {
                        cancelFlashlightNotification()
                    }
                }
                triggerHapticFeedback()
            }
        } catch (e: Exception) {
            Log.e("Flashlight", "Error toggling flashlight", e)
        }
    }

    private fun triggerHapticFeedback(specificType: HapticFeedbackType? = null) {
         try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                service.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                service.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null) {
                performHapticFeedback(vibrator, specificType ?: HapticFeedbackType.DOUBLE)
            }
        } catch (_: Exception) {}
    }
}
