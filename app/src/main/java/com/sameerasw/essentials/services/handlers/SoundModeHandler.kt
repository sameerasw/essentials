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
    private val audioManager get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs get() = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)

    fun isShizukuReady(): Boolean =
        prefs.getBoolean("sound_mode_use_shizuku", true) && ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()

    fun canChangeMode(): Boolean {
        if (isShizukuReady()) return true
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun cycleNextMode(): Int? {
        if (!canChangeMode()) return null

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

        return setMode(nextRingerMode)
    }

    fun setMode(ringerMode: Int): Int? {
        if (!canChangeMode()) return null
        if (isShizukuReady()) {
            val shizukuArg =
                when (ringerMode) {
                    AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
                    AudioManager.RINGER_MODE_SILENT -> "SILENT"
                    else -> "NORMAL"
                }
            try {
                ShizukuUtils.runCommand("cmd audio set-ringer-mode $shizukuArg")
                return ringerMode
            } catch (_: Exception) {
            }
        }
        try {
            audioManager.ringerMode = ringerMode
        } catch (_: Exception) {
        }
        return ringerMode
    }
}
