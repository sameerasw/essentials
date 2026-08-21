package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.ShutUpAppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object ShutUpManager {
    private const val TAG = "ShutUpManager"

    // Every caller (shortcut, accessibility event, and foreground service) uses the same
    // serialized setting transaction. This prevents an old restore from racing a new apply.
    private val settingsMutex = Mutex()

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
        return safeWriteSettingInternal(context, type, key, value)
    }

    suspend fun safeWriteSettingSync(context: Context, type: String, key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        safeWriteSettingInternal(context, type, key, value)
    }

    private fun safeWriteSettingInternal(context: Context, type: String, key: String, value: String): Boolean {
        val resolver = context.contentResolver
        val resolverSuccess = try {
            val result = when (type.uppercase()) {
                "GLOBAL" -> Settings.Global.putString(resolver, key, value)
                "SECURE" -> Settings.Secure.putString(resolver, key, value)
                "SYSTEM" -> Settings.System.putString(resolver, key, value)
                else -> false
            }
            Log.d(TAG, "Wrote setting via ContentResolver: [$type] $key = $value (success=$result)")
            result
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

        var shellSuccess = false
        if (ShellUtils.isAvailable(context) && ShellUtils.hasPermission(context)) {
            try {
                val shellType = type.lowercase()
                ShellUtils.runCommand(context, "settings put $shellType $key $value")
                shellSuccess = true
                if (key == "adb_wifi_enabled") {
                    val otherType = if (shellType == "global") "secure" else "global"
                    ShellUtils.runCommand(context, "settings put $otherType $key $value")
                }
                Log.d(TAG, "Executed setting put via Shell: [$type] $key = $value (success=$shellSuccess)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to write setting via Shell: [$type] $key = $value", e)
            }
        }

        return resolverSuccess || shellSuccess
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

    suspend fun applyShutUpSettings(
        context: Context,
        config: ShutUpAppConfig,
        repository: SettingsRepository? = null,
        reinforcement: Boolean = false
    ) = settingsMutex.withLock {
        Log.d(TAG, "Applying ShutUp settings for ${config.packageName}")
        withContext(Dispatchers.IO) {
            val repo = repository ?: SettingsRepository(context)
            val currentBackup = repo.getShutUpOriginalSettings()
            val originalSettings = currentBackup.toMutableMap()

            // Snapshot every original value before changing any setting. Re-enforcement never
            // creates or changes the snapshot.
            if (!reinforcement) {
                config.settings.forEach { setting ->
                    if (setting.enabled) {
                        val resolvedType = if (setting.key == "accessibility_enabled") "SECURE" else setting.settingType
                        val prefixedKey = "${resolvedType.lowercase()}:${setting.key}"
                        if (!originalSettings.containsKey(prefixedKey)) {
                            originalSettings[prefixedKey] = safeReadSetting(context, resolvedType, setting.key) ?: ""
                        }
                    }
                }

                val disableAccessibility = config.settings.any { it.key == "accessibility_enabled" && it.enabled }
                if (disableAccessibility) {
                    val prefixedAccKey = "secure:${Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES}"
                    if (!originalSettings.containsKey(prefixedAccKey)) {
                        originalSettings[prefixedAccKey] = safeReadSetting(
                            context,
                            "SECURE",
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                        ) ?: ""
                    }
                }

                if (originalSettings != currentBackup) {
                    repo.saveShutUpOriginalSettings(originalSettings)
                }
            }

            config.settings.forEach { setting ->
                if (setting.enabled) {
                    val resolvedType = if (setting.key == "accessibility_enabled") "SECURE" else setting.settingType
                    safeWriteSettingSync(context, resolvedType, setting.key, setting.valueOnLaunch)
                }
            }

            // Special handling for accessibility services
            val disableAccessibility = config.settings.any { it.key == "accessibility_enabled" && it.enabled }
            if (disableAccessibility) {
                safeWriteSettingSync(context, "SECURE", Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "")
            }
        }
    }

    suspend fun revertShutUpSettings(context: Context, config: ShutUpAppConfig) = settingsMutex.withLock {
        Log.d(TAG, "Reverting ShutUp settings for ${config.packageName}")
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
        settingsMutex.withLock {
            val originalSettings = repository.getShutUpOriginalSettings()
            if (originalSettings.isEmpty()) {
                Log.d(TAG, "No original settings to restore (backup empty)")
                return@withLock
            }

            Log.d(TAG, "Restoring original settings from backup (${originalSettings.size} entries)")
            withContext(Dispatchers.IO) {
                var restoreSucceeded = true
                originalSettings.forEach { (prefixedKey, value) ->
                    try {
                        val parts = prefixedKey.split(":", limit = 2)
                        if (parts.size < 2) return@forEach
                        val table = parts[0]
                        val key = parts[1]
                        restoreSucceeded = safeWriteSettingSync(context, table, key, value) && restoreSucceeded
                        Log.d(TAG, "Restored $prefixedKey = $value")
                    } catch (e: Exception) {
                        restoreSucceeded = false
                        Log.e(TAG, "Failed to restore setting $prefixedKey", e)
                    }
                }

                if (restoreSucceeded) repository.saveShutUpOriginalSettings(emptyMap())
            }

            if (repository.getShutUpOriginalSettings().isEmpty()) withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.shut_up_toast_restored),
                    Toast.LENGTH_SHORT
                ).show()
            }
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
