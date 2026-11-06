/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Screenshots
 * File: ScreenshotCleaner.kt
 * Description: Finds screenshots in MediaStore, moves them to the trash and schedules delayed cleanups.
 */

package com.sameerasw.essentials.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.ScreenshotTrashWorker
import java.util.concurrent.TimeUnit

object ScreenshotCleaner {
    const val TAG_SHARED = "screenshot_cleanup_shared"
    const val TAG_AUTO_CLEAN = "screenshot_cleanup_auto"

    private val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    // Pixel and AOSP save to Pictures/Screenshots, most other OEMs to DCIM/Screenshots
    private val SCREENSHOT_PATHS =
        arrayOf(
            "${Environment.DIRECTORY_PICTURES}/${Environment.DIRECTORY_SCREENSHOTS}/",
            "${Environment.DIRECTORY_DCIM}/${Environment.DIRECTORY_SCREENSHOTS}/",
        )

    fun findScreenshotAddedBetween(
        context: Context,
        fromSeconds: Long,
        toSeconds: Long,
    ): Long? =
        context.contentResolver
            .query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH} IN (?, ?) AND ${MediaStore.MediaColumns.DATE_ADDED} BETWEEN ? AND ?",
                arrayOf(*SCREENSHOT_PATHS, fromSeconds.toString(), toSeconds.toString()),
                "${MediaStore.MediaColumns.DATE_ADDED} DESC",
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

    fun screenshotIdsAddedSince(
        context: Context,
        lastSeenId: Long,
        sinceSeconds: Long,
    ): List<Long> =
        context.contentResolver
            .query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH} IN (?, ?) AND ${MediaStore.MediaColumns._ID} > ? AND ${MediaStore.MediaColumns.DATE_ADDED} >= ?",
                arrayOf(*SCREENSHOT_PATHS, lastSeenId.toString(), sinceSeconds.toString()),
                "${MediaStore.MediaColumns._ID} ASC",
            )?.use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) }
            } ?: emptyList()

    fun latestScreenshotId(context: Context): Long =
        context.contentResolver
            .query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH} IN (?, ?)",
                arrayOf(*SCREENSHOT_PATHS),
                "${MediaStore.MediaColumns._ID} DESC",
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L } ?: 0L

    fun trash(
        context: Context,
        mediaId: Long,
        keepStarred: Boolean = true,
    ): Boolean = trashSized(context, mediaId, keepStarred) != null

    fun trashSized(
        context: Context,
        mediaId: Long,
        keepStarred: Boolean = true,
    ): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val uri = ContentUris.withAppendedId(collection, mediaId)
        val size =
            context.contentResolver
                .query(uri, arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.IS_FAVORITE), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst() && !(keepStarred && cursor.getInt(1) == 1)) cursor.getLong(0) else null }
                ?: return null
        val trashed =
            runCatching {
                val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 1) }
                context.contentResolver.update(uri, values, null) > 0
            }.getOrDefault(false)
        if (!trashed) return null
        SettingsRepository(context).recordScreenshotCleaned(size)
        return size
    }

    fun restore(
        context: Context,
        mediaId: Long,
        bytes: Long,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val uri = ContentUris.withAppendedId(collection, mediaId)
        val extras = Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE) }
        val restored =
            runCatching {
                val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 0) }
                context.contentResolver.update(uri, values, extras) > 0
            }.getOrDefault(false)
        if (restored && bytes > 0) SettingsRepository(context).recordScreenshotRestored(bytes)
        return restored
    }

    fun schedule(
        context: Context,
        mediaId: Long,
        delayMinutes: Long,
        tag: String,
    ) {
        val keepStarred = tag != TAG_SHARED
        val request =
            OneTimeWorkRequestBuilder<ScreenshotTrashWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(workDataOf(ScreenshotTrashWorker.KEY_MEDIA_ID to mediaId, ScreenshotTrashWorker.KEY_KEEP_STARRED to keepStarred))
                .addTag(tag)
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(mediaId, tag), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(
        context: Context,
        mediaId: Long,
    ) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(workName(mediaId, TAG_SHARED))
        workManager.cancelUniqueWork(workName(mediaId, TAG_AUTO_CLEAN))
    }

    fun cancelAll(
        context: Context,
        tag: String,
    ) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag)
    }

    private fun workName(
        mediaId: Long,
        tag: String,
    ) = "screenshot_trash_${tag}_$mediaId"
}
