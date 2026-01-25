package com.sameerasw.essentials.viewmodels

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.MapsState
import com.sameerasw.essentials.domain.registry.SearchRegistry
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.data.repository.UpdateRepository
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingSide
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import com.sameerasw.essentials.domain.model.SearchableItem
import com.sameerasw.essentials.domain.model.UpdateInfo
import com.sameerasw.essentials.services.CaffeinateWakeLockService
import com.sameerasw.essentials.services.NotificationLightingService
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import com.sameerasw.essentials.services.receivers.SecurityDeviceAdminReceiver
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShizukuUtils
import com.sameerasw.essentials.utils.UpdateNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val pinnedFeatureKeys = mutableStateOf<List<String>>(emptyList())
    val isNotificationListenerEnabled = mutableStateOf(false)
    val isMapsPowerSavingEnabled = mutableStateOf(false)
    val isNotificationLightingEnabled = mutableStateOf(false)
    val isOverlayPermissionGranted = mutableStateOf(false)
    val isNotificationLightingAccessibilityEnabled = mutableStateOf(false)
    val hapticFeedbackType = mutableStateOf(HapticFeedbackType.SUBTLE)
    val defaultTab = mutableStateOf(com.sameerasw.essentials.domain.DIYTabs.ESSENTIALS)
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
    val snoozeChannels = mutableStateOf<List<com.sameerasw.essentials.domain.model.SnoozeChannel>>(emptyList())
    val isFlashlightAlwaysTurnOffEnabled = mutableStateOf(false)
    val isFlashlightFadeEnabled = mutableStateOf(false)
    val isFlashlightAdjustEnabled = mutableStateOf(false)
    val isFlashlightGlobalEnabled = mutableStateOf(false)
    val isFlashlightLiveUpdateEnabled = mutableStateOf(true)
    val flashlightLastIntensity = mutableStateOf(1)
    val isFlashlightPulseEnabled = mutableStateOf(false)
    val isFlashlightPulseFacedownOnly = mutableStateOf(true)
    val isLocationPermissionGranted = mutableStateOf(false)
    val isBackgroundLocationPermissionGranted = mutableStateOf(false)
    val isFullScreenIntentPermissionGranted = mutableStateOf(false)
    val isBluetoothPermissionGranted = mutableStateOf(false)
    
    val isBluetoothDevicesEnabled = mutableStateOf(false)
    val isCallVibrationsEnabled = mutableStateOf(false)



    val isScreenLockedSecurityEnabled = mutableStateOf(false)
    val isDeviceAdminEnabled = mutableStateOf(false)
    val isDeveloperModeEnabled = mutableStateOf(false)
    val skipSilentNotifications = mutableStateOf(true)
    val notificationLightingStyle = mutableStateOf(NotificationLightingStyle.STROKE)
    val notificationLightingColorMode = mutableStateOf(NotificationLightingColorMode.SYSTEM)
    val notificationLightingCustomColor = mutableIntStateOf(0xFF6200EE.toInt()) // Default purple
    val notificationLightingPulseCount = mutableStateOf(1f)
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
    val isRootEnabled = mutableStateOf(false)
    val isRootAvailable = mutableStateOf(false)
    val isRootPermissionGranted = mutableStateOf(false)

    val isPitchBlackThemeEnabled = mutableStateOf(false)
    
    // Keyboard Customization
    val keyboardHeight = mutableFloatStateOf(54f)
    val keyboardBottomPadding = mutableFloatStateOf(0f)
    val keyboardRoundness = mutableFloatStateOf(24f)
    val isKeyboardHapticsEnabled = mutableStateOf(true)
    val isKeyboardFunctionsBottom = mutableStateOf(false)
    val keyboardFunctionsPadding = mutableFloatStateOf(0f)
    val keyboardHapticStrength = mutableFloatStateOf(0.5f)
    val keyboardShape = mutableIntStateOf(0) // 0=Round, 1=Flat, 2=Inverse
    val isKeyboardAlwaysDark = mutableStateOf(false)
    val isKeyboardPitchBlack = mutableStateOf(false)
    val isKeyboardClipboardEnabled = mutableStateOf(true)
    val isKeyboardEnabled = mutableStateOf(false)
    val isKeyboardSelected = mutableStateOf(false)
    val isWriteSettingsEnabled = mutableStateOf(false)

    // AirSync Bridge
    val isAirSyncConnectionEnabled = mutableStateOf(false)
    val macBatteryLevel = mutableIntStateOf(-1)
    val isMacBatteryCharging = mutableStateOf(false)
    val macBatteryLastUpdated = mutableStateOf(0L)
    val isMacConnected = mutableStateOf(false)
    val batteryWidgetMaxDevices = mutableIntStateOf(8)
    val isBatteryWidgetBackgroundEnabled = mutableStateOf(true)

    private var lastUpdateCheckTime: Long = 0
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var updateRepository: UpdateRepository
    private var appContext: Context? = null

    private val preferenceChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // We still use this listener for now, attached via Repository
        if (key == null) return@OnSharedPreferenceChangeListener
        
        when (key) {
            SettingsRepository.KEY_EDGE_LIGHTING_ENABLED -> isNotificationLightingEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_DYNAMIC_NIGHT_LIGHT_ENABLED -> isDynamicNightLightEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_SCREEN_LOCKED_SECURITY_ENABLED -> isScreenLockedSecurityEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_MAPS_POWER_SAVING_ENABLED -> {
                isMapsPowerSavingEnabled.value = settingsRepository.getBoolean(key)
                MapsState.isEnabled = isMapsPowerSavingEnabled.value
            }
            SettingsRepository.KEY_STATUS_BAR_ICON_CONTROL_ENABLED -> isStatusBarIconControlEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_BUTTON_REMAP_ENABLED -> isButtonRemapEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_APP_LOCK_ENABLED -> isAppLockEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_FREEZE_WHEN_LOCKED_ENABLED -> isFreezeWhenLockedEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_FREEZE_LOCK_DELAY_INDEX -> freezeLockDelayIndex.intValue = settingsRepository.getInt(key, 1)
            SettingsRepository.KEY_FREEZE_AUTO_EXCLUDED_APPS -> {
                freezeAutoExcludedApps.value = settingsRepository.getFreezeAutoExcludedApps()
            }
            SettingsRepository.KEY_USE_ROOT -> isRootEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_CHECK_PRE_RELEASES_ENABLED -> isPreReleaseCheckEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_DEVELOPER_MODE_ENABLED -> {
                isDeveloperModeEnabled.value = settingsRepository.getBoolean(key)
            }
            SettingsRepository.KEY_PITCH_BLACK_THEME_ENABLED -> isPitchBlackThemeEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_KEYBOARD_HEIGHT -> keyboardHeight.floatValue = settingsRepository.getFloat(key, 54f)
            SettingsRepository.KEY_KEYBOARD_BOTTOM_PADDING -> keyboardBottomPadding.floatValue = settingsRepository.getFloat(key, 0f)
            SettingsRepository.KEY_KEYBOARD_ROUNDNESS -> keyboardRoundness.floatValue = settingsRepository.getFloat(key, 24f)
            SettingsRepository.KEY_KEYBOARD_HAPTICS_ENABLED -> isKeyboardHapticsEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_KEYBOARD_FUNCTIONS_BOTTOM -> isKeyboardFunctionsBottom.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_KEYBOARD_FUNCTIONS_PADDING -> keyboardFunctionsPadding.floatValue = settingsRepository.getFloat(key, 0f)
            SettingsRepository.KEY_KEYBOARD_HAPTIC_STRENGTH -> keyboardHapticStrength.floatValue = settingsRepository.getFloat(key, 0.5f)
            SettingsRepository.KEY_KEYBOARD_SHAPE -> keyboardShape.intValue = settingsRepository.getInt(key, 0)
            SettingsRepository.KEY_KEYBOARD_ALWAYS_DARK -> isKeyboardAlwaysDark.value = settingsRepository.getBoolean(key, false)
            SettingsRepository.KEY_KEYBOARD_PITCH_BLACK -> isKeyboardPitchBlack.value = settingsRepository.getBoolean(key, false)
            SettingsRepository.KEY_KEYBOARD_CLIPBOARD_ENABLED -> isKeyboardClipboardEnabled.value = settingsRepository.getBoolean(key, true)
            SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED -> isAirSyncConnectionEnabled.value = settingsRepository.getBoolean(key)
            SettingsRepository.KEY_MAC_BATTERY_LEVEL -> macBatteryLevel.intValue = settingsRepository.getInt(key, -1)
            SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING -> isMacBatteryCharging.value = settingsRepository.getBoolean(key, false)
            SettingsRepository.KEY_MAC_BATTERY_LAST_UPDATED -> macBatteryLastUpdated.value = settingsRepository.getLong(key, 0L)
            SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED -> isMacConnected.value = settingsRepository.getBoolean(key, false)
            SettingsRepository.KEY_BATTERY_WIDGET_MAX_DEVICES -> batteryWidgetMaxDevices.intValue = settingsRepository.getInt(key, 8)
            SettingsRepository.KEY_SNOOZE_DISCOVERED_CHANNELS, SettingsRepository.KEY_SNOOZE_BLOCKED_CHANNELS -> {
                appContext?.let { loadSnoozeChannels(it) }
            }
            SettingsRepository.KEY_PINNED_FEATURES -> {
                pinnedFeatureKeys.value = settingsRepository.getPinnedFeatures()
            }
            SettingsRepository.KEY_CALL_VIBRATIONS_ENABLED -> {
                isCallVibrationsEnabled.value = settingsRepository.getBoolean(key)
            }
            SettingsRepository.KEY_LIKE_SONG_TOAST_ENABLED -> {
                isLikeSongToastEnabled.value = settingsRepository.getBoolean(key)
            }
        }
    }

    fun check(context: Context) {
        appContext = context.applicationContext
        settingsRepository = SettingsRepository(context)
        updateRepository = UpdateRepository()
        
        isAccessibilityEnabled.value = PermissionUtils.isAccessibilityServiceEnabled(context)
        isWriteSecureSettingsEnabled.value = PermissionUtils.canWriteSecureSettings(context)
        isReadPhoneStateEnabled.value = PermissionUtils.hasReadPhoneStatePermission(context)
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
        isLocationPermissionGranted.value = PermissionUtils.hasLocationPermission(context)
        isBackgroundLocationPermissionGranted.value = PermissionUtils.hasBackgroundLocationPermission(context)
        isFullScreenIntentPermissionGranted.value = PermissionUtils.canUseFullScreenIntent(context)
        isKeyboardEnabled.value = PermissionUtils.isKeyboardEnabled(context)
        isKeyboardSelected.value = PermissionUtils.isKeyboardSelected(context)
        isWriteSettingsEnabled.value = PermissionUtils.canWriteSystemSettings(context)
        
        isBluetoothPermissionGranted.value = PermissionUtils.hasBluetoothPermission(context)
        
        settingsRepository.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        
        isWidgetEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_WIDGET_ENABLED)
        isStatusBarIconControlEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_STATUS_BAR_ICON_CONTROL_ENABLED)
        isMapsPowerSavingEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_MAPS_POWER_SAVING_ENABLED)
        isNotificationLightingEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_ENABLED)
        onlyShowWhenScreenOff.value = settingsRepository.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_ONLY_SCREEN_OFF, true)
        isAmbientDisplayEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_AMBIENT_DISPLAY)
        isAmbientShowLockScreenEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_AMBIENT_SHOW_LOCK_SCREEN)
        skipSilentNotifications.value = settingsRepository.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_SKIP_SILENT, true)
        skipPersistentNotifications.value = settingsRepository.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_SKIP_PERSISTENT)
        
        notificationLightingStyle.value = settingsRepository.getNotificationLightingStyle()
        notificationLightingColorMode.value = settingsRepository.getNotificationLightingColorMode()
        notificationLightingCustomColor.intValue = settingsRepository.getInt(SettingsRepository.KEY_EDGE_LIGHTING_CUSTOM_COLOR, 0xFF6200EE.toInt())
        notificationLightingPulseCount.value = settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_PULSE_COUNT, 1f)
        notificationLightingPulseDuration.value = settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_PULSE_DURATION, 3000f)
        notificationLightingIndicatorX.value = settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_X, 50f)
        notificationLightingIndicatorY.value = settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_Y, 2f)
        isRootEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_USE_ROOT)
        
        if (isRootEnabled.value) {
            isRootAvailable.value = com.sameerasw.essentials.utils.RootUtils.isRootAvailable()
            isRootPermissionGranted.value = com.sameerasw.essentials.utils.RootUtils.isRootPermissionGranted()
        } else {
             isRootAvailable.value = false
             isRootPermissionGranted.value = false
        }
        
        notificationLightingIndicatorScale.value = settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_SCALE, 1.0f)
        notificationLightingGlowSides.value = settingsRepository.getNotificationLightingGlowSides()
        
        MapsState.isEnabled = isMapsPowerSavingEnabled.value
        hapticFeedbackType.value = settingsRepository.getHapticFeedbackType()
        defaultTab.value = settingsRepository.getDIYTab()
        checkCaffeinateActive(context)
        
        // Button Remap & Migration
        isButtonRemapEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_BUTTON_REMAP_ENABLED, 
            settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_VOLUME_TOGGLE_ENABLED))
        isButtonRemapUseShizuku.value = settingsRepository.getBoolean(SettingsRepository.KEY_BUTTON_REMAP_USE_SHIZUKU)
        shizukuDetectedDevicePath.value = settingsRepository.getString(SettingsRepository.KEY_SHIZUKU_DETECTED_DEVICE_PATH)
            
        val oldTrigger = settingsRepository.getString(SettingsRepository.KEY_FLASHLIGHT_TRIGGER_BUTTON, "Volume Up")
        
        val hasLegacyToggle = settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_VOLUME_TOGGLE_ENABLED, false) // Default false here as key check logic
        
        volumeUpActionOff.value = settingsRepository.getString(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF, 
            settingsRepository.getString(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION, 
            if (oldTrigger == "Volume Up" && hasLegacyToggle) "Toggle flashlight" else "None")) ?: "None"
        
        volumeDownActionOff.value = settingsRepository.getString(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF, 
            settingsRepository.getString(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION, 
            if (oldTrigger == "Volume Down" && hasLegacyToggle) "Toggle flashlight" else "None")) ?: "None"
            
        volumeUpActionOn.value = settingsRepository.getString(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_ON, "None") ?: "None"
        volumeDownActionOn.value = settingsRepository.getString(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON, "None") ?: "None"
            
        val hapticName = settingsRepository.getString(SettingsRepository.KEY_BUTTON_REMAP_HAPTIC_TYPE, 
            settingsRepository.getString(SettingsRepository.KEY_FLASHLIGHT_HAPTIC_TYPE, HapticFeedbackType.DOUBLE.name))
        
        remapHapticType.value = try {
            val type = HapticFeedbackType.valueOf(hapticName ?: HapticFeedbackType.DOUBLE.name)
            if (type.name == "LONG") HapticFeedbackType.DOUBLE else type
        } catch (e: Exception) {
            HapticFeedbackType.DOUBLE
        }
        
        isDynamicNightLightEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_DYNAMIC_NIGHT_LIGHT_ENABLED)
        loadSnoozeChannels(context)
        isFlashlightAlwaysTurnOffEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_ALWAYS_TURN_OFF_ENABLED)
        isFlashlightFadeEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_FADE_ENABLED)
        isFlashlightAdjustEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_ADJUST_INTENSITY_ENABLED)
        isFlashlightGlobalEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_GLOBAL_ENABLED)
        isFlashlightLiveUpdateEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_LIVE_UPDATE_ENABLED, true)
        flashlightLastIntensity.value = settingsRepository.getInt(SettingsRepository.KEY_FLASHLIGHT_LAST_INTENSITY, 1)
        isFlashlightPulseEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_PULSE_ENABLED)
        isFlashlightPulseFacedownOnly.value = settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_PULSE_FACEDOWN_ONLY, true)
        isPitchBlackThemeEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_PITCH_BLACK_THEME_ENABLED)

        keyboardHeight.floatValue = settingsRepository.getFloat(SettingsRepository.KEY_KEYBOARD_HEIGHT, 54f)
        keyboardBottomPadding.floatValue = settingsRepository.getFloat(SettingsRepository.KEY_KEYBOARD_BOTTOM_PADDING, 0f)
        keyboardRoundness.floatValue = settingsRepository.getFloat(SettingsRepository.KEY_KEYBOARD_ROUNDNESS, 24f)
        isKeyboardHapticsEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_KEYBOARD_HAPTICS_ENABLED, true)
        isKeyboardFunctionsBottom.value = settingsRepository.getBoolean(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_BOTTOM, false)
        keyboardFunctionsPadding.floatValue = settingsRepository.getFloat(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_PADDING, 0f)
        keyboardHapticStrength.floatValue = settingsRepository.getFloat(SettingsRepository.KEY_KEYBOARD_HAPTIC_STRENGTH, 0.5f)
        keyboardShape.intValue = settingsRepository.getInt(SettingsRepository.KEY_KEYBOARD_SHAPE, 0)
        isKeyboardAlwaysDark.value = settingsRepository.getBoolean(SettingsRepository.KEY_KEYBOARD_ALWAYS_DARK, false)
        isKeyboardPitchBlack.value = settingsRepository.getBoolean(SettingsRepository.KEY_KEYBOARD_PITCH_BLACK, false)
        isKeyboardClipboardEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_KEYBOARD_CLIPBOARD_ENABLED, true)

        isAirSyncConnectionEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED)
        macBatteryLevel.intValue = settingsRepository.getInt(SettingsRepository.KEY_MAC_BATTERY_LEVEL, -1)
        isMacBatteryCharging.value = settingsRepository.getBoolean(SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING, false)
        macBatteryLastUpdated.value = settingsRepository.getLong(SettingsRepository.KEY_MAC_BATTERY_LAST_UPDATED, 0L)
        isMacConnected.value = settingsRepository.getBoolean(SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED, false)

        isBluetoothDevicesEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_SHOW_BLUETOOTH_DEVICES, false)
        isBluetoothDevicesEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_SHOW_BLUETOOTH_DEVICES, false)
        batteryWidgetMaxDevices.intValue = settingsRepository.getBatteryWidgetMaxDevices()
        isBatteryWidgetBackgroundEnabled.value = settingsRepository.isBatteryWidgetBackgroundEnabled()
        isCallVibrationsEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_CALL_VIBRATIONS_ENABLED)

        isScreenLockedSecurityEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_SCREEN_LOCKED_SECURITY_ENABLED)
        isDeviceAdminEnabled.value = isDeviceAdminActive(context)
        
        isAutoUpdateEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_AUTO_UPDATE_ENABLED, true)
        isUpdateNotificationEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_UPDATE_NOTIFICATION_ENABLED, true)
        lastUpdateCheckTime = settingsRepository.getLong(SettingsRepository.KEY_LAST_UPDATE_CHECK_TIME)
        isAppLockEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_APP_LOCK_ENABLED)
        isFreezeWhenLockedEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_FREEZE_WHEN_LOCKED_ENABLED)
        freezeLockDelayIndex.intValue = settingsRepository.getInt(SettingsRepository.KEY_FREEZE_LOCK_DELAY_INDEX, 1)
        freezeAutoExcludedApps.value = settingsRepository.getFreezeAutoExcludedApps()
        isDeveloperModeEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_DEVELOPER_MODE_ENABLED)
        isPreReleaseCheckEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_CHECK_PRE_RELEASES_ENABLED)
        pinnedFeatureKeys.value = settingsRepository.getPinnedFeatures()
        isLikeSongToastEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_LIKE_SONG_TOAST_ENABLED, true)
    }

    fun onSearchQueryChanged(query: String, context: Context) {
        searchQuery.value = query
        if (query.isBlank()) {
            searchResults.value = emptyList()
            isSearching.value = false
            return
        }

        isSearching.value = true
        searchResults.value = SearchRegistry.search(context, query)
        isSearching.value = false
    }

    fun togglePinFeature(featureId: String) {
        val current = pinnedFeatureKeys.value.toMutableList()
        if (current.contains(featureId)) {
            current.remove(featureId)
        } else {
            current.add(featureId) // Append at the end to keep order
        }
        pinnedFeatureKeys.value = current
        settingsRepository.savePinnedFeatures(current)
    }

    fun setAutoUpdateEnabled(enabled: Boolean, context: Context) {
        isAutoUpdateEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_AUTO_UPDATE_ENABLED, enabled)
    }

    fun setUpdateNotificationEnabled(enabled: Boolean, context: Context) {
        isUpdateNotificationEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_UPDATE_NOTIFICATION_ENABLED, enabled)
    }

    fun setPreReleaseCheckEnabled(enabled: Boolean, context: Context) {
        isPreReleaseCheckEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_CHECK_PRE_RELEASES_ENABLED, enabled)
    }

    fun setDeveloperModeEnabled(enabled: Boolean, context: Context) {
        isDeveloperModeEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_DEVELOPER_MODE_ENABLED, enabled)
    }

    fun setRootEnabled(enabled: Boolean, context: Context) {
        settingsRepository.putBoolean(SettingsRepository.KEY_USE_ROOT, enabled)
        isRootEnabled.value = enabled
        check(context)
    }

    fun setPitchBlackThemeEnabled(enabled: Boolean, context: Context) {
        isPitchBlackThemeEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_PITCH_BLACK_THEME_ENABLED, enabled)
    }

    fun checkForUpdates(context: Context, manual: Boolean = false) {
        if (isCheckingUpdate.value) return
        
        if (!manual) {
            if (!isAutoUpdateEnabled.value) return
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateCheckTime < 900000) return
        }

        isCheckingUpdate.value = true
        updateInfo.value = null // Clear stale data before checking
        viewModelScope.launch {
            try {
                val currentVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) {
                    "0.0"
                } ?: "0.0"
                
                val updateInfoResult = updateRepository.checkForUpdates(isPreReleaseCheckEnabled.value, currentVersion)

                if (updateInfoResult != null) {
                    updateInfo.value = updateInfoResult
                    isUpdateAvailable.value = updateInfoResult.isUpdateAvailable
                    
                    if (updateInfoResult.isUpdateAvailable && updateInfoResult.downloadUrl.isNotEmpty()) {
                        if (isUpdateNotificationEnabled.value) {
                            UpdateNotificationHelper.showUpdateNotification(context, updateInfoResult.versionName, updateInfoResult.downloadUrl)
                        }
                    }
                    
                    lastUpdateCheckTime = System.currentTimeMillis()
                    settingsRepository.putLong(SettingsRepository.KEY_LAST_UPDATE_CHECK_TIME, lastUpdateCheckTime)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isCheckingUpdate.value = false
            }
        }
    }

    private fun isDeviceAdminActive(context: Context): Boolean {
        return PermissionUtils.isDeviceAdminActive(context)
    }

    fun requestDeviceAdmin(context: Context) {
        val adminComponent = ComponentName(context, SecurityDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getString(R.string.perm_device_admin_explanation))
        }
        if (context is Activity) {
            context.startActivity(intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    fun requestReadPhoneStatePermission(activity: Activity) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.READ_PHONE_STATE),
            1005
        )
    }


    fun setWidgetEnabled(enabled: Boolean, context: Context) {
        isWidgetEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_WIDGET_ENABLED, enabled)
    }

    fun setStatusBarIconControlEnabled(enabled: Boolean, context: Context) {
        isStatusBarIconControlEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_STATUS_BAR_ICON_CONTROL_ENABLED, enabled)
    }

    fun setMapsPowerSavingEnabled(enabled: Boolean, context: Context) {
        isMapsPowerSavingEnabled.value = enabled
        MapsState.isEnabled = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_MAPS_POWER_SAVING_ENABLED, enabled)
    }

    fun setNotificationLightingEnabled(enabled: Boolean, context: Context) {
        isNotificationLightingEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_EDGE_LIGHTING_ENABLED, enabled)
    }

    fun setOnlyShowWhenScreenOff(enabled: Boolean, context: Context) {
        onlyShowWhenScreenOff.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_EDGE_LIGHTING_ONLY_SCREEN_OFF, enabled)
    }

    fun setAmbientDisplayEnabled(enabled: Boolean, context: Context) {
        isAmbientDisplayEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_EDGE_LIGHTING_AMBIENT_DISPLAY, enabled)
    }

    fun setAmbientShowLockScreenEnabled(enabled: Boolean, context: Context) {
        isAmbientShowLockScreenEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_EDGE_LIGHTING_AMBIENT_SHOW_LOCK_SCREEN, enabled)
    }
    
    fun setSkipSilentNotifications(enabled: Boolean, context: Context) {
        skipSilentNotifications.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_EDGE_LIGHTING_SKIP_SILENT, enabled)
    }

    fun setSkipPersistentNotifications(enabled: Boolean, context: Context) {
        skipPersistentNotifications.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_EDGE_LIGHTING_SKIP_PERSISTENT, enabled)
    }

    fun setNotificationLightingStyle(style: NotificationLightingStyle, context: Context) {
        notificationLightingStyle.value = style
        settingsRepository.putString(SettingsRepository.KEY_EDGE_LIGHTING_STYLE, style.name)
    }

    fun setNotificationLightingColorMode(mode: NotificationLightingColorMode, context: Context) {
        notificationLightingColorMode.value = mode
        settingsRepository.putString(SettingsRepository.KEY_EDGE_LIGHTING_COLOR_MODE, mode.name)
    }

    fun setNotificationLightingCustomColor(color: Int, context: Context) {
        notificationLightingCustomColor.intValue = color
        settingsRepository.putInt(SettingsRepository.KEY_EDGE_LIGHTING_CUSTOM_COLOR, color)
    }

    fun setButtonRemapEnabled(enabled: Boolean, context: Context) {
        isButtonRemapEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_BUTTON_REMAP_ENABLED, enabled)
    }

    fun setCallVibrationsEnabled(enabled: Boolean) {
        isCallVibrationsEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_CALL_VIBRATIONS_ENABLED, enabled)
    }

    fun setButtonRemapUseShizuku(enabled: Boolean, context: Context) {
        isButtonRemapUseShizuku.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_BUTTON_REMAP_USE_SHIZUKU, enabled)
    }

    fun setVolumeUpActionOff(action: String, context: Context) {
        volumeUpActionOff.value = action
        settingsRepository.putString(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF, action)
    }

    fun setVolumeDownActionOff(action: String, context: Context) {
        volumeDownActionOff.value = action
        settingsRepository.putString(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF, action)
    }

    fun setVolumeUpActionOn(action: String, context: Context) {
        volumeUpActionOn.value = action
        settingsRepository.putString(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_ON, action)
    }

    fun setVolumeDownActionOn(action: String, context: Context) {
        volumeDownActionOn.value = action
        settingsRepository.putString(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON, action)
    }

    fun setRemapHapticType(type: HapticFeedbackType, context: Context) {
        remapHapticType.value = type
        settingsRepository.putString(SettingsRepository.KEY_BUTTON_REMAP_HAPTIC_TYPE, type.name)
    }

    fun setDynamicNightLightEnabled(enabled: Boolean, context: Context) {
        isDynamicNightLightEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_DYNAMIC_NIGHT_LIGHT_ENABLED, enabled)
    }

    fun setAppLockEnabled(enabled: Boolean, context: Context) {
        isAppLockEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_APP_LOCK_ENABLED, enabled)
    }
    
    val isLikeSongToastEnabled = mutableStateOf(false)

    fun setLikeSongToastEnabled(enabled: Boolean) {
        isLikeSongToastEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_LIKE_SONG_TOAST_ENABLED, enabled)
    }

    fun setFreezeWhenLockedEnabled(enabled: Boolean, context: Context) {
        isFreezeWhenLockedEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FREEZE_WHEN_LOCKED_ENABLED, enabled)
    }

    fun setFreezeLockDelayIndex(index: Int, context: Context) {
        freezeLockDelayIndex.intValue = index
        settingsRepository.putInt(SettingsRepository.KEY_FREEZE_LOCK_DELAY_INDEX, index)
    }

    fun saveNotificationLightingPulseCount(context: Context, count: Float) {
        notificationLightingPulseCount.value = count
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_PULSE_COUNT, count)
    }

    fun saveNotificationLightingPulseDuration(context: Context, duration: Float) {
        notificationLightingPulseDuration.value = duration
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_PULSE_DURATION, duration)
    }

    fun setFlashlightPulseEnabled(enabled: Boolean, context: Context) {
        isFlashlightPulseEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FLASHLIGHT_PULSE_ENABLED, enabled)
    }

    fun setFlashlightPulseFacedownOnly(enabled: Boolean, context: Context) {
        isFlashlightPulseFacedownOnly.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FLASHLIGHT_PULSE_FACEDOWN_ONLY, enabled)
    }

    // Helper to show the overlay service for testing/triggering
    fun triggerNotificationLighting(context: Context) {
        val radius = settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_CORNER_RADIUS, 20f)
        val thickness = settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_STROKE_THICKNESS, 8f)
        try {
            val intent = Intent(context, com.sameerasw.essentials.services.NotificationLightingService::class.java).apply {
                putExtra("corner_radius_dp", radius)
                putExtra("stroke_thickness_dp", thickness)
                putExtra("ignore_screen_state", true)
                putExtra("style", notificationLightingStyle.value.name)
                putExtra("color_mode", notificationLightingColorMode.value.name)
                putExtra("custom_color", notificationLightingCustomColor.intValue)
                putExtra("pulse_count", notificationLightingPulseCount.value.toInt())
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
    fun triggerNotificationLightingWithRadius(context: Context, cornerRadiusDp: Float) {
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
    
    fun openImeSettings(context: Context) {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun showImePicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }
    fun triggerNotificationLightingWithRadiusAndThickness(context: Context, cornerRadiusDp: Float, strokeThicknessDp: Float) {
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

    
    fun triggerNotificationLightingForIndicator(context: Context, x: Float, y: Float, scale: Float) {
        try {
            val intent = Intent(context, com.sameerasw.essentials.services.NotificationLightingService::class.java).apply {
                putExtra("indicator_x", x)
                putExtra("indicator_y", y)
                putExtra("indicator_scale", scale)
                putExtra("is_preview", true)
                putExtra("ignore_screen_state", true)
                putExtra("style", NotificationLightingStyle.INDICATOR.name)
                putExtra("color_mode", notificationLightingColorMode.value.name)
                putExtra("custom_color", notificationLightingCustomColor.intValue)
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
                action = "SHOW_NOTIFICATION_LIGHTING"
                putExtra("remove_preview", true)
            }
            context.startService(intent2)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun setHapticFeedback(type: HapticFeedbackType, context: Context) {
        hapticFeedbackType.value = type
        settingsRepository.putString(SettingsRepository.KEY_HAPTIC_FEEDBACK_TYPE, type.name)
    }

    fun setDefaultTab(tab: com.sameerasw.essentials.domain.DIYTabs, context: Context) {
        defaultTab.value = tab
        settingsRepository.saveDIYTab(tab)
        settingsRepository.saveDIYTab(tab)
    }

    fun setKeyboardHeight(height: Float, context: Context) {
        keyboardHeight.floatValue = height
        settingsRepository.putFloat(SettingsRepository.KEY_KEYBOARD_HEIGHT, height)
    }

    fun setKeyboardBottomPadding(padding: Float, context: Context) {
        keyboardBottomPadding.floatValue = padding
        settingsRepository.putFloat(SettingsRepository.KEY_KEYBOARD_BOTTOM_PADDING, padding)
    }

    fun setKeyboardRoundness(roundness: Float, context: Context) {
        keyboardRoundness.floatValue = roundness
        settingsRepository.putFloat(SettingsRepository.KEY_KEYBOARD_ROUNDNESS, roundness)
    }

    fun setKeyboardHapticsEnabled(enabled: Boolean, context: Context) {
        isKeyboardHapticsEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_KEYBOARD_HAPTICS_ENABLED, enabled)
    }

    fun setKeyboardFunctionsBottom(isBottom: Boolean, context: Context) {
        isKeyboardFunctionsBottom.value = isBottom
        settingsRepository.putBoolean(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_BOTTOM, isBottom)
    }

    fun setKeyboardFunctionsPadding(padding: Float, context: Context) {
        keyboardFunctionsPadding.floatValue = padding
        settingsRepository.putFloat(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_PADDING, padding)
    }

    fun setKeyboardHapticStrength(strength: Float, context: Context) {
        keyboardHapticStrength.floatValue = strength
        settingsRepository.putFloat(SettingsRepository.KEY_KEYBOARD_HAPTIC_STRENGTH, strength)
    }

    fun setKeyboardShape(shape: Int, context: Context) {
        keyboardShape.intValue = shape
        settingsRepository.putInt(SettingsRepository.KEY_KEYBOARD_SHAPE, shape)
    }

    fun setKeyboardAlwaysDark(enabled: Boolean, context: Context) {
        isKeyboardAlwaysDark.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_KEYBOARD_ALWAYS_DARK, enabled)
    }

    fun setKeyboardPitchBlack(enabled: Boolean, context: Context) {
        isKeyboardPitchBlack.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_KEYBOARD_PITCH_BLACK, enabled)
    }

    fun setKeyboardClipboardEnabled(enabled: Boolean, context: Context) {
        isKeyboardClipboardEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_KEYBOARD_CLIPBOARD_ENABLED, enabled)
    }

    fun setAirSyncConnectionEnabled(enabled: Boolean, context: Context) {
        if (enabled) {
            // Request permission if not granted, though it's signature level so should be automatic if signed correctly
            // but we can check it
        }
        isAirSyncConnectionEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED, enabled)
    }

    fun setBluetoothDevicesEnabled(enabled: Boolean, context: Context) {
        isBluetoothDevicesEnabled.value = enabled
        settingsRepository.setBluetoothDevicesEnabled(enabled)
        
        // Trigger widget update to fetch data immediately
        val intent = Intent(context, com.sameerasw.essentials.services.widgets.BatteriesWidgetReceiver::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        context.sendBroadcast(intent)
    }

    fun setBatteryWidgetMaxDevices(count: Int, context: Context) {
        batteryWidgetMaxDevices.intValue = count
        settingsRepository.setBatteryWidgetMaxDevices(count)
        
        // Trigger widget update
        val intent = Intent(context, com.sameerasw.essentials.services.widgets.BatteriesWidgetReceiver::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        context.sendBroadcast(intent)
    }

    fun setBatteryWidgetBackgroundEnabled(enabled: Boolean, context: Context) {
        isBatteryWidgetBackgroundEnabled.value = enabled
        settingsRepository.setBatteryWidgetBackgroundEnabled(enabled)
        
        // Trigger widget update
        val intent = Intent(context, com.sameerasw.essentials.services.widgets.BatteriesWidgetReceiver::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        context.sendBroadcast(intent)
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

    fun requestLocationPermission(activity: androidx.activity.ComponentActivity) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            1003
        )
    }

    fun requestBackgroundLocationPermission(activity: androidx.activity.ComponentActivity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            androidx.core.app.ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                1004
            )
        }
    }

    fun requestBluetoothPermission(activity: androidx.activity.ComponentActivity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            androidx.core.app.ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                1005
            )
        }
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

    fun requestFullScreenIntentPermission(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to special app access
                val intent = Intent(Settings.ACTION_CONDITION_PROVIDER_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
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
        settingsRepository.saveNotificationLightingSelectedApps(apps)
    }

    fun loadNotificationLightingSelectedApps(context: Context): List<AppSelection> {
        return settingsRepository.loadNotificationLightingSelectedApps()
    }

    fun updateNotificationLightingAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        settingsRepository.updateNotificationLightingAppSelection(packageName, enabled)
    }

    // Notification Lighting Corner Radius Methods
    fun saveNotificationLightingCornerRadius(context: Context, radiusDp: Float) {
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_CORNER_RADIUS, radiusDp)
    }

    fun loadNotificationLightingCornerRadius(context: Context): Float {
        return settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_CORNER_RADIUS, 20f)
    }

    // Notification Lighting Stroke Thickness Methods
    fun saveNotificationLightingStrokeThickness(context: Context, thicknessDp: Float) {
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_STROKE_THICKNESS, thicknessDp)
    }

    fun loadNotificationLightingStrokeThickness(context: Context): Float {
        return settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_STROKE_THICKNESS, 8f)
    }

    // Dynamic Night Light App Selection Methods
    fun saveDynamicNightLightSelectedApps(context: Context, apps: List<AppSelection>) {
        settingsRepository.saveDynamicNightLightSelectedApps(apps)
    }

    fun loadDynamicNightLightSelectedApps(context: Context): List<AppSelection> {
        return settingsRepository.loadDynamicNightLightSelectedApps()
    }

    fun updateDynamicNightLightAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        settingsRepository.updateDynamicNightLightAppSelection(packageName, enabled)
    }

    // App Lock App Selection Methods
    fun saveAppLockSelectedApps(context: Context, apps: List<AppSelection>) {
        settingsRepository.saveAppLockSelectedApps(apps)
    }

    fun loadAppLockSelectedApps(context: Context): List<AppSelection> {
        return settingsRepository.loadAppLockSelectedApps()
    }

    fun updateAppLockAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        settingsRepository.updateAppLockAppSelection(packageName, enabled)
    }

    // Freeze App Selection Methods
    fun saveFreezeSelectedApps(context: Context, apps: List<AppSelection>) {
        settingsRepository.saveFreezeSelectedApps(apps)
        refreshFreezePickedApps(context, silent = false) // Full refresh if list structure changes significantly
    }

    fun loadFreezeSelectedApps(context: Context): List<AppSelection> {
        return settingsRepository.loadFreezeSelectedApps()
    }

    fun updateFreezeAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        settingsRepository.updateFreezeAppSelection(packageName, enabled)
        refreshFreezePickedApps(context, silent = true)
    }

    fun updateFreezeAppAutoFreeze(context: Context, packageName: String, autoFreezeEnabled: Boolean) {
        val currentSet = freezeAutoExcludedApps.value.toMutableSet()
        if (autoFreezeEnabled) {
            currentSet.remove(packageName)
        } else {
            currentSet.add(packageName)
        }
        freezeAutoExcludedApps.value = currentSet
        
        settingsRepository.saveFreezeAutoExcludedApps(currentSet)
        
        refreshFreezePickedApps(context, silent = true)
    }

    fun refreshFreezePickedApps(context: Context, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) isFreezePickedAppsLoading.value = true
            try {
                // Background processing for heavy list operations
                val result = withContext(Dispatchers.Default) {
                    // Only load apps that are actually marked as secondary selected (picked)
                    val selections = loadFreezeSelectedApps(context).filter { it.isEnabled }
                    if (selections.isEmpty()) return@withContext emptyList()
                    
                    // Efficiently load only the apps that are actually marked as secondary selected (picked)
                    val pickedPkgNames = selections.map { it.packageName }
                    val relevantApps = AppUtil.getAppsByPackageNames(context, pickedPkgNames)
                    
                    val merged = AppUtil.mergeWithSavedApps(relevantApps, selections)
                    val currentExcluded = freezeAutoExcludedApps.value
                    
                    // Cleanup: remove package names that are no longer picked (still on main because it updates state)
                    val filteredExcluded = currentExcluded.filter { pickedPkgNames.contains(it) }.toSet()
                    
                    // Prepare final list in background
                    merged.map { it.copy(isEnabled = !filteredExcluded.contains(it.packageName)) }
                        .sortedBy { it.appName.lowercase() }
                }

                // Final state update on Main
                freezePickedApps.value = result
                
                // Exclude check (this part still needs to update state if cleaned up)
                val currentExcluded = freezeAutoExcludedApps.value
                val selections = loadFreezeSelectedApps(context).filter { it.isEnabled }
                val pickedPkgNames = selections.map { it.packageName }
                val filteredExcluded = currentExcluded.filter { pickedPkgNames.contains(it) }.toSet()
                if (filteredExcluded.size != currentExcluded.size) {
                    freezeAutoExcludedApps.value = filteredExcluded
                    settingsRepository.saveFreezeAutoExcludedApps(filteredExcluded)
                }
            } finally {
                if (!silent) isFreezePickedAppsLoading.value = false
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
                com.sameerasw.essentials.utils.FreezeManager.unfreezeApp(context, packageName)
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

    fun loadSnoozeChannels(context: Context) {
        val discovered = settingsRepository.loadSnoozeDiscoveredChannels()
        val blocked = settingsRepository.loadSnoozeBlockedChannels()
        
        val channels = discovered.map { channel ->
            channel.copy(isBlocked = blocked.contains(channel.id))
        }
        
        snoozeChannels.value = channels.distinctBy { it.id }.sortedBy { it.name }
    }

    fun setSnoozeChannelBlocked(channelId: String, blocked: Boolean, context: Context) {
        val currentBlocked = settingsRepository.loadSnoozeBlockedChannels().toMutableSet()
        if (blocked) {
            currentBlocked.add(channelId)
        } else {
            currentBlocked.remove(channelId)
        }
        settingsRepository.saveSnoozeBlockedChannels(currentBlocked)
        loadSnoozeChannels(context)
    }

    fun setFlashlightAlwaysTurnOffEnabled(enabled: Boolean, context: Context) {
        isFlashlightAlwaysTurnOffEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FLASHLIGHT_ALWAYS_TURN_OFF_ENABLED, enabled)
    }

    fun setFlashlightFadeEnabled(enabled: Boolean, context: Context) {
        isFlashlightFadeEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FLASHLIGHT_FADE_ENABLED, enabled)
    }

    fun setFlashlightAdjustEnabled(enabled: Boolean, context: Context) {
        isFlashlightAdjustEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FLASHLIGHT_ADJUST_INTENSITY_ENABLED, enabled)
    }

    fun setFlashlightGlobalEnabled(enabled: Boolean, context: Context) {
        isFlashlightGlobalEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FLASHLIGHT_GLOBAL_ENABLED, enabled)
    }

    fun setFlashlightLiveUpdateEnabled(enabled: Boolean, context: Context) {
        isFlashlightLiveUpdateEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FLASHLIGHT_LIVE_UPDATE_ENABLED, enabled)
    }

    fun setFlashlightLastIntensity(intensity: Int, context: Context) {
        flashlightLastIntensity.value = intensity
        settingsRepository.putInt(SettingsRepository.KEY_FLASHLIGHT_LAST_INTENSITY, intensity)
    }






    fun setScreenLockedSecurityEnabled(enabled: Boolean, context: Context) {
        isScreenLockedSecurityEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_SCREEN_LOCKED_SECURITY_ENABLED, enabled)
    }

    fun setNotificationLightingGlowSides(sides: Set<NotificationLightingSide>, context: Context) {
        notificationLightingGlowSides.value = sides
        settingsRepository.saveNotificationLightingGlowSides(sides)
    }

    fun saveNotificationLightingIndicatorX(context: Context, x: Float) {
        notificationLightingIndicatorX.value = x
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_X, x)
    }

    fun saveNotificationLightingIndicatorY(context: Context, y: Float) {
        notificationLightingIndicatorY.value = y
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_Y, y)
    }

    fun saveNotificationLightingIndicatorScale(context: Context, scale: Float) {
        notificationLightingIndicatorScale.value = scale
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_SCALE, scale)
    }



    fun exportConfigs(context: Context, outputStream: java.io.OutputStream) {
        settingsRepository.exportConfigs(outputStream)
    }

    fun importConfigs(context: Context, inputStream: java.io.InputStream): Boolean {
        val success = settingsRepository.importConfigs(inputStream)
        if (success) {
            check(context)
        }
        return success
    }


    fun generateBugReport(context: Context): String {
        val settingsJson = settingsRepository.getAllConfigsAsJsonString()
        return com.sameerasw.essentials.utils.LogManager.generateReport(context, settingsJson)
    }

}
