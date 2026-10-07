package com.sameerasw.essentials.utils

import android.content.Context
import android.provider.Settings
import com.sameerasw.essentials.data.repository.SettingsRepository

object HiddenDebuggingUtils {
    enum class DetectResult { HIDDEN, NOT_HIDDEN, INCONCLUSIVE, SHELL_UNAVAILABLE }

    fun autoDetect(context: Context): DetectResult {
        if (!ShellUtils.isAvailable(context) || !ShellUtils.hasPermission(context)) return DetectResult.SHELL_UNAVAILABLE
        var anyEnabled = false
        for (key in listOf(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, Settings.Global.ADB_ENABLED)) {
            val shell = ShellUtils.runCommandWithOutput(context, "settings get global $key", notifyOnError = false)?.trim()
            val normal =
                try {
                    Settings.Global.getInt(context.contentResolver, key, 0)
                } catch (_: Exception) {
                    0
                }
            if (shell == "1") {
                anyEnabled = true
                if (normal == 0) return DetectResult.HIDDEN
            }
        }
        return if (anyEnabled) DetectResult.NOT_HIDDEN else DetectResult.INCONCLUSIVE
    }

    fun isSupportEnabled(context: Context): Boolean =
        SettingsRepository(context).getBoolean(SettingsRepository.KEY_HIDDEN_DEBUGGING_SUPPORT, false)

    fun isDevOptionsEnabled(context: Context): Boolean = readGlobal(context, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED)

    fun isUsbDebuggingEnabled(context: Context): Boolean = readGlobal(context, Settings.Global.ADB_ENABLED)

    private fun readGlobal(
        context: Context,
        key: String,
    ): Boolean {
        if (isSupportEnabled(context) && ShellUtils.isAvailable(context) && ShellUtils.hasPermission(context)) {
            val output = ShellUtils.runCommandWithOutput(context, "settings get global $key", notifyOnError = false)
            if (output != null) return output.trim() == "1"
        }
        return try {
            Settings.Global.getInt(context.contentResolver, key, 0) == 1
        } catch (_: Exception) {
            false
        }
    }
}
