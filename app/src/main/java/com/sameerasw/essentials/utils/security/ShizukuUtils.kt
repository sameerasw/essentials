/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Security Utilities
 * File: ShizukuUtils.kt
 * Description: Wrapper utility for interacting with Shizuku binder and privileged commands.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

object ShizukuUtils {
    private var binder: IBinder? = null

    private fun shizukuPermissionInfo(context: Context) =
        runCatching {
            context.packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0)
        }.getOrNull()

    fun isShizukuInstalled(context: Context): Boolean = shizukuPermissionInfo(context) != null

    private const val DHIZUKU_PACKAGE = "com.rosan.dhizuku"
    private const val PORTER_PACKAGE = "eu.darken.porter"
    private const val PORTER_ACTION_START = "eu.darken.porter.START"
    private const val PORTER_ACTION_STOP = "eu.darken.porter.STOP"
    private const val PORTER_TOKEN_EXTRA = "auth"

    fun isDhizukuInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(DHIZUKU_PACKAGE, 0) }.isSuccess

    fun isPorterInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(PORTER_PACKAGE, 0) }.isSuccess

    fun getShizukuPackageName(context: Context): String =
        shizukuPermissionInfo(context)?.packageName ?: "moe.shizuku.privileged.api"

    private const val SHEVERY_RECEIVER_CLASS = "moe.shizuku.manager.receiver.SheveryControlReceiver"
    private const val SHEVERY_ACTION_START = "moe.shizuku.manager.action.START_SERVER"
    private const val SHEVERY_ACTION_STOP = "moe.shizuku.manager.action.STOP_SERVER"

    fun isSheveryFork(context: Context): Boolean = isSheveryFork(context, getShizukuPackageName(context))

    private fun isSheveryFork(
        context: Context,
        shizukuPackage: String,
    ): Boolean =
        try {
            context.packageManager.getReceiverInfo(
                android.content.ComponentName(shizukuPackage, SHEVERY_RECEIVER_CLASS),
                0,
            )
            true
        } catch (_: Exception) {
            false
        }

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {
            binder = Shizuku.getBinder()
            try {
                if (hasPermission()) {
                    val appCtx = com.sameerasw.essentials.EssentialsApp.context
                    StatusBarManager.update(appCtx)
                }
            } catch (_: Throwable) {
            }
        }

    private val binderDeadListener =
        Shizuku.OnBinderDeadListener {
            binder = null
        }

    private val isBinderAlive: Boolean
        get() {
            if (binder?.isBinderAlive == true) return true
            return try {
                if (Shizuku.pingBinder()) {
                    binder = Shizuku.getBinder()
                    binder?.isBinderAlive == true
                } else {
                    false
                }
            } catch (
                @Suppress("UNUSED_PARAMETER") e: Exception,
            ) {
                false
            }
        }

    fun initialize() {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    fun hasPermission(): Boolean {
        if (!isBinderAlive) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (
            @Suppress("UNUSED_PARAMETER") e: Exception,
        ) {
            false
        }
    }

    fun isShizukuAvailable(): Boolean =
        try {
            Shizuku.pingBinder()
            true
        } catch (
            @Suppress("UNUSED_PARAMETER") e: Exception,
        ) {
            false
        }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(1003)
        } catch (
            @Suppress("UNUSED_PARAMETER") e: Exception,
        ) {
            // Permission request failed
        }
    }

    fun runCommand(command: String) {
        if (!hasPermission() || !isBinderAlive) return

        val service = IShizukuService.Stub.asInterface(binder)
        try {
            val process = service.newProcess(arrayOf("sh", "-c", command), null, "/")
            process?.waitFor()
        } catch (
            @Suppress("UNUSED_PARAMETER") e: RemoteException,
        ) {
            // Command execution failed
        }
    }

    fun grantWriteSecureSettingsPermission(): Boolean {
        if (!hasPermission() || !isBinderAlive) return false

        return try {
            runCommand("pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS")
            true
        } catch (
            @Suppress("UNUSED_PARAMETER") e: Exception,
        ) {
            false
        }
    }

    fun grantBatteryStatsPermission(): Boolean {
        if (!hasPermission() || !isBinderAlive) return false

        return try {
            runCommand("pm grant com.sameerasw.essentials android.permission.BATTERY_STATS")
            true
        } catch (
            @Suppress("UNUSED_PARAMETER") e: Exception,
        ) {
            false
        }
    }

    fun getSystemBinder(name: String): IBinder? {
        if (!hasPermission() || !isBinderAlive) return null

        val service = IShizukuService.Stub.asInterface(binder)
        return try {
            // Try known method names for Shizuku v13
            val method =
                service.javaClass.methods.find { it.name == "getSystemBinder" || it.name == "getService" }
            if (method != null) {
                if (method.parameterCount == 1) {
                    method.invoke(service, name) as? IBinder
                } else if (method.parameterCount == 2) {
                    method.invoke(service, name, null) as? IBinder
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun toggleShizuku(
        context: android.content.Context,
        start: Boolean,
    ) {
        if (ShellUtils.resolveMode(context) == PrivilegedMode.PORTER) {
            togglePorter(context, start)
            return
        }

        val shizukuPackage = getShizukuPackageName(context)

        if (isSheveryFork(context, shizukuPackage)) {
            val action = if (start) SHEVERY_ACTION_START else SHEVERY_ACTION_STOP
            try {
                val intent =
                    android.content.Intent(action).apply {
                        component = android.content.ComponentName(shizukuPackage, SHEVERY_RECEIVER_CLASS)
                        addFlags(android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                android.util.Log.e("ShizukuUtils", "Failed to ${if (start) "start" else "stop"} Shevery", e)
            }
            return
        }

        val action = if (start) "moe.shizuku.privileged.api.START" else "moe.shizuku.privileged.api.STOP"
        val settingsRepository =
            com.sameerasw.essentials.data.repository
                .SettingsRepository(context)
        val token = settingsRepository.getShizukuAuthToken()
        if (token.isEmpty()) {
            android.util.Log.w("ShizukuUtils", "Shizuku auth token is missing, cannot stop Shizuku")
            return
        }
        try {
            val intent =
                android.content.Intent(action).apply {
                    `package` = shizukuPackage
                    putExtra("auth", token)
                    addFlags(android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            android.util.Log.e("ShizukuUtils", "Failed to ${if (start) "start" else "stop"} Shizuku", e)
        }
    }

    private fun togglePorter(
        context: Context,
        start: Boolean,
    ) {
        val token =
            com.sameerasw.essentials.data.repository
                .SettingsRepository(context)
                .getShizukuAuthToken()
        if (token.isEmpty()) {
            android.util.Log.w("ShizukuUtils", "Porter token is missing, cannot toggle Porter")
            return
        }
        try {
            val intent =
                android.content.Intent(if (start) PORTER_ACTION_START else PORTER_ACTION_STOP).apply {
                    `package` = PORTER_PACKAGE
                    putExtra(PORTER_TOKEN_EXTRA, token)
                    addFlags(android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            android.util.Log.e("ShizukuUtils", "Failed to ${if (start) "start" else "stop"} Porter", e)
        }
    }
}
