/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Viewmodels
 * File: MainViewModel.kt
 * Description: Component file for MainViewModel.kt.
 */

package com.sameerasw.essentials.viewmodels

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.CalendarContract
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.data.repository.UpdateRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.MapsState
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.AppIcon
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.AppStandbyInfo
import com.sameerasw.essentials.domain.model.DnsPreset
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingSide
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import com.sameerasw.essentials.domain.model.NotificationLightingSweepPosition
import com.sameerasw.essentials.domain.model.ScaleAnimationsProfile
import com.sameerasw.essentials.domain.model.SearchableItem
import com.sameerasw.essentials.domain.model.UpdateInfo
import com.sameerasw.essentials.domain.registry.SearchRegistry
import com.sameerasw.essentials.services.AppUpdateWorker
import com.sameerasw.essentials.services.CaffeinateWakeLockService
import com.sameerasw.essentials.services.NotificationLightingService
import com.sameerasw.essentials.services.receivers.FlashlightActionReceiver
import com.sameerasw.essentials.services.receivers.SecurityDeviceAdminReceiver
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import com.sameerasw.essentials.utils.AppIconUtil
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.RefreshRateUtils
import com.sameerasw.essentials.utils.RootUtils
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.utils.ShizukuUtils
import com.sameerasw.essentials.utils.SurfaceFlingerControl
import com.sameerasw.essentials.utils.UpdateNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

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
    val pinnedQsTileKeys = mutableStateOf<List<String>>(emptyList())
    val isNotificationListenerEnabled = mutableStateOf(false)
    val isMapsPowerSavingEnabled = mutableStateOf(false)
    val isNotificationLightingEnabled = mutableStateOf(false)
    val isOverlayPermissionGranted = mutableStateOf(false)
    val isNotificationLightingAccessibilityEnabled = mutableStateOf(false)
    val hapticFeedbackType = mutableStateOf(HapticFeedbackType.SUBTLE)
    val defaultTab = mutableStateOf(com.sameerasw.essentials.domain.DIYTabs.ESSENTIALS)
    val selectedAppIcon = mutableStateOf(AppIcon.DEFAULT)
    val isDefaultBrowserSet = mutableStateOf(false)
    val onlyShowWhenScreenOff = mutableStateOf(true)
    val isAmbientDisplayEnabled = mutableStateOf(false)
    val isAmbientShowLockScreenEnabled = mutableStateOf(false)
    val isButtonRemapEnabled = mutableStateOf(false)
    val isButtonRemapUseShizuku = mutableStateOf(false)
    val shizukuDetectedDevicePath = mutableStateOf<String?>(null)
    val volumeUpActionOff = mutableStateOf<Action?>(null)
    val volumeDownActionOff = mutableStateOf<Action?>(null)
    val volumeUpActionOn = mutableStateOf<Action?>(null)
    val volumeDownActionOn = mutableStateOf<Action?>(null)
    val remapHapticType = mutableStateOf(HapticFeedbackType.DOUBLE)
    val isDynamicNightLightEnabled = mutableStateOf(false)
    val isSmartPixelsEnabled = mutableStateOf(false)
    val smartPixelsIntensity = mutableFloatStateOf(50f)
    val isSmartPixelsDisableOnCastEnabled = mutableStateOf(true)
    val snoozeChannels =
        mutableStateOf<List<com.sameerasw.essentials.domain.model.SnoozeChannel>>(emptyList())
    val mapsChannels =
        mutableStateOf<List<com.sameerasw.essentials.domain.model.MapsChannel>>(emptyList())
    val isSnoozeHeadsUpEnabled = mutableStateOf(false)
    val isFlashlightAlwaysTurnOffEnabled = mutableStateOf(false)
    val isFlashlightFadeEnabled = mutableStateOf(false)
    val isFlashlightAdjustEnabled = mutableStateOf(false)
    val isFlashlightGlobalEnabled = mutableStateOf(false)
    val isFlashlightLiveUpdateEnabled = mutableStateOf(true)
    val flashlightLastIntensity = mutableStateOf(1)
    val isFlashlightPulseEnabled = mutableStateOf(false)
    val isFlashlightPulseFacedownOnly = mutableStateOf(true)
    val isFlashlightPulseUseLightingApps = mutableStateOf(true)
    val flashlightPulseMaxIntensity = mutableFloatStateOf(0.5f)
    val isFlashlightPulseDisableOnDnd = mutableStateOf(true)
    val isFlashlightPocketTurnOffEnabled = mutableStateOf(false)
    val isFlashlightOverheatEnabled = mutableStateOf(true)
    val isLocationPermissionGranted = mutableStateOf(false)
    val isBackgroundLocationPermissionGranted = mutableStateOf(false)
    val isFullScreenIntentPermissionGranted = mutableStateOf(false)
    val isBluetoothPermissionGranted = mutableStateOf(false)
    val isUsageStatsPermissionGranted = mutableStateOf(false)
    val appLanguage = mutableStateOf("en")

    val isBluetoothDevicesEnabled = mutableStateOf(false)
    val isCallVibrationsEnabled = mutableStateOf(false)
    val isCalendarSyncEnabled = mutableStateOf(false)
    val isNotificationSyncEnabled = mutableStateOf(false)
    val isCallSyncEnabled = mutableStateOf(true)
    val isCalendarSyncPeriodicEnabled = mutableStateOf(false)
    val isBatteryNotificationEnabled = mutableStateOf(false)
    val isAodEnabled = mutableStateOf(false)
    val isNotificationGlanceEnabled = mutableStateOf(false)
    val isAodForceTurnOffEnabled = mutableStateOf(false)
    val isPocketModeEnabled = mutableStateOf(false)
    val isPocketModeUseLightSensor = mutableStateOf(false)
    val pocketModeTriggerDelay = mutableFloatStateOf(3f) // seconds
    val isPocketModeLockScreenOnly = mutableStateOf(false)
    val isAutoAccessibilityEnabled = mutableStateOf(false)
    val isNotificationGlanceSameAsLightingEnabled = mutableStateOf(true)
    val isOnboardingCompleted =
        mutableStateOf(true) // Default to true so it doesn't flash on first check if not loaded
    val isWhatsNewVisible = mutableStateOf(false)
    val dnsPresets = mutableStateListOf<DnsPreset>()
    val addedQSTiles = mutableStateOf<Set<String>>(emptySet())
    val isHideGestureBarEnabled = mutableStateOf(false)
    val isHideGestureBarOnLauncherEnabled = mutableStateOf(false)
    val isCircleToSearchGestureEnabled = mutableStateOf(false)
    val circleToSearchGestureHeight = mutableFloatStateOf(48f)
    val circleToSearchGestureWidth = mutableFloatStateOf(240f)
    val isCircleToSearchPreviewEnabled = mutableStateOf(false)
    val isDisableRotationSuggestionEnabled = mutableStateOf(false)
    val isAllowOverlaysInSettingsEnabled = mutableStateOf(false)
    val networkDownloadRateLimit = mutableIntStateOf(-1)
    val isMobileDataAlwaysOnEnabled = mutableStateOf(false)
    val isWirelessDisplayCertificationEnabled = mutableStateOf(false)
    val isTransparentNavigationBarEnabled = mutableStateOf(false)
    val isPreferGpuComposingEnabled = mutableStateOf(false)
    val standbyAppsList = mutableStateOf<List<AppStandbyInfo>>(emptyList())
    val isStandbyAppsLoading = mutableStateOf(false)
    val isPixelSearchbarEnabled = mutableStateOf(false)
    val pixelSearchbarType = mutableStateOf("empty")
    val pixelSearchbarDateFormat = mutableStateOf("EEEE, MMMM d")
    val pixelSearchbarBackgroundPill = mutableStateOf(false)
    val pixelSearchbarWidgetId =
        mutableIntStateOf(android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID)
    val pixelSearchbarWidgetProvider = mutableStateOf<String?>(null)
    val pixelSearchbarScrapedLine1 = mutableStateOf("")
    val pixelSearchbarScrapedLine2 = mutableStateOf("")
    val pixelSearchbarWidgetPaddingH = mutableIntStateOf(0)
    val pixelSearchbarWidgetPaddingV = mutableIntStateOf(0)
    val pixelSearchbarTapActionEnabled = mutableStateOf(true)
    val pixelSearchbarMusicTitle = mutableStateOf("")
    val pixelSearchbarMusicArtist = mutableStateOf("")
    val pixelSearchbarMusicPackage = mutableStateOf("")
    val lockScreenClockId = mutableStateOf<String?>(null)
    val lockScreenClockWeight = mutableIntStateOf(300)
    val lockScreenClockWidth = mutableIntStateOf(116)
    val lockScreenClockGrade = mutableIntStateOf(0)
    val lockScreenClockRoundness = mutableIntStateOf(100)
    val lockScreenClockColorTone = mutableIntStateOf(75)
    val lockScreenClockSelectedColorId = mutableStateOf("DEFAULT")
    val lockScreenClockSeedColor = mutableIntStateOf(0)

    // Live Wallpaper
    val liveWallpaperSelectedVideo = mutableStateOf(SettingsRepository.LIVE_WALLPAPER_DEFAULT_VIDEO)
    val liveWallpaperPlaybackTrigger =
        mutableStateOf(SettingsRepository.LIVE_WALLPAPER_TRIGGER_UNLOCK)
    val liveWallpaperCustomVideos = mutableStateListOf<String>()

    val shutUpConfigs =
        mutableStateOf<List<com.sameerasw.essentials.domain.model.ShutUpAppConfig>>(emptyList())
    val isShutUpLoading = mutableStateOf(false)
    val isShutUpAttemptShizukuRestart = mutableStateOf(true)
    val shutUpRestoreDelay = mutableIntStateOf(10)
    val shutUpRestoreMode = mutableStateOf("Auto")
    val shizukuAuthToken = mutableStateOf("")
    val edgeLightingSweepSelectedShapes = mutableStateOf<Set<String>>(emptySet())

    data class CalendarAccount(
        val id: Long,
        val name: String,
        val accountName: String,
        val isSelected: Boolean,
    )

    val availableCalendars = mutableStateListOf<CalendarAccount>()
    val selectedCalendarIds = mutableStateOf(setOf<String>())

    val isScreenLockedSecurityEnabled = mutableStateOf(false)
    val isDeviceAdminEnabled = mutableStateOf(false)
    val isDeveloperModeEnabled = mutableStateOf(false)
    val isNotificationPolicyAccessGranted = mutableStateOf(false)
    val skipSilentNotifications = mutableStateOf(true)
    val notificationLightingStyle = mutableStateOf(NotificationLightingStyle.STROKE)
    val notificationLightingColorMode = mutableStateOf(NotificationLightingColorMode.SYSTEM)
    val notificationLightingCustomColor = mutableIntStateOf(0xFF6200EE.toInt()) // Default purple
    val notificationLightingPulseCount = mutableStateOf(1f)
    val notificationLightingPulseDuration = mutableStateOf(3000f)
    val notificationLightingIndicatorX = mutableStateOf(50f) // 0-100 percentage
    val notificationLightingIndicatorY = mutableStateOf(2f) // 0-100 percentage, default top
    val notificationLightingIndicatorScale = mutableStateOf(1.0f)
    val notificationLightingGlowSides =
        mutableStateOf(setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT))
    val notificationLightingSweepPosition = mutableStateOf(NotificationLightingSweepPosition.CENTER)
    val notificationLightingSweepThickness = mutableFloatStateOf(8f)
    val notificationLightingSweepRandomShapes = mutableStateOf(false)
    val notificationLightingSystemMode = mutableIntStateOf(0) // 0: Charging ripple, 1: Auth ripple
    val skipPersistentNotifications = mutableStateOf(false)
    val isAppLockEnabled = mutableStateOf(false)
    val appLockAutoLockDelayIndex = mutableIntStateOf(0)
    val isUseUsageAccess = mutableStateOf(false)
    val isFreezeWhenLockedEnabled = mutableStateOf(false)
    val freezeLockDelayIndex = mutableIntStateOf(1) // Default: 1 minute
    val freezePickedApps = mutableStateOf<List<NotificationApp>>(emptyList())
    val isFreezePickedAppsLoading = mutableStateOf(false)
    val freezeAutoExcludedApps = mutableStateOf<Set<String>>(emptySet())
    val isFreezeDontFreezeActiveAppsEnabled = mutableStateOf(false)
    val freezeMode = mutableIntStateOf(0)
    val isFreezeShowInLauncherEnabled = mutableStateOf(true)
    val freezeTags = mutableStateOf<List<com.sameerasw.essentials.domain.model.AppTag>>(emptyList())
    val freezeAppTagMap = mutableStateOf<Map<String, List<String>>>(emptyMap())
    val isFreezeTagColorCodedEnabled = mutableStateOf(false)

    // Search state
    val searchQuery = mutableStateOf("")
    val searchResults = mutableStateOf<List<SearchableItem>>(emptyList())
    val isSearching = mutableStateOf(false)
    val recentSearches = mutableStateOf<List<SearchableItem>>(emptyList())
    private var searchJob: Job? = null

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
    val hasPendingUpdates = mutableStateOf(false)

    val isPitchBlackThemeEnabled = mutableStateOf(false)
    val isGenAIAutomationEnabled = mutableStateOf(false)
    val isLocationReachedFullScreenAlarmEnabled = mutableStateOf(true)

    val isEnableUnsupportedFeatures = mutableStateOf(false)
    val isBlurEnabled = mutableStateOf(true)
    val isBlurSettingEnabled = mutableStateOf(true)
    val isRippleEnabled = mutableStateOf(true)
    val isRippleSettingEnabled = mutableStateOf(true)
    val isSwipeTabsEnabled = mutableStateOf(true)
    val sentryReportMode = mutableStateOf("auto")
    val isPowerSaveModeEnabled = mutableStateOf(false)
    private var powerSaveReceiver: BroadcastReceiver? = null

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
    val isCalendarPermissionGranted = mutableStateOf(false)
    val isUserDictionaryEnabled = mutableStateOf(false)
    val userDictionaryWords = mutableStateOf<Map<String, Long>>(emptyMap())
    val isUserDictionarySheetVisible = mutableStateOf(false)
    val isLongPressSymbolsEnabled = mutableStateOf(false)
    val isAccentedCharactersEnabled = mutableStateOf(false)

    // AirSync Bridge
    val isAirSyncConnectionEnabled = mutableStateOf(false)
    val macBatteryLevel = mutableIntStateOf(-1)
    val isMacBatteryCharging = mutableStateOf(false)
    val macBatteryLastUpdated = mutableStateOf(0L)
    val isMacConnected = mutableStateOf(false)
    val batteryWidgetMaxDevices = mutableIntStateOf(8)
    val isBatteryWidgetBackgroundEnabled = mutableStateOf(true)
    val isAmbientMusicGlanceDockedModeEnabled = mutableStateOf(false)
    val isAmbientMusicGlanceRandomShapesEnabled = mutableStateOf(false)
    val ambientMusicGlanceAlbumArtMode = mutableStateOf("default")
    val ambientMusicGlanceClockSize = mutableIntStateOf(80)
    val ambientMusicGlanceClockWeight = mutableIntStateOf(400)
    val ambientMusicGlanceClockWidth = mutableIntStateOf(100)
    val ambientMusicGlanceClockRoundness = mutableIntStateOf(50)
    val isAmbientMusicGlanceForceFillWhileChargingEnabled = mutableStateOf(false)
    val isAmbientMusicGlanceRespectNotificationsEnabled = mutableStateOf(true)
    val scaleAnimationsMode = mutableStateOf("default")
    val isTouchSensitivityEnabled = mutableStateOf(false)
    val isAutoRotateEnabled = mutableStateOf(false)
    val screenTimeout = mutableStateOf(30000L)
    val refreshRateMode = mutableStateOf(RefreshRateUtils.MODE_FIXED)
    val fixedRefreshRate = mutableFloatStateOf(0f)
    val minRefreshRate = mutableFloatStateOf(0f)
    val peakRefreshRate = mutableFloatStateOf(0f)
    val fontScale = mutableFloatStateOf(1.0f)
    val fontWeight = mutableIntStateOf(0)
    val animatorDurationScale = mutableFloatStateOf(1.0f)
    val transitionAnimationScale = mutableFloatStateOf(1.0f)
    val windowAnimationScale = mutableFloatStateOf(1.0f)
    val smallestWidth = mutableIntStateOf(360)
    val hasShizukuPermission = mutableStateOf(false)
    val isAprilFoolsSheetVisible = mutableStateOf(false)
    val isAprilFoolsShown = mutableStateOf(false)

    // Battery Saver Constants
    val batterySaverConstants = mutableStateOf<Map<String, String>>(emptyMap())

    // Audio Safe Volume State
    val isAudioSafeVolumeDisabled = mutableStateOf(false)

    // Battery Saver Low Power Trigger Level
    val lowPowerTriggerLevel = mutableIntStateOf(0)

    // Notification Snooze
    val isShowNotificationSnoozeEnabled = mutableStateOf(false)
    val notificationSnoozeDefault = mutableIntStateOf(60)
    val notificationSnoozeOptions = mutableStateOf<List<Int>>(listOf(15, 30, 60, 120))

    private var lastUpdateCheckTime: Long = 0
    lateinit var settingsRepository: SettingsRepository
    private lateinit var updateRepository: UpdateRepository
    private var appContext: Context? = null

    val gitHubToken = mutableStateOf<String?>(null)
    val gitHubWorkflowToken = mutableStateOf<String?>(null)
    val wallpaperTriggerState = mutableStateOf<String?>(null)
    val workflowAuthState =
        mutableStateOf<com.sameerasw.essentials.viewmodels.AuthState>(com.sameerasw.essentials.viewmodels.AuthState.Idle)
    private var workflowPollingJob: kotlinx.coroutines.Job? = null
    val gitHubUser = mutableStateOf<com.sameerasw.essentials.domain.model.github.GitHubUser?>(null)

    private val contentObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(
                selfChange: Boolean,
                uri: Uri?,
            ) {
                uri?.let {
                    when (it) {
                        Settings.System.getUriFor(Settings.System.FONT_SCALE) -> {
                            fontScale.floatValue = settingsRepository.getFontScale()
                        }

                        Settings.Secure.getUriFor("font_weight_adjustment") -> {
                            fontWeight.intValue = settingsRepository.getFontWeight()
                        }

                        Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE) -> {
                            animatorDurationScale.floatValue =
                                settingsRepository.getAnimationScale(Settings.Global.ANIMATOR_DURATION_SCALE)
                        }

                        Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE) -> {
                            transitionAnimationScale.floatValue =
                                settingsRepository.getAnimationScale(Settings.Global.TRANSITION_ANIMATION_SCALE)
                        }

                        Settings.Global.getUriFor(Settings.Global.WINDOW_ANIMATION_SCALE) -> {
                            windowAnimationScale.floatValue =
                                settingsRepository.getAnimationScale(Settings.Global.WINDOW_ANIMATION_SCALE)
                        }

                        Settings.Secure.getUriFor("display_density_forced") -> {
                            smallestWidth.intValue = settingsRepository.getSmallestWidth()
                        }

                        Settings.Secure.getUriFor("doze_always_on") -> {
                            isAodEnabled.value = settingsRepository.isAodEnabled()
                        }

                        Settings.Secure.getUriFor("sysui_qs_tiles") -> {
                            appContext?.let { updateAddedQSTiles(it) }
                        }

                        Settings.System.getUriFor("peak_refresh_rate"),
                        Settings.System.getUriFor("min_refresh_rate"),
                        -> {
                            appContext?.let { syncRefreshRateState(it) }
                        }

                        Settings.Global.getUriFor("battery_saver_constants") -> {
                            appContext?.let { loadBatterySaverConstants(it) }
                        }

                        Settings.Global.getUriFor("audio_safe_volume_state") -> {
                            appContext?.let { syncAudioSafeVolumeState(it) }
                        }

                        Settings.Global.getUriFor("low_power_trigger_level") -> {
                            appContext?.let { syncLowPowerTriggerLevel(it) }
                        }

                        Settings.Secure.getUriFor("show_notification_snooze") -> {
                            appContext?.let { syncShowNotificationSnooze(it) }
                        }

                        Settings.Global.getUriFor("notification_snooze_options") -> {
                            appContext?.let { loadNotificationSnoozeOptions(it) }
                        }
                    }
                }
            }
        }

    private val preferenceChangeListener =
        object : android.content.SharedPreferences.OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(
                sharedPreferences: android.content.SharedPreferences?,
                key: String?,
            ) {
                if (key == null) return

                when (key) {
                    SettingsRepository.KEY_EDGE_LIGHTING_ENABLED ->
                        isNotificationLightingEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_DYNAMIC_NIGHT_LIGHT_ENABLED ->
                        isDynamicNightLightEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_SMART_PIXELS_ENABLED ->
                        isSmartPixelsEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_SMART_PIXELS_INTENSITY ->
                        smartPixelsIntensity.floatValue =
                            settingsRepository.getFloat(key, 50f)

                    SettingsRepository.KEY_SMART_PIXELS_DISABLE_ON_CAST ->
                        isSmartPixelsDisableOnCastEnabled.value =
                            settingsRepository.getBoolean(key, true)

                    SettingsRepository.KEY_SCREEN_LOCKED_SECURITY_ENABLED ->
                        isScreenLockedSecurityEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_MAPS_POWER_SAVING_ENABLED -> {
                        isMapsPowerSavingEnabled.value = settingsRepository.getBoolean(key)
                        MapsState.isEnabled = isMapsPowerSavingEnabled.value
                    }

                    SettingsRepository.KEY_STATUS_BAR_ICON_CONTROL_ENABLED ->
                        isStatusBarIconControlEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_BUTTON_REMAP_ENABLED ->
                        isButtonRemapEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_APP_LOCK_ENABLED -> {
                        isAppLockEnabled.value = settingsRepository.getBoolean(key)
                        appContext?.let { updateAppDetectionService(it) }
                    }

                    SettingsRepository.KEY_USE_USAGE_ACCESS -> {
                        isUseUsageAccess.value = settingsRepository.getBoolean(key)
                        appContext?.let { updateAppDetectionService(it) }
                    }

                    SettingsRepository.KEY_FREEZE_WHEN_LOCKED_ENABLED ->
                        isFreezeWhenLockedEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_FREEZE_DONT_FREEZE_ACTIVE_APPS ->
                        isFreezeDontFreezeActiveAppsEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_FREEZE_LOCK_DELAY_INDEX ->
                        freezeLockDelayIndex.intValue =
                            settingsRepository.getInt(key, 1)

                    SettingsRepository.KEY_FREEZE_AUTO_EXCLUDED_APPS -> {
                        freezeAutoExcludedApps.value =
                            settingsRepository.getFreezeAutoExcludedApps()
                    }

                    SettingsRepository.KEY_FREEZE_MODE -> {
                        freezeMode.intValue = settingsRepository.getFreezeMode()
                    }

                    SettingsRepository.KEY_FREEZE_SHOW_IN_LAUNCHER -> {
                        val enabled = settingsRepository.getBoolean(key, true)
                        isFreezeShowInLauncherEnabled.value = enabled
                        appContext?.let { ctx ->
                            val componentName =
                                ComponentName(ctx, "com.sameerasw.essentials.AppFreezingLauncher")
                            try {
                                ctx.packageManager.setComponentEnabledSetting(
                                    componentName,
                                    if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    PackageManager.DONT_KILL_APP,
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    SettingsRepository.KEY_USE_ROOT ->
                        isRootEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_CHECK_PRE_RELEASES_ENABLED ->
                        isPreReleaseCheckEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_DEVELOPER_MODE_ENABLED -> {
                        isDeveloperModeEnabled.value = settingsRepository.getBoolean(key)
                    }

                    SettingsRepository.KEY_PITCH_BLACK_THEME_ENABLED ->
                        isPitchBlackThemeEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_LOCATION_REACHED_FULL_SCREEN_ALARM_ENABLED ->
                        isLocationReachedFullScreenAlarmEnabled.value =
                            settingsRepository.getLocationReachedFullScreenAlarmEnabled()

                    SettingsRepository.KEY_ENABLE_UNSUPPORTED_FEATURES -> {
                        isEnableUnsupportedFeatures.value =
                            settingsRepository.isEnableUnsupportedFeatures()
                        if (searchQuery.value.isNotBlank()) {
                            appContext?.let { onSearchQueryChanged(searchQuery.value, it) }
                        }
                    }

                    SettingsRepository.KEY_KEYBOARD_HEIGHT ->
                        keyboardHeight.floatValue =
                            settingsRepository.getFloat(key, 54f)

                    SettingsRepository.KEY_KEYBOARD_BOTTOM_PADDING ->
                        keyboardBottomPadding.floatValue =
                            settingsRepository.getFloat(key, 0f)

                    SettingsRepository.KEY_KEYBOARD_ROUNDNESS ->
                        keyboardRoundness.floatValue =
                            settingsRepository.getFloat(key, 24f)

                    SettingsRepository.KEY_KEYBOARD_HAPTICS_ENABLED ->
                        isKeyboardHapticsEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_KEYBOARD_FUNCTIONS_BOTTOM ->
                        isKeyboardFunctionsBottom.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_KEYBOARD_FUNCTIONS_PADDING ->
                        keyboardFunctionsPadding.floatValue =
                            settingsRepository.getFloat(key, 0f)

                    SettingsRepository.KEY_KEYBOARD_HAPTIC_STRENGTH ->
                        keyboardHapticStrength.floatValue =
                            settingsRepository.getFloat(key, 0.5f)

                    SettingsRepository.KEY_KEYBOARD_SHAPE ->
                        keyboardShape.intValue =
                            settingsRepository.getInt(key, 0)

                    SettingsRepository.KEY_KEYBOARD_ALWAYS_DARK ->
                        isKeyboardAlwaysDark.value =
                            settingsRepository.getBoolean(key, false)

                    SettingsRepository.KEY_KEYBOARD_PITCH_BLACK ->
                        isKeyboardPitchBlack.value =
                            settingsRepository.getBoolean(key, false)

                    SettingsRepository.KEY_KEYBOARD_CLIPBOARD_ENABLED ->
                        isKeyboardClipboardEnabled.value =
                            settingsRepository.getBoolean(key, true)

                    SettingsRepository.KEY_KEYBOARD_LONG_PRESS_SYMBOLS ->
                        isLongPressSymbolsEnabled.value =
                            settingsRepository.getBoolean(key, false)

                    SettingsRepository.KEY_KEYBOARD_ACCENTED_CHARACTERS ->
                        isAccentedCharactersEnabled.value =
                            settingsRepository.getBoolean(key, false)

                    SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED ->
                        isAirSyncConnectionEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_MAC_BATTERY_LEVEL ->
                        macBatteryLevel.intValue =
                            settingsRepository.getInt(key, -1)

                    SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING ->
                        isMacBatteryCharging.value =
                            settingsRepository.getBoolean(key, false)

                    SettingsRepository.KEY_MAC_BATTERY_LAST_UPDATED ->
                        macBatteryLastUpdated.value =
                            settingsRepository.getLong(key, 0L)

                    SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED ->
                        isMacConnected.value =
                            settingsRepository.getBoolean(key, false)

                    SettingsRepository.KEY_BATTERY_WIDGET_MAX_DEVICES ->
                        batteryWidgetMaxDevices.intValue =
                            settingsRepository.getInt(key, 8)

                    SettingsRepository.KEY_SNOOZE_DISCOVERED_CHANNELS, SettingsRepository.KEY_SNOOZE_BLOCKED_CHANNELS -> {
                        appContext?.let { loadSnoozeChannels(it) }
                    }

                    SettingsRepository.KEY_MAPS_DISCOVERED_CHANNELS, SettingsRepository.KEY_MAPS_DETECTION_CHANNELS -> {
                        appContext?.let { loadMapsChannels(it) }
                    }

                    SettingsRepository.KEY_SNOOZE_HEADS_UP_ENABLED -> {
                        isSnoozeHeadsUpEnabled.value = settingsRepository.getBoolean(key)
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

                    SettingsRepository.KEY_LIKE_SONG_AOD_OVERLAY_ENABLED -> {
                        isLikeSongAodOverlayEnabled.value = settingsRepository.getBoolean(key)
                    }

                    SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ENABLED -> {
                        isAmbientMusicGlanceEnabled.value = settingsRepository.getBoolean(key)
                    }

                    SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_DOCKED_MODE -> {
                        isAmbientMusicGlanceDockedModeEnabled.value =
                            settingsRepository.getBoolean(key)
                    }

                    SettingsRepository.KEY_CALENDAR_SYNC_ENABLED -> {
                        isCalendarSyncEnabled.value = settingsRepository.getBoolean(key)
                    }

                    SettingsRepository.KEY_ONBOARDING_COMPLETED -> {
                        isOnboardingCompleted.value = settingsRepository.getBoolean(key, false)
                    }

                    SettingsRepository.KEY_TRACKED_REPOS -> {
                        appContext?.let { refreshTrackedUpdates(it) }
                    }

                    SettingsRepository.KEY_FONT_SCALE ->
                        fontScale.floatValue =
                            settingsRepository.getFontScale()

                    SettingsRepository.KEY_FONT_WEIGHT ->
                        fontWeight.intValue =
                            settingsRepository.getFontWeight()

                    SettingsRepository.KEY_ANIMATOR_DURATION_SCALE ->
                        animatorDurationScale.floatValue =
                            settingsRepository.getAnimationScale(Settings.Global.ANIMATOR_DURATION_SCALE)

                    SettingsRepository.KEY_TRANSITION_ANIMATION_SCALE ->
                        transitionAnimationScale.floatValue =
                            settingsRepository.getAnimationScale(Settings.Global.TRANSITION_ANIMATION_SCALE)

                    SettingsRepository.KEY_WINDOW_ANIMATION_SCALE ->
                        windowAnimationScale.floatValue =
                            settingsRepository.getAnimationScale(Settings.Global.WINDOW_ANIMATION_SCALE)

                    SettingsRepository.KEY_SMALLEST_WIDTH ->
                        smallestWidth.intValue =
                            settingsRepository.getSmallestWidth()

                    SettingsRepository.KEY_REFRESH_RATE_MODE ->
                        refreshRateMode.value =
                            settingsRepository.getRefreshRateMode()

                    SettingsRepository.KEY_REFRESH_RATE_FIXED,
                    SettingsRepository.KEY_REFRESH_RATE_MIN,
                    SettingsRepository.KEY_REFRESH_RATE_PEAK,
                    -> {
                        appContext?.let { syncRefreshRateState(it) }
                    }

                    SettingsRepository.KEY_NOTIFICATION_GLANCE_ENABLED ->
                        isNotificationGlanceEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_AOD_FORCE_TURN_OFF_ENABLED ->
                        isAodForceTurnOffEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_POCKET_MODE_ENABLED ->
                        isPocketModeEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_POCKET_MODE_USE_LIGHT_SENSOR ->
                        isPocketModeUseLightSensor.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_POCKET_MODE_LOCK_SCREEN_ONLY ->
                        isPocketModeLockScreenOnly.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_NOTIFICATION_GLANCE_SAME_AS_LIGHTING ->
                        isNotificationGlanceSameAsLightingEnabled.value =
                            settingsRepository.getBoolean(key, true)

                    SettingsRepository.KEY_AUTO_ACCESSIBILITY_ENABLED ->
                        isAutoAccessibilityEnabled.value =
                            settingsRepository.getBoolean(key)

                    SettingsRepository.KEY_USE_BLUR -> {
                        appContext?.let { updateBlurState(it) }
                    }

                    SettingsRepository.KEY_USE_RIPPLE -> {
                        appContext?.let { updateRippleState(it) }
                    }

                    SettingsRepository.KEY_PRIVATE_DNS_PRESETS -> {
                        dnsPresets.clear()
                        dnsPresets.addAll(settingsRepository.getPrivateDnsPresets())
                    }

                    SettingsRepository.KEY_APRIL_FOOLS_SHOWN -> {
                        isAprilFoolsShown.value = settingsRepository.getBoolean(key)
                    }

                    SettingsRepository.KEY_FLASHLIGHT_PULSE_MAX_INTENSITY -> {
                        flashlightPulseMaxIntensity.floatValue =
                            settingsRepository.getFloat(key, 0.5f)
                    }

                    SettingsRepository.KEY_FLASHLIGHT_POCKET_TURN_OFF_ENABLED -> {
                        isFlashlightPocketTurnOffEnabled.value = settingsRepository.getBoolean(key)
                    }

                    SettingsRepository.KEY_FLASHLIGHT_OVERHEAT_PREVENTION_ENABLED -> {
                        isFlashlightOverheatEnabled.value = settingsRepository.getBoolean(key, true)
                    }

                    SettingsRepository.KEY_CIRCLE_TO_SEARCH_GESTURE_ENABLED -> {
                        isCircleToSearchGestureEnabled.value = settingsRepository.getBoolean(key)
                    }

                    SettingsRepository.KEY_CIRCLE_TO_SEARCH_GESTURE_HEIGHT -> {
                        circleToSearchGestureHeight.floatValue =
                            settingsRepository.getFloat(key, 48f)
                    }

                    SettingsRepository.KEY_CIRCLE_TO_SEARCH_GESTURE_WIDTH -> {
                        circleToSearchGestureWidth.floatValue =
                            settingsRepository.getFloat(key, 240f)
                    }

                    SettingsRepository.KEY_CIRCLE_TO_SEARCH_PREVIEW_ENABLED -> {
                        isCircleToSearchPreviewEnabled.value = settingsRepository.getBoolean(key)
                    }

                    SettingsRepository.KEY_HIDE_GESTURE_BAR_ON_LAUNCHER_ENABLED -> {
                        isHideGestureBarOnLauncherEnabled.value = settingsRepository.getBoolean(key)
                        appContext?.let { updateAppDetectionService(it) }
                    }

                    SettingsRepository.KEY_LIVE_WALLPAPER_SELECTED_VIDEO -> {
                        liveWallpaperSelectedVideo.value =
                            settingsRepository.getLiveWallpaperSelectedVideo()
                    }

                    SettingsRepository.KEY_LIVE_WALLPAPER_PLAYBACK_TRIGGER -> {
                        liveWallpaperPlaybackTrigger.value =
                            settingsRepository.getLiveWallpaperPlaybackTrigger()
                    }

                    SettingsRepository.KEY_LIVE_WALLPAPER_CUSTOM_VIDEOS -> {
                        liveWallpaperCustomVideos.clear()
                        liveWallpaperCustomVideos.addAll(settingsRepository.getLiveWallpaperCustomVideos())
                    }

                    SettingsRepository.KEY_SHUT_UP_ATTEMPT_SHIZUKU_RESTART -> {
                        isShutUpAttemptShizukuRestart.value =
                            settingsRepository.isShutUpAttemptShizukuRestartEnabled()
                    }

                    SettingsRepository.KEY_SHUT_UP_RESTORE_DELAY -> {
                        shutUpRestoreDelay.intValue =
                            settingsRepository.getShutUpRestoreDelay()
                    }

                    SettingsRepository.KEY_SHUT_UP_RESTORE_MODE -> {
                        shutUpRestoreMode.value =
                            settingsRepository.getShutUpRestoreMode()
                    }

                    SettingsRepository.KEY_SHIZUKU_AUTH_TOKEN -> {
                        shizukuAuthToken.value =
                            settingsRepository.getShizukuAuthToken()
                    }

                    SettingsRepository.KEY_EDGE_LIGHTING_SWEEP_SELECTED_SHAPES -> {
                        edgeLightingSweepSelectedShapes.value =
                            settingsRepository.getEdgeLightingSweepSelectedShapes()
                    }

                    SettingsRepository.KEY_DISABLE_ROTATION_SUGGESTION -> {
                        isDisableRotationSuggestionEnabled.value =
                            settingsRepository.getBoolean(key)
                        appContext?.let {
                            applyDisableRotationSuggestion(
                                it,
                                isDisableRotationSuggestionEnabled.value,
                            )
                        }
                    }

                    SettingsRepository.KEY_ALLOW_OVERLAYS_IN_SETTINGS -> {
                        isAllowOverlaysInSettingsEnabled.value =
                            settingsRepository.getBoolean(key)
                        appContext?.let {
                            applyAllowOverlaysInSettings(
                                it,
                                isAllowOverlaysInSettingsEnabled.value,
                            )
                        }
                    }

                    SettingsRepository.KEY_NETWORK_DOWNLOAD_RATE_LIMIT -> {
                        networkDownloadRateLimit.intValue =
                            settingsRepository.getInt(key, -1)
                        appContext?.let {
                            applyNetworkDownloadRateLimit(
                                it,
                                networkDownloadRateLimit.intValue,
                            )
                        }
                    }

                    SettingsRepository.KEY_MOBILE_DATA_ALWAYS_ON -> {
                        isMobileDataAlwaysOnEnabled.value =
                            settingsRepository.getBoolean(key)
                        appContext?.let {
                            applyMobileDataAlwaysOn(
                                it,
                                isMobileDataAlwaysOnEnabled.value,
                            )
                        }
                    }

                    SettingsRepository.KEY_WIRELESS_DISPLAY_CERTIFICATION -> {
                        isWirelessDisplayCertificationEnabled.value =
                            settingsRepository.getBoolean(key)
                        appContext?.let {
                            applyWirelessDisplayCertification(
                                it,
                                isWirelessDisplayCertificationEnabled.value,
                            )
                        }
                    }

                    SettingsRepository.KEY_TRANSPARENT_NAVIGATION_BAR -> {
                        isTransparentNavigationBarEnabled.value =
                            settingsRepository.getBoolean(key)
                        appContext?.let {
                            applyTransparentNavigationBar(
                                it,
                                isTransparentNavigationBarEnabled.value,
                            )
                        }
                    }

                    SettingsRepository.KEY_PREFER_GPU_COMPOSING -> {
                        isPreferGpuComposingEnabled.value =
                            settingsRepository.getBoolean(key)
                        appContext?.let {
                            applyPreferGpuComposing(
                                it,
                                isPreferGpuComposingEnabled.value,
                            )
                        }
                    }

                    SettingsRepository.KEY_PIXEL_SEARCHBAR -> {
                        isPixelSearchbarEnabled.value =
                            settingsRepository.getBoolean(key)
                        appContext?.let {
                            applyPixelSearchbarSetting(
                                it,
                                isPixelSearchbarEnabled.value,
                            )
                        }
                    }
                }
            }
        }

    /**
     * Updates the Sentry crash and feedback reporting mode preference.
     *
     * @param mode [String] Desired report mode ("automatic", "manual", or "disabled").
     * @param context [Context] Application context for settings persistence.
     */
    fun setSentryReportMode(
        mode: String,
        context: Context,
    ) {
        sentryReportMode.value = mode
        settingsRepository.putString(SettingsRepository.KEY_SENTRY_REPORT_MODE, mode)
    }

    /**
     * Applies and updates the application-wide display language locale.
     *
     * @param languageCode [String] BCP-47 language tag (e.g., "en", "si", "de").
     */
    fun setAppLanguage(languageCode: String) {
        appLanguage.value = languageCode
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    /**
     * Loads saved ShutUp per-app media ducking and mute configurations from persistent storage.
     */
    fun loadShutUpConfigs() {
        shutUpConfigs.value = settingsRepository.loadShutUpConfigs()
    }

    /**
     * Updates ducking or mute configuration for a specific target package.
     *
     * @param config [com.sameerasw.essentials.domain.model.ShutUpAppConfig] The updated ShutUpAppConfig object to store.
     */
    fun updateShutUpConfig(config: com.sameerasw.essentials.domain.model.ShutUpAppConfig) {
        settingsRepository.updateShutUpConfig(config)
        loadShutUpConfigs()
    }

    /**
     * Executes the remove shut up config operation.
     *
     * @param packageName [String] Target package name.
     */
    fun removeShutUpConfig(packageName: String) {
        val current = shutUpConfigs.value.toMutableList()
        current.removeAll { it.packageName == packageName }
        settingsRepository.saveShutUpConfigs(current)
        loadShutUpConfigs()
    }

    /**
     * Executes the set shut up attempt shizuku restart enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setShutUpAttemptShizukuRestartEnabled(enabled: Boolean) {
        isShutUpAttemptShizukuRestart.value = enabled
        settingsRepository.setShutUpAttemptShizukuRestartEnabled(enabled)
    }

    /**
     * Executes the set shut up restore delay operation.
     *
     * @param delaySeconds [Int] Target delay seconds.
     */
    fun setShutUpRestoreDelay(delaySeconds: Int) {
        shutUpRestoreDelay.intValue = delaySeconds
        settingsRepository.setShutUpRestoreDelay(delaySeconds)
    }

    /**
     * Executes the set shut up restore mode operation.
     *
     * @param mode [String] Target mode.
     */
    fun setShutUpRestoreMode(mode: String) {
        shutUpRestoreMode.value = mode
        settingsRepository.setShutUpRestoreMode(mode)
    }

    /**
     * Executes the set shizuku auth token operation.
     *
     * @param token [String] Target token.
     */
    fun setShizukuAuthToken(token: String) {
        shizukuAuthToken.value = token
        settingsRepository.setShizukuAuthToken(token)
    }

    /**
     * Executes the save shut up selected apps operation.
     *
     * @param context [Context] Target context.
     * @param apps [List<AppSelection>] Target apps.
     */
    fun saveShutUpSelectedApps(
        context: Context,
        apps: List<AppSelection>,
    ) {
        val currentConfigs = settingsRepository.loadShutUpConfigs().associateBy { it.packageName }
        val newConfigs =
            apps.filter { it.isEnabled }.map {
                currentConfigs[it.packageName] ?: com.sameerasw.essentials.domain.model.ShutUpAppConfig(
                    it.packageName,
                )
            }
        settingsRepository.saveShutUpConfigs(newConfigs)
        loadShutUpConfigs()
    }

    fun createShutUpShortcut(
        context: Context,
        config: com.sameerasw.essentials.domain.model.ShutUpAppConfig,
    ) {
        val appName =
            try {
                val appInfo = context.packageManager.getApplicationInfo(config.packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                config.packageName
            }

        val intent =
            Intent(context, com.sameerasw.essentials.ShutUpShortcutActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                putExtra("package_name", config.packageName)
                data = Uri.parse("shutup://${config.packageName}")
            }

        if (androidx.core.content.pm.ShortcutManagerCompat
                .isRequestPinShortcutSupported(context)
        ) {
            val appIcon = AppUtil.getShortcutIcon(context, config.packageName)

            val pinShortcutInfo =
                androidx.core.content.pm.ShortcutInfoCompat
                    .Builder(context, config.packageName)
                    .setShortLabel(appName)
                    .setIcon(
                        androidx.core.graphics.drawable.IconCompat
                            .createWithBitmap(appIcon),
                    ).setIntent(intent)
                    .build()

            androidx.core.content.pm.ShortcutManagerCompat.requestPinShortcut(
                context,
                pinShortcutInfo,
                null,
            )
            Toast
                .makeText(
                    context,
                    context.getString(R.string.shut_up_shortcut_created, appName),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    /**
     * Executes the check operation.
     *
     * @param context [Context] Target context.
     */
    fun check(context: Context) {
        appContext = context.applicationContext
        settingsRepository = SettingsRepository(context)
        updateRepository = UpdateRepository()
        gitHubUser.value = settingsRepository.getGitHubUser()

        // Sync with system per-app language settings
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.isEmpty) {
            val locale = currentLocales.get(0)
            val langTag = locale?.toLanguageTag() ?: "en"
            appLanguage.value =
                when {
                    langTag.startsWith("pt-BR") -> "pt-BR"
                    langTag.startsWith("pt-PT") -> "pt-PT"
                    langTag.startsWith("pt") -> "pt-BR" // Fallback to Brazilian Portuguese as primary translated option
                    else -> locale?.language ?: "en"
                }
        } else {
            appLanguage.value = "en"
        }

        isAccessibilityEnabled.value = PermissionUtils.isAccessibilityServiceEnabled(context)
        isWriteSecureSettingsEnabled.value = PermissionUtils.canWriteSecureSettings(context)
        isShizukuAvailable.value = ShizukuUtils.isShizukuAvailable()
        isShizukuPermissionGranted.value = ShizukuUtils.hasPermission()
        if (cachedIsUpdateAvailable) {
            isUpdateAvailable.value = true
            updateInfo.value = cachedUpdateInfo
        }
        isAutoAccessibilityEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_AUTO_ACCESSIBILITY_ENABLED)
        isHideGestureBarEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_HIDE_GESTURE_BAR_ENABLED, false)
        isCircleToSearchGestureEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_CIRCLE_TO_SEARCH_GESTURE_ENABLED,
                false,
            )
        circleToSearchGestureHeight.floatValue =
            settingsRepository.getFloat(SettingsRepository.KEY_CIRCLE_TO_SEARCH_GESTURE_HEIGHT, 48f)
        circleToSearchGestureWidth.floatValue =
            settingsRepository.getFloat(SettingsRepository.KEY_CIRCLE_TO_SEARCH_GESTURE_WIDTH, 240f)
        isCircleToSearchPreviewEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_CIRCLE_TO_SEARCH_PREVIEW_ENABLED,
                false,
            )
        isHideGestureBarOnLauncherEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_HIDE_GESTURE_BAR_ON_LAUNCHER_ENABLED,
                false,
            )
        notificationLightingSystemMode.intValue =
            settingsRepository.getNotificationLightingSystemMode()

        isShutUpAttemptShizukuRestart.value =
            settingsRepository.isShutUpAttemptShizukuRestartEnabled()
        shutUpRestoreDelay.intValue =
            settingsRepository.getShutUpRestoreDelay()
        shutUpRestoreMode.value =
            settingsRepository.getShutUpRestoreMode()
        shizukuAuthToken.value =
            settingsRepository.getShizukuAuthToken()
        edgeLightingSweepSelectedShapes.value =
            settingsRepository.getEdgeLightingSweepSelectedShapes()
        isDisableRotationSuggestionEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_DISABLE_ROTATION_SUGGESTION, false)
        isAllowOverlaysInSettingsEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_ALLOW_OVERLAYS_IN_SETTINGS, false)
        networkDownloadRateLimit.intValue =
            settingsRepository.getInt(SettingsRepository.KEY_NETWORK_DOWNLOAD_RATE_LIMIT, -1)
        isMobileDataAlwaysOnEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_MOBILE_DATA_ALWAYS_ON, false)
        isWirelessDisplayCertificationEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_WIRELESS_DISPLAY_CERTIFICATION,
                false,
            )
        isTransparentNavigationBarEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_TRANSPARENT_NAVIGATION_BAR, false)
        isPreferGpuComposingEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_PREFER_GPU_COMPOSING, false)
        isPixelSearchbarEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_PIXEL_SEARCHBAR, false)
        pixelSearchbarType.value =
            settingsRepository.getPixelSearchbarType()
        pixelSearchbarDateFormat.value =
            settingsRepository.getPixelSearchbarDateFormat()
        pixelSearchbarBackgroundPill.value =
            settingsRepository.getPixelSearchbarBackgroundPill()
        pixelSearchbarWidgetId.intValue =
            settingsRepository.getPixelSearchbarWidgetId()
        pixelSearchbarWidgetProvider.value =
            settingsRepository.getPixelSearchbarWidgetProvider()
        pixelSearchbarScrapedLine1.value =
            settingsRepository.getPixelSearchbarScrapedLine1()
        pixelSearchbarScrapedLine2.value =
            settingsRepository.getPixelSearchbarScrapedLine2()
        pixelSearchbarWidgetPaddingH.intValue =
            settingsRepository.getPixelSearchbarWidgetPaddingH()
        pixelSearchbarWidgetPaddingV.intValue =
            settingsRepository.getPixelSearchbarWidgetPaddingV()
        pixelSearchbarTapActionEnabled.value =
            settingsRepository.getPixelSearchbarTapActionEnabled()
        pixelSearchbarMusicTitle.value =
            settingsRepository.getPixelSearchbarMusicTitle()
        pixelSearchbarMusicArtist.value =
            settingsRepository.getPixelSearchbarMusicArtist()
        pixelSearchbarMusicPackage.value =
            settingsRepository.getPixelSearchbarMusicPackage()
        lockScreenClockId.value = readCurrentLockScreenClockId(context)
        lockScreenClockWeight.intValue = settingsRepository.getLockScreenClockWeight()
        lockScreenClockWidth.intValue = settingsRepository.getLockScreenClockWidth()
        lockScreenClockGrade.intValue = settingsRepository.getLockScreenClockGrade()
        lockScreenClockRoundness.intValue = settingsRepository.getLockScreenClockRoundness()
        lockScreenClockColorTone.intValue = settingsRepository.getLockScreenClockColorTone()
        lockScreenClockSelectedColorId.value =
            settingsRepository.getLockScreenClockSelectedColorId()
        lockScreenClockSeedColor.intValue = settingsRepository.getLockScreenClockSeedColor()
        loadShutUpConfigs()
        recentSearches.value = settingsRepository.getRecentSearches()
        loadCachedWallpaper()
        isDailyWallpaperAutoUpdateEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_DAILY_WALLPAPER_AUTO_UPDATE, false)
        if (isDailyWallpaperAutoUpdateEnabled.value) {
            schedulePeriodicWallpaperCheck(context)
        }
        schedulePeriodicAppUpdateCheck(context)
        dailyWallpaperAutoUpdateTime.value =
            settingsRepository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_AUTO_UPDATE_TIME)
        isDailyWallpaperShowLastTime.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_DAILY_WALLPAPER_SHOW_LAST_TIME)

        if (isHideGestureBarEnabled.value) {
            applyHideGestureBar(context, true)
        }

        updateAppDetectionService(context)

        if (isAutoAccessibilityEnabled.value && !isAccessibilityEnabled.value) {
            val serviceName =
                "${context.packageName}/${ScreenOffAccessibilityService::class.java.name}"
            var success = false

            if (isWriteSecureSettingsEnabled.value) {
                try {
                    val enabledServices =
                        Settings.Secure.getString(
                            context.contentResolver,
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                        ) ?: ""
                    val newServices =
                        if (enabledServices.isEmpty()) {
                            serviceName
                        } else if (!enabledServices.contains(
                                serviceName,
                            )
                        ) {
                            "$enabledServices:$serviceName"
                        } else {
                            enabledServices
                        }
                    Settings.Secure.putString(
                        context.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                        newServices,
                    )
                    Settings.Secure.putString(
                        context.contentResolver,
                        Settings.Secure.ACCESSIBILITY_ENABLED,
                        "1",
                    )
                    success = true
                } catch (e: Exception) {
                    success = false
                }
            }

            if (success) {
                isAccessibilityEnabled.value =
                    PermissionUtils.isAccessibilityServiceEnabled(context)
                if (isAccessibilityEnabled.value) {
                    Toast
                        .makeText(
                            context,
                            "Accessibility auto-granted",
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }

        isReadPhoneStateEnabled.value = PermissionUtils.hasReadPhoneStatePermission(context)
        isPostNotificationsEnabled.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        isNotificationListenerEnabled.value =
            PermissionUtils.hasNotificationListenerPermission(context)
        isOverlayPermissionGranted.value = PermissionUtils.canDrawOverlays(context)
        isNotificationLightingAccessibilityEnabled.value =
            PermissionUtils.isNotificationLightingAccessibilityServiceEnabled(context)
        isDefaultBrowserSet.value = PermissionUtils.isDefaultBrowser(context)
        isLocationPermissionGranted.value = PermissionUtils.hasLocationPermission(context)
        isBackgroundLocationPermissionGranted.value =
            PermissionUtils.hasBackgroundLocationPermission(context)
        isFullScreenIntentPermissionGranted.value = PermissionUtils.canUseFullScreenIntent(context)
        isKeyboardEnabled.value = PermissionUtils.isKeyboardEnabled(context)
        isKeyboardSelected.value = PermissionUtils.isKeyboardSelected(context)
        isWriteSettingsEnabled.value = PermissionUtils.canWriteSystemSettings(context)
        isNotificationPolicyAccessGranted.value =
            PermissionUtils.hasNotificationPolicyAccess(context)
        isCalendarPermissionGranted.value = PermissionUtils.hasReadCalendarPermission(context)
        isUsageStatsPermissionGranted.value = PermissionUtils.hasUsageStatsPermission(context)

        isBluetoothPermissionGranted.value = PermissionUtils.hasBluetoothPermission(context)

        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.FONT_SCALE),
            false,
            contentObserver,
        )
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("font_weight_adjustment"),
            false,
            contentObserver,
        )
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            contentObserver,
        )
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE),
            false,
            contentObserver,
        )
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.WINDOW_ANIMATION_SCALE),
            false,
            contentObserver,
        )
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("display_density_forced"),
            false,
            contentObserver,
        )
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor("peak_refresh_rate"),
            false,
            contentObserver,
        )
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor("min_refresh_rate"),
            false,
            contentObserver,
        )

        try {
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor("doze_always_on"),
                false,
                contentObserver,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor("sysui_qs_tiles"),
                false,
                contentObserver,
            )
        } catch (e: Exception) {
            // This might fail on Android 14+ for some system keys
            e.printStackTrace()
        }

        try {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor("battery_saver_constants"),
                false,
                contentObserver,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor("audio_safe_volume_state"),
                false,
                contentObserver,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor("low_power_trigger_level"),
                false,
                contentObserver,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor("show_notification_snooze"),
                false,
                contentObserver,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor("notification_snooze_options"),
                false,
                contentObserver,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        loadBatterySaverConstants(context)
        syncAudioSafeVolumeState(context)
        syncLowPowerTriggerLevel(context)
        syncShowNotificationSnooze(context)
        loadNotificationSnoozeOptions(context)

        isPowerSaveModeEnabled.value = DeviceUtils.isPowerSaveMode(context)
        updateBlurState(context)
        updateRippleState(context)
        updateAddedQSTiles(context)

        if (powerSaveReceiver == null) {
            powerSaveReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?,
                    ) {
                        if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                            context?.let {
                                isPowerSaveModeEnabled.value = DeviceUtils.isPowerSaveMode(it)
                                updateBlurState(it)
                                updateRippleState(it)
                            }
                        }
                    }
                }
            context.applicationContext.registerReceiver(
                powerSaveReceiver,
                IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            )
        }

        settingsRepository.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        viewModelScope.launch {
            settingsRepository.gitHubToken.collect {
                gitHubToken.value = it
            }
        }

        viewModelScope.launch {
            settingsRepository.gitHubWorkflowToken.collect {
                gitHubWorkflowToken.value = it
            }
        }

        isWidgetEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_WIDGET_ENABLED)
        isStatusBarIconControlEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_STATUS_BAR_ICON_CONTROL_ENABLED)

        fontScale.floatValue = settingsRepository.getFontScale()
        fontWeight.intValue = settingsRepository.getFontWeight()
        animatorDurationScale.floatValue =
            settingsRepository.getAnimationScale(Settings.Global.ANIMATOR_DURATION_SCALE)
        transitionAnimationScale.floatValue =
            settingsRepository.getAnimationScale(Settings.Global.TRANSITION_ANIMATION_SCALE)
        windowAnimationScale.floatValue =
            settingsRepository.getAnimationScale(Settings.Global.WINDOW_ANIMATION_SCALE)
        smallestWidth.intValue = settingsRepository.getSmallestWidth()
        refreshRateMode.value = settingsRepository.getRefreshRateMode()
        syncRefreshRateState(context)
        hasShizukuPermission.value = ShizukuUtils.hasPermission() || RootUtils.isRootAvailable()

        isMapsPowerSavingEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_MAPS_POWER_SAVING_ENABLED)
        isNotificationLightingEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_ENABLED)
        onlyShowWhenScreenOff.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_EDGE_LIGHTING_ONLY_SCREEN_OFF,
                true,
            )
        isAmbientDisplayEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_AMBIENT_DISPLAY)
        isAmbientShowLockScreenEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_AMBIENT_SHOW_LOCK_SCREEN)
        skipSilentNotifications.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_SKIP_SILENT, true)
        skipPersistentNotifications.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_SKIP_PERSISTENT)

        notificationLightingStyle.value = settingsRepository.getNotificationLightingStyle()
        notificationLightingColorMode.value = settingsRepository.getNotificationLightingColorMode()
        isUseUsageAccess.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_USE_USAGE_ACCESS)
        isOnboardingCompleted.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_ONBOARDING_COMPLETED, false)

        val lastShownCounter =
            settingsRepository.getInt(SettingsRepository.KEY_WHATS_NEW_LAST_SHOWN_COUNTER, 0)
        isWhatsNewVisible.value =
            isOnboardingCompleted.value &&
            lastShownCounter < com.sameerasw.essentials.BuildConfig.WHATS_NEW_COUNTER

        notificationLightingCustomColor.intValue =
            settingsRepository.getInt(
                SettingsRepository.KEY_EDGE_LIGHTING_CUSTOM_COLOR,
                0xFF6200EE.toInt(),
            )
        notificationLightingPulseCount.value =
            settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_PULSE_COUNT, 1f)
        notificationLightingPulseDuration.value =
            settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_PULSE_DURATION, 3000f)
        notificationLightingIndicatorX.value =
            settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_X, 50f)
        notificationLightingIndicatorY.value =
            settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_Y, 2f)
        isAodEnabled.value = settingsRepository.isAodEnabled()

        isRootEnabled.value = settingsRepository.getBoolean(SettingsRepository.KEY_USE_ROOT)

        if (isRootEnabled.value) {
            isRootAvailable.value = RootUtils.isRootAvailable()
            isRootPermissionGranted.value =
                RootUtils.isRootPermissionGranted()
        } else {
            isRootAvailable.value = false
            isRootPermissionGranted.value = false
        }

        notificationLightingIndicatorScale.value =
            settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_SCALE, 1.0f)
        notificationLightingGlowSides.value = settingsRepository.getNotificationLightingGlowSides()
        notificationLightingSweepPosition.value =
            settingsRepository.getNotificationLightingSweepPosition()
        notificationLightingSweepThickness.floatValue =
            settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_SWEEP_THICKNESS, 8f)
        notificationLightingSweepRandomShapes.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_EDGE_LIGHTING_SWEEP_RANDOM_SHAPES,
                true,
            )

        MapsState.isEnabled = isMapsPowerSavingEnabled.value
        hapticFeedbackType.value = settingsRepository.getHapticFeedbackType()
        defaultTab.value = settingsRepository.getDIYTab()
        selectedAppIcon.value = settingsRepository.getAppIcon()
        isSwipeTabsEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_SWIPE_TABS, true)
        sentryReportMode.value =
            settingsRepository.getString(SettingsRepository.KEY_SENTRY_REPORT_MODE, "auto")
                ?: "auto"

        // Live Wallpaper initialization
        liveWallpaperSelectedVideo.value = settingsRepository.getLiveWallpaperSelectedVideo()
        liveWallpaperPlaybackTrigger.value = settingsRepository.getLiveWallpaperPlaybackTrigger()
        liveWallpaperCustomVideos.clear()
        liveWallpaperCustomVideos.addAll(settingsRepository.getLiveWallpaperCustomVideos())

        checkCaffeinateActive(context)

        // Button Remap & Migration
        isButtonRemapEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_BUTTON_REMAP_ENABLED,
                settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_VOLUME_TOGGLE_ENABLED),
            )
        isButtonRemapUseShizuku.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_BUTTON_REMAP_USE_SHIZUKU)
        shizukuDetectedDevicePath.value =
            settingsRepository.getString(SettingsRepository.KEY_SHIZUKU_DETECTED_DEVICE_PATH)

        val oldTrigger =
            settingsRepository.getString(
                SettingsRepository.KEY_FLASHLIGHT_TRIGGER_BUTTON,
                "Volume Up",
            )

        val hasLegacyToggle =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_FLASHLIGHT_VOLUME_TOGGLE_ENABLED,
                false,
            ) // Default false here as key check logic

        volumeUpActionOff.value = settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF)
        volumeDownActionOff.value = settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF)
        volumeUpActionOn.value = settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_ON)
        volumeDownActionOn.value = settingsRepository.getRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON)

        val hapticName =
            settingsRepository.getString(
                SettingsRepository.KEY_BUTTON_REMAP_HAPTIC_TYPE,
                settingsRepository.getString(
                    SettingsRepository.KEY_FLASHLIGHT_HAPTIC_TYPE,
                    HapticFeedbackType.DOUBLE.name,
                ),
            )

        remapHapticType.value =
            try {
                val type = HapticFeedbackType.valueOf(hapticName ?: HapticFeedbackType.DOUBLE.name)
                if (type.name == "LONG") HapticFeedbackType.DOUBLE else type
            } catch (e: Exception) {
                HapticFeedbackType.DOUBLE
            }

        isDynamicNightLightEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_DYNAMIC_NIGHT_LIGHT_ENABLED)
        isSmartPixelsEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_SMART_PIXELS_ENABLED)
        smartPixelsIntensity.floatValue =
            settingsRepository.getFloat(SettingsRepository.KEY_SMART_PIXELS_INTENSITY, 50f)
        isSmartPixelsDisableOnCastEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_SMART_PIXELS_DISABLE_ON_CAST, true)
        loadSnoozeChannels(context)
        loadMapsChannels(context)
        isSnoozeHeadsUpEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_SNOOZE_HEADS_UP_ENABLED)
        isFlashlightAlwaysTurnOffEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_ALWAYS_TURN_OFF_ENABLED)
        isFlashlightFadeEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_FADE_ENABLED)
        isFlashlightAdjustEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_ADJUST_INTENSITY_ENABLED)
        isFlashlightGlobalEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_GLOBAL_ENABLED)
        isFlashlightLiveUpdateEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_FLASHLIGHT_LIVE_UPDATE_ENABLED,
                true,
            )
        flashlightLastIntensity.value =
            settingsRepository.getInt(SettingsRepository.KEY_FLASHLIGHT_LAST_INTENSITY, 1)
        isFlashlightPulseEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_PULSE_ENABLED)
        isFlashlightPulseFacedownOnly.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_FLASHLIGHT_PULSE_FACEDOWN_ONLY,
                true,
            )
        isFlashlightPulseUseLightingApps.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_FLASHLIGHT_PULSE_SAME_AS_LIGHTING,
                true,
            )
        flashlightPulseMaxIntensity.floatValue =
            settingsRepository.getFloat(
                SettingsRepository.KEY_FLASHLIGHT_PULSE_MAX_INTENSITY,
                0.5f,
            )
        isFlashlightPulseDisableOnDnd.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_FLASHLIGHT_PULSE_DISABLE_ON_DND,
                true,
            )
        isFlashlightPocketTurnOffEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_FLASHLIGHT_POCKET_TURN_OFF_ENABLED)
        isFlashlightOverheatEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_FLASHLIGHT_OVERHEAT_PREVENTION_ENABLED,
                true,
            )
        isPitchBlackThemeEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_PITCH_BLACK_THEME_ENABLED)
        isLocationReachedFullScreenAlarmEnabled.value =
            settingsRepository.getLocationReachedFullScreenAlarmEnabled()
        isEnableUnsupportedFeatures.value = settingsRepository.isEnableUnsupportedFeatures()

        keyboardHeight.floatValue =
            settingsRepository.getFloat(SettingsRepository.KEY_KEYBOARD_HEIGHT, 54f)
        keyboardBottomPadding.floatValue =
            settingsRepository.getFloat(SettingsRepository.KEY_KEYBOARD_BOTTOM_PADDING, 0f)
        keyboardRoundness.floatValue =
            settingsRepository.getFloat(SettingsRepository.KEY_KEYBOARD_ROUNDNESS, 24f)
        isKeyboardHapticsEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_KEYBOARD_HAPTICS_ENABLED, true)
        isKeyboardFunctionsBottom.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_BOTTOM, false)
        keyboardFunctionsPadding.floatValue =
            settingsRepository.getFloat(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_PADDING, 0f)
        keyboardHapticStrength.floatValue =
            settingsRepository.getFloat(SettingsRepository.KEY_KEYBOARD_HAPTIC_STRENGTH, 0.5f)
        keyboardShape.intValue = settingsRepository.getInt(SettingsRepository.KEY_KEYBOARD_SHAPE, 0)
        isKeyboardAlwaysDark.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_KEYBOARD_ALWAYS_DARK, false)
        isKeyboardPitchBlack.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_KEYBOARD_PITCH_BLACK, false)
        isKeyboardClipboardEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_KEYBOARD_CLIPBOARD_ENABLED, true)
        isUserDictionaryEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_USER_DICTIONARY_ENABLED, false)
        isLongPressSymbolsEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_KEYBOARD_LONG_PRESS_SYMBOLS, false)
        isAccentedCharactersEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_KEYBOARD_ACCENTED_CHARACTERS,
                false,
            )

        isAirSyncConnectionEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED)

        // April Fools Check
        isAprilFoolsShown.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_APRIL_FOOLS_SHOWN)
        if (!isAprilFoolsShown.value) {
            val calendar = java.util.Calendar.getInstance()
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            if (month == java.util.Calendar.APRIL && day == 1) {
                isAprilFoolsSheetVisible.value = true
            }
        }

        macBatteryLevel.intValue =
            settingsRepository.getInt(SettingsRepository.KEY_MAC_BATTERY_LEVEL, -1)
        isMacBatteryCharging.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING, false)
        macBatteryLastUpdated.value =
            settingsRepository.getLong(SettingsRepository.KEY_MAC_BATTERY_LAST_UPDATED, 0L)
        isMacConnected.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED, false)

        isBluetoothDevicesEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_SHOW_BLUETOOTH_DEVICES, false)
        isBluetoothDevicesEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_SHOW_BLUETOOTH_DEVICES, false)
        batteryWidgetMaxDevices.intValue = settingsRepository.getBatteryWidgetMaxDevices()
        isBatteryWidgetBackgroundEnabled.value =
            settingsRepository.isBatteryWidgetBackgroundEnabled()
        isCallVibrationsEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_CALL_VIBRATIONS_ENABLED)

        isScreenLockedSecurityEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_SCREEN_LOCKED_SECURITY_ENABLED)
        isDeviceAdminEnabled.value = isDeviceAdminActive(context)

        isAutoUpdateEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_AUTO_UPDATE_ENABLED, true)
        isGenAIAutomationEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_GENAI_AUTOMATION_ENABLED, false)

        isUpdateNotificationEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_UPDATE_NOTIFICATION_ENABLED, true)
        freezeMode.intValue = settingsRepository.getFreezeMode()
        lastUpdateCheckTime =
            settingsRepository.getLong(SettingsRepository.KEY_LAST_UPDATE_CHECK_TIME)
        isAppLockEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_APP_LOCK_ENABLED)
        appLockAutoLockDelayIndex.intValue =
            settingsRepository.getInt(SettingsRepository.KEY_APP_LOCK_AUTO_LOCK_DELAY_INDEX, 0)
        isFreezeWhenLockedEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_FREEZE_WHEN_LOCKED_ENABLED)
        isFreezeDontFreezeActiveAppsEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_FREEZE_DONT_FREEZE_ACTIVE_APPS)
        freezeLockDelayIndex.intValue =
            settingsRepository.getInt(SettingsRepository.KEY_FREEZE_LOCK_DELAY_INDEX, 1)
        freezeAutoExcludedApps.value = settingsRepository.getFreezeAutoExcludedApps()
        isFreezeShowInLauncherEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_FREEZE_SHOW_IN_LAUNCHER, true)
        freezeTags.value = settingsRepository.getFreezeTags()
        freezeAppTagMap.value = settingsRepository.getFreezeAppTagMap()
        isFreezeTagColorCodedEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_FREEZE_TAG_COLOR_CODED_ENABLED,
                false,
            )

        // Sync PackageManager component enabled state on startup
        val showLauncher = isFreezeShowInLauncherEnabled.value
        val componentName = ComponentName(context, "com.sameerasw.essentials.AppFreezingLauncher")
        try {
            val currentState = context.packageManager.getComponentEnabledSetting(componentName)
            val targetState =
                if (showLauncher) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            if (currentState != targetState) {
                context.packageManager.setComponentEnabledSetting(
                    componentName,
                    targetState,
                    PackageManager.DONT_KILL_APP,
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        isDeveloperModeEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_DEVELOPER_MODE_ENABLED)

        dnsPresets.clear()
        dnsPresets.addAll(settingsRepository.getPrivateDnsPresets())

        isPreReleaseCheckEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_CHECK_PRE_RELEASES_ENABLED)
        pinnedFeatureKeys.value = settingsRepository.getPinnedFeatures()
        pinnedQsTileKeys.value = settingsRepository.getPinnedQsTiles()
        isLikeSongToastEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_LIKE_SONG_TOAST_ENABLED, true)
        isLikeSongAodOverlayEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_LIKE_SONG_AOD_OVERLAY_ENABLED,
                false,
            )
        isAmbientMusicGlanceEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ENABLED,
                false,
            )
        isAmbientMusicGlanceDockedModeEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_DOCKED_MODE,
                false,
            )
        isAmbientMusicGlanceRandomShapesEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_RANDOM_SHAPES,
                false,
            )
        ambientMusicGlanceAlbumArtMode.value =
            settingsRepository.getAmbientMusicGlanceAlbumArtMode()
        ambientMusicGlanceClockSize.intValue = settingsRepository.getAmbientMusicGlanceClockSize()
        ambientMusicGlanceClockWeight.intValue =
            settingsRepository.getAmbientMusicGlanceClockWeight()
        ambientMusicGlanceClockWidth.intValue = settingsRepository.getAmbientMusicGlanceClockWidth()
        ambientMusicGlanceClockRoundness.intValue =
            settingsRepository.getAmbientMusicGlanceClockRoundness()
        isAmbientMusicGlanceForceFillWhileChargingEnabled.value =
            settingsRepository.isAmbientMusicGlanceForceFillWhileChargingEnabled()
        isAmbientMusicGlanceRespectNotificationsEnabled.value =
            settingsRepository.isAmbientMusicGlanceRespectNotificationsEnabled()
        isCalendarSyncEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_CALENDAR_SYNC_ENABLED, false)
        isNotificationSyncEnabled.value =
            settingsRepository.getBoolean("watch_notif_sync_enabled", false)
        isCallSyncEnabled.value =
            settingsRepository.getBoolean("watch_call_sync_enabled", true)
        isCalendarSyncPeriodicEnabled.value = settingsRepository.isCalendarSyncPeriodicEnabled()
        isBatteryNotificationEnabled.value = settingsRepository.isBatteryNotificationEnabled()
        selectedCalendarIds.value = settingsRepository.getCalendarSyncSelectedCalendars()
        isNotificationGlanceEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_NOTIFICATION_GLANCE_ENABLED)
        isAodForceTurnOffEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_AOD_FORCE_TURN_OFF_ENABLED)
        isPocketModeEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_POCKET_MODE_ENABLED)
        isPocketModeUseLightSensor.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_POCKET_MODE_USE_LIGHT_SENSOR)
        pocketModeTriggerDelay.floatValue =
            settingsRepository.getFloat(SettingsRepository.KEY_POCKET_MODE_TRIGGER_DELAY, 3f)
        isPocketModeLockScreenOnly.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_POCKET_MODE_LOCK_SCREEN_ONLY)
        isNotificationGlanceSameAsLightingEnabled.value =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_NOTIFICATION_GLANCE_SAME_AS_LIGHTING,
                true,
            )
        scaleAnimationsMode.value = settingsRepository.getScaleAnimationsMode()
        isTouchSensitivityEnabled.value = settingsRepository.getTouchSensitivityEnabled()
        isAutoRotateEnabled.value = settingsRepository.getAutoRotateEnabled()
        screenTimeout.value = settingsRepository.getScreenTimeout()
        isPowerSaveModeEnabled.value = DeviceUtils.isPowerSaveMode(context)
        updateBlurState(context)
        updateRippleState(context)

        refreshTrackedUpdates(context)
        if (isBatteryNotificationEnabled.value) {
            startBatteryNotificationService(context)
        }

        isLockdownModeEnabled.value =
            settingsRepository.getBoolean(SettingsRepository.KEY_LOCKDOWN_MODE)
    }

    private fun startBatteryNotificationService(context: Context) {
        com.sameerasw.essentials.utils.ServiceUtils
            .startRequiredServices(context)
    }

    private fun stopBatteryNotificationService(context: Context) {
        val intent =
            Intent(
                context,
                com.sameerasw.essentials.services.BatteryNotificationService::class.java,
            )
        context.stopService(intent)
    }

    /**
     * Executes the set battery notification enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setBatteryNotificationEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isBatteryNotificationEnabled.value = enabled
        settingsRepository.setBatteryNotificationEnabled(enabled)
        if (enabled) {
            startBatteryNotificationService(context)
        } else {
            stopBatteryNotificationService(context)
        }
    }

    /**
     * Executes the set enable unsupported features operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setEnableUnsupportedFeatures(
        enabled: Boolean,
        context: Context,
    ) {
        isEnableUnsupportedFeatures.value = enabled
        settingsRepository.setEnableUnsupportedFeatures(enabled)
        if (searchQuery.value.isNotBlank()) {
            onSearchQueryChanged(searchQuery.value, context)
        }
    }

    /**
     * Executes the on search query changed operation.
     *
     * @param query [String] Target query.
     * @param context [Context] Target context.
     */
    fun onSearchQueryChanged(
        query: String,
        context: Context,
    ) {
        searchQuery.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            searchResults.value = emptyList()
            isSearching.value = false
            return
        }

        isSearching.value = true
        searchJob =
            viewModelScope.launch(Dispatchers.Default) {
                delay(300)
                val results =
                    SearchRegistry.search(
                        context,
                        query,
                        isEnableUnsupportedFeatures.value,
                    )
                withContext(Dispatchers.Main) {
                    searchResults.value = results
                    isSearching.value = false
                }
            }
    }

    /**
     * Executes the add recent search operation.
     *
     * @param item [SearchableItem] Target item.
     */
    fun addRecentSearch(item: SearchableItem) {
        val current = recentSearches.value.toMutableList()
        // Remove existing to move to top
        current.removeAll {
            it.title == item.title &&
                it.featureKey == item.featureKey &&
                it.targetSettingHighlightKey == item.targetSettingHighlightKey
        }
        current.add(0, item)
        // Limit to 10
        val limited = current.take(10)
        recentSearches.value = limited
        settingsRepository.saveRecentSearches(limited)
    }

    /**
     * Executes the clear recent searches operation.
     */
    fun clearRecentSearches() {
        recentSearches.value = emptyList()
        settingsRepository.saveRecentSearches(emptyList())
    }

    /**
     * Executes the toggle pin feature operation.
     *
     * @param featureId [String] Target feature id.
     */
    fun togglePinFeature(featureId: String) {
        val current = pinnedFeatureKeys.value.toMutableList()
        if (current.contains(featureId)) {
            current.remove(featureId)
        } else {
            current.add(featureId) // Append at the end to keep order
        }
        pinnedFeatureKeys.value = current
        settingsRepository.savePinnedFeatures(current)

        appContext?.let { context ->
            com.sameerasw.essentials.utils.ShortcutUtil
                .updateLauncherDynamicShortcuts(context)
            val intent =
                Intent("com.sameerasw.essentials.action.FAVORITES_WIDGET_UPDATE").apply {
                    setPackage(context.packageName)
                }
            context.sendBroadcast(intent)
        }
    }

    /**
     * Executes the toggle pin qs tile operation.
     *
     * @param serviceClassName [String] Target service class name.
     */
    fun togglePinQsTile(serviceClassName: String) {
        val current = pinnedQsTileKeys.value.toMutableList()
        if (current.contains(serviceClassName)) {
            current.remove(serviceClassName)
        } else {
            current.add(serviceClassName)
        }
        pinnedQsTileKeys.value = current
        settingsRepository.savePinnedQsTiles(current)

        appContext?.let { context ->
            val intent =
                Intent("com.sameerasw.essentials.action.QS_TILES_WIDGET_UPDATE").apply {
                    setPackage(context.packageName)
                }
            context.sendBroadcast(intent)
        }
    }

    /**
     * Executes the set auto update enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setAutoUpdateEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isAutoUpdateEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_AUTO_UPDATE_ENABLED, enabled)
    }

    /**
     * Executes the set gen ai automation enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setGenAIAutomationEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isGenAIAutomationEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_GENAI_AUTOMATION_ENABLED, enabled)
    }

    /**
     * Executes the set update notification enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setUpdateNotificationEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isUpdateNotificationEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_UPDATE_NOTIFICATION_ENABLED, enabled)
    }

    /**
     * Executes the set pre release check enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setPreReleaseCheckEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isPreReleaseCheckEnabled.value = enabled
        settingsRepository.putBooleanSync(SettingsRepository.KEY_CHECK_PRE_RELEASES_ENABLED, enabled)
        // Enabling pre-releases automatically enables Developer Mode; disabling turns it off
        isDeveloperModeEnabled.value = enabled
        settingsRepository.putBooleanSync(SettingsRepository.KEY_DEVELOPER_MODE_ENABLED, enabled)
    }

    /**
     * Executes the set developer mode enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setDeveloperModeEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isDeveloperModeEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_DEVELOPER_MODE_ENABLED, enabled)
    }

    /**
     * Executes the start workflow auth flow operation.
     *
     * @param context [Context] Target context.
     */
    fun startWorkflowAuthFlow(context: Context) {
        workflowAuthState.value = com.sameerasw.essentials.viewmodels.AuthState.Loading
        viewModelScope.launch {
            val authRepo =
                com.sameerasw.essentials.data.repository
                    .GitHubAuthRepository()
            val response = authRepo.requestDeviceCodeWithWorkflow()
            if (response != null) {
                workflowAuthState.value =
                    com.sameerasw.essentials.viewmodels.AuthState.CodeReceived(
                        userCode = response.userCode,
                        verificationUri = response.verificationUri,
                    )
                startWorkflowPolling(response.deviceCode, response.interval, context)
            } else {
                workflowAuthState.value =
                    com.sameerasw.essentials.viewmodels.AuthState
                        .Error("Failed to request device code")
            }
        }
    }

    private fun startWorkflowPolling(
        deviceCode: String,
        intervalSeconds: Int,
        context: Context,
    ) {
        workflowPollingJob?.cancel()
        workflowPollingJob =
            viewModelScope.launch {
                val authRepo =
                    com.sameerasw.essentials.data.repository
                        .GitHubAuthRepository()
                var currentInterval = intervalSeconds * 1000L
                while (isActive) {
                    kotlinx.coroutines.delay(currentInterval)
                    val tokenResponse = authRepo.pollForToken(deviceCode, intervalSeconds)

                    if (tokenResponse != null) {
                        when {
                            tokenResponse.accessToken != null -> {
                                workflowAuthState.value =
                                    com.sameerasw.essentials.viewmodels.AuthState.Authenticated(
                                        tokenResponse.accessToken,
                                    )
                                settingsRepository.saveGitHubWorkflowToken(tokenResponse.accessToken)
                                workflowPollingJob?.cancel()
                                return@launch
                            }

                            tokenResponse.error == "authorization_pending" -> {
                                // continue
                            }

                            tokenResponse.error == "slow_down" -> {
                                currentInterval += 5000L
                            }

                            tokenResponse.error == "expired_token" -> {
                                workflowAuthState.value =
                                    com.sameerasw.essentials.viewmodels.AuthState
                                        .Error("Code expired. Please try again.")
                                workflowPollingJob?.cancel()
                                return@launch
                            }

                            else -> {
                                workflowAuthState.value =
                                    com.sameerasw.essentials.viewmodels.AuthState
                                        .Error("Authentication failed: ${tokenResponse.error}")
                                workflowPollingJob?.cancel()
                                return@launch
                            }
                        }
                    }
                }
            }
    }

    /**
     * Executes the cancel workflow auth flow operation.
     */
    fun cancelWorkflowAuthFlow() {
        workflowPollingJob?.cancel()
        workflowAuthState.value = com.sameerasw.essentials.viewmodels.AuthState.Idle
    }

    /**
     * Executes the trigger wallpaper update operation.
     *
     * @param target [String] Target target.
     */
    fun triggerWallpaperUpdate(target: String) {
        val token = settingsRepository.getGitHubWorkflowToken() ?: return
        wallpaperTriggerState.value = "loading"
        viewModelScope.launch {
            val gitHubRepo =
                com.sameerasw.essentials.data.repository
                    .GitHubRepository()
            val success =
                gitHubRepo.triggerWorkflowDispatch(
                    token = token,
                    owner = "sameerasw",
                    repo = "sameerasw.com",
                    workflowFile = "daily-unsplash.yml",
                    ref = "main",
                    inputs = mapOf("target" to target),
                )
            if (success) {
                wallpaperTriggerState.value = "success"
            } else {
                wallpaperTriggerState.value = "error"
            }
            kotlinx.coroutines.delay(3000)
            wallpaperTriggerState.value = null
        }
    }

    /**
     * Executes the set root enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setRootEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        settingsRepository.putBoolean(SettingsRepository.KEY_USE_ROOT, enabled)
        isRootEnabled.value = enabled
        check(context)
    }

    /**
     * Executes the set user dictionary enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setUserDictionaryEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isUserDictionaryEnabled.value = enabled
        settingsRepository.setUserDictionaryEnabled(enabled)
    }

    /**
     * Executes the set long press symbols enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setLongPressSymbolsEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isLongPressSymbolsEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_KEYBOARD_LONG_PRESS_SYMBOLS, enabled)
    }

    /**
     * Executes the set accented characters enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setAccentedCharactersEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isAccentedCharactersEnabled.value = enabled
        settingsRepository.setAccentedCharactersEnabled(enabled)
    }

    /**
     * Executes the load user dictionary words operation.
     *
     * @param context [Context] Target context.
     */
    fun loadUserDictionaryWords(context: Context) {
        context.applicationContext as? com.sameerasw.essentials.ime.EssentialsInputMethodService

        viewModelScope.launch(Dispatchers.IO) {
            val file = java.io.File(context.filesDir, "user_dict.txt")
            if (file.exists()) {
                val map = mutableMapOf<String, Long>()
                file.forEachLine { line ->
                    val parts = line.split(" ")
                    if (parts.size >= 2) {
                        map[parts[0]] = parts[1].toLongOrNull() ?: 1L
                    }
                }
                withContext(Dispatchers.Main) {
                    userDictionaryWords.value = map
                }
            } else {
                withContext(Dispatchers.Main) {
                    userDictionaryWords.value = emptyMap()
                }
            }
        }
    }

    /**
     * Executes the delete user word operation.
     *
     * @param word [String] Target word.
     * @param context [Context] Target context.
     */
    fun deleteUserWord(
        word: String,
        context: Context,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Read, remove, write
            val file = java.io.File(context.filesDir, "user_dict.txt")
            if (file.exists()) {
                val lines = file.readLines().filter { !it.startsWith("$word ") }
                file.writeText(lines.joinToString("\n"))
                loadUserDictionaryWords(context)
                settingsRepository.putLong(
                    SettingsRepository.KEY_USER_DICT_LAST_UPDATE,
                    System.currentTimeMillis(),
                )
            }
        }
    }

    /**
     * Executes the clear user dictionary operation.
     *
     * @param context [Context] Target context.
     */
    fun clearUserDictionary(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = java.io.File(context.filesDir, "user_dict.txt")
            if (file.exists()) {
                file.delete()
                withContext(Dispatchers.Main) {
                    userDictionaryWords.value = emptyMap()
                }
                settingsRepository.putLong(
                    SettingsRepository.KEY_USER_DICT_LAST_UPDATE,
                    System.currentTimeMillis(),
                )
            }
        }
    }

    /**
     * Executes the set pitch black theme enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setPitchBlackThemeEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isPitchBlackThemeEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_PITCH_BLACK_THEME_ENABLED, enabled)
    }

    fun setLocationReachedFullScreenAlarmEnabled(enabled: Boolean) {
        isLocationReachedFullScreenAlarmEnabled.value = enabled
        settingsRepository.setLocationReachedFullScreenAlarmEnabled(enabled)
    }

    /**
     * Executes the set blur enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setBlurEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        settingsRepository.putBoolean(SettingsRepository.KEY_USE_BLUR, enabled)
        updateBlurState(context)
    }

    /**
     * Executes the set ripple animation enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setRippleEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        settingsRepository.putBoolean(SettingsRepository.KEY_USE_RIPPLE, enabled)
        updateRippleState(context)
    }

    /**
     * Executes the set swipe tabs enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setSwipeTabsEnabled(enabled: Boolean) {
        settingsRepository.putBoolean(SettingsRepository.KEY_SWIPE_TABS, enabled)
        isSwipeTabsEnabled.value = enabled
    }

    private fun updateBlurState(context: Context) {
        val useBlurSetting = settingsRepository.getBoolean(SettingsRepository.KEY_USE_BLUR, true)
        val isProblematic = DeviceUtils.isBlurProblematicDevice()
        val isPowerSave = DeviceUtils.isPowerSaveMode(context)

        isBlurSettingEnabled.value = useBlurSetting
        isBlurEnabled.value = useBlurSetting && !isProblematic && !isPowerSave
    }

    private fun updateRippleState(context: Context) {
        val useRippleSetting = settingsRepository.getBoolean(SettingsRepository.KEY_USE_RIPPLE, true)
        val isPowerSave = DeviceUtils.isPowerSaveMode(context)

        isRippleSettingEnabled.value = useRippleSetting
        isRippleEnabled.value = useRippleSetting && !isPowerSave
    }

    /**
     * Executes the check for updates operation.
     *
     * @param context [Context] Target context.
     * @param manual [Boolean] Target manual.
     */
    fun checkForUpdates(
        context: Context,
        manual: Boolean = false,
    ) {
        if (isCheckingUpdate.value) return
        if (isUpdateAvailable.value && !manual) return

        if (!manual) {
            if (!isAutoUpdateEnabled.value) return
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateCheckTime < 900000) return
        }

        isCheckingUpdate.value = true
        updateInfo.value = null // Clear stale data before checking
        viewModelScope.launch {
            try {
                val currentVersion =
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    } catch (e: Exception) {
                        "0.0"
                    } ?: "0.0"

                val updateInfoResult =
                    updateRepository.checkForUpdates(
                        context,
                        isPreReleaseCheckEnabled.value,
                        currentVersion,
                    )

                if (updateInfoResult != null) {
                    updateInfo.value = updateInfoResult
                    isUpdateAvailable.value = updateInfoResult.isUpdateAvailable
                    if (updateInfoResult.isUpdateAvailable) {
                        cachedIsUpdateAvailable = true
                        cachedUpdateInfo = updateInfoResult
                    }

                    if (updateInfoResult.isUpdateAvailable && updateInfoResult.downloadUrl.isNotEmpty()) {
                        if (isUpdateNotificationEnabled.value) {
                            UpdateNotificationHelper.showUpdateNotification(
                                context,
                                updateInfoResult.versionName,
                                updateInfoResult.downloadUrl,
                            )
                        }
                    }

                    lastUpdateCheckTime = System.currentTimeMillis()
                    settingsRepository.putLong(
                        SettingsRepository.KEY_LAST_UPDATE_CHECK_TIME,
                        lastUpdateCheckTime,
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isCheckingUpdate.value = false
            }
        }
    }

    /**
     * Executes the refresh tracked updates operation.
     *
     * @param context [Context] Target context.
     */
    fun refreshTrackedUpdates(context: Context) {
        val trackedRepos = settingsRepository.getTrackedRepos()
        if (trackedRepos.isEmpty()) {
            hasPendingUpdates.value = false
            return
        }

        hasPendingUpdates.value = trackedRepos.any { it.isUpdateAvailable }
    }

    private fun isDeviceAdminActive(context: Context): Boolean = PermissionUtils.isDeviceAdminActive(context)

    /**
     * Executes the request device admin operation.
     *
     * @param context [Context] Target context.
     */
    fun requestDeviceAdmin(context: Context) {
        val adminComponent = ComponentName(context, SecurityDeviceAdminReceiver::class.java)
        val intent =
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    context.getString(R.string.perm_device_admin_explanation),
                )
            }
        if (context is Activity) {
            context.startActivity(intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * Executes the request read phone state permission operation.
     *
     * @param activity [Activity] Target activity.
     */
    fun requestReadPhoneStatePermission(activity: Activity) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_PHONE_STATE),
            1005,
        )
    }

    /**
     * Executes the set widget enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setWidgetEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isWidgetEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_WIDGET_ENABLED, enabled)
    }

    /**
     * Executes the set status bar icon control enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setStatusBarIconControlEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isStatusBarIconControlEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_STATUS_BAR_ICON_CONTROL_ENABLED,
            enabled,
        )
    }

    /**
     * Executes the set maps power saving enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setMapsPowerSavingEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isMapsPowerSavingEnabled.value = enabled
        MapsState.isEnabled = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_MAPS_POWER_SAVING_ENABLED, enabled)
    }

    /**
     * Executes the set notification lighting enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setNotificationLightingEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isNotificationLightingEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_EDGE_LIGHTING_ENABLED, enabled)
    }

    /**
     * Executes the set only show when screen off operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setOnlyShowWhenScreenOff(
        enabled: Boolean,
        context: Context,
    ) {
        onlyShowWhenScreenOff.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_EDGE_LIGHTING_ONLY_SCREEN_OFF, enabled)
    }

    /**
     * Executes the set ambient display enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setAmbientDisplayEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isAmbientDisplayEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_EDGE_LIGHTING_AMBIENT_DISPLAY, enabled)
    }

    /**
     * Executes the set ambient show lock screen enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setAmbientShowLockScreenEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isAmbientShowLockScreenEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_EDGE_LIGHTING_AMBIENT_SHOW_LOCK_SCREEN,
            enabled,
        )
    }

    /**
     * Executes the set hide gesture bar enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setHideGestureBarEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isHideGestureBarEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_HIDE_GESTURE_BAR_ENABLED, enabled)
        applyHideGestureBar(context, enabled)
    }

    /**
     * Executes the set circle to search gesture enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setCircleToSearchGestureEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isCircleToSearchGestureEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_CIRCLE_TO_SEARCH_GESTURE_ENABLED,
            enabled,
        )
    }

    /**
     * Executes the set circle to search gesture height operation.
     *
     * @param height [Float] Target height.
     */
    fun setCircleToSearchGestureHeight(height: Float) {
        circleToSearchGestureHeight.floatValue = height
        settingsRepository.putFloat(SettingsRepository.KEY_CIRCLE_TO_SEARCH_GESTURE_HEIGHT, height)
    }

    /**
     * Executes the set circle to search gesture width operation.
     *
     * @param width [Float] Target width.
     */
    fun setCircleToSearchGestureWidth(width: Float) {
        circleToSearchGestureWidth.floatValue = width
        settingsRepository.putFloat(SettingsRepository.KEY_CIRCLE_TO_SEARCH_GESTURE_WIDTH, width)
    }

    /**
     * Executes the set circle to search preview enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setCircleToSearchPreviewEnabled(enabled: Boolean) {
        isCircleToSearchPreviewEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_CIRCLE_TO_SEARCH_PREVIEW_ENABLED,
            enabled,
        )
    }

    /**
     * Executes the set hide gesture bar on launcher enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setHideGestureBarOnLauncherEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isHideGestureBarOnLauncherEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_HIDE_GESTURE_BAR_ON_LAUNCHER_ENABLED,
            enabled,
        )

        if (!enabled) {
            com.sameerasw.essentials.utils.StatusBarManager.requestRestore(
                context,
                "GestureBarAutomation",
            )
        }

        updateAppDetectionService(context)
    }

    /**
     * Executes the set disable rotation suggestion enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setDisableRotationSuggestionEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isDisableRotationSuggestionEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_DISABLE_ROTATION_SUGGESTION, enabled)
        applyDisableRotationSuggestion(context, enabled)
    }

    /**
     * Executes the set allow overlays in settings enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setAllowOverlaysInSettingsEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isAllowOverlaysInSettingsEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_ALLOW_OVERLAYS_IN_SETTINGS, enabled)
        applyAllowOverlaysInSettings(context, enabled)
    }

    /**
     * Executes the refresh allow overlays in settings state operation.
     *
     * @param context [Context] Target context.
     */
    fun refreshAllowOverlaysInSettingsState(context: Context) {
        try {
            val currentVal =
                Settings.Secure.getInt(context.contentResolver, "secure_overlay_settings", 0) == 1
            isAllowOverlaysInSettingsEnabled.value = currentVal
            settingsRepository.putBoolean(
                SettingsRepository.KEY_ALLOW_OVERLAYS_IN_SETTINGS,
                currentVal,
            )
        } catch (e: Exception) {
            // Secure setting not accessible without permission
        }
    }

    private fun applyAllowOverlaysInSettings(
        context: Context,
        enabled: Boolean,
    ) {
        val value = if (enabled) 1 else 0
        val key = "secure_overlay_settings"
        var success = false

        if (PermissionUtils.canWriteSecureSettings(context)) {
            try {
                success = Settings.Secure.putInt(context.contentResolver, key, value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!success) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                ShellUtils.runCommand(context, "settings put secure $key $value")
            }
        }
    }

    /**
     * Executes the set network download rate limit operation.
     *
     * @param limit [Int] Target limit.
     * @param context [Context] Target context.
     */
    fun setNetworkDownloadRateLimit(
        limit: Int,
        context: Context,
    ) {
        networkDownloadRateLimit.intValue = limit
        settingsRepository.putInt(SettingsRepository.KEY_NETWORK_DOWNLOAD_RATE_LIMIT, limit)
        applyNetworkDownloadRateLimit(context, limit)
    }

    /**
     * Executes the set mobile data always on enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setMobileDataAlwaysOnEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isMobileDataAlwaysOnEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_MOBILE_DATA_ALWAYS_ON, enabled)
        applyMobileDataAlwaysOn(context, enabled)
    }

    /**
     * Executes the set wireless display certification enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setWirelessDisplayCertificationEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isWirelessDisplayCertificationEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_WIRELESS_DISPLAY_CERTIFICATION,
            enabled,
        )
        applyWirelessDisplayCertification(context, enabled)
    }

    /**
     * Executes the refresh networks state operation.
     *
     * @param context [Context] Target context.
     */
    fun refreshNetworksState(context: Context) {
        try {
            val liveRateLimit =
                Settings.Global.getInt(
                    context.contentResolver,
                    "ingress_rate_limit_bytes_per_second",
                    -1,
                )
            networkDownloadRateLimit.intValue = liveRateLimit
            settingsRepository.putInt(
                SettingsRepository.KEY_NETWORK_DOWNLOAD_RATE_LIMIT,
                liveRateLimit,
            )
        } catch (e: Exception) {
            // Permission restricted
        }

        try {
            val liveMobileData =
                Settings.Global.getInt(context.contentResolver, "mobile_data_always_on", 0) == 1
            isMobileDataAlwaysOnEnabled.value = liveMobileData
            settingsRepository.putBoolean(
                SettingsRepository.KEY_MOBILE_DATA_ALWAYS_ON,
                liveMobileData,
            )
        } catch (e: Exception) {
            // Permission restricted
        }

        try {
            val liveWirelessDisplay =
                Settings.Global.getInt(
                    context.contentResolver,
                    "wifi_display_certification_on",
                    0,
                ) == 1
            isWirelessDisplayCertificationEnabled.value = liveWirelessDisplay
            settingsRepository.putBoolean(
                SettingsRepository.KEY_WIRELESS_DISPLAY_CERTIFICATION,
                liveWirelessDisplay,
            )
        } catch (e: Exception) {
            // Permission restricted
        }
    }

    private fun applyNetworkDownloadRateLimit(
        context: Context,
        limit: Int,
    ) {
        val key = "ingress_rate_limit_bytes_per_second"
        var success = false
        if (PermissionUtils.canWriteSecureSettings(context)) {
            try {
                success = Settings.Global.putInt(context.contentResolver, key, limit)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (!success) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                ShellUtils.runCommand(context, "settings put global $key $limit")
            }
        }
    }

    private fun applyMobileDataAlwaysOn(
        context: Context,
        enabled: Boolean,
    ) {
        val value = if (enabled) 1 else 0
        val key = "mobile_data_always_on"
        var success = false
        if (PermissionUtils.canWriteSecureSettings(context)) {
            try {
                success = Settings.Global.putInt(context.contentResolver, key, value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (!success) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                ShellUtils.runCommand(context, "settings put global $key $value")
            }
        }
    }

    private fun applyWirelessDisplayCertification(
        context: Context,
        enabled: Boolean,
    ) {
        val value = if (enabled) 1 else 0
        val key = "wifi_display_certification_on"
        var success = false
        if (PermissionUtils.canWriteSecureSettings(context)) {
            try {
                success = Settings.Global.putInt(context.contentResolver, key, value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (!success) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                ShellUtils.runCommand(context, "settings put global $key $value")
            }
        }
    }

    /**
     * Executes the set transparent navigation bar enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setTransparentNavigationBarEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isTransparentNavigationBarEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_TRANSPARENT_NAVIGATION_BAR, enabled)
        applyTransparentNavigationBar(context, enabled)
    }

    /**
     * Executes the refresh transparent navigation bar state operation.
     *
     * @param context [Context] Target context.
     */
    fun refreshTransparentNavigationBarState(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pkg = "com.android.internal.systemui.navbar.transparent"
            val output = ShellUtils.runCommandWithOutput(context, "cmd overlay list")
            if (output != null) {
                val isEnabled =
                    output.lines().any { line ->
                        line.contains("[x]") && line.contains(pkg)
                    }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isTransparentNavigationBarEnabled.value = isEnabled
                    settingsRepository.putBoolean(
                        SettingsRepository.KEY_TRANSPARENT_NAVIGATION_BAR,
                        isEnabled,
                    )
                }
            }
        }
    }

    private fun applyTransparentNavigationBar(
        context: Context,
        enabled: Boolean,
    ) {
        val pkg = "com.android.internal.systemui.navbar.transparent"
        val action = if (enabled) "enable" else "disable"
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            ShellUtils.runCommand(context, "cmd overlay $action --user current $pkg")
        }
    }

    /**
     * Executes the load standby apps operation.
     *
     * @param context [Context] Target context.
     */
    fun loadStandbyApps(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isStandbyAppsLoading.value = true
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val apps = pm.queryIntentActivities(intent, 0)

            val appList =
                apps
                    .mapNotNull { resolveInfo ->
                        val pkg = resolveInfo.activityInfo.packageName
                        if (pkg == context.packageName) return@mapNotNull null

                        val label = resolveInfo.loadLabel(pm).toString()
                        val icon = resolveInfo.loadIcon(pm)

                        var bucket = 10
                        val output = ShellUtils.runCommandWithOutput(context, "am get-standby-bucket $pkg")
                        if (output != null) {
                            val text = output.lowercase().trim()
                            bucket =
                                when {
                                    text.contains("restricted") || text.contains("45") -> 45
                                    text.contains("rare") || text.contains("40") -> 40
                                    text.contains("frequent") || text.contains("30") -> 30
                                    text.contains("working") || text.contains("20") -> 20
                                    text.contains("active") || text.contains("10") -> 10
                                    else -> 10
                                }
                        }

                        AppStandbyInfo(pkg, label, icon, bucket)
                    }.sortedBy { it.label.lowercase() }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                standbyAppsList.value = appList
                isStandbyAppsLoading.value = false
            }
        }
    }

    /**
     * Executes the set app standby bucket operation.
     *
     * @param packageName [String] Target package name.
     * @param targetBucket [Int] Target target bucket.
     * @param context [Context] Target context.
     */
    fun setAppStandbyBucket(
        packageName: String,
        targetBucket: Int,
        context: Context,
    ) {
        val currentList = standbyAppsList.value
        val updatedList =
            currentList.map { app ->
                if (app.packageName == packageName) {
                    app.copy(bucket = targetBucket)
                } else {
                    app
                }
            }
        standbyAppsList.value = updatedList

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val bucketName =
                when (targetBucket) {
                    10 -> "active"
                    20 -> "working_set"
                    30 -> "frequent"
                    40 -> "rare"
                    45 -> "restricted"
                    else -> "active"
                }
            ShellUtils.runCommand(context, "am set-standby-bucket $packageName $bucketName")
        }
    }

    /**
     * Executes batch set app standby bucket operation for multiple apps.
     *
     * @param packageNames [Set<String>] Target package names.
     * @param targetBucket [Int] Target standby bucket code.
     * @param context [Context] Context for shell execution.
     */
    fun setAppsStandbyBucket(
        packageNames: Set<String>,
        targetBucket: Int,
        context: Context,
    ) {
        val currentList = standbyAppsList.value
        val updatedList =
            currentList.map { app ->
                if (packageNames.contains(app.packageName)) {
                    app.copy(bucket = targetBucket)
                } else {
                    app
                }
            }
        standbyAppsList.value = updatedList

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val bucketName =
                when (targetBucket) {
                    10 -> "active"
                    20 -> "working_set"
                    30 -> "frequent"
                    40 -> "rare"
                    45 -> "restricted"
                    else -> "active"
                }
            packageNames.forEach { pkg ->
                ShellUtils.runCommand(context, "am set-standby-bucket $pkg $bucketName")
            }
        }
    }

    /**
     * Executes the set prefer gpu composing enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setPreferGpuComposingEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isPreferGpuComposingEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_PREFER_GPU_COMPOSING, enabled)
        applyPreferGpuComposing(context, enabled)
    }

    /**
     * Executes the refresh prefer gpu composing state operation.
     *
     * @param context [Context] Target context.
     */
    fun refreshPreferGpuComposingState(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val isShellGranted =
                (isShizukuAvailable.value && isShizukuPermissionGranted.value) ||
                    (isRootAvailable.value && isRootPermissionGranted.value)
            if (isShellGranted) {
                val isRoot = isRootAvailable.value && isRootPermissionGranted.value
                val liveValue = SurfaceFlingerControl.isHwOverlaysDisabled(context, isRoot)
                isPreferGpuComposingEnabled.value = liveValue
                settingsRepository.putBoolean(
                    SettingsRepository.KEY_PREFER_GPU_COMPOSING,
                    liveValue,
                )
            }
        }
    }

    private fun applyPreferGpuComposing(
        context: Context,
        enabled: Boolean,
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val isRoot = isRootAvailable.value && isRootPermissionGranted.value
            SurfaceFlingerControl.setDisableHwOverlays(enabled, isRoot)
        }
    }

    private fun applyDisableRotationSuggestion(
        context: Context,
        enabled: Boolean,
    ) {
        val value = if (enabled) 0 else 1
        val key = "show_rotation_suggestions"

        var success = false
        if (PermissionUtils.canWriteSecureSettings(context)) {
            try {
                success = Settings.Secure.putInt(context.contentResolver, key, value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!success) {
            val command = "settings put secure $key $value"
            if (ShizukuUtils.hasPermission()) {
                ShizukuUtils.runCommand(command)
            } else if (RootUtils.isRootPermissionGranted()) {
                RootUtils.runCommand(command)
            }
        }
    }

    /**
     * Executes the set pixel searchbar enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setPixelSearchbarEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isPixelSearchbarEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_PIXEL_SEARCHBAR, enabled)
        applyPixelSearchbarSetting(context, enabled)
    }

    private fun applyPixelSearchbarSetting(
        context: Context,
        enabled: Boolean,
    ) {
        val value = if (enabled) "com.sameerasw.essentials" else null
        val key = "selected_search_engine"

        var success = false
        if (PermissionUtils.canWriteSecureSettings(context)) {
            try {
                success = Settings.Secure.putString(context.contentResolver, key, value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!success) {
            val command =
                if (enabled) {
                    "settings put secure $key com.sameerasw.essentials"
                } else {
                    "settings delete secure $key"
                }
            if (ShizukuUtils.hasPermission()) {
                ShizukuUtils.runCommand(command)
            } else if (RootUtils.isRootPermissionGranted()) {
                RootUtils.runCommand(command)
            }
        }

        // Force stop nexus launcher to apply setting
        val forceStopCommand = "am force-stop com.google.android.apps.nexuslauncher"
        if (ShizukuUtils.hasPermission()) {
            ShizukuUtils.runCommand(forceStopCommand)
        } else if (RootUtils.isRootPermissionGranted()) {
            RootUtils.runCommand(forceStopCommand)
        }
    }

    /**
     * Executes the set pixel searchbar type operation.
     *
     * @param type [String] Target type.
     * @param context [Context] Target context.
     */
    fun setPixelSearchbarType(
        type: String,
        context: Context,
    ) {
        pixelSearchbarType.value = type
        settingsRepository.setPixelSearchbarType(type)
        if (type == "music") {
            updateMediaFromActiveSession(context)
        }
        updatePixelSearchbarWidget(context)

        // Force stop nexus launcher to apply setting
        val forceStopCommand = "am force-stop com.google.android.apps.nexuslauncher"
        if (ShizukuUtils.hasPermission()) {
            ShizukuUtils.runCommand(forceStopCommand)
        } else if (RootUtils.isRootPermissionGranted()) {
            RootUtils.runCommand(forceStopCommand)
        }
    }

    /**
     * Executes the set pixel searchbar date format operation.
     *
     * @param format [String] Target format.
     * @param context [Context] Target context.
     */
    fun setPixelSearchbarDateFormat(
        format: String,
        context: Context,
    ) {
        pixelSearchbarDateFormat.value = format
        settingsRepository.setPixelSearchbarDateFormat(format)
        updatePixelSearchbarWidget(context)

        // Force stop nexus launcher to apply setting
        val forceStopCommand = "am force-stop com.google.android.apps.nexuslauncher"
        if (ShizukuUtils.hasPermission()) {
            ShizukuUtils.runCommand(forceStopCommand)
        } else if (RootUtils.isRootPermissionGranted()) {
            RootUtils.runCommand(forceStopCommand)
        }
    }

    /**
     * Executes the set pixel searchbar background pill operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setPixelSearchbarBackgroundPill(
        enabled: Boolean,
        context: Context,
    ) {
        pixelSearchbarBackgroundPill.value = enabled
        settingsRepository.setPixelSearchbarBackgroundPill(enabled)
        updatePixelSearchbarWidget(context)

        // Force stop nexus launcher to apply setting
        val forceStopCommand = "am force-stop com.google.android.apps.nexuslauncher"
        if (ShizukuUtils.hasPermission()) {
            ShizukuUtils.runCommand(forceStopCommand)
        } else if (RootUtils.isRootPermissionGranted()) {
            RootUtils.runCommand(forceStopCommand)
        }
    }

    /**
     * Executes the set pixel searchbar widget id operation.
     *
     * @param id [Int] Target id.
     * @param provider [String?] Target provider.
     * @param context [Context] Target context.
     */
    fun setPixelSearchbarWidgetId(
        id: Int,
        provider: String?,
        context: Context,
    ) {
        pixelSearchbarWidgetId.intValue = id
        pixelSearchbarWidgetProvider.value = provider
        settingsRepository.setPixelSearchbarWidgetId(id)
        settingsRepository.setPixelSearchbarWidgetProvider(provider)
        updatePixelSearchbarWidget(context)
    }

    /**
     * Executes the clear pixel searchbar widget operation.
     *
     * @param context [Context] Target context.
     */
    fun clearPixelSearchbarWidget(context: Context) {
        pixelSearchbarWidgetId.intValue = android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
        pixelSearchbarWidgetProvider.value = null
        settingsRepository.setPixelSearchbarWidgetId(android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID)
        settingsRepository.setPixelSearchbarWidgetProvider(null)
        context.stopService(
            android.content.Intent(
                context,
                com.sameerasw.essentials.services.widgets.WidgetScraperService::class.java,
            ),
        )
        updatePixelSearchbarWidget(context)
    }

    /**
     * Executes the update pixel searchbar scraped text operation.
     *
     * @param line1 [String] Target line1.
     * @param line2 [String] Target line2.
     * @param context [Context] Target context.
     */
    fun updatePixelSearchbarScrapedText(
        line1: String,
        line2: String,
        context: Context,
    ) {
        pixelSearchbarScrapedLine1.value = line1
        pixelSearchbarScrapedLine2.value = line2
        settingsRepository.setPixelSearchbarScrapedLine1(line1)
        settingsRepository.setPixelSearchbarScrapedLine2(line2)
        updatePixelSearchbarWidget(context)
    }

    /**
     * Executes the set pixel searchbar widget padding h operation.
     *
     * @param value [Int] Target value.
     * @param context [Context] Target context.
     */
    fun setPixelSearchbarWidgetPaddingH(
        value: Int,
        context: Context,
    ) {
        pixelSearchbarWidgetPaddingH.intValue = value
        settingsRepository.setPixelSearchbarWidgetPaddingH(value)
        updatePixelSearchbarWidget(context)
    }

    /**
     * Executes the set pixel searchbar widget padding v operation.
     *
     * @param value [Int] Target value.
     * @param context [Context] Target context.
     */
    fun setPixelSearchbarWidgetPaddingV(
        value: Int,
        context: Context,
    ) {
        pixelSearchbarWidgetPaddingV.intValue = value
        settingsRepository.setPixelSearchbarWidgetPaddingV(value)
        updatePixelSearchbarWidget(context)
    }

    /**
     * Executes the set pixel searchbar tap action enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setPixelSearchbarTapActionEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        pixelSearchbarTapActionEnabled.value = enabled
        settingsRepository.setPixelSearchbarTapActionEnabled(enabled)
        updatePixelSearchbarWidget(context)
    }

    fun updatePixelSearchbarMusic(
        title: String,
        artist: String,
        packageName: String,
        context: Context,
    ) {
        pixelSearchbarMusicTitle.value = title
        pixelSearchbarMusicArtist.value = artist
        pixelSearchbarMusicPackage.value = packageName
        settingsRepository.setPixelSearchbarMusicTitle(title)
        settingsRepository.setPixelSearchbarMusicArtist(artist)
        settingsRepository.setPixelSearchbarMusicPackage(packageName)
        settingsRepository.incrementPixelSearchbarWidgetRevision()
        updatePixelSearchbarWidget(context)
    }

    /**
     * Executes the update media from active session operation.
     *
     * @param context [Context] Target context.
     */
    fun updateMediaFromActiveSession(context: Context) {
        try {
            val manager =
                context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? android.media.session.MediaSessionManager
                    ?: return
            val componentName =
                android.content.ComponentName(
                    context,
                    com.sameerasw.essentials.services.NotificationListener::class.java,
                )
            val sessions = manager.getActiveSessions(componentName)
            val activeSession =
                sessions
                    ?.sortedWith(
                        compareByDescending<android.media.session.MediaController> {
                            val state = it.playbackState?.state
                            state == android.media.session.PlaybackState.STATE_PLAYING ||
                                state == android.media.session.PlaybackState.STATE_BUFFERING
                        }.thenByDescending {
                            val state = it.playbackState?.state
                            state == android.media.session.PlaybackState.STATE_PAUSED
                        },
                    )?.firstOrNull()

            if (activeSession != null) {
                val metadata = activeSession.metadata
                val title =
                    metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: ""
                val artist =
                    metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                val packageName = activeSession.packageName

                val artwork =
                    metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                        ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
                        ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)

                val filesDirFile = File(context.filesDir, "music_artwork.png")
                if (artwork != null) {
                    try {
                        FileOutputStream(filesDirFile).use { out ->
                            artwork.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    } catch (_: Exception) {
                    }
                } else {
                    if (filesDirFile.exists()) filesDirFile.delete()
                }

                pixelSearchbarMusicTitle.value = title
                pixelSearchbarMusicArtist.value = artist
                pixelSearchbarMusicPackage.value = packageName
                settingsRepository.setPixelSearchbarMusicTitle(title)
                settingsRepository.setPixelSearchbarMusicArtist(artist)
                settingsRepository.setPixelSearchbarMusicPackage(packageName)
                settingsRepository.incrementPixelSearchbarWidgetRevision()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Executes the update pixel searchbar widget operation.
     *
     * @param context [Context] Target context.
     */
    fun updatePixelSearchbarWidget(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                val widget =
                    com.sameerasw.essentials.services.widgets
                        .PixelSearchbarWidget()
                val glanceIds =
                    manager.getGlanceIds(com.sameerasw.essentials.services.widgets.PixelSearchbarWidget::class.java)
                for (glanceId in glanceIds) {
                    widget.update(context, glanceId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Executes the set lock screen clock id operation.
     *
     * @param clockId [String] Target clock id.
     * @param context [Context] Target context.
     */
    fun setLockScreenClockId(
        clockId: String,
        context: Context,
    ) {
        val timestamp = System.currentTimeMillis()
        val json =
            if (lockScreenClockSelectedColorId.value == "DEFAULT") {
                "{\"clockId\":\"$clockId\",\"metadata\":{\"metadataSelectedColorId\":\"DEFAULT\",\"metadataColorToneProgress\":${lockScreenClockColorTone.intValue},\"appliedTimestamp\":$timestamp},\"axes\":[{\"key\":\"wght\",\"value\":${lockScreenClockWeight.intValue}},{\"key\":\"wdth\",\"value\":${lockScreenClockWidth.intValue}},{\"key\":\"ROND\",\"value\":${lockScreenClockRoundness.intValue}}]}"
            } else {
                "{\"clockId\":\"$clockId\",\"seedColor\":${lockScreenClockSeedColor.intValue},\"metadata\":{\"metadataSelectedColorId\":\"${lockScreenClockSelectedColorId.value}\",\"metadataColorToneProgress\":${lockScreenClockColorTone.intValue},\"appliedTimestamp\":$timestamp},\"axes\":[{\"key\":\"wght\",\"value\":${lockScreenClockWeight.intValue}},{\"key\":\"wdth\",\"value\":${lockScreenClockWidth.intValue}},{\"key\":\"ROND\",\"value\":${lockScreenClockRoundness.intValue}}]}"
            }
        val command = "settings put secure lock_screen_custom_clock_face '$json'"
        var success = false

        if (PermissionUtils.canWriteSecureSettings(context)) {
            try {
                success =
                    Settings.Secure.putString(
                        context.contentResolver,
                        "lock_screen_custom_clock_face",
                        json,
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!success) {
            if (ShizukuUtils.hasPermission()) {
                ShizukuUtils.runCommand(command)
                success = true
            } else if (RootUtils.isRootPermissionGranted()) {
                RootUtils.runCommand(command)
                success = true
            }
        }

        if (success) {
            lockScreenClockId.value = clockId
        }
    }

    /**
     * Executes the set lock screen clock weight operation.
     *
     * @param value [Int] Target value.
     * @param context [Context] Target context.
     */
    fun setLockScreenClockWeight(
        value: Int,
        context: Context,
    ) {
        lockScreenClockWeight.intValue = value
        settingsRepository.setLockScreenClockWeight(value)
        lockScreenClockId.value?.let { setLockScreenClockId(it, context) }
    }

    /**
     * Executes the set lock screen clock width operation.
     *
     * @param value [Int] Target value.
     * @param context [Context] Target context.
     */
    fun setLockScreenClockWidth(
        value: Int,
        context: Context,
    ) {
        lockScreenClockWidth.intValue = value
        settingsRepository.setLockScreenClockWidth(value)
        lockScreenClockId.value?.let { setLockScreenClockId(it, context) }
    }

    /**
     * Executes the set lock screen clock grade operation.
     *
     * @param value [Int] Target value.
     * @param context [Context] Target context.
     */
    fun setLockScreenClockGrade(
        value: Int,
        context: Context,
    ) {
        lockScreenClockGrade.intValue = value
        settingsRepository.setLockScreenClockGrade(value)
        lockScreenClockId.value?.let { setLockScreenClockId(it, context) }
    }

    /**
     * Executes the set lock screen clock roundness operation.
     *
     * @param value [Int] Target value.
     * @param context [Context] Target context.
     */
    fun setLockScreenClockRoundness(
        value: Int,
        context: Context,
    ) {
        lockScreenClockRoundness.intValue = value
        settingsRepository.setLockScreenClockRoundness(value)
        lockScreenClockId.value?.let { setLockScreenClockId(it, context) }
    }

    /**
     * Executes the set lock screen clock color tone operation.
     *
     * @param value [Int] Target value.
     * @param context [Context] Target context.
     */
    fun setLockScreenClockColorTone(
        value: Int,
        context: Context,
    ) {
        lockScreenClockColorTone.intValue = value
        settingsRepository.setLockScreenClockColorTone(value)

        // Update effective seed color based on new tone
        if (lockScreenClockSelectedColorId.value != "DEFAULT") {
            val effectiveSeed =
                calculateEffectiveSeedColor(lockScreenClockSelectedColorId.value, value)
            lockScreenClockSeedColor.intValue = effectiveSeed
            settingsRepository.setLockScreenClockSeedColor(effectiveSeed)
        }

        lockScreenClockId.value?.let { setLockScreenClockId(it, context) }
    }

    /**
     * Executes the set lock screen clock color operation.
     *
     * @param id [String] Target id.
     * @param seed [Int] Target seed.
     * @param context [Context] Target context.
     */
    fun setLockScreenClockColor(
        id: String,
        seed: Int,
        context: Context,
    ) {
        lockScreenClockSelectedColorId.value = id
        val effectiveSeed =
            if (id == "DEFAULT") {
                0
            } else {
                calculateEffectiveSeedColor(
                    id,
                    lockScreenClockColorTone.intValue,
                )
            }
        lockScreenClockSeedColor.intValue = effectiveSeed
        settingsRepository.setLockScreenClockSelectedColorId(id)
        settingsRepository.setLockScreenClockSeedColor(effectiveSeed)
        lockScreenClockId.value?.let { setLockScreenClockId(it, context) }
    }

    private fun calculateEffectiveSeedColor(
        colorId: String,
        tone: Int,
    ): Int {
        val baseColor =
            when (colorId) {
                "RED" -> android.graphics.Color.parseColor("#E57373")
                "GREEN" -> android.graphics.Color.parseColor("#81C784")
                "BLUE" -> android.graphics.Color.parseColor("#64B5F6")
                "YELLOW" -> android.graphics.Color.parseColor("#FFF176")
                "ORANGE" -> android.graphics.Color.parseColor("#FFB74D")
                "PURPLE" -> android.graphics.Color.parseColor("#BA68C8")
                "PINK" -> android.graphics.Color.parseColor("#F06292")
                "TEAL" -> android.graphics.Color.parseColor("#4DB6AC")
                else -> return 0
            }
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor, hsv)

        // Calibrated HSV mapping to match user examples:
        // Tone 0  -> Saturation ~0.8, Value ~0.35 (Dark, saturated)
        // Tone 100 -> Saturation ~0.2, Value ~1.0  (Light, pastel)
        hsv[1] = (0.8f - (tone / 100f) * 0.6f).coerceIn(0f, 1f)
        hsv[2] = (0.35f + (tone / 100f) * 0.65f).coerceIn(0f, 1f)

        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun readCurrentLockScreenClockId(context: Context): String? {
        return try {
            val raw =
                Settings.Secure.getString(
                    context.contentResolver,
                    "lock_screen_custom_clock_face",
                ) ?: return null
            // Extract clockId from JSON string like {"clockId":"DIGITAL_CLOCK_WEATHER"}
            val match = Regex(""""clockId":\s*"([^"]+)"""").find(raw)
            match?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun applyHideGestureBar(
        context: Context,
        enabled: Boolean,
    ) {
        if (enabled) {
            com.sameerasw.essentials.utils.StatusBarManager.requestDisable(
                context,
                "HideGestureBar",
                setOf(com.sameerasw.essentials.utils.StatusBarManager.FLAG_HOME),
            )
        } else {
            com.sameerasw.essentials.utils.StatusBarManager.requestRestore(
                context,
                "HideGestureBar",
            )
        }
    }

    /**
     * Executes the set skip silent notifications operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setSkipSilentNotifications(
        enabled: Boolean,
        context: Context,
    ) {
        skipSilentNotifications.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_EDGE_LIGHTING_SKIP_SILENT, enabled)
    }

    /**
     * Executes the set skip persistent notifications operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setSkipPersistentNotifications(
        enabled: Boolean,
        context: Context,
    ) {
        skipPersistentNotifications.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_EDGE_LIGHTING_SKIP_PERSISTENT, enabled)
    }

    /**
     * Executes the set notification lighting style operation.
     *
     * @param style [NotificationLightingStyle] Target style.
     * @param context [Context] Target context.
     */
    fun setNotificationLightingStyle(
        style: NotificationLightingStyle,
        context: Context,
    ) {
        if (style == NotificationLightingStyle.SYSTEM && !ShellUtils.hasPermission(context)) {
            // Permission handling should be done in UI, but we can ensure state consistency here
            return
        }
        notificationLightingStyle.value = style
        settingsRepository.putString(SettingsRepository.KEY_EDGE_LIGHTING_STYLE, style.name)
    }

    /**
     * Executes the set notification lighting system mode operation.
     *
     * @param mode [Int] Target mode.
     * @param context [Context] Target context.
     */
    fun setNotificationLightingSystemMode(
        mode: Int,
        context: Context,
    ) {
        notificationLightingSystemMode.intValue = mode
        settingsRepository.saveNotificationLightingSystemMode(mode)
    }

    /**
     * Executes the set notification lighting color mode operation.
     *
     * @param mode [NotificationLightingColorMode] Target mode.
     * @param context [Context] Target context.
     */
    fun setNotificationLightingColorMode(
        mode: NotificationLightingColorMode,
        context: Context,
    ) {
        notificationLightingColorMode.value = mode
        settingsRepository.putString(SettingsRepository.KEY_EDGE_LIGHTING_COLOR_MODE, mode.name)
    }

    /**
     * Executes the set notification lighting custom color operation.
     *
     * @param color [Int] Target color.
     * @param context [Context] Target context.
     */
    fun setNotificationLightingCustomColor(
        color: Int,
        context: Context,
    ) {
        notificationLightingCustomColor.intValue = color
        settingsRepository.putInt(SettingsRepository.KEY_EDGE_LIGHTING_CUSTOM_COLOR, color)
    }

    /**
     * Executes the set button remap enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setButtonRemapEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isButtonRemapEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_BUTTON_REMAP_ENABLED, enabled)
    }

    /**
     * Executes the set call vibrations enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setCallVibrationsEnabled(enabled: Boolean) {
        isCallVibrationsEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_CALL_VIBRATIONS_ENABLED, enabled)
    }

    /**
     * Executes the set button remap use shizuku operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setButtonRemapUseShizuku(
        enabled: Boolean,
        context: Context,
    ) {
        isButtonRemapUseShizuku.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_BUTTON_REMAP_USE_SHIZUKU, enabled)
    }

    /**
     * Executes the set volume up action off operation.
     *
     * @param action [Action?] Target action.
     * @param context [Context] Target context.
     */
    fun setVolumeUpActionOff(
        action: Action?,
        context: Context,
    ) {
        volumeUpActionOff.value = action
        settingsRepository.setRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF, action)
    }

    /**
     * Executes the set volume down action off operation.
     *
     * @param action [Action?] Target action.
     * @param context [Context] Target context.
     */
    fun setVolumeDownActionOff(
        action: Action?,
        context: Context,
    ) {
        volumeDownActionOff.value = action
        settingsRepository.setRemapAction(
            SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF,
            action,
        )
    }

    /**
     * Executes the set volume up action on operation.
     *
     * @param action [Action?] Target action.
     * @param context [Context] Target context.
     */
    fun setVolumeUpActionOn(
        action: Action?,
        context: Context,
    ) {
        volumeUpActionOn.value = action
        settingsRepository.setRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_ON, action)
    }

    /**
     * Executes the set volume down action on operation.
     *
     * @param action [Action?] Target action.
     * @param context [Context] Target context.
     */
    fun setVolumeDownActionOn(
        action: Action?,
        context: Context,
    ) {
        volumeDownActionOn.value = action
        settingsRepository.setRemapAction(SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON, action)
    }

    /**
     * Executes the set remap haptic type operation.
     *
     * @param type [HapticFeedbackType] Target type.
     * @param context [Context] Target context.
     */
    fun setRemapHapticType(
        type: HapticFeedbackType,
        context: Context,
    ) {
        remapHapticType.value = type
        settingsRepository.putString(SettingsRepository.KEY_BUTTON_REMAP_HAPTIC_TYPE, type.name)
    }

    /**
     * Executes the set dynamic night light enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setDynamicNightLightEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isDynamicNightLightEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_DYNAMIC_NIGHT_LIGHT_ENABLED, enabled)
        updateAppDetectionService(context)
    }

    /**
     * Executes the set smart pixels enabled operation.
     *
     * @param context [Context] Target context.
     * @param enabled [Boolean] Target enabled.
     */
    fun setSmartPixelsEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        isSmartPixelsEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_SMART_PIXELS_ENABLED, enabled)
    }

    /**
     * Executes the set smart pixels intensity operation.
     *
     * @param context [Context] Target context.
     * @param intensity [Float] Target intensity.
     */
    fun setSmartPixelsIntensity(
        context: Context,
        intensity: Float,
    ) {
        smartPixelsIntensity.floatValue = intensity
        settingsRepository.putFloat(SettingsRepository.KEY_SMART_PIXELS_INTENSITY, intensity)
    }

    /**
     * Executes the set smart pixels disable on cast enabled operation.
     *
     * @param context [Context] Target context.
     * @param enabled [Boolean] Target enabled.
     */
    fun setSmartPixelsDisableOnCastEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        isSmartPixelsDisableOnCastEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_SMART_PIXELS_DISABLE_ON_CAST, enabled)
    }

    /**
     * Executes the set app lock enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setAppLockEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isAppLockEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_APP_LOCK_ENABLED, enabled)
        updateAppDetectionService(context)
    }

    /**
     * Executes the set app lock auto lock delay index operation.
     *
     * @param index [Int] Target index.
     */
    fun setAppLockAutoLockDelayIndex(index: Int) {
        appLockAutoLockDelayIndex.intValue = index
        settingsRepository.putInt(SettingsRepository.KEY_APP_LOCK_AUTO_LOCK_DELAY_INDEX, index)
    }

    /**
     * Executes the set use usage access operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setUseUsageAccess(
        enabled: Boolean,
        context: Context,
    ) {
        isUseUsageAccess.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_USE_USAGE_ACCESS, enabled)
        updateAppDetectionService(context)
    }

    private fun updateAppDetectionService(context: Context) {
        com.sameerasw.essentials.utils.ServiceUtils
            .startRequiredServices(context)
    }

    val isLikeSongToastEnabled = mutableStateOf(false)

    /**
     * Executes the set like song toast enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setLikeSongToastEnabled(enabled: Boolean) {
        isLikeSongToastEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_LIKE_SONG_TOAST_ENABLED, enabled)
    }

    val isLikeSongAodOverlayEnabled = mutableStateOf(false)

    /**
     * Executes the set like song aod overlay enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setLikeSongAodOverlayEnabled(enabled: Boolean) {
        isLikeSongAodOverlayEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_LIKE_SONG_AOD_OVERLAY_ENABLED, enabled)
    }

    val isAmbientMusicGlanceEnabled = mutableStateOf(false)

    /**
     * Executes the set ambient music glance enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setAmbientMusicGlanceEnabled(enabled: Boolean) {
        isAmbientMusicGlanceEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ENABLED, enabled)
    }

    /**
     * Executes the set ambient music glance random shapes enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setAmbientMusicGlanceRandomShapesEnabled(enabled: Boolean) {
        isAmbientMusicGlanceRandomShapesEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_RANDOM_SHAPES,
            enabled,
        )
    }

    /**
     * Executes the set ambient music glance album art mode operation.
     *
     * @param mode [String] Target mode.
     */
    fun setAmbientMusicGlanceAlbumArtMode(mode: String) {
        ambientMusicGlanceAlbumArtMode.value = mode
        settingsRepository.setAmbientMusicGlanceAlbumArtMode(mode)
    }

    /**
     * Executes the set ambient music glance clock size operation.
     *
     * @param size [Int] Target size.
     */
    fun setAmbientMusicGlanceClockSize(size: Int) {
        ambientMusicGlanceClockSize.intValue = size
        settingsRepository.setAmbientMusicGlanceClockSize(size)
    }

    /**
     * Executes the set ambient music glance clock weight operation.
     *
     * @param weight [Int] Target weight.
     */
    fun setAmbientMusicGlanceClockWeight(weight: Int) {
        ambientMusicGlanceClockWeight.intValue = weight
        settingsRepository.setAmbientMusicGlanceClockWeight(weight)
    }

    /**
     * Executes the set ambient music glance clock width operation.
     *
     * @param width [Int] Target width.
     */
    fun setAmbientMusicGlanceClockWidth(width: Int) {
        ambientMusicGlanceClockWidth.intValue = width
        settingsRepository.setAmbientMusicGlanceClockWidth(width)
    }

    /**
     * Executes the set ambient music glance clock roundness operation.
     *
     * @param roundness [Int] Target roundness.
     */
    fun setAmbientMusicGlanceClockRoundness(roundness: Int) {
        ambientMusicGlanceClockRoundness.intValue = roundness
        settingsRepository.setAmbientMusicGlanceClockRoundness(roundness)
    }

    /**
     * Executes the set ambient music glance force fill while charging enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setAmbientMusicGlanceForceFillWhileChargingEnabled(enabled: Boolean) {
        isAmbientMusicGlanceForceFillWhileChargingEnabled.value = enabled
        settingsRepository.setAmbientMusicGlanceForceFillWhileChargingEnabled(enabled)
    }

    /**
     * Executes the set ambient music glance respect notifications enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setAmbientMusicGlanceRespectNotificationsEnabled(enabled: Boolean) {
        isAmbientMusicGlanceRespectNotificationsEnabled.value = enabled
        settingsRepository.setAmbientMusicGlanceRespectNotificationsEnabled(enabled)
    }

    /**
     * Executes the switch scale animations mode operation.
     *
     * @param mode [String] Target mode.
     */
    fun switchScaleAnimationsMode(mode: String) {
        val oldMode = scaleAnimationsMode.value
        if (oldMode == mode) return

        // 1. Save current state to old profile slot
        val currentProfile =
            ScaleAnimationsProfile(
                fontScale = fontScale.floatValue,
                fontWeight = fontWeight.intValue,
                animatorDurationScale = animatorDurationScale.floatValue,
                transitionAnimationScale = transitionAnimationScale.floatValue,
                windowAnimationScale = windowAnimationScale.floatValue,
                smallestWidth = smallestWidth.intValue,
                touchSensitivityEnabled = isTouchSensitivityEnabled.value,
                autoRotateEnabled = isAutoRotateEnabled.value,
                screenTimeout = screenTimeout.value,
            )
        settingsRepository.saveScaleAnimationsProfile(oldMode, currentProfile)

        // 2. Load new profile
        val newProfile = settingsRepository.getScaleAnimationsProfile(mode)

        // 3. Update mode
        scaleAnimationsMode.value = mode
        settingsRepository.setScaleAnimationsMode(mode)

        // 4. Apply new profile
        setFontScale(newProfile.fontScale)
        setFontWeight(newProfile.fontWeight)
        setAnimationScale(
            Settings.Global.ANIMATOR_DURATION_SCALE,
            newProfile.animatorDurationScale,
        )
        setAnimationScale(
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            newProfile.transitionAnimationScale,
        )
        setAnimationScale(
            Settings.Global.WINDOW_ANIMATION_SCALE,
            newProfile.windowAnimationScale,
        )
        setSmallestWidth(newProfile.smallestWidth)
        setTouchSensitivityEnabled(newProfile.touchSensitivityEnabled)
        setAutoRotateEnabled(newProfile.autoRotateEnabled)
        setScreenTimeout(newProfile.screenTimeout)
    }

    /**
     * Executes the set touch sensitivity enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setTouchSensitivityEnabled(enabled: Boolean) {
        isTouchSensitivityEnabled.value = enabled
        settingsRepository.setTouchSensitivityEnabled(enabled)
    }

    /**
     * Executes the set auto rotate enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setAutoRotateEnabled(enabled: Boolean) {
        isAutoRotateEnabled.value = enabled
        settingsRepository.setAutoRotateEnabled(enabled)
    }

    /**
     * Executes the restart system ui operation.
     */
    fun restartSystemUI() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()) {
                    ShizukuUtils.runCommand("am crash com.android.systemui")
                } else if (RootUtils.isRootAvailable() && RootUtils.isRootPermissionGranted()) {
                    RootUtils.runCommand("am crash com.android.systemui")
                }
            }
        }
    }

    /**
     * Executes the set screen timeout operation.
     *
     * @param timeoutMs [Long] Target timeout ms.
     */
    fun setScreenTimeout(timeoutMs: Long) {
        screenTimeout.value = timeoutMs
        settingsRepository.setScreenTimeout(timeoutMs)
    }

    /**
     * Executes the set refresh rate mode operation.
     *
     * @param mode [String] Target mode.
     */
    fun setRefreshRateMode(mode: String) {
        refreshRateMode.value = mode
        if (mode == RefreshRateUtils.MODE_RANGE) {
            if (minRefreshRate.floatValue <= 0f && peakRefreshRate.floatValue <= 0f) {
                val seedValue =
                    when {
                        fixedRefreshRate.floatValue > 0f ->
                            RefreshRateUtils.normalizeRate(
                                fixedRefreshRate.floatValue,
                            )

                        else -> 60f
                    }
                minRefreshRate.floatValue = seedValue
                peakRefreshRate.floatValue = seedValue
            }
        } else if (fixedRefreshRate.floatValue <= 0f) {
            fixedRefreshRate.floatValue =
                when {
                    peakRefreshRate.floatValue > 0f -> RefreshRateUtils.normalizeRate(peakRefreshRate.floatValue)
                    minRefreshRate.floatValue > 0f -> RefreshRateUtils.normalizeRate(minRefreshRate.floatValue)
                    else -> 60f
                }
        }
        settingsRepository.setRefreshRateMode(mode)
    }

    /**
     * Executes the update fixed refresh rate operation.
     *
     * @param value [Float] Target value.
     */
    fun updateFixedRefreshRate(value: Float) {
        fixedRefreshRate.floatValue = value
    }

    /**
     * Executes the update min refresh rate operation.
     *
     * @param value [Float] Target value.
     */
    fun updateMinRefreshRate(value: Float) {
        val safeMin = value.coerceAtMost(peakRefreshRate.floatValue.takeIf { it > 0f } ?: value)
        minRefreshRate.floatValue = safeMin
        if (peakRefreshRate.floatValue > 0f && peakRefreshRate.floatValue < safeMin) {
            peakRefreshRate.floatValue = safeMin
        }
    }

    /**
     * Executes the update peak refresh rate operation.
     *
     * @param value [Float] Target value.
     */
    fun updatePeakRefreshRate(value: Float) {
        val safePeak = value.coerceAtLeast(minRefreshRate.floatValue.takeIf { it > 0f } ?: value)
        peakRefreshRate.floatValue = safePeak
        if (minRefreshRate.floatValue > safePeak) {
            minRefreshRate.floatValue = safePeak
        }
    }

    /**
     * Executes the apply fixed refresh rate operation.
     *
     * @param context [Context] Target context.
     */
    fun applyFixedRefreshRate(context: Context) {
        val value = fixedRefreshRate.floatValue
        if (value <= 0f) {
            resetRefreshRate(context)
            return
        }

        if (RefreshRateUtils.applyFixedRefreshRate(context, value)) {
            val normalized = RefreshRateUtils.normalizeRate(value)
            fixedRefreshRate.floatValue = normalized
            minRefreshRate.floatValue = normalized
            peakRefreshRate.floatValue = normalized
            refreshRateMode.value = RefreshRateUtils.MODE_FIXED
            persistRefreshRateStateIfNeeded(
                mode = RefreshRateUtils.MODE_FIXED,
                fixed = normalized,
                min = normalized,
                peak = normalized,
            )
        } else {
            syncRefreshRateState(context)
        }
    }

    /**
     * Executes the apply refresh rate range operation.
     *
     * @param context [Context] Target context.
     */
    fun applyRefreshRateRange(context: Context) {
        val minValue = minRefreshRate.floatValue
        val peakValue = peakRefreshRate.floatValue
        if (minValue <= 0f || peakValue <= 0f) {
            persistRefreshRateStateIfNeeded(
                mode = RefreshRateUtils.MODE_RANGE,
                fixed = fixedRefreshRate.floatValue,
                min = minValue,
                peak = peakValue,
            )
            return
        }

        if (RefreshRateUtils.applyRangeRefreshRate(context, minValue, peakValue)) {
            val normalizedMin = RefreshRateUtils.normalizeRate(minValue)
            val normalizedPeak = RefreshRateUtils.normalizeRate(maxOf(minValue, peakValue))
            minRefreshRate.floatValue = normalizedMin
            peakRefreshRate.floatValue = normalizedPeak
            fixedRefreshRate.floatValue = normalizedPeak
            refreshRateMode.value = RefreshRateUtils.MODE_RANGE
            persistRefreshRateStateIfNeeded(
                mode = RefreshRateUtils.MODE_RANGE,
                fixed = normalizedPeak,
                min = normalizedMin,
                peak = normalizedPeak,
            )
        } else {
            syncRefreshRateState(context)
        }
    }

    /**
     * Executes the reset refresh rate operation.
     *
     * @param context [Context] Target context.
     */
    fun resetRefreshRate(context: Context) {
        val restoreInfinityPeak = settingsRepository.shouldRestoreInfinityPeakOnRefreshRateReset()
        if (RefreshRateUtils.resetRefreshRate(context, restoreInfinityPeak)) {
            fixedRefreshRate.floatValue = 0f
            minRefreshRate.floatValue = 0f
            peakRefreshRate.floatValue = 0f
            persistRefreshRateStateIfNeeded(
                mode = refreshRateMode.value,
                fixed = 0f,
                min = 0f,
                peak = 0f,
            )
        } else {
            syncRefreshRateState(context)
        }
    }

    private fun syncRefreshRateState(context: Context) {
        val refreshRateState = RefreshRateUtils.getCurrentState(context)
        if (refreshRateState.isSystemManaged) {
            settingsRepository.setRestoreInfinityPeakOnRefreshRateReset(
                refreshRateState.usesInfinityDefaultPeak,
            )
        }
        val actualMin = refreshRateState.min
        val actualPeak = refreshRateState.peak
        val hasCustom = !refreshRateState.isSystemManaged && (actualMin > 0f || actualPeak > 0f)
        val storedMode = settingsRepository.getRefreshRateMode()

        if (!hasCustom) {
            fixedRefreshRate.floatValue = 0f
            minRefreshRate.floatValue = 0f
            peakRefreshRate.floatValue = 0f
            persistRefreshRateStateIfNeeded(
                mode = storedMode,
                fixed = 0f,
                min = 0f,
                peak = 0f,
            )
            return
        }

        val resolvedMin = if (actualMin > 0f) actualMin else actualPeak
        val resolvedPeak = if (actualPeak > 0f) actualPeak else actualMin
        val resolvedMode =
            if (resolvedMin > 0f && resolvedPeak > 0f && resolvedMin != resolvedPeak) {
                RefreshRateUtils.MODE_RANGE
            } else {
                storedMode
            }

        refreshRateMode.value = resolvedMode
        fixedRefreshRate.floatValue = resolvedPeak
        minRefreshRate.floatValue = resolvedMin
        peakRefreshRate.floatValue = resolvedPeak
        persistRefreshRateStateIfNeeded(
            mode = resolvedMode,
            fixed = resolvedPeak,
            min = resolvedMin,
            peak = resolvedPeak,
        )
    }

    private fun persistRefreshRateStateIfNeeded(
        mode: String,
        fixed: Float,
        min: Float,
        peak: Float,
    ) {
        val storedMode = settingsRepository.getRefreshRateMode()
        val storedFixed = settingsRepository.getFloat(SettingsRepository.KEY_REFRESH_RATE_FIXED, 0f)
        val storedMin = settingsRepository.getFloat(SettingsRepository.KEY_REFRESH_RATE_MIN, 0f)
        val storedPeak = settingsRepository.getFloat(SettingsRepository.KEY_REFRESH_RATE_PEAK, 0f)

        if (storedMode == mode &&
            storedFixed == fixed &&
            storedMin == min &&
            storedPeak == peak
        ) {
            return
        }

        settingsRepository.saveRefreshRateState(
            mode = mode,
            fixed = fixed,
            min = min,
            peak = peak,
        )
    }

    /**
     * Executes the update font scale operation.
     *
     * @param scale [Float] Target scale.
     */
    fun updateFontScale(scale: Float) {
        fontScale.floatValue = scale
    }

    /**
     * Executes the save font scale operation.
     */
    fun saveFontScale() {
        settingsRepository.setFontScale(fontScale.floatValue)
    }

    /**
     * Executes the set font scale operation.
     *
     * @param scale [Float] Target scale.
     */
    fun setFontScale(scale: Float) {
        fontScale.floatValue = scale
        settingsRepository.setFontScale(scale)
    }

    /**
     * Executes the set font weight operation.
     *
     * @param weight [Int] Target weight.
     */
    fun setFontWeight(weight: Int) {
        fontWeight.intValue = weight
        settingsRepository.setFontWeight(weight)
    }

    /**
     * Executes the set animation scale operation.
     *
     * @param key [String] Target key.
     * @param scale [Float] Target scale.
     */
    fun setAnimationScale(
        key: String,
        scale: Float,
    ) {
        when (key) {
            Settings.Global.ANIMATOR_DURATION_SCALE ->
                animatorDurationScale.floatValue =
                    scale

            Settings.Global.TRANSITION_ANIMATION_SCALE ->
                transitionAnimationScale.floatValue =
                    scale

            Settings.Global.WINDOW_ANIMATION_SCALE ->
                windowAnimationScale.floatValue =
                    scale
        }
        settingsRepository.setAnimationScale(key, scale)
    }

    /**
     * Executes the reset text to default operation.
     */
    fun resetTextToDefault() {
        setFontScale(1.0f)
        setFontWeight(0)
    }

    /**
     * Executes the reset animations to default operation.
     */
    fun resetAnimationsToDefault() {
        setAnimationScale(Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        setAnimationScale(Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f)
        setAnimationScale(Settings.Global.WINDOW_ANIMATION_SCALE, 1.0f)
    }

    /**
     * Executes the update smallest width operation.
     *
     * @param width [Int] Target width.
     */
    fun updateSmallestWidth(width: Int) {
        smallestWidth.intValue = width
    }

    /**
     * Executes the save smallest width operation.
     */
    fun saveSmallestWidth() {
        settingsRepository.setSmallestWidth(smallestWidth.intValue)
    }

    /**
     * Executes the set smallest width operation.
     *
     * @param width [Int] Target width.
     */
    fun setSmallestWidth(width: Int) {
        smallestWidth.intValue = width
        settingsRepository.setSmallestWidth(width)
    }

    /**
     * Executes the reset scale to default operation.
     */
    fun resetScaleToDefault() {
        settingsRepository.resetSmallestWidth()
        smallestWidth.intValue = settingsRepository.getSmallestWidth()
    }

    /**
     * Executes the set ambient music glance docked mode enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setAmbientMusicGlanceDockedModeEnabled(enabled: Boolean) {
        isAmbientMusicGlanceDockedModeEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_DOCKED_MODE,
            enabled,
        )
    }

    /**
     * Executes the set calendar sync enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setCalendarSyncEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isCalendarSyncEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_CALENDAR_SYNC_ENABLED, enabled)
        if (enabled) {
            com.sameerasw.essentials.services.CalendarSyncManager
                .forceSync(context)
            if (isCalendarSyncPeriodicEnabled.value) {
                schedulePeriodicCalendarSync(context)
            }
        } else {
            cancelPeriodicCalendarSync(context)
        }
    }

    fun setNotificationSyncEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isNotificationSyncEnabled.value = enabled
        settingsRepository.putBoolean("watch_notif_sync_enabled", enabled)
    }

    fun setCallSyncEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isCallSyncEnabled.value = enabled
        settingsRepository.putBoolean("watch_call_sync_enabled", enabled)
    }

    /**
     * Executes the fetch calendars operation.
     *
     * @param context [Context] Target context.
     */
    fun fetchCalendars(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val savedSelected = settingsRepository.getCalendarSyncSelectedCalendars()
            withContext(Dispatchers.Main) {
                selectedCalendarIds.value = savedSelected
            }

            val calendars = mutableListOf<CalendarAccount>()
            val projection =
                arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.ACCOUNT_NAME,
                    CalendarContract.Calendars.CALENDAR_COLOR,
                )

            context.contentResolver
                .query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    val nameColumn = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                    val accountColumn = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn) ?: "Unnamed Calendar"
                        val account = cursor.getString(accountColumn) ?: "Local"

                        calendars.add(
                            CalendarAccount(
                                id,
                                name,
                                account,
                                selectedCalendarIds.value.contains(id.toString()),
                            ),
                        )
                    }
                }

            withContext(Dispatchers.Main) {
                availableCalendars.clear()
                availableCalendars.addAll(calendars)
            }
        }
    }

    /**
     * Executes the toggle calendar selection operation.
     *
     * @param calendarId [Long] Target calendar id.
     */
    fun toggleCalendarSelection(calendarId: Long, context: Context? = null) {
        val currentIds = selectedCalendarIds.value.toMutableSet()
        val idString = calendarId.toString()
        if (currentIds.contains(idString)) {
            currentIds.remove(idString)
        } else {
            currentIds.add(idString)
        }
        selectedCalendarIds.value = currentIds
        settingsRepository.saveCalendarSyncSelectedCalendars(currentIds)

        // Update availableCalendars list
        val index = availableCalendars.indexOfFirst { it.id == calendarId }
        if (index != -1) {
            availableCalendars[index] =
                availableCalendars[index].copy(isSelected = currentIds.contains(idString))
        }

        context?.let {
            com.sameerasw.essentials.services.CalendarSyncManager.forceSync(it)
        }
    }

    /**
     * Executes the set calendar sync periodic enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setCalendarSyncPeriodicEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isCalendarSyncPeriodicEnabled.value = enabled
        settingsRepository.setCalendarSyncPeriodicEnabled(enabled)
        if (enabled && isCalendarSyncEnabled.value) {
            schedulePeriodicCalendarSync(context)
        } else {
            cancelPeriodicCalendarSync(context)
        }
    }

    private fun schedulePeriodicCalendarSync(context: Context) {
        val workRequest =
            PeriodicWorkRequestBuilder<com.sameerasw.essentials.services.CalendarSyncWorker>(
                15,
                java.util.concurrent.TimeUnit.MINUTES,
            ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "calendar_sync_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    private fun cancelPeriodicCalendarSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("calendar_sync_work")
    }

    /**
     * Executes the trigger calendar sync now operation.
     *
     * @param context [Context] Target context.
     */
    fun triggerCalendarSyncNow(context: Context) {
        com.sameerasw.essentials.services.CalendarSyncManager
            .forceSync(context)
    }

    /**
     * Executes the set freeze when locked enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFreezeWhenLockedEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isFreezeWhenLockedEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FREEZE_WHEN_LOCKED_ENABLED, enabled)
    }

    /**
     * Executes the set freeze dont freeze active apps enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFreezeDontFreezeActiveAppsEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isFreezeDontFreezeActiveAppsEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_FREEZE_DONT_FREEZE_ACTIVE_APPS,
            enabled,
        )
    }

    /**
     * Executes the set freeze lock delay index operation.
     *
     * @param index [Int] Target index.
     * @param context [Context] Target context.
     */
    fun setFreezeLockDelayIndex(
        index: Int,
        context: Context,
    ) {
        freezeLockDelayIndex.intValue = index
        settingsRepository.putInt(SettingsRepository.KEY_FREEZE_LOCK_DELAY_INDEX, index)
    }

    /**
     * Executes the set freeze show in launcher enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFreezeShowInLauncherEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isFreezeShowInLauncherEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FREEZE_SHOW_IN_LAUNCHER, enabled)

        // Dynamically enable or disable the AppFreezingLauncher activity-alias component
        val componentName = ComponentName(context, "com.sameerasw.essentials.AppFreezingLauncher")
        try {
            context.packageManager.setComponentEnabledSetting(
                componentName,
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the save notification lighting pulse count operation.
     *
     * @param context [Context] Target context.
     * @param count [Float] Target count.
     */
    fun saveNotificationLightingPulseCount(
        context: Context,
        count: Float,
    ) {
        notificationLightingPulseCount.value = count
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_PULSE_COUNT, count)
    }

    /**
     * Executes the save notification lighting pulse duration operation.
     *
     * @param context [Context] Target context.
     * @param duration [Float] Target duration.
     */
    fun saveNotificationLightingPulseDuration(
        context: Context,
        duration: Float,
    ) {
        notificationLightingPulseDuration.value = duration
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_PULSE_DURATION, duration)
    }

    /**
     * Executes the set flashlight pulse enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFlashlightPulseEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isFlashlightPulseEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FLASHLIGHT_PULSE_ENABLED, enabled)
    }

    /**
     * Executes the set flashlight pulse facedown only operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFlashlightPulseFacedownOnly(
        enabled: Boolean,
        context: Context,
    ) {
        isFlashlightPulseFacedownOnly.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_FLASHLIGHT_PULSE_FACEDOWN_ONLY,
            enabled,
        )
    }

    /**
     * Executes the set flashlight pulse use lighting apps operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFlashlightPulseUseLightingApps(
        enabled: Boolean,
        context: Context,
    ) {
        isFlashlightPulseUseLightingApps.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_FLASHLIGHT_PULSE_SAME_AS_LIGHTING,
            enabled,
        )
    }

    /**
     * Executes the set flashlight pulse max intensity operation.
     *
     * @param intensity [Float] Target intensity.
     */
    fun setFlashlightPulseMaxIntensity(intensity: Float) {
        flashlightPulseMaxIntensity.floatValue = intensity
        settingsRepository.putFloat(
            SettingsRepository.KEY_FLASHLIGHT_PULSE_MAX_INTENSITY,
            intensity,
        )
    }

    /**
     * Executes the set flashlight pulse disable on dnd operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFlashlightPulseDisableOnDnd(
        enabled: Boolean,
        context: Context,
    ) {
        isFlashlightPulseDisableOnDnd.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_FLASHLIGHT_PULSE_DISABLE_ON_DND,
            enabled,
        )
    }

    /**
     * Executes the preview flashlight pulse operation.
     *
     * @param context [Context] Target context.
     */
    fun previewFlashlightPulse(context: Context) {
        val intent =
            Intent(context, FlashlightActionReceiver::class.java).apply {
                action = FlashlightActionReceiver.ACTION_PULSE_NOTIFICATION
                putExtra(FlashlightActionReceiver.EXTRA_IS_PREVIEW, true)
            }
        context.sendBroadcast(intent)
    }

    private fun Intent.addLightingExtras(
        cornerRadiusDp: Float? = null,
        strokeThicknessDp: Float? = null,
        isPreview: Boolean = true,
        styleOverride: NotificationLightingStyle? = null,
    ) {
        val radius =
            cornerRadiusDp
                ?: settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_CORNER_RADIUS, 20f)
        val thickness =
            strokeThicknessDp
                ?: settingsRepository.getFloat(
                    SettingsRepository.KEY_EDGE_LIGHTING_STROKE_THICKNESS,
                    8f,
                )

        putExtra("corner_radius_dp", radius)
        putExtra("stroke_thickness_dp", thickness)
        putExtra("is_preview", isPreview)
        putExtra("ignore_screen_state", true)
        putExtra("style", (styleOverride ?: notificationLightingStyle.value).name)
        putExtra("color_mode", notificationLightingColorMode.value.name)
        putExtra("custom_color", notificationLightingCustomColor.intValue)
        putExtra("pulse_count", notificationLightingPulseCount.value.toInt())
        putExtra("pulse_duration", notificationLightingPulseDuration.value.toLong())
        putExtra(
            "glow_sides",
            notificationLightingGlowSides.value.map { it.name }.toTypedArray(),
        )
        putExtra("indicator_x", notificationLightingIndicatorX.value)
        putExtra("indicator_y", notificationLightingIndicatorY.value)
        putExtra("indicator_scale", notificationLightingIndicatorScale.value)
        putExtra("sweep_position", notificationLightingSweepPosition.value.name)
        putExtra("sweep_thickness", notificationLightingSweepThickness.floatValue)
        putExtra("random_shapes", notificationLightingSweepRandomShapes.value)
        putExtra("system_lighting_mode", notificationLightingSystemMode.intValue)
    }

    /**
     * Executes the trigger notification lighting operation.
     *
     * @param context [Context] Target context.
     */
    fun triggerNotificationLighting(context: Context) {
        if (notificationLightingStyle.value == NotificationLightingStyle.SYSTEM) {
            triggerNotificationLightingSystem(context)
            return
        }
        try {
            val intent =
                Intent(context, NotificationLightingService::class.java).apply {
                    addLightingExtras(isPreview = false)
                }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Executes the trigger notification lighting system operation.
     *
     * @param context [Context] Target context.
     */
    fun triggerNotificationLightingSystem(context: Context) {
        if (!ShellUtils.hasPermission(context)) return

        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val metrics = android.util.DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val centerX = metrics.widthPixels / 2
        val centerY = metrics.heightPixels / 2

        val command =
            if (notificationLightingSystemMode.intValue == 0) {
                "cmd statusbar charging-ripple"
            } else if (notificationLightingSystemMode.intValue == 1) {
                "cmd statusbar auth-ripple custom $centerX $centerY"
            } else {
                val posX = (notificationLightingIndicatorX.value / 100f * metrics.widthPixels).toInt()
                val posY = (notificationLightingIndicatorY.value / 100f * metrics.heightPixels).toInt()
                "cmd statusbar auth-ripple custom $posX $posY"
            }

        ShellUtils.runCommand(context, command)
    }

    // Helper to show the overlay service
    fun triggerNotificationLightingPreview(context: Context) {
        try {
            val intent =
                Intent(context, NotificationLightingService::class.java).apply {
                    addLightingExtras(isPreview = true)
                }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Executes the open ime settings operation.
     *
     * @param context [Context] Target context.
     */
    fun openImeSettings(context: Context) {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Executes the request write secure settings permission operation.
     *
     * @param context [Context] Target context.
     */
    fun requestWriteSecureSettingsPermission(context: Context) {
        val adbCommand =
            "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("adb_command", adbCommand)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Executes the request usage stats permission operation.
     *
     * @param context [Context] Target context.
     */
    fun requestUsageStatsPermission(context: Context) {
        PermissionUtils.openUsageStatsSettings(context)
    }

    /**
     * Executes the request write settings permission operation.
     *
     * @param context [Context] Target context.
     */
    fun requestWriteSettingsPermission(context: Context) {
        PermissionUtils.openWriteSettings(context)
    }

    /**
     * Executes the show ime picker operation.
     *
     * @param context [Context] Target context.
     */
    fun showImePicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }

    /**
     * Executes the trigger notification lighting with radius operation.
     *
     * @param context [Context] Target context.
     * @param cornerRadiusDp [Float] Target corner radius dp.
     */
    fun triggerNotificationLightingWithRadius(
        context: Context,
        cornerRadiusDp: Float,
    ) {
        try {
            val intent =
                Intent(context, NotificationLightingService::class.java).apply {
                    addLightingExtras(cornerRadiusDp = cornerRadiusDp)
                }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun triggerNotificationLightingWithRadiusAndThickness(
        context: Context,
        cornerRadiusDp: Float,
        strokeThicknessDp: Float,
    ) {
        try {
            val intent =
                Intent(context, NotificationLightingService::class.java).apply {
                    addLightingExtras(cornerRadiusDp, strokeThicknessDp)
                }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun triggerNotificationLightingForIndicator(
        context: Context,
        x: Float,
        y: Float,
        scale: Float,
    ) {
        notificationLightingIndicatorX.value = x
        notificationLightingIndicatorY.value = y
        notificationLightingIndicatorScale.value = scale

        try {
            val intent =
                Intent(context, NotificationLightingService::class.java).apply {
                    addLightingExtras(styleOverride = NotificationLightingStyle.INDICATOR)
                }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun triggerNotificationLightingForSweep(
        context: Context,
        position: NotificationLightingSweepPosition,
        thickness: Float,
    ) {
        notificationLightingSweepPosition.value = position
        notificationLightingSweepThickness.floatValue = thickness

        try {
            val intent =
                Intent(context, NotificationLightingService::class.java).apply {
                    addLightingExtras(styleOverride = NotificationLightingStyle.SWEEP)
                }
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    // Helper to remove preview overlay
    fun removePreviewOverlay(context: Context) {
        try {
            val intent1 =
                Intent(context, NotificationLightingService::class.java).apply {
                    putExtra("remove_preview", true)
                }
            context.startService(intent1)

            // Also remove from ScreenOffAccessibilityService if it's running
            val intent2 =
                Intent(context, ScreenOffAccessibilityService::class.java).apply {
                    action = "SHOW_NOTIFICATION_LIGHTING"
                    putExtra("remove_preview", true)
                }
            context.startService(intent2)
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Executes the set haptic feedback operation.
     *
     * @param type [HapticFeedbackType] Target type.
     * @param context [Context] Target context.
     */
    fun setHapticFeedback(
        type: HapticFeedbackType,
        context: Context,
    ) {
        hapticFeedbackType.value = type
        settingsRepository.putString(SettingsRepository.KEY_HAPTIC_FEEDBACK_TYPE, type.name)
    }

    /**
     * Executes the set default tab operation.
     *
     * @param tab [com.sameerasw.essentials.domain.DIYTabs] Target tab.
     * @param context [Context] Target context.
     */
    fun setDefaultTab(
        tab: com.sameerasw.essentials.domain.DIYTabs,
        context: Context,
    ) {
        defaultTab.value = tab
        settingsRepository.saveDIYTab(tab)
    }

    fun setAppIcon(
        appIcon: AppIcon,
        context: Context,
    ) {
        selectedAppIcon.value = appIcon
        settingsRepository.setAppIcon(appIcon)
        AppIconUtil.setAppIcon(context, appIcon)
    }

    /**
     * Executes the set keyboard height operation.
     *
     * @param height [Float] Target height.
     * @param context [Context] Target context.
     */
    fun setKeyboardHeight(
        height: Float,
        context: Context,
    ) {
        keyboardHeight.floatValue = height
        settingsRepository.putFloat(SettingsRepository.KEY_KEYBOARD_HEIGHT, height)
    }

    /**
     * Executes the set keyboard bottom padding operation.
     *
     * @param padding [Float] Target padding.
     * @param context [Context] Target context.
     */
    fun setKeyboardBottomPadding(
        padding: Float,
        context: Context,
    ) {
        keyboardBottomPadding.floatValue = padding
        settingsRepository.putFloat(SettingsRepository.KEY_KEYBOARD_BOTTOM_PADDING, padding)
    }

    /**
     * Executes the set keyboard roundness operation.
     *
     * @param roundness [Float] Target roundness.
     * @param context [Context] Target context.
     */
    fun setKeyboardRoundness(
        roundness: Float,
        context: Context,
    ) {
        keyboardRoundness.floatValue = roundness
        settingsRepository.putFloat(SettingsRepository.KEY_KEYBOARD_ROUNDNESS, roundness)
    }

    /**
     * Executes the set keyboard haptics enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setKeyboardHapticsEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isKeyboardHapticsEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_KEYBOARD_HAPTICS_ENABLED, enabled)
    }

    /**
     * Executes the set keyboard functions bottom operation.
     *
     * @param isBottom [Boolean] Target is bottom.
     * @param context [Context] Target context.
     */
    fun setKeyboardFunctionsBottom(
        isBottom: Boolean,
        context: Context,
    ) {
        isKeyboardFunctionsBottom.value = isBottom
        settingsRepository.putBoolean(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_BOTTOM, isBottom)
    }

    /**
     * Executes the set keyboard functions padding operation.
     *
     * @param padding [Float] Target padding.
     * @param context [Context] Target context.
     */
    fun setKeyboardFunctionsPadding(
        padding: Float,
        context: Context,
    ) {
        keyboardFunctionsPadding.floatValue = padding
        settingsRepository.putFloat(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_PADDING, padding)
    }

    /**
     * Executes the set keyboard haptic strength operation.
     *
     * @param strength [Float] Target strength.
     * @param context [Context] Target context.
     */
    fun setKeyboardHapticStrength(
        strength: Float,
        context: Context,
    ) {
        keyboardHapticStrength.floatValue = strength
        settingsRepository.putFloat(SettingsRepository.KEY_KEYBOARD_HAPTIC_STRENGTH, strength)
    }

    /**
     * Executes the set keyboard shape operation.
     *
     * @param shape [Int] Target shape.
     * @param context [Context] Target context.
     */
    fun setKeyboardShape(
        shape: Int,
        context: Context,
    ) {
        keyboardShape.intValue = shape
        settingsRepository.putInt(SettingsRepository.KEY_KEYBOARD_SHAPE, shape)
    }

    /**
     * Executes the set keyboard always dark operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setKeyboardAlwaysDark(
        enabled: Boolean,
        context: Context,
    ) {
        isKeyboardAlwaysDark.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_KEYBOARD_ALWAYS_DARK, enabled)
    }

    /**
     * Executes the set keyboard pitch black operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setKeyboardPitchBlack(
        enabled: Boolean,
        context: Context,
    ) {
        isKeyboardPitchBlack.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_KEYBOARD_PITCH_BLACK, enabled)
    }

    /**
     * Executes the set keyboard clipboard enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setKeyboardClipboardEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isKeyboardClipboardEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_KEYBOARD_CLIPBOARD_ENABLED, enabled)
    }

    /**
     * Executes the set air sync connection enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setAirSyncConnectionEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        if (enabled) {
            // Request permission if not granted, though it's signature level so should be automatic if signed correctly
            // but we can check it
        }
        isAirSyncConnectionEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED, enabled)
    }

    /**
     * Executes the set bluetooth devices enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setBluetoothDevicesEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isBluetoothDevicesEnabled.value = enabled
        settingsRepository.setBluetoothDevicesEnabled(enabled)

        // Trigger widget update to fetch data immediately
        val intent =
            Intent(
                context,
                com.sameerasw.essentials.services.widgets.BatteriesWidgetReceiver::class.java,
            ).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
        context.sendBroadcast(intent)
    }

    /**
     * Executes the set battery widget max devices operation.
     *
     * @param count [Int] Target count.
     * @param context [Context] Target context.
     */
    fun setBatteryWidgetMaxDevices(
        count: Int,
        context: Context,
    ) {
        batteryWidgetMaxDevices.intValue = count
        settingsRepository.setBatteryWidgetMaxDevices(count)

        // Trigger widget update
        val intent =
            Intent(
                context,
                com.sameerasw.essentials.services.widgets.BatteriesWidgetReceiver::class.java,
            ).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
        context.sendBroadcast(intent)
    }

    /**
     * Executes the set battery widget background enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setBatteryWidgetBackgroundEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isBatteryWidgetBackgroundEnabled.value = enabled
        settingsRepository.setBatteryWidgetBackgroundEnabled(enabled)

        // Trigger widget update
        val intent =
            Intent(
                context,
                com.sameerasw.essentials.services.widgets.BatteriesWidgetReceiver::class.java,
            ).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
        context.sendBroadcast(intent)
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean = PermissionUtils.isAccessibilityServiceEnabled(context)

    /**
     * Executes the can write secure settings operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun canWriteSecureSettings(context: Context): Boolean = PermissionUtils.canWriteSecureSettings(context)

    /**
     * Executes the request read phone state permission operation.
     *
     * @param activity [androidx.activity.ComponentActivity] Target activity.
     */
    fun requestReadPhoneStatePermission(activity: androidx.activity.ComponentActivity) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_PHONE_STATE),
            1001,
        )
    }

    /**
     * Executes the request location permission operation.
     *
     * @param activity [androidx.activity.ComponentActivity] Target activity.
     */
    fun requestLocationPermission(activity: androidx.activity.ComponentActivity) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            1003,
        )
    }

    /**
     * Executes the request background location permission operation.
     *
     * @param activity [androidx.activity.ComponentActivity] Target activity.
     */
    fun requestBackgroundLocationPermission(activity: androidx.activity.ComponentActivity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            androidx.core.app.ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                1004,
            )
        }
    }

    /**
     * Executes the request bluetooth permission operation.
     *
     * @param activity [androidx.activity.ComponentActivity] Target activity.
     */
    fun requestBluetoothPermission(activity: androidx.activity.ComponentActivity) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            } else {
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
            },
            1005,
        )
    }

    /**
     * Executes the request calendar permission operation.
     *
     * @param activity [androidx.activity.ComponentActivity] Target activity.
     */
    fun requestCalendarPermission(activity: androidx.activity.ComponentActivity) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
            ),
            1006,
        )
    }

    /**
     * Executes the request notification permission operation.
     *
     * @param activity [androidx.activity.ComponentActivity] Target activity.
     */
    fun requestNotificationPermission(activity: androidx.activity.ComponentActivity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1002,
            )
        }
    }

    /**
     * Executes the request full screen intent permission operation.
     *
     * @param context [Context] Target context.
     */
    fun requestFullScreenIntentPermission(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val intent =
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.fromParts("package", context.packageName, null)
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

    private fun hasNotificationListenerPermission(context: Context): Boolean = PermissionUtils.hasNotificationListenerPermission(context)

    /**
     * Executes the request notification listener permission operation.
     *
     * @param context [Context] Target context.
     */
    fun requestNotificationListenerPermission(context: Context) {
        val intent =
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        context.startActivity(intent)
    }

    /**
     * Executes the request shizuku permission operation.
     */
    fun requestShizukuPermission() {
        ShizukuUtils.requestPermission()
        isShizukuPermissionGranted.value = ShizukuUtils.hasPermission()
    }

    /**
     * Executes the grant write secure settings with shizuku operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun grantWriteSecureSettingsWithShizuku(context: Context): Boolean {
        val success = ShizukuUtils.grantWriteSecureSettingsPermission()
        if (success) {
            // Refresh the write secure settings check
            isWriteSecureSettingsEnabled.value = canWriteSecureSettings(context)
            isShizukuPermissionGranted.value = ShizukuUtils.hasPermission()
        }
        return success
    }

    /**
     * Executes the check caffeinate active operation.
     *
     * @param context [Context] Target context.
     */
    fun checkCaffeinateActive(context: Context) {
        isCaffeinateActive.value = isCaffeinateServiceRunning(context)
    }

    /**
     * Executes the start caffeinate operation.
     *
     * @param context [Context] Target context.
     */
    fun startCaffeinate(context: Context) {
        context.startService(Intent(context, CaffeinateWakeLockService::class.java))
        isCaffeinateActive.value = true
    }

    /**
     * Executes the stop caffeinate operation.
     *
     * @param context [Context] Target context.
     */
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

    private fun canDrawOverlays(context: Context): Boolean = PermissionUtils.canDrawOverlays(context)

    private fun isNotificationLightingAccessibilityServiceEnabled(context: Context): Boolean =
        PermissionUtils.isNotificationLightingAccessibilityServiceEnabled(context)

    private fun isDefaultBrowser(context: Context): Boolean = PermissionUtils.isDefaultBrowser(context)

    // Notification Lighting App Selection Methods
    fun saveNotificationLightingSelectedApps(
        context: Context,
        apps: List<AppSelection>,
    ) {
        settingsRepository.saveNotificationLightingSelectedApps(apps)
    }

    /**
     * Executes the load notification lighting selected apps operation.
     *
     * @param context [Context] Target context.
     * @return The resulting List<AppSelection> data.
     */
    fun loadNotificationLightingSelectedApps(context: Context): List<AppSelection> =
        settingsRepository.loadNotificationLightingSelectedApps()

    fun updateNotificationLightingAppEnabled(
        context: Context,
        packageName: String,
        enabled: Boolean,
    ) {
        settingsRepository.updateNotificationLightingAppSelection(packageName, enabled)
    }

    /**
     * Executes the load flashlight pulse selected apps operation.
     *
     * @param context [Context] Target context.
     * @return The resulting List<AppSelection> data.
     */
    fun loadFlashlightPulseSelectedApps(context: Context): List<AppSelection> = settingsRepository.loadFlashlightPulseSelectedApps()

    /**
     * Executes the save flashlight pulse selected apps operation.
     *
     * @param context [Context] Target context.
     * @param apps [List<AppSelection>] Target apps.
     */
    fun saveFlashlightPulseSelectedApps(
        context: Context,
        apps: List<AppSelection>,
    ) {
        settingsRepository.saveFlashlightPulseSelectedApps(apps)
    }

    /**
     * Executes the update flashlight pulse app enabled operation.
     *
     * @param context [Context] Target context.
     * @param packageName [String] Target package name.
     * @param enabled [Boolean] Target enabled.
     */
    fun updateFlashlightPulseAppEnabled(
        context: Context,
        packageName: String,
        enabled: Boolean,
    ) {
        settingsRepository.updateFlashlightPulseAppSelection(packageName, enabled)
    }

    // Notification Lighting Corner Radius Methods
    fun saveNotificationLightingCornerRadius(
        context: Context,
        radiusDp: Float,
    ) {
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_CORNER_RADIUS, radiusDp)
    }

    /**
     * Executes the load notification lighting corner radius operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Float data.
     */
    fun loadNotificationLightingCornerRadius(context: Context): Float =
        settingsRepository.getFloat(SettingsRepository.KEY_EDGE_LIGHTING_CORNER_RADIUS, 20f)

    // Notification Lighting Stroke Thickness Methods
    fun saveNotificationLightingStrokeThickness(
        context: Context,
        thicknessDp: Float,
    ) {
        settingsRepository.putFloat(
            SettingsRepository.KEY_EDGE_LIGHTING_STROKE_THICKNESS,
            thicknessDp,
        )
    }

    /**
     * Executes the load notification lighting stroke thickness operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Float data.
     */
    fun loadNotificationLightingStrokeThickness(context: Context): Float =
        settingsRepository.getFloat(
            SettingsRepository.KEY_EDGE_LIGHTING_STROKE_THICKNESS,
            8f,
        )

    // Dynamic Night Light App Selection Methods
    fun saveDynamicNightLightSelectedApps(
        context: Context,
        apps: List<AppSelection>,
    ) {
        settingsRepository.saveDynamicNightLightSelectedApps(apps)
    }

    /**
     * Executes the load dynamic night light selected apps operation.
     *
     * @param context [Context] Target context.
     * @return The resulting List<AppSelection> data.
     */
    fun loadDynamicNightLightSelectedApps(context: Context): List<AppSelection> = settingsRepository.loadDynamicNightLightSelectedApps()

    /**
     * Executes the update dynamic night light app enabled operation.
     *
     * @param context [Context] Target context.
     * @param packageName [String] Target package name.
     * @param enabled [Boolean] Target enabled.
     */
    fun updateDynamicNightLightAppEnabled(
        context: Context,
        packageName: String,
        enabled: Boolean,
    ) {
        settingsRepository.updateDynamicNightLightAppSelection(packageName, enabled)
    }

    // App Lock App Selection Methods
    fun saveAppLockSelectedApps(
        context: Context,
        apps: List<AppSelection>,
    ) {
        settingsRepository.saveAppLockSelectedApps(apps)
    }

    /**
     * Executes the load app lock selected apps operation.
     *
     * @param context [Context] Target context.
     * @return The resulting List<AppSelection> data.
     */
    fun loadAppLockSelectedApps(context: Context): List<AppSelection> = settingsRepository.loadAppLockSelectedApps()

    /**
     * Executes the update app lock app enabled operation.
     *
     * @param context [Context] Target context.
     * @param packageName [String] Target package name.
     * @param enabled [Boolean] Target enabled.
     */
    fun updateAppLockAppEnabled(
        context: Context,
        packageName: String,
        enabled: Boolean,
    ) {
        settingsRepository.updateAppLockAppSelection(packageName, enabled)
    }

    // Freeze App Selection Methods
    fun saveFreezeSelectedApps(
        context: Context,
        apps: List<AppSelection>,
    ) {
        settingsRepository.saveFreezeSelectedApps(apps)
        refreshFreezePickedApps(
            context,
            silent = false,
        ) // Full refresh if list structure changes significantly
    }

    /**
     * Executes the load freeze selected apps operation.
     *
     * @param context [Context] Target context.
     * @return The resulting List<AppSelection> data.
     */
    fun loadFreezeSelectedApps(context: Context): List<AppSelection> = settingsRepository.loadFreezeSelectedApps()

    /**
     * Executes the update freeze app enabled operation.
     *
     * @param context [Context] Target context.
     * @param packageName [String] Target package name.
     * @param enabled [Boolean] Target enabled.
     */
    fun updateFreezeAppEnabled(
        context: Context,
        packageName: String,
        enabled: Boolean,
    ) {
        settingsRepository.updateFreezeAppSelection(packageName, enabled)
        refreshFreezePickedApps(context, silent = true)
    }

    fun updateFreezeAppAutoFreeze(
        context: Context,
        packageName: String,
        autoFreezeEnabled: Boolean,
    ) {
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

    private fun syncNeverAutoFreezeApps(context: Context) {
        val neverAutoFreezeTagIds =
            freezeTags.value
                .filter { it.neverAutoFreeze }
                .map { it.id }
                .toSet()
        if (neverAutoFreezeTagIds.isEmpty()) return

        val currentExcluded = freezeAutoExcludedApps.value.toMutableSet()
        var changed = false
        freezeAppTagMap.value.forEach { (pkg, tagIds) ->
            if (tagIds.any { neverAutoFreezeTagIds.contains(it) }) {
                if (currentExcluded.add(pkg)) {
                    changed = true
                }
            }
        }
        if (changed) {
            freezeAutoExcludedApps.value = currentExcluded
            settingsRepository.saveFreezeAutoExcludedApps(currentExcluded)
            refreshFreezePickedApps(context, silent = true)
        }
    }

    /**
     * Executes the add freeze tag operation.
     *
     * @param context [Context] Target context.
     * @param tag [com.sameerasw.essentials.domain.model.AppTag] Target tag.
     */
    fun addFreezeTag(
        context: Context,
        tag: com.sameerasw.essentials.domain.model.AppTag,
    ) {
        val updated = freezeTags.value + tag
        freezeTags.value = updated
        settingsRepository.saveFreezeTags(updated)
        syncNeverAutoFreezeApps(context)
    }

    /**
     * Executes the update freeze tag operation.
     *
     * @param context [Context] Target context.
     * @param tag [com.sameerasw.essentials.domain.model.AppTag] Target tag.
     */
    fun updateFreezeTag(
        context: Context,
        tag: com.sameerasw.essentials.domain.model.AppTag,
    ) {
        val updated = freezeTags.value.map { if (it.id == tag.id) tag else it }
        freezeTags.value = updated
        settingsRepository.saveFreezeTags(updated)
        syncNeverAutoFreezeApps(context)
    }

    /**
     * Executes the delete freeze tag operation.
     *
     * @param context [Context] Target context.
     * @param tagId [String] Target tag id.
     */
    fun deleteFreezeTag(
        context: Context,
        tagId: String,
    ) {
        val updatedTags = freezeTags.value.filter { it.id != tagId }
        freezeTags.value = updatedTags
        settingsRepository.saveFreezeTags(updatedTags)

        val updatedMap =
            freezeAppTagMap.value
                .mapValues { (_, tagIds) ->
                    tagIds.filter { it != tagId }
                }.filterValues { it.isNotEmpty() }
        freezeAppTagMap.value = updatedMap
        settingsRepository.saveFreezeAppTagMap(updatedMap)
    }

    /**
     * Executes the set app tags operation.
     *
     * @param context [Context] Target context.
     * @param packageName [String] Target package name.
     * @param tagIds [List<String>] Target tag ids.
     */
    fun setAppTags(
        context: Context,
        packageName: String,
        tagIds: List<String>,
    ) {
        val currentMap = freezeAppTagMap.value.toMutableMap()
        if (tagIds.isEmpty()) {
            currentMap.remove(packageName)
        } else {
            currentMap[packageName] = tagIds
        }
        freezeAppTagMap.value = currentMap
        settingsRepository.saveFreezeAppTagMap(currentMap)
        syncNeverAutoFreezeApps(context)
    }

    /**
     * Executes the set freeze tag color coded operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setFreezeTagColorCoded(enabled: Boolean) {
        isFreezeTagColorCodedEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_FREEZE_TAG_COLOR_CODED_ENABLED,
            enabled,
        )
    }

    /**
     * Executes the refresh freeze picked apps operation.
     *
     * @param context [Context] Target context.
     * @param silent [Boolean] Target silent.
     */
    fun refreshFreezePickedApps(
        context: Context,
        silent: Boolean = false,
    ) {
        viewModelScope.launch {
            if (!silent) isFreezePickedAppsLoading.value = true
            try {
                // Background processing for heavy list operations
                val result =
                    withContext(Dispatchers.Default) {
                        // Only load apps that are actually marked as secondary selected (picked)
                        val selections = loadFreezeSelectedApps(context).filter { it.isEnabled }
                        if (selections.isEmpty()) return@withContext emptyList()

                        // Efficiently load only the apps that are actually marked as secondary selected (picked)
                        val pickedPkgNames = selections.map { it.packageName }
                        val relevantApps = AppUtil.getAppsByPackageNames(context, pickedPkgNames)

                        val merged = AppUtil.mergeWithSavedApps(relevantApps, selections)
                        val currentExcluded = freezeAutoExcludedApps.value

                        // Cleanup: remove package names that are no longer picked (still on main because it updates state)
                        val filteredExcluded =
                            currentExcluded.filter { pickedPkgNames.contains(it) }.toSet()

                        // Prepare final list in background
                        merged
                            .map { it.copy(isEnabled = !filteredExcluded.contains(it.packageName)) }
                            .sortedBy { it.appName.lowercase() }
                    }

                // Final state update on Main
                freezePickedApps.value = result

                // Exclude check (this part still needs to update state if cleaned up)
                val currentExcluded = freezeAutoExcludedApps.value
                val selections = loadFreezeSelectedApps(context).filter { it.isEnabled }
                val pickedPkgNames = selections.map { it.packageName }
                val filteredExcluded =
                    currentExcluded.filter { pickedPkgNames.contains(it) }.toSet()
                if (filteredExcluded.size != currentExcluded.size) {
                    freezeAutoExcludedApps.value = filteredExcluded
                    settingsRepository.saveFreezeAutoExcludedApps(filteredExcluded)
                }
            } finally {
                if (!silent) isFreezePickedAppsLoading.value = false
            }
        }
    }

    /**
     * Executes the freeze all auto operation.
     *
     * @param context [Context] Target context.
     */
    fun freezeAllAuto(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager
                .freezeAll(context)
        }
    }

    /**
     * Executes the unfreeze all auto operation.
     *
     * @param context [Context] Target context.
     */
    fun unfreezeAllAuto(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager
                .unfreezeAll(context)
        }
    }

    /**
     * Executes the freeze all manual operation.
     *
     * @param context [Context] Target context.
     */
    fun freezeAllManual(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager
                .freezeAllManual(context)
        }
    }

    /**
     * Executes the unfreeze all manual operation.
     *
     * @param context [Context] Target context.
     */
    fun unfreezeAllManual(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager
                .unfreezeAllManual(context)
        }
    }

    /**
     * Executes the launch and unfreeze app operation.
     *
     * @param context [Context] Target context.
     * @param packageName [String] Target package name.
     */
    fun launchAndUnfreezeApp(
        context: Context,
        packageName: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFrozen =
                com.sameerasw.essentials.utils.FreezeManager
                    .isAppFrozen(context, packageName)
            if (isFrozen) {
                com.sameerasw.essentials.utils.FreezeManager
                    .unfreezeApp(context, packageName)
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

    /**
     * Executes the freeze all apps operation.
     *
     * @param context [Context] Target context.
     */
    fun freezeAllApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager
                .freezeAllManual(context)
            refreshFreezePickedApps(context)
        }
    }

    /**
     * Executes the unfreeze all apps operation.
     *
     * @param context [Context] Target context.
     */
    fun unfreezeAllApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager
                .unfreezeAllManual(context)
            refreshFreezePickedApps(context)
        }
    }

    /**
     * Executes the freeze automatic apps operation.
     *
     * @param context [Context] Target context.
     */
    fun freezeAutomaticApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.sameerasw.essentials.utils.FreezeManager
                .freezeAll(context)
            refreshFreezePickedApps(context)
        }
    }

    /**
     * Executes the any apps currently frozen operation.
     *
     * @param context [Context] Target context.
     * @return The resulting Boolean data.
     */
    fun anyAppsCurrentlyFrozen(context: Context): Boolean {
        val picked = freezePickedApps.value
        return picked.any {
            com.sameerasw.essentials.utils.FreezeManager.isAppFrozen(
                context,
                it.packageName,
            )
        }
    }

    /**
     * Executes the set freeze mode operation.
     *
     * @param mode [Int] Target mode.
     * @param context [Context] Target context.
     */
    fun setFreezeMode(
        mode: Int,
        context: Context,
    ) {
        freezeMode.intValue = mode
        settingsRepository.putInt(SettingsRepository.KEY_FREEZE_MODE, mode)
    }

    /**
     * Executes the load snooze channels operation.
     *
     * @param context [Context] Target context.
     */
    fun loadSnoozeChannels(context: Context) {
        val discovered = settingsRepository.loadSnoozeDiscoveredChannels()
        val blocked = settingsRepository.loadSnoozeBlockedChannels()

        val channels =
            discovered.map { channel ->
                channel.copy(isBlocked = blocked.contains(channel.id))
            }

        snoozeChannels.value = channels.distinctBy { it.id }.sortedBy { it.name }
    }

    /**
     * Executes the set snooze channel blocked operation.
     *
     * @param channelId [String] Target channel id.
     * @param blocked [Boolean] Target blocked.
     * @param context [Context] Target context.
     */
    fun setSnoozeChannelBlocked(
        channelId: String,
        blocked: Boolean,
        context: Context,
    ) {
        val currentBlocked = settingsRepository.loadSnoozeBlockedChannels().toMutableSet()
        if (blocked) {
            currentBlocked.add(channelId)
        } else {
            currentBlocked.remove(channelId)
        }
        settingsRepository.saveSnoozeBlockedChannels(currentBlocked)
        loadSnoozeChannels(context)
    }

    private fun loadMapsChannels(context: Context) {
        val discovered = settingsRepository.loadMapsDiscoveredChannels()
        val detectionIds = settingsRepository.loadMapsDetectionChannels()

        mapsChannels.value =
            discovered
                .map { channel ->
                    channel.copy(isEnabled = detectionIds.contains(channel.id))
                }.distinctBy { it.id }
                .sortedBy { it.name }
    }

    /**
     * Executes the set maps channel detected operation.
     *
     * @param channelId [String] Target channel id.
     * @param detected [Boolean] Target detected.
     * @param context [Context] Target context.
     */
    fun setMapsChannelDetected(
        channelId: String,
        detected: Boolean,
        context: Context,
    ) {
        val currentDetected = settingsRepository.loadMapsDetectionChannels().toMutableSet()
        if (detected) {
            currentDetected.add(channelId)
        } else {
            currentDetected.remove(channelId)
        }
        settingsRepository.saveMapsDetectionChannels(currentDetected)
        loadMapsChannels(context)
    }

    /**
     * Executes the set snooze heads up enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setSnoozeHeadsUpEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isSnoozeHeadsUpEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_SNOOZE_HEADS_UP_ENABLED, enabled)
    }

    /**
     * Executes the set flashlight always turn off enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFlashlightAlwaysTurnOffEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isFlashlightAlwaysTurnOffEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_FLASHLIGHT_ALWAYS_TURN_OFF_ENABLED,
            enabled,
        )
    }

    /**
     * Executes the set flashlight pocket turn off enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFlashlightPocketTurnOffEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isFlashlightPocketTurnOffEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_FLASHLIGHT_POCKET_TURN_OFF_ENABLED,
            enabled,
        )
    }

    /**
     * Executes the set flashlight overheat enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFlashlightOverheatEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isFlashlightOverheatEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_FLASHLIGHT_OVERHEAT_PREVENTION_ENABLED,
            enabled,
        )
    }

    /**
     * Executes the set flashlight fade enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFlashlightFadeEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isFlashlightFadeEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FLASHLIGHT_FADE_ENABLED, enabled)
    }

    /**
     * Executes the set flashlight adjust enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFlashlightAdjustEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isFlashlightAdjustEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_FLASHLIGHT_ADJUST_INTENSITY_ENABLED,
            enabled,
        )
    }

    /**
     * Executes the set flashlight global enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFlashlightGlobalEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isFlashlightGlobalEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_FLASHLIGHT_GLOBAL_ENABLED, enabled)
    }

    /**
     * Executes the set flashlight live update enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setFlashlightLiveUpdateEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isFlashlightLiveUpdateEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_FLASHLIGHT_LIVE_UPDATE_ENABLED,
            enabled,
        )
    }

    /**
     * Executes the set flashlight last intensity operation.
     *
     * @param intensity [Int] Target intensity.
     * @param context [Context] Target context.
     */
    fun setFlashlightLastIntensity(
        intensity: Int,
        context: Context,
    ) {
        flashlightLastIntensity.value = intensity
        settingsRepository.putInt(SettingsRepository.KEY_FLASHLIGHT_LAST_INTENSITY, intensity)
    }

    /**
     * Executes the set screen locked security enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setScreenLockedSecurityEnabled(
        enabled: Boolean,
        context: Context,
    ) {
        isScreenLockedSecurityEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_SCREEN_LOCKED_SECURITY_ENABLED,
            enabled,
        )
        if (!enabled) {
            com.sameerasw.essentials.utils.StatusBarManager.requestRestore(
                context,
                "DisableQsWhenLocked",
            )
        }
    }

    /**
     * Executes the set notification lighting glow sides operation.
     *
     * @param sides [Set<NotificationLightingSide>] Target sides.
     * @param context [Context] Target context.
     */
    fun setNotificationLightingGlowSides(
        sides: Set<NotificationLightingSide>,
        context: Context,
    ) {
        notificationLightingGlowSides.value = sides
        settingsRepository.saveNotificationLightingGlowSides(sides)
    }

    /**
     * Executes the save notification lighting indicator x operation.
     *
     * @param context [Context] Target context.
     * @param x [Float] Target x.
     */
    fun saveNotificationLightingIndicatorX(
        context: Context,
        x: Float,
    ) {
        notificationLightingIndicatorX.value = x
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_X, x)
    }

    /**
     * Executes the save notification lighting indicator y operation.
     *
     * @param context [Context] Target context.
     * @param y [Float] Target y.
     */
    fun saveNotificationLightingIndicatorY(
        context: Context,
        y: Float,
    ) {
        notificationLightingIndicatorY.value = y
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_Y, y)
    }

    /**
     * Executes the save notification lighting indicator scale operation.
     *
     * @param context [Context] Target context.
     * @param scale [Float] Target scale.
     */
    fun saveNotificationLightingIndicatorScale(
        context: Context,
        scale: Float,
    ) {
        notificationLightingIndicatorScale.value = scale
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_INDICATOR_SCALE, scale)
    }

    fun setNotificationLightingSweepPosition(
        position: NotificationLightingSweepPosition,
        context: Context,
    ) {
        notificationLightingSweepPosition.value = position
        settingsRepository.saveNotificationLightingSweepPosition(position)
    }

    /**
     * Executes the save notification lighting sweep thickness operation.
     *
     * @param context [Context] Target context.
     * @param thickness [Float] Target thickness.
     */
    fun saveNotificationLightingSweepThickness(
        context: Context,
        thickness: Float,
    ) {
        notificationLightingSweepThickness.floatValue = thickness
        settingsRepository.putFloat(SettingsRepository.KEY_EDGE_LIGHTING_SWEEP_THICKNESS, thickness)
    }

    /**
     * Executes the save notification lighting sweep random shapes operation.
     *
     * @param context [Context] Target context.
     * @param enabled [Boolean] Target enabled.
     */
    fun saveNotificationLightingSweepRandomShapes(
        context: Context,
        enabled: Boolean,
    ) {
        notificationLightingSweepRandomShapes.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_EDGE_LIGHTING_SWEEP_RANDOM_SHAPES,
            enabled,
        )
    }

    /**
     * Executes the save edge lighting sweep selected shapes operation.
     *
     * @param shapes [Set<String>] Target shapes.
     */
    fun saveEdgeLightingSweepSelectedShapes(shapes: Set<String>) {
        edgeLightingSweepSelectedShapes.value = shapes
        settingsRepository.saveEdgeLightingSweepSelectedShapes(shapes)
    }

    /**
     * Executes the export configs operation.
     *
     * @param context [Context] Target context.
     * @param outputStream [java.io.OutputStream] Target output stream.
     */
    fun exportConfigs(
        context: Context,
        outputStream: java.io.OutputStream,
    ) {
        settingsRepository.exportConfigs(outputStream)
    }

    fun importConfigs(
        context: Context,
        inputStream: java.io.InputStream,
        keepPrefs: Boolean,
    ): Boolean {
        val success = settingsRepository.importConfigs(inputStream, keepPrefs)
        if (success) {
            settingsRepository.syncSystemSettingsWithSaved()
            com.sameerasw.essentials.domain.diy.DIYRepository
                .reloadAutomations()
            refreshFreezePickedApps(context, silent = true)
            check(context)
        }
        return success
    }

    data class FreezeBackupData(
        val apps: List<AppSelection>,
        val tags: List<com.sameerasw.essentials.domain.model.AppTag> = emptyList(),
        val appTagMap: Map<String, List<String>> = emptyMap(),
    )

    /**
     * Executes the export freeze apps operation.
     *
     * @param outputStream [java.io.OutputStream] Target output stream.
     */
    fun exportFreezeApps(outputStream: java.io.OutputStream) {
        try {
            val apps = settingsRepository.loadFreezeSelectedApps()
            val tags = settingsRepository.getFreezeTags()
            val appTagMap = settingsRepository.getFreezeAppTagMap()
            val backupData = FreezeBackupData(apps = apps, tags = tags, appTagMap = appTagMap)
            val gson = com.google.gson.Gson()
            val json = gson.toJson(backupData)
            outputStream.write(json.toByteArray())
            outputStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                outputStream.close()
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Executes the import freeze apps operation.
     *
     * @param context [Context] Target context.
     * @param inputStream [java.io.InputStream] Target input stream.
     * @return The resulting Boolean data.
     */
    fun importFreezeApps(
        context: Context,
        inputStream: java.io.InputStream,
    ): Boolean =
        try {
            val json = inputStream.bufferedReader().use { it.readText() }
            val gson = com.google.gson.Gson()

            var importedApps: List<AppSelection> = emptyList()
            var importedTags: List<com.sameerasw.essentials.domain.model.AppTag> = emptyList()
            var importedMap: Map<String, List<String>> = emptyMap()

            // Gracefully handle legacy backup (JSON Array of AppSelection) vs new backup (FreezeBackupData object)
            if (json.trim().startsWith("[")) {
                importedApps = gson.fromJson(json, Array<AppSelection>::class.java).toList()
            } else {
                val backupData = gson.fromJson(json, FreezeBackupData::class.java)
                if (backupData != null) {
                    importedApps = backupData.apps ?: emptyList()
                    importedTags = backupData.tags ?: emptyList()
                    importedMap = backupData.appTagMap ?: emptyMap()
                }
            }

            // Filter out non-installed apps
            val pm = context.packageManager
            val installedApps =
                importedApps.filter { app ->
                    try {
                        pm.getPackageInfo(app.packageName, 0)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

            settingsRepository.saveFreezeSelectedApps(installedApps)

            if (importedTags.isNotEmpty()) {
                val currentTags = settingsRepository.getFreezeTags().toMutableList()
                importedTags.forEach { importedTag ->
                    if (currentTags.none { it.id == importedTag.id }) {
                        currentTags.add(importedTag)
                    }
                }
                freezeTags.value = currentTags
                settingsRepository.saveFreezeTags(currentTags)
            }

            if (importedMap.isNotEmpty()) {
                val installedPkgs = installedApps.map { it.packageName }.toSet()
                val currentMap = settingsRepository.getFreezeAppTagMap().toMutableMap()
                importedMap.forEach { (pkg, tagIds) ->
                    if (installedPkgs.contains(pkg)) {
                        currentMap[pkg] = tagIds
                    }
                }
                freezeAppTagMap.value = currentMap
                settingsRepository.saveFreezeAppTagMap(currentMap)
                syncNeverAutoFreezeApps(context)
            }

            refreshFreezePickedApps(context, silent = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                inputStream.close()
            } catch (e: Exception) {
            }
        }

    /**
     * Executes the set auto accessibility enabled operation.
     *
     * @param isEnabled [Boolean] Target is enabled.
     * @param context [Context] Target context.
     */
    fun setAutoAccessibilityEnabled(
        isEnabled: Boolean,
        context: Context,
    ) {
        settingsRepository.putBoolean(SettingsRepository.KEY_AUTO_ACCESSIBILITY_ENABLED, isEnabled)
        isAutoAccessibilityEnabled.value = isEnabled
    }

    /**
     * Executes the generate bug report operation.
     *
     * @param context [Context] Target context.
     * @return The resulting String data.
     */
    fun generateBugReport(context: Context): String {
        val settingsJson = settingsRepository.getAllConfigsAsJsonString()
        return com.sameerasw.essentials.utils.LogManager
            .generateReport(context, settingsJson)
    }

    /**
     * Executes the set aod enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setAodEnabled(enabled: Boolean) {
        isAodEnabled.value = enabled
        settingsRepository.setAodEnabled(enabled)
    }

    /**
     * Executes the toggle notification glance enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun toggleNotificationGlanceEnabled(enabled: Boolean) {
        settingsRepository.putBoolean(SettingsRepository.KEY_NOTIFICATION_GLANCE_ENABLED, enabled)
        isNotificationGlanceEnabled.value = enabled
    }

    /**
     * Executes the toggle aod force turn off enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun toggleAodForceTurnOffEnabled(enabled: Boolean) {
        settingsRepository.putBoolean(SettingsRepository.KEY_AOD_FORCE_TURN_OFF_ENABLED, enabled)
        isAodForceTurnOffEnabled.value = enabled
    }

    /**
     * Executes the set pocket mode enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setPocketModeEnabled(enabled: Boolean) {
        settingsRepository.putBoolean(SettingsRepository.KEY_POCKET_MODE_ENABLED, enabled)
        isPocketModeEnabled.value = enabled
    }

    /**
     * Executes the set pocket mode use light sensor operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setPocketModeUseLightSensor(enabled: Boolean) {
        settingsRepository.putBoolean(SettingsRepository.KEY_POCKET_MODE_USE_LIGHT_SENSOR, enabled)
        isPocketModeUseLightSensor.value = enabled
    }

    /**
     * Executes the set pocket mode trigger delay operation.
     *
     * @param seconds [Float] Target seconds.
     */
    fun setPocketModeTriggerDelay(seconds: Float) {
        pocketModeTriggerDelay.floatValue = seconds
        settingsRepository.putFloat(SettingsRepository.KEY_POCKET_MODE_TRIGGER_DELAY, seconds)
    }

    /**
     * Executes the set pocket mode lock screen only operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setPocketModeLockScreenOnly(enabled: Boolean) {
        settingsRepository.putBoolean(SettingsRepository.KEY_POCKET_MODE_LOCK_SCREEN_ONLY, enabled)
        isPocketModeLockScreenOnly.value = enabled
    }

    /**
     * Executes the set notification glance same as lighting enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setNotificationGlanceSameAsLightingEnabled(enabled: Boolean) {
        isNotificationGlanceSameAsLightingEnabled.value = enabled
        settingsRepository.putBoolean(
            SettingsRepository.KEY_NOTIFICATION_GLANCE_SAME_AS_LIGHTING,
            enabled,
        )
    }

    /**
     * Executes the load notification glance selected apps operation.
     *
     * @param context [Context] Target context.
     * @return The resulting List<AppSelection> data.
     */
    fun loadNotificationGlanceSelectedApps(context: Context): List<AppSelection> = settingsRepository.loadNotificationGlanceSelectedApps()

    /**
     * Executes the save notification glance selected apps operation.
     *
     * @param context [Context] Target context.
     * @param apps [List<AppSelection>] Target apps.
     */
    fun saveNotificationGlanceSelectedApps(
        context: Context,
        apps: List<AppSelection>,
    ) {
        settingsRepository.saveNotificationGlanceSelectedApps(apps)
    }

    fun updateNotificationGlanceAppEnabled(
        context: Context,
        packageName: String,
        enabled: Boolean,
    ) {
        settingsRepository.updateNotificationGlanceAppSelection(packageName, enabled)
    }

    /**
     * Executes the load pocket mode excluded apps operation.
     *
     * @param context [Context] Target context.
     * @return The resulting List<AppSelection> data.
     */
    fun loadPocketModeExcludedApps(context: Context): List<AppSelection> = settingsRepository.loadPocketModeExcludedApps()

    /**
     * Executes the save pocket mode excluded apps operation.
     *
     * @param context [Context] Target context.
     * @param apps [List<AppSelection>] Target apps.
     */
    fun savePocketModeExcludedApps(
        context: Context,
        apps: List<AppSelection>,
    ) {
        settingsRepository.savePocketModeExcludedApps(apps)
    }

    fun updatePocketModeExcludedAppEnabled(
        context: Context,
        packageName: String,
        enabled: Boolean,
    ) {
        settingsRepository.updatePocketModeExcludedAppSelection(packageName, enabled)
    }

    override fun onCleared() {
        super.onCleared()
        appContext?.let { context ->
            try {
                context.contentResolver.unregisterContentObserver(contentObserver)
            } catch (e: Exception) {
            }
            try {
                powerSaveReceiver?.let { context.unregisterReceiver(it) }
            } catch (e: Exception) {
            }
        }
        if (::settingsRepository.isInitialized) {
            settingsRepository.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
    }

    /**
     * Executes the set onboarding completed operation.
     *
     * @param completed [Boolean] Target completed.
     * @param context [Context] Target context.
     */
    fun setOnboardingCompleted(
        completed: Boolean,
        context: Context,
    ) {
        isOnboardingCompleted.value = completed
        settingsRepository.putBoolean(SettingsRepository.KEY_ONBOARDING_COMPLETED, completed)
        if (completed) {
            settingsRepository.putInt(
                SettingsRepository.KEY_WHATS_NEW_LAST_SHOWN_COUNTER,
                com.sameerasw.essentials.BuildConfig.WHATS_NEW_COUNTER,
            )
        }
    }

    /**
     * Executes the complete whats new operation.
     */
    fun completeWhatsNew() {
        isWhatsNewVisible.value = false
        settingsRepository.putInt(
            SettingsRepository.KEY_WHATS_NEW_LAST_SHOWN_COUNTER,
            com.sameerasw.essentials.BuildConfig.WHATS_NEW_COUNTER,
        )
    }

    /**
     * Executes the reset onboarding operation.
     *
     * @param context [Context] Target context.
     */
    fun resetOnboarding(context: Context) {
        setOnboardingCompleted(false, context)
        // Reset tab to ESSENTIALS
        setDefaultTab(com.sameerasw.essentials.domain.DIYTabs.ESSENTIALS, context)
    }

    /**
     * Executes the reset update note operation.
     *
     * @param context [Context] Target context.
     */
    fun resetUpdateNote(context: Context) {
        settingsRepository.putInt(SettingsRepository.KEY_WHATS_NEW_LAST_SHOWN_COUNTER, 0)
    }

    /**
     * Executes the reset dns presets operation.
     */
    fun resetDnsPresets() {
        settingsRepository.resetPrivateDnsPresets()
    }

    /**
     * Executes the add dns preset operation.
     *
     * @param name [String] Target name.
     * @param hostname [String] Target hostname.
     */
    fun addDnsPreset(
        name: String,
        hostname: String,
    ) {
        val current = settingsRepository.getPrivateDnsPresets().toMutableList()
        current.add(DnsPreset(name = name, hostname = hostname))
        settingsRepository.savePrivateDnsPresets(current)
    }

    /**
     * Executes the remove dns preset operation.
     *
     * @param preset [DnsPreset] Target preset.
     */
    fun removeDnsPreset(preset: DnsPreset) {
        val current = settingsRepository.getPrivateDnsPresets().toMutableList()
        current.removeAll { it.id == preset.id }
        settingsRepository.savePrivateDnsPresets(current)
    }

    private fun updateAddedQSTiles(context: Context) {
        var tilesString = ""
        try {
            tilesString = Settings.Secure.getString(context.contentResolver, "sysui_qs_tiles") ?: ""
        } catch (e: Exception) {
            // sysui_qs_tiles is restricted on Android 14+ (API 34+) for apps targeting API 34+
            e.printStackTrace()
        }

        if (tilesString.isBlank() && ShellUtils.hasPermission(context)) {
            try {
                tilesString = ShellUtils.runCommandWithOutput(context, "settings get secure sysui_qs_tiles") ?: ""
                if (tilesString == "null") tilesString = ""
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val fromSecureSettings =
            tilesString
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()

        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val fromPrefs =
            prefs.all
                .filter { (key, value) -> key.endsWith("_is_added") && value == true }
                .keys
                .map { it.removeSuffix("_is_added") }
                .toSet()

        addedQSTiles.value = fromSecureSettings + fromPrefs
    }

    // Daily Wallpaper Support
    val dailyWallpaperInfo =
        mutableStateOf<com.sameerasw.essentials.domain.model.WallpaperInfo?>(null)
    val isWallpaperLoading = mutableStateOf(false)
    private val wallpaperRepository =
        com.sameerasw.essentials.data.repository
            .WallpaperRepository()

    /**
     * Executes the load cached wallpaper operation.
     */
    fun loadCachedWallpaper() {
        if (!::settingsRepository.isInitialized) return
        val id =
            settingsRepository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_LAST_ID) ?: return
        val url =
            settingsRepository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_LAST_URL) ?: ""
        val urlMobile =
            settingsRepository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_LAST_URL_MOBILE)
                ?: ""
        val urlFull =
            settingsRepository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_LAST_URL) ?: ""
        val authorName =
            settingsRepository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_AUTHOR_NAME) ?: ""
        val authorLink =
            settingsRepository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_AUTHOR_LINK) ?: ""
        val photoLink =
            settingsRepository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_PHOTO_LINK) ?: ""
        val updatedAt =
            settingsRepository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_UPDATED_AT) ?: ""

        dailyWallpaperInfo.value =
            com.sameerasw.essentials.domain.model.WallpaperInfo(
                id = id,
                url = url,
                urlMobile = urlMobile,
                urlFull = urlFull,
                authorName = authorName,
                authorUsername = "",
                authorLink = authorLink,
                photoLink = photoLink,
                updatedAt = updatedAt,
            )
    }

    /**
     * Executes the fetch today wallpaper operation.
     *
     * @param context [Context] Target context.
     */
    fun fetchTodayWallpaper(context: Context) {
        viewModelScope.launch {
            isWallpaperLoading.value = true
            val info = wallpaperRepository.fetchTodayWallpaper()
            if (info != null) {
                dailyWallpaperInfo.value = info
                settingsRepository.putString(
                    SettingsRepository.KEY_DAILY_WALLPAPER_LAST_ID,
                    info.id,
                )
                settingsRepository.putString(
                    SettingsRepository.KEY_DAILY_WALLPAPER_LAST_URL,
                    info.url,
                )
                settingsRepository.putString(
                    SettingsRepository.KEY_DAILY_WALLPAPER_LAST_URL_MOBILE,
                    info.urlMobile,
                )
                settingsRepository.putString(
                    SettingsRepository.KEY_DAILY_WALLPAPER_AUTHOR_NAME,
                    info.authorName,
                )
                settingsRepository.putString(
                    SettingsRepository.KEY_DAILY_WALLPAPER_AUTHOR_LINK,
                    info.authorLink,
                )
                settingsRepository.putString(
                    SettingsRepository.KEY_DAILY_WALLPAPER_PHOTO_LINK,
                    info.photoLink,
                )
                settingsRepository.putString(
                    SettingsRepository.KEY_DAILY_WALLPAPER_UPDATED_AT,
                    info.updatedAt,
                )
            }
            isWallpaperLoading.value = false
        }
    }

    /**
     * Executes the apply wallpaper operation.
     *
     * @param context [Context] Target context.
     * @param url [String] Target url.
     * @param onResult [(Boolean] Target on result.
     */
    fun applyWallpaper(
        context: Context,
        url: String,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            isWallpaperLoading.value = true
            val success = wallpaperRepository.applyWallpaper(context, url)
            isWallpaperLoading.value = false
            onResult(success)
        }
    }

    val isDailyWallpaperAutoUpdateEnabled = mutableStateOf(false)
    val dailyWallpaperAutoUpdateTime = mutableStateOf<String?>(null)
    val isDailyWallpaperShowLastTime = mutableStateOf(false)

    /**
     * Executes the set daily wallpaper show last time operation.
     */
    fun setDailyWallpaperShowLastTime() {
        val status = !isDailyWallpaperShowLastTime.value
        isDailyWallpaperShowLastTime.value = status
        settingsRepository.putBoolean(SettingsRepository.KEY_DAILY_WALLPAPER_SHOW_LAST_TIME, status)
    }

    /**
     * Executes the set daily wallpaper auto update operation.
     *
     * @param enabled [Boolean] Target enabled.
     * @param context [Context] Target context.
     */
    fun setDailyWallpaperAutoUpdate(
        enabled: Boolean,
        context: Context,
    ) {
        isDailyWallpaperAutoUpdateEnabled.value = enabled
        settingsRepository.putBoolean(SettingsRepository.KEY_DAILY_WALLPAPER_AUTO_UPDATE, enabled)
        updateDailyWallpaperAutoUpdateTime(enabled)
        if (enabled) {
            schedulePeriodicWallpaperCheck(context)
            triggerInstantWallpaperUpdate(context)
        } else {
            cancelPeriodicWallpaperCheck(context)
        }
    }

    private fun triggerInstantWallpaperUpdate(context: Context) {
        val data =
            androidx.work.Data
                .Builder()
                .putBoolean("force", true)
                .build()
        val workRequest =
            androidx.work
                .OneTimeWorkRequestBuilder<com.sameerasw.essentials.services.DailyWallpaperWorker>()
                .setInputData(data)
                .build()
        androidx.work.WorkManager
            .getInstance(context)
            .enqueue(workRequest)
    }

    private fun schedulePeriodicWallpaperCheck(context: Context) {
        val workRequest =
            androidx.work
                .PeriodicWorkRequestBuilder<com.sameerasw.essentials.services.DailyWallpaperWorker>(
                    24,
                    java.util.concurrent.TimeUnit.HOURS,
                ).build()

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_wallpaper_check_work",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    private fun schedulePeriodicAppUpdateCheck(context: Context) {
        val constraints =
            androidx.work.Constraints
                .Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

        val workRequest =
            androidx.work
                .PeriodicWorkRequestBuilder<AppUpdateWorker>(
                    12,
                    java.util.concurrent.TimeUnit.HOURS,
                ).setConstraints(constraints)
                .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "app_update_check_work",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest,
        )
    }

    private fun cancelPeriodicWallpaperCheck(context: Context) {
        androidx.work.WorkManager
            .getInstance(context)
            .cancelUniqueWork("daily_wallpaper_check_work")
        androidx.work.WorkManager
            .getInstance(context)
            .cancelUniqueWork("daily_wallpaper_retry_work")
    }

    private fun updateDailyWallpaperAutoUpdateTime(enabled: Boolean) {
        if (enabled) {
            val currentTime = LocalDateTime.now().toString()
            dailyWallpaperAutoUpdateTime.value = currentTime
            settingsRepository.putString(
                SettingsRepository.KEY_DAILY_WALLPAPER_AUTO_UPDATE_TIME,
                currentTime,
            )
            settingsRepository.putInt(SettingsRepository.KEY_DAILY_WALLPAPER_RETRY_COUNT, 0)
        } else {
            dailyWallpaperAutoUpdateTime.value = null
            settingsRepository.remove(SettingsRepository.KEY_DAILY_WALLPAPER_AUTO_UPDATE_TIME)
            settingsRepository.remove(SettingsRepository.KEY_DAILY_WALLPAPER_RETRY_COUNT)
        }
    }

    /**
     * Executes the load battery saver constants operation.
     *
     * @param context [Context] Target context.
     */
    fun loadBatterySaverConstants(context: Context) {
        val constantsStr =
            Settings.Global.getString(context.contentResolver, "battery_saver_constants") ?: ""
        val map = mutableMapOf<String, String>()
        if (constantsStr.isNotEmpty()) {
            constantsStr.split(",").forEach { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    map[parts[0].trim()] = parts[1].trim()
                }
            }
        }
        batterySaverConstants.value = map
    }

    /**
     * Executes the update battery saver constant operation.
     *
     * @param context [Context] Target context.
     * @param key [String] Target key.
     * @param value [String] Target value.
     */
    fun updateBatterySaverConstant(
        context: Context,
        key: String,
        value: String,
    ) {
        val currentMap = batterySaverConstants.value.toMutableMap()
        currentMap[key] = value
        saveBatterySaverConstants(context, currentMap)
    }

    /**
     * Executes the remove battery saver constant operation.
     *
     * @param context [Context] Target context.
     * @param key [String] Target key.
     */
    fun removeBatterySaverConstant(
        context: Context,
        key: String,
    ) {
        val currentMap = batterySaverConstants.value.toMutableMap()
        currentMap.remove(key)
        saveBatterySaverConstants(context, currentMap)
    }

    /**
     * Executes the reset battery saver constants operation.
     *
     * @param context [Context] Target context.
     */
    fun resetBatterySaverConstants(context: Context) {
        try {
            Settings.Global.putString(context.contentResolver, "battery_saver_constants", null)
            batterySaverConstants.value = emptyMap()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveBatterySaverConstants(
        context: Context,
        map: Map<String, String>,
    ) {
        val constantsStr = map.map { "${it.key}=${it.value}" }.joinToString(",")
        try {
            Settings.Global.putString(
                context.contentResolver,
                "battery_saver_constants",
                constantsStr,
            )
            batterySaverConstants.value = map
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the sync audio safe volume state operation.
     *
     * @param context [Context] Target context.
     */
    fun syncAudioSafeVolumeState(context: Context) {
        val rawValue = Settings.Global.getInt(context.contentResolver, "audio_safe_volume_state", 3)
        // 1 = Disable (toggle is on), 3 = Active (toggle is off)
        isAudioSafeVolumeDisabled.value = (rawValue == 1)
    }

    /**
     * Executes the set audio safe volume disabled operation.
     *
     * @param context [Context] Target context.
     * @param disabled [Boolean] Target disabled.
     */
    fun setAudioSafeVolumeDisabled(
        context: Context,
        disabled: Boolean,
    ) {
        val targetValue = if (disabled) 1 else 3
        try {
            Settings.Global.putInt(context.contentResolver, "audio_safe_volume_state", targetValue)
            isAudioSafeVolumeDisabled.value = disabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the sync low power trigger level operation.
     *
     * @param context [Context] Target context.
     */
    fun syncLowPowerTriggerLevel(context: Context) {
        val level = Settings.Global.getInt(context.contentResolver, "low_power_trigger_level", 0)
        lowPowerTriggerLevel.intValue = level
    }

    /**
     * Executes the set low power trigger level operation.
     *
     * @param context [Context] Target context.
     * @param level [Int] Target level.
     */
    fun setLowPowerTriggerLevel(
        context: Context,
        level: Int,
    ) {
        try {
            Settings.Global.putInt(context.contentResolver, "low_power_trigger_level", level)
            lowPowerTriggerLevel.intValue = level
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the sync show notification snooze operation.
     *
     * @param context [Context] Target context.
     */
    fun syncShowNotificationSnooze(context: Context) {
        val enabled =
            Settings.Secure.getInt(context.contentResolver, "show_notification_snooze", 0) == 1
        isShowNotificationSnoozeEnabled.value = enabled
    }

    /**
     * Executes the set show notification snooze enabled operation.
     *
     * @param context [Context] Target context.
     * @param enabled [Boolean] Target enabled.
     */
    fun setShowNotificationSnoozeEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        try {
            Settings.Secure.putInt(
                context.contentResolver,
                "show_notification_snooze",
                if (enabled) 1 else 0,
            )
            isShowNotificationSnoozeEnabled.value = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the load notification snooze options operation.
     *
     * @param context [Context] Target context.
     */
    fun loadNotificationSnoozeOptions(context: Context) {
        val raw =
            Settings.Global.getString(context.contentResolver, "notification_snooze_options") ?: ""
        var def = 60
        var opts = listOf(15, 30, 60, 120)
        if (raw.isNotEmpty()) {
            // Format: default=60,options_array=15:30:60:120
            raw.split(",").forEach { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    if (key == "default") {
                        def = value.toIntOrNull() ?: 60
                    } else if (key == "options_array") {
                        opts = value.split(":").mapNotNull { it.trim().toIntOrNull() }
                    }
                }
            }
        }
        notificationSnoozeDefault.intValue = def
        notificationSnoozeOptions.value = opts
    }

    /**
     * Executes the save notification snooze options operation.
     *
     * @param context [Context] Target context.
     * @param def [Int] Target def.
     * @param opts [List<Int>] Target opts.
     */
    fun saveNotificationSnoozeOptions(
        context: Context,
        def: Int,
        opts: List<Int>,
    ) {
        val serialized = "default=$def,options_array=${opts.joinToString(":")}"
        try {
            Settings.Global.putString(
                context.contentResolver,
                "notification_snooze_options",
                serialized,
            )
            notificationSnoozeDefault.intValue = def
            notificationSnoozeOptions.value = opts
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the reset notification snooze options operation.
     *
     * @param context [Context] Target context.
     */
    fun resetNotificationSnoozeOptions(context: Context) {
        try {
            Settings.Global.putString(context.contentResolver, "notification_snooze_options", null)
            notificationSnoozeDefault.intValue = 60
            notificationSnoozeOptions.value = listOf(15, 30, 60, 120)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        var cachedIsUpdateAvailable: Boolean = false
        var cachedUpdateInfo: UpdateInfo? = null
    }

    val isLockdownModeEnabled = mutableStateOf(false)

    /**
     * Executes the toggle lockdown mode operation.
     */
    fun toggleLockdownMode() {
        settingsRepository.putBoolean(
            SettingsRepository.KEY_LOCKDOWN_MODE,
            !isLockdownModeEnabled.value,
        )
        isLockdownModeEnabled.value = !isLockdownModeEnabled.value
    }
}
