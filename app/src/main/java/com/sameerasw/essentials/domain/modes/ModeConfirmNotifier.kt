/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeConfirmNotifier.kt
 * Description: Asks before a mode turns on by itself.
 */

package com.sameerasw.essentials.domain.modes

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.receivers.ModeConfirmReceiver

object ModeConfirmNotifier {
    private const val CHANNEL_ID = "modes_confirm"
    private const val NOTIFICATION_TAG = "mode_confirm"

    fun show(
        context: Context,
        mode: Mode,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel(context)
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(ModeIcons.resolve(mode.iconName))
                .setContentTitle(context.getString(R.string.mode_confirm_title, mode.name))
                .addAction(0, context.getString(R.string.diy_set_mode_turn_on), responseIntent(context, mode.id, true))
                .addAction(0, context.getString(R.string.mode_confirm_decline), responseIntent(context, mode.id, false))
                .setDeleteIntent(responseIntent(context, mode.id, false))
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_TAG, mode.id.hashCode(), notification)
    }

    fun dismiss(
        context: Context,
        modeId: String,
    ) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_TAG, modeId.hashCode())
    }

    private fun responseIntent(
        context: Context,
        modeId: String,
        accept: Boolean,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            modeId.hashCode() * 2 + if (accept) 1 else 0,
            Intent(context, ModeConfirmReceiver::class.java)
                .putExtra(ModeConfirmReceiver.EXTRA_MODE_ID, modeId)
                .putExtra(ModeConfirmReceiver.EXTRA_ACCEPT, accept),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(CHANNEL_ID, context.getString(R.string.mode_confirm_channel), NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
