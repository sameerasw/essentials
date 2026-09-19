/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Models
 * File: ActiveNotificationAlert.kt
 * Description: Data class representing an incoming notification alert for Duo notification island.
 */

package com.sameerasw.essentials.domain.model

import android.app.PendingIntent
import android.graphics.Bitmap

data class ActiveNotificationAlert(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val icon: Bitmap?,
    val contentIntent: PendingIntent?,
    val timestamp: Long = System.currentTimeMillis(),
    val appColor: Int? = null,
    val senderName: String? = null,
    val appName: String? = null,
    val appIcon: Bitmap? = null,
    val actions: List<NotificationActionItem> = emptyList(),
)

