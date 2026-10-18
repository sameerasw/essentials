/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: ScreenshotShareCleanupHandler.kt
 * Description: Moves a screenshot to the trash after it is shared from the screenshot preview, right away or after a delay.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.res.Resources
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.ScreenshotCleaner
import com.sameerasw.essentials.utils.ScreenshotCleanupNotifier
import com.sameerasw.essentials.utils.ScreenshotShareTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenshotShareCleanupHandler(
    private val service: AccessibilityService,
    private val scope: CoroutineScope,
) {
    private val settings = SettingsRepository(service)
    private val tracker by lazy { ScreenshotShareTracker(SHARE_SHEET, systemImageEditorPackage() ?: MARKUP) }
    private var screenshotId: Long? = null

    private val shareDescription by lazy { systemUiString("screenshot_share_description") ?: "Share screenshot" }

    fun onViewClicked(
        event: AccessibilityEvent,
        foregroundPackage: String?,
    ) {
        if (!settings.isScreenshotShareCleanupEnabled()) return
        if (event.packageName == SYSTEM_UI && event.contentDescription?.startsWith(shareDescription) == true) {
            val now = System.currentTimeMillis()
            tracker.onShareTapped(now, foregroundPackage)
            screenshotId = null
            scope.launch(Dispatchers.IO) { screenshotId = findScreenshot(now) }
        }
    }

    fun onForegroundPackage(packageName: String?) {
        if (packageName == null) return
        val sharedAt = tracker.sharedAt
        if (tracker.onForegroundPackage(packageName, System.currentTimeMillis())) {
            trashScreenshot(screenshotId, sharedAt)
        }
    }

    fun onScreenOff() = tracker.onScreenOff()

    private fun trashScreenshot(
        capturedId: Long?,
        sharedAt: Long,
    ) {
        scope.launch {
            val delayMinutes = settings.getScreenshotShareCleanupDelayMinutes()
            if (delayMinutes <= 0) delay(TRASH_DELAY_MS)
            val outcome =
                withContext(Dispatchers.IO) {
                    val mediaId = capturedId ?: findScreenshot(sharedAt) ?: return@withContext null
                    ScreenshotCleanupNotifier.dismiss(service, mediaId)
                    if (delayMinutes > 0) {
                        ScreenshotCleaner.schedule(service, mediaId, delayMinutes.toLong(), ScreenshotCleaner.TAG_SHARED)
                        return@withContext R.string.screenshot_share_cleanup_scheduled
                    }
                    ScreenshotCleaner.cancel(service, mediaId)
                    if (ScreenshotCleaner.trash(service, mediaId, keepStarred = false)) R.string.screenshot_share_cleanup_trashed else null
                }
            outcome?.let { Toast.makeText(service, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun findScreenshot(sharedAt: Long): Long? {
        val sharedAtSeconds = sharedAt / 1000
        return ScreenshotCleaner.findScreenshotAddedBetween(
            service,
            sharedAtSeconds - SCREENSHOT_LOOKBACK_SECONDS,
            sharedAtSeconds + SCREENSHOT_LOOKAHEAD_SECONDS,
        )
    }

    private fun systemUiString(name: String): String? =
        runCatching {
            val res = service.packageManager.getResourcesForApplication(SYSTEM_UI)
            val id = res.getIdentifier(name, "string", SYSTEM_UI)
            if (id != 0) res.getString(id) else null
        }.getOrNull()

    private fun systemImageEditorPackage(): String? =
        runCatching {
            val res = Resources.getSystem()
            val id = res.getIdentifier("config_systemImageEditor", "string", "android")
            if (id != 0) ComponentName.unflattenFromString(res.getString(id))?.packageName else null
        }.getOrNull()

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"
        private const val SHARE_SHEET = "com.android.intentresolver"
        private const val MARKUP = "com.google.android.markup"
        private const val SCREENSHOT_LOOKBACK_SECONDS = 120L
        private const val SCREENSHOT_LOOKAHEAD_SECONDS = 5L
        private const val TRASH_DELAY_MS = 60_000L
    }
}
