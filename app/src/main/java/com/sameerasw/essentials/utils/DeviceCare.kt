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
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action

object DeviceCare {
    fun run(
        context: Context,
        action: Action.DeviceCare,
    ) {
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

        val message =
            if (action.clearCache) {
                context.getString(R.string.diy_device_care_done, Formatter.formatShortFileSize(context, freed))
            } else {
                context.getString(R.string.diy_device_care_done_apps)
            }
        showToast(context, message)
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
        val excluded = getExcludedPackages(context)
        return context.packageManager
            .getInstalledApplications(0)
            .asSequence()
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { it.packageName }
            .filter { it !in excluded && !FreezeManager.isAppActive(context, it) }
            .toList()
    }

    private fun getExcludedPackages(context: Context): Set<String> {
        val excluded = mutableSetOf(context.packageName, "moe.shizuku.privileged.api")

        context.packageManager
            .resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY,
            )?.activityInfo
            ?.packageName
            ?.let { excluded.add(it) }

        Settings.Secure
            .getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
            ?.takeIf { it.isNotBlank() }
            ?.let { excluded.add(it) }

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
