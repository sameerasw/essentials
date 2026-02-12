package com.sameerasw.essentials.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

enum class ShizukuStatus { READY, PERMISSION_NEEDED, NOT_RUNNING }

class ShizukuPermissionHelper(private val context: Context) {
    fun isReady() = try {
        Shizuku.pingBinder()
    } catch (e: Exception) {
        false
    }

    fun hasPermission() = try {
        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            context.checkSelfPermission("moe.shizuku.privileged.api.permission.BIND_SHIZUKU_SERVICE") == PackageManager.PERMISSION_GRANTED
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    } catch (e: Exception) {
        false
    }

    fun getStatus() = when {
        isReady() && hasPermission() -> ShizukuStatus.READY
        isReady() -> ShizukuStatus.PERMISSION_NEEDED
        else -> ShizukuStatus.NOT_RUNNING
    }

    fun requestPermission(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            } else {
                Shizuku.requestPermission(0)
                Shizuku.addRequestPermissionResultListener(listener)
            }
        } catch (e: Exception) {
        }
    }

    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.removeRequestPermissionResultListener(listener)
        } catch (e: Exception) {
        }
    }
}
