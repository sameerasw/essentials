/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: PermissionGrantUtil.kt
 * Description: Grants as many of the app's permissions as possible through Shizuku/root.
 */

package com.sameerasw.essentials.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService

object PermissionGrantUtil {
    private val APP_OPS = listOf(
        "SYSTEM_ALERT_WINDOW",
        "WRITE_SETTINGS",
        "GET_USAGE_STATS",
        "SCHEDULE_EXACT_ALARM",
        "MANAGE_EXTERNAL_STORAGE",
        "REQUEST_INSTALL_PACKAGES",
        "ACCESS_NOTIFICATIONS",
    )

    fun grantAll(context: Context): Boolean {
        if (!ShellUtils.isAvailable(context) || !ShellUtils.hasPermission(context)) return false
        val pkg = context.packageName
        val commands = mutableListOf<String>()

        requestedPermissions(context).forEach { commands += "pm grant $pkg $it" }
        APP_OPS.forEach { commands += "appops set $pkg $it allow" }

        commands += "cmd notification allow_listener $pkg/${NotificationListener::class.java.name}"
        commands += "cmd notification allow_dnd $pkg"
        commands += "cmd deviceidle whitelist +$pkg"

        val accessibility = "$pkg/${ScreenOffAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        if (!enabled.split(':').contains(accessibility)) {
            val updated = if (enabled.isBlank()) accessibility else "$enabled:$accessibility"
            commands += "settings put secure enabled_accessibility_services '$updated'"
            commands += "settings put secure accessibility_enabled 1"
        }

        ShellUtils.runCommandWithOutput(context, commands.joinToString("; ") { "$it >/dev/null 2>&1" }, notifyOnError = false)
        return true
    }

    private const val OP_SENSITIVE_NOTIFICATIONS = "android:receive_sensitive_notifications"

    fun hasSensitiveNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return true
        return try {
            val appOps = context.getSystemService(AppOpsManager::class.java)
            appOps.unsafeCheckOpNoThrow(OP_SENSITIVE_NOTIFICATIONS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            true
        }
    }

    fun grantSensitiveNotifications(context: Context, featureName: String? = null) {
        val pkg = context.packageName
        val listener = "$pkg/${NotificationListener::class.java.name}"
        ShellUtils.runCommandWithOutput(
            context,
            "appops set $pkg RECEIVE_SENSITIVE_NOTIFICATIONS allow; " +
                "cmd notification disallow_listener $listener; cmd notification allow_listener $listener",
            featureName = featureName,
        )
        if (!PermissionUtils.hasNotificationListenerPermission(context)) {
            ShellUtils.runCommandWithOutput(context, "cmd notification allow_listener $listener", featureName = featureName)
        }
    }

    private fun requestedPermissions(context: Context): List<String> = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }
        info.requestedPermissions?.filter { it.startsWith("android.permission.") }.orEmpty()
    } catch (_: Exception) {
        emptyList()
    }
}
