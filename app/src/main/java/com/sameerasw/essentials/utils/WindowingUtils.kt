/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Windowing & Native Bubbles
 * File: WindowingUtils.kt
 * Description: Helper utility for launching private and standard web previews using native Android Bubbles API.
 */

package com.sameerasw.essentials.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.activities.BubbleWebActivity

object WindowingUtils {
    private const val TAG = "WindowingUtils"
    private const val BUBBLE_CHANNEL_ID = "bubble_web_preview_channel"
    private const val NOTIFICATION_ID = 90210

    /**
     * Checks if native Android Bubbles / floating mode is supported on this device.
     */
    fun isFloatingModeSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val pm = context.packageManager
        val isWatch = pm.hasSystemFeature(PackageManager.FEATURE_WATCH)
        val isTv = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val isAutomotive = pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        if (isWatch || isTv || isAutomotive) return false

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        if (am?.isLowRamDevice == true) return false

        return true
    }

    /**
     * Checks if notification bubbles are enabled globally in system settings.
     */
    fun areNotificationBubblesEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "notification_bubbles", 1) == 1
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Enables notification bubbles globally via WRITE_SECURE_SETTINGS.
     */
    fun enableNotificationBubbles(context: Context): Boolean {
        return try {
            if (PermissionUtils.canWriteSecureSettings(context)) {
                Settings.Secure.putInt(context.contentResolver, "notification_bubbles", 1)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Launches the native Android Bubble for the given URL with optional Private Incognito mode.
     */
    fun launchOverlayWindow(context: Context, uri: Uri, isPrivate: Boolean = false): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isFloatingModeSupported(context)) {
                if (!areNotificationBubblesEnabled(context) && PermissionUtils.canWriteSecureSettings(context)) {
                    enableNotificationBubbles(context)
                }
                launchNativeBubble(context, uri, isPrivate)
                true
            } else {
                // Fallback to standard browser intent for older API levels
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch native bubble window", e)
            false
        }
    }

    private fun launchNativeBubble(context: Context, uri: Uri, isPrivate: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BUBBLE_CHANNEL_ID,
                context.getString(R.string.preview_web_title),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.preview_web_desc)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            nm.createNotificationChannel(channel)
        }

        val targetUrl = uri.toString()
        val host = uri.host ?: targetUrl
        val shortcutId = "bubble_web_preview_${Math.abs(host.hashCode())}"
        val iconRes = R.drawable.rounded_globe_24
        val bubbleIcon = IconCompat.createWithResource(context, iconRes)

        val bubbleIntent = Intent(context, BubbleWebActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            putExtra(BubbleWebActivity.EXTRA_URL, targetUrl)
            putExtra(BubbleWebActivity.EXTRA_PRIVATE_MODE, isPrivate)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // 1. AndroidX Person with matching unique key
        val person = Person.Builder()
            .setName(host)
            .setIcon(bubbleIcon)
            .setKey(shortcutId)
            .setImportant(true)
            .build()

        // 2. Publish Dynamic Conversation Shortcut via ShortcutManagerCompat
        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(host.take(25))
            .setLongLabel(context.getString(R.string.preview_web_title))
            .setIcon(bubbleIcon)
            .setIntent(bubbleIntent)
            .setLongLived(true)
            .setPerson(person)
            .setCategories(setOf("android.shortcut.conversation"))
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

        val displayMetrics = context.resources.displayMetrics
        val screenHeightDp = (displayMetrics.heightPixels / displayMetrics.density).toInt()

        // 3. Build BubbleMetadata using shortcutId on Android 11+ (API 30+)
        val bubbleMetadata = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            NotificationCompat.BubbleMetadata.Builder(shortcutId)
                .setDesiredHeight(screenHeightDp)
                .setAutoExpandBubble(true)
                .build()
        } else {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val bubblePendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                bubbleIntent,
                flags,
            )
            NotificationCompat.BubbleMetadata.Builder(bubblePendingIntent, bubbleIcon)
                .setDesiredHeight(screenHeightDp)
                .setAutoExpandBubble(true)
                .build()
        }

        // 4. Create MessagingStyle Notification linked to the conversation shortcut
        val messagingStyle = NotificationCompat.MessagingStyle(person)
            .addMessage(
                NotificationCompat.MessagingStyle.Message(
                    targetUrl,
                    System.currentTimeMillis(),
                    person,
                )
            )

        val builder = NotificationCompat.Builder(context, BUBBLE_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.preview_web_title))
            .setContentText(host)
            .setSmallIcon(iconRes)
            .setStyle(messagingStyle)
            .setShortcutInfo(shortcut)
            .setShortcutId(shortcutId)
            .setBubbleMetadata(bubbleMetadata)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            nm.notify(NOTIFICATION_ID, builder.build())
        } else {
            // If notification permission is denied on Android 13+, launch activity directly
            context.startActivity(bubbleIntent)
        }
    }
}

