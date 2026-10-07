/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Battery Utilities
 * File: BatteryStatsUtil.kt
 * Description: Calculates per-app battery consumption statistics.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.sameerasw.essentials.data.repository.SettingsRepository

data class BatteryUsageApp(
    val uid: Int,
    val packageName: String?,
    val appName: String,
    val powerMah: Double,
    val fgTimeMs: Long,
    val bgTimeMs: Long,
    val icon: Drawable?,
)

object BatteryStatsUtil {
    fun resetStats(context: Context): Boolean {
        val res1 = ShellUtils.runCommandWithOutput(context, "dumpsys batterystats --reset")
        ShellUtils.runCommand(context, "cmd battery reset")
        if (res1 != null) {
            val repo = SettingsRepository(context)
            repo.putLong("last_battery_stats_reset_time", System.currentTimeMillis())
            BatteryHistoryManager.clearHistory(context)
        }
        return res1 != null
    }

    fun parseUsageApps(context: Context): List<BatteryUsageApp> {
        val output =
            ShellUtils.runCommandWithOutput(context, "dumpsys batterystats --usage")
                ?: return emptyList()

        val pm = context.packageManager
        val list = mutableListOf<BatteryUsageApp>()

        var currentUid: Int? = null
        var currentMah = 0.0
        var currentFg = 0L
        var currentBg = 0L

        fun getSystemUidLabel(
            uid: Int,
            pkg: String?,
        ): String =
            when (uid) {
                -5 -> "Tethering & Hotspot"
                0 -> "Root / Kernel"
                1001 -> "Telephony"
                1003 -> "Graphics / GPU"
                1010 -> "Wi-Fi"
                1013 -> "Media Server"
                1017 -> "Keystore"
                1019 -> "DRM"
                1020 -> "Multicast DNS"
                1021 -> "GPS"
                1036 -> "Log Daemon"
                1040 -> "Media Extractor"
                1041 -> "Audio Server"
                1046 -> "Media Codec"
                1047 -> "Camera Server"
                1053 -> "WebView Zygote"
                1058 -> "Crash Dumps"
                1064 -> "Hardware Security"
                1066 -> "Stats Daemon"
                1067 -> "Incident Daemon"
                1069 -> "Low Memory Killer"
                1072 -> "GPU Service"
                1080 -> "Context Hub"
                1082 -> "ART Service"
                1083 -> "UWB Subsystem"
                1092 -> "PRNG Seeder"
                else -> pkg?.let { getAppName(pm, it) } ?: "System ($uid)"
            }

        output.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("UID ")) {
                val uid = currentUid
                if (uid != null && currentMah > 0.0001 && isVisibleUid(uid)) {
                    val pkg = firstPackageForUid(pm, uid)
                    val label = getSystemUidLabel(uid, pkg)
                    val drawable = pkg?.let { getAppIcon(pm, it) }
                    list.add(
                        BatteryUsageApp(
                            uid,
                            pkg,
                            label,
                            currentMah,
                            currentFg,
                            currentBg,
                            drawable,
                        ),
                    )
                }
                val parts = trimmed.split(":")
                currentUid =
                    parts[0].removePrefix("UID ").trim().let { uStr ->
                        APP_UID_PATTERN.matchEntire(uStr)?.let { match ->
                            val userId = match.groupValues[1].toIntOrNull() ?: 0
                            val appId = match.groupValues[2].toIntOrNull() ?: 0
                            userId * 100000 + 10000 + appId
                        } ?: uStr.toIntOrNull()
                    }
                currentMah =
                    parts
                        .getOrNull(1)
                        ?.trim()
                        ?.split(" ")
                        ?.firstOrNull()
                        ?.toDoubleOrNull() ?: 0.0
                currentFg = 0L
                currentBg = 0L
            } else if (currentUid != null && trimmed.startsWith("cpu=")) {
                // parse times if present
            }
        }

        val lastUid = currentUid
        if (lastUid != null && currentMah > 0.0001 && isVisibleUid(lastUid)) {
            val pkg = firstPackageForUid(pm, lastUid)
            val label = getSystemUidLabel(lastUid, pkg)
            val drawable = pkg?.let { getAppIcon(pm, it) }
            list.add(
                BatteryUsageApp(
                    lastUid,
                    pkg,
                    label,
                    currentMah,
                    currentFg,
                    currentBg,
                    drawable,
                ),
            )
        }

        return list.sortedByDescending { it.powerMah }
    }

    private val APP_UID_PATTERN = Regex("u(\\d+)a(\\d+)")

    private fun isVisibleUid(uid: Int): Boolean = uid < 10000 || uid / 100000 == android.os.Process.myUid() / 100000

    private fun firstPackageForUid(
        pm: PackageManager,
        uid: Int,
    ): String? =
        try {
            pm.getPackagesForUid(uid)?.firstOrNull()
        } catch (_: SecurityException) {
            null
        }

    private fun getAppName(
        pm: PackageManager,
        packageName: String,
    ): String =
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

    private fun getAppIcon(
        pm: PackageManager,
        packageName: String,
    ): Drawable? =
        try {
            pm.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
}
