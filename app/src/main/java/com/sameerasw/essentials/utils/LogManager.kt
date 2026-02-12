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
    private var lastCrashLog: String? = null
    private val isInitialized = AtomicBoolean(false)

    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    )

    fun init(context: Context) {
        if (isInitialized.getAndSet(true)) return

        // Read last crash log if exists
        val crashFile = File(context.filesDir, CRASH_LOG_FILENAME)
        if (crashFile.exists()) {
            try {
                lastCrashLog = crashFile.readText()
                // delete after reading so we don't report old crashes forever? 
                // meaningful to keep it until a successful report? Let's keep it for now but maybe we can clear it if needed.
                // For now, let's keep it.
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

    private fun handleCrash(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val report = buildString {
            append("Crash Time: ${formatDate(System.currentTimeMillis())}\n")
            append("Thread: ${thread.name}\n")
            append("Exception: ${throwable.javaClass.simpleName}\n")
            append("Message: ${throwable.message}\n")
            append("Stack Trace:\n$stackTrace\n")
            append("\n--- Last Logs before crash ---\n")
            synchronized(logBuffer) {
               // Take last 50 logs for context
               logBuffer.takeLast(50).forEach { entry ->
                   append(formatLogEntry(entry))
                   append("\n")
               }
            }
        }

        try {
            val crashFile = File(context.filesDir, CRASH_LOG_FILENAME)
            crashFile.writeText(report)
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to write crash log", e)
        }
    }

    fun log(tag: String, message: String) {
        addLog("INFO", tag, message)
        Log.i(tag, message)
    }
    
    fun debug(tag: String, message: String) {
        addLog("DEBUG", tag, message)
        Log.d(tag, message)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        addLog("ERROR", tag, message, throwable)
        Log.e(tag, message, throwable)
    }
    
    fun warn(tag: String, message: String) {
        addLog("WARN", tag, message)
        Log.w(tag, message)
    }

    private fun addLog(level: String, tag: String, message: String, throwable: Throwable? = null) {
        synchronized(logBuffer) {
            if (logBuffer.size >= MAX_LOG_SIZE) {
                logBuffer.removeFirst()
            }
            logBuffer.add(LogEntry(System.currentTimeMillis(), level, tag, message, throwable))
        }
    }

    fun generateReport(context: Context, settingsJson: String): String {
        val report = JSONObject()

        // Device Info
        val deviceInfo = JSONObject().apply {
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
                put("AppVersionCode", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode.toLong())
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
            "$base\n${sw}"
        } else {
            base
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
    }
}
