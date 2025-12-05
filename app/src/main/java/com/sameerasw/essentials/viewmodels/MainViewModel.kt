package com.sameerasw.essentials.viewmodels

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.sameerasw.essentials.MapsState
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.services.CaffeinateWakeLockService
import com.sameerasw.essentials.services.ScreenOffAccessibilityService
import com.sameerasw.essentials.utils.HapticFeedbackType
import com.sameerasw.essentials.utils.ShizukuUtils
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.utils.AppUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.services.EdgeLightingService

class MainViewModel : ViewModel() {
    val isAccessibilityEnabled = mutableStateOf(false)
    val isWidgetEnabled = mutableStateOf(false)
    val isStatusBarIconControlEnabled = mutableStateOf(false)
    val isWriteSecureSettingsEnabled = mutableStateOf(false)
    val isReadPhoneStateEnabled = mutableStateOf(false)
    val isPostNotificationsEnabled = mutableStateOf(false)
    val isCaffeinateActive = mutableStateOf(false)
    val isShizukuPermissionGranted = mutableStateOf(false)
    val isShizukuAvailable = mutableStateOf(false)
    val isNotificationListenerEnabled = mutableStateOf(false)
    val isMapsPowerSavingEnabled = mutableStateOf(false)
    val isEdgeLightingEnabled = mutableStateOf(false)
    val isOverlayPermissionGranted = mutableStateOf(false)
    val isEdgeLightingAccessibilityEnabled = mutableStateOf(false)
    val hapticFeedbackType = mutableStateOf(HapticFeedbackType.SUBTLE)

    fun check(context: Context) {
        isAccessibilityEnabled.value = isAccessibilityServiceEnabled(context)
        isWriteSecureSettingsEnabled.value = canWriteSecureSettings(context)
        isReadPhoneStateEnabled.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        isPostNotificationsEnabled.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        isShizukuAvailable.value = ShizukuUtils.isShizukuAvailable()
        isShizukuPermissionGranted.value = ShizukuUtils.hasPermission()
        isNotificationListenerEnabled.value = hasNotificationListenerPermission(context)
        isOverlayPermissionGranted.value = canDrawOverlays(context)
        isEdgeLightingAccessibilityEnabled.value = isEdgeLightingAccessibilityServiceEnabled(context)
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        isWidgetEnabled.value = prefs.getBoolean("widget_enabled", false)
        isStatusBarIconControlEnabled.value = prefs.getBoolean("status_bar_icon_control_enabled", false)
        isMapsPowerSavingEnabled.value = prefs.getBoolean("maps_power_saving_enabled", false)
        isEdgeLightingEnabled.value = prefs.getBoolean("edge_lighting_enabled", false)
        MapsState.isEnabled = isMapsPowerSavingEnabled.value
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

    fun setMapsPowerSavingEnabled(enabled: Boolean, context: Context) {
        isMapsPowerSavingEnabled.value = enabled
        MapsState.isEnabled = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("maps_power_saving_enabled", enabled)
        }
    }

    fun setEdgeLightingEnabled(enabled: Boolean, context: Context) {
        isEdgeLightingEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("edge_lighting_enabled", enabled)
        }
    }

    // Helper to show the overlay service for testing/triggering
    fun triggerEdgeLighting(context: Context) {
        val radius = loadEdgeLightingCornerRadius(context)
        val thickness = loadEdgeLightingStrokeThickness(context)
        try {
            val intent = Intent(context, com.sameerasw.essentials.services.EdgeLightingService::class.java).apply {
                putExtra("corner_radius_dp", radius)
                putExtra("stroke_thickness_dp", thickness)
            }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    // Helper to show the overlay service with custom corner radius
    fun triggerEdgeLightingWithRadius(context: Context, cornerRadiusDp: Int) {
        try {
            val intent = Intent(context, com.sameerasw.essentials.services.EdgeLightingService::class.java).apply {
                putExtra("corner_radius_dp", cornerRadiusDp)
                putExtra("is_preview", true)
            }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    // Helper to show the overlay service with custom corner radius and stroke thickness
    fun triggerEdgeLightingWithRadiusAndThickness(context: Context, cornerRadiusDp: Int, strokeThicknessDp: Int) {
        try {
            val intent = Intent(context, com.sameerasw.essentials.services.EdgeLightingService::class.java).apply {
                putExtra("corner_radius_dp", cornerRadiusDp)
                putExtra("stroke_thickness_dp", strokeThicknessDp)
                putExtra("is_preview", true)
            }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    // Helper to remove preview overlay
    fun removePreviewOverlay(context: Context) {
        try {
            // Remove from EdgeLightingService
            val intent1 = Intent(context, EdgeLightingService::class.java).apply {
                putExtra("remove_preview", true)
            }
            context.startService(intent1)

            // Also remove from ScreenOffAccessibilityService if it's running
            val intent2 = Intent(context, ScreenOffAccessibilityService::class.java).apply {
                action = "SHOW_EDGE_LIGHTING"
                putExtra("remove_preview", true)
            }
            context.startService(intent2)
        } catch (e: Exception) {
            // ignore
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

    fun requestReadPhoneStatePermission(activity: androidx.activity.ComponentActivity) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_PHONE_STATE),
            1001
        )
    }

    private fun hasNotificationListenerPermission(context: Context): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val componentName = ComponentName(context, NotificationListener::class.java)
            enabledServices.contains(componentName.flattenToString())
        } catch (_: Exception) {
            false
        }
    }

    fun requestNotificationListenerPermission(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun requestShizukuPermission() {
        ShizukuUtils.requestPermission()
    }

    fun grantWriteSecureSettingsWithShizuku(context: Context): Boolean {
        val success = ShizukuUtils.grantWriteSecureSettingsPermission()
        if (success) {
            // Refresh the write secure settings check
            isWriteSecureSettingsEnabled.value = canWriteSecureSettings(context)
        }
        return success
    }

    fun checkCaffeinateActive(context: Context) {
        isCaffeinateActive.value = isCaffeinateServiceRunning(context)
    }

    fun startCaffeinate(context: Context) {
        context.startService(Intent(context, CaffeinateWakeLockService::class.java))
        isCaffeinateActive.value = true
    }

    fun stopCaffeinate(context: Context) {
        context.stopService(Intent(context, CaffeinateWakeLockService::class.java))
        isCaffeinateActive.value = false
    }

    private fun isCaffeinateServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (CaffeinateWakeLockService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun canDrawOverlays(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun isEdgeLightingAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val serviceName = "${context.packageName}/${ScreenOffAccessibilityService::class.java.name}"
            enabledServices?.contains(serviceName) == true
        } catch (e: Exception) {
            false
        }
    }

    // Edge Lighting App Selection Methods
    fun saveEdgeLightingSelectedApps(context: Context, apps: List<NotificationApp>) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val selections = apps.map { AppSelection(it.packageName, it.isEnabled) }
        val gson = Gson()
        val json = gson.toJson(selections)
        prefs.edit().putString("edge_lighting_selected_apps", json).apply()
    }

    fun loadEdgeLightingSelectedApps(context: Context): List<AppSelection> {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("edge_lighting_selected_apps", null)
        return if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<List<AppSelection>>() {}.type
            try {
                val selections: List<AppSelection> = gson.fromJson(json, type)
                selections
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun updateEdgeLightingAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        val currentSelections = loadEdgeLightingSelectedApps(context).toMutableList()
        val selectionIndex = currentSelections.indexOfFirst { it.packageName == packageName }
        if (selectionIndex != -1) {
            currentSelections[selectionIndex] = currentSelections[selectionIndex].copy(isEnabled = enabled)
        } else {
            // Add new selection if not found
            currentSelections.add(AppSelection(packageName, enabled))
        }
        val gson = Gson()
        val json = gson.toJson(currentSelections)
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("edge_lighting_selected_apps", json)
            .apply()
    }

    // Edge Lighting Corner Radius Methods
    fun saveEdgeLightingCornerRadius(context: Context, radiusDp: Int) {
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putInt("edge_lighting_corner_radius", radiusDp)
        }
    }

    fun loadEdgeLightingCornerRadius(context: Context): Int {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("edge_lighting_corner_radius", 20) // Default to 20 dp
    }

    // Edge Lighting Stroke Thickness Methods
    fun saveEdgeLightingStrokeThickness(context: Context, thicknessDp: Int) {
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putInt("edge_lighting_stroke_thickness", thicknessDp)
        }
    }

    fun loadEdgeLightingStrokeThickness(context: Context): Int {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("edge_lighting_stroke_thickness", 8) // Default to 8 dp
    }
}