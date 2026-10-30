/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Controllers
 * File: HilightController.kt
 * Description: Plays short effects on the Pixel Hilight LED array and hands it back to the system afterwards.
 */

package com.sameerasw.essentials.domain.controller

import android.content.Context
import android.os.BatteryManager
import android.os.IBinder
import android.os.SystemClock
import com.sameerasw.essentials.EssentialsApp
import com.sameerasw.essentials.domain.model.HilightEffect
import com.sameerasw.essentials.domain.model.HilightFrames
import com.sameerasw.essentials.domain.model.HilightPattern
import com.sameerasw.essentials.utils.AppColorUtil
import com.sameerasw.essentials.utils.hardware.HilightLights
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

object HilightController {
    // Alpha-only black before true black; some units were reported to latch an LED on otherwise
    private const val RELEASE_BLACK = 0x01000000
    private const val MIN_FRAME_MS = 33L
    private const val MAX_FRAME_MS = 250L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun isAvailable(): Boolean = HilightLights.array() != null

    fun play(
        effect: HilightEffect,
        onStarted: (() -> Unit)? = null,
    ): Boolean {
        val ledCount = HilightLights.array()?.ids?.size ?: HilightEffect.DEFAULT_LED_COUNT
        val batteryLevel =
            if (effect.pattern == HilightPattern.BATTERY) {
                EssentialsApp.context
                    .getSystemService(BatteryManager::class.java)
                    ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            } else {
                100
            }
        return play(
            durationMs = HilightFrames.durationMs(effect, ledCount),
            onStarted = onStarted,
        ) { elapsedMs, count -> HilightFrames.frame(effect, elapsedMs, count, batteryLevel) }
    }

    // Plays in the app's colour when autoColor is on; the lookup is cached after the first call
    fun playForApp(
        context: Context,
        packageName: String,
        effect: HilightEffect,
        onStarted: (() -> Unit)? = null,
    ): Boolean {
        if (!effect.autoColor) return play(effect, onStarted)
        if (!HilightLights.isAccessGranted()) return false
        AppColorUtil.resolveColor(context, packageName) { color ->
            play(effect.copy(color = HilightEffect.appColorOr(color, effect.color)), onStarted)
        }
        return true
    }

    // Shows frame(elapsedMs, ledCount) until durationMs, replacing whatever is playing
    @Synchronized
    fun play(
        durationMs: Long,
        onStarted: (() -> Unit)? = null,
        frame: (Long, Int) -> IntArray,
    ): Boolean {
        if (!HilightLights.isAccessGranted()) return false
        val previous = job
        job =
            scope.launch {
                previous?.let {
                    it.cancel()
                    it.join()
                }
                render(durationMs.coerceAtMost(HilightEffect.MAX_DURATION_MS), frame, onStarted)
            }
        return true
    }

    @Synchronized
    fun stop() {
        job?.cancel()
    }

    private suspend fun render(
        durationMs: Long,
        frame: (Long, Int) -> IntArray,
        onStarted: (() -> Unit)?,
    ) {
        val leds = HilightLights.array() ?: return
        val frameMs = leds.minUpdatePeriodMs.coerceIn(MIN_FRAME_MS, MAX_FRAME_MS)
        // The session is held only while lit, since an open session hides Pixel's own Hilight effects
        val token = HilightLights.openSession() ?: return
        onStarted?.invoke()
        try {
            val start = SystemClock.elapsedRealtime()
            var last: IntArray? = null
            while (true) {
                coroutineContext.ensureActive()
                val elapsed = SystemClock.elapsedRealtime() - start
                if (elapsed >= durationMs) break
                val colors = frame(elapsed, leds.ids.size)
                if (!colors.contentEquals(last)) {
                    if (!HilightLights.setColors(token, leds.ids, colors)) break
                    last = colors
                }
                delay(frameMs)
            }
        } finally {
            withContext(NonCancellable) { release(token, leds, frameMs) }
        }
    }

    private suspend fun release(
        token: IBinder,
        leds: HilightLights.LedArray,
        frameMs: Long,
    ) {
        HilightLights.setColors(token, leds.ids, intArrayOf(RELEASE_BLACK))
        delay(frameMs)
        HilightLights.setColors(token, leds.ids, intArrayOf(HilightFrames.OFF))
        delay(frameMs)
        HilightLights.closeSession(token)
    }
}
