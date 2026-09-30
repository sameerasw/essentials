/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Utilities
 * File: HapticUtil.kt
 * Description: Provides haptic feedback helpers using HapticFeedbackType preferences.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType as ComposeHapticFeedbackType
import com.sameerasw.essentials.domain.HapticFeedbackType

/**
 * Centralized haptic feedback utility that can be toggled on/off app-wide.
 * Controls in-app UI haptics and service/gesture haptics.
 */
enum class AppHapticMode { DISABLED, ENABLED, STRONGER }

fun VibrationEffect.Composition.composeBoosted(vibrator: Vibrator): VibrationEffect {
    if (HapticUtil.hapticMode.value == AppHapticMode.STRONGER &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)
    ) {
        addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.6f, 0)
    }
    return compose()
}

object HapticUtil {
    val hapticMode = mutableStateOf(AppHapticMode.ENABLED)

    fun boost(scale: Float): Float =
        if (hapticMode.value == AppHapticMode.STRONGER) (scale * 1.6f + 0.2f).coerceAtMost(1f) else scale

    // Mutable state to track if in-app haptics are enabled
    val isAppHapticsEnabled = mutableStateOf(true)

    fun gate(context: Context, delegate: HapticFeedback): HapticFeedback =
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: ComposeHapticFeedbackType) {
                if (!isAppHapticsEnabled.value) return
                if (hapticMode.value == AppHapticMode.STRONGER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    performCustomHaptic(context, composeStrength(hapticFeedbackType))
                } else {
                    delegate.performHapticFeedback(hapticFeedbackType)
                }
            }
        }

    private fun playStronger(view: View, strength: Float): Boolean {
        if (hapticMode.value != AppHapticMode.STRONGER || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        performCustomHaptic(view.context, strength)
        return true
    }

    private fun composeStrength(type: ComposeHapticFeedbackType): Float =
        when (type) {
            ComposeHapticFeedbackType.LongPress -> 0.7f
            ComposeHapticFeedbackType.TextHandleMove -> 0.3f
            ComposeHapticFeedbackType.SegmentFrequentTick -> 0.25f
            ComposeHapticFeedbackType.SegmentTick -> 0.35f
            ComposeHapticFeedbackType.GestureThresholdActivate -> 0.6f
            ComposeHapticFeedbackType.GestureEnd -> 0.5f
            ComposeHapticFeedbackType.Confirm -> 0.7f
            ComposeHapticFeedbackType.Reject -> 0.85f
            ComposeHapticFeedbackType.ToggleOn, ComposeHapticFeedbackType.ToggleOff -> 0.5f
            else -> 0.5f
        }

    fun vibrate(context: Context, effect: VibrationEffect) {
        if (!isAppHapticsEnabled.value) return
        runCatching { getVibrator(context).vibrate(effect) }
    }

    /**
     * Perform standard UI interaction haptic feedback (keyboard tap / standard click)
     */
    fun performUIHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.5f)) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Perform light tick haptic feedback (clock tick / segment tick)
     */
    fun performLightHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.3f)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * Perform an extra light micro tick haptic feedback
     */
    fun performMicroHaptic(view: View) {
        performCustomHaptic(view, 0.05f)
    }

    /**
     * Perform medium impact haptic feedback (context click)
     */
    fun performMediumHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.6f)) return
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    /**
     * Perform heavy / virtual key haptic feedback
     */
    fun performHeavyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.85f)) return
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Perform slider haptic feedback
     */
    fun performSliderHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.25f)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * Perform virtual key haptic feedback
     */
    fun performVirtualKeyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.85f)) return
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Perform confirmation haptic feedback
     */
    fun performConfirmHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.7f)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    /**
     * Perform rejection haptic feedback
     */
    fun performRejectHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.85f)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Perform gesture threshold crossing haptic feedback
     */
    fun performGestureThresholdHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.6f)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    /**
     * Helper to obtain vibrator service cleanly across all Android versions
     */
    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Perform haptic feedback for background services (Context-based)
     */
    fun performHapticForService(
        context: Context,
        type: HapticFeedbackType = HapticFeedbackType.SUBTLE,
    ) {
        if (!isAppHapticsEnabled.value) return
        val vibrator = getVibrator(context)
        performHapticFeedback(vibrator, type)
    }

    fun performRumbleHaptic(context: Context) {
        if (!isAppHapticsEnabled.value) return
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
            )
        ) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, boost(0.4f))
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, boost(0.9f), 35)
                .composeBoosted(vibrator)
            vibrateWithTouchAttributes(vibrator, effect)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            vibrateWithTouchAttributes(vibrator, effect)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 18, 30, 24)
            val amplitudes = intArrayOf(0, 90, 0, 180)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        }
    }

    fun performThunderRumble(
        context: Context,
        intensity: Float,
    ) {
        if (!isAppHapticsEnabled.value) return
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return
        val scale = intensity.coerceIn(0.1f, 1f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
        ) {
            try {
                val strike = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)
                ) {
                    VibrationEffect.Composition.PRIMITIVE_THUD
                } else {
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                }
                val composition = VibrationEffect.startComposition().addPrimitive(strike, boost(0.55f * scale))
                THUNDER_TAIL.forEachIndexed { index, level ->
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, boost(level * scale), if (index == 0) 20 else 45)
                }
                vibrateWithTouchAttributes(vibrator, composition.composeBoosted(vibrator))
                return
            } catch (_: Exception) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 60, 40, 90, 60, 120)
            val amplitudes = intArrayOf(0, (140 * scale).toInt(), (40 * scale).toInt(), (90 * scale).toInt(), (35 * scale).toInt(), (20 * scale).toInt())
            vibrateWithTouchAttributes(vibrator, VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }

    private val THUNDER_TAIL = floatArrayOf(0.3f, 0.22f, 0.16f, 0.12f, 0.4f, 0.2f, 0.12f, 0.07f, 0.04f)

    fun performStrongDoubleHaptic(context: Context) {
        if (!isAppHapticsEnabled.value) return
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
        ) {
            val composition = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 80)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)
            ) {
                composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 50)
            } else {
                composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 80)
            }
            val effect = composition.composeBoosted(vibrator)
            vibrateWithTouchAttributes(vibrator, effect)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrateWithTouchAttributes(vibrator, VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 35, 70, 45, 50, 40), intArrayOf(0, 255, 0, 255, 0, 255), -1))
        }
    }

    fun performStrongTickHaptic(context: Context) {
        if (!isAppHapticsEnabled.value) return
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
        ) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                .composeBoosted(vibrator)
            vibrateWithTouchAttributes(vibrator, effect)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            vibrateWithTouchAttributes(vibrator, effect)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(20, 255)
            vibrator.vibrate(effect)
        }
    }

    fun performCustomHaptic(
        context: Context,
        strength: Float,
    ) {
        if (!isAppHapticsEnabled.value) return
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        val clampedStrength = strength.coerceIn(0.01f, 1.0f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val primitiveStrength = boost(clampedStrength)
                val primitive = if (clampedStrength < 0.35f &&
                    vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
                ) {
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                } else if (clampedStrength < 0.75f &&
                    vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
                ) {
                    VibrationEffect.Composition.PRIMITIVE_TICK
                } else if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    VibrationEffect.Composition.PRIMITIVE_CLICK
                } else {
                    null
                }

                if (primitive != null) {
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(primitive, primitiveStrength)
                        .composeBoosted(vibrator)
                    vibrateWithTouchAttributes(vibrator, effect)
                    return
                }
            } catch (_: Exception) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val stronger = hapticMode.value == AppHapticMode.STRONGER
            val effect = if (stronger) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            } else if (clampedStrength < 0.5f) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            } else {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            }
            vibrateWithTouchAttributes(vibrator, effect)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (vibrator.hasAmplitudeControl()) {
                val amplitude = (boost(clampedStrength) * boost(clampedStrength) * 255).toInt().coerceIn(1, 255)
                val effect = VibrationEffect.createOneShot(12, amplitude)
                vibrator.vibrate(effect)
            } else {
                val effect = VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            }
        }
    }

    fun performCustomHaptic(
        view: View,
        strength: Float,
    ) {
        if (!isAppHapticsEnabled.value) return
        performCustomHaptic(view.context, strength)
    }

    private fun vibrateWithTouchAttributes(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_TOUCH)
                .build()
            vibrator.vibrate(effect, attrs)
        } else {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()
            vibrator.vibrate(effect, attrs)
        }
    }

    fun startRampingHoldHaptic(
        context: Context,
        durationMs: Long = 2000L,
    ) {
        if (!isAppHapticsEnabled.value) return
        val vibrator =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val stepDuration = 20L
            val segments = (durationMs / stepDuration).toInt().coerceAtLeast(10)
            val timings = LongArray(segments) { stepDuration }
            val minAmp = 1f
            val maxAmp = 80f
            val amplitudes =
                IntArray(segments) { i ->
                    val progress = (i + 1).toFloat() / segments
                    val curve = Math.pow(progress.toDouble(), 1.6).toFloat()
                    (minAmp + ((maxAmp - minAmp) * curve)).toInt().coerceIn(1, 100)
                }
            runCatching {
                val effect = android.os.VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            }
        }
    }

    fun startTickRampHaptic(
        context: Context,
        durationMs: Long,
    ) {
        if (!isAppHapticsEnabled.value) return
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && vibrator.areEnvelopeEffectsSupported()) {
            runCatching {
                val effect = VibrationEffect.BasicEnvelopeBuilder()
                    .setInitialSharpness(0.1f)
                    .addControlPoint(boost(0.08f), 0.2f, (durationMs * 0.35f).toLong().coerceAtLeast(1L))
                    .addControlPoint(boost(0.35f), 0.5f, (durationMs * 0.4f).toLong().coerceAtLeast(1L))
                    .addControlPoint(boost(0.75f), 0.85f, (durationMs * 0.25f).toLong().coerceAtLeast(1L))
                    .addControlPoint(0f, 0.85f, 20L)
                    .build()
                vibrator.vibrate(effect)
            }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE)
        ) {
            runCatching {
                vibrator.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, boost(0.8f))
                        .composeBoosted(vibrator),
                )
            }
            return
        }
        startRampingHoldHaptic(context, durationMs)
    }

    fun performOpenClickHaptic(context: Context) {
        if (!isAppHapticsEnabled.value) return
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
        ) {
            runCatching {
                vibrator.cancel()
                vibrator.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f)
                        .composeBoosted(vibrator),
                )
            }
        } else {
            performStrongTickHaptic(context)
        }
    }

    fun stopHoldHaptic(context: Context) {
        val vibrator =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        runCatching { vibrator.cancel() }
    }

    fun loadHapticMode(context: Context): AppHapticMode {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val stored = prefs.getString("app_haptics_mode", null)
        if (stored != null) return runCatching { AppHapticMode.valueOf(stored) }.getOrDefault(AppHapticMode.ENABLED)
        return if (prefs.getBoolean("app_haptics_enabled", true)) AppHapticMode.ENABLED else AppHapticMode.DISABLED
    }

    fun saveHapticMode(
        context: Context,
        mode: AppHapticMode,
    ) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_haptics_mode", mode.name).apply()
        applyMode(mode)
    }

    fun initialize(context: Context) {
        applyMode(loadHapticMode(context))
    }

    private fun applyMode(mode: AppHapticMode) {
        hapticMode.value = mode
        isAppHapticsEnabled.value = mode != AppHapticMode.DISABLED
    }
}
