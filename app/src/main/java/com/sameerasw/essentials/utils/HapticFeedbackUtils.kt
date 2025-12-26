package com.sameerasw.essentials.utils

import android.Manifest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/**
 * Provides haptic feedback based on the given type.
 */
@RequiresPermission(Manifest.permission.VIBRATE)
fun performHapticFeedback(
    vibrator: Vibrator,
    feedbackType: HapticFeedbackType
) {
    if (!vibrator.hasVibrator()) return

    when (feedbackType) {
        HapticFeedbackType.NONE -> Unit

        HapticFeedbackType.SUBTLE -> {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        }

        HapticFeedbackType.DOUBLE -> {
            val pattern = longArrayOf(0, 30, 100, 30)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }

        HapticFeedbackType.CLICK -> {
            val pattern = longArrayOf(0, 50, 60, 30)
            val amplitudes = intArrayOf(0, 200, 0, 150)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        }

        HapticFeedbackType.TICK -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }

        HapticFeedbackType.LONG -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val stepMs = 5L
                val timings = mutableListOf<Long>()
                val amplitudes = mutableListOf<Int>()
                
                // Ramp up: 0.25s (250ms)
                val upSteps = (50 / stepMs).toInt()
                for (i in 0 until upSteps) {
                    timings.add(stepMs)
                    amplitudes.add((i.toFloat() / upSteps * 255).toInt().coerceIn(0, 155))
                }
                
                // Ramp down: 0.75s (750ms)
                val downSteps = (500 / stepMs).toInt()
                for (i in 0 until downSteps) {
                    timings.add(stepMs)
                    amplitudes.add(((1f - i.toFloat() / downSteps) * 255).toInt().coerceIn(0, 155))
                }
                
                vibrator.vibrate(VibrationEffect.createWaveform(timings.toLongArray(), amplitudes.toIntArray(), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(600)
            }
        }
    }
}

@Composable
fun rememberHapticFeedback(feedbackType: HapticFeedbackType): () -> Unit {
    val view = LocalView.current
    return {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

