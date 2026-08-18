/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: AutomationEditorActivity.kt
 * Description: Activity component for AutomationEditorActivity.kt.
 */

package com.sameerasw.essentials.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.NotificationApp
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import com.sameerasw.essentials.ui.components.CategoryExpandableSection
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.core.cards.AppToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.BluetoothDeviceSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.CustomSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.DimWallpaperSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.ScreenOffSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.SoundModeSettingsSheet
import com.sameerasw.essentials.ui.core.sheets.WifiNetworkSelectionSheet
import com.sameerasw.essentials.ui.features.apps.sheets.KeyboardSelectionSheet
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sameerasw.essentials.domain.diy.State as DIYState

class AutomationEditorActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_AUTOMATION_ID = "automation_id"
        private const val EXTRA_AUTOMATION_TYPE = "automation_type"

        fun createIntent(context: Context, automationId: String): Intent {
            return Intent(context, AutomationEditorActivity::class.java).apply {
                putExtra(EXTRA_AUTOMATION_ID, automationId)
            }
        }

        fun createIntent(context: Context, type: Automation.Type): Intent {
            return Intent(context, AutomationEditorActivity::class.java).apply {
                putExtra(EXTRA_AUTOMATION_TYPE, type.name)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init repository
        DIYRepository.init(applicationContext)

        val automationId = intent.getStringExtra(EXTRA_AUTOMATION_ID)
        val automationTypeStr = intent.getStringExtra(EXTRA_AUTOMATION_TYPE)

        val existingAutomation =
            if (automationId != null) DIYRepository.getAutomation(automationId) else null
        val isEditMode = existingAutomation != null

        val automationType = if (isEditMode) {
            existingAutomation.type
        } else {
            try {
                Automation.Type.valueOf(automationTypeStr ?: Automation.Type.TRIGGER.name)
            } catch (e: Exception) {
                Automation.Type.TRIGGER
            }
        }

        val titleRes = when (automationType) {
            Automation.Type.TRIGGER -> if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_editor_new_title
            Automation.Type.ACTION_SHORTCUT -> if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_editor_new_title
            Automation.Type.PIXEL_SEARCHBAR -> if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_editor_new_title
            Automation.Type.STATE -> if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_editor_new_title
            Automation.Type.APP -> if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_create_app_title
        }

        setContent {
            val viewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.check(context)
            }
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val view = LocalView.current
                val coroutineScope = rememberCoroutineScope()
                var carouselState = rememberCarouselState { 2 } // 0: Trigger/State, 1: Actions

                // Haptic on carousel page change
                LaunchedEffect(carouselState) {
                    var isFirst = true
                    snapshotFlow { carouselState.currentItem }
                        .collect {
                            if (isFirst) {
                                isFirst = false
                            } else {
                                HapticUtil.performHeavyHaptic(view)
                            }
                        }
                }

                // State for selections
                // Initialize with existing data or defaults
                var selectedTrigger by remember { mutableStateOf<Trigger?>(existingAutomation?.trigger) }
                var selectedState by remember { mutableStateOf<DIYState?>(existingAutomation?.state) }
                var selectedApps by remember {
                    mutableStateOf<List<String>>(
                        existingAutomation?.selectedApps ?: emptyList()
                    )
                }

                // App Picker State
                var searchQuery by remember { mutableStateOf("") }
                var allApps by remember { mutableStateOf<List<NotificationApp>>(emptyList()) }
                var isLoadingApps by remember { mutableStateOf(false) }
                var showSystemApps by remember { mutableStateOf(false) }

                // Load apps if needed
                LaunchedEffect(automationType) {
                    if (automationType == Automation.Type.APP) {
                        isLoadingApps = true
                        withContext(Dispatchers.IO) {
                            try {
                                val installed = AppUtil.getInstalledApps(context)
                                // Merge with selection if existing
                                val merged = AppUtil.mergeWithSavedApps(
                                    installed,
                                    selectedApps.map { AppSelection(it, true) })
                                withContext(Dispatchers.Main) {
                                    allApps = merged
                                    isLoadingApps = false
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) { isLoadingApps = false }
                            }
                        }
                    }
                }

                val filteredApps = remember(allApps, searchQuery, showSystemApps, selectedApps) {
                    allApps.filter {
                        val matchesSearch = searchQuery.isEmpty() || it.appName.contains(
                            searchQuery,
                            ignoreCase = true
                        )
                        val isVisible =
                            !it.isSystemApp || showSystemApps || selectedApps.contains(it.packageName)
                        matchesSearch && isVisible
                    }
                        .sortedWith(compareByDescending<NotificationApp> { selectedApps.contains(it.packageName) }.thenBy { it.appName.lowercase() })
                }

                // Actions
                // For Trigger type
                var selectedAction by remember { mutableStateOf<Action?>(existingAutomation?.actions?.firstOrNull()) }

                // For State type
                var selectedInAction by remember { mutableStateOf<Action?>(existingAutomation?.entryAction) }
                var selectedOutAction by remember { mutableStateOf<Action?>(existingAutomation?.exitAction) }

                // Tab for State Actions
                var selectedActionTab by remember { mutableIntStateOf(0) } // 0: In, 1: Out

                // Menu State
                var showMenu by remember { mutableStateOf(false) }

                // Config Sheets
                var showDimSettings by remember { mutableStateOf(false) }
                var showScreenOffSettings by remember { mutableStateOf(false) }
                var showDeviceEffectsSettings by remember { mutableStateOf(false) }
                var showSoundModeSettings by remember { mutableStateOf(false) }
                var showSometimesEssentialsSettings by remember { mutableStateOf(false) }
                var showFreezeTagSettings by remember { mutableStateOf(false) }
                var showOpenAppSettings by remember { mutableStateOf(false) }
                var showFreezeAppsSettings by remember { mutableStateOf(false) }
                var temporarySelectedAppsForAction by remember { mutableStateOf<List<String>>(emptyList()) }
                var showTimeSettings by remember { mutableStateOf(false) }
                var showBluetoothSettings by remember { mutableStateOf(false) }
                var showWifiSettings by remember { mutableStateOf(false) }
                var showSetKeyboardSheet by remember { mutableStateOf(false) }
                var selectedIme by remember { mutableStateOf<String?>(null) }
                var showCustomSettingsSettings by remember { mutableStateOf(false) }
                var configAction by remember { mutableStateOf<Action?>(null) } // Generic config action

                val isTriggerConfigured = when (val trigger = selectedTrigger) {
                    is Trigger.BluetoothConnected -> trigger.deviceAddress.isNotBlank()
                    is Trigger.BluetoothDisconnected -> trigger.deviceAddress.isNotBlank()
                    is Trigger.WifiConnected -> trigger.ssid.isNotBlank()
                    is Trigger.WifiDisconnected -> trigger.ssid.isNotBlank()
                    else -> true
                }

                var showPermissionSheet by remember { mutableStateOf(false) }
                var permissionKeysToShow by remember { mutableStateOf<List<String>>(emptyList()) }
                var permissionFeatureTitle by remember { mutableStateOf<Any>("") }

                // Automatic refresh on resume
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.check(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                fun isActionConfigured(action: Action?): Boolean = when (action) {
                    is Action.OpenApp -> action.packageName.isNotBlank()
                    is Action.CustomSettings -> action.entries.isNotEmpty()
                    else -> true
                }

                // Validation
                val isValid = when (automationType) {
                    Automation.Type.TRIGGER -> selectedTrigger != null && selectedAction != null && isTriggerConfigured && isActionConfigured(
                        selectedAction
                    )

                    Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction != null && isActionConfigured(
                        selectedAction
                    )

                    Automation.Type.STATE -> selectedState != null && (selectedInAction != null || selectedOutAction != null) && isActionConfigured(
                        selectedInAction
                    ) && isActionConfigured(selectedOutAction)

                    Automation.Type.APP -> selectedApps.isNotEmpty() && (selectedInAction != null || selectedOutAction != null) && isActionConfigured(
                        selectedInAction
                    ) && isActionConfigured(selectedOutAction)
                }

                var showDiscardDialog by remember { mutableStateOf(false) }
                val isBlurEnabled by viewModel.isBlurEnabled

                val handleBackClick = {
                    showDiscardDialog = true
                }

                BackHandler {
                    handleBackClick()
                }

                if (showDiscardDialog) {
                    AlertDialog(
                        onDismissRequest = { showDiscardDialog = false },
                        title = { Text(stringResource(R.string.diy_discard_warning_title)) },
                        text = { Text(stringResource(R.string.diy_discard_warning_desc)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    showDiscardDialog = false
                                    finish()
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.translation_discard),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    showDiscardDialog = false
                                }
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }

                fun getMissingPermissionsHelper(action: Action?): List<String> {
                    if (action == null) return emptyList()
                    val resolvedPermissions = action.permissions.map { permKey ->
                        if (permKey == "SHIZUKU" || permKey == "ROOT") {
                            if (com.sameerasw.essentials.utils.ShellUtils.isRootEnabled(context)) "ROOT" else "SHIZUKU"
                        } else {
                            permKey
                        }
                    }.distinct()

                    return resolvedPermissions.filter { permKey ->
                        when (permKey) {
                            "SHIZUKU" -> !viewModel.isShizukuPermissionGranted.value
                            "ROOT" -> !viewModel.isRootPermissionGranted.value
                            "WRITE_SETTINGS" -> !viewModel.isWriteSettingsEnabled.value
                            "NOTIFICATION_POLICY" -> !viewModel.isNotificationPolicyAccessGranted.value
                            "WRITE_SECURE_SETTINGS" -> !viewModel.isWriteSecureSettingsEnabled.value
                            else -> false
                        }
                    }
                }

                val performSave = {
                    val actionsToCheck = when (automationType) {
                        Automation.Type.TRIGGER, Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> listOfNotNull(selectedAction)
                        else -> listOfNotNull(selectedInAction, selectedOutAction)
                    }
                    val allMissingPermissions = actionsToCheck.flatMap { getMissingPermissionsHelper(it) }.distinct()
                    if (allMissingPermissions.isNotEmpty()) {
                        permissionKeysToShow = allMissingPermissions
                        permissionFeatureTitle = R.string.tab_diy
                        showPermissionSheet = true
                    } else {
                        if (automationType == Automation.Type.TRIGGER) {
                            val newAutomation = Automation(
                                id = if (isEditMode) existingAutomation.id else java.util.UUID.randomUUID().toString(),
                                type = Automation.Type.TRIGGER,
                                trigger = selectedTrigger,
                                actions = listOfNotNull(selectedAction)
                            )
                            if (isEditMode) DIYRepository.updateAutomation(newAutomation) else DIYRepository.addAutomation(newAutomation)
                        } else if (automationType == Automation.Type.ACTION_SHORTCUT || automationType == Automation.Type.PIXEL_SEARCHBAR) {
                            val newAutomation = Automation(
                                id = if (isEditMode) existingAutomation.id else java.util.UUID.randomUUID().toString(),
                                type = automationType,
                                actions = listOfNotNull(selectedAction)
                            )
                            if (isEditMode) DIYRepository.updateAutomation(newAutomation) else DIYRepository.addAutomation(newAutomation)
                        } else if (automationType == Automation.Type.STATE) {
                            val newAutomation = Automation(
                                id = if (isEditMode) existingAutomation.id else java.util.UUID.randomUUID().toString(),
                                type = Automation.Type.STATE,
                                state = selectedState,
                                entryAction = selectedInAction,
                                exitAction = selectedOutAction
                            )
                            if (isEditMode) DIYRepository.updateAutomation(newAutomation) else DIYRepository.addAutomation(newAutomation)
                        } else if (automationType == Automation.Type.APP) {
                            val newAutomation = Automation(
                                id = if (isEditMode) existingAutomation.id else java.util.UUID.randomUUID().toString(),
                                type = Automation.Type.APP,
                                selectedApps = selectedApps,
                                entryAction = selectedInAction,
                                exitAction = selectedOutAction
                            )
                            if (isEditMode) DIYRepository.updateAutomation(newAutomation) else DIYRepository.addAutomation(newAutomation)
                        }
                        finish()
                    }
                }

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) { _ ->
                    val density = LocalDensity.current
                    val statusBarHeightPx = with(density) {
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
                    }
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .progressiveBlur(
                                blurRadius = if (isBlurEnabled) 40f else 0f,
                                height = statusBarHeightPx * 1.15f,
                                direction = BlurDirection.TOP
                            )
                    ) {
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp

                        // Haptic Connection for Swipe Texture
                        val nestedScrollConnection = remember {
                            object : NestedScrollConnection {
                                var accumulatedScroll = 0f
                                val threshold = 40f

                                override fun onPreScroll(
                                    available: Offset,
                                    source: NestedScrollSource
                                ): Offset {
                                    if (source == NestedScrollSource.UserInput) {
                                        accumulatedScroll += available.x

                                        if (kotlin.math.abs(accumulatedScroll) >= threshold) {
                                            HapticUtil.performSliderHaptic(view)
                                            accumulatedScroll = 0f
                                        }
                                    }
                                    return Offset.Zero
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .progressiveBlur(
                                    blurRadius = if (isBlurEnabled) 40f else 0f,
                                    height = with(density) { 150.dp.toPx() },
                                    direction = BlurDirection.BOTTOM
                                )
                        ) {
                        HorizontalMultiBrowseCarousel(
                            state = carouselState,
                            preferredItemWidth = screenWidth,
                            itemSpacing = 4.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .nestedScroll(nestedScrollConnection),
                            contentPadding = PaddingValues(horizontal = 18.dp)
                        ) { index ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                val isCurrentSelected = carouselState.currentItem == index

                                if (index == 0) {
                                    // PAGE 0: Trigger or State Picker
                                    if (automationType == Automation.Type.APP) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Spacer(modifier = Modifier.height(statusBarHeight + 4.dp))
                                            Text(
                                                text = stringResource(R.string.diy_create_app_title),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )

                                            // Search Bar
                                            OutlinedTextField(
                                                value = searchQuery,
                                                onValueChange = { searchQuery = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                placeholder = { Text(stringResource(R.string.label_search)) },
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.rounded_search_24),
                                                        contentDescription = stringResource(R.string.action_search)
                                                    )
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp)
                                            )

                                            // System Apps Toggle
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable {
                                                        HapticUtil.performVirtualKeyHaptic(view)
                                                        showSystemApps = !showSystemApps
                                                    }
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.rounded_settings_24),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = stringResource(R.string.toggle_show_system_apps),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.weight(1f),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Switch(
                                                    checked = showSystemApps,
                                                    onCheckedChange = {
                                                        HapticUtil.performVirtualKeyHaptic(view)
                                                        showSystemApps = it
                                                    }
                                                )
                                            }

                                            if (isLoadingApps) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    LoadingIndicator()
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(24.dp)),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                                    contentPadding = PaddingValues(
                                                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                                                    )
                                                ) {
                                                    items(
                                                        filteredApps,
                                                        key = { it.packageName }) { app ->
                                                        val isSelected =
                                                            selectedApps.contains(app.packageName)
                                                        AppToggleItem(
                                                            icon = app.icon,
                                                            title = app.appName,
                                                            isChecked = isSelected,
                                                            onCheckedChange = { isChecked ->
                                                                val current =
                                                                    selectedApps.toMutableList()
                                                                if (isChecked) current.add(app.packageName) else current.remove(
                                                                    app.packageName
                                                                )
                                                                selectedApps = current
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else if (automationType == Automation.Type.ACTION_SHORTCUT || automationType == Automation.Type.PIXEL_SEARCHBAR) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Spacer(modifier = Modifier.height(statusBarHeight + 4.dp))
                                            Text(
                                                text = stringResource(R.string.diy_select_trigger),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )

                                            RoundedCardContainer(spacing = 2.dp) {
                                                val editorTitle =
                                                    if (automationType == Automation.Type.PIXEL_SEARCHBAR) {
                                                        stringResource(R.string.diy_create_pixel_searchbar_title)
                                                    } else {
                                                        stringResource(R.string.diy_create_action_shortcut_title)
                                                    }
                                                val editorIcon =
                                                    if (automationType == Automation.Type.PIXEL_SEARCHBAR) {
                                                        R.drawable.rounded_search_24
                                                    } else {
                                                        R.drawable.rounded_rocket_launch_24
                                                    }
                                                EditorActionItem(
                                                    title = editorTitle,
                                                    iconRes = editorIcon,
                                                    isSelected = true,
                                                    isConfigurable = false,
                                                    onClick = {}
                                                )
                                            }
                                            Spacer(
                                                modifier = Modifier.height(
                                                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                                                )
                                            )
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Spacer(modifier = Modifier.height(statusBarHeight + 4.dp))
                                            Text(
                                                text = stringResource(if (automationType == Automation.Type.TRIGGER) R.string.diy_select_trigger else R.string.diy_select_state),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )

                                            if (automationType == Automation.Type.TRIGGER) {
                                                val triggerCategories = remember(selectedTrigger) {
                                                    listOf(
                                                        R.string.diy_category_system_screen to listOf(
                                                            Trigger.ScreenOff,
                                                            Trigger.ScreenOn,
                                                            Trigger.DeviceUnlock
                                                        ),
                                                        R.string.diy_category_battery_power to listOf(
                                                            Trigger.ChargerConnected,
                                                            Trigger.ChargerDisconnected,
                                                            Trigger.PowerSavingOn,
                                                            Trigger.PowerSavingOff
                                                        ),
                                                        R.string.diy_category_connectivity to listOf(
                                                            Trigger.BluetoothConnected(
                                                                deviceAddress = (selectedTrigger as? Trigger.BluetoothConnected)?.deviceAddress ?: "",
                                                                deviceName = (selectedTrigger as? Trigger.BluetoothConnected)?.deviceName ?: ""
                                                            ),
                                                            Trigger.BluetoothDisconnected(
                                                                deviceAddress = (selectedTrigger as? Trigger.BluetoothDisconnected)?.deviceAddress ?: "",
                                                                deviceName = (selectedTrigger as? Trigger.BluetoothDisconnected)?.deviceName ?: ""
                                                            ),
                                                            Trigger.WifiConnected(
                                                                ssid = (selectedTrigger as? Trigger.WifiConnected)?.ssid ?: ""
                                                            ),
                                                            Trigger.WifiDisconnected(
                                                                ssid = (selectedTrigger as? Trigger.WifiDisconnected)?.ssid ?: ""
                                                            )
                                                        ),
                                                        R.string.diy_category_time_schedule to listOf(
                                                            Trigger.Schedule(
                                                                hour = (selectedTrigger as? Trigger.Schedule)?.hour ?: 0,
                                                                minute = (selectedTrigger as? Trigger.Schedule)?.minute ?: 0,
                                                                days = (selectedTrigger as? Trigger.Schedule)?.days ?: emptySet()
                                                            )
                                                        )
                                                    )
                                                }

                                                var expandedTriggerCategory by remember {
                                                    mutableStateOf<Int?>(
                                                        triggerCategories.firstOrNull { (_, list) ->
                                                            list.any { selectedTrigger != null && it::class == selectedTrigger!!::class }
                                                        }?.first ?: triggerCategories.firstOrNull()?.first
                                                    )
                                                }

                                                triggerCategories.forEach { (categoryTitleRes, triggerList) ->
                                                    CategoryExpandableSection(
                                                        title = stringResource(categoryTitleRes),
                                                        itemCount = triggerList.size,
                                                        isExpanded = expandedTriggerCategory == categoryTitleRes,
                                                        onToggleExpand = {
                                                            expandedTriggerCategory = if (expandedTriggerCategory == categoryTitleRes) null else categoryTitleRes
                                                        }
                                                    ) {
                                                        triggerList.forEach { trigger ->
                                                            val isSelected = selectedTrigger != null && selectedTrigger!!::class == trigger::class
                                                            EditorActionItem(
                                                                title = stringResource(trigger.title),
                                                                iconRes = trigger.icon,
                                                                isSelected = isSelected,
                                                                isConfigurable = trigger.isConfigurable,
                                                                onClick = { selectedTrigger = trigger },
                                                                onSettingsClick = {
                                                                    when (trigger) {
                                                                        is Trigger.Schedule -> showTimeSettings = true
                                                                        is Trigger.BluetoothConnected, is Trigger.BluetoothDisconnected -> showBluetoothSettings = true
                                                                        is Trigger.WifiConnected, is Trigger.WifiDisconnected -> showWifiSettings = true
                                                                        else -> {}
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                val stateCategories = remember(selectedState) {
                                                    listOf(
                                                        R.string.diy_category_battery_power to listOf(
                                                            DIYState.Charging,
                                                            DIYState.PowerSaving
                                                        ),
                                                        R.string.diy_category_system_screen to listOf(
                                                            DIYState.ScreenOn
                                                        ),
                                                        R.string.diy_category_time_schedule to listOf(
                                                            DIYState.TimePeriod(
                                                                startHour = (selectedState as? DIYState.TimePeriod)?.startHour ?: 0,
                                                                startMinute = (selectedState as? DIYState.TimePeriod)?.startMinute ?: 0,
                                                                endHour = (selectedState as? DIYState.TimePeriod)?.endHour ?: 0,
                                                                endMinute = (selectedState as? DIYState.TimePeriod)?.endMinute ?: 0,
                                                                days = (selectedState as? DIYState.TimePeriod)?.days ?: emptySet()
                                                            )
                                                        )
                                                    )
                                                }

                                                var expandedStateCategory by remember {
                                                    mutableStateOf<Int?>(
                                                        stateCategories.firstOrNull { (_, list) ->
                                                            list.any { selectedState != null && it::class == selectedState!!::class }
                                                        }?.first ?: stateCategories.firstOrNull()?.first
                                                    )
                                                }

                                                stateCategories.forEach { (categoryTitleRes, stateList) ->
                                                    CategoryExpandableSection(
                                                        title = stringResource(categoryTitleRes),
                                                        itemCount = stateList.size,
                                                        isExpanded = expandedStateCategory == categoryTitleRes,
                                                        onToggleExpand = {
                                                            expandedStateCategory = if (expandedStateCategory == categoryTitleRes) null else categoryTitleRes
                                                        }
                                                    ) {
                                                        stateList.forEach { state ->
                                                            val isSelected = selectedState != null && selectedState!!::class == state::class
                                                            EditorActionItem(
                                                                title = stringResource(state.title),
                                                                iconRes = state.icon,
                                                                isSelected = isSelected,
                                                                onClick = { selectedState = state },
                                                                isConfigurable = state is DIYState.TimePeriod,
                                                                onSettingsClick = {
                                                                    if (state is DIYState.TimePeriod) {
                                                                        showTimeSettings = true
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(
                                                modifier = Modifier.height(
                                                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    // PAGE 1: Action Picker
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Spacer(modifier = Modifier.height(statusBarHeight + 4.dp))
                                        Text(
                                            text = stringResource(R.string.diy_select_action),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )

                                        if (automationType == Automation.Type.STATE || automationType == Automation.Type.APP) {
                                            // Tabs for In/Out
                                            val options = listOf(
                                                stringResource(R.string.diy_in_action_label),
                                                stringResource(R.string.diy_out_action_label)
                                            )
                                            SegmentedPicker(
                                                items = options,
                                                selectedItem = options[selectedActionTab],
                                                onItemSelected = {
                                                    HapticUtil.performUIHaptic(view)
                                                    selectedActionTab = options.indexOf(it)
                                                },
                                                labelProvider = { it },
                                                modifier = Modifier.fillMaxWidth(),
                                                        cornerShape = MaterialTheme.shapes.extraExtraLarge.bottomEnd
                                            )
                                        }

                                        val currentSelection = when (automationType) {
                                            Automation.Type.TRIGGER -> selectedAction
                                            Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction
                                            Automation.Type.STATE -> if (selectedActionTab == 0) selectedInAction else selectedOutAction
                                            Automation.Type.APP -> if (selectedActionTab == 0) selectedInAction else selectedOutAction
                                        }

                                        // None option
                                        RoundedCardContainer(spacing = 2.dp) {
                                            EditorActionItem(
                                                title = stringResource(R.string.haptic_none),
                                                iconRes = R.drawable.rounded_do_not_disturb_on_24,
                                                isSelected = currentSelection == null,
                                                onClick = {
                                                    when (automationType) {
                                                        Automation.Type.TRIGGER -> selectedAction = null
                                                        Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction = null
                                                        Automation.Type.STATE, Automation.Type.APP -> {
                                                            if (selectedActionTab == 0) selectedInAction = null
                                                            else selectedOutAction = null
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        val actionCategories = remember(currentSelection) {
                                            val connectivityActions = listOf(
                                                Action.TurnOnWifi,
                                                Action.TurnOffWifi,
                                                Action.TurnOnCellularData,
                                                Action.TurnOffCellularData,
                                                Action.TurnOnHotspot,
                                                Action.TurnOffHotspot,
                                                Action.ToggleHotspot
                                            )
                                            val displayActions = mutableListOf<Action>(
                                                Action.TurnOnAutoBrightness,
                                                Action.TurnOffAutoBrightness,
                                                Action.DimWallpaper(),
                                                Action.ScreenOff()
                                            ).apply {
                                                if (android.os.Build.VERSION.SDK_INT >= 35) {
                                                    add(Action.DeviceEffects())
                                                }
                                            }
                                            val appsActions = listOf(
                                                Action.OpenApp(),
                                                Action.AIAssistant,
                                                Action.FreezeApps(),
                                                Action.UnfreezeApps(),
                                                Action.FreezeTag(),
                                                Action.PinApp,
                                                Action.Keyboard()
                                            )
                                            val systemActions = listOf(
                                                Action.TurnOnFlashlight,
                                                Action.TurnOffFlashlight,
                                                Action.ToggleFlashlight,
                                                Action.TurnOnLowPower,
                                                Action.TurnOffLowPower,
                                                Action.CustomSettings(),
                                                Action.CircleToSearch,
                                                Action.TakeScreenshot,
                                                Action.ShowNotification,
                                                Action.RemoveNotification
                                            )
                                            val soundMediaActions = listOf(
                                                Action.SoundMode(),
                                                Action.HapticVibration,
                                                Action.ToggleMediaVolume,
                                                Action.MediaPlayPause,
                                                Action.MediaNext,
                                                Action.MediaPrevious,
                                                Action.LikeCurrentSong
                                            )
                                            val essentialsActions = listOf(
                                                Action.SometimesEssentials()
                                            )

                                            listOf(
                                                R.string.diy_category_connectivity to connectivityActions,
                                                R.string.diy_category_display to displayActions,
                                                R.string.diy_category_apps to appsActions,
                                                R.string.diy_category_system to systemActions,
                                                R.string.diy_category_sound_media to soundMediaActions,
                                                R.string.diy_category_essentials to essentialsActions
                                            )
                                        }

                                        var expandedActionCategory by remember {
                                            mutableStateOf<Int?>(
                                                actionCategories.firstOrNull { (_, list) ->
                                                    list.any { currentSelection != null && it::class == currentSelection::class }
                                                }?.first ?: actionCategories.firstOrNull()?.first
                                            )
                                        }

                                        actionCategories.forEach { (categoryTitleRes, actions) ->
                                            CategoryExpandableSection(
                                                title = stringResource(categoryTitleRes),
                                                itemCount = actions.size,
                                                isExpanded = expandedActionCategory == categoryTitleRes,
                                                onToggleExpand = {
                                                    expandedActionCategory = if (expandedActionCategory == categoryTitleRes) null else categoryTitleRes
                                                }
                                            ) {
                                                actions.forEach { action ->
                                                    val resolvedAction = if (currentSelection != null && currentSelection::class == action::class) currentSelection else action
                                                    val missing = getMissingPermissionsHelper(resolvedAction)
                                                    fun showPermissionSheet() {
                                                        permissionKeysToShow = missing
                                                        permissionFeatureTitle = resolvedAction.title
                                                        showPermissionSheet = true
                                                    }

                                                    EditorActionItem(
                                                        title = stringResource(resolvedAction.title),
                                                        iconRes = resolvedAction.icon,
                                                        isSelected = currentSelection != null && currentSelection::class == resolvedAction::class,
                                                        isConfigurable = resolvedAction.isConfigurable,
                                                        onClick = {
                                                            when (automationType) {
                                                                Automation.Type.TRIGGER -> selectedAction = resolvedAction
                                                                Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction = resolvedAction
                                                                Automation.Type.STATE, Automation.Type.APP -> {
                                                                    if (selectedActionTab == 0) selectedInAction = resolvedAction
                                                                    else selectedOutAction = resolvedAction
                                                                }
                                                            }
                                                            if(missing.isNotEmpty()) showPermissionSheet()
                                                        },
                                                        onSettingsClick = {
                                                            if(missing.isNotEmpty()) {
                                                                showPermissionSheet()
                                                                return@EditorActionItem
                                                            }

                                                            configAction = resolvedAction
                                                            when (resolvedAction) {
                                                                is Action.DimWallpaper -> showDimSettings = true
                                                                is Action.ScreenOff -> showScreenOffSettings = true
                                                                is Action.DeviceEffects -> showDeviceEffectsSettings = true
                                                                is Action.SoundMode -> showSoundModeSettings = true
                                                                is Action.SometimesEssentials -> showSometimesEssentialsSettings = true
                                                                is Action.FreezeTag -> showFreezeTagSettings = true
                                                                is Action.OpenApp -> showOpenAppSettings = true
                                                                is Action.FreezeApps -> {
                                                                    temporarySelectedAppsForAction = resolvedAction.packageNames
                                                                    showFreezeAppsSettings = true
                                                                }
                                                                is Action.UnfreezeApps -> {
                                                                    temporarySelectedAppsForAction = resolvedAction.packageNames
                                                                    showFreezeAppsSettings = true
                                                                }
                                                                is Action.Keyboard -> {
                                                                    showSetKeyboardSheet = true
                                                                    selectedIme = resolvedAction.packageName
                                                                }
                                                                is Action.CustomSettings -> showCustomSettingsSettings = true
                                                                else -> {}
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(
                                            modifier = Modifier.height(
                                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                                            )
                                        )
                                    }
                                }

                                if (!isCurrentSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                            .clickable {
                                                HapticUtil.performUIHaptic(view)
                                                coroutineScope.launch {
                                                    carouselState.animateScrollToItem(index)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = if (index > carouselState.currentItem) {
                                                    R.drawable.rounded_chevron_forward_24
                                                } else {
                                                    R.drawable.rounded_chevron_backward_24
                                                }
                                            ),
                                            contentDescription = stringResource(R.string.action_expand),
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(36.dp)
                                         )
                                     }
                                 }
                             }
                         }

                        if (showTimeSettings) {
                            com.sameerasw.essentials.ui.core.sheets.TimeSelectionSheet(
                                initialTrigger = selectedTrigger as? Trigger.Schedule,
                                initialState = selectedState as? DIYState.TimePeriod,
                                onDismiss = { showTimeSettings = false },
                                onSaveTrigger = {
                                    selectedTrigger = it
                                    showTimeSettings = false
                                },
                                onSaveState = {
                                    selectedState = it
                                    showTimeSettings = false
                                }
                            )
                        }

                        if (showBluetoothSettings) {
                            BluetoothDeviceSelectionSheet(
                                onDismiss = { showBluetoothSettings = false },
                                onSave = { address, name ->
                                    selectedTrigger = when (selectedTrigger) {
                                        is Trigger.BluetoothConnected -> Trigger.BluetoothConnected(
                                            deviceAddress = address,
                                            deviceName = name
                                        )

                                        is Trigger.BluetoothDisconnected -> Trigger.BluetoothDisconnected(
                                            deviceAddress = address,
                                            deviceName = name
                                        )

                                        else -> selectedTrigger
                                    }
                                    showBluetoothSettings = false
                                }
                            )
                        }

                        if (showWifiSettings) {
                            WifiNetworkSelectionSheet(
                                initialSsid = when (val trigger = selectedTrigger) {
                                    is Trigger.WifiConnected -> trigger.ssid
                                    is Trigger.WifiDisconnected -> trigger.ssid
                                    else -> null
                                },
                                onDismiss = { showWifiSettings = false },
                                onSave = { ssid ->
                                    selectedTrigger = when (selectedTrigger) {
                                        is Trigger.WifiConnected -> Trigger.WifiConnected(ssid = ssid)
                                        is Trigger.WifiDisconnected -> Trigger.WifiDisconnected(ssid = ssid)
                                        else -> selectedTrigger
                                    }
                                    showWifiSettings = false
                                }
                            )
                        }

                        if (showDimSettings && configAction is Action.DimWallpaper) {
                            DimWallpaperSettingsSheet(
                                initialAction = configAction as Action.DimWallpaper,
                                onDismiss = { showDimSettings = false },
                                onSave = { newAction ->
                                    showDimSettings = false
                                    // Update the selection with configured action
                                    when (automationType) {
                                        Automation.Type.TRIGGER -> selectedAction = newAction
                                        Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction =
                                            newAction

                                        Automation.Type.STATE, Automation.Type.APP -> {
                                            if (selectedActionTab == 0) selectedInAction = newAction
                                            else selectedOutAction = newAction
                                        }
                                    }
                                    configAction = null
                                }
                            )
                        }

                        if (showScreenOffSettings && configAction is Action.ScreenOff) {
                            ScreenOffSettingsSheet(
                                initialAction = configAction as Action.ScreenOff,
                                onDismiss = { showScreenOffSettings = false },
                                onSave = { newAction ->
                                    showScreenOffSettings = false
                                    when (automationType) {
                                        Automation.Type.TRIGGER -> selectedAction = newAction
                                        Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction =
                                            newAction

                                        Automation.Type.STATE, Automation.Type.APP -> {
                                            if (selectedActionTab == 0) selectedInAction = newAction
                                            else selectedOutAction = newAction
                                        }
                                    }
                                    configAction = null
                                }
                            )
                        }

                        if (showDeviceEffectsSettings && configAction is Action.DeviceEffects) {
                            com.sameerasw.essentials.ui.core.sheets.DeviceEffectsSettingsSheet(
                                initialAction = configAction as Action.DeviceEffects,
                                onDismiss = { showDeviceEffectsSettings = false },
                                onSave = { newAction ->
                                    showDeviceEffectsSettings = false
                                    when (automationType) {
                                        Automation.Type.TRIGGER -> selectedAction = newAction
                                        Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction =
                                            newAction

                                        Automation.Type.STATE, Automation.Type.APP -> {
                                            if (selectedActionTab == 0) selectedInAction = newAction
                                            else selectedOutAction = newAction
                                        }
                                    }
                                    configAction = null
                                }
                            )
                        }

                        if (showSoundModeSettings && configAction is Action.SoundMode) {
                            SoundModeSettingsSheet(
                                initialAction = configAction as Action.SoundMode,
                                onDismiss = { showSoundModeSettings = false },
                                onSave = { newAction ->
                                    showSoundModeSettings = false
                                    when (automationType) {
                                        Automation.Type.TRIGGER -> selectedAction = newAction
                                        Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction =
                                            newAction

                                        Automation.Type.STATE, Automation.Type.APP -> {
                                            if (selectedActionTab == 0) selectedInAction = newAction
                                            else selectedOutAction = newAction
                                        }
                                    }
                                    configAction = null
                                }
                            )
                        }
                        if (showSometimesEssentialsSettings && configAction is Action.SometimesEssentials) {
                            com.sameerasw.essentials.ui.core.sheets.SometimesEssentialsSettingsSheet(
                                initialAction = configAction as Action.SometimesEssentials,
                                onDismiss = { showSometimesEssentialsSettings = false },
                                onSave = { newAction ->
                                    showSometimesEssentialsSettings = false
                                    when (automationType) {
                                        Automation.Type.TRIGGER -> selectedAction = newAction
                                        Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction =
                                            newAction

                                        Automation.Type.STATE, Automation.Type.APP -> {
                                            if (selectedActionTab == 0) selectedInAction = newAction
                                            else selectedOutAction = newAction
                                        }
                                    }
                                    configAction = null
                                }
                            )
                        }

                        if (showFreezeTagSettings && configAction is Action.FreezeTag) {
                            val availableTags = remember {
                                com.sameerasw.essentials.data.repository.SettingsRepository(context)
                                    .getFreezeTags()
                            }
                            com.sameerasw.essentials.ui.core.sheets.FreezeTagSettingsSheet(
                                initialAction = configAction as Action.FreezeTag,
                                availableTags = availableTags,
                                onDismiss = { showFreezeTagSettings = false },
                                onSave = { newAction ->
                                    showFreezeTagSettings = false
                                    when (automationType) {
                                        Automation.Type.TRIGGER -> selectedAction = newAction
                                        Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction =
                                            newAction

                                        Automation.Type.STATE, Automation.Type.APP -> {
                                            if (selectedActionTab == 0) selectedInAction = newAction
                                            else selectedOutAction = newAction
                                        }
                                    }
                                    configAction = null
                                }
                            )
                        }

                        if (showOpenAppSettings) {
                            SingleAppSelectionSheet(
                                onDismissRequest = { showOpenAppSettings = false },
                                onAppSelected = { app ->
                                    val newAction = Action.OpenApp(packageName = app.packageName)
                                    when (automationType) {
                                        Automation.Type.TRIGGER -> selectedAction = newAction
                                        Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction =
                                            newAction

                                        Automation.Type.STATE, Automation.Type.APP -> {
                                            if (selectedActionTab == 0) selectedInAction = newAction
                                            else selectedOutAction = newAction
                                        }
                                    }
                                    configAction = null
                                }
                            )
                        }

                        if (showFreezeAppsSettings && (configAction is Action.FreezeApps || configAction is Action.UnfreezeApps)) {
                            AppSelectionSheet(
                                onDismissRequest = {
                                    val finalAction = when (val action = configAction) {
                                        is Action.FreezeApps -> action.copy(packageNames = temporarySelectedAppsForAction)
                                        is Action.UnfreezeApps -> action.copy(packageNames = temporarySelectedAppsForAction)
                                        else -> configAction
                                    }
                                    if (finalAction != null) {
                                        when (automationType) {
                                            Automation.Type.TRIGGER -> selectedAction = finalAction
                                             Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction = finalAction
                                            Automation.Type.STATE, Automation.Type.APP -> {
                                                if (selectedActionTab == 0) selectedInAction = finalAction
                                                else selectedOutAction = finalAction
                                            }
                                        }
                                    }
                                    showFreezeAppsSettings = false
                                    configAction = null
                                },
                                onLoadApps = {
                                    temporarySelectedAppsForAction.map { AppSelection(it, true) }
                                },
                                onSaveApps = { _, selections ->
                                    temporarySelectedAppsForAction = selections.filter { it.isEnabled }.map { it.packageName }
                                },
                                excludePackages = if (automationType == Automation.Type.APP) selectedApps else emptyList()
                            )
                        }

                            if (showSetKeyboardSheet && configAction is Action.Keyboard) {
                                KeyboardSelectionSheet(
                                    onDismissRequest = { newIme ->
                                        showSetKeyboardSheet = false
                                        when (automationType) {
                                            Automation.Type.TRIGGER -> selectedAction = Action.Keyboard(newIme)
                                            Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction =
                                                Action.Keyboard(newIme)

                                            Automation.Type.STATE, Automation.Type.APP -> {
                                                if (selectedActionTab == 0) selectedInAction = Action.Keyboard(newIme)
                                                else selectedOutAction = Action.Keyboard(newIme)
                                            }
                                        }
                                        configAction = null
                                    },
                                    selectedIme
                                )
                            }

                        if (showCustomSettingsSettings && configAction is Action.CustomSettings) {
                            CustomSettingsSheet(
                                initialAction = configAction as Action.CustomSettings,
                                onDismiss = { showCustomSettingsSettings = false },
                                onSave = { newAction ->
                                    showCustomSettingsSettings = false
                                    when (automationType) {
                                        Automation.Type.TRIGGER -> selectedAction = newAction
                                        Automation.Type.ACTION_SHORTCUT, Automation.Type.PIXEL_SEARCHBAR -> selectedAction =
                                            newAction

                                        Automation.Type.STATE, Automation.Type.APP -> {
                                            if (selectedActionTab == 0) selectedInAction = newAction
                                            else selectedOutAction = newAction
                                        }
                                    }
                                    configAction = null
                                }
                            )
                        }
                        }

                        if (showPermissionSheet) {
                            val permissionItems = com.sameerasw.essentials.utils.PermissionUIHelper.getPermissionItems(
                                permissionKeysToShow,
                                context,
                                viewModel,
                                this@AutomationEditorActivity
                            )
                            if (permissionItems.isNotEmpty()) {
                                com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet(
                                    onDismissRequest = {
                                        showPermissionSheet = false
                                        permissionKeysToShow = emptyList()
                                    },
                                    featureTitle = permissionFeatureTitle,
                                    permissions = permissionItems
                                )
                            }
                        }

                        // Floating Bottom Toolbar
                        EssentialsFloatingToolbar(
                            title = stringResource(titleRes),
                            onBackClick = handleBackClick,
                            fabAction = if (isValid) {
                                { performSave() }
                            } else null,
                            fabIconRes = R.drawable.rounded_check_24,
                            fabContentDescription = stringResource(R.string.action_save),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorActionItem(
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    isConfigurable: Boolean = false,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                HapticUtil.performUIHaptic(view)
                onClick()
            }
            .background(
                color = MaterialTheme.colorScheme.surfaceBright,
                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isSelected && isConfigurable) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_settings_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
