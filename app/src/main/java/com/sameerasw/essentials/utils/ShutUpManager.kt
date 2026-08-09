package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.ShutUpAppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

object ShutUpManager {
    private const val TAG = "ShutUpManager"

    // Shared scope for async shell commands. Kept separate so we can cancel pending jobs.
    private val shellScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pendingShellJobs = CopyOnWriteArrayList<Job>()

    private fun cancelPendingShellJobs() {
        pendingShellJobs.forEach { it.cancel() }
        pendingShellJobs.clear()
        Log.d(TAG, "Cancelled all pending shell jobs")
    }

    private val ignoredSystemPackages = listOf(
        "android",
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.google.android.gms"
    )

    fun isPackageIgnored(packageName: String): Boolean {
        return ignoredSystemPackages.contains(packageName) ||
                packageName.startsWith("com.android.inputmethod") ||
                packageName.startsWith("com.google.android.inputmethod") ||
                packageName.contains("autofill")
    }

    fun isAppRunning(context: Context, packageName: String): Boolean {
        if (ShellUtils.isAvailable(context) && ShellUtils.hasPermission(context)) {
            try {
                val output = ShellUtils.runCommandWithOutput(context, "pidof $packageName")
                if (!output.isNullOrBlank()) {
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "pidof check failed for $packageName", e)
            }
            try {
                val output = ShellUtils.runCommandWithOutput(context, "pgrep -f $packageName")
                if (!output.isNullOrBlank()) {
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "pgrep check failed for $packageName", e)
            }
        }

        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val processes = am.runningAppProcesses
            if (processes != null) {
                for (process in processes) {
                    if (process.processName == packageName) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ActivityManager check failed for $packageName", e)
        }

        return false
    }

    fun safeWriteSetting(context: Context, type: String, key: String, value: String): Boolean {
        return safeWriteSettingInternal(context, type, key, value, sync = false)
    }

    suspend fun safeWriteSettingSync(context: Context, type: String, key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        safeWriteSettingInternal(context, type, key, value, sync = true)
    }

    private fun safeWriteSettingInternal(context: Context, type: String, key: String, value: String, sync: Boolean): Boolean {
        val resolver = context.contentResolver
        val resolverSuccess = try {
            when (type.uppercase()) {
                "GLOBAL" -> Settings.Global.putString(resolver, key, value)
                "SECURE" -> Settings.Secure.putString(resolver, key, value)
                "SYSTEM" -> Settings.System.putString(resolver, key, value)
                else -> false
            }
            Log.d(TAG, "Successfully wrote setting via ContentResolver: [$type] $key = $value")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException writing setting via ContentResolver: [$type] $key = $value", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error writing setting via ContentResolver: [$type] $key = $value", e)
            false
        }

        // Special handling for wireless debugging key: write to both global and secure tables
        if (key == "adb_wifi_enabled") {
            try {
                Settings.Global.putString(resolver, key, value)
                Settings.Secure.putString(resolver, key, value)
            } catch (e: Exception) { }
        }

        // Run shell command to ensure daemon receives the setting put
        if (ShellUtils.isAvailable(context) && ShellUtils.hasPermission(context)) {
            val runShell = {
                try {
                    val shellType = type.lowercase()
                    ShellUtils.runCommand(context, "settings put $shellType $key $value")
                    if (key == "adb_wifi_enabled") {
                        val otherType = if (shellType == "global") "secure" else "global"
                        ShellUtils.runCommand(context, "settings put $otherType $key $value")
                    }
                    Log.d(TAG, "Successfully executed setting put via Shell: [$type] $key = $value")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to write setting via Shell: [$type] $key = $value", e)
                }
            }

            if (sync) {
                runShell()
            } else {
                val job = shellScope.launch { runShell() }
                pendingShellJobs.add(job)
                pendingShellJobs.removeAll { it.isCompleted || it.isCancelled }
            }
        }

        return resolverSuccess
    }

    fun safeReadSetting(context: Context, type: String, key: String): String? {
        val resolver = context.contentResolver
        return try {
            when (type.uppercase()) {
                "GLOBAL" -> Settings.Global.getString(resolver, key)
                "SECURE" -> Settings.Secure.getString(resolver, key)
                "SYSTEM" -> Settings.System.getString(resolver, key)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun applyShutUpSettings(context: Context, config: ShutUpAppConfig, repository: SettingsRepository? = null) {
        Log.d(TAG, "Applying ShutUp settings for ${config.packageName}")
        // Cancel any stale revert shell commands before applying to prevent race conditions
        cancelPendingShellJobs()
        withContext(Dispatchers.IO) {
            val repo = repository ?: SettingsRepository(context)
            val currentBackup = repo.getShutUpOriginalSettings()
            val originalSettings = currentBackup.toMutableMap()

            config.settings.forEach { setting ->
                if (setting.enabled) {
                    val resolvedType = if (setting.key == "accessibility_enabled") "SECURE" else setting.settingType
                    val prefixedKey = "${resolvedType.lowercase()}:${setting.key}"
                    if (!originalSettings.containsKey(prefixedKey)) {
                        val currentVal = safeReadSetting(context, resolvedType, setting.key)
                        if (currentVal != null) {
                            originalSettings[prefixedKey] = currentVal
                        }
                    }
                    safeWriteSettingSync(context, resolvedType, setting.key, setting.valueOnLaunch)
                }
            }

            // Special handling for accessibility services
            val disableAccessibility = config.settings.any { it.key == "accessibility_enabled" && it.enabled }
            if (disableAccessibility) {
                val prefixedAccKey = "secure:${Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES}"
                if (!originalSettings.containsKey(prefixedAccKey)) {
                    val currentAccServices = safeReadSetting(context, "SECURE", Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                    if (!currentAccServices.isNullOrEmpty()) {
                        originalSettings[prefixedAccKey] = currentAccServices
                        safeWriteSettingSync(context, "SECURE", Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "")
                    }
                }
            }

            if (originalSettings.isNotEmpty() && originalSettings != currentBackup) {
                repo.saveShutUpOriginalSettings(originalSettings)
            }
        }
    }

    suspend fun revertShutUpSettings(context: Context, config: ShutUpAppConfig) {
        Log.d(TAG, "Reverting ShutUp settings for ${config.packageName}")
        // Cancel any stale apply shell commands before reverting to prevent race conditions
        cancelPendingShellJobs()
        withContext(Dispatchers.IO) {
            config.settings.forEach { setting ->
                if (setting.enabled) {
                    val resolvedType = if (setting.key == "accessibility_enabled") "SECURE" else setting.settingType
                    safeWriteSettingSync(context, resolvedType, setting.key, setting.valueOnRevert)
                }
            }
        }
    }

    suspend fun restoreOriginalSettings(context: Context, repository: SettingsRepository) {
        // Drop any fire-and-forget writes from an older apply before restoring values.
        cancelPendingShellJobs()
        val originalSettings = repository.getShutUpOriginalSettings()
        if (originalSettings.isEmpty()) {
            Log.d(TAG, "No original settings to restore (backup empty)")
            return
        }

        Log.d(TAG, "Restoring original settings from backup (${originalSettings.size} entries)")
        withContext(Dispatchers.IO) {
            var restoredAccessibilityServices = false
            originalSettings.forEach { (prefixedKey, value) ->
                try {
                    val parts = prefixedKey.split(":", limit = 2)
                    if (parts.size < 2) return@forEach
                    val table = parts[0]
                    val key = parts[1]
                    safeWriteSettingSync(context, table, key, value)
                    if (key == Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) {
                        restoredAccessibilityServices = true
                    }
                    Log.d(TAG, "Restored $prefixedKey = $value")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore setting $prefixedKey", e)
                }
            }

            // Re-enable accessibility master switch when the services list was restored.
            // Clearing ENABLED_ACCESSIBILITY_SERVICES also disables the master switch,
            // so we must explicitly turn it back on.
            if (restoredAccessibilityServices) {
                try {
                    Settings.Secure.putString(
                        context.contentResolver,
                        Settings.Secure.ACCESSIBILITY_ENABLED,
                        "1"
                    )
                    Log.d(TAG, "Re-enabled ACCESSIBILITY_ENABLED master switch")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to re-enable ACCESSIBILITY_ENABLED", e)
                }
            }

            // Clear backup so AppFlowHandler doesn't double-restore
            repository.saveShutUpOriginalSettings(emptyMap())
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                context.getString(R.string.shut_up_toast_restored),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    suspend fun restartShizuku(context: Context) {
        Log.d(TAG, "Waiting 1500ms for developer/ADB services to stabilize before restarting Shizuku")
        delay(1500)
        Log.d(TAG, "Attempting Shizuku restart now")

        val repository = SettingsRepository(context)
        val savedToken = repository.getShizukuAuthToken()
        val authTokens = if (savedToken.isNotBlank()) {
            listOf(savedToken, "y95fuaRb9USHiIg724tvTHIs")
        } else {
            listOf("y95fuaRb9USHiIg724tvTHIs")
        }

        authTokens.forEach { token ->
            // Try explicit ManualStartReceiver broadcast
            try {
                val intent = Intent("moe.shizuku.privileged.api.START").apply {
                    setClassName("moe.shizuku.privileged.api", "moe.shizuku.manager.receiver.ManualStartReceiver")
                    putExtra("auth", token)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                context.sendBroadcast(intent)
                Log.d(TAG, "Sent explicit ManualStartReceiver broadcast")
            } catch (e: Exception) {
                Log.e(TAG, "Failed explicit ManualStartReceiver broadcast", e)
            }

            // Try explicit BootReceiver broadcast
            try {
                val intent = Intent("moe.shizuku.privileged.api.START").apply {
                    setClassName("moe.shizuku.privileged.api", "moe.shizuku.manager.receiver.BootReceiver")
                    putExtra("auth", token)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                context.sendBroadcast(intent)
                Log.d(TAG, "Sent explicit BootReceiver broadcast")
            } catch (e: Exception) {
                Log.e(TAG, "Failed explicit BootReceiver broadcast", e)
            }

            // Try legacy/implicit broadcast
            try {
                val intent = Intent("moe.shizuku.privileged.api.START").apply {
                    setPackage("moe.shizuku.privileged.api")
                    putExtra("auth", token)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                context.sendBroadcast(intent)
                Log.d(TAG, "Sent legacy implicit broadcast")
            } catch (e: Exception) {
                Log.e(TAG, "Failed legacy implicit broadcast", e)
            }
        }

        // If shell/root is available, run Shizuku start script
        if (ShellUtils.isAvailable(context) && ShellUtils.hasPermission(context)) {
            Log.d(TAG, "Shell/Root is available, running Shizuku start script via Shell")
            withContext(Dispatchers.IO) {
                val scripts = listOf(
                    "sh /data/data/moe.shizuku.privileged.api/start.sh",
                    "sh /sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh",
                    "sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/files/start.sh"
                )
                scripts.forEach { script ->
                    try {
                        val success = ShellUtils.runCommand(context, script)
                        Log.d(TAG, "Executed shell command: '$script', success: $success")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed shell command: '$script'", e)
                    }
                }
            }
        }
    }
}
