/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: LogManager.kt
 * Description: Utility helper for LogManager.kt.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object LogManager {
    private const val MAX_LOG_SIZE = 500
    private val logBuffer = LinkedList<LogEntry>()
    private const val CRASH_LOG_FILENAME = "last_crash.log"
    private const val CRASH_REPORTS_DIR = "crash_reports"
    private const val MAX_CRASH_REPORTS = 5
    private var lastCrashLog: String? = null
    private val isInitialized = AtomicBoolean(false)

    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null,
    )

    fun init(context: Context) {
        if (isInitialized.getAndSet(true)) return

        // Read last crash log if exists
        val crashFile = File(context.filesDir, CRASH_LOG_FILENAME)
        if (crashFile.exists()) {
            try {
                lastCrashLog = crashFile.readText()
            } catch (e: Exception) {
                Log.e("LogManager", "Failed to read crash log", e)
            }
        }

        // Set UncaughtExceptionHandler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                handleCrash(context, thread, throwable)
            } catch (e: Exception) {
                Log.e("LogManager", "Error handling crash", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun getCrashReportsDirectory(context: Context): File {
        val externalDir = context.getExternalFilesDir(CRASH_REPORTS_DIR)
        val dir = if (externalDir != null) {
            externalDir
        } else {
            File(context.filesDir, CRASH_REPORTS_DIR)
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAllCrashReports(context: Context): List<File> {
        val dir = getCrashReportsDirectory(context)
        return dir.listFiles { file -> file.isFile && (file.extension == "log" || file.extension == "txt") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun getLatestCrashReport(context: Context): File? {
        return getAllCrashReports(context).firstOrNull()
    }

    fun clearAllCrashReports(context: Context) {
        try {
            getCrashReportsDirectory(context).listFiles()?.forEach { it.delete() }
            File(context.filesDir, CRASH_LOG_FILENAME).delete()
            lastCrashLog = null
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to clear crash reports", e)
        }
    }

    private fun pruneOldCrashReports(context: Context) {
        try {
            val reports = getAllCrashReports(context)
            if (reports.size > MAX_CRASH_REPORTS) {
                reports.drop(MAX_CRASH_REPORTS).forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to prune crash reports", e)
        }
    }

    fun saveCrashReport(
        context: Context,
        threadName: String = Thread.currentThread().name,
        throwable: Throwable? = null,
        customMessage: String? = null,
    ): File? {
        val timestamp = System.currentTimeMillis()
        val fileDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(timestamp))
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        } ?: ""

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }

        val report = buildString {
            append("Crash Time: ${formatDate(timestamp)}\n")
            append("App Version: $appVersion\n")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})\n")
            append("Android OS: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
            append("Thread: $threadName\n")
            if (throwable != null) {
                append("Exception: ${throwable.javaClass.name}\n")
                append("Message: ${throwable.message}\n")
                append("Stack Trace:\n$stackTrace\n")
            }
            if (!customMessage.isNullOrBlank()) {
                append("Details:\n$customMessage\n")
            }
            append("\n--- Last Logs before crash ---\n")
            synchronized(logBuffer) {
                logBuffer.takeLast(50).forEach { entry ->
                    append(formatLogEntry(entry))
                    append("\n")
                }
            }
        }

        return try {
            // Write legacy last_crash.log
            val crashFile = File(context.filesDir, CRASH_LOG_FILENAME)
            crashFile.writeText(report)
            lastCrashLog = report

            // Write dated file in crash_reports directory
            val reportsDir = getCrashReportsDirectory(context)
            val logFile = File(reportsDir, "crash_$fileDateFormat.log")
            logFile.writeText(report)

            pruneOldCrashReports(context)
            logFile
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to write crash log", e)
            null
        }
    }

    private fun handleCrash(
        context: Context,
        thread: Thread,
        throwable: Throwable,
    ) {
        saveCrashReport(
            context = context,
            threadName = thread.name,
            throwable = throwable,
        )
    }

    fun log(
        tag: String,
        message: String,
    ) {
        addLog("INFO", tag, message)
        Log.i(tag, message)
    }

    fun debug(
        tag: String,
        message: String,
    ) {
        addLog("DEBUG", tag, message)
        Log.d(tag, message)
    }

    fun error(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        addLog("ERROR", tag, message, throwable)
        Log.e(tag, message, throwable)
    }

    fun warn(
        tag: String,
        message: String,
    ) {
        addLog("WARN", tag, message)
        Log.w(tag, message)
    }

    private fun addLog(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        synchronized(logBuffer) {
            if (logBuffer.size >= MAX_LOG_SIZE) {
                logBuffer.removeFirst()
            }
            logBuffer.add(LogEntry(System.currentTimeMillis(), level, tag, message, throwable))
        }
    }

    fun generateReport(
        context: Context,
        settingsJson: String,
    ): String {
        val report = JSONObject()

        // Device Info
        val deviceInfo =
            JSONObject().apply {
                put("Manufacturer", Build.MANUFACTURER)
                put("Model", Build.MODEL)
                put("Brand", Build.BRAND)
                put("Device", Build.DEVICE)
                put("Board", Build.BOARD)
                put("Hardware", Build.HARDWARE)
                put("AndroidVersion", Build.VERSION.RELEASE)
                put("SDK", Build.VERSION.SDK_INT)
                put("SecurityPatch", Build.VERSION.SECURITY_PATCH)
                try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    put("AppVersionName", pInfo.versionName)
                    put(
                        "AppVersionCode",
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            pInfo.longVersionCode
                        } else {
                            @Suppress(
                                "DEPRECATION",
                            )
                            pInfo.versionCode.toLong()
                        },
                    )
                } catch (e: Exception) {
                    put("AppVersion", "Unknown")
                }
            }
        report.put("device_info", deviceInfo)

        // Logs
        val logsArray = JSONArray()
        synchronized(logBuffer) {
            logBuffer.forEach { entry ->
                logsArray.put(formatLogEntry(entry))
            }
        }
        report.put("logs", logsArray)

        // Crash Log
        if (lastCrashLog != null) {
            report.put("last_crash_log", lastCrashLog)
        }

        // Settings
        try {
            report.put("settings", JSONObject(settingsJson))
        } catch (e: Exception) {
            report.put("settings", "Failed to parse settings JSON: ${e.message}")
            report.put("settings_raw", settingsJson)
        }

        return report.toString(4) // Pretty print with 4 indentation
    }

    private fun formatLogEntry(entry: LogEntry): String {
        val base = "${formatDate(entry.timestamp)} [${entry.level}] ${entry.tag}: ${entry.message}"
        return if (entry.throwable != null) {
            val sw = StringWriter()
            entry.throwable.printStackTrace(PrintWriter(sw))
            "$base\n$sw"
        } else {
            base
        }
    }

    private fun formatDate(timestamp: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}
