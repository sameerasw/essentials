/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: DeviceCare.kt
 * Description: Clears app caches and closes idle background apps.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import java.util.concurrent.atomic.AtomicBoolean

object DeviceCare {
    private val isRunning = AtomicBoolean(false)

    private val ROOT_MANAGERS =
        setOf(
            "com.topjohnwu.magisk",
            "me.weishu.kernelsu",
            "me.bmax.apatch",
        )

    private val COMPANION_PACKAGE_REGEX =
        """(?:packageName|pkg)=['"]?([A-Za-z][\w]*(?:\.[\w]+)+)""".toRegex()

    fun run(
        context: Context,
        action: Action.DeviceCare,
    ) {
        if (!isRunning.compareAndSet(false, true)) return
        try {
            if (!ShellUtils.hasPermission(context)) return

            val before = StatFs(Environment.getDataDirectory().path).availableBytes

            if (action.clearCache) {
                clearCache(context)
            }
            if (action.closeApps) {
                closeApps(context)
            }

            val after = StatFs(Environment.getDataDirectory().path).availableBytes
            val freed = (after - before).coerceAtLeast(0)

            if (action.showResult) {
                val message =
                    if (action.clearCache && freed > 0) {
                        context.getString(
                            R.string.diy_device_care_done,
                            Formatter.formatShortFileSize(context, freed),
                        )
                    } else {
                        context.getString(R.string.diy_device_care_finished)
                    }
                showToast(context, message)
            }
        } finally {
            isRunning.set(false)
        }
    }

    private fun clearCache(context: Context) {
        ShellUtils.runCommand(
            context,
            "pm trim-caches 999G",
            featureName = context.getString(R.string.diy_action_device_care),
        )
    }

    private fun closeApps(context: Context) {
        val packages = getKillablePackages(context)
        if (packages.isEmpty()) return

        val command = packages.joinToString("; ") { "am kill $it" }
        ShellUtils.runCommand(
            context,
            command,
            featureName = context.getString(R.string.diy_action_device_care),
        )
    }

    private fun getKillablePackages(context: Context): List<String> {
        val featureName = context.getString(R.string.diy_action_device_care)
        val psOutput =
            ShellUtils
                .runCommandWithOutput(
                    context,
                    "ps -A -o NAME",
                    featureName = featureName,
                    notifyOnError = false,
                ).orEmpty()
        val runningPackages = parseProcessPackages(psOutput)

        val installedUserApps =
            context.packageManager
                .getInstalledApplications(0)
                .asSequence()
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map { it.packageName }
                .toSet()

        val excluded = getExcludedPackages(context, featureName)
        val candidates = runningPackages.intersect(installedUserApps) - excluded

        return candidates.filter { !FreezeManager.isAppActive(context, it) }.toList()
    }

    internal fun parseProcessPackages(psOutput: String): Set<String> =
        psOutput
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "NAME" }
            .map { it.substringBefore(':') }
            .toSet()

    internal fun parseCompanionPackages(output: String): Set<String> =
        COMPANION_PACKAGE_REGEX
            .findAll(output)
            .map { it.groupValues[1] }
            .toSet()

    private fun getExcludedPackages(
        context: Context,
        featureName: String,
    ): Set<String> {
        val excluded =
            mutableSetOf(
                context.packageName,
                ShizukuUtils.getShizukuPackageName(context),
            )
        excluded.addAll(ROOT_MANAGERS)

        val roleOutput =
            ShellUtils
                .runCommandWithOutput(
                    context,
                    "for r in HOME DIALER SMS ASSISTANT CALL_SCREENING; do cmd role get-role-holders --user 0 android.app.role.\$r; done",
                    featureName = featureName,
                    notifyOnError = false,
                ).orEmpty()
        roleOutput
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.contains(' ') && it.contains('.') }
            .forEach { excluded.add(it) }

        val companionOutput =
            ShellUtils
                .runCommandWithOutput(
                    context,
                    "cmd companiondevice list 0",
                    featureName = featureName,
                    notifyOnError = false,
                ).orEmpty()
        excluded.addAll(parseCompanionPackages(companionOutput))

        Settings.Secure
            .getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
            ?.takeIf { it.isNotBlank() }
            ?.let { excluded.add(it) }

        listOf(
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "enabled_notification_listeners",
        ).forEach { setting ->
            Settings.Secure.getString(context.contentResolver, setting)?.let { value ->
                value.split(':').forEach { entry ->
                    val pkg = entry.substringBefore('/').trim()
                    if (pkg.isNotBlank()) {
                        excluded.add(pkg)
                    }
                }
            }
        }

        return excluded
    }

    private fun showToast(
        context: Context,
        message: String,
    ) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
