/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Radial Menus
 * File: QuickBallSettingsUI.kt
 * Description: Compose UI settings screen for Quick Ball radial menus, edge pill gestures,
 * styling themes, dimensions, and behavior triggers organized in structured sub-menus.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.cards.SelectionCardItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.ColorSwatchPicker
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionItem
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.ui.features.display.actions.GestureActionPickerSheet
import com.sameerasw.essentials.ui.features.system.quickball.ConnectorStyleSample
import com.sameerasw.essentials.ui.features.system.quickball.MenuConnectorStyle
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallBallStyle
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallConnectorSheet
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallHapticIntensity
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallIconPack
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallIconResolver
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallMenuPreview
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallPillStyle
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallPresetTheme
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallPresetThemeSheet
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallShortcutStyle
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallTheme
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.viewmodels.MainViewModel
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import java.util.Locale
import kotlin.math.roundToInt

const val QUICK_BALL_FEATURE_ID = "feat_quick_ball"

/**
 * Logical categorization folders for Quick Ball customization settings.
 */
enum class QuickBallSubMenu(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val iconRes: Int,
) {
    SHORTCUTS(
        R.string.quick_ball_menu_shortcuts_title,
        R.string.quick_ball_menu_shortcuts_subtitle,
        R.drawable.rounded_apps_24,
    ),
    GESTURES(
        R.string.quick_ball_menu_gestures_title,
        R.string.quick_ball_menu_gestures_subtitle,
        R.drawable.rounded_touch_app_24,
    ),
    STYLING(
        R.string.quick_ball_menu_styling_title,
        R.string.quick_ball_menu_styling_subtitle,
        R.drawable.rounded_palette_24,
    ),
    DIMENSIONS(
        R.string.quick_ball_menu_dimensions_title,
        R.string.quick_ball_menu_dimensions_subtitle,
        R.drawable.rounded_crop_portrait_24,
    ),
    BEHAVIOR(
        R.string.quick_ball_menu_behavior_title,
        R.string.quick_ball_menu_behavior_subtitle,
        R.drawable.rounded_settings_24,
    ),
}

/**
 * Stable entry model for fluid drag-and-drop shortcut slot reordering.
 */
data class QuickBallSlotEntry(
    val id: Long,
    val action: Action,
)

@Composable
fun QuickBallSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isAccessibilityEnabled = viewModel.isAccessibilityEnabled.value
    var showPermissionSheet by remember { mutableStateOf(false) }

    // Active sub-menu folder state
    var activeSubMenu by remember {
        mutableStateOf(
            when (highlightSetting) {
                "quick_ball_gestures" -> QuickBallSubMenu.GESTURES
                "quick_ball_styling" -> QuickBallSubMenu.STYLING
                "quick_ball_dimensions" -> QuickBallSubMenu.DIMENSIONS
                "quick_ball_behavior" -> QuickBallSubMenu.BEHAVIOR
                else -> null
            }
        )
    }

    // Intercept hardware/gesture back press when in a sub-menu to return to category hub
    BackHandler(enabled = activeSubMenu != null) {
        activeSubMenu = null
    }

    // Dialog & Picker States
    var activePickingSlot by remember { mutableIntStateOf(0) }
    var slotPendingAppSelection by remember { mutableIntStateOf(0) }
    var activePickingGesture by remember { mutableStateOf<String?>(null) }
    var showConnectorSheet by remember { mutableStateOf(false) }
    var showPresetThemeSheet by remember { mutableStateOf(false) }
    var showBallStyleSheet by remember { mutableStateOf(false) }
    var showPillStyleSheet by remember { mutableStateOf(false) }
    var showShortcutStyleSheet by remember { mutableStateOf(false) }
    var showIconPackSheet by remember { mutableStateOf(false) }
    var showAppSelectionSheet by remember { mutableStateOf(false) }

    if (showPermissionSheet) {
        val accessibilityPermission =
            PermissionItem(
                iconRes = R.drawable.rounded_accessibility_new_24,
                title = R.string.perm_accessibility_title,
                description = R.string.perm_accessibility_desc_common,
                dependentFeatures = listOf(R.string.feat_quick_ball_title),
                actionLabel = if (isAccessibilityEnabled) R.string.label_enabled else R.string.perm_action_enable,
                action = { PermissionUtils.openAccessibilitySettings(context) },
                isGranted = isAccessibilityEnabled,
            )

        PermissionsBottomSheet(
            featureTitle = R.string.feat_quick_ball_title,
            permissions = listOf(accessibilityPermission),
            onDismissRequest = { showPermissionSheet = false },
        )
    }

    // Shortcut slot picker
    if (activePickingSlot in 1..9) {
        val currentSlotAction =
            when (activePickingSlot) {
                1 -> viewModel.quickBallSlot1Action.value
                2 -> viewModel.quickBallSlot2Action.value
                3 -> viewModel.quickBallSlot3Action.value
                4 -> viewModel.quickBallSlot4Action.value
                5 -> viewModel.quickBallSlot5Action.value
                6 -> viewModel.quickBallSlot6Action.value
                7 -> viewModel.quickBallSlot7Action.value
                8 -> viewModel.quickBallSlot8Action.value
                9 -> viewModel.quickBallSlot9Action.value
                else -> Action.Back
            }

        GestureActionPickerSheet(
            viewModel = viewModel,
            currentAction = currentSlotAction,
            onActionSelected = { selectedAction ->
                if (selectedAction != null) {
                    if (selectedAction is Action.OpenApp && selectedAction.packageName.isBlank()) {
                        slotPendingAppSelection = activePickingSlot
                    } else {
                        viewModel.setQuickBallSlotAction(activePickingSlot, selectedAction)
                    }
                }
                activePickingSlot = 0
            },
            onPickerClosed = {
                activePickingSlot = 0
            },
        )
    }

    if (slotPendingAppSelection in 1..9) {
        SingleAppSelectionSheet(
            includeSelf = true,
            onDismissRequest = { slotPendingAppSelection = 0 },
            onAppSelected = { app ->
                viewModel.setQuickBallSlotAction(
                    slotPendingAppSelection,
                    Action.OpenApp(packageName = app.packageName),
                )
                slotPendingAppSelection = 0
            },
        )
    }

    // Pill gesture picker
    if (activePickingGesture != null) {
        val gestureKey = activePickingGesture!!
        val currentGestureAction =
            when (gestureKey) {
                "DOUBLE_TAP" -> viewModel.quickBallDoubleTapAction.value
                "TRIPLE_TAP" -> viewModel.quickBallTripleTapAction.value
                "LONG_PRESS" -> viewModel.quickBallLongPressAction.value
                "SWIPE_UP" -> viewModel.quickBallSwipeUpAction.value
                "SWIPE_DOWN" -> viewModel.quickBallSwipeDownAction.value
                else -> null
            }

        GestureActionPickerSheet(
            viewModel = viewModel,
            currentAction = currentGestureAction,
            onActionSelected = { selectedAction ->
                viewModel.setQuickBallGestureAction(gestureKey, selectedAction)
                activePickingGesture = null
            },
            onPickerClosed = {
                activePickingGesture = null
            },
        )
    }

    // Preset Theme Sheet
    if (showPresetThemeSheet) {
        val currentStyle = MenuConnectorStyle.fromName(viewModel.quickBallConnectorStyle.value)
        QuickBallPresetThemeSheet(
            selectedThemeName = viewModel.quickBallPresetTheme.value,
            connectorStyle = currentStyle,
            onThemeSelected = { theme ->
                HapticUtil.performUIHaptic(view)
                viewModel.setQuickBallPresetTheme(theme.name)
                showPresetThemeSheet = false
            },
            onDismissRequest = { showPresetThemeSheet = false },
        )
    }

    // Connector style sheet
    if (showConnectorSheet) {
        QuickBallConnectorSheet(
            selectedStyleName = viewModel.quickBallConnectorStyle.value,
            onStyleSelected = { style ->
                HapticUtil.performUIHaptic(view)
                viewModel.setQuickBallConnectorStyle(style.name)
                showConnectorSheet = false
            },
            onDismissRequest = { showConnectorSheet = false },
        )
    }

    // Ball style sheet
    if (showBallStyleSheet) {
        EssentialsBottomSheet(
            onDismissRequest = { showBallStyleSheet = false },
        ) {
            Text(
                text = stringResource(R.string.quick_ball_style_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                QuickBallBallStyle.entries.forEach { style ->
                    val isSelected = viewModel.quickBallBallStyle.value == style.name
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable {
                                HapticUtil.performUIHaptic(view)
                                viewModel.setQuickBallBallStyle(style.name)
                                showBallStyleSheet = false
                            },
                        headlineContent = {
                            Text(
                                text = stringResource(style.titleRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        trailingContent = if (isSelected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_check_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        } else null,
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceBright,
                        ),
                    )
                }
            }
        }
    }

    // Pill style sheet
    if (showPillStyleSheet) {
        EssentialsBottomSheet(
            onDismissRequest = { showPillStyleSheet = false },
        ) {
            Text(
                text = stringResource(R.string.quick_ball_pill_style_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                QuickBallPillStyle.entries.forEach { style ->
                    val isSelected = viewModel.quickBallPillStyle.value == style.name
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable {
                                HapticUtil.performUIHaptic(view)
                                viewModel.setQuickBallPillStyle(style.name)
                                showPillStyleSheet = false
                            },
                        headlineContent = {
                            Text(
                                text = stringResource(style.titleRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(style.descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingContent = {
                            PillStyleMiniPreview(
                                style = style,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(width = 36.dp, height = 36.dp),
                            )
                        },
                        trailingContent = if (isSelected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_check_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        } else null,
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceBright,
                        ),
                    )
                }
            }
        }
    }

    // Shortcut display style sheet
    if (showShortcutStyleSheet) {
        EssentialsBottomSheet(
            onDismissRequest = { showShortcutStyleSheet = false },
        ) {
            Text(
                text = stringResource(R.string.quick_ball_shortcut_style_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                QuickBallShortcutStyle.entries.forEach { style ->
                    val isSelected = viewModel.quickBallShortcutStyle.value == style.name
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable {
                                HapticUtil.performUIHaptic(view)
                                viewModel.setQuickBallShortcutStyle(style.name)
                                showShortcutStyleSheet = false
                            },
                        headlineContent = {
                            Text(
                                text = stringResource(style.titleRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        trailingContent = if (isSelected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_check_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        } else null,
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceBright,
                        ),
                    )
                }
            }
        }
    }

    // System icon pack sheet
    if (showIconPackSheet) {
        EssentialsBottomSheet(
            onDismissRequest = { showIconPackSheet = false },
        ) {
            Text(
                text = stringResource(R.string.quick_ball_icon_pack_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                QuickBallIconPack.entries.forEach { pack ->
                    val isSelected = viewModel.quickBallIconPack.value == pack.name
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable {
                                HapticUtil.performUIHaptic(view)
                                viewModel.setQuickBallIconPack(pack.name)
                                showIconPackSheet = false
                            },
                        headlineContent = {
                            Text(
                                text = stringResource(pack.titleRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        trailingContent = if (isSelected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_check_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        } else null,
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceBright,
                        ),
                    )
                }
            }
        }
    }

    // Auto-hide in apps sheet
    if (showAppSelectionSheet) {
        AppSelectionSheet(
            onDismissRequest = { showAppSelectionSheet = false },
            onLoadApps = { _ ->
                viewModel.quickBallAutoHideApps.value.map { AppSelection(it, true) }
            },
            onSaveApps = { _, apps ->
                val selectedPackages = apps.filter { it.isEnabled }.map { it.packageName }.toSet()
                viewModel.setQuickBallAutoHideApps(selectedPackages)
            },
        )
    }

    val isEnabled = viewModel.isQuickBallEnabled.value
    val slotCount = viewModel.quickBallSlotCount.intValue

    val currentActions = (1..slotCount).map { i ->
        when (i) {
            1 -> viewModel.quickBallSlot1Action.value
            2 -> viewModel.quickBallSlot2Action.value
            3 -> viewModel.quickBallSlot3Action.value
            4 -> viewModel.quickBallSlot4Action.value
            5 -> viewModel.quickBallSlot5Action.value
            6 -> viewModel.quickBallSlot6Action.value
            7 -> viewModel.quickBallSlot7Action.value
            8 -> viewModel.quickBallSlot8Action.value
            9 -> viewModel.quickBallSlot9Action.value
            else -> Action.Back
        }
    }

    // Stable slot entries for fluid reordering
    var idSeed by remember { mutableLongStateOf(slotCount.toLong()) }
    var slotEntries by remember {
        mutableStateOf(
            currentActions.mapIndexed { index, action ->
                QuickBallSlotEntry(id = (index + 1).toLong(), action = action)
            }
        )
    }

    LaunchedEffect(slotCount, currentActions) {
        if (slotEntries.size != slotCount || slotEntries.map { it.action } != currentActions) {
            val newEntries = mutableListOf<QuickBallSlotEntry>()
            for (i in 0 until slotCount) {
                val act = currentActions.getOrElse(i) { Action.Back }
                val existing = slotEntries.getOrNull(i)
                if (existing != null && existing.action == act) {
                    newEntries.add(existing)
                } else {
                    idSeed += 1
                    newEntries.add(QuickBallSlotEntry(id = idSeed, action = act))
                }
            }
            slotEntries = newEntries
        }
    }

    AnimatedContent(
        targetState = activeSubMenu,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width / 3 } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width / 3 } + fadeOut(),
                )
            } else {
                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width / 3 } + fadeOut(),
                )
            }
        },
        label = "QuickBallSubMenuTransition",
    ) { currentMenu ->
        if (currentMenu == null) {
            // ==========================================
            // MAIN CATEGORY HUB
            // ==========================================
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Master Toggle Card
                FeatureCard(
                    title = stringResource(R.string.quick_ball_enable_title),
                    description = stringResource(R.string.quick_ball_enable_desc),
                    iconRes = R.drawable.rounded_touch_app_24,
                    isEnabled = isEnabled,
                    onToggle = { enabled ->
                        if (enabled && !isAccessibilityEnabled) {
                            showPermissionSheet = true
                        } else {
                            viewModel.setQuickBallEnabled(enabled)
                        }
                    },
                    onClick = {
                        if (!isAccessibilityEnabled) {
                            showPermissionSheet = true
                        }
                    },
                    showToggle = true,
                    modifier = Modifier.highlight(highlightSetting == "quick_ball_enabled"),
                )

                AnimatedVisibility(
                    visible = isEnabled,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Live radial menu preview
                        val resolvedColors = QuickBallTheme.resolveColors(
                            context,
                            com.sameerasw.essentials.data.repository.SettingsRepository(context),
                        )
                        val connectorStyle = MenuConnectorStyle.fromName(viewModel.quickBallConnectorStyle.value)
                        val currentShortcutStyle = QuickBallShortcutStyle.fromName(viewModel.quickBallShortcutStyle.value)

                        QuickBallMenuPreview(
                            actions = currentActions,
                            connectorStyle = connectorStyle,
                            connectorColor = Color(resolvedColors.connectorColor),
                            connectorWidthDp = viewModel.quickBallConnectorWidth.floatValue,
                            ballSizeDp = viewModel.quickBallSize.floatValue,
                            buttonSizeDp = viewModel.quickBallMenuSize.floatValue,
                            menuRadiusDp = viewModel.quickBallMenuRadius.floatValue,
                            iconSizeDp = viewModel.quickBallMenuIconSize.floatValue,
                            ballColor = Color(resolvedColors.ballColor),
                            ballIconColor = Color(resolvedColors.ballIconColor),
                            buttonColor = Color(resolvedColors.surfaceColor),
                            buttonIconColor = Color(resolvedColors.contentColor),
                            isBorderEnabled = viewModel.isQuickBallBorderEnabled.value,
                            borderWidthDp = viewModel.quickBallBorderWidth.floatValue,
                            borderColor = Color(resolvedColors.borderColor),
                            ballBorderColor = Color(resolvedColors.ballBorderColor),
                            shortcutStyle = currentShortcutStyle,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )

                        // Categories Header
                        Text(
                            text = stringResource(R.string.quick_ball_categories_header),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )

                        // Folder Hub Cards
                        RoundedCardContainer {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                QuickBallSubMenu.entries.forEachIndexed { index, menu ->
                                    val summaryBadge = when (menu) {
                                        QuickBallSubMenu.SHORTCUTS -> "$slotCount/9 active"
                                        QuickBallSubMenu.GESTURES -> {
                                            val style = QuickBallPillStyle.fromName(viewModel.quickBallPillStyle.value)
                                            stringResource(style.titleRes)
                                        }
                                        QuickBallSubMenu.STYLING -> {
                                            when (viewModel.quickBallColorMode.value) {
                                                "DYNAMIC" -> stringResource(R.string.quick_ball_color_mode_dynamic_short)
                                                "PRESET" -> QuickBallPresetTheme.fromName(viewModel.quickBallPresetTheme.value).title
                                                else -> stringResource(R.string.quick_ball_color_mode_custom_short)
                                            }
                                        }
                                        QuickBallSubMenu.DIMENSIONS -> "${viewModel.quickBallSize.floatValue.roundToInt()}dp ball"
                                        QuickBallSubMenu.BEHAVIOR -> "${String.format(Locale.getDefault(), "%.1fs", viewModel.quickBallStashDelay.longValue / 1000f)} stash"
                                    }

                                    ListItem(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.small)
                                            .clickable {
                                                HapticUtil.performUIHaptic(view)
                                                activeSubMenu = menu
                                            },
                                        leadingContent = {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                        shape = CircleShape,
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = menu.iconRes),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp),
                                                )
                                            }
                                        },
                                        headlineContent = {
                                            Text(
                                                text = stringResource(id = menu.titleRes),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = stringResource(id = menu.subtitleRes),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        trailingContent = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                    shape = RoundedCornerShape(12.dp),
                                                ) {
                                                    Text(
                                                        text = summaryBadge,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    )
                                                }
                                                Icon(
                                                    painter = painterResource(R.drawable.rounded_chevron_right_24),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                                        ),
                                    )

                                    if (index < QuickBallSubMenu.entries.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // SUB-MENU VIEW
            // ==========================================
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header Bar with Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            activeSubMenu = null
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_arrow_back_24),
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(currentMenu.titleRes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(currentMenu.subtitleRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                when (currentMenu) {
                    // ----------------------------------------------------
                    // 1. SHORTCUTS & RADIAL MENU
                    // ----------------------------------------------------
                    QuickBallSubMenu.SHORTCUTS -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.quick_ball_slots_section),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "$slotCount/9",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        val resolvedColors = QuickBallTheme.resolveColors(
                            context,
                            com.sameerasw.essentials.data.repository.SettingsRepository(context),
                        )
                        val connectorStyle = MenuConnectorStyle.fromName(viewModel.quickBallConnectorStyle.value)
                        val currentShortcutStyle = QuickBallShortcutStyle.fromName(viewModel.quickBallShortcutStyle.value)

                        QuickBallMenuPreview(
                            actions = currentActions,
                            connectorStyle = connectorStyle,
                            connectorColor = Color(resolvedColors.connectorColor),
                            connectorWidthDp = viewModel.quickBallConnectorWidth.floatValue,
                            ballSizeDp = viewModel.quickBallSize.floatValue,
                            buttonSizeDp = viewModel.quickBallMenuSize.floatValue,
                            menuRadiusDp = viewModel.quickBallMenuRadius.floatValue,
                            iconSizeDp = viewModel.quickBallMenuIconSize.floatValue,
                            ballColor = Color(resolvedColors.ballColor),
                            ballIconColor = Color(resolvedColors.ballIconColor),
                            buttonColor = Color(resolvedColors.surfaceColor),
                            buttonIconColor = Color(resolvedColors.contentColor),
                            isBorderEnabled = viewModel.isQuickBallBorderEnabled.value,
                            borderWidthDp = viewModel.quickBallBorderWidth.floatValue,
                            borderColor = Color(resolvedColors.borderColor),
                            ballBorderColor = Color(resolvedColors.ballBorderColor),
                            shortcutStyle = currentShortcutStyle,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )

                        RoundedCardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                ReorderableColumn(
                                    list = slotEntries,
                                    onSettle = { from, to ->
                                        if (from != to) {
                                            slotEntries = slotEntries.toMutableList().apply {
                                                add(to, removeAt(from))
                                            }
                                            viewModel.reorderQuickBallSlots(from + 1, to + 1)
                                        }
                                    },
                                    onMove = {
                                        HapticUtil.performUIHaptic(view)
                                    },
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) { index, entry, isDragging ->
                                    key(entry.id) {
                                        ReorderableItem {
                                            QuickBallSlotRow(
                                                slotIndex = index + 1,
                                                action = entry.action,
                                                isDragging = isDragging,
                                                dragHandleModifier = Modifier.draggableHandle(),
                                                onClick = {
                                                    HapticUtil.performUIHaptic(view)
                                                    activePickingSlot = index + 1
                                                },
                                                onDelete = if (slotCount > 2) {
                                                    {
                                                        HapticUtil.performVirtualKeyHaptic(view)
                                                        viewModel.deleteQuickBallSlot(index + 1)
                                                    }
                                                } else null,
                                            )
                                        }
                                    }
                                }

                                if (slotCount < 9) {
                                    Button(
                                        onClick = {
                                            HapticUtil.performUIHaptic(view)
                                            viewModel.setQuickBallSlotCount(slotCount + 1)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_add_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text(stringResource(R.string.quick_ball_add_slot))
                                    }
                                }
                            }
                        }
                    }

                    // ----------------------------------------------------
                    // 2. DOCKED PILL & GESTURES
                    // ----------------------------------------------------
                    QuickBallSubMenu.GESTURES -> {
                        // Pill Style Selector
                        Text(
                            text = stringResource(R.string.quick_ball_pill_style_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            val currentPillStyle = QuickBallPillStyle.fromName(viewModel.quickBallPillStyle.value)
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable {
                                        HapticUtil.performUIHaptic(view)
                                        showPillStyleSheet = true
                                    },
                                leadingContent = {
                                    PillStyleMiniPreview(
                                        style = currentPillStyle,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(width = 36.dp, height = 36.dp),
                                    )
                                },
                                headlineContent = { Text(stringResource(currentPillStyle.titleRes)) },
                                supportingContent = { Text(stringResource(currentPillStyle.descRes)) },
                                trailingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_chevron_right_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                                ),
                            )
                        }

                        // Edge Gestures
                        Text(
                            text = stringResource(R.string.quick_ball_gestures_section),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                IconToggleItem(
                                    title = stringResource(R.string.quick_ball_gestures_enable_title),
                                    description = stringResource(R.string.quick_ball_gestures_enable_desc),
                                    isChecked = viewModel.isQuickBallGesturesEnabled.value,
                                    onCheckedChange = { viewModel.setQuickBallGesturesEnabled(it) },
                                    iconRes = R.drawable.rounded_touch_app_24,
                                )

                                AnimatedVisibility(
                                    visible = viewModel.isQuickBallGesturesEnabled.value,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        )
                                        QuickBallGestureRow(
                                            gestureTitle = stringResource(R.string.quick_ball_gesture_double_tap),
                                            action = viewModel.quickBallDoubleTapAction.value,
                                            onClick = { activePickingGesture = "DOUBLE_TAP" },
                                        )
                                        QuickBallGestureRow(
                                            gestureTitle = stringResource(R.string.quick_ball_gesture_triple_tap),
                                            action = viewModel.quickBallTripleTapAction.value,
                                            onClick = { activePickingGesture = "TRIPLE_TAP" },
                                        )
                                        QuickBallGestureRow(
                                            gestureTitle = stringResource(R.string.quick_ball_gesture_long_press),
                                            action = viewModel.quickBallLongPressAction.value,
                                            onClick = { activePickingGesture = "LONG_PRESS" },
                                        )
                                        QuickBallGestureRow(
                                            gestureTitle = stringResource(R.string.quick_ball_gesture_swipe_up),
                                            action = viewModel.quickBallSwipeUpAction.value,
                                            onClick = { activePickingGesture = "SWIPE_UP" },
                                        )
                                        QuickBallGestureRow(
                                            gestureTitle = stringResource(R.string.quick_ball_gesture_swipe_down),
                                            action = viewModel.quickBallSwipeDownAction.value,
                                            onClick = { activePickingGesture = "SWIPE_DOWN" },
                                        )
                                    }
                                }
                            }
                        }

                        // Flick to Select & Hold-to-Move Delay
                        Text(
                            text = stringResource(R.string.quick_ball_flick_to_select_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                IconToggleItem(
                                    title = stringResource(R.string.quick_ball_flick_to_select_title),
                                    description = stringResource(R.string.quick_ball_flick_to_select_desc),
                                    isChecked = viewModel.isQuickBallFlickToSelect.value,
                                    onCheckedChange = { viewModel.setQuickBallFlickToSelect(it) },
                                    iconRes = R.drawable.rounded_touch_app_24,
                                )

                                AnimatedVisibility(
                                    visible = viewModel.isQuickBallFlickToSelect.value,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        )
                                        ConfigSliderItem(
                                            title = stringResource(R.string.quick_ball_hold_to_move_duration_title),
                                            value = (viewModel.quickBallHoldToMoveDuration.longValue / 1000f),
                                            onValueChange = { viewModel.setQuickBallHoldToMoveDuration((it * 1000).toLong()) },
                                            valueRange = 1.0f..3.0f,
                                            steps = 19,
                                            icon = R.drawable.rounded_timer_24,
                                            valueFormatter = { String.format(Locale.getDefault(), "%.1fs", it) },
                                            description = stringResource(R.string.quick_ball_hold_to_move_duration_desc),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ----------------------------------------------------
                    // 3. COLORS & VISUAL STYLING
                    // ----------------------------------------------------
                    QuickBallSubMenu.STYLING -> {
                        // Harmonized Color Mode Picker
                        Text(
                            text = stringResource(R.string.quick_ball_color_mode_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                val colorModes = listOf("DYNAMIC", "PRESET", "CUSTOM")
                                val currentColorMode = viewModel.quickBallColorMode.value
                                val dynamicLabel = stringResource(R.string.quick_ball_color_mode_dynamic_short)
                                val presetLabel = stringResource(R.string.quick_ball_color_mode_preset_short)
                                val customLabel = stringResource(R.string.quick_ball_color_mode_custom_short)

                                SegmentedPicker(
                                    items = colorModes,
                                    selectedItem = currentColorMode,
                                    onItemSelected = { mode ->
                                        viewModel.setQuickBallColorMode(mode)
                                    },
                                    labelProvider = { mode ->
                                        when (mode) {
                                            "DYNAMIC" -> dynamicLabel
                                            "PRESET" -> presetLabel
                                            "CUSTOM" -> customLabel
                                            else -> mode
                                        }
                                    },
                                    iconProvider = { mode ->
                                        val iconRes = when (mode) {
                                            "DYNAMIC" -> R.drawable.rounded_auto_awesome_24
                                            "PRESET" -> R.drawable.rounded_palette_24
                                            "CUSTOM" -> R.drawable.rounded_colorize_24
                                            else -> R.drawable.rounded_palette_24
                                        }
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Text(
                                    text = when (currentColorMode) {
                                        "DYNAMIC" -> stringResource(R.string.quick_ball_color_mode_dynamic_sub)
                                        "PRESET" -> stringResource(R.string.quick_ball_color_mode_preset_sub)
                                        "CUSTOM" -> stringResource(R.string.quick_ball_color_mode_custom_sub)
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )

                                AnimatedVisibility(
                                    visible = currentColorMode == "PRESET",
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    val preset = QuickBallPresetTheme.fromName(viewModel.quickBallPresetTheme.value)
                                    val currentStyle = MenuConnectorStyle.fromName(viewModel.quickBallConnectorStyle.value)
                                    ListItem(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.small)
                                            .clickable {
                                                HapticUtil.performUIHaptic(view)
                                                showPresetThemeSheet = true
                                            },
                                        headlineContent = { Text(stringResource(R.string.quick_ball_preset_theme_title)) },
                                        supportingContent = { Text(preset.title) },
                                        leadingContent = {
                                            ConnectorStyleSample(
                                                style = currentStyle,
                                                connectorColor = Color(preset.connectorColor),
                                                itemColor = Color(preset.menuColor),
                                                ballColor = Color(preset.ballColor),
                                                itemBorderColor = Color(preset.menuBorderColor),
                                                ballBorderColor = Color(preset.ballBorderColor),
                                                itemIconColor = Color(preset.menuIconColor),
                                                ballIconColor = Color(preset.ballIconColor),
                                                modifier = Modifier.size(width = 54.dp, height = 36.dp),
                                            )
                                        },
                                        trailingContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_chevron_right_24),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ),
                                    )
                                }

                                AnimatedVisibility(
                                    visible = currentColorMode == "CUSTOM",
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                        Text(
                                            text = stringResource(R.string.quick_ball_custom_color_title),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                        )
                                        ColorSwatchPicker(
                                            selectedColorHex = viewModel.quickBallCustomColor.value,
                                            onColorSelected = { hex ->
                                                viewModel.setQuickBallCustomColor(hex)
                                            },
                                            allowAuto = false,
                                        )
                                    }
                                }
                            }
                        }

                        // Shapes & Styles
                        Text(
                            text = stringResource(R.string.quick_ball_color_section),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                val currentBallStyle = QuickBallBallStyle.fromName(viewModel.quickBallBallStyle.value)
                                SelectionCardItem(
                                    title = stringResource(R.string.quick_ball_style_title),
                                    description = stringResource(currentBallStyle.titleRes),
                                    iconRes = R.drawable.rounded_circles_24,
                                    onClick = { showBallStyleSheet = true },
                                )

                                val currentShortcutDispStyle = QuickBallShortcutStyle.fromName(viewModel.quickBallShortcutStyle.value)
                                SelectionCardItem(
                                    title = stringResource(R.string.quick_ball_shortcut_style_title),
                                    description = stringResource(currentShortcutDispStyle.titleRes),
                                    iconRes = R.drawable.rounded_apps_24,
                                    onClick = { showShortcutStyleSheet = true },
                                )

                                val currentIconPack = QuickBallIconPack.fromName(viewModel.quickBallIconPack.value)
                                SelectionCardItem(
                                    title = stringResource(R.string.quick_ball_icon_pack_title),
                                    description = stringResource(currentIconPack.titleRes),
                                    iconRes = R.drawable.rounded_palette_24,
                                    onClick = { showIconPackSheet = true },
                                )

                                val currentConnectorStyle = MenuConnectorStyle.fromName(viewModel.quickBallConnectorStyle.value)
                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable {
                                            HapticUtil.performUIHaptic(view)
                                            showConnectorSheet = true
                                        },
                                    headlineContent = { Text(stringResource(R.string.quick_ball_connector_style)) },
                                    supportingContent = { Text(stringResource(currentConnectorStyle.titleRes)) },
                                    leadingContent = {
                                        ConnectorStyleSample(
                                            style = currentConnectorStyle,
                                            connectorColor = MaterialTheme.colorScheme.primary,
                                            itemColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            ballColor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(width = 54.dp, height = 36.dp),
                                        )
                                    },
                                    trailingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_chevron_right_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                                    ),
                                )
                            }
                        }

                        // High-contrast Outlines & Borders
                        Text(
                            text = stringResource(R.string.quick_ball_border_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                IconToggleItem(
                                    title = stringResource(R.string.quick_ball_border_title),
                                    description = stringResource(R.string.quick_ball_border_desc),
                                    isChecked = viewModel.isQuickBallBorderEnabled.value,
                                    onCheckedChange = { viewModel.setQuickBallBorderEnabled(it) },
                                    iconRes = R.drawable.rounded_circles_24,
                                )

                                AnimatedVisibility(
                                    visible = viewModel.isQuickBallBorderEnabled.value,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        )
                                        ConfigSliderItem(
                                            title = stringResource(R.string.quick_ball_border_width_title),
                                            value = viewModel.quickBallBorderWidth.floatValue,
                                            onValueChange = { viewModel.setQuickBallBorderWidth(it) },
                                            valueRange = 0.5f..4.0f,
                                            steps = 7,
                                            icon = R.drawable.rounded_line_weight_24,
                                            valueFormatter = { String.format(Locale.getDefault(), "%.1fdp", it) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ----------------------------------------------------
                    // 4. DIMENSIONS & SIZING
                    // ----------------------------------------------------
                    QuickBallSubMenu.DIMENSIONS -> {
                        Text(
                            text = stringResource(R.string.quick_ball_appearance_section),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                ConfigSliderItem(
                                    title = stringResource(R.string.quick_ball_size_title),
                                    value = viewModel.quickBallSize.floatValue,
                                    onValueChange = { viewModel.setQuickBallSize(it) },
                                    valueRange = 36f..72f,
                                    steps = 35,
                                    icon = R.drawable.rounded_circle_24,
                                    valueFormatter = { "${it.roundToInt()} dp" },
                                    description = stringResource(R.string.quick_ball_size_desc),
                                )
                                ConfigSliderItem(
                                    title = stringResource(R.string.quick_ball_idle_alpha_title),
                                    value = viewModel.quickBallIdleAlpha.floatValue,
                                    onValueChange = { viewModel.setQuickBallIdleAlpha(it) },
                                    valueRange = 0.1f..1.0f,
                                    icon = R.drawable.rounded_visibility_24,
                                    valueFormatter = { String.format(Locale.getDefault(), "%.1f", it) },
                                    description = stringResource(R.string.quick_ball_idle_alpha_desc),
                                )
                                ConfigSliderItem(
                                    title = stringResource(R.string.quick_ball_radius_title),
                                    value = viewModel.quickBallMenuRadius.floatValue,
                                    onValueChange = { viewModel.setQuickBallMenuRadius(it) },
                                    valueRange = 60f..140f,
                                    icon = R.drawable.rounded_circles_24,
                                    valueFormatter = { "${it.roundToInt()} dp" },
                                    description = stringResource(R.string.quick_ball_radius_desc),
                                )
                                ConfigSliderItem(
                                    title = stringResource(R.string.quick_ball_menu_size_title),
                                    value = viewModel.quickBallMenuSize.floatValue,
                                    onValueChange = { viewModel.setQuickBallMenuSize(it) },
                                    valueRange = 40f..60f,
                                    icon = R.drawable.rounded_circle_24,
                                    valueFormatter = { "${it.roundToInt()} dp" },
                                )
                                ConfigSliderItem(
                                    title = stringResource(R.string.quick_ball_menu_icon_size_title),
                                    value = viewModel.quickBallMenuIconSize.floatValue,
                                    onValueChange = { viewModel.setQuickBallMenuIconSize(it) },
                                    valueRange = 16f..36f,
                                    icon = R.drawable.rounded_apps_24,
                                    valueFormatter = { "${it.roundToInt()} dp" },
                                )
                                ConfigSliderItem(
                                    title = stringResource(R.string.quick_ball_connector_width_title),
                                    value = viewModel.quickBallConnectorWidth.floatValue,
                                    onValueChange = { viewModel.setQuickBallConnectorWidth(it) },
                                    valueRange = 1.0f..6.0f,
                                    steps = 9,
                                    icon = R.drawable.rounded_line_weight_24,
                                    valueFormatter = { String.format(Locale.getDefault(), "%.1fdp", it) },
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.quick_ball_pill_section),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                ConfigSliderItem(
                                    title = stringResource(R.string.quick_ball_pill_height_title),
                                    value = viewModel.quickBallPillHeight.floatValue,
                                    onValueChange = { viewModel.setQuickBallPillHeight(it) },
                                    valueRange = 30f..80f,
                                    icon = R.drawable.rounded_view_headline_24,
                                    valueFormatter = { "${it.roundToInt()} dp" },
                                    description = stringResource(R.string.quick_ball_pill_height_desc),
                                )
                                ConfigSliderItem(
                                    title = stringResource(R.string.quick_ball_pill_thickness_title),
                                    value = viewModel.quickBallPillThickness.floatValue,
                                    onValueChange = { viewModel.setQuickBallPillThickness(it) },
                                    valueRange = 1.5f..6.0f,
                                    icon = R.drawable.rounded_line_weight_24,
                                    valueFormatter = { String.format(Locale.getDefault(), "%.1f dp", it) },
                                    description = stringResource(R.string.quick_ball_pill_thickness_desc),
                                )
                                ConfigSliderItem(
                                    title = stringResource(R.string.quick_ball_pill_touch_width_title),
                                    value = viewModel.quickBallPillTouchWidth.floatValue,
                                    onValueChange = { viewModel.setQuickBallPillTouchWidth(it) },
                                    valueRange = 16f..40f,
                                    icon = R.drawable.rounded_touch_app_24,
                                    valueFormatter = { "${it.roundToInt()} dp" },
                                    description = stringResource(R.string.quick_ball_pill_touch_width_desc),
                                )
                            }
                        }
                    }

                    // ----------------------------------------------------
                    // 5. BEHAVIOR & TRIGGERS
                    // ----------------------------------------------------
                    QuickBallSubMenu.BEHAVIOR -> {
                        // Collapse & HUD
                        Text(
                            text = stringResource(R.string.quick_ball_behavior_section),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                ConfigSliderItem(
                                    title = stringResource(R.string.quick_ball_stash_delay_title),
                                    value = (viewModel.quickBallStashDelay.longValue / 1000f),
                                    onValueChange = { viewModel.setQuickBallStashDelay((it * 1000).toLong()) },
                                    valueRange = 0.5f..5.0f,
                                    icon = R.drawable.rounded_timer_24,
                                    valueFormatter = { String.format(Locale.getDefault(), "%.1f s", it) },
                                    description = stringResource(R.string.quick_ball_stash_delay_desc),
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                                IconToggleItem(
                                    title = stringResource(R.string.quick_ball_action_hud_title),
                                    description = stringResource(R.string.quick_ball_action_hud_desc),
                                    isChecked = viewModel.isQuickBallActionHudEnabled.value,
                                    onCheckedChange = { viewModel.setQuickBallActionHudEnabled(it) },
                                    iconRes = R.drawable.rounded_notifications_24,
                                )
                                AnimatedVisibility(
                                    visible = viewModel.isQuickBallActionHudEnabled.value,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        ConfigSliderItem(
                                            title = stringResource(R.string.quick_ball_action_hud_duration_title),
                                            value = (viewModel.quickBallActionHudDuration.longValue / 1000f),
                                            onValueChange = { viewModel.setQuickBallActionHudDuration((it * 1000).toLong()) },
                                            valueRange = 1.0f..5.0f,
                                            icon = R.drawable.rounded_timer_24,
                                            valueFormatter = { String.format(Locale.getDefault(), "%.1f s", it) },
                                        )
                                    }
                                }
                            }
                        }

                        // Triggers & Edge Snapping
                        Text(
                            text = stringResource(R.string.settings_section_other),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                IconToggleItem(
                                    title = stringResource(R.string.quick_ball_stick_to_edge_title),
                                    description = stringResource(R.string.quick_ball_stick_to_edge_desc),
                                    isChecked = viewModel.isQuickBallStickToEdge.value,
                                    onCheckedChange = { viewModel.setQuickBallStickToEdge(it) },
                                    iconRes = R.drawable.rounded_dock_24,
                                )
                                IconToggleItem(
                                    title = stringResource(R.string.quick_ball_lock_position_title),
                                    description = stringResource(R.string.quick_ball_lock_position_desc),
                                    isChecked = viewModel.isQuickBallLockPosition.value,
                                    onCheckedChange = { viewModel.setQuickBallLockPosition(it) },
                                    iconRes = R.drawable.rounded_lock_24,
                                )
                                IconToggleItem(
                                    title = stringResource(R.string.quick_ball_hide_landscape_title),
                                    description = stringResource(R.string.quick_ball_hide_landscape_desc),
                                    isChecked = viewModel.isQuickBallHideLandscape.value,
                                    onCheckedChange = { viewModel.setQuickBallHideLandscape(it) },
                                    iconRes = R.drawable.rounded_mobile_rotate_24,
                                )
                                IconToggleItem(
                                    title = stringResource(R.string.quick_ball_show_lockscreen_title),
                                    description = stringResource(R.string.quick_ball_show_lockscreen_desc),
                                    isChecked = viewModel.isQuickBallShowLockscreen.value,
                                    onCheckedChange = { viewModel.setQuickBallShowLockscreen(it) },
                                    iconRes = R.drawable.rounded_lock_clock_24,
                                )
                            }
                        }

                        // App Exclusion
                        Text(
                            text = stringResource(R.string.quick_ball_auto_hide_apps_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            val autoHideCount = viewModel.quickBallAutoHideApps.value.size
                            SelectionCardItem(
                                title = stringResource(R.string.quick_ball_auto_hide_apps_title),
                                description = if (autoHideCount > 0) "$autoHideCount apps selected" else stringResource(R.string.quick_ball_auto_hide_apps_desc),
                                iconRes = R.drawable.rounded_apps_24,
                                onClick = { showAppSelectionSheet = true },
                            )
                        }

                        // Haptic Feedback
                        Text(
                            text = stringResource(R.string.quick_ball_haptic_intensity_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        RoundedCardContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                IconToggleItem(
                                    title = stringResource(R.string.quick_ball_vibration_title),
                                    description = stringResource(R.string.quick_ball_vibration_desc),
                                    isChecked = viewModel.isQuickBallHaptic.value,
                                    onCheckedChange = { viewModel.setQuickBallHaptic(it) },
                                    iconRes = R.drawable.rounded_mobile_vibrate_24,
                                )

                                AnimatedVisibility(
                                    visible = viewModel.isQuickBallHaptic.value,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    val currentIntensity = QuickBallHapticIntensity.fromName(viewModel.quickBallHapticIntensity.value)
                                    val hapticLevels = QuickBallHapticIntensity.entries
                                    val hapticLabels = hapticLevels.associateWith { stringResource(it.titleRes) }

                                    SegmentedPicker(
                                        items = hapticLevels,
                                        selectedItem = currentIntensity,
                                        onItemSelected = { intensity ->
                                            viewModel.setQuickBallHapticIntensity(intensity.name)
                                            when (intensity) {
                                                QuickBallHapticIntensity.LIGHT -> com.sameerasw.essentials.island.ui.IslandHaptics.touchDown(context)
                                                QuickBallHapticIntensity.MEDIUM -> com.sameerasw.essentials.island.ui.IslandHaptics.commit(context)
                                                QuickBallHapticIntensity.STRONG -> com.sameerasw.essentials.island.ui.IslandHaptics.thresholdReached(context)
                                            }
                                        },
                                        labelProvider = { intensity ->
                                            hapticLabels[intensity].orEmpty()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun QuickBallSlotRow(
    slotIndex: Int,
    action: Action,
    isDragging: Boolean,
    dragHandleModifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val appInfo = remember(action) {
        if (action is Action.OpenApp) {
            try {
                pm.getApplicationInfo(action.packageName, 0)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    val appLabel = remember(appInfo) { appInfo?.loadLabel(pm)?.toString() }
    val appIcon = remember(appInfo) { appInfo?.loadIcon(pm) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDragging) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.surfaceBright
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = dragHandleModifier,
            onClick = {},
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_drag_handle_24),
                contentDescription = stringResource(R.string.content_desc_drag_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$slotIndex",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (action is Action.OpenApp && appIcon != null) {
            Image(
                bitmap = remember(appIcon) {
                    val bmp = android.graphics.Bitmap.createBitmap(
                        appIcon.intrinsicWidth.coerceAtLeast(1),
                        appIcon.intrinsicHeight.coerceAtLeast(1),
                        android.graphics.Bitmap.Config.ARGB_8888,
                    )
                    val canvas = android.graphics.Canvas(bmp)
                    appIcon.setBounds(0, 0, canvas.width, canvas.height)
                    appIcon.draw(canvas)
                    bmp.asImageBitmap()
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        } else {
            val iconRes = QuickBallIconResolver.resolveIcon(action, QuickBallIconPack.CLASSIC)
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = appLabel ?: stringResource(action.title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_remove_24),
                    contentDescription = stringResource(R.string.quick_ball_delete_slot, slotIndex),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun QuickBallGestureRow(
    gestureTitle: String,
    action: Action?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gestureTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (action != null) stringResource(action.title) else stringResource(R.string.quick_ball_gesture_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (action != null) {
            Icon(
                painter = painterResource(id = action.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun PillStyleMiniPreview(
    style: QuickBallPillStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (style) {
            QuickBallPillStyle.MIUI_ARC -> {
                val strokeW = 2.5.dp.toPx()
                val sweep = 100f
                drawArc(
                    color = color,
                    startAngle = 130f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(w - 20.dp.toPx(), 4.dp.toPx()),
                    size = Size(24.dp.toPx(), h - 8.dp.toPx()),
                    style = Stroke(width = strokeW),
                )
            }

            QuickBallPillStyle.IOS_CAPSULE -> {
                val capW = 5.dp.toPx()
                val capH = h * 0.7f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w - capW - 4.dp.toPx(), (h - capH) / 2f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(capW / 2f, capW / 2f),
                )
            }

            QuickBallPillStyle.ANDROID_BAR -> {
                val barW = 3.5.dp.toPx()
                val barH = h * 0.6f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w - barW - 2.dp.toPx(), (h - barH) / 2f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f),
                )
            }

            QuickBallPillStyle.SAMSUNG_EDGE -> {
                val tabW = 9.dp.toPx()
                val tabH = h * 0.72f
                val top = (h - tabH) / 2f
                val left = w - tabW - 1.dp.toPx()
                drawRoundRect(
                    color = color.copy(alpha = 0.85f),
                    topLeft = Offset(left, top),
                    size = Size(tabW, tabH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
                val cx = left + tabW / 2f
                val cy = h / 2f
                val chevW = 2.5.dp.toPx()
                val chevH = 3.5.dp.toPx()
                drawLine(
                    color = Color.White,
                    start = Offset(cx + chevW / 2f, cy - chevH),
                    end = Offset(cx - chevW / 2f, cy),
                    strokeWidth = 1.5.dp.toPx(),
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cx - chevW / 2f, cy),
                    end = Offset(cx + chevW / 2f, cy + chevH),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }

            QuickBallPillStyle.ONEPLUS_SLIDER -> {
                val tabW = 7.dp.toPx()
                val tabH = h * 0.75f
                val left = w - tabW - 1.dp.toPx()
                drawRoundRect(
                    color = color.copy(alpha = 0.5f),
                    topLeft = Offset(left, (h - tabH) / 2f),
                    size = Size(tabW, tabH),
                    cornerRadius = CornerRadius(tabW / 2f, tabW / 2f),
                )
                val lineH = tabH * 0.45f
                drawLine(
                    color = color,
                    start = Offset(left + tabW / 2f, (h - lineH) / 2f),
                    end = Offset(left + tabW / 2f, (h + lineH) / 2f),
                    strokeWidth = 2.dp.toPx(),
                )
            }

            QuickBallPillStyle.NOTHING_GLYPH -> {
                val dashW = 3.5.dp.toPx()
                val left = w - dashW - 2.dp.toPx()
                val mainH = h * 0.38f
                val gap = 2.5.dp.toPx()
                val dotH = 3.dp.toPx()
                val cy = h / 2f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, cy - mainH / 2f),
                    size = Size(dashW, mainH),
                    cornerRadius = CornerRadius(dashW / 2f, dashW / 2f),
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, cy - mainH / 2f - gap - dotH),
                    size = Size(dashW, dotH),
                    cornerRadius = CornerRadius(dashW / 2f, dashW / 2f),
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, cy + mainH / 2f + gap),
                    size = Size(dashW, dotH),
                    cornerRadius = CornerRadius(dashW / 2f, dashW / 2f),
                )
            }

            QuickBallPillStyle.MINIMAL_DOT -> {
                val rad = 5.dp.toPx()
                drawArc(
                    color = color,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w - rad * 2, h / 2f - rad),
                    size = Size(rad * 2, rad * 2),
                )
            }
        }
    }
}
