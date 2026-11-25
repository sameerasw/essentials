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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.sameerasw.essentials.domain.modes.ModeIcons
import com.sameerasw.essentials.domain.modes.ModesController
import com.sameerasw.essentials.domain.modes.ModesRepository
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PRESET_PASTEL_COLORS
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.features.modes.ModeAppearanceFields
import com.sameerasw.essentials.ui.features.modes.ModeConditionsSection
import com.sameerasw.essentials.ui.features.modes.ModeFeaturesSection
import com.sameerasw.essentials.ui.features.modes.ModeFreezeSection
import com.sameerasw.essentials.ui.features.modes.ModeSectionTitle
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
    var showSystemModePicker by remember { mutableStateOf(false) }
    val actionCategories = remember { ActionRegistry.getCategories() }

    fun requirePermissionsFor(action: Action) {
        val missing = AutomationFailureNotifier.missingPermissions(context, action)
        if (missing.isNotEmpty()) missingPermissions = missing
    }

    val isValid = name.isNotBlank()

    fun save() {
        val mode =
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
            )
        ModesRepository.upsert(mode)
        ModesController.requestSync(appContext, reapply = true)
        onBack()
    }

    fun delete(mode: Mode) {
        ModesRepository.remove(mode.id)
        ModesController.requestSync(appContext)
        onBack()
    }

    val previewColor by animateColorAsState(
        targetValue = runCatching { Color(colorHex.toColorInt()) }.getOrDefault(MaterialTheme.colorScheme.primaryContainer),
        label = "mode_preview_color",
    )
    val previewSize by animateDpAsState(if (isValid) 96.dp else 80.dp, label = "mode_preview_size")

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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(previewSize)
                        .background(previewColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = linkBase?.let { rememberModeIconPainter(it) } ?: painterResource(ModeIcons.resolve(iconName)),
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp),
                )
            }

            ModeAppearanceFields(
                name = name,
                onNameChange = { name = it },
                colorHex = colorHex,
                onColorChange = { colorHex = it },
                iconName = iconName,
                onIconChange = { iconName = it },
                identityLocked = linkBase != null,
            )

            linkBase?.linkedModeName?.let { linkedName ->
                ModeSectionTitle(R.string.mode_section_system)
                RoundedCardContainer(spacing = 2.dp) {
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
            }

            ModeConditionsSection(
                conditions = conditions,
                onConditionsChange = { conditions = it },
                askBeforeActivating = askBeforeActivating,
                onAskBeforeActivatingChange = { askBeforeActivating = it },
                onMissingPermissions = { missingPermissions = it },
            )

            ModeFeaturesSection(
                features = features,
                onFeatureChange = { feature, value ->
                    features = if (value == null) features - feature.name else features + (feature.name to value)
                    if (value != null) requirePermissionsFor(feature.action(value))
                },
            )

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

            ModeSectionTitle(R.string.mode_section_start_actions)
            ActionSequenceEditor(
                viewModel = viewModel,
                actions = entryActions,
                onActionsChange = { entryActions = it },
                screenOnOnly = false,
                listKey = "mode_entry",
                emptyText = stringResource(R.string.diy_no_actions),
                categories = actionCategories,
            )

            ModeSectionTitle(R.string.mode_section_end_actions)
            ActionSequenceEditor(
                viewModel = viewModel,
                actions = exitActions,
                onActionsChange = { exitActions = it },
                screenOnOnly = false,
                listKey = "mode_exit",
                emptyText = stringResource(R.string.diy_no_actions),
                categories = actionCategories,
            )

            if (existing != null) {
                RoundedCardContainer(spacing = 2.dp) {
                    val settingsIntent = remember(existing.id) { ModesController.systemSettingsIntent(appContext, existing.id) }
                    if (settingsIntent != null) {
                        IconToggleItem(
                            iconRes = R.drawable.rounded_open_in_new_24,
                            title = stringResource(R.string.mode_system_settings),
                            showToggle = false,
                            onClick = { runCatching { context.startActivity(settingsIntent) } },
                        )
                    }
                    val isLinked = existing.isLinked
                    IconToggleItem(
                        iconRes = if (isLinked) R.drawable.rounded_link_off_24 else R.drawable.rounded_delete_24,
                        title = stringResource(if (isLinked) R.string.mode_unlink else R.string.action_delete),
                        showToggle = false,
                        onClick = { delete(existing) },
                    )
                }
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
                permissions = PermissionUIHelper.getPermissionItems(missingPermissions, context, viewModel),
            )
        }

        ProvideBottomBar(
            route = route,
            spec =
                BottomBarSpec(
                    title = stringResource(if (existing == null) R.string.action_new_mode else R.string.mode_editor_edit_title),
                    onBack = onBack,
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
