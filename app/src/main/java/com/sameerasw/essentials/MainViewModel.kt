package com.sameerasw.essentials

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val isAccessibilityEnabled = mutableStateOf(false)
    val isWidgetEnabled = mutableStateOf(false)

    fun check(context: Context) {
        isAccessibilityEnabled.value = isAccessibilityServiceEnabled(context)
        isWidgetEnabled.value = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).getBoolean("widget_enabled", false)
    }

    fun setWidgetEnabled(enabled: Boolean, context: Context) {
        isWidgetEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit().putBoolean("widget_enabled", enabled).apply()
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val serviceName = "${context.packageName}/${ScreenOffAccessibilityService::class.java.name}"
        return enabledServices?.contains(serviceName) == true
    }
}
