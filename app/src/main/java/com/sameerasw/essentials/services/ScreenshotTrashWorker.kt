/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: ScreenshotTrashWorker.kt
 * Description: Moves a scheduled screenshot to the trash when its cleanup delay runs out.
 */

package com.sameerasw.essentials.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sameerasw.essentials.utils.ScreenshotCleaner
import com.sameerasw.essentials.utils.ScreenshotCleanupNotifier

class ScreenshotTrashWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val mediaId = inputData.getLong(KEY_MEDIA_ID, -1L)
        if (mediaId < 0) return Result.failure()
        ScreenshotCleaner.trash(applicationContext, mediaId, inputData.getBoolean(KEY_KEEP_STARRED, true))
        ScreenshotCleanupNotifier.dismiss(applicationContext, mediaId)
        return Result.success()
    }

    companion object {
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_KEEP_STARRED = "keep_starred"
    }
}
