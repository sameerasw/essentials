package com.sameerasw.essentials.services.handlers

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager

class SoundModeHandler(private val context: Context) {

    fun cycleNextMode(): Int? {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return null
        }

        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val defaultOrder = listOf("Sound", "Vibrate", "Silent")
        val orderString = prefs.getString("sound_mode_order", defaultOrder.joinToString(","))
            ?: defaultOrder.joinToString(",")
        val order = orderString.split(",")

        val currentMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "Sound"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            else -> "Sound"
        }

        val currentIndex = order.indexOf(currentMode)
        val nextIndex = (currentIndex + 1) % order.size
        val nextMode = order[nextIndex]

        val nextRingerMode = when (nextMode) {
            "Sound" -> AudioManager.RINGER_MODE_NORMAL
            "Vibrate" -> AudioManager.RINGER_MODE_VIBRATE
            "Silent" -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }

        audioManager.ringerMode = nextRingerMode
        return nextRingerMode
    }
}
