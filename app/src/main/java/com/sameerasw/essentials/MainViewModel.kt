package com.sameerasw.essentials

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.sameerasw.essentials.utils.HapticFeedbackType

class MainViewModel : ViewModel() {
    val isAccessibilityEnabled = mutableStateOf(false)
    val isWidgetEnabled = mutableStateOf(false)
    val isStatusBarIconControlEnabled = mutableStateOf(false)
    val isWriteSecureSettingsEnabled = mutableStateOf(false)
    val isReadPhoneStateEnabled = mutableStateOf(false)
    val isPostNotificationsEnabled = mutableStateOf(false)
    val isCaffeinateActive = mutableStateOf(false)
    val hapticFeedbackType = mutableStateOf(HapticFeedbackType.SUBTLE)

    fun check(context: Context) {
        isAccessibilityEnabled.value = isAccessibilityServiceEnabled(context)
        isWriteSecureSettingsEnabled.value = canWriteSecureSettings(context)
        isReadPhoneStateEnabled.value = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        isPostNotificationsEnabled.value = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        isWidgetEnabled.value = prefs.getBoolean("widget_enabled", false)
        isStatusBarIconControlEnabled.value = prefs.getBoolean("status_bar_icon_control_enabled", false)
        loadHapticFeedback(context)
        checkCaffeinateActive(context)
    }

    fun setWidgetEnabled(enabled: Boolean, context: Context) {
        isWidgetEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("widget_enabled", enabled)
        }
    }

    fun setStatusBarIconControlEnabled(enabled: Boolean, context: Context) {
        isStatusBarIconControlEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("status_bar_icon_control_enabled", enabled)
        }
    }

    fun setHapticFeedback(type: HapticFeedbackType, context: Context) {
        hapticFeedbackType.value = type
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putString("haptic_feedback_type", type.name)
        }
    }

    private fun loadHapticFeedback(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val typeName = prefs.getString("haptic_feedback_type", HapticFeedbackType.SUBTLE.name)
        hapticFeedbackType.value = try {
            HapticFeedbackType.valueOf(typeName ?: HapticFeedbackType.SUBTLE.name)
        } catch (e: Exception) {
            HapticFeedbackType.SUBTLE
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val serviceName = "${context.packageName}/${ScreenOffAccessibilityService::class.java.name}"
        return enabledServices?.contains(serviceName) == true
    }

    fun canWriteSecureSettings(context: Context): Boolean {
        return try {
            // Try to write to the setting to test if we have permission
            val currentValue = Settings.Secure.getString(
                context.contentResolver,
                "icon_blacklist"
            )
            // Try to write the same value back (no-op) to verify permission
            Settings.Secure.putString(
                context.contentResolver,
                "icon_blacklist",
                currentValue ?: ""
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun checkCaffeinateActive(context: Context) {
        isCaffeinateActive.value = isCaffeinateServiceRunning(context)
    }

    fun startCaffeinate(context: Context) {
        context.startService(android.content.Intent(context, CaffeinateWakeLockService::class.java))
        isCaffeinateActive.value = true
    }

    fun stopCaffeinate(context: Context) {
        context.stopService(android.content.Intent(context, CaffeinateWakeLockService::class.java))
        isCaffeinateActive.value = false
    }

    private fun isCaffeinateServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (CaffeinateWakeLockService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
