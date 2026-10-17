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
        ScreenshotCleaner.cancel(context, mediaId)
        ScreenshotCleanupNotifier.dismiss(context, mediaId)

        when (intent.action) {
            ACTION_KEEP -> Toast.makeText(context, R.string.screenshot_kept, Toast.LENGTH_SHORT).show()

            ACTION_TRASH_NOW -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (ScreenshotCleaner.trash(context, mediaId, keepStarred = false)) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, R.string.screenshot_moved_to_trash, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_KEEP = "com.sameerasw.essentials.action.KEEP_SCREENSHOT"
        const val ACTION_TRASH_NOW = "com.sameerasw.essentials.action.TRASH_SCREENSHOT"
        const val EXTRA_MEDIA_ID = "extra_media_id"
    }
}
