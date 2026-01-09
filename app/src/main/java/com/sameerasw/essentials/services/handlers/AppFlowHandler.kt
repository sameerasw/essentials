package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import com.sameerasw.essentials.domain.model.AppSelection
import com.google.gson.reflect.TypeToken

class AppFlowHandler(
    private val service: AccessibilityService
) {
    private val handler = Handler(Looper.getMainLooper())
    
    private val authenticatedPackages = mutableSetOf<String>()
    
    // Night Light State
    private var wasNightLightOnBeforeAutoToggle = false
    private var isNightLightAutoToggledOff = false
    private var pendingNLRunnable: Runnable? = null
    private val nlDebounceDelay = 500L
    
    private val ignoredSystemPackages = listOf(
        "android",
    )

    fun onPackageChanged(packageName: String) {
        checkAppLock(packageName)
        checkHighlightNightLight(packageName)
    }

    fun onAuthenticated(packageName: String) {
        authenticatedPackages.add(packageName)
    }

    fun clearAuthenticated() {
        authenticatedPackages.clear()
    }

    private fun checkAppLock(packageName: String) {
        val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("app_lock_enabled", false)
        if (!isEnabled) return

        if (packageName == service.packageName) {
            return
        }

        val json = prefs.getString("app_lock_selected_apps", null)
        val selectedApps: List<AppSelection> = if (json != null) {
            try {
                Gson().fromJson(json, object : TypeToken<List<AppSelection>>() {}.type)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val isLocked = selectedApps.find { it.packageName == packageName }?.isEnabled ?: false
        
        if (isLocked && !authenticatedPackages.contains(packageName)) {
            Log.d("AppLock", "App $packageName is locked and not authenticated. Showing lock screen.")
            val intent = Intent().apply {
                component = ComponentName(service, "com.sameerasw.essentials.AppLockActivity")
                putExtra("package_to_lock", packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            service.startActivity(intent)
        }
    }

    private fun checkHighlightNightLight(packageName: String) {
        pendingNLRunnable?.let { handler.removeCallbacks(it) }

        if (ignoredSystemPackages.contains(packageName)) {
            Log.d("NightLight", "Ignoring system package $packageName")
            return
        }

        val runnable = Runnable {
            processNightLightChange(packageName)
        }
        pendingNLRunnable = runnable
        handler.postDelayed(runnable, nlDebounceDelay)
    }

    private fun processNightLightChange(packageName: String) {
        val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("dynamic_night_light_enabled", false)
        if (!isEnabled) return

        val json = prefs.getString("dynamic_night_light_selected_apps", null)
        val selectedApps: List<AppSelection> = if (json != null) {
            try {
                Gson().fromJson(json, object : TypeToken<List<AppSelection>>() {}.type)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val isAppSelected = selectedApps.find { it.packageName == packageName }?.isEnabled ?: false
        val isNLCurrentlyOn = isNightLightEnabled()

        if (isAppSelected) {
            if (isNLCurrentlyOn) {
                Log.d("NightLight", "Turning off night light for $packageName")
                wasNightLightOnBeforeAutoToggle = true
                isNightLightAutoToggledOff = true
                setNightLightEnabled(false)
            }
        } else {
            if (isNightLightAutoToggledOff && wasNightLightOnBeforeAutoToggle) {
                Log.d("NightLight", "Restoring night light (was turned off for previous app)")
                setNightLightEnabled(true)
                isNightLightAutoToggledOff = false
                wasNightLightOnBeforeAutoToggle = false
            } else if (isNightLightAutoToggledOff) {
                isNightLightAutoToggledOff = false
            }
        }
    }

    private fun isNightLightEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(service.contentResolver, "night_display_activated", 0) == 1
        } catch (_: Exception) {
            false
        }
    }

    private fun setNightLightEnabled(enabled: Boolean) {
        try {
            Settings.Secure.putInt(service.contentResolver, "night_display_activated", if (enabled) 1 else 0)
        } catch (e: Exception) {
            Log.w("NightLight", "Failed to set night light: ${e.message}. Ensure WRITE_SECURE_SETTINGS is granted.")
        }
    }
}
