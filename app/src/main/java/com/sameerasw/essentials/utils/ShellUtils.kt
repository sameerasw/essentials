/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core Utilities
 * File: ShellUtils.kt
 * Description: Executes shell commands with root or Shizuku fallback.
 */

package com.sameerasw.essentials.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository

object ShellUtils {
    private var lastAlertTime = 0L
    private const val ALERT_COOLDOWN = 180000L // 3 minutes

    fun getSelectedMode(context: Context): PrivilegedMode {
        val prefs =
            context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(SettingsRepository.KEY_PRIVILEGED_MODE, null)
        if (stored != null) return PrivilegedMode.fromKey(stored)
        return if (prefs.getBoolean(SettingsRepository.KEY_USE_ROOT, false)) {
            PrivilegedMode.ROOT
        } else {
            PrivilegedMode.AUTO
        }
    }

    fun resolveMode(context: Context): PrivilegedMode {
        val selected = getSelectedMode(context)
        if (selected != PrivilegedMode.AUTO) return selected
        return when {
            ShizukuUtils.isShizukuInstalled(context) -> PrivilegedMode.SHIZUKU
            ShizukuUtils.isPorterInstalled(context) -> PrivilegedMode.PORTER
            ShizukuUtils.isDhizukuInstalled(context) -> PrivilegedMode.DHIZUKU
            else -> PrivilegedMode.SHIZUKU
        }
    }

    fun isRootEnabled(context: Context): Boolean = resolveMode(context) == PrivilegedMode.ROOT

    fun usesShizukuBinder(context: Context): Boolean =
        resolveMode(context).let { it == PrivilegedMode.SHIZUKU || it == PrivilegedMode.PORTER }

    fun usesAuthToken(context: Context): Boolean =
        when (resolveMode(context)) {
            PrivilegedMode.SHIZUKU -> !ShizukuUtils.isSheveryFork(context)
            PrivilegedMode.PORTER -> true
            else -> false
        }

    fun isAvailable(context: Context): Boolean =
        if (isRootEnabled(context)) {
            RootUtils.isRootAvailable()
        } else {
            ShizukuUtils.isShizukuAvailable()
        }

    fun hasPermission(context: Context): Boolean =
        if (isRootEnabled(context)) {
            RootUtils.isRootPermissionGranted()
        } else {
            ShizukuUtils.hasPermission()
        }

    fun runCommand(
        context: Context,
        command: String,
        featureName: String? = null,
        notifyOnError: Boolean = true,
    ) {
        if (isRootEnabled(context)) {
            RootUtils.runCommand(command)
        } else {
            if (!ShizukuUtils.isShizukuAvailable()) {
                if (notifyOnError) {
                    val message = if (!featureName.isNullOrBlank()) {
                        context.getString(R.string.shizuku_not_running_feature_desc, featureName)
                    } else {
                        context.getString(R.string.shizuku_not_running_desc)
                    }
                    notifyShizukuError(
                        context,
                        context.getString(R.string.shizuku_not_running_title),
                        message,
                    )
                }
                return
            }
            if (!ShizukuUtils.hasPermission()) {
                if (notifyOnError) {
                    val message = if (!featureName.isNullOrBlank()) {
                        context.getString(R.string.shizuku_permission_missing_feature_desc, featureName)
                    } else {
                        context.getString(R.string.shizuku_permission_missing_desc)
                    }
                    notifyShizukuError(
                        context,
                        context.getString(R.string.shizuku_permission_missing_title),
                        message,
                    )
                }
                return
            }
            ShizukuUtils.runCommand(command)
        }
    }

    fun runCommandWithOutput(
        context: Context,
        command: String,
        featureName: String? = null,
        notifyOnError: Boolean = true,
    ): String? =
        try {
            val process = newProcess(context, arrayOf("sh", "-c", command), featureName, notifyOnError)
            process
                ?.inputStream
                ?.bufferedReader()
                ?.use { it.readText() }
                ?.trim()
        } catch (e: Exception) {
            null
        }

    fun newProcess(
        context: Context,
        command: Array<String>,
        featureName: String? = null,
        notifyOnError: Boolean = true,
    ): Process? {
        return if (isRootEnabled(context)) {
            RootUtils.newProcess(command)
        } else {
            if (!ShizukuUtils.isShizukuAvailable()) {
                if (notifyOnError) {
                    val message = if (!featureName.isNullOrBlank()) {
                        context.getString(R.string.shizuku_not_running_feature_desc, featureName)
                    } else {
                        context.getString(R.string.shizuku_not_running_desc)
                    }
                    notifyShizukuError(
                        context,
                        context.getString(R.string.shizuku_not_running_title),
                        message,
                    )
                }
                return null
            }
            if (!ShizukuUtils.hasPermission()) {
                if (notifyOnError) {
                    val message = if (!featureName.isNullOrBlank()) {
                        context.getString(R.string.shizuku_permission_missing_feature_desc, featureName)
                    } else {
                        context.getString(R.string.shizuku_permission_missing_desc)
                    }
                    notifyShizukuError(
                        context,
                        context.getString(R.string.shizuku_permission_missing_title),
                        message,
                    )
                }
                return null
            }
            try {
                com.sameerasw.essentials.shizuku.ShizukuProcessHelper
                    .newProcess(command)
            } catch (e: Exception) {
                if (notifyOnError) {
                    notifyShizukuError(
                        context,
                        "Shizuku execution error",
                        "An error occurred while running command: ${e.localizedMessage}",
                    )
                }
                null
            }
        }
    }

    private fun notifyShizukuError(
        context: Context,
        title: String,
        message: String,
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAlertTime < ALERT_COOLDOWN) return
        lastAlertTime = currentTime

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

        val channelId = "shizuku_status_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    context.getString(R.string.shizuku_status_alerts_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val restartIntent =
            Intent(
                context,
                com.sameerasw.essentials.services.receivers.ShizukuActionReceiver::class.java,
            ).apply {
                action = "com.sameerasw.essentials.ACTION_RESTART_SHIZUKU"
            }
        val restartPendingIntent =
            PendingIntent.getBroadcast(
                context,
                9001,
                restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat
                .Builder(context, channelId)
                .setSmallIcon(R.drawable.app_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.rounded_power_settings_new_24,
                    context.getString(R.string.action_restart_shizuku),
                    restartPendingIntent,
                )

        notificationManager.notify(9001, builder.build())
    }
}
