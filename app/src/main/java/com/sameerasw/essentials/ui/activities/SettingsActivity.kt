/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: SettingsActivity.kt
 * Description: Activity component for SettingsActivity.kt.
 */

package com.sameerasw.essentials

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import com.sameerasw.essentials.domain.DIYTabs
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.domain.registry.PermissionRegistry
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.translation.ui.TranslationSessionSheet
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.components.MadebySameeraswCard
import com.sameerasw.essentials.ui.components.dialogs.AboutSection
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.cards.PermissionCard
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.AppIconPicker
import com.sameerasw.essentials.ui.core.pickers.CrashReportingPicker
import com.sameerasw.essentials.ui.core.pickers.DefaultTabPicker
import com.sameerasw.essentials.ui.core.pickers.LanguagePicker
import com.sameerasw.essentials.ui.core.sheets.GitHubAuthSheet
import com.sameerasw.essentials.ui.core.sheets.ImportConfigConfirmationSheet
import com.sameerasw.essentials.ui.core.sheets.InstructionsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.PreReleaseConfirmationSheet
import com.sameerasw.essentials.ui.core.sheets.UnsupportedFeaturesConfirmationSheet
import com.sameerasw.essentials.ui.core.sheets.UpdateBottomSheet
import androidx.compose.ui.geometry.Offset
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.liquidRipple
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.modifiers.scrollMotionBlur
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.GitHubAuthViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.ui.core.sheets.CrashLogsBottomSheet
import rikka.shizuku.Shizuku
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val shizukuPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                viewModel.check(this)
            }
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isDarkMode =
            (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawableResource(if (isDarkMode) android.R.color.black else R.color.app_window_background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        // Register Shizuku permission listener
        Shizuku.addRequestPermissionResultListener(shizukuPermissionResultListener)
        setContent {
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val context = LocalContext.current
                LocalView.current

                var showBugReportSheet by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.check(context)
                }

                if (showBugReportSheet) {
                    com.sameerasw.essentials.ui.core.sheets.BugReportBottomSheet(
                        viewModel = viewModel,
                        onDismissRequest = { showBugReportSheet = false },
                    )
                }

                val statusBarHeightPx =
                    with(LocalDensity.current) {
                        WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding()
                            .toPx()
                    }
                val statusBarHeight =
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

                val isBlurEnabled by viewModel.isBlurEnabled
                val isRippleEnabled by viewModel.isRippleEnabled
                var iconRippleTrigger by remember { mutableStateOf(0) }
                var iconRippleOrigin by remember { mutableStateOf(Offset.Zero) }

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .liquidRipple(
                                trigger = iconRippleTrigger,
                                origin = iconRippleOrigin,
                                enabled = isRippleEnabled,
                                durationMillis = 2800,
                                amplitudeDp = 34f,
                                frequency = 12f,
                                decay = 4.5f,
                                speedDp = 1400f,
                            )
                            .progressiveBlur(
                                blurRadius = if (isBlurEnabled) 40f else 0f,
                                height = statusBarHeightPx * 1.15f,
                                direction = BlurDirection.TOP,
                            ),
                ) {
                    val contentPadding =
                        PaddingValues(
                            top = statusBarHeight,
                            bottom = 150.dp,
                            start = 16.dp,
                            end = 16.dp,
                        )

                    val expandPermissions = intent.getBooleanExtra("expand_permissions", false)

                    SettingsContent(
                        viewModel = viewModel,
                        contentPadding = contentPadding,
                        expandPermissionsInitial = expandPermissions,
                        onAppIconSelectedWithPosition = { _, pos ->
                            iconRippleOrigin = pos
                            iconRippleTrigger++
                        },
                        onAvatarLongClickWithPosition = { pos ->
                            iconRippleOrigin = pos
                            iconRippleTrigger++
                        },
                        onRippleToggleEnabledWithPosition = { pos ->
                            iconRippleOrigin = pos
                            iconRippleTrigger++
                        },
                        modifier =
                            Modifier
                                .progressiveBlur(
                                    blurRadius = if (isBlurEnabled) 40f else 0f,
                                    height = with(LocalDensity.current) { 150.dp.toPx() },
                                    direction = BlurDirection.BOTTOM,
                                ),
                    )

                    EssentialsFloatingToolbar(
                        title = stringResource(R.string.label_settings),
                        onBackClick = { finish() },
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(1f),
                        fabAction = { showBugReportSheet = true },
                        fabIconRes = R.drawable.rounded_bug_report_24,
                        fabContentDescription = stringResource(R.string.action_report_bug),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.check(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionResultListener)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode in 1001..1006) {
            viewModel.check(this)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    expandPermissionsInitial: Boolean = false,
    onAppIconSelectedWithPosition: ((com.sameerasw.essentials.domain.model.AppIcon, Offset) -> Unit)? = null,
    onAvatarLongClickWithPosition: ((Offset) -> Unit)? = null,
    onRippleToggleEnabledWithPosition: ((Offset) -> Unit)? = null,
) {
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
    val isPostNotificationsEnabled by viewModel.isPostNotificationsEnabled
    val isReadPhoneStateEnabled by viewModel.isReadPhoneStateEnabled
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
    val isShizukuAvailable by viewModel.isShizukuAvailable
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted
    val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled
    val isDefaultBrowserSet by viewModel.isDefaultBrowserSet
    val isWriteSettingsEnabled by viewModel.isWriteSettingsEnabled
    val isNotificationPolicyAccessGranted by viewModel.isNotificationPolicyAccessGranted
    val isLocationPermissionGranted by viewModel.isLocationPermissionGranted
    val isBackgroundLocationPermissionGranted by viewModel.isBackgroundLocationPermissionGranted
    val isDeviceAdminEnabled by viewModel.isDeviceAdminEnabled
    val isCalendarPermissionGranted by viewModel.isCalendarPermissionGranted
    val isUsageStatsPermissionGranted by viewModel.isUsageStatsPermissionGranted
    val context = LocalContext.current
    val isAppHapticsEnabled = remember { mutableStateOf(HapticUtil.loadAppHapticsEnabled(context)) }
    var isPermissionsExpanded by remember { mutableStateOf(expandPermissionsInitial) }
    var showUpdateSheet by remember { mutableStateOf(false) }
    val updateInfo by viewModel.updateInfo
    val isUpdateAvailable by viewModel.isUpdateAvailable
    val isAutoUpdateEnabled by viewModel.isAutoUpdateEnabled
    val isGenAIAutomationEnabled by viewModel.isGenAIAutomationEnabled
    var isGenAISupported by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isGenAISupported =
            com.sameerasw.essentials.domain.genai.GenAIAutomationService
                .isSupported()
    }

    val isUpdateNotificationEnabled by viewModel.isUpdateNotificationEnabled
    val isPreReleaseCheckEnabled by viewModel.isPreReleaseCheckEnabled
    val isRootEnabled by viewModel.isRootEnabled
    val isRootPermissionGranted by viewModel.isRootPermissionGranted
    val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled
    var showInstructionsSheet by remember { mutableStateOf(false) }
    var showCrashLogsSheet by remember { mutableStateOf(false) }
    var showShizukuHelpBottomSheet by remember { mutableStateOf(false) }
    var showUnsupportedFeaturesSheet by remember { mutableStateOf(false) }
    var showPreReleaseConfirmSheet by remember { mutableStateOf(false) }
    var pendingPreReleaseState by remember { mutableStateOf(false) }
    var showImportConfirmSheet by remember { mutableStateOf(false) }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }

    var showTranslationSessionSheet by remember { mutableStateOf(false) }
    var showLanguagePickerSheet by remember { mutableStateOf(false) }
    var showGitHubAuthSheet by remember { mutableStateOf(false) }
    var showTranslationWarningDialog by remember { mutableStateOf(false) }
    val gitHubAuthViewModel: GitHubAuthViewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel()
    val settingsRepo =
        remember {
            com.sameerasw.essentials.data.repository
                .SettingsRepository(context)
        }
    var currentUser by remember { mutableStateOf(settingsRepo.getGitHubUser()) }

    val isTranslationModeActive by TranslationManager.isTranslationModeEnabled
    val sessionEditsCount = TranslationManager.session.edits.size

    var openTranslationPRs by remember {
        mutableStateOf<List<com.sameerasw.essentials.domain.model.github.GitHubPullRequest>>(
            emptyList(),
        )
    }
    val gitHubRepo =
        remember {
            com.sameerasw.essentials.data.repository
                .GitHubRepository()
        }

    androidx.compose.runtime.LaunchedEffect(isTranslationModeActive, currentUser) {
        val user = currentUser
        if (isTranslationModeActive && user != null) {
            val userToken = settingsRepo.getGitHubToken()
            val prs =
                gitHubRepo.getOpenTranslationPRs(
                    owner = "sameerasw",
                    repo = "essentials",
                    author = user.login,
                    token = userToken,
                )
            openTranslationPRs = prs
        } else {
            openTranslationPRs = emptyList()
        }
    }

    val onImportConfig: (Boolean) -> Unit = { keepPrefs ->
        selectedImportUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    if (viewModel.importConfigs(context, inputStream, keepPrefs)) {
                        Toast
                            .makeText(context, "Config imported successfully", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast
                            .makeText(context, "Failed to import config", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import config", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                selectedImportUri = null
                showImportConfirmSheet = false
            }
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            uri?.let {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        viewModel.exportConfigs(context, outputStream)
                        Toast
                            .makeText(context, "Config exported successfully", Toast.LENGTH_SHORT)
                            .show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to export config", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let {
                selectedImportUri = it
                showImportConfirmSheet = true
            }
        }

    if (showUpdateSheet) {
        UpdateBottomSheet(
            updateInfo = updateInfo,
            isChecking = viewModel.isCheckingUpdate.value,
            onDismissRequest = { showUpdateSheet = false },
        )
    }

    if (showInstructionsSheet) {
        InstructionsBottomSheet(
            onDismissRequest = { showInstructionsSheet = false },
        )
    }

    if (showCrashLogsSheet) {
        CrashLogsBottomSheet(
            onDismissRequest = { showCrashLogsSheet = false },
        )
    }

    if (showUnsupportedFeaturesSheet) {
        UnsupportedFeaturesConfirmationSheet(
            onDismissRequest = { showUnsupportedFeaturesSheet = false },
            onConfirm = {
                showUnsupportedFeaturesSheet = false
                viewModel.setEnableUnsupportedFeatures(true, context)
            },
            featureTitleResIds = FeatureRegistry.getUnsupportedFeatures(context).map { it.title },
        )
    }

    if (showPreReleaseConfirmSheet) {
        PreReleaseConfirmationSheet(
            isEnabling = pendingPreReleaseState,
            onDismissRequest = { showPreReleaseConfirmSheet = false },
            onConfirmRestart = {
                showPreReleaseConfirmSheet = false
                viewModel.setPreReleaseCheckEnabled(pendingPreReleaseState, context)
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                if (intent != null) {
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                }
            },
        )
    }

    if (showImportConfirmSheet) {
        ImportConfigConfirmationSheet(
            onDismissRequest = {
                showImportConfirmSheet = false
                selectedImportUri = null
            },
            onConfirmOverride = {
                onImportConfig(false)
            },
            onConfirmMerge = {
                onImportConfig(true)
            },
        )
    }

    if (showShizukuHelpBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showShizukuHelpBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Shizuku Auth Token",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "In shizuku, in the 'Control Shizuku with automation apps' section, Open 'View intents' and copy and paste the auth token from 'Extras' section.\n\nThis allows Essentials to automate and re-start Shizuku on demand in features such as Shut-Up",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { showShizukuHelpBottomSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Got it")
                }
            }
        }
    }

    val sentryMode by viewModel.sentryReportMode
    val isMotionBlurEnabled by viewModel.isMotionBlurEnabled
    val scrollState = rememberScrollState()
    var permissionsSectionY by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(expandPermissionsInitial, permissionsSectionY) {
        if (expandPermissionsInitial && permissionsSectionY != null) {
            scrollState.animateScrollTo(permissionsSectionY!!.toInt())
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .scrollMotionBlur(scrollState, enabled = isMotionBlurEnabled)
                .verticalScroll(scrollState)
                .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        val view = LocalView.current

        // Help & Guides
        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_help_24,
                title = stringResource(R.string.label_help_guide),
                isChecked = false,
                onCheckedChange = {
                    showInstructionsSheet = true
                },
                showToggle = false,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp),
            )
        }

        // Updates 
        Text(
            text = "Updates",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_check_24,
                title = "Auto check for updates",
                description = "Check for updates at app launch",
                isChecked = isAutoUpdateEnabled,
                onCheckedChange = { viewModel.setAutoUpdateEnabled(it, context) },
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_experiment_24,
                title = context.getString(R.string.check_pre_releases_label),
                description = context.getString(R.string.check_pre_releases_desc),
                isChecked = isPreReleaseCheckEnabled,
                onCheckedChange = { targetState ->
                    pendingPreReleaseState = targetState
                    showPreReleaseConfirmSheet = true
                },
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = "Notify for new updates",
                isChecked = isUpdateNotificationEnabled,
                onCheckedChange = { viewModel.setUpdateNotificationEnabled(it, context) },
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceBright,
                            shape = MaterialTheme.shapes.extraSmall,
                        ).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val buttonText =
                    if (isUpdateAvailable && !updateInfo?.versionName.isNullOrEmpty()) {
                        stringResource(
                            R.string.action_update_to_version,
                            updateInfo?.versionName ?: "",
                        )
                    } else {
                        stringResource(R.string.action_check_for_updates)
                    }

                val buttonIconRes = R.drawable.rounded_mobile_arrow_down_24

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.checkForUpdates(context, manual = true)
                        showUpdateSheet = true
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 4.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                ) {
                    Icon(
                        painter = painterResource(id = buttonIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Customizations 
        Text(
            text = stringResource(R.string.settings_section_customizations),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer {
            val selectedAppIcon by viewModel.selectedAppIcon
            AppIconPicker(
                selectedIcon = selectedAppIcon,
                onIconSelected = { viewModel.setAppIcon(it, context) },
                onIconSelectedWithPosition = onAppIconSelectedWithPosition,
            )

            val appLanguage by viewModel.appLanguage
            LanguagePicker(
                selectedLanguageCode = appLanguage,
                onLanguageSelected = { viewModel.setAppLanguage(it) },
            )

            val defaultTab by viewModel.defaultTab
            val availableTabs = remember { DIYTabs.entries }
            DefaultTabPicker(
                selectedTab = defaultTab,
                onTabSelected = { viewModel.setDefaultTab(it, context) },
                options = availableTabs,
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_touch_app_24,
                title = stringResource(R.string.setting_swipe_tabs_title),
                isChecked = viewModel.isSwipeTabsEnabled.value,
                onCheckedChange = { viewModel.setSwipeTabsEnabled(it) },
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_vibrate_24,
                title = "Haptic Feedback",
                isChecked = isAppHapticsEnabled.value,
                onCheckedChange = { isChecked ->
                    isAppHapticsEnabled.value = isChecked
                    HapticUtil.saveAppHapticsEnabled(context, isChecked)
                },
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_invert_colors_24,
                title = stringResource(R.string.setting_pitch_black_theme_title),
                isChecked = viewModel.isPitchBlackThemeEnabled.value,
                onCheckedChange = { viewModel.setPitchBlackThemeEnabled(it, context) },
            )
            val isBlurProblematic = remember { DeviceUtils.isBlurProblematicDevice() }

            IconToggleItem(
                iconRes = R.drawable.rounded_blur_on_24,
                title = stringResource(R.string.label_use_blur),
                description =
                    if (isBlurProblematic) {
                        stringResource(R.string.msg_blur_compatibility_error)
                    } else {
                        null
                    },
                isChecked = viewModel.isBlurSettingEnabled.value,
                onCheckedChange = { viewModel.setBlurEnabled(it, context) },
                enabled = !isBlurProblematic,
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_blur_linear_24,
                title = stringResource(R.string.label_ripple_animation),
                isChecked = viewModel.isRippleSettingEnabled.value,
                onCheckedChange = { viewModel.setRippleEnabled(it, context) },
                onCheckedChangeWithPosition = { isChecked, pos ->
                    if (isChecked) {
                        onRippleToggleEnabledWithPosition?.invoke(pos)
                    }
                },
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_settings_motion_mode_24,
                title = stringResource(R.string.label_motion_blur),
                isChecked = viewModel.isMotionBlurSettingEnabled.value,
                onCheckedChange = { viewModel.setMotionBlurEnabled(it, context) },
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_music_video_24,
                title = stringResource(R.string.label_online_help_media),
                isChecked = viewModel.isOnlineHelpMediaEnabled.value,
                onCheckedChange = { viewModel.setOnlineHelpMediaEnabled(it, context) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Permissions 
        Text(
            text = stringResource(R.string.settings_section_permissions),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = Shapes.extraSmall,
                        )
                        .onGloballyPositioned { coordinates ->
                            permissionsSectionY = coordinates.positionInParent().y
                        }
                        .clickable { isPermissionsExpanded = !isPermissionsExpanded }
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_shield_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.settings_permissions_all),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Icon(
                    painter =
                        painterResource(
                            id = if (isPermissionsExpanded) R.drawable.rounded_keyboard_arrow_up_24 else R.drawable.rounded_keyboard_arrow_down_24,
                        ),
                    contentDescription = if (isPermissionsExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = isPermissionsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                val permissionItems =
                    remember(
                        isAccessibilityEnabled,
                        isWriteSecureSettingsEnabled,
                        isRootEnabled,
                        isRootPermissionGranted,
                        isShizukuPermissionGranted,
                        isShizukuAvailable,
                        isReadPhoneStateEnabled,
                        isPostNotificationsEnabled,
                        isOverlayPermissionGranted,
                        isNotificationListenerEnabled,
                        isWriteSettingsEnabled,
                        isNotificationPolicyAccessGranted,
                        isDefaultBrowserSet,
                        isUsageStatsPermissionGranted,
                        isLocationPermissionGranted,
                        isBackgroundLocationPermissionGranted,
                        isDeviceAdminEnabled,
                        isCalendarPermissionGranted,
                    ) {
                        PermissionUIHelper.getAllPermissionItems(context, viewModel, context as? ComponentActivity)
                    }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    permissionItems.forEach { item ->
                        PermissionCard(
                            iconRes = item.iconRes,
                            title = item.title,
                            dependentFeatures = item.dependentFeatures,
                            actionLabel = item.actionLabel ?: R.string.perm_action_grant,
                            isGranted = item.isGranted,
                            onActionClick = { item.action?.invoke() },
                            secondaryActionLabel = item.secondaryActionLabel,
                            onSecondaryActionClick = item.secondaryAction,
                            shizukuActionLabel = item.shizukuActionLabel,
                            shizukuActionEnabled = item.shizukuActionEnabled,
                            onShizukuActionClick = item.shizukuAction,
                            instructions = item.instructions,
                            description = item.description,
                        )
                    }
                }
            }

            IconToggleItem(
                iconRes = R.drawable.rounded_numbers_24,
                title = stringResource(R.string.setting_use_root_title),
                description = stringResource(R.string.setting_use_root_desc),
                isChecked = viewModel.isRootEnabled.value,
                onCheckedChange = { viewModel.setRootEnabled(it, context) },
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceBright,
                            shape = MaterialTheme.shapes.extraSmall,
                        ).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val view = LocalView.current
                var tokenText by remember { mutableStateOf(viewModel.shizukuAuthToken.value) }
                var isTokenVisible by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = tokenText,
                    onValueChange = { tokenText = it },
                    label = { Text("Shizuku auth token") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                            Icon(
                                painter =
                                    painterResource(
                                        id = if (isTokenVisible) R.drawable.rounded_visibility_24 else R.drawable.rounded_visibility_off_24,
                                    ),
                                contentDescription = if (isTokenVisible) "Hide token" else "Show token",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                )

                Button(
                    onClick = {
                        viewModel.setShizukuAuthToken(tokenText)
                        HapticUtil.performUIHaptic(view)
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_save_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }

                OutlinedButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        showShizukuHelpBottomSheet = true
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_help_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            IconToggleItem(
                iconRes = R.drawable.rounded_data_usage_24,
                title = stringResource(R.string.setting_use_usage_access_title),
                description = stringResource(R.string.setting_use_usage_access_desc),
                isChecked = viewModel.isUseUsageAccess.value,
                onCheckedChange = { viewModel.setUseUsageAccess(it, context) },
            )

            if (isGenAISupported) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_auto_awesome_24,
                    title = stringResource(R.string.settings_genai_automation_title),
                    description = stringResource(R.string.settings_genai_automation_desc),
                    isChecked = isGenAIAutomationEnabled,
                    onCheckedChange = { viewModel.setGenAIAutomationEnabled(it, context) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // More
        Text(
            text = stringResource(R.string.settings_section_more),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer {
            CrashReportingPicker(
                selectedMode = sentryMode,
                onModeSelected = { viewModel.setSentryReportMode(it, context) },
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_release_alert_24,
                title = stringResource(R.string.setting_enable_unsupported_features_title),
                description = stringResource(R.string.setting_enable_unsupported_features_desc),
                isChecked = viewModel.isEnableUnsupportedFeatures.value,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        showUnsupportedFeaturesSheet = true
                    } else {
                        viewModel.setEnableUnsupportedFeatures(false, context)
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Translations Section
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_translations_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val currentAppLocale =
            LocalContext.current.resources.configuration.locales[0]
                .language
        val isEnglishApp = currentAppLocale == "en" || currentAppLocale.isBlank()

        RoundedCardContainer {
            // GitHub Account Card (Tap to Sign In when logged out, Long Press to Sign Out when logged in)
            FeatureCard(
                title = if (currentUser != null) "@${currentUser?.login}" else stringResource(R.string.action_sign_in_github),
                description =
                    if (currentUser !=
                        null
                    ) {
                        "Logged in as ${currentUser?.name ?: currentUser?.login}"
                    } else {
                        "Sign in required to translate"
                    },
                isEnabled = true,
                onToggle = {},
                onClick = {
                    if (currentUser == null) {
                        HapticUtil.performUIHaptic(view)
                        showGitHubAuthSheet = true
                    }
                },
                showToggle = false,
                iconRes = R.drawable.brand_github,
                additionalMenuItems =
                    if (currentUser != null) {
                        @Composable { onDismiss ->
                            SegmentedDropdownMenuItem(
                                text = { Text("Sign Out") },
                                onClick = {
                                    onDismiss()
                                    HapticUtil.performUIHaptic(view)
                                    gitHubAuthViewModel.signOut(context)
                                    currentUser = null
                                    TranslationManager.isTranslationModeEnabled.value = false
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_logout_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                            )
                        }
                    } else {
                        null
                    },
            )

            // Translation Mode Switch (IconToggleItem - clean toggle variant without sub-menu divider)
            IconToggleItem(
                iconRes = R.drawable.rounded_translate_24,
                title = stringResource(R.string.settings_translate_mode),
                description =
                    if (isEnglishApp) {
                        "App language is English. Change app language to translate strings"
                    } else {
                        stringResource(
                            R.string.settings_translate_mode_desc,
                        )
                    },
                isChecked = isTranslationModeActive && !isEnglishApp,
                enabled = !isEnglishApp,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (currentUser == null) {
                            showGitHubAuthSheet = true
                        } else if (!settingsRepo.isTranslationModeWarningSuppressed()) {
                            showTranslationWarningDialog = true
                        } else {
                            TranslationManager.isTranslationModeEnabled.value = true
                        }
                    } else {
                        TranslationManager.isTranslationModeEnabled.value = false
                    }
                },
            )

            // Pending Edits Summary Card
            if (sessionEditsCount > 0) {
                FeatureCard(
                    title = R.string.settings_translated_texts,
                    description =
                        stringResource(
                            R.string.settings_translated_texts_desc,
                            sessionEditsCount,
                        ),
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        showTranslationSessionSheet = true
                    },
                    showToggle = false,
                    iconRes = R.drawable.rounded_edit_24,
                )
            }

            // Existing Open Translation PRs
            if (isTranslationModeActive && openTranslationPRs.isNotEmpty()) {
                openTranslationPRs.forEach { pr ->
                    val dateFormatted =
                        try {
                            pr.updatedAt.take(10)
                        } catch (e: Exception) {
                            ""
                        }
                    val subtitleText =
                        if (dateFormatted.isNotBlank()) "PR #${pr.number} • Updated $dateFormatted" else "PR #${pr.number}"

                    FeatureCard(
                        title = "View existing PR",
                        description = subtitleText,
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            val intent =
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(pr.htmlUrl),
                                )

                            context.startActivity(intent)
                        },
                        showToggle = false,
                        iconRes = R.drawable.brand_github,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        RoundedCardContainer {
            FeatureCard(
                title = R.string.action_restart_systemui,
                description = R.string.desc_restart_systemui,
                isEnabled = true,
                onToggle = {},
                showToggle = false,
                onClick = { viewModel.restartSystemUI() },
                iconRes = R.drawable.rounded_refresh_24,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        MadebySameeraswCard()

        Spacer(modifier = Modifier.height(4.dp))

        RoundedCardContainer {
            AboutSection(
                onAvatarLongClick = {
                    val newState = !isDeveloperModeEnabled
                    viewModel.setDeveloperModeEnabled(newState, context)
                    Toast
                        .makeText(
                            context,
                            if (newState) "Developer options enabled" else "Developer options disabled",
                            Toast.LENGTH_SHORT,
                        ).show()
                },
                onAvatarLongClickWithPosition = onAvatarLongClickWithPosition,
            )
        }

        if (isDeveloperModeEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            // Updates Section
            Text(
                text = "Developer Options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RoundedCardContainer {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = Shapes.extraSmall,
                            ).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val timeStamp =
                                SimpleDateFormat(
                                    "yyyyMMdd_HHmmss",
                                    Locale.getDefault(),
                                ).format(Date())
                            exportLauncher.launch("essentials_config_$timeStamp.json")
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp),
                        shape = ButtonGroupDefaults.connectedLeadingButtonShapes().shape,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_vertical_align_bottom_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_export_config))
                    }
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            importLauncher.launch(arrayOf("application/json"))
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp),
                        shape = ButtonGroupDefaults.connectedTrailingButtonShapes().shape,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_vertical_align_top_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_import_config))
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = Shapes.extraSmall,
                            ).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.resetOnboarding(context)
                            Toast.makeText(context, context.getString(R.string.toast_onboarding_reset), Toast.LENGTH_SHORT).show()
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_refresh_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_reset_onboarding))
                    }

                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.resetUpdateNote(context)
                            Toast.makeText(context, context.getString(R.string.toast_update_note_reset), Toast.LENGTH_SHORT).show()
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_refresh_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_reset_update_note))
                    }

                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.clearRecentSearches()
                            Toast.makeText(context, context.getString(R.string.toast_search_history_cleared), Toast.LENGTH_SHORT).show()
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_delete_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_clear_search_history))
                    }
                }

                IconToggleItem(
                    iconRes = R.drawable.rounded_bug_report_24,
                    title = stringResource(R.string.crash_logs_title),
                    description = stringResource(R.string.crash_logs_desc),
                    showToggle = false,
                    onClick = {
                        showCrashLogsSheet = true
                    },
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = Shapes.extraSmall,
                            ).padding(16.dp),
                ) {
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            throw RuntimeException("Simulated crash from Developer Options")
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp),
                        shape = ButtonDefaults.shape,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_bug_report_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.simulate_crash),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                IconToggleItem(
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    title = stringResource(R.string.feat_auto_accessibility_title),
                    description = stringResource(R.string.feat_auto_accessibility_desc),
                    isChecked = viewModel.isAutoAccessibilityEnabled.value,
                    onCheckedChange = { viewModel.setAutoAccessibilityEnabled(it, context) },
                )
            }

            val gitHubUser = viewModel.gitHubUser.value
            if (gitHubUser?.login == "sameerasw") {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Wallpaper Update",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val workflowToken by remember { mutableStateOf(viewModel.gitHubWorkflowToken) }
                val hasWorkflowToken = !workflowToken.value.isNullOrEmpty()

                RoundedCardContainer {
                    if (!hasWorkflowToken) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(color = MaterialTheme.colorScheme.surfaceBright)
                                    .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Workflow authorization is required to trigger wallpaper updates remotely.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            val workflowAuthState by viewModel.workflowAuthState

                            when (workflowAuthState) {
                                is com.sameerasw.essentials.viewmodels.AuthState.Idle -> {
                                    Button(
                                        onClick = {
                                            viewModel.startWorkflowAuthFlow(context)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Grant Workflow Access")
                                    }
                                }

                                is com.sameerasw.essentials.viewmodels.AuthState.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }

                                is com.sameerasw.essentials.viewmodels.AuthState.CodeReceived -> {
                                    val codeData =
                                        workflowAuthState as com.sameerasw.essentials.viewmodels.AuthState.CodeReceived
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = "Verification Code:",
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            text = codeData.userCode,
                                            style =
                                                MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = "Go to: ${codeData.verificationUri}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Button(
                                                onClick = {
                                                    val intent =
                                                        Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(codeData.verificationUri),
                                                        )
                                                    context.startActivity(intent)
                                                },
                                            ) {
                                                Text("Open Page")
                                            }
                                            TextButton(
                                                onClick = {
                                                    viewModel.cancelWorkflowAuthFlow()
                                                },
                                            ) {
                                                Text("Cancel")
                                            }
                                        }
                                    }
                                }

                                is com.sameerasw.essentials.viewmodels.AuthState.Error -> {
                                    val err =
                                        workflowAuthState as com.sameerasw.essentials.viewmodels.AuthState.Error
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = err.message,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.startWorkflowAuthFlow(context)
                                            },
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }

                                is com.sameerasw.essentials.viewmodels.AuthState.Authenticated -> {
                                    // Handled by token recomposition
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(color = MaterialTheme.colorScheme.surfaceBright)
                                    .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Trigger unsplash wallpaper update on your website directly:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val triggerState by viewModel.wallpaperTriggerState
                                val isTriggering = triggerState != null

                                Button(
                                    onClick = {
                                        HapticUtil.performUIHaptic(view)
                                        viewModel.triggerWallpaperUpdate("desktop")
                                    },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 44.dp),
                                    shape = ButtonGroupDefaults.connectedLeadingButtonShapes().shape,
                                    enabled = !isTriggering,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_laptop_mac_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_wallpaper_desktop))
                                }
                                Button(
                                    onClick = {
                                        HapticUtil.performUIHaptic(view)
                                        viewModel.triggerWallpaperUpdate("both")
                                    },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 44.dp),
                                    shape = ButtonGroupDefaults.connectedMiddleButtonShapes().shape,
                                    enabled = !isTriggering,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_devices_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_wallpaper_both))
                                }
                                Button(
                                    onClick = {
                                        HapticUtil.performUIHaptic(view)
                                        viewModel.triggerWallpaperUpdate("mobile")
                                    },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 44.dp),
                                    shape = ButtonGroupDefaults.connectedTrailingButtonShapes().shape,
                                    enabled = !isTriggering,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_mobile_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_wallpaper_mobile))
                                }
                            }

                            val triggerState by viewModel.wallpaperTriggerState
                            if (triggerState != null) {
                                Text(
                                    text =
                                        when (triggerState) {
                                            "loading" -> "Sending trigger request..."
                                            "success" -> "Trigger sent successfully!"
                                            "error" -> "Failed to send trigger."
                                            else -> ""
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        when (triggerState) {
                                            "success" -> MaterialTheme.colorScheme.primary
                                            "error" -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showTranslationSessionSheet) {
        TranslationSessionSheet(
            onDismissRequest = { showTranslationSessionSheet = false },
            onNeedLogin = {
                showTranslationSessionSheet = false
                showGitHubAuthSheet = true
            },
        )
    }

    if (showGitHubAuthSheet) {
        GitHubAuthSheet(
            viewModel = gitHubAuthViewModel,
            onDismissRequest = {
                showGitHubAuthSheet = false
                currentUser = settingsRepo.getGitHubUser()
            },
        )
    }

    if (showTranslationWarningDialog) {
        com.sameerasw.essentials.translation.ui.TranslationWarningBottomSheet(
            onDismissRequest = { showTranslationWarningDialog = false },
            onConfirm = { dontShow ->
                if (dontShow) {
                    settingsRepo.setTranslationModeWarningSuppressed(true)
                }
                TranslationManager.isTranslationModeEnabled.value = true
                showTranslationWarningDialog = false
            },
        )
    }
}
