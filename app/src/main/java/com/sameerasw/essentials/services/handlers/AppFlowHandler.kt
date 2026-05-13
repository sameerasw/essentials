package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.utils.FreezeManager
import com.sameerasw.essentials.utils.StatusBarManager
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
    private val scope = CoroutineScope(Dispatchers.Main)

    private val authenticatedPackages = mutableSetOf<String>()
    private val lastLeaveTimes = mutableMapOf<String, Long>()
    private val activeCountdowns = mutableMapOf<String, Job>()

    private val shutUpReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
            when (intent.action) {
                ACTION_FREEZE_NOW -> {
                    activeCountdowns[packageName]?.cancel()
                    activeCountdowns.remove(packageName)
                    context?.let { FreezeManager.freezeApp(it, packageName) }
                    cancelNotification(packageName)
                }

                ACTION_ABORT_FREEZE -> {
                    activeCountdowns[packageName]?.cancel()
                    activeCountdowns.remove(packageName)
                    cancelNotification(packageName)
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(ACTION_FREEZE_NOW)
            addAction(ACTION_ABORT_FREEZE)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(shutUpReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(shutUpReceiver, filter)
        }
    }

    // App Lock State
    private var lockingPackage: String? = null
    private var lastLockRequestTime: Long = 0
    var currentPackage: String? = null
        private set

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

    fun onPackageChanged(packageName: String, isFromUsageStats: Boolean = false) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val useUsageAccess = prefs.getBoolean("use_usage_access", false)

        val oldPackage = currentPackage
        currentPackage = packageName

        if (oldPackage != null && oldPackage != packageName) {
            lastLeaveTimes[oldPackage] = System.currentTimeMillis()
        }

        if (packageName != context.packageName && packageName != lockingPackage) {
            lockingPackage = null
        }

        if (isFromUsageStats == useUsageAccess) {
            checkAppLock(packageName)
            checkHighlightNightLight(packageName)
            checkAppAutomations(packageName)
            checkGestureBarAutomation(packageName)
            checkShutUpRestore(oldPackage, packageName)
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

    private fun checkShutUpRestore(oldPackage: String?, newPackage: String?) {
        Log.d("AppFlowHandler", "checkShutUpRestore: old=$oldPackage, new=$newPackage")
        if (oldPackage == null || oldPackage == newPackage) return

        val settingsRepository =
            com.sameerasw.essentials.data.repository.SettingsRepository(context)
        val shutUpConfigs = settingsRepository.loadShutUpConfigs()

        val wasShutUpConfig = shutUpConfigs.find { it.packageName == oldPackage && it.isEnabled }

        // Check if it was already frozen to avoid duplicate triggers (e.g. on screen off)
        val isAlreadyFrozen = oldPackage.let { FreezeManager.isAppFrozen(context, it) }

        // We consider the new app a Shut-Up app if it's in the list OR if it's the shortcut activity
        val isNewAppShutUp = shutUpConfigs.any { it.packageName == newPackage && it.isEnabled } ||
                newPackage == "com.sameerasw.essentials.ShutUpShortcutActivity"

        Log.d(
            "AppFlowHandler",
            "checkShutUpRestore: wasShutUpConfig=${wasShutUpConfig != null}, isNewAppShutUp=$isNewAppShutUp, isAlreadyFrozen=$isAlreadyFrozen"
        )

        // If it's already frozen, we've already handled it
        if (isAlreadyFrozen) return

        // If we are entering a Shut-Up app, cancel ANY pending countdowns for other apps
        if (isNewAppShutUp) {
            if (activeCountdowns.isNotEmpty()) {
                Log.d(
                    "AppFlowHandler",
                    "checkShutUpRestore: Entering Shut-Up app, cancelling all pending countdowns"
                )
                activeCountdowns.values.forEach { it.cancel() }
                activeCountdowns.keys.forEach { cancelNotification(it) }
                activeCountdowns.clear()
            }
        }

        if (wasShutUpConfig != null && !isNewAppShutUp) {
            Log.d("AppFlowHandler", "checkShutUpRestore: Triggering restoration for $oldPackage")
            restoreShutUpSettings(
                settingsRepository,
                if (wasShutUpConfig.autoArchive) wasShutUpConfig.packageName else null
            )
        }
    }

    private fun startAutoArchiveCountdown(packageName: String) {
        Log.d("AppFlowHandler", "startAutoArchiveCountdown: $packageName")
        // Cancel existing countdown for this app if any
        activeCountdowns[packageName]?.cancel()

        val appName = try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.e("AppFlowHandler", "Failed to get app name for $packageName", e)
            packageName
        }

        val job = scope.launch {
            Log.d("AppFlowHandler", "Countdown job started for $packageName")
            for (i in 10 downTo 1) {
                Log.d("AppFlowHandler", "Countdown for $packageName: $i")
                showCountdownNotification(packageName, appName, i)
                delay(1000)
            }
            // countdown finished
            Log.d("AppFlowHandler", "Countdown finished for $packageName, freezing...")
            val success = withContext(Dispatchers.IO) {
                FreezeManager.freezeApp(context, packageName)
            }
            Log.d("AppFlowHandler", "Freeze result for $packageName: $success")
            cancelNotification(packageName)
            activeCountdowns.remove(packageName)
        }
        activeCountdowns[packageName] = job
    }

    private fun showCountdownNotification(packageName: String, appName: String, secondsLeft: Int) {
        createNotificationChannel()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val freezeIntent = Intent(ACTION_FREEZE_NOW).apply {
            `package` = context.packageName
            putExtra(EXTRA_PACKAGE_NAME, packageName)
        }
        val freezePendingIntent = PendingIntent.getBroadcast(
            context,
            packageName.hashCode() + 1,
            freezeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val abortIntent = Intent(ACTION_ABORT_FREEZE).apply {
            `package` = context.packageName
            putExtra(EXTRA_PACKAGE_NAME, packageName)
        }
        val abortPendingIntent = PendingIntent.getBroadcast(
            context,
            packageName.hashCode() + 2,
            abortIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title =
            context.getString(com.sameerasw.essentials.R.string.shut_up_auto_archive_notif_title)
        val text = context.getString(
            com.sameerasw.essentials.R.string.shut_up_auto_archive_notif_text,
            appName,
            secondsLeft
        )
        val criticalText = secondsLeft.toString()

        val notification =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val builder = android.app.Notification.Builder(context, "shutup_alerts_channel")
                    .setSmallIcon(com.sameerasw.essentials.R.drawable.rounded_snowflake_24)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setCategory(android.app.Notification.CATEGORY_SERVICE)
                    .setShowWhen(false)
                    .setGroup("shutup_auto_archive")
                    .setColorized(false)

                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    builder.setForegroundServiceBehavior(android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE)
                }

                builder.addAction(
                    android.app.Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(
                            context,
                            com.sameerasw.essentials.R.drawable.rounded_snowflake_24
                        ),
                        context.getString(com.sameerasw.essentials.R.string.shut_up_auto_archive_action_freeze),
                        freezePendingIntent
                    ).build()
                )
                builder.addAction(
                    android.app.Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(
                            context,
                            com.sameerasw.essentials.R.drawable.rounded_close_24
                        ),
                        context.getString(com.sameerasw.essentials.R.string.shut_up_auto_archive_action_abort),
                        abortPendingIntent
                    ).build()
                )

                // Live Update Status Chip
                try {
                    val setRequestPromotedOngoing = builder.javaClass.getMethod(
                        "setRequestPromotedOngoing",
                        Boolean::class.javaPrimitiveType
                    )
                    setRequestPromotedOngoing.invoke(builder, true)

                    val setShortCriticalText = builder.javaClass.getMethod(
                        "setShortCriticalText",
                        CharSequence::class.java
                    )
                    setShortCriticalText.invoke(builder, criticalText)
                } catch (_: Throwable) {
                }

                val extras = android.os.Bundle()
                extras.putBoolean("android.requestPromotedOngoing", true)
                extras.putString("android.shortCriticalText", criticalText)
                builder.addExtras(extras)

                builder.setProgress(10, secondsLeft, false)

                builder.build()
            } else {
                NotificationCompat.Builder(context, "shutup_alerts_channel")
                    .setSmallIcon(com.sameerasw.essentials.R.drawable.rounded_snowflake_24)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .setProgress(10, secondsLeft, false)
                    .addAction(
                        com.sameerasw.essentials.R.drawable.rounded_snowflake_24,
                        context.getString(com.sameerasw.essentials.R.string.shut_up_auto_archive_action_freeze),
                        freezePendingIntent
                    )
                    .addAction(
                        com.sameerasw.essentials.R.drawable.rounded_close_24,
                        context.getString(com.sameerasw.essentials.R.string.shut_up_auto_archive_action_abort),
                        abortPendingIntent
                    )
                    .addExtras(android.os.Bundle().apply {
                        putBoolean("android.requestPromotedOngoing", true)
                        putString("android.shortCriticalText", criticalText)
                    })
                    .build()
            }

        Log.d("AppFlowHandler", "Showing notification for $packageName, secondsLeft=$secondsLeft")
        notificationManager.notify(packageName.hashCode(), notification)
    }

    private fun cancelNotification(packageName: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(packageName.hashCode())
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

            val alertChannel = android.app.NotificationChannel(
                "shutup_alerts_channel",
                "Shut-Up! Alerts",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Live update notifications for auto archiving"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    private fun restoreShutUpSettings(
        repository: com.sameerasw.essentials.data.repository.SettingsRepository,
        autoArchivePackage: String? = null
    ) {
        val originalSettings = repository.getShutUpOriginalSettings()
        if (originalSettings.isEmpty()) {
            if (autoArchivePackage != null) {
                startAutoArchiveCountdown(autoArchivePackage)
            }
            return
        }

        scope.launch {
            // Delay to ensure the app has fully settled before restoring system settings
            delay(2000)

            val canWriteSecure =
                com.sameerasw.essentials.utils.PermissionUtils.canWriteSecureSettings(context)
            val canWriteSystem = Settings.System.canWrite(context)

            originalSettings.forEach { (prefixedKey, value) ->
                try {
                    val parts = prefixedKey.split(":", limit = 2)
                    if (parts.size < 2) return@forEach

                    val table = parts[0]
                    val key = parts[1]

                    when (table) {
                        "global" -> {
                            if (canWriteSecure) {
                                Settings.Global.putString(context.contentResolver, key, value)
                            }
                        }

                        "secure" -> {
                            if (canWriteSecure) {
                                Settings.Secure.putString(context.contentResolver, key, value)
                            }
                        }

                        "system" -> {
                            if (canWriteSystem) {
                                Settings.System.putString(context.contentResolver, key, value)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AppFlowHandler", "Failed to restore setting $prefixedKey", e)
                }
            }

            // Clear original settings after restoration
            repository.saveShutUpOriginalSettings(emptyMap())

            // Wait a bit and Restart Shizuku as ADB might have been toggled back on
            delay(1000)
            restartShizuku()

            android.widget.Toast.makeText(
                context,
                context.getString(com.sameerasw.essentials.R.string.shut_up_toast_restored),
                android.widget.Toast.LENGTH_SHORT
            ).show()

            // Start auto-archive countdown AFTER everything is restored and Shizuku is starting
            if (autoArchivePackage != null) {
                startAutoArchiveCountdown(autoArchivePackage)
            }
        }
    }

    private fun restartShizuku() {
        try {
            val intent = Intent("moe.shizuku.privileged.api.START").apply {
                `package` = "moe.shizuku.privileged.api"
                putExtra("auth", "y95fuaRb9USHiIg724tvTHIs")
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("AppFlowHandler", "Failed to restart Shizuku", e)
        }
    }

    companion object {
        const val ACTION_FREEZE_NOW = "com.sameerasw.essentials.ACTION_FREEZE_NOW"
        const val ACTION_ABORT_FREEZE = "com.sameerasw.essentials.ACTION_ABORT_FREEZE"
        const val EXTRA_PACKAGE_NAME = "package_name"
    }
}
