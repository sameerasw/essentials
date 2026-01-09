package com.sameerasw.essentials.utils

import android.Manifest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission
import com.sameerasw.essentials.domain.HapticFeedbackType

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
            // A quick double tap that feels like a physical switch
            val pattern = longArrayOf(0, 40, 60, 40)
            val amplitudes = intArrayOf(0, 180, 0, 220)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
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

    }
}

