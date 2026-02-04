package com.sameerasw.essentials

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.domain.HapticFeedbackType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.domain.registry.PermissionRegistry
import com.sameerasw.essentials.ui.components.linkActions.LinkPickerScreen
import com.sameerasw.essentials.ui.composables.configs.StatusBarIconSettingsUI
import com.sameerasw.essentials.ui.composables.configs.CaffeinateSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ScreenOffWidgetSettingsUI
import com.sameerasw.essentials.ui.composables.configs.NotificationLightingSettingsUI
import com.sameerasw.essentials.ui.composables.configs.SoundModeTileSettingsUI
import com.sameerasw.essentials.ui.composables.configs.QuickSettingsTilesSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ButtonRemapSettingsUI
import com.sameerasw.essentials.ui.composables.configs.DynamicNightLightSettingsUI
import com.sameerasw.essentials.ui.composables.configs.SnoozeNotificationsSettingsUI
import com.sameerasw.essentials.ui.composables.configs.AmbientMusicGlanceSettingsUI
import com.sameerasw.essentials.ui.composables.configs.LocationReachedSettingsUI
import com.sameerasw.essentials.ui.composables.configs.BatteriesSettingsUI
import com.sameerasw.essentials.ui.composables.configs.MapsPowerSavingSettingsUI
import com.sameerasw.essentials.viewmodels.CaffeinateViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.viewmodels.StatusBarIconViewModel
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet

import com.sameerasw.essentials.ui.composables.configs.AppLockSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ScreenLockedSecuritySettingsUI
import com.sameerasw.essentials.ui.composables.configs.KeyboardSettingsUI
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.BiometricHelper

@OptIn(ExperimentalMaterial3Api::class)
class FeatureSettingsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val featureId = intent.getStringExtra("feature") ?: ""
        val featureObj = FeatureRegistry.ALL_FEATURES.find { it.id == featureId }
        val highlightSetting = intent.getStringExtra("highlight_setting")

        if (featureId == "Link actions") {
            setContent {
                val viewModel: MainViewModel = viewModel()
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    viewModel.check(context)
                }
                val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
                EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                    LinkPickerScreen(
                        uri = "https://sameerasw.com".toUri(),
                        onFinish = { finish() },
                        modifier = Modifier.fillMaxSize(),
                        demo = true
                    )
                }
            }
            return
        }

        setContent {
            val context = LocalContext.current
            val viewModel: MainViewModel = viewModel()
            val statusBarViewModel: StatusBarIconViewModel = viewModel()
            val caffeinateViewModel: CaffeinateViewModel = viewModel()

            // Automatic refresh on resume
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.check(context)
                        if (featureId == "Statusbar icons") {
                            statusBarViewModel.check(context)
                        }
                        if (featureId == "Caffeinate") {
                            caffeinateViewModel.check(context)
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.check(context)
            }

            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            val pinnedFeatureKeys by viewModel.pinnedFeatureKeys

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val view = LocalView.current
                val prefs = context.getSharedPreferences("essentials_prefs", MODE_PRIVATE)

                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
                }

                var selectedHaptic by remember {
                    val name = prefs.getString("haptic_feedback_type", HapticFeedbackType.NONE.name)
                    mutableStateOf(
                        try {
                            HapticFeedbackType.valueOf(name ?: HapticFeedbackType.NONE.name)
                        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
                            HapticFeedbackType.NONE
                        }
                    )
                }

                // Permission sheet state
                var showPermissionSheet by remember { mutableStateOf(false) }
                var childFeatureForPermissions by remember { mutableStateOf<String?>(null) }

                val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
                val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
                val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted
                val isNotificationLightingAccessibilityEnabled by viewModel.isNotificationLightingAccessibilityEnabled
                val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled
                val isReadPhoneStateEnabled by viewModel.isReadPhoneStateEnabled

                // FAB State for Notification Lighting
                var fabExpanded by remember { mutableStateOf(true) }
                LaunchedEffect(featureId) {
                    if (featureId == "Notification lighting") {
                        fabExpanded = true
                        delay(3000)
                        fabExpanded = false
                    }
                }

                // Show permission sheet if feature has missing permissions
                LaunchedEffect(
                    featureId,
                    isAccessibilityEnabled,
                    isWriteSecureSettingsEnabled,
                    isOverlayPermissionGranted,
                    isNotificationLightingAccessibilityEnabled,
                    isNotificationListenerEnabled,
                    isReadPhoneStateEnabled
                ) {
                    val hasMissingPermissions = when (featureId) {
                        "Screen off widget" -> !isAccessibilityEnabled
                        "Statusbar icons" -> !isWriteSecureSettingsEnabled
                        "Notification lighting" -> !isOverlayPermissionGranted || !isNotificationLightingAccessibilityEnabled || !isNotificationListenerEnabled
                        "Button remap" -> !isAccessibilityEnabled
                        "Dynamic night light" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled
                        "Snooze system notifications" -> !isNotificationListenerEnabled
                        "Screen locked security" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled || !viewModel.isDeviceAdminEnabled.value
                        "App lock" -> !isAccessibilityEnabled
                        "Freeze" -> !com.sameerasw.essentials.utils.ShellUtils.hasPermission(context)
                        "Location reached" -> !viewModel.isLocationPermissionGranted.value || !viewModel.isBackgroundLocationPermissionGranted.value
                        "Quick settings tiles" -> !viewModel.isWriteSettingsEnabled.value
                        // Top level checks for other features (rarely hit if they are children, but safe to add)
                        "Ambient music glance" -> !isAccessibilityEnabled || !isNotificationListenerEnabled
                        "Call vibrations" -> !isReadPhoneStateEnabled || !isNotificationListenerEnabled
                        "Maps power saving mode" -> !isNotificationListenerEnabled || !com.sameerasw.essentials.utils.ShellUtils.hasPermission(context)
                        "Caffeinate" -> !viewModel.isPostNotificationsEnabled.value
                        else -> false
                    }
                    if (hasMissingPermissions) {
                        showPermissionSheet = true
                    }
                }


                if (showPermissionSheet) {
                    val featureIdForPermissions = childFeatureForPermissions ?: featureId
                    val featureObjForPermissions = com.sameerasw.essentials.domain.registry.FeatureRegistry.ALL_FEATURES.find { it.id == featureIdForPermissions }
                    
                    val permissionItems = if (featureObjForPermissions != null) {
                        com.sameerasw.essentials.utils.PermissionUIHelper.getPermissionItems(
                            featureObjForPermissions.permissionKeys,
                            context,
                            viewModel,
                            this@FeatureSettingsActivity
                        )
                    } else {
                        emptyList()
                    }

                    if (permissionItems.isNotEmpty()) {
                        PermissionsBottomSheet(
                            onDismissRequest = {
                                showPermissionSheet = false
                                childFeatureForPermissions = null
                            },
                            featureTitle = if (featureObjForPermissions != null && childFeatureForPermissions == null) stringResource(featureObjForPermissions.title) else featureIdForPermissions,
                            permissions = permissionItems
                        )
                    }
                }

                val scrollBehavior =
                    TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(
                        0,
                        0,
                        0,
                        0
                    ),
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = if (featureObj != null) stringResource(featureObj.title) else featureId,
                            hasBack = true,
                            hasSearch = false,
                            onBackClick = { finish() },
                            scrollBehavior = scrollBehavior,
                            subtitle = if (featureObj != null) stringResource(featureObj.description) else "",
                            isBeta = featureObj?.isBeta ?: false
                        )
                    },
                    floatingActionButton = {
                        if (featureId == "Notification lighting") {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.triggerNotificationLighting(context)
                                },
                                expanded = fabExpanded,
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_play_arrow_24),
                                        contentDescription = null
                                    )
                                },
                                text = { Text(stringResource(R.string.action_preview)) },
                                modifier = Modifier.height(64.dp)
                            )
                        }
                    }
                ) { innerPadding ->
                    val hasScroll = featureId != "Sound mode tile"
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .then(if (hasScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    ) {
                        val children =
                            FeatureRegistry.ALL_FEATURES.filter { it.parentFeatureId == featureId }
                        if (children.isNotEmpty()) {
                            RoundedCardContainer(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp)
                            ) {
                                children.forEachIndexed { index, child ->
                                    val permissionAwareToggle: (Boolean) -> Unit = { enabled ->
                                        val missingPermission = when (child.id) {
                                            "Screen off widget" -> !isAccessibilityEnabled
                                            "Statusbar icons" -> !isWriteSecureSettingsEnabled
                                            "Notification lighting" -> !isOverlayPermissionGranted || !isNotificationLightingAccessibilityEnabled || !isNotificationListenerEnabled
                                            "Button remap" -> !isAccessibilityEnabled
                                            "Dynamic night light" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled
                                            "Snooze system notifications" -> !isNotificationListenerEnabled
                                            "Screen locked security" -> !isAccessibilityEnabled || !isWriteSecureSettingsEnabled || !viewModel.isDeviceAdminEnabled.value
                                            "App lock" -> !isAccessibilityEnabled
                                            "Freeze" -> !com.sameerasw.essentials.utils.ShellUtils.hasPermission(context)
                                            "Ambient music glance" -> !isAccessibilityEnabled || !isNotificationListenerEnabled
                                            "Call vibrations" -> !isReadPhoneStateEnabled || !isNotificationListenerEnabled
                                            "Calendar Sync" -> androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                            "Batteries" -> (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                                            "Maps power saving mode" -> !isNotificationListenerEnabled || !com.sameerasw.essentials.utils.ShellUtils.hasPermission(context)
                                            "Caffeinate" -> !viewModel.isPostNotificationsEnabled.value
                                            else -> false
                                        }

                                        if (missingPermission) {
                                            childFeatureForPermissions = child.id
                                            showPermissionSheet = true
                                        } else {
                                            if (child.category == R.string.cat_security) {
                                                BiometricHelper.showBiometricPrompt(
                                                    activity = this@FeatureSettingsActivity,
                                                    title = getString(
                                                        R.string.biometric_title_settings_format,
                                                        getString(child.title)
                                                    ),
                                                    subtitle = if (enabled) getString(R.string.biometric_subtitle_enable_feature) else getString(
                                                        R.string.biometric_subtitle_disable_feature
                                                    ),
                                                    onSuccess = {
                                                        child.onToggle(
                                                            viewModel,
                                                            context,
                                                            enabled
                                                        )
                                                    }
                                                )
                                            } else {
                                                child.onToggle(viewModel, context, enabled)
                                            }
                                        }
                                    }

                                    FeatureCard(
                                        title = child.title,
                                        description = child.description,
                                        iconRes = child.iconRes,
                                        isEnabled = child.isEnabled(viewModel),
                                        isToggleEnabled = child.isToggleEnabled(viewModel, context),
                                        showToggle = child.showToggle,
                                        onDisabledToggleClick = { permissionAwareToggle(true) },
                                        hasMoreSettings = child.hasMoreSettings,
                                        isBeta = child.isBeta,
                                        onToggle = permissionAwareToggle,
                                        onClick = {
                                            if (child.category == R.string.cat_security) {
                                                BiometricHelper.showBiometricPrompt(
                                                    activity = this@FeatureSettingsActivity,
                                                    title = getString(
                                                        R.string.biometric_title_settings_format,
                                                        getString(child.title)
                                                    ),
                                                    subtitle = getString(R.string.biometric_subtitle_access_settings),
                                                    onSuccess = {
                                                        child.onClick(
                                                            context,
                                                            viewModel
                                                        )
                                                    }
                                                )
                                            } else {
                                                child.onClick(context, viewModel)
                                            }
                                        },
                                        isPinned = pinnedFeatureKeys.contains(child.id),
                                        onPinToggle = { viewModel.togglePinFeature(child.id) }
                                    )
                                }
                            }
                        } else {
                            when (featureId) {
                                "Screen off widget" -> {
                                    ScreenOffWidgetSettingsUI(
                                        viewModel = viewModel,
                                        selectedHaptic = selectedHaptic,
                                        onHapticSelected = { type -> selectedHaptic = type },
                                        vibrator = vibrator,
                                        prefs = prefs,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Statusbar icons" -> {
                                    StatusBarIconSettingsUI(
                                        viewModel = statusBarViewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Caffeinate" -> {
                                    CaffeinateSettingsUI(
                                        viewModel = caffeinateViewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Notification lighting" -> {
                                    NotificationLightingSettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Sound mode tile" -> {
                                    SoundModeTileSettingsUI(
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Button remap" -> {
                                    ButtonRemapSettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Dynamic night light" -> {
                                    DynamicNightLightSettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Snooze system notifications" -> {
                                    SnoozeNotificationsSettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Screen locked security" -> {
                                    ScreenLockedSecuritySettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "App lock" -> {
                                    AppLockSettingsUI(
                                        viewModel = viewModel,
                                        highlightKey = highlightSetting
                                    )
                                }

                                "Freeze" -> {
                                    com.sameerasw.essentials.ui.composables.configs.FreezeSettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightKey = highlightSetting
                                    )
                                }

                                "Quick settings tiles" -> {
                                    QuickSettingsTilesSettingsUI(
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Location reached" -> {
                                    LocationReachedSettingsUI(
                                        mainViewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "System Keyboard" -> {
                                    KeyboardSettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Batteries" -> {
                                    BatteriesSettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }

                                "Ambient music glance" -> {
                                    AmbientMusicGlanceSettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Calendar Sync" -> {
                                    com.sameerasw.essentials.ui.composables.configs.CalendarSyncSettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightKey = highlightSetting
                                    )
                                }

                                "Maps power saving mode" -> {
                                    MapsPowerSavingSettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }

                                "Flashlight pulse" -> {
                                    com.sameerasw.essentials.ui.composables.configs.FlashlightPulseSettingsUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(top = 16.dp),
                                        highlightSetting = highlightSetting
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}