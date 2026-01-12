package com.sameerasw.essentials.utils

import android.content.Context
import com.sameerasw.essentials.data.repository.SettingsRepository

object ShellUtils {

    fun isRootEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(SettingsRepository.KEY_USE_ROOT, false)
    }

    fun isAvailable(context: Context): Boolean {
        return if (isRootEnabled(context)) {
            RootUtils.isRootAvailable()
        } else {
            ShizukuUtils.isShizukuAvailable()
        }
    }

    fun hasPermission(context: Context): Boolean {
        return if (isRootEnabled(context)) {
            RootUtils.isRootPermissionGranted()
        } else {
            ShizukuUtils.hasPermission()
        }
    }

    fun runCommand(context: Context, command: String) {
        if (isRootEnabled(context)) {
            RootUtils.runCommand(command)
        } else {
            ShizukuUtils.runCommand(command)
        }
    }

    fun newProcess(context: Context, command: Array<String>): Process? {
        return if (isRootEnabled(context)) {
            RootUtils.newProcess(command)
        } else {
            com.sameerasw.essentials.shizuku.ShizukuProcessHelper.newProcess(command)
        }
    }
}
