package com.sameerasw.essentials.viewmodels

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.MapsState
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import com.sameerasw.essentials.domain.model.NotificationLightingSide
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.domain.model.SearchableItem
import com.sameerasw.essentials.domain.model.UpdateInfo
import com.sameerasw.essentials.SearchRegistry
import com.sameerasw.essentials.services.CaffeinateWakeLockService
import com.sameerasw.essentials.services.NotificationLightingService
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.services.ScreenOffAccessibilityService
import com.sameerasw.essentials.services.receivers.SecurityDeviceAdminReceiver
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticFeedbackType
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShizukuUtils
import com.sameerasw.essentials.utils.UpdateNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

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
    val isNotificationLightingEnabled = mutableStateOf(false)
    val isOverlayPermissionGranted = mutableStateOf(false)
    val isNotificationLightingAccessibilityEnabled = mutableStateOf(false)
    val hapticFeedbackType = mutableStateOf(HapticFeedbackType.SUBTLE)
    val isDefaultBrowserSet = mutableStateOf(false)
    val onlyShowWhenScreenOff = mutableStateOf(true)
    val isAmbientDisplayEnabled = mutableStateOf(false)
    val isAmbientShowLockScreenEnabled = mutableStateOf(false)
    val isButtonRemapEnabled = mutableStateOf(false)
    val isButtonRemapUseShizuku = mutableStateOf(false)
    val shizukuDetectedDevicePath = mutableStateOf<String?>(null)
    val volumeUpActionOff = mutableStateOf("None")
    val volumeDownActionOff = mutableStateOf("None")
    val volumeUpActionOn = mutableStateOf("None")
    val volumeDownActionOn = mutableStateOf("None")
    val remapHapticType = mutableStateOf(HapticFeedbackType.DOUBLE)
    val isDynamicNightLightEnabled = mutableStateOf(false)
    val isSnoozeDebuggingEnabled = mutableStateOf(false)
    val isSnoozeFileTransferEnabled = mutableStateOf(false)
    val isSnoozeChargingEnabled = mutableStateOf(false)
    val isFlashlightAlwaysTurnOffEnabled = mutableStateOf(false)
    val isFlashlightFadeEnabled = mutableStateOf(false)
    val isFlashlightAdjustEnabled = mutableStateOf(false)
    val isFlashlightGlobalEnabled = mutableStateOf(false)
    val isFlashlightLiveUpdateEnabled = mutableStateOf(true)
    val flashlightLastIntensity = mutableStateOf(1)
    val isFlashlightPulseEnabled = mutableStateOf(false)
    val isFlashlightPulseFacedownOnly = mutableStateOf(true)



    val isScreenLockedSecurityEnabled = mutableStateOf(false)
    val isDeviceAdminEnabled = mutableStateOf(false)
    val isDeveloperModeEnabled = mutableStateOf(false)
    val skipSilentNotifications = mutableStateOf(true)
    val notificationLightingStyle = mutableStateOf(NotificationLightingStyle.STROKE)
    val notificationLightingColorMode = mutableStateOf(NotificationLightingColorMode.SYSTEM)
    val notificationLightingCustomColor = mutableIntStateOf(0xFF6200EE.toInt()) // Default purple
    val notificationLightingPulseCount = mutableIntStateOf(1)
    val notificationLightingPulseDuration = mutableStateOf(3000f)
    val notificationLightingIndicatorX = mutableStateOf(50f) // 0-100 percentage
    val notificationLightingIndicatorY = mutableStateOf(2f)  // 0-100 percentage, default top
    val notificationLightingIndicatorScale = mutableStateOf(1.0f)
    val notificationLightingGlowSides = mutableStateOf(setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT))
    val skipPersistentNotifications = mutableStateOf(false)
    val isAppLockEnabled = mutableStateOf(false)
    val isFreezeWhenLockedEnabled = mutableStateOf(false)
    val freezeLockDelayIndex = mutableIntStateOf(1) // Default: 1 minute
    val freezePickedApps = mutableStateOf<List<NotificationApp>>(emptyList())
    val isFreezePickedAppsLoading = mutableStateOf(false)
    val freezeAutoExcludedApps = mutableStateOf<Set<String>>(emptySet())

    // Search state
    val searchQuery = mutableStateOf("")
    val searchResults = mutableStateOf<List<SearchableItem>>(emptyList())
    val isSearching = mutableStateOf(false)

    // Update state
    val updateInfo = mutableStateOf<UpdateInfo?>(null)
    val isUpdateAvailable = mutableStateOf(false)
    val isCheckingUpdate = mutableStateOf(false)
    val isAutoUpdateEnabled = mutableStateOf(true)
    val isUpdateNotificationEnabled = mutableStateOf(true)
    val isPreReleaseCheckEnabled = mutableStateOf(false)
    private var lastUpdateCheckTime: Long = 0
    
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "edge_lighting_enabled" -> isNotificationLightingEnabled.value = sharedPreferences.getBoolean(key, false)
            "dynamic_night_light_enabled" -> isDynamicNightLightEnabled.value = sharedPreferences.getBoolean(key, false)
            "screen_locked_security_enabled" -> isScreenLockedSecurityEnabled.value = sharedPreferences.getBoolean(key, false)
            "maps_power_saving_enabled" -> {
                isMapsPowerSavingEnabled.value = sharedPreferences.getBoolean(key, false)
                MapsState.isEnabled = isMapsPowerSavingEnabled.value
            }
            "status_bar_icon_control_enabled" -> isStatusBarIconControlEnabled.value = sharedPreferences.getBoolean(key, false)
            "button_remap_enabled" -> isButtonRemapEnabled.value = sharedPreferences.getBoolean(key, false)
            "app_lock_enabled" -> isAppLockEnabled.value = sharedPreferences.getBoolean(key, false)
            "freeze_when_locked_enabled" -> isFreezeWhenLockedEnabled.value = sharedPreferences.getBoolean(key, false)
            "freeze_lock_delay_index" -> freezeLockDelayIndex.intValue = sharedPreferences.getInt(key, 1)
            "freeze_auto_excluded_apps" -> {
                val json = sharedPreferences.getString(key, null)
                freezeAutoExcludedApps.value = if (json != null) {
                    try {
                        Gson().fromJson(json, object : TypeToken<Set<String>>() {}.type) ?: emptySet()
                    } catch (e: Exception) { emptySet() }
                } else emptySet()
            }
            "check_pre_releases_enabled" -> isPreReleaseCheckEnabled.value = sharedPreferences.getBoolean(key, false)
        }
    }

    fun check(context: Context) {
        isAccessibilityEnabled.value = PermissionUtils.isAccessibilityServiceEnabled(context)
        isWriteSecureSettingsEnabled.value = PermissionUtils.canWriteSecureSettings(context)
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
        isNotificationListenerEnabled.value = PermissionUtils.hasNotificationListenerPermission(context)
        isOverlayPermissionGranted.value = PermissionUtils.canDrawOverlays(context)
        isNotificationLightingAccessibilityEnabled.value = PermissionUtils.isNotificationLightingAccessibilityServiceEnabled(context)
        isDefaultBrowserSet.value = PermissionUtils.isDefaultBrowser(context)
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        
        isWidgetEnabled.value = prefs.getBoolean("widget_enabled", false)
        isStatusBarIconControlEnabled.value = prefs.getBoolean("status_bar_icon_control_enabled", false)
        isMapsPowerSavingEnabled.value = prefs.getBoolean("maps_power_saving_enabled", false)
        isNotificationLightingEnabled.value = prefs.getBoolean("edge_lighting_enabled", false)
        onlyShowWhenScreenOff.value = prefs.getBoolean("edge_lighting_only_screen_off", true)
        isAmbientDisplayEnabled.value = prefs.getBoolean("edge_lighting_ambient_display", false)
        isAmbientShowLockScreenEnabled.value = prefs.getBoolean("edge_lighting_ambient_show_lock_screen", false)
        skipSilentNotifications.value = prefs.getBoolean("edge_lighting_skip_silent", true)
        skipPersistentNotifications.value = prefs.getBoolean("edge_lighting_skip_persistent", false)
        val styleName = prefs.getString("edge_lighting_style", NotificationLightingStyle.STROKE.name)
        notificationLightingStyle.value = NotificationLightingStyle.valueOf(styleName ?: NotificationLightingStyle.STROKE.name)
        val colorModeName = prefs.getString("edge_lighting_color_mode", NotificationLightingColorMode.SYSTEM.name)
        notificationLightingColorMode.value = NotificationLightingColorMode.valueOf(colorModeName ?: NotificationLightingColorMode.SYSTEM.name)
        notificationLightingCustomColor.intValue = prefs.getInt("edge_lighting_custom_color", 0xFF6200EE.toInt())
        notificationLightingPulseCount.intValue = prefs.getInt("edge_lighting_pulse_count", 1)
        notificationLightingPulseDuration.value = prefs.getFloat("edge_lighting_pulse_duration", 3000f)
        notificationLightingIndicatorX.value = prefs.getFloat("edge_lighting_indicator_x", 50f)
        notificationLightingIndicatorY.value = prefs.getFloat("edge_lighting_indicator_y", 2f)
        notificationLightingIndicatorScale.value = prefs.getFloat("edge_lighting_indicator_scale", 1.0f)
        notificationLightingGlowSides.value = loadNotificationLightingGlowSides(context)
        MapsState.isEnabled = isMapsPowerSavingEnabled.value
        loadHapticFeedback(context)
        checkCaffeinateActive(context)
        
        // Button Remap & Migration
        isButtonRemapEnabled.value = prefs.getBoolean("button_remap_enabled", 
            prefs.getBoolean("flashlight_volume_toggle_enabled", false))
        isButtonRemapUseShizuku.value = prefs.getBoolean("button_remap_use_shizuku", false)
        shizukuDetectedDevicePath.value = prefs.getString("shizuku_detected_device_path", null)
            
        val oldTrigger = prefs.getString("flashlight_trigger_button", "Volume Up")
        volumeUpActionOff.value = prefs.getString("button_remap_vol_up_action_off", 
            prefs.getString("button_remap_vol_up_action", // Migration from previous version
            if (oldTrigger == "Volume Up" && prefs.contains("flashlight_volume_toggle_enabled")) "Toggle flashlight" else "None")) ?: "None"
        
        volumeDownActionOff.value = prefs.getString("button_remap_vol_down_action_off", 
            prefs.getString("button_remap_vol_down_action", // Migration from previous version
            if (oldTrigger == "Volume Down" && prefs.contains("flashlight_volume_toggle_enabled")) "Toggle flashlight" else "None")) ?: "None"
            
        volumeUpActionOn.value = prefs.getString("button_remap_vol_up_action_on", "None") ?: "None"
        volumeDownActionOn.value = prefs.getString("button_remap_vol_down_action_on", "None") ?: "None"
            
        val hapticName = prefs.getString("button_remap_haptic_type", 
            prefs.getString("flashlight_haptic_type", HapticFeedbackType.DOUBLE.name))
        remapHapticType.value = try {
            val type = HapticFeedbackType.valueOf(hapticName ?: HapticFeedbackType.DOUBLE.name)
            if (type.name == "LONG") HapticFeedbackType.DOUBLE else type
        } catch (e: Exception) {
            HapticFeedbackType.DOUBLE
        }
        
        isDynamicNightLightEnabled.value = prefs.getBoolean("dynamic_night_light_enabled", false)
        isSnoozeDebuggingEnabled.value = prefs.getBoolean("snooze_debugging_enabled", false)
        isSnoozeFileTransferEnabled.value = prefs.getBoolean("snooze_file_transfer_enabled", false)
        isSnoozeChargingEnabled.value = prefs.getBoolean("snooze_charging_enabled", false)
        isFlashlightAlwaysTurnOffEnabled.value = prefs.getBoolean("flashlight_always_turn_off_enabled", false)
        isFlashlightFadeEnabled.value = prefs.getBoolean("flashlight_fade_enabled", false)
        isFlashlightAdjustEnabled.value = prefs.getBoolean("flashlight_adjust_intensity_enabled", false)
        isFlashlightGlobalEnabled.value = prefs.getBoolean("flashlight_global_enabled", false)
        isFlashlightLiveUpdateEnabled.value = prefs.getBoolean("flashlight_live_update_enabled", true)
        flashlightLastIntensity.value = prefs.getInt("flashlight_last_intensity", 1)
        isFlashlightPulseEnabled.value = prefs.getBoolean("flashlight_pulse_enabled", false)
        isFlashlightPulseFacedownOnly.value = prefs.getBoolean("flashlight_pulse_facedown_only", true)



        isScreenLockedSecurityEnabled.value = prefs.getBoolean("screen_locked_security_enabled", false)
        isDeviceAdminEnabled.value = isDeviceAdminActive(context)
        
        isAutoUpdateEnabled.value = prefs.getBoolean("auto_update_enabled", true)
        isUpdateNotificationEnabled.value = prefs.getBoolean("update_notification_enabled", true)
        lastUpdateCheckTime = prefs.getLong("last_update_check_time", 0)
        isAppLockEnabled.value = prefs.getBoolean("app_lock_enabled", false)
        isFreezeWhenLockedEnabled.value = prefs.getBoolean("freeze_when_locked_enabled", false)
        freezeLockDelayIndex.intValue = prefs.getInt("freeze_lock_delay_index", 1)
        val excludedJson = prefs.getString("freeze_auto_excluded_apps", null)
        freezeAutoExcludedApps.value = if (excludedJson != null) {
            try {
                Gson().fromJson(excludedJson, object : TypeToken<Set<String>>() {}.type) ?: emptySet()
            } catch (e: Exception) { emptySet() }
        } else emptySet()
        isDeveloperModeEnabled.value = prefs.getBoolean("developer_mode_enabled", false)
        isPreReleaseCheckEnabled.value = prefs.getBoolean("check_pre_releases_enabled", false)
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
        if (query.isBlank()) {
            searchResults.value = emptyList()
            isSearching.value = false
            return
        }

        isSearching.value = true
        searchResults.value = SearchRegistry.search(query)
        isSearching.value = false
    }

    fun setAutoUpdateEnabled(enabled: Boolean, context: Context) {
        isAutoUpdateEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("auto_update_enabled", enabled)
        }
    }

    fun setUpdateNotificationEnabled(enabled: Boolean, context: Context) {
        isUpdateNotificationEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("update_notification_enabled", enabled)
        }
    }

    fun setPreReleaseCheckEnabled(enabled: Boolean, context: Context) {
        isPreReleaseCheckEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("check_pre_releases_enabled", enabled)
        }
    }

    fun setDeveloperModeEnabled(enabled: Boolean, context: Context) {
        isDeveloperModeEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("developer_mode_enabled", enabled)
        }
    }

    fun checkForUpdates(context: Context, manual: Boolean = false) {
        if (isCheckingUpdate.value) return
        
        if (!manual) {
            if (!isAutoUpdateEnabled.value) return
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateCheckTime < 900000) return
        }

        isCheckingUpdate.value = true
        viewModelScope.launch {
            try {
                val currentVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) {
                    "0.0"
                } ?: "0.0"
                
                val releaseData = withContext(Dispatchers.IO) {
                    // If pre-releases are enabled, fetch all releases and pick top one.
                    // If not, fetch only the latest stable release.
                    val url = if (isPreReleaseCheckEnabled.value) {
                        URL("https://api.github.com/repos/sameerasw/essentials/releases")
                    } else {
                        URL("https://api.github.com/repos/sameerasw/essentials/releases/latest")
                    }
                    url.readText()
                }

                val release: Map<String, Any>? = if (isPreReleaseCheckEnabled.value) {
                    val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val releases: List<Map<String, Any>> = Gson().fromJson(releaseData, listType)
                    releases.firstOrNull()
                } else {
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    Gson().fromJson(releaseData, mapType)
                }

                if (release == null) return@launch

                val latestVersion = (release["tag_name"] as? String)?.removePrefix("v") ?: "0.0"
                val body = release["body"] as? String ?: ""
                val releaseUrl = release["html_url"] as? String ?: ""
                val assets = release["assets"] as? List<Map<String, Any>>
                val downloadUrl = assets?.firstOrNull { it["name"].toString() == "app-release.apk" }?.get("browser_download_url") as? String 
                    ?: assets?.firstOrNull { it["name"].toString().endsWith(".apk") }?.get("browser_download_url") as? String 
                    ?: ""

                val hasUpdate = isNewerVersion(currentVersion, latestVersion)
                
                updateInfo.value = UpdateInfo(
                    versionName = latestVersion,
                    releaseNotes = body,
                    downloadUrl = downloadUrl,
                    releaseUrl = releaseUrl,
                    isUpdateAvailable = hasUpdate
                )
                isUpdateAvailable.value = hasUpdate
                
                if (hasUpdate && downloadUrl.isNotEmpty()) {
                    if (isUpdateNotificationEnabled.value) {
                        UpdateNotificationHelper.showUpdateNotification(context, latestVersion, downloadUrl)
                    }
                }
                
                // Update last check time on success
                lastUpdateCheckTime = System.currentTimeMillis()
                context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
                    putLong("last_update_check_time", lastUpdateCheckTime)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                if (manual) {
                    // Fail silently or handle error
                }
            } finally {
                isCheckingUpdate.value = false
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        return try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLength) {
                val v1 = if (i < currentParts.size) currentParts[i] else 0
                val v2 = if (i < latestParts.size) latestParts[i] else 0
                if (v2 > v1) return true
                if (v1 > v2) return false
            }
            false
        } catch (e: Exception) {
            latest != current
        }
    }

    private fun isDeviceAdminActive(context: Context): Boolean {
        return PermissionUtils.isDeviceAdminActive(context)
    }

    fun requestDeviceAdmin(context: Context) {
        val adminComponent = ComponentName(context, SecurityDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to hard-lock the device when unauthorized network changes are attempted on lock screen.")
        }
        if (context is Activity) {
            context.startActivity(intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
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

    fun setNotificationLightingEnabled(enabled: Boolean, context: Context) {
        isNotificationLightingEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("edge_lighting_enabled", enabled)
        }
    }

    fun setOnlyShowWhenScreenOff(enabled: Boolean, context: Context) {
        onlyShowWhenScreenOff.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("edge_lighting_only_screen_off", enabled)
        }
    }

    fun setAmbientDisplayEnabled(enabled: Boolean, context: Context) {
        isAmbientDisplayEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("edge_lighting_ambient_display", enabled)
        }
    }

    fun setAmbientShowLockScreenEnabled(enabled: Boolean, context: Context) {
        isAmbientShowLockScreenEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("edge_lighting_ambient_show_lock_screen", enabled)
        }
    }
    
    fun setSkipSilentNotifications(enabled: Boolean, context: Context) {
        skipSilentNotifications.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("edge_lighting_skip_silent", enabled)
        }
    }

    fun setSkipPersistentNotifications(enabled: Boolean, context: Context) {
        skipPersistentNotifications.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("edge_lighting_skip_persistent", enabled)
        }
    }

    fun setNotificationLightingStyle(style: NotificationLightingStyle, context: Context) {
        notificationLightingStyle.value = style
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putString("edge_lighting_style", style.name)
        }
    }

    fun setNotificationLightingColorMode(mode: NotificationLightingColorMode, context: Context) {
        notificationLightingColorMode.value = mode
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putString("edge_lighting_color_mode", mode.name)
        }
    }

    fun setNotificationLightingCustomColor(color: Int, context: Context) {
        notificationLightingCustomColor.intValue = color
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putInt("edge_lighting_custom_color", color)
        }
    }

    fun setButtonRemapEnabled(enabled: Boolean, context: Context) {
        isButtonRemapEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("button_remap_enabled", enabled)
        }
    }

    fun setButtonRemapUseShizuku(enabled: Boolean, context: Context) {
        isButtonRemapUseShizuku.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("button_remap_use_shizuku", enabled)
        }
    }

    fun setVolumeUpActionOff(action: String, context: Context) {
        volumeUpActionOff.value = action
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putString("button_remap_vol_up_action_off", action)
        }
    }

    fun setVolumeDownActionOff(action: String, context: Context) {
        volumeDownActionOff.value = action
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putString("button_remap_vol_down_action_off", action)
        }
    }

    fun setVolumeUpActionOn(action: String, context: Context) {
        volumeUpActionOn.value = action
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putString("button_remap_vol_up_action_on", action)
        }
    }

    fun setVolumeDownActionOn(action: String, context: Context) {
        volumeDownActionOn.value = action
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putString("button_remap_vol_down_action_on", action)
        }
    }

    fun setRemapHapticType(type: HapticFeedbackType, context: Context) {
        remapHapticType.value = type
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putString("button_remap_haptic_type", type.name)
        }
    }

    fun setDynamicNightLightEnabled(enabled: Boolean, context: Context) {
        isDynamicNightLightEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("dynamic_night_light_enabled", enabled)
        }
    }

    fun setAppLockEnabled(enabled: Boolean, context: Context) {
        isAppLockEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("app_lock_enabled", enabled)
        }
    }

    fun setFreezeWhenLockedEnabled(enabled: Boolean, context: Context) {
        isFreezeWhenLockedEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("freeze_when_locked_enabled", enabled)
        }
    }

    fun setFreezeLockDelayIndex(index: Int, context: Context) {
        freezeLockDelayIndex.intValue = index
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putInt("freeze_lock_delay_index", index)
        }
    }

    fun saveNotificationLightingPulseCount(context: Context, count: Int) {
        notificationLightingPulseCount.intValue = count
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putInt("edge_lighting_pulse_count", count)
        }
    }

    fun saveNotificationLightingPulseDuration(context: Context, duration: Float) {
        notificationLightingPulseDuration.value = duration
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putFloat("edge_lighting_pulse_duration", duration)
        }
    }

    fun setFlashlightPulseEnabled(enabled: Boolean, context: Context) {
        isFlashlightPulseEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("flashlight_pulse_enabled", enabled)
        }
    }

    fun setFlashlightPulseFacedownOnly(enabled: Boolean, context: Context) {
        isFlashlightPulseFacedownOnly.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("flashlight_pulse_facedown_only", enabled)
        }
    }

    // Helper to show the overlay service for testing/triggering
    fun triggerNotificationLighting(context: Context) {
        val radius = loadNotificationLightingCornerRadius(context)
        val thickness = loadNotificationLightingStrokeThickness(context)
        try {
            val intent = Intent(context, com.sameerasw.essentials.services.NotificationLightingService::class.java).apply {
                putExtra("corner_radius_dp", radius)
                putExtra("stroke_thickness_dp", thickness)
                putExtra("ignore_screen_state", true)
                putExtra("style", notificationLightingStyle.value.name)
                putExtra("color_mode", notificationLightingColorMode.value.name)
                putExtra("custom_color", notificationLightingCustomColor.intValue)
                putExtra("pulse_count", notificationLightingPulseCount.intValue)
                putExtra("pulse_duration", notificationLightingPulseDuration.value.toLong())
                putExtra("glow_sides", notificationLightingGlowSides.value.map { it.name }.toTypedArray())
                putExtra("indicator_x", notificationLightingIndicatorX.value)
                putExtra("indicator_y", notificationLightingIndicatorY.value)
                putExtra("indicator_scale", notificationLightingIndicatorScale.value)
            }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    // Helper to show the overlay service with custom corner radius
    fun triggerNotificationLightingWithRadius(context: Context, cornerRadiusDp: Int) {
        try {
            val intent = Intent(context, com.sameerasw.essentials.services.NotificationLightingService::class.java).apply {
                putExtra("corner_radius_dp", cornerRadiusDp)
                putExtra("is_preview", true)
                putExtra("ignore_screen_state", true)
                putExtra("style", notificationLightingStyle.value.name)
                putExtra("color_mode", notificationLightingColorMode.value.name)
                putExtra("custom_color", notificationLightingCustomColor.intValue)
                putExtra("glow_sides", notificationLightingGlowSides.value.map { it.name }.toTypedArray())
                putExtra("indicator_x", notificationLightingIndicatorX.value)
                putExtra("indicator_y", notificationLightingIndicatorY.value)
                putExtra("indicator_scale", notificationLightingIndicatorScale.value)
            }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    // Helper to show the overlay service with custom corner radius and stroke thickness
    fun triggerNotificationLightingWithRadiusAndThickness(context: Context, cornerRadiusDp: Int, strokeThicknessDp: Int) {
        try {
            val intent = Intent(context, com.sameerasw.essentials.services.NotificationLightingService::class.java).apply {
                putExtra("corner_radius_dp", cornerRadiusDp)
                putExtra("stroke_thickness_dp", strokeThicknessDp)
                putExtra("is_preview", true)
                putExtra("ignore_screen_state", true)
                putExtra("style", notificationLightingStyle.value.name)
                putExtra("color_mode", notificationLightingColorMode.value.name)
                putExtra("custom_color", notificationLightingCustomColor.intValue)
                putExtra("glow_sides", notificationLightingGlowSides.value.map { it.name }.toTypedArray())
                putExtra("indicator_x", notificationLightingIndicatorX.value)
                putExtra("indicator_y", notificationLightingIndicatorY.value)
                putExtra("indicator_scale", notificationLightingIndicatorScale.value)
            }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    // Helper to remove preview overlay
    fun removePreviewOverlay(context: Context) {
        try {
            val intent1 = Intent(context, NotificationLightingService::class.java).apply {
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
        return PermissionUtils.isAccessibilityServiceEnabled(context)
    }

    fun canWriteSecureSettings(context: Context): Boolean {
        return PermissionUtils.canWriteSecureSettings(context)
    }

    fun requestReadPhoneStatePermission(activity: androidx.activity.ComponentActivity) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_PHONE_STATE),
            1001
        )
    }

    fun requestNotificationPermission(activity: androidx.activity.ComponentActivity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1002
            )
        }
    }

    private fun hasNotificationListenerPermission(context: Context): Boolean {
        return PermissionUtils.hasNotificationListenerPermission(context)
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
        return PermissionUtils.canDrawOverlays(context)
    }

    private fun isNotificationLightingAccessibilityServiceEnabled(context: Context): Boolean {
        return PermissionUtils.isNotificationLightingAccessibilityServiceEnabled(context)
    }

    private fun isDefaultBrowser(context: Context): Boolean {
        return PermissionUtils.isDefaultBrowser(context)
    }

    // Notification Lighting App Selection Methods
    fun saveNotificationLightingSelectedApps(context: Context, apps: List<AppSelection>) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(apps)
        prefs.edit().putString("edge_lighting_selected_apps", json).apply()
    }

    fun loadNotificationLightingSelectedApps(context: Context): List<AppSelection> {
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

    fun updateNotificationLightingAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        val currentSelections = loadNotificationLightingSelectedApps(context).toMutableList()
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

    // Notification Lighting Corner Radius Methods
    fun saveNotificationLightingCornerRadius(context: Context, radiusDp: Int) {
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putInt("edge_lighting_corner_radius", radiusDp)
        }
    }

    fun loadNotificationLightingCornerRadius(context: Context): Int {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("edge_lighting_corner_radius", 20) // Default to 20 dp
    }

    // Notification Lighting Stroke Thickness Methods
    fun saveNotificationLightingStrokeThickness(context: Context, thicknessDp: Int) {
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putInt("edge_lighting_stroke_thickness", thicknessDp)
        }
    }

    fun loadNotificationLightingStrokeThickness(context: Context): Int {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("edge_lighting_stroke_thickness", 8) // Default to 8 dp
    }

    // Dynamic Night Light App Selection Methods
    fun saveDynamicNightLightSelectedApps(context: Context, apps: List<AppSelection>) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(apps)
        prefs.edit().putString("dynamic_night_light_selected_apps", json).apply()
    }

    fun loadDynamicNightLightSelectedApps(context: Context): List<AppSelection> {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("dynamic_night_light_selected_apps", null)
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

    fun updateDynamicNightLightAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        val currentSelections = loadDynamicNightLightSelectedApps(context).toMutableList()
        val selectionIndex = currentSelections.indexOfFirst { it.packageName == packageName }
        if (selectionIndex != -1) {
            currentSelections[selectionIndex] = currentSelections[selectionIndex].copy(isEnabled = enabled)
        } else {
            currentSelections.add(AppSelection(packageName, enabled))
        }
        val gson = Gson()
        val json = gson.toJson(currentSelections)
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("dynamic_night_light_selected_apps", json)
            .apply()
    }

    // App Lock App Selection Methods
    fun saveAppLockSelectedApps(context: Context, apps: List<AppSelection>) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(apps)
        prefs.edit().putString("app_lock_selected_apps", json).apply()
    }

    fun loadAppLockSelectedApps(context: Context): List<AppSelection> {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("app_lock_selected_apps", null)
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

    fun updateAppLockAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        val currentSelections = loadAppLockSelectedApps(context).toMutableList()
        val selectionIndex = currentSelections.indexOfFirst { it.packageName == packageName }
        if (selectionIndex != -1) {
            currentSelections[selectionIndex] = currentSelections[selectionIndex].copy(isEnabled = enabled)
        } else {
            currentSelections.add(AppSelection(packageName, enabled))
        }
        val gson = Gson()
        val json = gson.toJson(currentSelections)
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_lock_selected_apps", json)
            .apply()
    }

    // Freeze App Selection Methods
    fun saveFreezeSelectedApps(context: Context, apps: List<AppSelection>) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val filteredApps = apps.filter { it.isEnabled } // Only save picked apps
        val gson = Gson()
        val json = gson.toJson(filteredApps)
        prefs.edit().putString("freeze_selected_apps", json).apply()
        
        // Refresh full app list for UI
        refreshFreezePickedApps(context)
    }

    fun loadFreezeSelectedApps(context: Context): List<AppSelection> {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("freeze_selected_apps", null)
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

    fun updateFreezeAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        val currentSelections = loadFreezeSelectedApps(context).toMutableList()
        val selectionIndex = currentSelections.indexOfFirst { it.packageName == packageName }
        if (selectionIndex != -1) {
            currentSelections[selectionIndex] = currentSelections[selectionIndex].copy(isEnabled = enabled)
        } else {
            currentSelections.add(AppSelection(packageName, enabled))
        }
        
        // Filter: Only keep apps that are picked (enabled)
        val filteredSelections = currentSelections.filter { it.isEnabled }
        
        val gson = Gson()
        val json = gson.toJson(filteredSelections)
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("freeze_selected_apps", json)
            .apply()
        
        // Refresh full app list for UI
        refreshFreezePickedApps(context)
    }

    fun updateFreezeAppAutoFreeze(context: Context, packageName: String, autoFreezeEnabled: Boolean) {
        val currentSet = freezeAutoExcludedApps.value.toMutableSet()
        if (autoFreezeEnabled) {
            currentSet.remove(packageName)
        } else {
            currentSet.add(packageName)
        }
        freezeAutoExcludedApps.value = currentSet
        
        val json = Gson().toJson(currentSet)
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("freeze_auto_excluded_apps", json)
            .apply()
        
        refreshFreezePickedApps(context)
    }

    fun refreshFreezePickedApps(context: Context) {
        viewModelScope.launch {
            isFreezePickedAppsLoading.value = true
            try {
                // Only load apps that are actually marked as secondary selected (picked)
                val selections = loadFreezeSelectedApps(context).filter { it.isEnabled }
                if (selections.isEmpty()) {
                    freezePickedApps.value = emptyList()
                    return@launch
                }
                
                // Efficiently load only the apps that are actually marked as secondary selected (picked)
                val pickedPkgNames = selections.map { it.packageName }
                val relevantApps = AppUtil.getAppsByPackageNames(context, pickedPkgNames)
                
                val merged = AppUtil.mergeWithSavedApps(relevantApps, selections)
                val currentExcluded = freezeAutoExcludedApps.value
                
                // Cleanup: remove package names that are no longer picked
                val filteredExcluded = currentExcluded.filter { pickedPkgNames.contains(it) }.toSet()
                if (filteredExcluded.size != currentExcluded.size) {
                    freezeAutoExcludedApps.value = filteredExcluded
                    val json = Gson().toJson(filteredExcluded)
                    context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("freeze_auto_excluded_apps", json)
                        .apply()
                }
                
                freezePickedApps.value = merged.map { it.copy(isEnabled = !filteredExcluded.contains(it.packageName)) }
                    .sortedBy { it.appName.lowercase() }
            } finally {
                isFreezePickedAppsLoading.value = false
            }
        }
    }

    fun freezeAllAuto(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager.freezeAll(context)
        }
    }

    fun unfreezeAllAuto(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager.unfreezeAll(context)
        }
    }

    fun freezeAllManual(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager.freezeAllManual(context)
        }
    }

    fun unfreezeAllManual(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager.unfreezeAllManual(context)
        }
    }

    fun launchAndUnfreezeApp(context: Context, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFrozen = com.sameerasw.essentials.utils.FreezeManager.isAppFrozen(context, packageName)
            if (isFrozen) {
                com.sameerasw.essentials.utils.FreezeManager.unfreezeApp(packageName)
                // Small delay to ensure system registers the change before launch
                delay(100)
            }
            
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        }
    }

    fun freezeAllApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager.freezeAllManual(context)
            refreshFreezePickedApps(context)
        }
    }

    fun unfreezeAllApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager.unfreezeAllManual(context)
            refreshFreezePickedApps(context)
        }
    }

    fun freezeAutomaticApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager.freezeAll(context)
            refreshFreezePickedApps(context)
        }
    }

    fun setSnoozeDebuggingEnabled(enabled: Boolean, context: Context) {
        isSnoozeDebuggingEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("snooze_debugging_enabled", enabled)
        }
    }

    fun setSnoozeFileTransferEnabled(enabled: Boolean, context: Context) {
        isSnoozeFileTransferEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("snooze_file_transfer_enabled", enabled)
        }
    }

    fun setSnoozeChargingEnabled(enabled: Boolean, context: Context) {
        isSnoozeChargingEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("snooze_charging_enabled", enabled)
        }
    }

    fun setFlashlightAlwaysTurnOffEnabled(enabled: Boolean, context: Context) {
        isFlashlightAlwaysTurnOffEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("flashlight_always_turn_off_enabled", enabled)
        }
    }

    fun setFlashlightFadeEnabled(enabled: Boolean, context: Context) {
        isFlashlightFadeEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("flashlight_fade_enabled", enabled)
        }
    }

    fun setFlashlightAdjustEnabled(enabled: Boolean, context: Context) {
        isFlashlightAdjustEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("flashlight_adjust_intensity_enabled", enabled)
        }
    }

    fun setFlashlightGlobalEnabled(enabled: Boolean, context: Context) {
        isFlashlightGlobalEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("flashlight_global_enabled", enabled)
        }
    }

    fun setFlashlightLiveUpdateEnabled(enabled: Boolean, context: Context) {
        isFlashlightLiveUpdateEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("flashlight_live_update_enabled", enabled)
        }
    }

    fun setFlashlightLastIntensity(intensity: Int, context: Context) {
        flashlightLastIntensity.value = intensity
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putInt("flashlight_last_intensity", intensity)
        }
    }






    fun setScreenLockedSecurityEnabled(enabled: Boolean, context: Context) {
        isScreenLockedSecurityEnabled.value = enabled
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("screen_locked_security_enabled", enabled)
        }
    }

    fun setNotificationLightingGlowSides(sides: Set<NotificationLightingSide>, context: Context) {
        notificationLightingGlowSides.value = sides
        saveNotificationLightingGlowSides(context, sides)
    }

    fun saveNotificationLightingIndicatorX(context: Context, x: Float) {
        notificationLightingIndicatorX.value = x
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putFloat("edge_lighting_indicator_x", x)
        }
    }

    fun saveNotificationLightingIndicatorY(context: Context, y: Float) {
        notificationLightingIndicatorY.value = y
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putFloat("edge_lighting_indicator_y", y)
        }
    }

    fun saveNotificationLightingIndicatorScale(context: Context, scale: Float) {
        notificationLightingIndicatorScale.value = scale
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).edit {
            putFloat("edge_lighting_indicator_scale", scale)
        }
    }

    private fun saveNotificationLightingGlowSides(context: Context, sides: Set<NotificationLightingSide>) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(sides)
        prefs.edit().putString("edge_lighting_glow_sides", json).apply()
    }

    fun exportConfigs(context: Context, outputStream: java.io.OutputStream) {
        try {
            // Map<FileName, Map<PrefKey, WrappedValue>>
            val allConfigs = mutableMapOf<String, Map<String, Map<String, Any>>>()
            val prefFiles = listOf("essentials_prefs", "caffeinate_prefs", "link_prefs")

            prefFiles.forEach { fileName ->
                val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                val wrapperMap = mutableMapOf<String, Map<String, Any>>()
                
                prefs.all.forEach { (key, value) ->
                    val type = when (value) {
                        is Boolean -> "Boolean"
                        is Int -> "Int"
                        is Long -> "Long"
                        is Float -> "Float"
                        is String -> "String"
                        is Set<*> -> "StringSet"
                        else -> "Unknown"
                    }
                    if (value != null && type != "Unknown") {
                        wrapperMap[key] = mapOf("type" to type, "value" to value)
                    }
                }
                allConfigs[fileName] = wrapperMap
            }

            val json = Gson().toJson(allConfigs)
            outputStream.write(json.toByteArray())
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importConfigs(context: Context, inputStream: java.io.InputStream): Boolean {
        return try {
            val json = inputStream.bufferedReader().use { it.readText() }
            // Map<FileName, Map<PrefKey, Map<Type_Value, Value>>>
            val type = object : TypeToken<Map<String, Map<String, Map<String, Any>>>>() {}.type
            val allConfigs: Map<String, Map<String, Map<String, Any>>> = Gson().fromJson(json, type)
            
            allConfigs.forEach { (fileName, prefWrapper) ->
                val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                prefs.edit {
                    clear()
                    prefWrapper.forEach { (key, item) ->
                        val itemType = item["type"] as? String
                        val itemValue = item["value"]
                        
                        if (itemType != null && itemValue != null) {
                            try {
                                when (itemType) {
                                    "Boolean" -> putBoolean(key, itemValue as Boolean)
                                    "Int" -> putInt(key, (itemValue as Double).toInt())
                                    "Long" -> putLong(key, (itemValue as Double).toLong())
                                    "Float" -> putFloat(key, (itemValue as Double).toFloat())
                                    "String" -> putString(key, itemValue as String)
                                    "StringSet" -> {
                                        @Suppress("UNCHECKED_CAST")
                                        putStringSet(key, (itemValue as List<String>).toSet())
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            // Trigger a refresh of states
            check(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            inputStream.close()
        }
    }

    private fun loadNotificationLightingGlowSides(context: Context): Set<NotificationLightingSide> {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("edge_lighting_glow_sides", null)
        return if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<Set<NotificationLightingSide>>() {}.type
            try {
                gson.fromJson(json, type)
            } catch (e: Exception) {
                setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
            }
        } else {
            setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
        }
    }
}
