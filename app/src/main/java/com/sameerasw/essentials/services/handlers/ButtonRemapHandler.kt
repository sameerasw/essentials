package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.os.VibratorManager
import android.os.Vibrator
import android.app.NotificationManager
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.utils.performHapticFeedback
import com.sameerasw.essentials.utils.ShizukuUtils
import com.sameerasw.essentials.services.InputEventListenerService

class ButtonRemapHandler(
    private val service: AccessibilityService,
    private val flashlightHandler: FlashlightHandler
) {
    private val soundModeHandler = SoundModeHandler(service)
    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressTriggered: Boolean = false
    private var lastPressedKeyCode: Int = -1
    private var lastPendingAction: String? = null
    private val longPressTimeout = 500L

    private val longPressRunnable = Runnable {
        isLongPressTriggered = true
        lastPendingAction?.let { handleLongPress(it) }
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }

        val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val isButtonRemapEnabled = prefs.getBoolean("button_remap_enabled", false)
        val isButtonRemapUseShizuku = prefs.getBoolean("button_remap_use_shizuku", false)
        val isAdjustEnabled = prefs.getBoolean("flashlight_adjust_intensity_enabled", false)
        val isGlobalEnabled = prefs.getBoolean("flashlight_global_enabled", false)

        val powerManager = service.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenInteractive = try { powerManager.isInteractive } catch(e: Exception) { false }
        
        // This usually requires import of the isAod method or similar logic. 
        // Typically AOD check is: Display.STATE_DOZE or STATE_DOZE_SUSPEND
        val isAod = isAodShowing()

        val shellReady = com.sameerasw.essentials.utils.ShellUtils.isAvailable(service) && com.sameerasw.essentials.utils.ShellUtils.hasPermission(service)
        val devicePathDetected = !prefs.getString("shizuku_detected_device_path", null).isNullOrEmpty()

        // If using Shizuku and screen off, verify if we should INTERCEPT or let Shizuku handle it? 
        // Original logic: returns true (consumes event) if isMapped or isTorchControl.
        // Wait, if returns true here, the accessibility service consumes it, so it DOES NOT reach the app?
        // Actually, accessibility service `onKeyEvent` returning true means "I handled this, don't pass it to the system/app".
        // The original logic checks: "if ... return true". 
        // It seems this block is to prevent system volume change if we are going to handle it via Shizuku service separately?
        // OR, the original logic meant: "If handled by Shizuku InputEventListenerService, we also consume it here so system volume doesn't change?"
        // But InputEventListenerService is a separate service. 
        // The check `if (isButtonRemapUseShizuku ...)` suggests we might skip handling here because Shizuku service handles it?
        // NO, the return true means "CONSUME". So if Shizuku is handling it, we consume it here to prevent default volume action?
        // Let's stick to the original logic flow.
        
        val useShell = isButtonRemapUseShizuku || com.sameerasw.essentials.utils.ShellUtils.isRootEnabled(service)

        if (useShell && isButtonRemapEnabled && shellReady && devicePathDetected && !isScreenInteractive && !isAod) {
             val isTorchControl = flashlightHandler.isTorchOn && (isAdjustEnabled || isGlobalEnabled) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
             
             val suffix = "_off"
             val actionKey = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) "button_remap_vol_up_action$suffix" else "button_remap_vol_down_action$suffix"
             val action = prefs.getString(actionKey, "None")
             val isMapped = action != null && action != "None"
             
             if (isMapped || isTorchControl) {
                 return true
             }
        }

        // Flashlight Brightness Control (Volume Keys + Torch On)
        if (flashlightHandler.isTorchOn && (isAdjustEnabled || isGlobalEnabled) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) {
                    isLongPressTriggered = false
                    lastPressedKeyCode = keyCode
                    lastPendingAction = "Toggle flashlight" 
                    handler.postDelayed(longPressRunnable, longPressTimeout)
                }
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                handler.removeCallbacks(longPressRunnable)
                if (!isLongPressTriggered) {
                    flashlightHandler.adjustFlashlightIntensity(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                }
                return true
            }
        }

        if (!isButtonRemapEnabled) return false

        val isScreenOn = isScreenInteractive

        val actionKeySuffix = if (isScreenOn) "_on" else "_off"
        val actionKey = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            "button_remap_vol_up_action$actionKeySuffix"
        } else {
            "button_remap_vol_down_action$actionKeySuffix"
        }
        
        val action = prefs.getString(actionKey, "None") ?: "None"
        val isAlwaysTurnOffEnabled = prefs.getBoolean("flashlight_always_turn_off_enabled", false)
        
        val isVolUpFlashlight = prefs.getString("button_remap_vol_up_action_off", "None") == "Toggle flashlight" ||
                                prefs.getString("button_remap_vol_up_action_on", "None") == "Toggle flashlight"
        val isVolDownFlashlight = prefs.getString("button_remap_vol_down_action_off", "None") == "Toggle flashlight" ||
                                  prefs.getString("button_remap_vol_down_action_on", "None") == "Toggle flashlight"
        
        val isFlashlightCapableButton = (keyCode == KeyEvent.KEYCODE_VOLUME_UP && isVolUpFlashlight) ||
                                       (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && isVolDownFlashlight)

        var finalAction = action
        if (flashlightHandler.isTorchOn && isAlwaysTurnOffEnabled && isFlashlightCapableButton) {
            finalAction = "Toggle flashlight"
        }

        if (finalAction == "None") return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount == 0) {
                lastPressedKeyCode = keyCode
                lastPendingAction = finalAction
                isLongPressTriggered = false
                handler.postDelayed(longPressRunnable, longPressTimeout)
            }
            return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            handler.removeCallbacks(longPressRunnable)
            if (!isLongPressTriggered) {
                // Short press - re-simulate volume behavior
                val am = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            }
            return true
        }
        
        return false
    }
    
    fun handleExternalVolumeLongPress(intent: Intent) {
        if (intent.action == InputEventListenerService.ACTION_VOLUME_LONG_PRESSED) {
            val direction = intent.getStringExtra(InputEventListenerService.EXTRA_DIRECTION)
            if (direction != null) {
                 val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                 val isScreenOn = try {
                     (service.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
                 } catch(e: Exception) { false }
                 
                 val actionKeySuffix = if (isScreenOn) "_on" else "_off"
                 val actionKey = if (direction == "UP") {
                     "button_remap_vol_up_action$actionKeySuffix"
                 } else {
                     "button_remap_vol_down_action$actionKeySuffix"
                 }
                 val action = prefs.getString(actionKey, "None") ?: "None"
                 handleLongPress(action)
            }
        }
    }

    private fun handleLongPress(action: String) {
        when (action) {
            "Toggle flashlight" -> flashlightHandler.toggleFlashlight()
            "Media play/pause" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            "Media next" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            "Media previous" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "Toggle vibrate" -> toggleRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            "Toggle mute" -> toggleRingerMode(AudioManager.RINGER_MODE_SILENT)
            "AI assistant" -> launchAssistant()
            "Take screenshot" -> takeScreenshot()
            "Cycle sound modes" -> cycleSoundModes()
            "Toggle media volume" -> toggleMediaVolume()
            "Like current song" -> service.sendBroadcast(Intent("com.sameerasw.essentials.ACTION_LIKE_CURRENT_SONG").setPackage(service.packageName))
        }
    }

    private fun cycleSoundModes() {
        soundModeHandler.cycleNextMode()
        triggerHapticFeedback()
    }

    private fun takeScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            triggerHapticFeedback()
        } else {
            Log.w("ButtonRemap", "Take screenshot is only supported on Android 9+")
        }
    }

    private fun sendMediaKey(keyCode: Int) {
        val am = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        triggerHapticFeedback()
    }

    private fun toggleMediaVolume() {
        val am = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)

        if (currentVolume > 0) {
            // Mute and save current volume
            prefs.edit().putInt("last_media_volume", currentVolume).apply()
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
        } else {
            // Restore last known volume or default to mid-range
            val lastVolume = prefs.getInt("last_media_volume", am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, lastVolume, AudioManager.FLAG_SHOW_UI)
        }
        triggerHapticFeedback()
    }

    private fun toggleRingerMode(targetMode: Int) {
        val am = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentMode = am.ringerMode
        if (currentMode == targetMode) {
            am.ringerMode = AudioManager.RINGER_MODE_NORMAL
        } else {
            am.ringerMode = targetMode
        }
        triggerHapticFeedback()
    }

    private fun launchAssistant() {
        try {
            val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            service.startActivity(intent)
            triggerHapticFeedback()
        } catch (e: Exception) {
            Log.e("ButtonRemap", "Failed to launch assistant", e)
        }
    }
    
    private fun triggerHapticFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                service.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                service.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null) {
                // Use default from Button Remap preference
                val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                val typeName = prefs.getString("button_remap_haptic_type", HapticFeedbackType.DOUBLE.name)
                val type = try { HapticFeedbackType.valueOf(typeName ?: HapticFeedbackType.DOUBLE.name) } catch(_:Exception) { HapticFeedbackType.DOUBLE }
                performHapticFeedback(vibrator, if (type.name == "LONG") HapticFeedbackType.DOUBLE else type)
            }
        } catch (_: Exception) {}
    }

    private fun isAodShowing(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            val display = (service.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
            display.state == android.view.Display.STATE_DOZE || display.state == android.view.Display.STATE_DOZE_SUSPEND
        } else {
            false
        }
    }
}
