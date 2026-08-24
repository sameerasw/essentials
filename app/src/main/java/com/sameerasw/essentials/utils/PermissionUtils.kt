/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core Utilities
 * File: PermissionUtils.kt
 * Description: System permission validation and settings navigation helpers.
 */

package com.sameerasw.essentials.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.services.receivers.SecurityDeviceAdminReceiver
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService

object PermissionUtils {
    /**
     * Executes the is accessibility service enabled operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
        val serviceName = "${context.packageName}/${ScreenOffAccessibilityService::class.java.name}"
        return enabledServices?.contains(serviceName) == true
    }

    /**
     * Executes the can write secure settings operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun canWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /**
     * Executes the has notification listener permission operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun hasNotificationListenerPermission(context: Context): Boolean {
        return try {
            val enabledServices =
                Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners",
                ) ?: return false
            val componentName = ComponentName(context, NotificationListener::class.java)
            enabledServices.contains(componentName.flattenToString())
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Executes the can draw overlays operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun canDrawOverlays(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

    /**
     * Executes the is device admin active operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, SecurityDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    /**
     * Executes the is notification lighting accessibility service enabled operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun isNotificationLightingAccessibilityServiceEnabled(context: Context): Boolean =
        try {
            val enabledServices =
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                )
            val serviceName =
                "${context.packageName}/${ScreenOffAccessibilityService::class.java.name}"
            enabledServices?.contains(serviceName) == true
        } catch (e: Exception) {
            false
        }

    /**
     * Executes the is default browser operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun isDefaultBrowser(context: Context): Boolean =
        try {
            val pm = context.packageManager
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
            val resolveInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.resolveActivity(
                        browserIntent,
                        android.content.pm.PackageManager.ResolveInfoFlags
                            .of(
                                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                                    .toLong(),
                            ),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.resolveActivity(
                        browserIntent,
                        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
                    )
                }
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            false
        }

    /**
     * Executes the open accessibility settings operation.
     *
     * @param context [Context] Target context.
     */
    fun openAccessibilitySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // ACTION_ACCESSIBILITY_DETAILS_SETTINGS / EXTRA_ACCESSIBILITY_COMPONENT_NAME are
                // hidden from the public SDK stub (no compile-time constants), but the platform
                // still honors these literal action/extra strings from third-party callers on API 31+.
                val componentName = ComponentName(context, ScreenOffAccessibilityService::class.java)
                val intent = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
                intent.putExtra("android.provider.extra.ACCESSIBILITY_COMPONENT_NAME", componentName.flattenToString())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // Fall through to the generic accessibility list below.
            }
        }
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback or ignore
        }
    }

    /**
     * Executes the has location permission operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun hasLocationPermission(context: Context): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /**
     * Executes the has background location permission operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun hasBackgroundLocationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    /**
     * Executes the can use full screen intent operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun canUseFullScreenIntent(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.canUseFullScreenIntent()
        } else {
            true
        }

    /**
     * Executes the is keyboard enabled operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun isKeyboardEnabled(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledImeList = imm.enabledInputMethodList
        return enabledImeList.any { it.packageName == context.packageName }
    }

    /**
     * Executes the is keyboard selected operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun isKeyboardSelected(context: Context): Boolean {
        val defaultIme =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD,
            )
        return defaultIme?.startsWith(context.packageName) == true
    }

    /**
     * Executes the can write system settings operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun canWriteSystemSettings(context: Context): Boolean = Settings.System.canWrite(context)

    /**
     * Executes the has bluetooth permission operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun hasBluetoothPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

    /**
     * Executes the has read phone state permission operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun hasReadPhoneStatePermission(context: Context): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun hasCallPermissions(context: Context): Boolean {
        val hasPhoneState =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_PHONE_STATE,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasAnswerCalls =
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ANSWER_PHONE_CALLS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasContacts =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasCallLog =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALL_LOG,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return hasPhoneState && hasAnswerCalls && hasContacts && hasCallLog
    }

    /**
     * Executes the has notification policy access operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun hasNotificationPolicyAccess(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Executes the has read calendar permission operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun hasReadCalendarPermission(context: Context): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /**
     * Executes the open notification policy settings operation.
     *
     * @param context [Context] Target context.
     */
    fun openNotificationPolicySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback
        }
    }

    /**
     * Executes the open write settings operation.
     *
     * @param context [Context] Target context.
     */
    fun openWriteSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * Executes the open overlay settings operation.
     *
     * @param context [Context] Target context.
     */
    fun openOverlaySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * Executes the is post notifications enabled operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun isPostNotificationsEnabled(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    /**
     * Executes the has usage stats permission operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            } else {
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    /**
     * Executes the open usage stats settings operation.
     *
     * @param context [Context] Target context.
     */
    fun openUsageStatsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /**
     * Executes the open device admin settings operation.
     *
     * @param context [Context] Target context.
     */
    fun openDeviceAdminSettings(context: Context) {
        try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            val adminComponent = ComponentName(context, SecurityDeviceAdminReceiver::class.java)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
        }
    }
}
