/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: AutomationNotificationPoster.kt
 * Description: Posts user-configured notifications for automation actions.
 */

package com.sameerasw.essentials.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action

object AutomationNotificationPoster {
    private const val CHANNEL_ID = "automation_notifications"

    @Volatile
    private var channelCreated = false

    fun post(
        context: Context,
        action: Action.SendNotification,
    ) {
        if (!PermissionUtils.isPostNotificationsEnabled(context)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(context, manager)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setAutoCancel(true)
                .setContentIntent(
                    launchIntent?.let {
                        PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    },
                )

        if (action.notificationTitle.isNotBlank()) {
            builder.setContentTitle(action.notificationTitle)
        }
        if (action.body.isNotBlank()) {
            builder.setContentText(action.body)
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(action.body))
        }
        if (action.label.isNotBlank()) {
            builder.setSubText(action.label)
        }

        manager.notify(action.hashCode(), builder.build())
    }

    private fun ensureChannel(
        context: Context,
        manager: NotificationManager,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || channelCreated) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.automation_notifications_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        channelCreated = true
    }
}
