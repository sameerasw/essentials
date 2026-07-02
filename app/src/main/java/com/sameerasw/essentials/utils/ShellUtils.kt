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

    fun isRootEnabled(context: Context): Boolean {
        val prefs =
            context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(SettingsRepository.KEY_USE_ROOT, false)
    }

    fun isAvailable(context: Context): Boolean {
        return if (isRootEnabled(context)) {
            RootUtils.isRootAvailable()
        } else {
            ShizukuUtils.isShizukuAvailable()
        }
    }

    fun hasPermission(context: Context): Boolean {
        return if (isRootEnabled(context)) {
            RootUtils.isRootPermissionGranted()
        } else {
            ShizukuUtils.hasPermission()
        }
    }

    fun runCommand(context: Context, command: String) {
        if (isRootEnabled(context)) {
            RootUtils.runCommand(command)
        } else {
            if (!ShizukuUtils.isShizukuAvailable()) {
                notifyShizukuError(context, "Shizuku is not running", "Please start Shizuku from its app to enable features.")
                return
            }
            if (!ShizukuUtils.hasPermission()) {
                notifyShizukuError(context, "Shizuku permission missing", "Please grant Shizuku permission for Essentials.")
                return
            }
            ShizukuUtils.runCommand(command)
        }
    }

    fun runCommandWithOutput(context: Context, command: String): String? {
        return try {
            val process = newProcess(context, arrayOf("sh", "-c", command))
            process?.inputStream?.bufferedReader()?.use { it.readText() }?.trim()
        } catch (e: Exception) {
            null
        }
    }

    fun newProcess(context: Context, command: Array<String>): Process? {
        return if (isRootEnabled(context)) {
            RootUtils.newProcess(command)
        } else {
            if (!ShizukuUtils.isShizukuAvailable()) {
                notifyShizukuError(context, "Shizuku is not running", "Please start Shizuku to enable features.")
                return null
            }
            if (!ShizukuUtils.hasPermission()) {
                notifyShizukuError(context, "Shizuku permission missing", "Please grant Shizuku permission for Essentials.")
                return null
            }
            try {
                com.sameerasw.essentials.shizuku.ShizukuProcessHelper.newProcess(command)
            } catch (e: Exception) {
                notifyShizukuError(context, "Shizuku execution error", "An error occurred while running command: ${e.localizedMessage}")
                null
            }
        }
    }

    private fun notifyShizukuError(context: Context, title: String, message: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAlertTime < ALERT_COOLDOWN) return
        lastAlertTime = currentTime

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

        val channelId = "shizuku_status_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Shizuku Status Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val restartIntent = Intent(context, com.sameerasw.essentials.services.receivers.ShizukuActionReceiver::class.java).apply {
            action = "com.sameerasw.essentials.ACTION_RESTART_SHIZUKU"
        }
        val restartPendingIntent = PendingIntent.getBroadcast(
            context,
            9001,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.rounded_power_settings_new_24,
                context.getString(R.string.action_restart_shizuku),
                restartPendingIntent
            )

        notificationManager.notify(9001, builder.build())
    }
}
