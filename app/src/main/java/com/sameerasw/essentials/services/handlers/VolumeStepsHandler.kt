/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: VolumeStepsHandler.kt
 * Description: Moves media volume by several steps per volume key press and while a key is held.
 */

package com.sameerasw.essentials.services.handlers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.ViewConfiguration
import com.sameerasw.essentials.data.repository.SettingsRepository

class VolumeStepsHandler(
    context: Context,
) {
    private val settingsRepository = SettingsRepository(context)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var repeatRaise = true
    private var holdStartedAt = 0L

    private val repeatRunnable =
        object : Runnable {
            override fun run() {
                val held = SystemClock.uptimeMillis() - holdStartedAt
                if (!isActive() || held > MAX_HOLD_MS || !stepMediaVolume(repeatRaise)) return
                handler.postDelayed(this, VolumeStepMath.repeatIntervalMs(settingsRepository.getVolumeStepsPerPress()))
            }
        }

    fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }
        if (!isActive()) {
            stop()
            return false
        }

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            repeatRaise = keyCode == KeyEvent.KEYCODE_VOLUME_UP
            holdStartedAt = SystemClock.uptimeMillis()
            stepMediaVolume(repeatRaise)
            handler.removeCallbacks(repeatRunnable)
            handler.postDelayed(repeatRunnable, ViewConfiguration.getKeyRepeatTimeout().toLong())
        } else if (event.action == KeyEvent.ACTION_UP) {
            stop()
        }
        return true
    }

    fun adjustIfEnabled(raise: Boolean): Boolean {
        if (!isActive()) return false
        stepMediaVolume(raise)
        return true
    }

    fun stop() {
        handler.removeCallbacks(repeatRunnable)
    }

    private fun isActive(): Boolean =
        settingsRepository.isVolumeStepsEnabled() &&
            !audioManager.isVolumeFixed &&
            audioManager.mode == AudioManager.MODE_NORMAL &&
            !isAlarmPlaying()

    private fun isAlarmPlaying(): Boolean =
        audioManager.activePlaybackConfigurations.any { it.audioAttributes.usage == AudioAttributes.USAGE_ALARM }

    private fun stepMediaVolume(raise: Boolean): Boolean {
        val steps = settingsRepository.getVolumeStepsPerPress()
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val min =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
            } else {
                0
            }
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = VolumeStepMath.target(current, min, max, steps, raise)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return target != current
    }

    private companion object {
        const val MAX_HOLD_MS = 10_000L
    }
}
