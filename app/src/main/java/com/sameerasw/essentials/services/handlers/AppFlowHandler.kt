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
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppFlowHandler(
    private val service: AccessibilityService
) {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)

    private val authenticatedPackages = mutableSetOf<String>()

    // App Lock State
    private var lockingPackage: String? = null
    private var lastLockRequestTime: Long = 0

    // App Automation State
    private val activeAppAutomationIds = mutableSetOf<String>()

    // Night Light State
    private var wasNightLightOnBeforeAutoToggle = false
    private var isNightLightAutoToggledOff = false
    private var pendingNLRunnable: Runnable? = null
    private val nlDebounceDelay = 500L

    private val ignoredSystemPackages = listOf(
        "android",
        "com.android.systemui",
        "com.google.android.inputmethod.latin"
    )

    fun onPackageChanged(packageName: String) {
        if (packageName != service.packageName && packageName != lockingPackage) {
            lockingPackage = null
        }

        checkAppLock(packageName)
        checkHighlightNightLight(packageName)
        checkAppAutomations(packageName)
    }

    fun onAuthenticated(packageName: String) {
        authenticatedPackages.add(packageName)
        if (packageName == lockingPackage) {
            lockingPackage = null
        }
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
            // Skip if we already requested a lock for this package very recently
            val now = System.currentTimeMillis()
            if (packageName == lockingPackage && now - lastLockRequestTime < 1500) {
                return
            }

            lockingPackage = packageName
            lastLockRequestTime = now

            Log.d(
                "AppLock",
                "App $packageName is locked and not authenticated. Showing lock screen."
            )
            val intent = Intent().apply {
                component = ComponentName(service, "com.sameerasw.essentials.AppLockActivity")
                putExtra("package_to_lock", packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
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
            Settings.Secure.putInt(
                service.contentResolver,
                "night_display_activated",
                if (enabled) 1 else 0
            )
        } catch (e: Exception) {
            Log.w(
                "NightLight",
                "Failed to set night light: ${e.message}. Ensure WRITE_SECURE_SETTINGS is granted."
            )
        }
    }

    private fun checkAppAutomations(packageName: String) {
        scope.launch {
            val automations = DIYRepository.automations.value
            val appAutomations =
                automations.filter { it.isEnabled && it.type == Automation.Type.APP }

            // Exiting Automations
            // An automation is exiting if it was active, but the new package is NOT in its selected apps list
            val exiting = appAutomations.filter {
                activeAppAutomationIds.contains(it.id) && !it.selectedApps.contains(packageName)
            }

            exiting.forEach { automation ->
                activeAppAutomationIds.remove(automation.id)
                automation.exitAction?.let { action ->
                    CombinedActionExecutor.execute(service, action)
                }
            }

            // Entering Automations
            // An automation is entering if it was NOT active, and the new package IS in its selected apps list
            val entering = appAutomations.filter {
                !activeAppAutomationIds.contains(it.id) && it.selectedApps.contains(packageName)
            }

            entering.forEach { automation ->
                activeAppAutomationIds.add(automation.id)
                automation.entryAction?.let { action ->
                    CombinedActionExecutor.execute(service, action)
                }
            }
        }
    }
}
