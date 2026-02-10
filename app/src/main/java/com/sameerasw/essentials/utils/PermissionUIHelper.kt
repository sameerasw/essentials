package com.sameerasw.essentials.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.registry.PermissionRegistry
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.viewmodels.MainViewModel

object PermissionUIHelper {

    fun getPermissionItem(
        key: String,
        context: Context,
        viewModel: MainViewModel,
        activity: Activity? = null
    ): PermissionItem? {
        return when (key) {
            "ACCESSIBILITY" -> PermissionItem(
                iconRes = R.drawable.rounded_settings_accessibility_24,
                title = R.string.perm_accessibility_title,
                description = R.string.perm_accessibility_desc_common,
                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                actionLabel = R.string.perm_action_enable,
                action = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                },
                isGranted = viewModel.isAccessibilityEnabled.value
            )

            "WRITE_SECURE_SETTINGS" -> PermissionItem(
                iconRes = R.drawable.rounded_security_24,
                title = R.string.perm_write_secure_title,
                description = R.string.perm_write_secure_desc_common,
                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                actionLabel = R.string.perm_action_copy_adb,
                action = {
                    val adbCommand = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
                    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("adb_command", adbCommand)
                    clipboard.setPrimaryClip(clip)
                },
                secondaryActionLabel = R.string.perm_action_check,
                secondaryAction = {
                    viewModel.check(context)
                },
                isGranted = viewModel.isWriteSecureSettingsEnabled.value
            )

            "NOTIFICATION_LISTENER" -> PermissionItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = R.string.perm_notif_listener_title,
                description = if (PermissionRegistry.getFeatures("NOTIFICATION_LISTENER").contains(R.string.feat_freeze_title))
                    R.string.perm_notif_listener_desc_freeze else R.string.perm_notif_listener_desc_lighting,
                dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_LISTENER"),
                actionLabel = R.string.perm_action_grant,
                action = { viewModel.requestNotificationListenerPermission(context) },
                isGranted = viewModel.isNotificationListenerEnabled.value
            )

            "DRAW_OVERLAYS" -> PermissionItem(
                iconRes = R.drawable.rounded_magnify_fullscreen_24,
                title = R.string.perm_overlay_title,
                description = R.string.perm_overlay_desc,
                dependentFeatures = PermissionRegistry.getFeatures("DRAW_OVERLAYS"),
                actionLabel = R.string.perm_action_grant,
                action = { PermissionUtils.openOverlaySettings(context) },
                isGranted = viewModel.isOverlayPermissionGranted.value
            )

            "WRITE_SETTINGS" -> PermissionItem(
                iconRes = R.drawable.rounded_security_24,
                title = R.string.perm_write_settings_title,
                description = R.string.perm_write_settings_desc,
                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SETTINGS"),
                actionLabel = R.string.perm_action_grant,
                action = { PermissionUtils.openWriteSettings(context) },
                isGranted = viewModel.isWriteSettingsEnabled.value
            )

            "NOTIFICATION_POLICY" -> PermissionItem(
                iconRes = R.drawable.rounded_volume_up_24,
                title = R.string.perm_notif_policy_title,
                description = R.string.perm_notif_policy_desc,
                dependentFeatures = PermissionRegistry.getFeatures("NOTIFICATION_POLICY"),
                actionLabel = R.string.perm_action_grant,
                action = { PermissionUtils.openNotificationPolicySettings(context) },
                isGranted = viewModel.isNotificationPolicyAccessGranted.value
            )

            "POST_NOTIFICATIONS" -> PermissionItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = R.string.permission_post_notifications_title,
                description = R.string.permission_post_notifications_desc,
                dependentFeatures = PermissionRegistry.getFeatures("POST_NOTIFICATIONS"),
                actionLabel = R.string.perm_action_grant,
                action = {
                    if (activity != null) {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            103
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                         val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                },
                isGranted = viewModel.isPostNotificationsEnabled.value
            )

            "READ_PHONE_STATE" -> PermissionItem(
                iconRes = R.drawable.rounded_mobile_vibrate_24,
                title = R.string.permission_read_phone_state_title,
                description = R.string.permission_read_phone_state_desc_call_vibrations,
                dependentFeatures = PermissionRegistry.getFeatures("READ_PHONE_STATE"),
                actionLabel = R.string.perm_action_grant,
                action = {
                    if (activity != null) {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(android.Manifest.permission.READ_PHONE_STATE),
                            102
                        )
                    }
                },
                isGranted = viewModel.isReadPhoneStateEnabled.value
            )

            "LOCATION" -> PermissionItem(
                iconRes = R.drawable.rounded_navigation_24,
                title = R.string.perm_location_title,
                description = R.string.perm_location_desc,
                dependentFeatures = PermissionRegistry.getFeatures("LOCATION"),
                actionLabel = R.string.perm_action_grant,
                action = { 
                    (activity as? androidx.activity.ComponentActivity)?.let { viewModel.requestLocationPermission(it) }
                },
                isGranted = viewModel.isLocationPermissionGranted.value
            )

            "BACKGROUND_LOCATION" -> PermissionItem(
                iconRes = R.drawable.rounded_navigation_24,
                title = R.string.perm_bg_location_title,
                description = R.string.perm_bg_location_desc,
                dependentFeatures = PermissionRegistry.getFeatures("BACKGROUND_LOCATION"),
                actionLabel = R.string.perm_action_grant,
                action = { 
                    (activity as? androidx.activity.ComponentActivity)?.let { viewModel.requestBackgroundLocationPermission(it) }
                },
                isGranted = viewModel.isBackgroundLocationPermissionGranted.value
            )

            "DEVICE_ADMIN" -> PermissionItem(
                iconRes = R.drawable.rounded_security_24,
                title = R.string.perm_device_admin_title,
                description = R.string.perm_device_admin_desc,
                dependentFeatures = PermissionRegistry.getFeatures("DEVICE_ADMIN"),
                actionLabel = R.string.action_enable_in_settings,
                action = { viewModel.requestDeviceAdmin(context) },
                isGranted = viewModel.isDeviceAdminEnabled.value
            )

            "ROOT" -> PermissionItem(
                iconRes = R.drawable.rounded_numbers_24,
                title = R.string.perm_root_title,
                description = R.string.perm_root_desc,
                dependentFeatures = PermissionRegistry.getFeatures("ROOT"),
                actionLabel = R.string.perm_action_grant,
                action = { viewModel.check(context) },
                isGranted = viewModel.isRootPermissionGranted.value
            )

            "SHIZUKU" -> PermissionItem(
                iconRes = R.drawable.rounded_mode_cool_24,
                title = R.string.perm_shizuku_title,
                description = R.string.perm_shizuku_desc,
                dependentFeatures = PermissionRegistry.getFeatures("SHIZUKU"),
                actionLabel = R.string.perm_action_grant,
                action = { viewModel.requestShizukuPermission() },
                isGranted = viewModel.isShizukuPermissionGranted.value
            )
            
            "READ_CALENDAR" -> PermissionItem(
                iconRes = R.drawable.rounded_sync_24,
                title = R.string.feat_calendar_sync_title,
                description = R.string.feat_calendar_sync_desc,
                dependentFeatures = PermissionRegistry.getFeatures("READ_CALENDAR"),
                actionLabel = R.string.perm_action_grant,
                action = {
                    if (activity != null) {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(android.Manifest.permission.READ_CALENDAR),
                            104
                        )
                    }
                },
                isGranted = viewModel.isCalendarPermissionGranted.value
            )

            "USAGE_STATS" -> PermissionItem(
                iconRes = R.drawable.rounded_data_usage_24,
                title = R.string.perm_usage_stats_title,
                description = R.string.perm_usage_stats_desc,
                dependentFeatures = PermissionRegistry.getFeatures("USAGE_STATS"),
                actionLabel = R.string.perm_action_grant,
                action = { PermissionUtils.openUsageStatsSettings(context) },
                isGranted = viewModel.isUsageStatsPermissionGranted.value
            )

            else -> null
        }
    }

    fun getPermissionItems(
        keys: List<String>,
        context: Context,
        viewModel: MainViewModel,
        activity: Activity? = null
    ): List<PermissionItem> {
        return keys.mapNotNull { getPermissionItem(it, context, viewModel, activity) }
    }
}
