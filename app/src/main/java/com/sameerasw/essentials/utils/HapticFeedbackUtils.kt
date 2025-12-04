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
    }
}

@Composable
fun rememberHapticFeedback(feedbackType: HapticFeedbackType): () -> Unit {
    val view = LocalView.current
    return {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

