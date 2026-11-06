/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: AutomationFailureNotifier.kt
 * Description: Checks automation action requirements and posts a notification when an action fails.
 */

package com.sameerasw.essentials.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.MainActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.AppPermission

object AutomationFailureNotifier {
    private const val CHANNEL_ID = "automations"

    fun missingPermissions(
        context: Context,
        action: Action,
    ): List<AppPermission> =
        action.permissions
            .mapNotNull { key ->
                if (key.equals("SHIZUKU", true) || key.equals("ROOT", true)) {
                    if (ShellUtils.isRootEnabled(context)) AppPermission.ROOT else AppPermission.SHIZUKU
                } else {
                    AppPermission.fromKey(key)
                }
            }.distinct()
            .filterNot { it.isGranted(context) }

    fun notifyMissingPermissions(
        context: Context,
        action: Action,
        missing: List<AppPermission>,
    ) {
        val reason = context.getString(R.string.automation_failed_missing, missing.joinToString { context.getString(it.titleRes) })
        notifyFailure(context, action, reason)
    }

    fun notifyFailure(
        context: Context,
        action: Action,
        reason: String,
    ) {
        if (!PermissionUtils.isPostNotificationsEnabled(context)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.automation_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        val actionName = context.getString(action.title)
        val openApp =
            PendingIntent.getActivity(
                context,
                0,
                com.sameerasw.essentials.utils.AppIconUtil.mainIntent(context).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val text = context.getString(R.string.automation_failed_text, actionName, reason)
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(context.getString(R.string.automation_failed_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
        manager.notify(action::class.java.name.hashCode(), notification)
    }
}
