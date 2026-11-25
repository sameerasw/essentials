/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeEditorActivity.kt
 * Description: Creates and edits Essentials modes, and opens from the system mode settings page.
 */

package com.sameerasw.essentials.ui.activities

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.translation.TranslatableText
import androidx.compose.ui.platform.LocalView
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionRegistry
import com.sameerasw.essentials.domain.model.AppPermission
import com.sameerasw.essentials.domain.modes.Mode
import com.sameerasw.essentials.domain.modes.ModeAutomation
import com.sameerasw.essentials.domain.modes.ModeIcons
import com.sameerasw.essentials.domain.modes.ModesController
import com.sameerasw.essentials.domain.modes.ModesRepository
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PRESET_PASTEL_COLORS
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.features.modes.ModeAppearanceFields
import com.sameerasw.essentials.ui.features.modes.findActivity
import com.sameerasw.essentials.ui.features.modes.ModeConditionsSection
import androidx.compose.runtime.saveable.rememberSaveable
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.features.modes.ModeEditorHeader
import com.sameerasw.essentials.ui.features.modes.ModeCollapsibleSection
import com.sameerasw.essentials.ui.features.modes.ModeExitTripRow
import com.sameerasw.essentials.ui.features.modes.ModeFeaturesSection
import com.sameerasw.essentials.ui.features.modes.ModeFreezeSection
import com.sameerasw.essentials.ui.features.modes.SystemModePickerSheet
import com.sameerasw.essentials.ui.features.modes.rememberModeIconPainter
import com.sameerasw.essentials.ui.features.system.ActionSequenceEditor
import com.sameerasw.essentials.ui.navigation.AppRoute
import com.sameerasw.essentials.ui.navigation.AppRoutes
import com.sameerasw.essentials.ui.navigation.BottomBarSpec
import com.sameerasw.essentials.ui.navigation.ProvideBottomBar
import com.sameerasw.essentials.utils.AutomationFailureNotifier
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun ModeEditorScreen(
    route: AppRoute.ModeEditor,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val viewModel: MainViewModel = viewModel()
    LaunchedEffect(Unit) { viewModel.check(context) }

    val existing = remember(route.modeId) {
        ModesRepository.init(appContext)
        route.modeId?.let(ModesRepository::getMode)
    }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: PRESET_PASTEL_COLORS.random()) }
    var iconName by remember { mutableStateOf(existing?.iconName ?: ModeIcons.DEFAULT) }
    var features by remember { mutableStateOf(existing?.features ?: emptyMap()) }
    var freezeTagIds by remember { mutableStateOf(existing?.freezeTagIds ?: emptySet()) }
    var freezeCategories by remember { mutableStateOf(existing?.freezeCategories ?: emptySet()) }
    var entryActions by remember { mutableStateOf(existing?.entryActions ?: emptyList()) }
    var exitActions by remember { mutableStateOf(existing?.exitActions ?: emptyList()) }
    var missingPermissions by remember { mutableStateOf<List<AppPermission>>(emptyList()) }
    var linkBase by remember { mutableStateOf(existing?.takeIf { it.isLinked }) }
    var conditions by remember { mutableStateOf(existing?.conditions ?: emptyList()) }
    var askBeforeActivating by remember { mutableStateOf(existing?.askBeforeActivating ?: false) }
    var exitTripAlarmId by remember { mutableStateOf(existing?.exitTripAlarmId) }
    var tripOnStart by remember { mutableStateOf(existing?.tripOnStart ?: false) }
    var showSystemModePicker by remember { mutableStateOf(false) }
    val actionCategories = remember { ActionRegistry.getCategories() }

    fun requirePermissionsFor(action: Action) {
        val missing = AutomationFailureNotifier.missingPermissions(context, action)
        if (missing.isNotEmpty()) missingPermissions = missing
    }

    val isValid = name.isNotBlank()

    fun draftMode(): Mode =
            (linkBase ?: existing ?: Mode()).copy(
                name = linkBase?.name ?: name.trim(),
                colorHex = colorHex,
                iconName = iconName,
                features = features,
                freezeTagIds = freezeTagIds,
                freezeCategories = freezeCategories,
                entryActions = entryActions,
                exitActions = exitActions,
                conditions = conditions,
                askBeforeActivating = askBeforeActivating,
                exitTripAlarmId = exitTripAlarmId,
                tripOnStart = tripOnStart,
            )

    fun save() {
        val mode = draftMode()
        if (existing != null && (existing.conditions != conditions || existing.askBeforeActivating != askBeforeActivating)) {
            ModeAutomation.resetHold(appContext, existing.id)
        }
        ModesRepository.upsert(mode)
        ModesController.requestSync(appContext, reapply = true)
        onBack()
    }

    fun delete(mode: Mode) {
        ModesRepository.remove(mode.id)
        ModesController.requestSync(appContext)
        onBack()
    }

    val hasUnsavedChanges = if (existing != null) draftMode() != existing else name.isNotBlank()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val view = LocalView.current
    val handleBack = { if (hasUnsavedChanges) showDiscardDialog = true else onBack() }
    BackHandler { handleBack() }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { TranslatableText(R.string.diy_discard_warning_title) },
            text = { TranslatableText(R.string.diy_discard_warning_desc) },
            confirmButton = {
                TextButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showDiscardDialog = false
                        onBack()
                    },
                ) {
                    TranslatableText(stringResId = R.string.translation_discard, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showDiscardDialog = false
                    },
                ) {
                    TranslatableText(R.string.action_cancel)
                }
            },
        )
    }

    var isEditingIdentity by rememberSaveable { mutableStateOf(existing == null) }
    var customActionsOnStart by rememberSaveable { mutableStateOf(true) }
    val nothingConfigured =
        remember {
            conditions.isEmpty() && features.isEmpty() && freezeTagIds.isEmpty() && freezeCategories.isEmpty() &&
                entryActions.isEmpty() && exitActions.isEmpty() && exitTripAlarmId == null
        }
    val headerColor = runCatching { Color(colorHex.toColorInt()) }.getOrDefault(MaterialTheme.colorScheme.primaryContainer)
    val settingsIntent = remember(existing?.id, linkBase) { existing?.let { ModesController.systemSettingsIntent(appContext, it.id) } }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ModeEditorHeader(
                icon = linkBase?.let { rememberModeIconPainter(it) } ?: painterResource(ModeIcons.resolve(iconName)),
                color = headerColor,
                name = linkBase?.name ?: name,
                subtitle =
                    linkBase?.linkedModeName?.let { stringResource(R.string.mode_linked_to, it) }
                        ?: stringResource(R.string.mode_owned),
                isEditing = isEditingIdentity,
                onEditToggle = { isEditingIdentity = !isEditingIdentity },
                onOpenSystemSettings = settingsIntent?.let { intent -> { runCatching { context.startActivity(intent) } } },
            ) {
                ModeAppearanceFields(
                    name = name,
                    onNameChange = { name = it },
                    colorHex = colorHex,
                    onColorChange = { colorHex = it },
                    iconName = iconName,
                    onIconChange = { iconName = it },
                    identityLocked = linkBase != null,
                )
                if (existing != null) {
                    RoundedCardContainer(spacing = 2.dp) {
                        linkBase?.linkedModeName?.let { linkedName ->
                            IconToggleItem(
                                iconRes = R.drawable.rounded_link_24,
                                title = linkedName,
                                showToggle = false,
                                onClick = {
                                    if (ModesController.canLinkSystemModes()) {
                                        showSystemModePicker = true
                                    } else {
                                        missingPermissions = listOf(AppPermission.SHIZUKU)
                                    }
                                },
                            )
                        }
                        IconToggleItem(
                            iconRes = if (existing.isLinked) R.drawable.rounded_link_off_24 else R.drawable.rounded_delete_24,
                            title = stringResource(if (existing.isLinked) R.string.mode_unlink else R.string.action_delete),
                            showToggle = false,
                            onClick = { delete(existing) },
                        )
                    }
                }
            }

            ModeCollapsibleSection(R.string.mode_section_conditions, initiallyExpanded = conditions.isNotEmpty() || nothingConfigured) {
                ModeConditionsSection(
                    conditions = conditions,
                    onConditionsChange = { conditions = it },
                    askBeforeActivating = askBeforeActivating,
                    onAskBeforeActivatingChange = { askBeforeActivating = it },
                    onMissingPermissions = { missingPermissions = it },
                    viewModel = viewModel,
                )
            }

            ModeCollapsibleSection(R.string.mode_section_features, initiallyExpanded = features.isNotEmpty()) {
                ModeFeaturesSection(
                    features = features,
                    onFeatureChange = { feature, value ->
                        features = if (value == null) features - feature.name else features + (feature.name to value)
                        if (value != null) requirePermissionsFor(feature.action(value))
                    },
                )
            }

            ModeCollapsibleSection(
                R.string.mode_section_freeze,
                initiallyExpanded = freezeTagIds.isNotEmpty() || freezeCategories.isNotEmpty(),
            ) {
                ModeFreezeSection(
                    tagIds = freezeTagIds,
                    categories = freezeCategories,
                    onTagChange = { id, checked ->
                        freezeTagIds = if (checked) freezeTagIds + id else freezeTagIds - id
                        if (checked) requirePermissionsFor(Action.FreezeApps())
                    },
                    onCategoryChange = { category, checked ->
                        freezeCategories = if (checked) freezeCategories + category else freezeCategories - category
                        if (checked) requirePermissionsFor(Action.FreezeApps())
                    },
                )
            }

            ModeCollapsibleSection(
                R.string.button_remap_power_actions,
                initiallyExpanded = entryActions.isNotEmpty() || exitActions.isNotEmpty(),
            ) {
                val startLabel = stringResource(R.string.mode_trip_on_start)
                val endLabel = stringResource(R.string.mode_trip_on_end)
                RoundedCardContainer(containerColor = MaterialTheme.colorScheme.surfaceBright) {
                    SegmentedPicker(
                        items = listOf(true, false),
                        selectedItem = customActionsOnStart,
                        onItemSelected = { customActionsOnStart = it },
                        labelProvider = { if (it) startLabel else endLabel },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ActionSequenceEditor(
                    viewModel = viewModel,
                    actions = if (customActionsOnStart) entryActions else exitActions,
                    onActionsChange = { if (customActionsOnStart) entryActions = it else exitActions = it },
                    screenOnOnly = false,
                    listKey = if (customActionsOnStart) "mode_entry" else "mode_exit",
                    emptyText = stringResource(R.string.diy_no_actions),
                    categories = actionCategories,
                )
            }

            ModeCollapsibleSection(R.string.tile_location_reached, initiallyExpanded = exitTripAlarmId != null) {
                ModeExitTripRow(
                    alarmId = exitTripAlarmId,
                    onAlarmChange = { exitTripAlarmId = it },
                    onStart = tripOnStart,
                    onOnStartChange = { tripOnStart = it },
                    viewModel = viewModel,
                )
            }
        }

        if (showSystemModePicker && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val takenIds =
                remember {
                    ModesRepository.modes.value
                        .filter { it.id != existing?.id }
                        .mapNotNull { it.linkedModeId }
                        .toSet()
                }
            SystemModePickerSheet(
                selectedId = linkBase?.linkedModeId,
                excludedIds = takenIds,
                onDismiss = { showSystemModePicker = false },
                onSelect = { systemMode ->
                    linkBase = (linkBase ?: existing ?: Mode()).linkedTo(systemMode)
                    name = systemMode.name
                    showSystemModePicker = false
                },
            )
        }

        if (missingPermissions.isNotEmpty()) {
            PermissionsBottomSheet(
                onDismissRequest = {
                    missingPermissions = emptyList()
                    viewModel.check(context)
                },
                featureTitle = R.string.tab_modes,
                permissions = PermissionUIHelper.getPermissionItems(missingPermissions, context, viewModel, context.findActivity()),
            )
        }

        ProvideBottomBar(
            route = route,
            spec =
                BottomBarSpec(
                    title = stringResource(if (existing == null) R.string.action_new_mode else R.string.mode_editor_edit_title),
                    onBack = handleBack,
                    fabAction = if (isValid) { { save() } } else null,
                    fabIconRes = R.drawable.rounded_check_24,
                    fabContentDescription = stringResource(R.string.action_save),
                ),
        )
    }
}

class ModeEditorActivity : Activity() {
    companion object {
        const val EXTRA_MODE_ID = "mode_id"

        fun createIntent(
            context: Context,
            modeId: String?,
        ): Intent =
            Intent(context, ModeEditorActivity::class.java).apply {
                modeId?.let { putExtra(EXTRA_MODE_ID, it) }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The system mode settings page launches this with the rule id rather than our mode id
        if (!intent.hasExtra(EXTRA_MODE_ID)) {
            intent.getStringExtra(NotificationManager.EXTRA_AUTOMATIC_RULE_ID)
                ?.let { ModesController.modeIdForRule(this, it) }
                ?.let { intent.putExtra(EXTRA_MODE_ID, it) }
        }
        AppRoutes.redirectToMain(this)
    }
}
