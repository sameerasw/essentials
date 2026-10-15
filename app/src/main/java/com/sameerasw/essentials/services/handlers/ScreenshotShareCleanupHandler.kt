/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: ScreenshotShareCleanupHandler.kt
 * Description: Moves a screenshot to the trash after it is shared from the screenshot preview.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.res.Resources
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
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
    private var screenshotUri: Uri? = null

    private val shareDescription by lazy { systemUiString("screenshot_share_description") ?: "Share screenshot" }

    fun onViewClicked(
        event: AccessibilityEvent,
        foregroundPackage: String?,
    ) {
        if (!settings.isScreenshotShareCleanupEnabled()) return
        if (event.packageName == SYSTEM_UI && event.contentDescription?.startsWith(shareDescription) == true) {
            val now = System.currentTimeMillis()
            tracker.onShareTapped(now, foregroundPackage)
            screenshotUri = null
            scope.launch(Dispatchers.IO) { screenshotUri = findScreenshot(now) }
        }
    }

    fun onForegroundPackage(packageName: String?) {
        if (packageName == null) return
        val sharedAt = tracker.sharedAt
        if (tracker.onForegroundPackage(packageName, System.currentTimeMillis())) {
            trashScreenshot(screenshotUri, sharedAt)
        }
    }

    fun onScreenOff() = tracker.onScreenOff()

    private fun trashScreenshot(
        capturedUri: Uri?,
        sharedAt: Long,
    ) {
        scope.launch {
            delay(TRASH_DELAY_MS)
            val trashed =
                withContext(Dispatchers.IO) {
                    val uri = capturedUri ?: findScreenshot(sharedAt) ?: return@withContext false
                    runCatching {
                        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 1) }
                        service.contentResolver.update(uri, values, null) > 0
                    }.getOrDefault(false)
                }
            if (trashed) {
                Toast.makeText(service, R.string.screenshot_share_cleanup_trashed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun findScreenshot(sharedAt: Long): Uri? {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DATE_ADDED} BETWEEN ? AND ?"
        val sharedAtSeconds = sharedAt / 1000
        val args =
            arrayOf(
                SCREENSHOTS_PATH,
                (sharedAtSeconds - SCREENSHOT_LOOKBACK_SECONDS).toString(),
                (sharedAtSeconds + SCREENSHOT_LOOKAHEAD_SECONDS).toString(),
            )
        return service.contentResolver
            .query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                args,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC",
            )?.use { cursor ->
                if (cursor.moveToFirst()) ContentUris.withAppendedId(collection, cursor.getLong(0)) else null
            }
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
        private val SCREENSHOTS_PATH = "${Environment.DIRECTORY_PICTURES}/${Environment.DIRECTORY_SCREENSHOTS}/"
        private const val SCREENSHOT_LOOKBACK_SECONDS = 120L
        private const val SCREENSHOT_LOOKAHEAD_SECONDS = 5L
        private const val TRASH_DELAY_MS = 60_000L
    }
}
