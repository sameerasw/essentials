package com.sameerasw.essentials.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.services.ScreenOffAccessibilityService
import com.sameerasw.essentials.services.receivers.SecurityDeviceAdminReceiver

object PermissionUtils {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val serviceName = "${context.packageName}/${ScreenOffAccessibilityService::class.java.name}"
        return enabledServices?.contains(serviceName) == true
    }

    fun canWriteSecureSettings(context: Context): Boolean {
        return try {
            val currentValue = Settings.Secure.getString(
                context.contentResolver,
                "icon_blacklist"
            )
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

    fun hasNotificationListenerPermission(context: Context): Boolean {
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

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, SecurityDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    fun isEdgeLightingAccessibilityServiceEnabled(context: Context): Boolean {
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

    fun isDefaultBrowser(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(browserIntent, android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(browserIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            }
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
    }
}
