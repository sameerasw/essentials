/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: ButtonRemapHandler.kt
 * Description: Background service component for ButtonRemapHandler.kt.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Display
import android.view.KeyEvent
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.services.InputEventListenerService
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.utils.performHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ButtonRemapHandler(
    private val service: AccessibilityService,
    private val flashlightHandler: FlashlightHandler,
) {
    private val settingsRepository = SettingsRepository(service)
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isLongPressTriggered: Boolean = false
    private var lastPressedKeyCode: Int = -1
    private var lastPendingAction: Action? = null
    private val longPressTimeout = 500L

    private val longPressRunnable =
        Runnable {
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
        val isScreenInteractive =
            try {
                powerManager.isInteractive
            } catch (e: Exception) {
                false
            }

        val isAod = isAodShowing()

        val shellReady =
            com.sameerasw.essentials.utils.ShellUtils
                .isAvailable(service) &&
                com.sameerasw.essentials.utils.ShellUtils.hasPermission(
                    service,
                )
        val devicePathDetected =
            !prefs.getString("shizuku_detected_device_path", null).isNullOrEmpty()

        val useShell =
            isButtonRemapUseShizuku ||
                com.sameerasw.essentials.utils.ShellUtils.isRootEnabled(
                    service,
                )

        if (useShell && isButtonRemapEnabled && shellReady && devicePathDetected && !isScreenInteractive && !isAod) {
            val isTorchControl =
                flashlightHandler.isTorchOn && (isAdjustEnabled || isGlobalEnabled) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

            val suffix = "_off"
            val actionKey =
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) "button_remap_vol_up_action$suffix" else "button_remap_vol_down_action$suffix"
            val action = settingsRepository.getRemapAction(actionKey)
            val isMapped = action != null

            if (isMapped || isTorchControl) {
                return true
            }
        }

        // Flashlight Brightness Control (Volume Keys + Torch On)
        if (flashlightHandler.isTorchOn && (isAdjustEnabled || isGlobalEnabled) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isAlwaysTurnOffEnabled =
                prefs.getBoolean("flashlight_always_turn_off_enabled", false)
            val isVolUpFlashlight =
                settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF) is Action.ToggleFlashlight ||
                    settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_ON) is Action.ToggleFlashlight
            val isVolDownFlashlight =
                settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF) is Action.ToggleFlashlight ||
                    settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON) is Action.ToggleFlashlight
            val isFlashlightCapableButton =
                (keyCode == KeyEvent.KEYCODE_VOLUME_UP && isVolUpFlashlight) ||
                    (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && isVolDownFlashlight)

            val actionKey =
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    if (isScreenInteractive) SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_ON else SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF
                } else {
                    if (isScreenInteractive) SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON else SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF
                }
            val mappedAction = settingsRepository.getRemapAction(actionKey)

            val targetLongPressAction: Action =
                if (isAlwaysTurnOffEnabled && isFlashlightCapableButton) {
                    Action.ToggleFlashlight
                } else {
                    mappedAction ?: Action.ToggleFlashlight
                }

            if (event.action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) {
                    isLongPressTriggered = false
                    lastPressedKeyCode = keyCode
                    lastPendingAction = targetLongPressAction
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

        val actionKey =
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (isScreenOn) SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_ON else SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF
            } else {
                if (isScreenOn) SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON else SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF
            }

        val action = settingsRepository.getRemapAction(actionKey)
        val isAlwaysTurnOffEnabled = prefs.getBoolean("flashlight_always_turn_off_enabled", false)

        val isVolUpFlashlight =
            settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF) is Action.ToggleFlashlight ||
                settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_ON) is Action.ToggleFlashlight
        val isVolDownFlashlight =
            settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF) is Action.ToggleFlashlight ||
                settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON) is Action.ToggleFlashlight

        val isFlashlightCapableButton =
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP && isVolUpFlashlight) ||
                (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && isVolDownFlashlight)

        var finalAction = action
        if (flashlightHandler.isTorchOn && isAlwaysTurnOffEnabled && isFlashlightCapableButton) {
            finalAction = Action.ToggleFlashlight
        }

        if (finalAction == null) return false

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
                val direction =
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                am.adjustSuggestedStreamVolume(
                    direction,
                    AudioManager.USE_DEFAULT_STREAM_TYPE,
                    AudioManager.FLAG_SHOW_UI,
                )
            }
            return true
        }

        return true
    }

    fun handleExternalVolumeLongPress(intent: Intent) {
        if (intent.action == InputEventListenerService.ACTION_VOLUME_LONG_PRESSED) {
            val direction = intent.getStringExtra(InputEventListenerService.EXTRA_DIRECTION)
            if (direction != null) {
                val isScreenOn =
                    try {
                        (service.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
                    } catch (e: Exception) {
                        false
                    }

                val actionKey =
                    if (direction == "UP") {
                        if (isScreenOn) SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_ON else SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF
                    } else {
                        if (isScreenOn) SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON else SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF
                    }
                val action = settingsRepository.getRemapAction(actionKey)
                if (action != null) {
                    handleLongPress(action)
                }
            }
        }
    }

    private fun handleLongPress(action: Action) {
        if (action is Action.ToggleFlashlight) {
            flashlightHandler.toggleFlashlight()
        } else {
            scope.launch {
                CombinedActionExecutor.execute(service, action)
            }
            triggerHapticFeedback()
        }
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    service.getSystemService(VibratorManager::class.java)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    service.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

            if (vibrator != null) {
                // Use default from Button Remap preference
                val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                val typeName =
                    prefs.getString("button_remap_haptic_type", HapticFeedbackType.DOUBLE.name)
                val type =
                    try {
                        HapticFeedbackType.valueOf(typeName ?: HapticFeedbackType.DOUBLE.name)
                    } catch (_: Exception) {
                        HapticFeedbackType.DOUBLE
                    }
                performHapticFeedback(
                    vibrator,
                    if (type.name == "LONG") HapticFeedbackType.DOUBLE else type,
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun isAodShowing(): Boolean =
        try {
            val displayManager = service.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
            display?.state == Display.STATE_DOZE || display?.state == Display.STATE_DOZE_SUSPEND
        } catch (_: Exception) {
            false
        }
}
