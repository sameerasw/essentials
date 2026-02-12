package com.sameerasw.essentials.utils

import android.content.Context
import android.os.Vibrator
import android.view.View
import androidx.compose.runtime.mutableStateOf
import com.sameerasw.essentials.domain.HapticFeedbackType

/**
 * Centralized haptic feedback utility that can be toggled on/off app-wide.
 * Controls in-app UI haptics (not widget haptics).
 */
object HapticUtil {
    // Mutable state to track if in-app haptics are enabled
    val isAppHapticsEnabled = mutableStateOf(true)

    /**
     * Perform a UI interaction haptic feedback (light tick)
     * Only performs if app haptics are enabled
     */
    fun performUIHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Perform a light tick haptic feedback
     */
    fun performLightHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Perform a medium impact haptic feedback
     */
    fun performMediumHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
    }

    /**
     * Perform a heavy/virtual key haptic feedback
     */
    fun performHeavyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun performSliderHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
    }

    /**
     * Perform a virtual key haptic feedback (stronger)
     * Only performs if app haptics are enabled
     */
    fun performVirtualKeyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Perform haptic feedback for background services (Context-based)
     */
    fun performHapticForService(context: android.content.Context, type: HapticFeedbackType = HapticFeedbackType.SUBTLE) {
        if (!isAppHapticsEnabled.value) return
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        performHapticFeedback(vibrator, type)
    }

    fun performCustomHaptic(view: View, strength: Float) {
        if (!isAppHapticsEnabled.value) return
        
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = view.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            view.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        // Use Primitives (API 30+) for the most consistent, crisp feedback with scaling
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
             try {
                 if (vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                     val effect = android.os.VibrationEffect.startComposition()
                         .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength)
                         .compose()
                     
                     val attrs = android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_TOUCH)
                     vibrator.vibrate(effect, attrs)
                     return
                 }
             } catch (e: Exception) {
                 // Fallback if primitive check fails
             }
        }

        // Fallback for API 26-29 or devices without primitive support
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val hasAmplitudeControl = vibrator.hasAmplitudeControl()
            
            if (hasAmplitudeControl) {
                // Use quadratic scaling for better low-end control
                val amplitude = (strength * strength * 255).toInt().coerceIn(1, 255)
                // 12ms is a sweet spot for one-shot clicks
                val effect = android.os.VibrationEffect.createOneShot(12, amplitude)
                
                // Use standard vibrate, attributes handled if possible
                vibrator.vibrate(effect)
            } else {
                // No amplitude control: differentiate by strength
                if (strength < 0.5f) {
                     view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                } else {
                     view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        } else {
            // Pre-Oreo
            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Load app haptic preference from SharedPreferences
     */
    fun loadAppHapticsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("app_haptics_enabled", true) // Default: enabled
    }

    /**
     * Save app haptic preference to SharedPreferences
     */
    fun saveAppHapticsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("app_haptics_enabled", enabled).apply()
        isAppHapticsEnabled.value = enabled
    }

    /**
     * Initialize haptic setting from SharedPreferences
     */
    fun initialize(context: Context) {
        isAppHapticsEnabled.value = loadAppHapticsEnabled(context)
    }
}

