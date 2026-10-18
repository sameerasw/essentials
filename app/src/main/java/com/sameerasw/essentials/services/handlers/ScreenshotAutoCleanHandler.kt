/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: ScreenshotAutoCleanHandler.kt
 * Description: Schedules every new screenshot for the trash and posts a notification to keep it.
 */

package com.sameerasw.essentials.services.handlers

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.ScreenshotCleaner
import com.sameerasw.essentials.utils.ScreenshotCleanupNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ScreenshotAutoCleanHandler(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val settings = SettingsRepository(context)
    private val mutex = Mutex()

    private val handler = Handler(Looper.getMainLooper())

    private val scan = Runnable { scope.launch(Dispatchers.IO) { scheduleNewScreenshots() } }

    private val observer =
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handler.removeCallbacks(scan)
                handler.postDelayed(scan, SCAN_DEBOUNCE_MS)
            }
        }

    fun register() {
        handler.post(scan)
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
    }

    fun unregister() {
        handler.removeCallbacks(scan)
        context.contentResolver.unregisterContentObserver(observer)
    }

    private suspend fun scheduleNewScreenshots() {
        if (!settings.isScreenshotAutoCleanEnabled()) return
        mutex.withLock {
            val days = settings.getScreenshotAutoCleanDays()
            val since = System.currentTimeMillis() / 1000 - NEW_SCREENSHOT_WINDOW_SECONDS
            ScreenshotCleaner.screenshotIdsAddedSince(context, settings.getScreenshotAutoCleanLastId(), since).forEach { id ->
                ScreenshotCleaner.schedule(context, id, days * MINUTES_PER_DAY, ScreenshotCleaner.TAG_AUTO_CLEAN)
                ScreenshotCleanupNotifier.show(context, id, days)
                settings.setScreenshotAutoCleanLastId(id)
            }
        }
    }

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60L
        private const val NEW_SCREENSHOT_WINDOW_SECONDS = 6 * 60 * 60L
        private const val SCAN_DEBOUNCE_MS = 1_000L
    }
}
