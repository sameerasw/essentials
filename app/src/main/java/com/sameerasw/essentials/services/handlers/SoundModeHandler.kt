/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: SoundModeHandler.kt
 * Description: Background service component for SoundModeHandler.kt.
 */

package com.sameerasw.essentials.services.handlers

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.sameerasw.essentials.utils.ShizukuUtils

class SoundModeHandler(
    private val context: Context,
) {
    fun cycleNextMode(): Int? {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val useShizukuPref = prefs.getBoolean("sound_mode_use_shizuku", true)
        val isShizukuReady = useShizukuPref && ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()

        if (!isShizukuReady && !notificationManager.isNotificationPolicyAccessGranted) {
            return null
        }

        val defaultOrder = listOf("Sound", "Vibrate", "Silent")
        val orderString =
            prefs.getString("sound_mode_order", defaultOrder.joinToString(","))
                ?: defaultOrder.joinToString(",")
        val order = orderString.split(",")

        val currentMode =
            when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> "Sound"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                AudioManager.RINGER_MODE_SILENT -> "Silent"
                else -> "Sound"
            }

        val currentIndex = order.indexOf(currentMode)
        val nextIndex = (currentIndex + 1) % order.size
        val nextMode = order[nextIndex]

        val nextRingerMode =
            when (nextMode) {
                "Sound" -> AudioManager.RINGER_MODE_NORMAL
                "Vibrate" -> AudioManager.RINGER_MODE_VIBRATE
                "Silent" -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }

        if (isShizukuReady) {
            val shizukuArg =
                when (nextRingerMode) {
                    AudioManager.RINGER_MODE_NORMAL -> "NORMAL"
                    AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
                    AudioManager.RINGER_MODE_SILENT -> "SILENT"
                    else -> "NORMAL"
                }
            try {
                ShizukuUtils.runCommand("cmd audio set-ringer-mode $shizukuArg")
            } catch (e: Exception) {
                // Fallback to standard AudioManager
                try {
                    audioManager.ringerMode = nextRingerMode
                } catch (ex: Exception) {
                }
            }
        } else {
            try {
                audioManager.ringerMode = nextRingerMode
            } catch (e: Exception) {
                // OEM-specific restrictions or race conditions
            }
        }

        return nextRingerMode
    }
}
