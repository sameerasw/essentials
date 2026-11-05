/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Security Utilities
 * File: SheveryUtils.kt
 * Description: Wrapper utility for interacting with Shevery native Privilege API and privileged commands.
 */

package com.sameerasw.essentials.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import com.hamondev.shevery.api.SheveryApiConstants
import com.hamondev.shevery.api.SheveryPrivilege
import com.hamondev.shevery.api.SheveryProvider
import com.sameerasw.essentials.EssentialsApp
import com.sameerasw.essentials.data.repository.SettingsRepository

object SheveryUtils {
    private const val SHEVERY_PACKAGE = SheveryApiConstants.MANAGER_APPLICATION_ID
    private const val SHEVERY_PLUGIN_RECEIVER = "com.hamondev.shevery.tasker.PluginReceiver"
    private const val ACTION_START = "com.hamondev.shevery.action.START_SERVER"
    private const val ACTION_STOP = "com.hamondev.shevery.action.STOP_SERVER"
    private const val ACTION_RESTART = "com.hamondev.shevery.action.RESTART_SERVER"
    private const val EXTRA_AUTH = "auth"

    fun isSheveryInstalled(context: Context): Boolean =
        runCatching {
            context.packageManager.getPermissionInfo(SheveryProvider.PERMISSION, 0) != null
        }.getOrDefault(false) ||
        runCatching {
            context.packageManager.getPackageInfo(SHEVERY_PACKAGE, 0)
            true
        }.getOrDefault(false)

    fun isSheveryAvailable(): Boolean =
        try {
            SheveryPrivilege.pingBinder()
        } catch (_: Throwable) {
            false
        }

    fun hasPermission(): Boolean {
        if (!isSheveryAvailable()) return false
        return try {
            SheveryPrivilege.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestPermission(requestCode: Int = 1004) {
        try {
            SheveryPrivilege.requestPermission(requestCode)
        } catch (_: Throwable) {
        }
    }

    fun initialize(context: Context) {
        try {
            SheveryPrivilege.connectFast(context)
        } catch (_: Throwable) {
        }
        SheveryPrivilege.addBinderReceivedListenerSticky {
            try {
                if (hasPermission()) {
                    val appCtx = EssentialsApp.context
                    StatusBarManager.update(appCtx)
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun runCommand(command: String) {
        if (!hasPermission() || !isSheveryAvailable()) return
        try {
            val process = SheveryPrivilege.newProcess(arrayOf("sh", "-c", command), null, "/")
            process?.waitFor()
        } catch (_: Throwable) {
        }
    }

    fun newProcess(
        cmd: Array<String>,
        env: Array<String>? = null,
        dir: String? = null,
    ): Process? {
        if (!hasPermission() || !isSheveryAvailable()) return null
        return try {
            SheveryPrivilege.newProcess(cmd, env, dir)
        } catch (_: Throwable) {
            null
        }
    }

    fun grantWriteSecureSettingsPermission(): Boolean {
        if (!hasPermission() || !isSheveryAvailable()) return false
        return try {
            runCommand("pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS")
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun grantBatteryStatsPermission(): Boolean {
        if (!hasPermission() || !isSheveryAvailable()) return false
        return try {
            runCommand("pm grant com.sameerasw.essentials android.permission.BATTERY_STATS")
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun getSystemBinder(name: String): IBinder? {
        if (!hasPermission() || !isSheveryAvailable()) return null
        return try {
            SheveryPrivilege.getSystemService(name)
        } catch (_: Throwable) {
            null
        }
    }

    fun toggleShevery(
        context: Context,
        start: Boolean,
    ) {
        val action = if (start) ACTION_START else ACTION_STOP
        val token = SettingsRepository(context).getShizukuAuthToken()
        try {
            val intent = Intent(action).apply {
                component = ComponentName(SHEVERY_PACKAGE, SHEVERY_PLUGIN_RECEIVER)
                `package` = SHEVERY_PACKAGE
                if (token.isNotEmpty()) {
                    putExtra(EXTRA_AUTH, token)
                }
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            android.util.Log.e("SheveryUtils", "Failed to ${if (start) "start" else "stop"} Shevery", e)
        }
    }

    fun restartShevery(context: Context) {
        val token = SettingsRepository(context).getShizukuAuthToken()
        try {
            val intent = Intent(ACTION_RESTART).apply {
                component = ComponentName(SHEVERY_PACKAGE, SHEVERY_PLUGIN_RECEIVER)
                `package` = SHEVERY_PACKAGE
                if (token.isNotEmpty()) {
                    putExtra(EXTRA_AUTH, token)
                }
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            android.util.Log.e("SheveryUtils", "Failed to restart Shevery", e)
        }
    }
}
