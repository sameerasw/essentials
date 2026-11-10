/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - System
 * File: QuickBallSettingsUI.kt
 * Description: UI settings composable for configuring the Quick Ball assistive touch feature.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import com.sameerasw.essentials.ui.features.system.quickball.DraggableItem
import com.sameerasw.essentials.ui.features.system.quickball.draggableContainer
import com.sameerasw.essentials.ui.features.system.quickball.rememberDragDropState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.cards.SelectionCardItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.ColorSwatchPicker
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionItem
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.features.display.actions.GestureActionPickerSheet
import com.sameerasw.essentials.ui.features.system.quickball.ConnectorStylePreview
import com.sameerasw.essentials.ui.features.system.quickball.ConnectorStyleSample
import com.sameerasw.essentials.ui.features.system.quickball.MenuConnectorStyle
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallBallStyle
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallColorMode
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallHapticIntensity
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallHaptics
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallIconPack
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallIconResolver
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallMenuPreview
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallPresetTheme
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallPresetThemeSheet
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallConnectorSheet
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallShortcutStyle
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallTheme
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

const val QUICK_BALL_FEATURE_ID = "feat_quick_ball"

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

    // Dialog & Picker States
    var activePickingSlot by remember { mutableIntStateOf(0) }
    var slotPendingAppSelection by remember { mutableIntStateOf(0) }
    var activePickingGesture by remember { mutableStateOf<String?>(null) }
    var showConnectorSheet by remember { mutableStateOf(false) }
    var showPresetThemeSheet by remember { mutableStateOf(false) }
    var showBallStyleSheet by remember { mutableStateOf(false) }
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
            onLoadApps = { ctx ->
                viewModel.quickBallAutoHideApps.value.map { AppSelection(it, true) }
            },
            onSaveApps = { ctx, apps ->
                val selectedPackages = apps.filter { it.isEnabled }.map { it.packageName }.toSet()
                viewModel.setQuickBallAutoHideApps(selectedPackages)
            },
        )
    }

    val isEnabled = viewModel.isQuickBallEnabled.value
    val slotCount = viewModel.quickBallSlotCount.intValue

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

                // SECTION 1: Shortcuts & Radial Menu
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

                RoundedCardContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
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

                        val resolvedColors = QuickBallTheme.resolveColors(context, com.sameerasw.essentials.data.repository.SettingsRepository(context))

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
                            modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        val listState = rememberLazyListState()
                        val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
                            if (fromIndex != toIndex) {
                                HapticUtil.performUIHaptic(view)
                                viewModel.reorderQuickBallSlots(fromIndex + 1, toIndex + 1)
                            }
                        }

                        val rowHeight = 52.dp
                        val listHeight = rowHeight * slotCount

                        LazyColumn(
                            state = listState,
                            userScrollEnabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(listHeight)
                                .draggableContainer(
                                    dragDropState = dragDropState,
                                    handleWidthPx = with(LocalDensity.current) { 56.dp.toPx() },
                                ),
                        ) {
                            items(slotCount, key = { it }) { index ->
                                val i = index + 1
                                val action = currentActions[index]
                                DraggableItem(dragDropState = dragDropState, index = index) { isDragging ->
                                    QuickBallSlotRow(
                                        slotIndex = i,
                                        action = action,
                                        isDragging = isDragging,
                                        onClick = {
                                            HapticUtil.performUIHaptic(view)
                                            activePickingSlot = i
                                        },
                                        onDelete = if (slotCount > 2) {
                                            {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                viewModel.deleteQuickBallSlot(i)
                                            }
                                        } else null,
                                    )
                                }
                            }
                        }

                        // Add Slot Button
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

                // SECTION 2: Pill Gestures
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

                        IconToggleItem(
                            title = stringResource(R.string.quick_ball_flick_to_select_title),
                            description = stringResource(R.string.quick_ball_flick_to_select_desc),
                            isChecked = viewModel.isQuickBallFlickToSelect.value,
                            onCheckedChange = { viewModel.setQuickBallFlickToSelect(it) },
                            iconRes = R.drawable.rounded_touch_app_24,
                        )

                        AnimatedVisibility(
                            visible = viewModel.isQuickBallGesturesEnabled.value,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

                // SECTION 3: Colors & Styling
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
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val currentMode = viewModel.quickBallColorMode.value
                        val isDynamic = currentMode == "DYNAMIC"
                        val isPreset = currentMode == "PRESET"
                        val isCustom = currentMode == "CUSTOM"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilterChip(
                                selected = isDynamic,
                                onClick = {
                                    HapticUtil.performUIHaptic(view)
                                    viewModel.setQuickBallColorMode("DYNAMIC")
                                },
                                label = { Text(stringResource(R.string.quick_ball_color_mode_dynamic)) },
                                leadingIcon = if (isDynamic) {
                                    {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_check_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else null,
                                modifier = Modifier.weight(1f),
                            )

                            FilterChip(
                                selected = isPreset,
                                onClick = {
                                    HapticUtil.performUIHaptic(view)
                                    viewModel.setQuickBallColorMode("PRESET")
                                },
                                label = { Text(stringResource(R.string.quick_ball_color_mode_preset)) },
                                leadingIcon = if (isPreset) {
                                    {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_check_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else null,
                                modifier = Modifier.weight(1f),
                            )

                            FilterChip(
                                selected = isCustom,
                                onClick = {
                                    HapticUtil.performUIHaptic(view)
                                    viewModel.setQuickBallColorMode("CUSTOM")
                                },
                                label = { Text(stringResource(R.string.quick_ball_color_mode_custom)) },
                                leadingIcon = if (isCustom) {
                                    {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_check_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else null,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        AnimatedVisibility(
                            visible = isPreset,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            val preset = QuickBallPresetTheme.fromName(viewModel.quickBallPresetTheme.value)
                            val connectorStyle = MenuConnectorStyle.fromName(viewModel.quickBallConnectorStyle.value)
                            Column(modifier = Modifier.padding(top = 4.dp)) {
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
                                            style = connectorStyle,
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
                        }

                        AnimatedVisibility(
                            visible = isCustom,
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

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        // Ball Style Selector
                        val currentBallStyleName = viewModel.quickBallBallStyle.value
                        val currentBallStyle = QuickBallBallStyle.fromName(currentBallStyleName)
                        SelectionCardItem(
                            title = stringResource(R.string.quick_ball_style_title),
                            description = stringResource(currentBallStyle.titleRes),
                            iconRes = R.drawable.rounded_circles_24,
                            onClick = { showBallStyleSheet = true },
                        )

                        // Shortcut Display Style Selector
                        val currentShortcutStyle = QuickBallShortcutStyle.fromName(viewModel.quickBallShortcutStyle.value)
                        SelectionCardItem(
                            title = stringResource(R.string.quick_ball_shortcut_style_title),
                            description = stringResource(currentShortcutStyle.titleRes),
                            iconRes = R.drawable.rounded_apps_24,
                            onClick = { showShortcutStyleSheet = true },
                        )

                        // System Icon Pack Selector
                        val currentIconPack = QuickBallIconPack.fromName(viewModel.quickBallIconPack.value)
                        SelectionCardItem(
                            title = stringResource(R.string.quick_ball_icon_pack_title),
                            description = stringResource(currentIconPack.titleRes),
                            iconRes = R.drawable.rounded_palette_24,
                            onClick = { showIconPackSheet = true },
                        )

                        // Connector Style Selector with live Canvas sample preview
                        val currentStyleName = viewModel.quickBallConnectorStyle.value
                        val currentStyle = MenuConnectorStyle.fromName(currentStyleName)
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    HapticUtil.performUIHaptic(view)
                                    showConnectorSheet = true
                                },
                            headlineContent = { Text(stringResource(R.string.quick_ball_connector_style)) },
                            supportingContent = { Text(stringResource(currentStyle.titleRes)) },
                            leadingContent = {
                                ConnectorStyleSample(
                                    style = currentStyle,
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
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        )

                        // Connector Line Width Slider
                        ConfigSliderItem(
                            title = stringResource(R.string.quick_ball_connector_width_title),
                            value = viewModel.quickBallConnectorWidth.floatValue,
                            onValueChange = { viewModel.setQuickBallConnectorWidth(it) },
                            valueRange = 1.0f..4.0f,
                            icon = R.drawable.rounded_line_weight_24,
                            valueFormatter = { "%.1f dp".format(it) },
                        )

                        // Button & Ball Borders Toggle
                        IconToggleItem(
                            title = stringResource(R.string.quick_ball_border_title),
                            description = stringResource(R.string.quick_ball_border_desc),
                            isChecked = viewModel.isQuickBallBorderEnabled.value,
                            onCheckedChange = { viewModel.setQuickBallBorderEnabled(it) },
                            iconRes = R.drawable.rounded_target_24,
                        )

                        AnimatedVisibility(
                            visible = viewModel.isQuickBallBorderEnabled.value,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            ConfigSliderItem(
                                title = stringResource(R.string.quick_ball_border_width_title),
                                value = viewModel.quickBallBorderWidth.floatValue,
                                onValueChange = { viewModel.setQuickBallBorderWidth(it) },
                                valueRange = 1.0f..4.0f,
                                icon = R.drawable.rounded_line_weight_24,
                                valueFormatter = { "%.1f dp".format(it) },
                            )
                        }
                    }
                }

                // SECTION 4: Dimensions & Geometry
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
                            icon = R.drawable.rounded_grid_view_24,
                            valueFormatter = { "%.0f dp".format(it) },
                            modifier = Modifier.highlight(highlightSetting == "quick_ball_size"),
                        )

                        ConfigSliderItem(
                            title = stringResource(R.string.quick_ball_idle_alpha_title),
                            value = viewModel.quickBallIdleAlpha.floatValue,
                            onValueChange = { viewModel.setQuickBallIdleAlpha(it) },
                            valueRange = 0.1f..1.0f,
                            icon = R.drawable.rounded_visibility_24,
                            valueFormatter = { "%.1f".format(it) },
                            modifier = Modifier.highlight(highlightSetting == "quick_ball_idle_alpha"),
                        )

                        // Menu Arc Radius Slider
                        ConfigSliderItem(
                            title = stringResource(R.string.quick_ball_radius_title),
                            value = viewModel.quickBallMenuRadius.floatValue,
                            onValueChange = { viewModel.setQuickBallMenuRadius(it) },
                            valueRange = 60f..140f,
                            icon = R.drawable.rounded_circles_24,
                            valueFormatter = { "%.0f dp".format(it) },
                        )

                        // Shortcut Button Size Slider
                        ConfigSliderItem(
                            title = stringResource(R.string.quick_ball_menu_size_title),
                            value = viewModel.quickBallMenuSize.floatValue,
                            onValueChange = { viewModel.setQuickBallMenuSize(it) },
                            valueRange = 40f..60f,
                            icon = R.drawable.rounded_circle_24,
                            valueFormatter = { "%.0f dp".format(it) },
                        )

                        // Shortcut Icon Size Slider
                        ConfigSliderItem(
                            title = stringResource(R.string.quick_ball_menu_icon_size_title),
                            value = viewModel.quickBallMenuIconSize.floatValue,
                            onValueChange = { viewModel.setQuickBallMenuIconSize(it) },
                            valueRange = 16f..36f,
                            icon = R.drawable.rounded_apps_24,
                            valueFormatter = { "%.0f dp".format(it) },
                        )

                        // Auto-Collapse Delay Slider
                        ConfigSliderItem(
                            title = stringResource(R.string.quick_ball_stash_delay_title),
                            value = (viewModel.quickBallStashDelay.longValue / 1000f),
                            onValueChange = { viewModel.setQuickBallStashDelay((it * 1000).toLong()) },
                            valueRange = 0.5f..5.0f,
                            icon = R.drawable.rounded_timer_24,
                            valueFormatter = { "%.1f s".format(it) },
                        )

                        // Stashed Pill Dimensions
                        ConfigSliderItem(
                            title = stringResource(R.string.quick_ball_pill_height_title),
                            value = viewModel.quickBallPillHeight.floatValue,
                            onValueChange = { viewModel.setQuickBallPillHeight(it) },
                            valueRange = 30f..80f,
                            icon = R.drawable.rounded_view_headline_24,
                            valueFormatter = { "%.0f dp".format(it) },
                        )

                        ConfigSliderItem(
                            title = stringResource(R.string.quick_ball_pill_thickness_title),
                            value = viewModel.quickBallPillThickness.floatValue,
                            onValueChange = { viewModel.setQuickBallPillThickness(it) },
                            valueRange = 1.5f..6.0f,
                            icon = R.drawable.rounded_line_weight_24,
                            valueFormatter = { "%.1f dp".format(it) },
                        )

                        ConfigSliderItem(
                            title = stringResource(R.string.quick_ball_pill_touch_width_title),
                            value = viewModel.quickBallPillTouchWidth.floatValue,
                            onValueChange = { viewModel.setQuickBallPillTouchWidth(it) },
                            valueRange = 16f..40f,
                            icon = R.drawable.rounded_touch_app_24,
                            valueFormatter = { "%.0f dp".format(it) },
                        )
                    }
                }

                // SECTION 5: Haptic Feedback
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
                            val currentIntensity = viewModel.quickBallHapticIntensity.value

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                QuickBallHapticIntensity.entries.forEach { intensity ->
                                    val isSelected = intensity.name.equals(currentIntensity, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setQuickBallHapticIntensity(intensity.name)
                                            when (intensity) {
                                                QuickBallHapticIntensity.LIGHT -> com.sameerasw.essentials.island.ui.IslandHaptics.touchDown(context)
                                                QuickBallHapticIntensity.MEDIUM -> com.sameerasw.essentials.island.ui.IslandHaptics.commit(context)
                                                QuickBallHapticIntensity.STRONG -> com.sameerasw.essentials.island.ui.IslandHaptics.thresholdReached(context)
                                            }
                                        },
                                        label = { Text(stringResource(intensity.titleRes)) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    painter = painterResource(R.drawable.rounded_check_24),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 4: Behavior
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
                            ConfigSliderItem(
                                title = stringResource(R.string.quick_ball_action_hud_duration_title),
                                value = (viewModel.quickBallActionHudDuration.longValue / 1000f),
                                onValueChange = { viewModel.setQuickBallActionHudDuration((it * 1000).toLong()) },
                                valueRange = 1.0f..5.0f,
                                icon = R.drawable.rounded_timer_24,
                                valueFormatter = { "%.1f s".format(it) },
                            )
                        }

                        val autoHideCount = viewModel.quickBallAutoHideApps.value.size
                        SelectionCardItem(
                            title = stringResource(R.string.quick_ball_auto_hide_apps_title),
                            description = if (autoHideCount > 0) "$autoHideCount apps selected" else stringResource(R.string.quick_ball_auto_hide_apps_desc),
                            iconRes = R.drawable.rounded_apps_24,
                            onClick = { showAppSelectionSheet = true },
                        )
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
    isDragging: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    var appLabel by remember(action) { mutableStateOf<String?>(null) }
    var appIconDrawable by remember(action) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(action) {
        if (action is Action.OpenApp && action.packageName.isNotBlank()) {
            try {
                val pm = context.packageManager
                val info = pm.getApplicationInfo(action.packageName, 0)
                appLabel = pm.getApplicationLabel(info).toString()
                appIconDrawable = pm.getApplicationIcon(info)
            } catch (_: Exception) {
                appLabel = null
                appIconDrawable = null
            }
        } else {
            appLabel = null
            appIconDrawable = null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.rounded_drag_handle_24),
            contentDescription = null,
            tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        val iconDrawable = appIconDrawable
        if (iconDrawable != null) {
            Image(
                bitmap = remember(iconDrawable) {
                    val bmp = android.graphics.Bitmap.createBitmap(
                        iconDrawable.intrinsicWidth.coerceAtLeast(1),
                        iconDrawable.intrinsicHeight.coerceAtLeast(1),
                        android.graphics.Bitmap.Config.ARGB_8888,
                    )
                    val canvas = android.graphics.Canvas(bmp)
                    iconDrawable.setBounds(0, 0, canvas.width, canvas.height)
                    iconDrawable.draw(canvas)
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

        Spacer(modifier = Modifier.width(14.dp))

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
