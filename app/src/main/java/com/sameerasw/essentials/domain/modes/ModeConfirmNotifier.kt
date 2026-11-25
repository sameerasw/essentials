/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeConfirmNotifier.kt
 * Description: Suggests turning a mode on as a Live Update, instead of switching it by itself.
 */

package com.sameerasw.essentials.domain.modes

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.receivers.ModeConfirmReceiver

object ModeConfirmNotifier {
    private const val CHANNEL_ID = "modes"
    private const val LEGACY_CHANNEL_ID = "modes_confirm"
    private const val NOTIFICATION_TAG = "mode_confirm"
    private const val LARGE_ICON_SIZE = 128

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
                .setSmallIcon(R.drawable.rounded_auto_awesome_24)
                .setLargeIcon(modeIcon(context, mode))
                .setContentTitle(context.getString(R.string.mode_confirm_title, mode.name))
                .addAction(0, context.getString(R.string.diy_set_mode_turn_on), responseIntent(context, mode.id, true))
                .addAction(0, context.getString(R.string.mode_confirm_decline), responseIntent(context, mode.id, false))
                .setOngoing(true)
                .setRequestPromotedOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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

    // The mode's own icon on its colour, matching how the Modes tab draws it
    private fun modeIcon(
        context: Context,
        mode: Mode,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(LARGE_ICON_SIZE, LARGE_ICON_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = runCatching { mode.colorHex.toColorInt() }.getOrDefault(Color.LTGRAY)
        canvas.drawCircle(LARGE_ICON_SIZE / 2f, LARGE_ICON_SIZE / 2f, LARGE_ICON_SIZE / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background })

        val icon =
            (if (mode.isLinked) SystemModeIcons.load(context, mode.linkedIconPackage, mode.linkedIconName, mode.linkedType) else null)
                ?: ContextCompat.getDrawable(context, ModeIcons.resolve(mode.iconName))
        icon?.mutate()?.let {
            val inset = LARGE_ICON_SIZE / 4
            it.setBounds(inset, inset, LARGE_ICON_SIZE - inset, LARGE_ICON_SIZE - inset)
            it.setTint(ColorUtils.setAlphaComponent(Color.BLACK, 179))
            it.draw(canvas)
        }
        return bitmap
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.tab_modes), NotificationManager.IMPORTANCE_HIGH))
    }
}
