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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.sameerasw.essentials.ui.features.system.quickball.MenuConnectorStyle
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallBallStyle
import com.sameerasw.essentials.ui.features.system.quickball.QuickBallColorMode
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
    var showBallStyleSheet by remember { mutableStateOf(false) }
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

    // Connector style sheet
    if (showConnectorSheet) {
        EssentialsBottomSheet(
            onDismissRequest = { showConnectorSheet = false },
        ) {
            Text(
                text = stringResource(R.string.quick_ball_connector_style),
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
                MenuConnectorStyle.entries.forEach { style ->
                    val isSelected = viewModel.quickBallConnectorStyle.value == style.name
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable {
                                HapticUtil.performUIHaptic(view)
                                viewModel.setQuickBallConnectorStyle(style.name)
                                showConnectorSheet = false
                            },
                        headlineContent = {
                            Text(
                                text = stringResource(style.titleRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        supportingContent = if (isSelected) {
                            { Text("Selected", color = MaterialTheme.colorScheme.primary) }
                        } else null,
                        leadingContent = {
                            ConnectorStylePreview(
                                style = style,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                Text(
                    text = stringResource(R.string.quick_ball_slots_section),
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
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (i in 1..slotCount) {
                            val action = when (i) {
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

                            val slotLabel = when (i) {
                                1 -> stringResource(R.string.quick_ball_slot_1)
                                2 -> stringResource(R.string.quick_ball_slot_2)
                                3 -> stringResource(R.string.quick_ball_slot_3)
                                4 -> stringResource(R.string.quick_ball_slot_4)
                                5 -> stringResource(R.string.quick_ball_slot_5)
                                else -> stringResource(R.string.quick_ball_slot_n, i)
                            }

                            QuickBallSlotRow(
                                slotLabel = slotLabel,
                                slotIndex = i,
                                action = action,
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Connector Style Selector
                        val currentStyleName = viewModel.quickBallConnectorStyle.value
                        val currentStyle = MenuConnectorStyle.fromName(currentStyleName)
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.extraSmall)
                                .clickable {
                                    HapticUtil.performUIHaptic(view)
                                    showConnectorSheet = true
                                },
                            headlineContent = { Text(stringResource(R.string.quick_ball_connector_style)) },
                            supportingContent = { Text(stringResource(currentStyle.titleRes)) },
                            leadingContent = {
                                ConnectorStylePreview(
                                    style = currentStyle,
                                    color = MaterialTheme.colorScheme.primary,
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
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                        )

                        // Menu Radius Slider
                        ConfigSliderItem(
                            title = stringResource(R.string.quick_ball_menu_radius_title),
                            value = viewModel.quickBallMenuRadius.floatValue,
                            onValueChange = { viewModel.setQuickBallMenuRadius(it) },
                            valueRange = 60f..130f,
                            icon = R.drawable.rounded_target_24,
                            valueFormatter = { "%.0f dp".format(it) },
                            modifier = Modifier.highlight(highlightSetting == "quick_ball_menu_radius"),
                        )

                        // Button Size Slider
                        ConfigSliderItem(
                            title = stringResource(R.string.quick_ball_menu_size_title),
                            value = viewModel.quickBallMenuSize.floatValue,
                            onValueChange = { viewModel.setQuickBallMenuSize(it) },
                            valueRange = 36f..60f,
                            icon = R.drawable.rounded_circle_24,
                            valueFormatter = { "%.0f dp".format(it) },
                            modifier = Modifier.highlight(highlightSetting == "quick_ball_menu_size"),
                        )
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

                // SECTION 3: Appearance
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

                        // Ball Style Selector
                        val currentBallStyleName = viewModel.quickBallBallStyle.value
                        val currentBallStyle = QuickBallBallStyle.fromName(currentBallStyleName)
                        SelectionCardItem(
                            title = stringResource(R.string.quick_ball_style_title),
                            description = stringResource(currentBallStyle.titleRes),
                            iconRes = R.drawable.rounded_circles_24,
                            onClick = { showBallStyleSheet = true },
                        )

                        IconToggleItem(
                            title = stringResource(R.string.quick_ball_vibration_title),
                            description = stringResource(R.string.quick_ball_vibration_desc),
                            isChecked = viewModel.isQuickBallHaptic.value,
                            onCheckedChange = { viewModel.setQuickBallHaptic(it) },
                            iconRes = R.drawable.rounded_mobile_vibrate_24,
                        )
                    }
                }

                // SECTION: Colors & Styling
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
                        val isDynamic = viewModel.quickBallColorMode.value == "DYNAMIC"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                selected = !isDynamic,
                                onClick = {
                                    HapticUtil.performUIHaptic(view)
                                    viewModel.setQuickBallColorMode("CUSTOM")
                                },
                                label = { Text(stringResource(R.string.quick_ball_color_mode_custom)) },
                                leadingIcon = if (!isDynamic) {
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
                            visible = !isDynamic,
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
    slotLabel: String,
    slotIndex: Int,
    action: Action,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slotLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (appLabel != null) {
                    "${stringResource(action.title)} ($appLabel)"
                } else {
                    stringResource(action.title)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            Icon(
                painter = painterResource(id = action.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        if (onDelete != null) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_remove_24),
                    contentDescription = stringResource(R.string.quick_ball_delete_slot, slotIndex),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
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
