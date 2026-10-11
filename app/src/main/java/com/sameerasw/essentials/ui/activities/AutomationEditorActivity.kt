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
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionRegistry
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.ui.components.CategoryExpandableSection
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.core.cards.AppToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.BluetoothDeviceSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.WifiNetworkSelectionSheet
import com.sameerasw.essentials.ui.features.system.ActionSequenceEditor
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.modifiers.scrollMotionBlur
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sameerasw.essentials.domain.diy.State as DIYState

class AutomationEditorActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_AUTOMATION_ID = "automation_id"
        private const val EXTRA_AUTOMATION_TYPE = "automation_type"

        fun createIntent(
            context: Context,
            automationId: String,
        ): Intent =
            Intent(context, AutomationEditorActivity::class.java).apply {
                putExtra(EXTRA_AUTOMATION_ID, automationId)
            }

        fun createIntent(
            context: Context,
            type: Automation.Type,
        ): Intent =
            Intent(context, AutomationEditorActivity::class.java).apply {
                putExtra(EXTRA_AUTOMATION_TYPE, type.name)
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

        val initialAutomationType =
            if (isEditMode) {
                existingAutomation.type
            } else {
                try {
                    Automation.Type.valueOf(automationTypeStr ?: Automation.Type.TRIGGER.name)
                } catch (e: Exception) {
                    Automation.Type.TRIGGER
                }
            }

        val titleRes =
            when (initialAutomationType) {
                Automation.Type.TRIGGER -> if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_editor_new_title
                Automation.Type.ACTION_SHORTCUT -> if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_editor_new_title
                Automation.Type.ACCESSIBILITY_SHORTCUT,
                Automation.Type.ACCESSIBILITY_SHORTCUT_1,
                Automation.Type.ACCESSIBILITY_SHORTCUT_2,
                Automation.Type.ACCESSIBILITY_SHORTCUT_3 -> if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_editor_new_title
                Automation.Type.PIXEL_SEARCHBAR -> if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_editor_new_title
                Automation.Type.STATE -> if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_editor_new_title
                Automation.Type.APP -> if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_create_app_title
            }

        setContent {
            val viewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
                androidx.lifecycle.viewmodel.compose
                    .viewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.check(context)
            }
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            val isMotionBlurEnabled by viewModel.isMotionBlurEnabled
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

                // The type can be changed on the first page; whatever was set up for the other types is kept until saving
                var automationType by remember { mutableStateOf(initialAutomationType) }

                // State for selections
                // Initialize with existing data or defaults
                var selectedTrigger by remember { mutableStateOf<Trigger?>(existingAutomation?.trigger) }
                var selectedState by remember { mutableStateOf<DIYState?>(existingAutomation?.state) }
                var selectedApps by remember {
                    mutableStateOf<List<String>>(
                        existingAutomation?.selectedApps ?: emptyList(),
                    )
                }

                val isAccessibilityShortcutType = remember(automationType) {
                    automationType == Automation.Type.ACCESSIBILITY_SHORTCUT ||
                    automationType == Automation.Type.ACCESSIBILITY_SHORTCUT_1 ||
                    automationType == Automation.Type.ACCESSIBILITY_SHORTCUT_2 ||
                    automationType == Automation.Type.ACCESSIBILITY_SHORTCUT_3
                }

                val existingAccessibilityAutomations = remember {
                    DIYRepository.automations.value.filter {
                        (it.id != existingAutomation?.id) && (
                            it.type == Automation.Type.ACCESSIBILITY_SHORTCUT ||
                            it.type == Automation.Type.ACCESSIBILITY_SHORTCUT_1 ||
                            it.type == Automation.Type.ACCESSIBILITY_SHORTCUT_2 ||
                            it.type == Automation.Type.ACCESSIBILITY_SHORTCUT_3
                        )
                    }
                }

                val usedAccessibilitySlots = remember(existingAccessibilityAutomations) {
                    existingAccessibilityAutomations.map {
                        when (it.type) {
                            Automation.Type.ACCESSIBILITY_SHORTCUT, Automation.Type.ACCESSIBILITY_SHORTCUT_1 -> 1
                            Automation.Type.ACCESSIBILITY_SHORTCUT_2 -> 2
                            Automation.Type.ACCESSIBILITY_SHORTCUT_3 -> 3
                            else -> 1
                        }
                    }.toSet()
                }

                val otherAutomations = remember { DIYRepository.automations.value.filter { it.id != existingAutomation?.id } }
                val isPixelSearchbarEnabled = remember { SettingsRepository(context).getBoolean(SettingsRepository.KEY_PIXEL_SEARCHBAR, false) }
                val availableTypes =
                    remember(usedAccessibilitySlots) {
                        buildList {
                            add(Automation.Type.TRIGGER)
                            add(Automation.Type.STATE)
                            add(Automation.Type.APP)
                            if (otherAutomations.none { it.type == Automation.Type.ACTION_SHORTCUT }) add(Automation.Type.ACTION_SHORTCUT)
                            if (usedAccessibilitySlots.size < 3) add(Automation.Type.ACCESSIBILITY_SHORTCUT_1)
                            if ((isPixelSearchbarEnabled || initialAutomationType == Automation.Type.PIXEL_SEARCHBAR) &&
                                otherAutomations.none { it.type == Automation.Type.PIXEL_SEARCHBAR }
                            ) {
                                add(Automation.Type.PIXEL_SEARCHBAR)
                            }
                        }
                    }

                var selectedAccessibilitySlot by remember {
                    val initialSlot = if (isEditMode) {
                        when (existingAutomation.type) {
                            Automation.Type.ACCESSIBILITY_SHORTCUT, Automation.Type.ACCESSIBILITY_SHORTCUT_1 -> 1
                            Automation.Type.ACCESSIBILITY_SHORTCUT_2 -> 2
                            Automation.Type.ACCESSIBILITY_SHORTCUT_3 -> 3
                            else -> 1
                        }
                    } else {
                        (1..3).firstOrNull { it !in usedAccessibilitySlots } ?: 1
                    }
                    mutableStateOf(initialSlot)
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
                                val installed = AppUtil.getInstalledApps(context, includeSelf = true)
                                // Merge with selection if existing
                                val merged =
                                    AppUtil.mergeWithSavedApps(
                                        installed,
                                        selectedApps.map { AppSelection(it, true) },
                                    )
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

                val filteredApps =
                    remember(allApps, searchQuery, showSystemApps, selectedApps) {
                        allApps
                            .filter {
                                val matchesSearch =
                                    searchQuery.isEmpty() ||
                                        it.appName.contains(
                                            searchQuery,
                                            ignoreCase = true,
                                        )
                                val isVisible =
                                    !it.isSystemApp || showSystemApps || selectedApps.contains(it.packageName)
                                matchesSearch && isVisible
                            }.sortedWith(
                                compareByDescending<NotificationApp> {
                                    selectedApps.contains(
                                        it.packageName,
                                    )
                                }.thenBy { it.appName.lowercase() },
                            )
                    }

                // Actions run in order: one list for triggers and shortcuts, in and out lists for states and apps
                var selectedActions by remember { mutableStateOf(existingAutomation?.actionList.orEmpty()) }
                var selectedInActions by remember { mutableStateOf(existingAutomation?.entryActionList.orEmpty()) }
                var selectedOutActions by remember { mutableStateOf(existingAutomation?.exitActionList.orEmpty()) }

                fun selectAutomationType(type: Automation.Type) {
                    HapticUtil.performUIHaptic(view)
                    if (type == Automation.Type.ACCESSIBILITY_SHORTCUT_1 && selectedAccessibilitySlot in usedAccessibilitySlots) {
                        selectedAccessibilitySlot = (1..3).first { it !in usedAccessibilitySlots }
                    }
                    // Carry the actions across when moving between one sequence and separate in and out sequences
                    val isInOut = { t: Automation.Type -> t == Automation.Type.STATE || t == Automation.Type.APP }
                    if (isInOut(type) && !isInOut(automationType) && selectedInActions.isEmpty()) {
                        selectedInActions = selectedActions
                    } else if (!isInOut(type) && isInOut(automationType) && selectedActions.isEmpty()) {
                        selectedActions = selectedInActions
                    }
                    automationType = type
                }

                // Tab for State Actions
                var selectedActionTab by remember { mutableIntStateOf(0) } // 0: In, 1: Out

                // Menu State
                var showMenu by remember { mutableStateOf(false) }

                // Config Sheets
                var showTimeSettings by remember { mutableStateOf(false) }
                var showCalendarStateSettings by remember { mutableStateOf(false) }
                var showBatteryLevelSettings by remember { mutableStateOf(false) }
                var showBluetoothSettings by remember { mutableStateOf(false) }
                var showWifiSettings by remember { mutableStateOf(false) }

                val isTriggerConfigured =
                    when (val trigger = selectedTrigger) {
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
                    val observer =
                        LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                viewModel.check(context)
                            }
                        }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                fun isActionConfigured(action: Action): Boolean =
                    when (action) {
                        is Action.OpenApp -> action.packageName.isNotBlank()
                        is Action.OpenActivity -> action.className.isNotBlank()
                        is Action.CustomSettings -> action.entries.isNotEmpty()
                        is Action.Keyboard -> !action.inputMethodId.isNullOrBlank()
                        else -> true
                    }

                // Validation
                val isValid =
                    when (automationType) {
                        Automation.Type.TRIGGER ->
                            selectedTrigger != null &&
                                selectedActions.isNotEmpty() &&
                                isTriggerConfigured &&
                                selectedActions.all(::isActionConfigured)

                        Automation.Type.ACTION_SHORTCUT,
                        Automation.Type.ACCESSIBILITY_SHORTCUT,
                        Automation.Type.ACCESSIBILITY_SHORTCUT_1,
                        Automation.Type.ACCESSIBILITY_SHORTCUT_2,
                        Automation.Type.ACCESSIBILITY_SHORTCUT_3,
                        Automation.Type.PIXEL_SEARCHBAR ->
                            selectedActions.isNotEmpty() && selectedActions.all(::isActionConfigured)

                        Automation.Type.STATE ->
                            selectedState != null &&
                                (selectedInActions + selectedOutActions).let { it.isNotEmpty() && it.all(::isActionConfigured) }

                        Automation.Type.APP ->
                            selectedApps.isNotEmpty() &&
                                (selectedInActions + selectedOutActions).let { it.isNotEmpty() && it.all(::isActionConfigured) }
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
                                },
                            ) {
                                Text(
                                    text = stringResource(R.string.translation_discard),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    showDiscardDialog = false
                                },
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        },
                    )
                }

                fun getMissingPermissionsHelper(action: Action?): List<String> {
                    if (action == null) return emptyList()
                    val resolvedPermissions =
                        action.permissions
                            .map { permKey ->
                                if (permKey == "SHIZUKU" || permKey == "ROOT") {
                                    if (com.sameerasw.essentials.utils.ShellUtils
                                            .isRootEnabled(context)
                                    ) {
                                        "ROOT"
                                    } else {
                                        "SHIZUKU"
                                    }
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
                            "DRAW_OVERLAYS" -> !viewModel.isOverlayPermissionGranted.value
                            "ACCESSIBILITY" -> !viewModel.isAccessibilityEnabled.value
                            else -> false
                        }
                    }
                }

                val performSave = {
                    val actionsToCheck =
                        when (automationType) {
                            Automation.Type.TRIGGER,
                            Automation.Type.ACTION_SHORTCUT,
                            Automation.Type.ACCESSIBILITY_SHORTCUT,
                            Automation.Type.ACCESSIBILITY_SHORTCUT_1,
                            Automation.Type.ACCESSIBILITY_SHORTCUT_2,
                            Automation.Type.ACCESSIBILITY_SHORTCUT_3,
                            Automation.Type.PIXEL_SEARCHBAR -> selectedActions
                            else -> selectedInActions + selectedOutActions
                        }
                    val allMissingPermissions = actionsToCheck.flatMap { getMissingPermissionsHelper(it) }.distinct()
                    if (allMissingPermissions.isNotEmpty()) {
                        permissionKeysToShow = allMissingPermissions
                        permissionFeatureTitle = R.string.tab_diy
                        showPermissionSheet = true
                    } else {
                        if (automationType == Automation.Type.TRIGGER) {
                            val newAutomation =
                                Automation(
                                    id =
                                        if (isEditMode) {
                                            existingAutomation.id
                                        } else {
                                            java.util.UUID
                                                .randomUUID()
                                                .toString()
                                        },
                                    isEnabled = existingAutomation?.isEnabled ?: true,
                                    type = Automation.Type.TRIGGER,
                                    trigger = selectedTrigger,
                                    actions = selectedActions,
                                )
                            if (isEditMode) DIYRepository.updateAutomation(newAutomation) else DIYRepository.addAutomation(newAutomation)
                        } else if (isAccessibilityShortcutType) {
                            val savedType = when (selectedAccessibilitySlot) {
                                1 -> Automation.Type.ACCESSIBILITY_SHORTCUT_1
                                2 -> Automation.Type.ACCESSIBILITY_SHORTCUT_2
                                3 -> Automation.Type.ACCESSIBILITY_SHORTCUT_3
                                else -> Automation.Type.ACCESSIBILITY_SHORTCUT_1
                            }
                            val newAutomation =
                                Automation(
                                    id =
                                        if (isEditMode) {
                                            existingAutomation.id
                                        } else {
                                            java.util.UUID
                                                .randomUUID()
                                                .toString()
                                        },
                                    isEnabled = existingAutomation?.isEnabled ?: true,
                                    type = savedType,
                                    actions = selectedActions,
                                )
                            if (isEditMode) DIYRepository.updateAutomation(newAutomation) else DIYRepository.addAutomation(newAutomation)
                        } else if (
                            automationType == Automation.Type.ACTION_SHORTCUT ||
                            automationType == Automation.Type.PIXEL_SEARCHBAR
                        ) {
                            val newAutomation =
                                Automation(
                                    id =
                                        if (isEditMode) {
                                            existingAutomation.id
                                        } else {
                                            java.util.UUID
                                                .randomUUID()
                                                .toString()
                                        },
                                    isEnabled = existingAutomation?.isEnabled ?: true,
                                    type = automationType,
                                    actions = selectedActions,
                                )
                            if (isEditMode) DIYRepository.updateAutomation(newAutomation) else DIYRepository.addAutomation(newAutomation)
                        } else if (automationType == Automation.Type.STATE) {
                            val newAutomation =
                                Automation(
                                    id =
                                        if (isEditMode) {
                                            existingAutomation.id
                                        } else {
                                            java.util.UUID
                                                .randomUUID()
                                                .toString()
                                        },
                                    isEnabled = existingAutomation?.isEnabled ?: true,
                                    type = Automation.Type.STATE,
                                    state = selectedState,
                                    entryActions = selectedInActions,
                                    exitActions = selectedOutActions,
                                )
                            if (isEditMode) DIYRepository.updateAutomation(newAutomation) else DIYRepository.addAutomation(newAutomation)
                        } else if (automationType == Automation.Type.APP) {
                            val newAutomation =
                                Automation(
                                    id =
                                        if (isEditMode) {
                                            existingAutomation.id
                                        } else {
                                            java.util.UUID
                                                .randomUUID()
                                                .toString()
                                        },
                                    isEnabled = existingAutomation?.isEnabled ?: true,
                                    type = Automation.Type.APP,
                                    selectedApps = selectedApps,
                                    entryActions = selectedInActions,
                                    exitActions = selectedOutActions,
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
                    val statusBarHeightPx =
                        with(density) {
                            WindowInsets.statusBars
                                .asPaddingValues()
                                .calculateTopPadding()
                                .toPx()
                        }
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .progressiveBlur(
                                    blurRadius = if (isBlurEnabled) 40f else 0f,
                                    height = statusBarHeightPx * 1.15f,
                                    direction = BlurDirection.TOP,
                                ),
                    ) {
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp

                        // Haptic Connection for Swipe Texture
                        val nestedScrollConnection =
                            remember {
                                object : NestedScrollConnection {
                                    var accumulatedScroll = 0f
                                    val threshold = 40f

                                    override fun onPreScroll(
                                        available: Offset,
                                        source: NestedScrollSource,
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
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .progressiveBlur(
                                        blurRadius = if (isBlurEnabled) 40f else 0f,
                                        height = with(density) { 150.dp.toPx() },
                                        direction = BlurDirection.BOTTOM,
                                    ),
                        ) {
                            HorizontalMultiBrowseCarousel(
                                state = carouselState,
                                preferredItemWidth = screenWidth,
                                itemSpacing = 4.dp,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .nestedScroll(nestedScrollConnection),
                                contentPadding = PaddingValues(horizontal = 18.dp),
                            ) { index ->
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.extraLarge)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                ) {
                                    val isCurrentSelected = carouselState.currentItem == index

                                    if (index == 0) {
                                        // PAGE 0: Trigger or State Picker
                                        if (automationType == Automation.Type.APP) {
                                            Column(
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                            ) {
                                                Spacer(modifier = Modifier.height(statusBarHeight + 4.dp))
                                                Text(
                                                    text = stringResource(R.string.diy_create_app_title),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                )

                                                AutomationTypePicker(
                                                    selected = automationType,
                                                    available = availableTypes,
                                                    onSelected = ::selectAutomationType,
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
                                                            contentDescription = stringResource(R.string.action_search),
                                                        )
                                                    },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                )

                                                // System Apps Toggle
                                                Row(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .clickable {
                                                                HapticUtil.performVirtualKeyHaptic(view)
                                                                showSystemApps = !showSystemApps
                                                            }.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.rounded_settings_24),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.toggle_show_system_apps),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        modifier = Modifier.weight(1f),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                    )
                                                    Switch(
                                                        checked = showSystemApps,
                                                        onCheckedChange = {
                                                            HapticUtil.performVirtualKeyHaptic(view)
                                                            showSystemApps = it
                                                        },
                                                    )
                                                }

                                                if (isLoadingApps) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        LoadingIndicator()
                                                    }
                                                } else {
                                                    val appsLazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
                                                    LazyColumn(
                                                        state = appsLazyListState,
                                                        modifier =
                                                            Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(24.dp))
                                                                .scrollMotionBlur(appsLazyListState, enabled = isMotionBlurEnabled),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                                        contentPadding =
                                                            PaddingValues(
                                                                bottom =
                                                                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                                                        80.dp,
                                                             ),
                                                    ) {
                                                        items(
                                                            filteredApps,
                                                            key = { it.packageName },
                                                        ) { app ->
                                                            val isSelected =
                                                                selectedApps.contains(app.packageName)
                                                            AppToggleItem(
                                                                icon = app.icon,
                                                                title = app.appName,
                                                                isChecked = isSelected,
                                                                onCheckedChange = { isChecked ->
                                                                    val current =
                                                                        selectedApps.toMutableList()
                                                                    if (isChecked) {
                                                                        current.add(app.packageName)
                                                                    } else {
                                                                        current.remove(
                                                                            app.packageName,
                                                                        )
                                                                    }
                                                                    selectedApps = current
                                                                },
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else if (isAccessibilityShortcutType) {
                                            val triggerScrollState = rememberScrollState()
                                            Column(
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .scrollMotionBlur(triggerScrollState, enabled = isMotionBlurEnabled)
                                                        .verticalScroll(triggerScrollState)
                                                        .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                            ) {
                                                Spacer(modifier = Modifier.height(statusBarHeight + 4.dp))
                                                Text(
                                                    text = stringResource(R.string.diy_select_trigger),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                )

                                                AutomationTypePicker(
                                                    selected = automationType,
                                                    available = availableTypes,
                                                    onSelected = ::selectAutomationType,
                                                )

                                                RoundedCardContainer(spacing = 2.dp) {
                                                    val slots = listOf(1, 2, 3)
                                                    slots.forEach { slot ->
                                                        val title = when (slot) {
                                                            1 -> stringResource(R.string.diy_create_accessibility_shortcut_1_title)
                                                            2 -> stringResource(R.string.diy_create_accessibility_shortcut_2_title)
                                                            3 -> stringResource(R.string.diy_create_accessibility_shortcut_3_title)
                                                            else -> ""
                                                        }
                                                        val slotIcon = when (slot) {
                                                            1 -> R.drawable.rounded_circle_24
                                                            2 -> R.drawable.rounded_pentagon_24
                                                            3 -> R.drawable.rounded_square_24
                                                            else -> R.drawable.rounded_circle_24
                                                        }
                                                        val isSelected = selectedAccessibilitySlot == slot
                                                        val isEnabled = slot !in usedAccessibilitySlots

                                                        EditorActionItem(
                                                            title = title,
                                                            iconRes = slotIcon,
                                                            isSelected = isSelected,
                                                            enabled = isEnabled,
                                                            isConfigurable = false,
                                                            onClick = {
                                                                if (!PermissionUtils.isAccessibilityShortcutServiceEnabled(context, slot)) {
                                                                    val promptRes = when (slot) {
                                                                        1 -> R.string.diy_enable_accessibility_shortcut_1_prompt
                                                                        2 -> R.string.diy_enable_accessibility_shortcut_2_prompt
                                                                        3 -> R.string.diy_enable_accessibility_shortcut_3_prompt
                                                                        else -> R.string.diy_enable_accessibility_shortcut_1_prompt
                                                                    }
                                                                    Toast.makeText(context, context.getString(promptRes), Toast.LENGTH_LONG).show()
                                                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                                    }
                                                                    context.startActivity(intent)
                                                                }
                                                                selectedAccessibilitySlot = slot
                                                            },
                                                        )
                                                    }
                                                }
                                                Spacer(
                                                    modifier =
                                                        Modifier.height(
                                                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp,
                                                        ),
                                                )
                                            }
                                        } else if (
                                            automationType == Automation.Type.ACTION_SHORTCUT ||
                                            automationType == Automation.Type.PIXEL_SEARCHBAR
                                        ) {
                                            val triggerScrollState = rememberScrollState()
                                            Column(
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .scrollMotionBlur(triggerScrollState, enabled = isMotionBlurEnabled)
                                                        .verticalScroll(triggerScrollState)
                                                        .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                            ) {
                                                Spacer(modifier = Modifier.height(statusBarHeight + 4.dp))
                                                Text(
                                                    text = stringResource(R.string.diy_select_trigger),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                )

                                                AutomationTypePicker(
                                                    selected = automationType,
                                                    available = availableTypes,
                                                    onSelected = ::selectAutomationType,
                                                )

                                                RoundedCardContainer(spacing = 2.dp) {
                                                    val editorTitle =
                                                        when (automationType) {
                                                            Automation.Type.PIXEL_SEARCHBAR -> stringResource(R.string.diy_create_pixel_searchbar_title)
                                                            else -> stringResource(R.string.diy_create_action_shortcut_title)
                                                        }
                                                    val editorIcon =
                                                        when (automationType) {
                                                            Automation.Type.PIXEL_SEARCHBAR -> R.drawable.rounded_search_24
                                                            else -> R.drawable.rounded_rocket_launch_24
                                                        }
                                                    EditorActionItem(
                                                        title = editorTitle,
                                                        iconRes = editorIcon,
                                                        isSelected = true,
                                                        isConfigurable = false,
                                                        onClick = {},
                                                    )
                                                }
                                                Spacer(
                                                    modifier =
                                                        Modifier.height(
                                                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp,
                                                        ),
                                                )
                                            }
                                        } else {
                                            val triggerStateScrollState = rememberScrollState()
                                            Column(
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .scrollMotionBlur(triggerStateScrollState, enabled = isMotionBlurEnabled)
                                                        .verticalScroll(triggerStateScrollState)
                                                        .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                            ) {
                                                Spacer(modifier = Modifier.height(statusBarHeight + 4.dp))
                                                Text(
                                                    text =
                                                        stringResource(
                                                            if (automationType ==
                                                                Automation.Type.TRIGGER
                                                            ) {
                                                                R.string.diy_select_trigger
                                                            } else {
                                                                R.string.diy_select_state
                                                            },
                                                        ),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                )

                                                AutomationTypePicker(
                                                    selected = automationType,
                                                    available = availableTypes,
                                                    onSelected = ::selectAutomationType,
                                                )

                                                if (automationType == Automation.Type.TRIGGER) {
                                                    val triggerCategories =
                                                        remember(selectedTrigger) {
                                                            listOf(
                                                                R.string.diy_category_system_screen to
                                                                    listOf(
                                                                        Trigger.ScreenOff,
                                                                        Trigger.ScreenOn,
                                                                        Trigger.DeviceUnlock,
                                                                    ),
                                                                R.string.diy_category_battery_power to
                                                                    listOf(
                                                                        Trigger.ChargerConnected,
                                                                        Trigger.ChargerDisconnected,
                                                                        Trigger.PowerSavingOn,
                                                                        Trigger.PowerSavingOff,
                                                                    ),
                                                                R.string.diy_category_connectivity to
                                                                    listOf(
                                                                        Trigger.BluetoothConnected(
                                                                            deviceAddress =
                                                                                (selectedTrigger as? Trigger.BluetoothConnected)?.deviceAddress
                                                                                    ?: "",
                                                                            deviceName =
                                                                                (selectedTrigger as? Trigger.BluetoothConnected)?.deviceName
                                                                                    ?: "",
                                                                        ),
                                                                        Trigger.BluetoothDisconnected(
                                                                            deviceAddress =
                                                                                (selectedTrigger as? Trigger.BluetoothDisconnected)?.deviceAddress
                                                                                    ?: "",
                                                                            deviceName =
                                                                                (selectedTrigger as? Trigger.BluetoothDisconnected)?.deviceName
                                                                                    ?: "",
                                                                        ),
                                                                        Trigger.WifiConnected(
                                                                            ssid = (selectedTrigger as? Trigger.WifiConnected)?.ssid ?: "",
                                                                        ),
                                                                        Trigger.WifiDisconnected(
                                                                            ssid =
                                                                                (selectedTrigger as? Trigger.WifiDisconnected)?.ssid ?: "",
                                                                        ),
                                                                    ),
                                                                R.string.diy_category_time_schedule to
                                                                    listOf(
                                                                        Trigger.Schedule(
                                                                            hour = (selectedTrigger as? Trigger.Schedule)?.hour ?: 0,
                                                                            minute = (selectedTrigger as? Trigger.Schedule)?.minute ?: 0,
                                                                            days =
                                                                                (selectedTrigger as? Trigger.Schedule)?.days ?: emptySet(),
                                                                        ),
                                                                    ),
                                                            )
                                                        }

                                                    var expandedTriggerCategory by remember {
                                                        mutableStateOf<Int?>(
                                                            triggerCategories
                                                                .firstOrNull { (_, list) ->
                                                                    list.any {
                                                                        selectedTrigger != null &&
                                                                            it::class == selectedTrigger!!::class
                                                                    }
                                                                }?.first ?: triggerCategories.firstOrNull()?.first,
                                                        )
                                                    }

                                                    triggerCategories.forEach { (categoryTitleRes, triggerList) ->
                                                        CategoryExpandableSection(
                                                            title = stringResource(categoryTitleRes),
                                                            itemCount = triggerList.size,
                                                            isExpanded = expandedTriggerCategory == categoryTitleRes,
                                                            onToggleExpand = {
                                                                expandedTriggerCategory =
                                                                    if (expandedTriggerCategory ==
                                                                        categoryTitleRes
                                                                    ) {
                                                                        null
                                                                    } else {
                                                                        categoryTitleRes
                                                                    }
                                                            },
                                                        ) {
                                                            triggerList.forEach { trigger ->
                                                                val isSelected =
                                                                    selectedTrigger != null && selectedTrigger!!::class == trigger::class
                                                                EditorActionItem(
                                                                    title = stringResource(trigger.title),
                                                                    iconRes = trigger.icon,
                                                                    isSelected = isSelected,
                                                                    isConfigurable = trigger.isConfigurable,
                                                                    onClick = { selectedTrigger = trigger },
                                                                    onSettingsClick = {
                                                                        when (trigger) {
                                                                            is Trigger.Schedule -> showTimeSettings = true
                                                                            is Trigger.BluetoothConnected, is Trigger.BluetoothDisconnected ->
                                                                                showBluetoothSettings =
                                                                                    true
                                                                            is Trigger.WifiConnected, is Trigger.WifiDisconnected ->
                                                                                showWifiSettings =
                                                                                    true
                                                                            else -> {}
                                                                        }
                                                                    },
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    val stateCategories =
                                                        remember(selectedState) {
                                                            listOf(
                                                                R.string.diy_category_battery_power to
                                                                    listOf(
                                                                        DIYState.Charging,
                                                                        DIYState.PowerSaving,
                                                                        (selectedState as? DIYState.BatteryLevel) ?: DIYState.BatteryLevel(),
                                                                    ),
                                                                R.string.diy_category_system_screen to
                                                                    listOf(
                                                                        DIYState.ScreenOn,
                                                                    ),
                                                                R.string.diy_category_time_schedule to
                                                                    listOf(
                                                                        DIYState.TimePeriod(
                                                                            startHour =
                                                                                (selectedState as? DIYState.TimePeriod)?.startHour ?: 0,
                                                                            startMinute =
                                                                                (selectedState as? DIYState.TimePeriod)?.startMinute ?: 0,
                                                                            endHour = (selectedState as? DIYState.TimePeriod)?.endHour ?: 0,
                                                                            endMinute =
                                                                                (selectedState as? DIYState.TimePeriod)?.endMinute ?: 0,
                                                                            days =
                                                                                (selectedState as? DIYState.TimePeriod)?.days ?: emptySet(),
                                                                        ),
                                                                    ),
                                                                R.string.diy_category_calendar to
                                                                    listOf(
                                                                        (selectedState as? DIYState.CalendarEvent) ?: DIYState.CalendarEvent(),
                                                                    ),
                                                            )
                                                        }

                                                    var expandedStateCategory by remember {
                                                        mutableStateOf<Int?>(
                                                            stateCategories
                                                                .firstOrNull { (_, list) ->
                                                                    list.any {
                                                                        selectedState != null && it::class == selectedState!!::class
                                                                    }
                                                                }?.first ?: stateCategories.firstOrNull()?.first,
                                                        )
                                                    }

                                                    stateCategories.forEach { (categoryTitleRes, stateList) ->
                                                        CategoryExpandableSection(
                                                            title = stringResource(categoryTitleRes),
                                                            itemCount = stateList.size,
                                                            isExpanded = expandedStateCategory == categoryTitleRes,
                                                            onToggleExpand = {
                                                                expandedStateCategory =
                                                                    if (expandedStateCategory ==
                                                                        categoryTitleRes
                                                                    ) {
                                                                        null
                                                                    } else {
                                                                        categoryTitleRes
                                                                    }
                                                            },
                                                        ) {
                                                            stateList.forEach { state ->
                                                                val isSelected =
                                                                    selectedState != null && selectedState!!::class == state::class
                                                                EditorActionItem(
                                                                    title = stringResource(state.title),
                                                                    iconRes = state.icon,
                                                                    isSelected = isSelected,
                                                                    onClick = { selectedState = state },
                                                                    isConfigurable = state is DIYState.TimePeriod || state is DIYState.CalendarEvent || state is DIYState.BatteryLevel,
                                                                    onSettingsClick = {
                                                                        if (state is DIYState.TimePeriod) {
                                                                            showTimeSettings = true
                                                                        }
                                                                        if (state is DIYState.BatteryLevel) {
                                                                            selectedState = state
                                                                            showBatteryLevelSettings = true
                                                                        }
                                                                        if (state is DIYState.CalendarEvent) {
                                                                            selectedState = state
                                                                            showCalendarStateSettings = true
                                                                        }
                                                                    },
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                Spacer(
                                                    modifier =
                                                        Modifier.height(
                                                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp,
                                                        ),
                                                )
                                            }
                                        }
                                    } else {
                                        // PAGE 1: Action Picker
                                        val actionScrollState = rememberScrollState()
                                        Column(
                                            modifier =
                                                Modifier
                                                    .fillMaxSize()
                                                    .scrollMotionBlur(actionScrollState, enabled = isMotionBlurEnabled)
                                                    .verticalScroll(actionScrollState)
                                                    .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp),
                                        ) {
                                            Spacer(modifier = Modifier.height(statusBarHeight + 4.dp))
                                            Text(
                                                text = stringResource(R.string.diy_select_action),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                            )

                                            if (automationType == Automation.Type.STATE || automationType == Automation.Type.APP) {
                                                // Tabs for In/Out
                                                val options =
                                                    listOf(
                                                        stringResource(R.string.diy_in_action_label),
                                                        stringResource(R.string.diy_out_action_label),
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
                                                    cornerShape = MaterialTheme.shapes.extraExtraLarge.bottomEnd,
                                                )
                                            }

                                            val isInOut = automationType == Automation.Type.STATE || automationType == Automation.Type.APP
                                            ActionSequenceEditor(
                                                viewModel = viewModel,
                                                actions =
                                                    when {
                                                        !isInOut -> selectedActions
                                                        selectedActionTab == 0 -> selectedInActions
                                                        else -> selectedOutActions
                                                    },
                                                onActionsChange = {
                                                    when {
                                                        !isInOut -> selectedActions = it
                                                        selectedActionTab == 0 -> selectedInActions = it
                                                        else -> selectedOutActions = it
                                                    }
                                                },
                                                screenOnOnly = false,
                                                listKey = if (isInOut) selectedActionTab else automationType,
                                                emptyText = stringResource(R.string.diy_no_actions),
                                                categories = remember { ActionRegistry.getCategories() },
                                            )
                                            Spacer(
                                                modifier =
                                                    Modifier.height(
                                                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp,
                                                    ),
                                            )
                                        }
                                    }

                                    if (!isCurrentSelected) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                                    .clickable {
                                                        HapticUtil.performUIHaptic(view)
                                                        coroutineScope.launch {
                                                            carouselState.animateScrollToItem(index)
                                                        }
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        id =
                                                            if (index > carouselState.currentItem) {
                                                                R.drawable.rounded_chevron_forward_24
                                                            } else {
                                                                R.drawable.rounded_chevron_backward_24
                                                            },
                                                    ),
                                                contentDescription = stringResource(R.string.action_expand),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(36.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            if (showBatteryLevelSettings) {
                                com.sameerasw.essentials.ui.core.sheets.BatteryLevelStateSheet(
                                    initialState = selectedState as? DIYState.BatteryLevel,
                                    onDismiss = { showBatteryLevelSettings = false },
                                    onStateChange = { selectedState = it },
                                )
                            }

                            if (showCalendarStateSettings) {
                                com.sameerasw.essentials.ui.core.sheets.CalendarStateSheet(
                                    initialState = selectedState as? DIYState.CalendarEvent,
                                    onDismiss = { showCalendarStateSettings = false },
                                    onStateChange = { selectedState = it },
                                )
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
                                    },
                                )
                            }

                            if (showBluetoothSettings) {
                                BluetoothDeviceSelectionSheet(
                                    onDismiss = { showBluetoothSettings = false },
                                    onSave = { address, name ->
                                        selectedTrigger =
                                            when (selectedTrigger) {
                                                is Trigger.BluetoothConnected ->
                                                    Trigger.BluetoothConnected(
                                                        deviceAddress = address,
                                                        deviceName = name,
                                                    )

                                                is Trigger.BluetoothDisconnected ->
                                                    Trigger.BluetoothDisconnected(
                                                        deviceAddress = address,
                                                        deviceName = name,
                                                    )

                                                else -> selectedTrigger
                                            }
                                        showBluetoothSettings = false
                                    },
                                )
                            }

                            if (showWifiSettings) {
                                WifiNetworkSelectionSheet(
                                    initialSsid =
                                        when (val trigger = selectedTrigger) {
                                            is Trigger.WifiConnected -> trigger.ssid
                                            is Trigger.WifiDisconnected -> trigger.ssid
                                            else -> null
                                        },
                                    onDismiss = { showWifiSettings = false },
                                    onSave = { ssid ->
                                        selectedTrigger =
                                            when (selectedTrigger) {
                                                is Trigger.WifiConnected -> Trigger.WifiConnected(ssid = ssid)
                                                is Trigger.WifiDisconnected -> Trigger.WifiDisconnected(ssid = ssid)
                                                else -> selectedTrigger
                                            }
                                        showWifiSettings = false
                                    },
                                )
                            }
                        }

                        if (showPermissionSheet) {
                            val permissionItems =
                                com.sameerasw.essentials.utils.PermissionUIHelper.getPermissionItems(
                                    permissionKeysToShow,
                                    context,
                                    viewModel,
                                    this@AutomationEditorActivity,
                                )
                            if (permissionItems.isNotEmpty()) {
                                com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet(
                                    onDismissRequest = {
                                        showPermissionSheet = false
                                        permissionKeysToShow = emptyList()
                                    },
                                    featureTitle = permissionFeatureTitle,
                                    permissions = permissionItems,
                                )
                            }
                        }

                        // Floating Bottom Toolbar
                        EssentialsFloatingToolbar(
                            title = stringResource(titleRes),
                            onBackClick = handleBackClick,
                            fabAction =
                                if (isValid) {
                                    { performSave() }
                                } else {
                                    null
                                },
                            fabIconRes = R.drawable.rounded_check_24,
                            fabContentDescription = stringResource(R.string.action_save),
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .zIndex(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomationTypePicker(
    selected: Automation.Type,
    available: List<Automation.Type>,
    onSelected: (Automation.Type) -> Unit,
) {
    val isAccessibility: (Automation.Type) -> Boolean = {
        it == Automation.Type.ACCESSIBILITY_SHORTCUT ||
            it == Automation.Type.ACCESSIBILITY_SHORTCUT_1 ||
            it == Automation.Type.ACCESSIBILITY_SHORTCUT_2 ||
            it == Automation.Type.ACCESSIBILITY_SHORTCUT_3
    }
    val options =
        listOf(
            Automation.Type.TRIGGER to R.string.diy_type_trigger,
            Automation.Type.STATE to R.string.diy_type_state,
            Automation.Type.APP to R.string.diy_type_app,
            Automation.Type.ACTION_SHORTCUT to R.string.diy_type_action_shortcut,
            Automation.Type.ACCESSIBILITY_SHORTCUT_1 to R.string.diy_type_accessibility_shortcut,
            Automation.Type.PIXEL_SEARCHBAR to R.string.diy_type_pixel_searchbar,
        )
    RoundedCardContainer {
        FlowRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (type, label) ->
                val isSelected = type == selected || (isAccessibility(type) && isAccessibility(selected))
                // Types already used by another automation stay visible but can't be picked, as in the new automation sheet
                if (type in available || isSelected) {
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (!isSelected) onSelected(type) },
                        label = { Text(stringResource(label)) },
                    )
                } else if (type != Automation.Type.PIXEL_SEARCHBAR) {
                    FilterChip(
                        selected = false,
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(label)) },
                    )
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
    enabled: Boolean = true,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                ).clickable {
                    HapticUtil.performUIHaptic(view)
                    onClick()
                }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = isSelected,
            onClick = if (enabled) onClick else null,
            enabled = enabled,
        )

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                isSelected -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        if (isSelected && isConfigurable && enabled) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_settings_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
