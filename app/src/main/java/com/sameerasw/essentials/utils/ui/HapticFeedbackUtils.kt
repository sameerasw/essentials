/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Ui
 * File: HapticFeedbackUtils.kt
 * Description: Utility helper for HapticFeedbackUtils.kt.
 */

package com.sameerasw.essentials.utils

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission
import com.sameerasw.essentials.domain.HapticFeedbackType

/**
 * Provides rich haptic feedback based on the given type using modern Android APIs.
 */
@RequiresPermission(Manifest.permission.VIBRATE)
fun performHapticFeedback(
    vibrator: Vibrator,
    feedbackType: HapticFeedbackType,
) {
    if (!vibrator.hasVibrator()) return

    when (feedbackType) {
        HapticFeedbackType.NONE -> Unit

        HapticFeedbackType.SUBTLE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f)
                    .compose()
                vibrateWithTouchAttributes(vibrator, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                vibrateWithTouchAttributes(vibrator, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(10, 80)
                vibrator.vibrate(effect)
            }
        }

        HapticFeedbackType.TICK -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f)
                    .compose()
                vibrateWithTouchAttributes(vibrator, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                vibrateWithTouchAttributes(vibrator, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(12, 160)
                vibrator.vibrate(effect)
            }
        }

        HapticFeedbackType.CLICK -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                    .compose()
                vibrateWithTouchAttributes(vibrator, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                vibrateWithTouchAttributes(vibrator, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(20, 220)
                vibrator.vibrate(effect)
            }
        }

        HapticFeedbackType.DOUBLE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 60)
                    .compose()
                vibrateWithTouchAttributes(vibrator, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                vibrateWithTouchAttributes(vibrator, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 20, 50, 25)
                val amplitudes = intArrayOf(0, 160, 0, 220)
                val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
                vibrator.vibrate(effect)
            }
        }
    }
}

private fun vibrateWithTouchAttributes(vibrator: Vibrator, effect: VibrationEffect) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val attrs = VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_TOUCH)
            .build()
        vibrator.vibrate(effect, attrs)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val attrs = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
        vibrator.vibrate(effect, attrs)
    } else {
        vibrator.vibrate(effect)
    }
}
