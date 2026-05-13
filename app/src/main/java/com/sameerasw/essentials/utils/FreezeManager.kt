package com.sameerasw.essentials.utils

import android.content.Context
import android.os.Build
import android.os.IBinder
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object FreezeManager {
    private const val TAG = "FreezeManager"

    // Hidden API constants
    private const val COMPONENT_ENABLED_STATE_DEFAULT = 0
    private const val COMPONENT_ENABLED_STATE_ENABLED = 1
    private const val COMPONENT_ENABLED_STATE_DISABLED = 2
    private const val COMPONENT_ENABLED_STATE_DISABLED_USER = 3
    private const val COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED = 4

    /**
     * Freeze an application using Shizuku or Root.
     * Uses either 'pm disable-user' or 'pm suspend' based on configuration.
     */
    fun freezeApp(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val mode = prefs.getInt("freeze_mode", 0) // 0: FREEZE, 1: SUSPEND

        return if (mode == 1) {
            suspendApp(context, packageName)
        } else {
            setApplicationEnabledSetting(
                context,
                packageName,
                COMPONENT_ENABLED_STATE_DISABLED_USER
            )
        }
    }

    /**
     * Unfreeze an application using Shizuku or Root.
     * Uses either 'pm enable' or 'pm unsuspend' based on configuration.
     */
    fun unfreezeApp(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val mode = prefs.getInt("freeze_mode", 0)

        return if (mode == 1) {
            unsuspendApp(context, packageName)
        } else {
            setApplicationEnabledSetting(context, packageName, COMPONENT_ENABLED_STATE_ENABLED)
        }
    }

    /**
     * Freeze all selected apps from settings that have auto-freeze ENABLED.
     */
    fun freezeAll(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("freeze_selected_apps", null)
        val excludedJson = prefs.getString("freeze_auto_excluded_apps", null)
        val dontFreezeActive = prefs.getBoolean("freeze_dont_freeze_active_apps", false)

        if (json != null) {
            val gson = com.google.gson.Gson()
            try {
                val apps: List<com.sameerasw.essentials.domain.model.AppSelection> =
                    gson.fromJson(
                        json,
                        Array<com.sameerasw.essentials.domain.model.AppSelection>::class.java
                    ).toList()
                val excludedSet: Set<String> = if (excludedJson != null) {
                    gson.fromJson(excludedJson, Array<String>::class.java).toSet()
                } else emptySet()

                apps.forEach { app ->
                    // Freezing happens if it's in the list AND not excluded
                    if (!excludedSet.contains(app.packageName)) {
                        if (!dontFreezeActive || !isAppActive(context, app.packageName)) {
                            freezeApp(context, app.packageName)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isAppActive(context: Context, packageName: String): Boolean {
        // 1. Check Foreground
        if (isAppInForeground(context, packageName)) return true

        // 2. Check Media Sessions
        if (isAppStreamingMedia(context, packageName)) return true

        // 3. Check Ongoing Notifications
        if (hasOngoingNotification(context, packageName)) return true

        return false
    }

    private fun isAppInForeground(context: Context, packageName: String): Boolean {
        try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val time = System.currentTimeMillis()
            val events = usageStatsManager.queryEvents(time - 10 * 1000, time)
            val event = android.app.usage.UsageEvents.Event()
            var lastEvent = -1

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName == packageName) {
                    lastEvent = event.eventType
                }
            }
            return lastEvent == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ||
                    lastEvent == android.app.usage.UsageEvents.Event.USER_INTERACTION
        } catch (_: Exception) {
            return false
        }
    }

    private fun isAppStreamingMedia(context: Context, packageName: String): Boolean {
        try {
            // Check AudioManager first for general music playback
            val audioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            if (!audioManager.isMusicActive) return false

            // Check Media Sessions for specific package
            val mediaSessionManager =
                context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
            val componentName = android.content.ComponentName(
                context,
                "com.sameerasw.essentials.services.NotificationListener"
            )
            val sessions = mediaSessionManager.getActiveSessions(componentName)

            return sessions.any { it.packageName == packageName && it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
        } catch (_: Exception) {
            return false
        }
    }

    private fun hasOngoingNotification(context: Context, packageName: String): Boolean {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val activeNotifications = notificationManager.activeNotifications
            return activeNotifications.any {
                it.packageName == packageName && (it.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
            }
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Freeze EVERY app in the selection list immediately.
     */
    fun freezeAllManual(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("freeze_selected_apps", null)
        if (json != null) {
            val gson = com.google.gson.Gson()
            try {
                val apps: List<com.sameerasw.essentials.domain.model.AppSelection> =
                    gson.fromJson(
                        json,
                        Array<com.sameerasw.essentials.domain.model.AppSelection>::class.java
                    ).toList()
                apps.forEach { app ->
                    freezeApp(context, app.packageName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Unfreeze all picked apps from settings that have auto-freeze ENABLED.
     */
    fun unfreezeAll(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("freeze_selected_apps", null)
        val excludedJson = prefs.getString("freeze_auto_excluded_apps", null)

        if (json != null) {
            val gson = com.google.gson.Gson()
            try {
                val apps: List<com.sameerasw.essentials.domain.model.AppSelection> =
                    gson.fromJson(
                        json,
                        Array<com.sameerasw.essentials.domain.model.AppSelection>::class.java
                    ).toList()
                val excludedSet: Set<String> = if (excludedJson != null) {
                    gson.fromJson(excludedJson, Array<String>::class.java).toSet()
                } else emptySet()

                apps.forEach { app ->
                    if (!excludedSet.contains(app.packageName)) {
                        unfreezeApp(context, app.packageName)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Unfreeze all picked apps in settings, ignoring the auto-freeze toggle.
     */
    fun unfreezeAllManual(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("freeze_selected_apps", null)
        if (json != null) {
            val gson = com.google.gson.Gson()
            try {
                val apps: List<com.sameerasw.essentials.domain.model.AppSelection> =
                    gson.fromJson(
                        json,
                        Array<com.sameerasw.essentials.domain.model.AppSelection>::class.java
                    ).toList()
                apps.forEach { app ->
                    unfreezeApp(context, app.packageName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Check if an application is currently frozen/disabled/suspended.
     */
    fun isAppFrozen(context: Context, packageName: String): Boolean {
        return try {
            val state = context.packageManager.getApplicationEnabledSetting(packageName)
            val isSuspended = context.packageManager.isPackageSuspended(packageName)
            state == COMPONENT_ENABLED_STATE_DISABLED_USER || state == COMPONENT_ENABLED_STATE_DISABLED || isSuspended
        } catch (e: Exception) {
            false
        }
    }

    private fun suspendApp(context: Context, packageName: String): Boolean {
        if (ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()) {
            if (setAppSuspendedWithShizuku(packageName, true)) return true
        }

        if (!ShellUtils.hasPermission(context)) return false
        return try {
            ShellUtils.runCommand(context, "pm suspend --user 0 $packageName")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun unsuspendApp(context: Context, packageName: String): Boolean {
        if (ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()) {
            if (setAppSuspendedWithShizuku(packageName, false)) return true
        }

        if (!ShellUtils.hasPermission(context)) return false
        return try {
            ShellUtils.runCommand(context, "pm unsuspend --user 0 $packageName")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun setAppSuspendedWithShizuku(packageName: String, suspended: Boolean): Boolean {
        return try {
            if (suspended) forceStopAppWithShizuku(packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setAppRestrictedWithShizuku(packageName, suspended)
            }

            val pm =
                getService("package", "android.content.pm.IPackageManager\$Stub") ?: return false
            val callerPackage = getSuspenderPackage()
            val userId = getUserId()

            val dialogInfo = if (suspended) {
                val builderClass = Class.forName("android.content.pm.SuspendDialogInfo\$Builder")
                val builder = HiddenApiBypass.newInstance(builderClass)
                HiddenApiBypass.invoke(
                    builderClass,
                    builder,
                    "setNeutralButtonAction",
                    1 /*BUTTON_ACTION_UNSUSPEND*/
                )
                HiddenApiBypass.invoke(builderClass, builder, "build")
            } else null

            fun callSetPackagesSuspended(version: Int): Array<*>? {
                return try {
                    when (version) {
                        0 -> HiddenApiBypass.invoke(
                            pm.javaClass,
                            pm,
                            "setPackagesSuspendedAsUser",
                            arrayOf(packageName),
                            suspended,
                            null,
                            null,
                            dialogInfo,
                            0,
                            callerPackage,
                            userId,
                            userId
                        ) as? Array<*>

                        1 -> HiddenApiBypass.invoke(
                            pm.javaClass,
                            pm,
                            "setPackagesSuspendedAsUser",
                            arrayOf(packageName),
                            suspended,
                            null,
                            null,
                            dialogInfo,
                            callerPackage,
                            userId
                        ) as? Array<*>

                        2 -> HiddenApiBypass.invoke(
                            pm.javaClass, pm, "setPackagesSuspendedAsUser",
                            arrayOf(packageName), suspended, null, null, null, callerPackage, userId
                        ) as? Array<*>

                        else -> pm.javaClass.getMethod(
                            "setPackagesSuspendedAsUser",
                            Array<String>::class.java,
                            Boolean::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType
                        )
                            .invoke(pm, arrayOf(packageName), suspended, userId) as? Array<*>
                    }
                } catch (_: Exception) {
                    null
                }
            }

            val result = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> callSetPackagesSuspended(
                    0
                ) ?: callSetPackagesSuspended(1)

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> callSetPackagesSuspended(1)
                    ?: callSetPackagesSuspended(2)

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> callSetPackagesSuspended(2)
                else -> callSetPackagesSuspended(3)
            }

            result?.isEmpty() ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun forceStopAppWithShizuku(packageName: String) {
        val am =
            getService(Context.ACTIVITY_SERVICE, "android.app.IActivityManager\$Stub") ?: return
        try {
            HiddenApiBypass.invoke(am.javaClass, am, "forceStopPackage", packageName, getUserId())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setAppRestrictedWithShizuku(packageName: String, restricted: Boolean) {
        val appops =
            getService(Context.APP_OPS_SERVICE, "com.android.internal.app.IAppOpsService\$Stub")
                ?: return
        try {
            val appOpsManagerClass = Class.forName("android.app.AppOpsManager")
            val op = HiddenApiBypass.invoke(
                appOpsManagerClass,
                null,
                "strOpToOp",
                "android:run_any_in_background"
            ) as Int
            val uid = getPackageUid(packageName)
            if (uid != -1) {
                val mode = if (restricted) 1 /*MODE_IGNORED*/ else 0 /*MODE_ALLOWED*/
                HiddenApiBypass.invoke(
                    appops.javaClass,
                    appops,
                    "setMode",
                    op,
                    uid,
                    packageName,
                    mode
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPackageUid(packageName: String): Int {
        val pm = getService("package", "android.content.pm.IPackageManager\$Stub") ?: return -1
        return try {
            HiddenApiBypass.invoke(pm.javaClass, pm, "getPackageUid", packageName, 0, 0) as Int
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private fun getService(serviceName: String, stubClassName: String): Any? {
        return try {
            val binder = SystemServiceHelper.getSystemService(serviceName) ?: return null
            val stubClass = Class.forName(stubClassName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                HiddenApiBypass.invoke(stubClass, null, "asInterface", ShizukuBinderWrapper(binder))
            } else {
                stubClass.getMethod("asInterface", IBinder::class.java)
                    .invoke(null, ShizukuBinderWrapper(binder))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getSuspenderPackage(): String =
        if (Shizuku.getUid() == 0) "com.sameerasw.essentials" else "com.android.shell"

    private fun getUserId(): Int {
        return try {
            val userHandle = android.os.Process.myUserHandle()
            val method = userHandle.javaClass.getMethod("getIdentifier")
            method.invoke(userHandle) as Int
        } catch (_: Exception) {
            0
        }
    }

    private fun setApplicationEnabledSetting(
        context: Context,
        packageName: String,
        newState: Int
    ): Boolean {
        // 1. Try Shizuku first
        if (ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()) {
            try {
                val pm = getService("package", "android.content.pm.IPackageManager\$Stub")
                if (pm != null) {
                    val userId = getUserId()
                    Log.d(
                        "FreezeManager",
                        "Shizuku: setting $packageName to $newState for user $userId"
                    )
                    HiddenApiBypass.invoke(
                        pm.javaClass, pm, "setApplicationEnabledSetting",
                        packageName, newState, 0, userId, "android"
                    )
                    return true
                }
            } catch (e: Exception) {
                Log.e("FreezeManager", "Shizuku call failed", e)
            }
        }

        // 2. Fallback to Shell (Root)
        if (!ShellUtils.hasPermission(context)) return false

        val cmd = when (newState) {
            COMPONENT_ENABLED_STATE_DISABLED_USER -> "pm disable-user --user 0 $packageName"
            COMPONENT_ENABLED_STATE_ENABLED -> "pm enable $packageName"
            else -> return false
        }

        return try {
            ShellUtils.runCommand(context, cmd)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
