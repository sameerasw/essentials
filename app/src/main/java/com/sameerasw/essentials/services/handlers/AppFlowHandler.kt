package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.media.AudioManager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import android.view.inputmethod.InputMethodManager
import com.google.gson.Gson
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.AppRefreshRateConfig
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.utils.FreezeManager
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.utils.StatusBarManager
import com.sameerasw.essentials.utils.RefreshRateUtils
import com.sameerasw.essentials.utils.ShutUpManager
import com.sameerasw.essentials.domain.model.ShutUpAppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppFlowHandler(
    private val context: Context,
    private val service: AccessibilityService? = null
) {
    private val handler = Handler(Looper.getMainLooper())
    private var lastOrientation = context.resources.configuration.orientation
    private val componentCallbacks = object : android.content.ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) {
            val newOrientation = newConfig.orientation
            if (newOrientation != lastOrientation) {
                lastOrientation = newOrientation
                val currentPkg = currentPackage
                if (currentPkg != null) {
                    checkPerAppRefreshRate(currentPkg)
                }
            }
        }
        override fun onLowMemory() {}
        override fun onTrimMemory(level: Int) {}
    }

    private val mediaReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.sameerasw.essentials.MEDIA_PLAYBACK_CHANGED") {
                val pkg = intent.getStringExtra("package_name")
                if (pkg != null && pkg == currentPackage) {
                    checkPerAppRefreshRate(pkg)
                }
            }
        }
    }

    init {
        context.registerComponentCallbacks(componentCallbacks)
        val filter = IntentFilter("com.sameerasw.essentials.MEDIA_PLAYBACK_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(mediaReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(mediaReceiver, filter)
        }
    }

    fun destroy() {
        try {
            context.unregisterComponentCallbacks(componentCallbacks)
        } catch (_: Exception) {}
        try {
            context.unregisterReceiver(mediaReceiver)
        } catch (_: Exception) {}
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val authenticatedPackages = mutableSetOf<String>()
    private val lastLeaveTimes = mutableMapOf<String, Long>()



    // App Lock State
    private var lockingPackage: String? = null
    private var lastLockRequestTime: Long = 0
    var currentPackage: String? = null
        private set
    private var currentUsageStatsPackage: String? = null

    // Per-App Refresh Rate State
    private var perAppRateSnapshot: RefreshRateUtils.RefreshRateState? = null
    private var perAppCurrentPackage: String? = null
    private var pendingRateRunnable: Runnable? = null
    private var pendingRestoreRunnable: Runnable? = null

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
        "com.google.android.inputmethod.latin",
        "com.google.android.gms"
    )

    private fun isIgnoredPackage(packageName: String): Boolean {
        if (ignoredSystemPackages.contains(packageName)) return true
        
        val lowerPkg = packageName.lowercase()
        if (lowerPkg.contains("systemui") ||
            lowerPkg.contains("keyguard") ||
            lowerPkg.contains("volume") ||
            lowerPkg.contains("soundassistant") ||
            lowerPkg.contains("dialer") ||
            lowerPkg.contains("telecom") ||
            lowerPkg.contains("phone") ||
            lowerPkg.contains("incallui") ||
            lowerPkg.contains("packageinstaller") ||
            lowerPkg.contains("permissioncontroller")
        ) {
            return true
        }

        // Check active call state via AudioManager mode
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            val mode = audioManager.mode
            if (mode == AudioManager.MODE_IN_CALL ||
                mode == AudioManager.MODE_IN_COMMUNICATION ||
                mode == AudioManager.MODE_RINGTONE
            ) {
                return true
            }
        }

        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val ims = imm?.enabledInputMethodList
            ims?.any { it.packageName == packageName } == true
        } catch (_: Exception) {
            false
        }
    }

    fun onPackageChanged(packageName: String, isFromUsageStats: Boolean = false) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val useUsageAccess = prefs.getBoolean("use_usage_access", false)

        Log.d("AppFlowHandler", "onPackageChanged: packageName=$packageName, isFromUsageStats=$isFromUsageStats, useUsageAccess=$useUsageAccess, currentPackage=$currentPackage")

        // If the new foreground window belongs to a system overlay (status bar, quick settings,
        // notifications), a keyboard (IME), a volume dialog, or a phone call, completely ignore it.
        // We do NOT update currentPackage so that state-dependent features remain stable.
        if (isIgnoredPackage(packageName)) {
            Log.d("AppFlowHandler", "onPackageChanged: Ignoring system/IME/volume/call package $packageName")
            return
        }
        val oldPackage = currentPackage
        if (isFromUsageStats == useUsageAccess) {
            currentPackage = packageName
            if (oldPackage != null && oldPackage != packageName) {
                lastLeaveTimes[oldPackage] = System.currentTimeMillis()
            }
            if (packageName != context.packageName && packageName != lockingPackage) {
                lockingPackage = null
            }
            Log.d("AppFlowHandler", "onPackageChanged: Processing package change because isFromUsageStats matches useUsageAccess")
            checkAppLock(packageName)
            checkHighlightNightLight(packageName)
            checkAppAutomations(packageName)
            checkGestureBarAutomation(packageName)
            checkPerAppRefreshRate(packageName)
            checkShutUp(packageName)
        }
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

    private fun checkShutUp(packageName: String) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val serviceEnabled = prefs.getBoolean("shutup_service_enabled", false)
        if (!serviceEnabled) return

        val json = prefs.getString("shut_up_selected_apps", null) ?: return
        val configs: List<ShutUpAppConfig> = try {
            Gson().fromJson(json, Array<ShutUpAppConfig>::class.java).toList()
        } catch (_: Exception) {
            return
        }

        val config = configs.find { it.packageName == packageName && it.isEnabled } ?: return

        scope.launch(Dispatchers.IO) {
            Log.d("AppFlowHandler", "checkShutUp: Immediately applying ShutUp settings for $packageName via accessibility event")
            ShutUpManager.applyShutUpSettings(context, config)
        }
    }

    private fun checkAppLock(packageName: String) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("app_lock_enabled", false)
        if (!isEnabled) return

        if (packageName == context.packageName) {
            return
        }

        val json = prefs.getString("app_lock_selected_apps", null)
        val selectedApps: List<AppSelection> = if (json != null) {
            try {
                Gson().fromJson(json, Array<AppSelection>::class.java).toList()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val isLocked = selectedApps.find { it.packageName == packageName }?.isEnabled ?: false

        if (isLocked && authenticatedPackages.contains(packageName)) {
            val delayIndex = prefs.getInt("app_lock_auto_lock_delay_index", 0)
            if (delayIndex > 0) {
                val delayMinutes = when (delayIndex) {
                    1 -> 1
                    2 -> 5
                    3 -> 10
                    4 -> 20
                    5 -> 30
                    else -> 0
                }

                val lastLeaveTime = lastLeaveTimes[packageName] ?: 0L
                if (lastLeaveTime > 0) {
                    val now = System.currentTimeMillis()
                    if (now - lastLeaveTime > delayMinutes * 60 * 1000L) {
                        authenticatedPackages.remove(packageName)
                        lastLeaveTimes.remove(packageName)
                    }
                }
            }
        }

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
                component = ComponentName(context, "com.sameerasw.essentials.AppLockActivity")
                putExtra("package_to_lock", packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            context.startActivity(intent)
        }
    }

    private fun checkHighlightNightLight(packageName: String) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("dynamic_night_light_enabled", false)
        if (!isEnabled) return

        pendingNLRunnable?.let { handler.removeCallbacks(it) }

        if (isIgnoredPackage(packageName)) {
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
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)

        val json = prefs.getString("dynamic_night_light_selected_apps", null)
        val selectedApps: List<AppSelection> = if (json != null) {
            try {
                Gson().fromJson(json, Array<AppSelection>::class.java).toList()
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
            Settings.Secure.getInt(context.contentResolver, "night_display_activated", 0) == 1
        } catch (_: Exception) {
            false
        }
    }

    private fun setNightLightEnabled(enabled: Boolean) {
        try {
            Settings.Secure.putInt(
                context.contentResolver,
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
        if (isIgnoredPackage(packageName)) {
            Log.d("AppFlowHandler", "checkAppAutomations: Ignoring system/IME package $packageName")
            return
        }
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
                    CombinedActionExecutor.execute(context, action)
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
                    CombinedActionExecutor.execute(context, action)
                }
            }
        }
    }

    fun isCameraApp(packageName: String? = currentPackage): Boolean {
        if (packageName == null) return false

        // Known camera packages
        val cameraPackages = listOf(
            "com.google.android.GoogleCamera",
            "com.android.camera",
            "com.sec.android.app.camera",
            "com.huawei.camera",
            "com.oneplus.camera",
            "com.oppo.camera",
            "com.miui.camera",
            "com.sonyericsson.android.camera",
            "com.sonymobile.android.camera"
        )
        if (cameraPackages.any { packageName.startsWith(it) }) return true

        if (packageName.lowercase().contains("camera")) return true

        return false
    }

    private fun checkGestureBarAutomation(packageName: String) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("hide_gesture_bar_on_launcher_enabled", false)
        if (!isEnabled) return

        if (isLauncher(packageName)) {
            StatusBarManager.requestRestore(context, "GestureBarAutomation")
        } else {
            StatusBarManager.requestDisable(
                context,
                "GestureBarAutomation",
                setOf(StatusBarManager.FLAG_HOME)
            )
        }
    }

    private fun isLauncher(packageName: String): Boolean {
        if (packageName == "com.android.systemui") return true

        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo =
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val defaultLauncher = resolveInfo?.activityInfo?.packageName

        if (packageName == defaultLauncher) return true

        // Secondary check for other launchers if not default
        val launchers =
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return launchers.any { it.activityInfo.packageName == packageName }
    }



    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = android.app.NotificationChannel(
                "app_detection_service_channel",
                context.getString(com.sameerasw.essentials.R.string.app_detection_service_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for app detection alerts"
            }
            notificationManager.createNotificationChannel(channel)

        }
    }

    private fun isMediaPlaying(packageName: String): Boolean {
        return try {
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? android.media.session.MediaSessionManager
            val componentName = android.content.ComponentName(context, NotificationListener::class.java)
            val sessions = msm?.getActiveSessions(componentName)
            sessions?.any {
                it.packageName == packageName &&
                it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun getTargetRefreshRateForConfig(config: AppRefreshRateConfig): Float {
        val landscapeRate = config.landscapeRefreshRate
        if (landscapeRate != null) {
            val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape) {
                if (config.onlyOnMediaPlaying) {
                    return if (isMediaPlaying(config.packageName)) landscapeRate else config.refreshRate
                }
                return landscapeRate
            }
        }
        return config.refreshRate
    }

    private fun checkPerAppRefreshRate(packageName: String) {
        if (ignoredSystemPackages.contains(packageName)) {
            return
        }

        val settingsRepository = SettingsRepository(context)
        val isEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_PER_APP_REFRESH_RATE_ENABLED, false)
        if (!isEnabled) {
            cancelPendingRateRunnable()
            cancelPendingRestoreRunnable()
            if (perAppRateSnapshot != null) {
                restoreFromSnapshot()
            }
            return
        }

        val configs = settingsRepository.loadPerAppRefreshRateConfigs()
        val config = configs.find { it.packageName == packageName && it.isEnabled }

        if (config != null) {
            cancelPendingRestoreRunnable()
            if (perAppRateSnapshot == null) {
                perAppRateSnapshot = RefreshRateUtils.getCurrentState(context)
                Log.d("AppFlowHandler", "per-app refresh rate: snapshotted state: $perAppRateSnapshot")
            }
            perAppCurrentPackage = packageName
            val targetRate = getTargetRefreshRateForConfig(config)
            Log.d("AppFlowHandler", "per-app refresh rate: applying $targetRate Hz (isFixed=${config.isFixed}) for $packageName")
            if (config.isFixed) {
                RefreshRateUtils.applyFixedRefreshRate(context, targetRate)
            } else {
                RefreshRateUtils.applyDynamicRefreshRate(context, targetRate)
            }

            // Re-apply after a short delay to beat OEM adaptive display controllers that
            // fire asynchronously after window transitions (e.g. resuming from recents).
            cancelPendingRateRunnable()
            val runnable = Runnable {
                if (perAppCurrentPackage == packageName) {
                    val delayedRate = getTargetRefreshRateForConfig(config)
                    Log.d("AppFlowHandler", "per-app refresh rate: delayed re-apply $delayedRate Hz (isFixed=${config.isFixed}) for $packageName")
                    if (config.isFixed) {
                        RefreshRateUtils.applyFixedRefreshRate(context, delayedRate)
                    } else {
                        RefreshRateUtils.applyDynamicRefreshRate(context, delayedRate)
                    }
                }
            }
            pendingRateRunnable = runnable
            handler.postDelayed(runnable, 400L)
        } else {
            cancelPendingRateRunnable()
            perAppCurrentPackage = null
            if (perAppRateSnapshot != null && pendingRestoreRunnable == null) {
                Log.d("AppFlowHandler", "per-app refresh rate: scheduling delayed restoration (1000ms) for leaving $packageName")
                val runnable = Runnable {
                    if (perAppCurrentPackage == null && perAppRateSnapshot != null) {
                        Log.d("AppFlowHandler", "per-app refresh rate: restoring to global state from snapshot (delayed)")
                        restoreFromSnapshot()
                    }
                    pendingRestoreRunnable = null
                }
                pendingRestoreRunnable = runnable
                handler.postDelayed(runnable, 1000L)
            }
        }
    }

    private fun cancelPendingRateRunnable() {
        pendingRateRunnable?.let { handler.removeCallbacks(it) }
        pendingRateRunnable = null
    }

    private fun cancelPendingRestoreRunnable() {
        pendingRestoreRunnable?.let { handler.removeCallbacks(it) }
        pendingRestoreRunnable = null
    }

    private fun restoreFromSnapshot() {
        val snapshot = perAppRateSnapshot ?: return
        try {
            if (snapshot.isSystemManaged) {
                RefreshRateUtils.resetRefreshRate(context, snapshot.usesInfinityDefaultPeak)
            } else if (snapshot.min > 0f && snapshot.peak > 0f && snapshot.min != snapshot.peak) {
                RefreshRateUtils.applyRangeRefreshRate(context, snapshot.min, snapshot.peak)
            } else {
                RefreshRateUtils.applyFixedRefreshRate(context, snapshot.peak.coerceAtLeast(snapshot.min))
            }
        } catch (e: Exception) {
            Log.e("AppFlowHandler", "Failed to restore refresh rate from snapshot", e)
        } finally {
            perAppRateSnapshot = null
        }
    }

    companion object {
    }
}
