/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: ScreenshotCleanupActionReceiver.kt
 * Description: Handles the Keep and Trash now actions on the screenshot cleanup notification.
 */

package com.sameerasw.essentials.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.ScreenshotCleaner
import com.sameerasw.essentials.utils.ScreenshotCleanupNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenshotCleanupActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val mediaId = intent.getLongExtra(EXTRA_MEDIA_ID, -1L)
        if (mediaId < 0) return
        val days = intent.getIntExtra(EXTRA_DAYS, 7)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_KEEP -> {
                        ScreenshotCleaner.cancel(context, mediaId)
                        ScreenshotCleanupNotifier.showResult(context, mediaId, R.string.screenshot_kept, days, NOT_TRASHED)
                    }

                    ACTION_TRASH_NOW -> {
                        val size = ScreenshotCleaner.trashSized(context, mediaId, keepStarred = false)
                        if (size == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, R.string.screenshot_trash_failed, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            ScreenshotCleaner.cancel(context, mediaId)
                            ScreenshotCleanupNotifier.showResult(context, mediaId, R.string.screenshot_moved_to_trash, days, size)
                        }
                    }

                    ACTION_UNDO -> {
                        val trashedBytes = intent.getLongExtra(EXTRA_TRASHED_BYTES, NOT_TRASHED)
                        val restored = trashedBytes == NOT_TRASHED || ScreenshotCleaner.restore(context, mediaId, trashedBytes)
                        if (restored && SettingsRepository(context).isScreenshotAutoCleanEnabled()) {
                            ScreenshotCleaner.schedule(context, mediaId, days * MINUTES_PER_DAY, ScreenshotCleaner.TAG_AUTO_CLEAN)
                            ScreenshotCleanupNotifier.show(context, mediaId, days)
                        } else {
                            ScreenshotCleanupNotifier.dismiss(context, mediaId)
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_KEEP = "com.sameerasw.essentials.action.KEEP_SCREENSHOT"
        const val ACTION_TRASH_NOW = "com.sameerasw.essentials.action.TRASH_SCREENSHOT"
        const val ACTION_UNDO = "com.sameerasw.essentials.action.UNDO_SCREENSHOT_ACTION"
        const val EXTRA_MEDIA_ID = "extra_media_id"
        const val EXTRA_DAYS = "extra_days"
        const val EXTRA_TRASHED_BYTES = "extra_trashed_bytes"
        private const val NOT_TRASHED = -1L
        private const val MINUTES_PER_DAY = 24 * 60L
    }
}
