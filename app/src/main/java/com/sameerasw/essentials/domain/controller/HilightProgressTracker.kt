/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Controllers
 * File: HilightProgressTracker.kt
 * Description: Shows live notification progress on the Hilight LEDs each time it reaches a new LED.
 */

package com.sameerasw.essentials.domain.controller

import android.content.Context
import android.os.PowerManager
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.HilightProgressColorMode
import com.sameerasw.essentials.domain.model.HilightProgressFrames
import com.sameerasw.essentials.domain.model.ProgressNotificationData
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.PriorityModeUtil
import com.sameerasw.essentials.utils.hardware.HilightLights

object HilightProgressTracker {
    // LEDs lit at the last showing per notification key; INDETERMINATE marks a sweep
    private val shown = mutableMapOf<String, Int>()
    private val completed = mutableSetOf<String>()
    private var latestKey: String? = null

    private const val INDETERMINATE = -1

    fun isTracking(key: String): Boolean = synchronized(this) { key == latestKey }

    // Called off the main thread with the newest progress notification, or null once none are left
    fun onProgress(
        context: Context,
        data: ProgressNotificationData?,
    ) = synchronized(this) {
        val settings = SettingsRepository(context)
        if (!DeviceUtils.isHilightDevice() || !settings.isHilightProgressEnabled()) {
            reset()
            return
        }
        latestKey = data?.key
        if (data == null) {
            // The last progress notification finished without reaching 100%, e.g. its bar was replaced by "Done"
            val unfinished = shown.any { (key, lit) -> lit != INDETERMINATE && key !in completed }
            reset()
            if (unfinished) show(context, settings, HilightProgressFrames.COMPLETION_DURATION_MS, HilightProgressFrames::completion)
            return
        }
        val ledCount = HilightLights.array()?.ids?.size ?: return

        val showing =
            when {
                data.isIndeterminate -> INDETERMINATE
                data.progress >= 100f -> ledCount
                else -> HilightProgressFrames.litCount(data.progress, ledCount)
            }
        if (shown[data.key] == showing) return
        shown[data.key] = showing

        when {
            data.isIndeterminate ->
                show(context, settings, settings.getHilightProgressDurationMs(), HilightProgressFrames::indeterminate)
            data.progress >= 100f -> {
                completed += data.key
                show(context, settings, HilightProgressFrames.COMPLETION_DURATION_MS, HilightProgressFrames::completion)
            }
            else -> {
                completed -= data.key
                show(context, settings, settings.getHilightProgressDurationMs()) { _, mode, color, count ->
                    HilightProgressFrames.progress(data.progress, mode, color, count)
                }
            }
        }
    }

    private fun show(
        context: Context,
        settings: SettingsRepository,
        durationMs: Long,
        frame: (Long, HilightProgressColorMode, Int, Int) -> IntArray,
    ) {
        if (settings.isHilightOnlyWhenScreenOff() && context.getSystemService(PowerManager::class.java)?.isInteractive == true) return
        if (settings.isHilightSkipDnd() && PriorityModeUtil.isActive(context)) return
        val mode = settings.getHilightProgressColorMode()
        val color = settings.getHilightProgressColor()
        HilightController.play(durationMs = durationMs) { elapsed, ledCount -> frame(elapsed, mode, color, ledCount) }
    }

    private fun reset() {
        shown.clear()
        completed.clear()
        latestKey = null
    }
}
