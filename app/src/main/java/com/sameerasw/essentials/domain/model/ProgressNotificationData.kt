/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Models
 * File: ProgressNotificationData.kt
 * Description: Data class representing active progress notification details.
 */

package com.sameerasw.essentials.domain.model

import android.app.PendingIntent
import android.graphics.Bitmap

data class ProgressNotificationData(
    val key: String,
    val packageName: String,
    val progress: Float,
    val isIndeterminate: Boolean,
    val icon: Bitmap?,
    val postTime: Long,
    val title: String? = null,
    val text: String? = null,
    val appName: String? = null,
    val contentIntent: PendingIntent? = null,
    val actions: List<NotificationActionItem> = emptyList(),
)
