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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PermissionItem
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.features.display.actions.GestureActionPickerSheet
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

    // Slot picker state: slot index (1..5) being configured, or 0 if none
    var activePickingSlot by remember { mutableIntStateOf(0) }

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

    if (activePickingSlot in 1..5) {
        val currentSlotAction =
            when (activePickingSlot) {
                1 -> viewModel.quickBallSlot1Action.value
                2 -> viewModel.quickBallSlot2Action.value
                3 -> viewModel.quickBallSlot3Action.value
                4 -> viewModel.quickBallSlot4Action.value
                5 -> viewModel.quickBallSlot5Action.value
                else -> Action.Back
            }

        GestureActionPickerSheet(
            viewModel = viewModel,
            currentAction = currentSlotAction,
            onActionSelected = { selected ->
                if (selected != null) {
                    viewModel.setQuickBallSlotAction(activePickingSlot, selected)
                }
                activePickingSlot = 0
            },
            onPickerClosed = {
                activePickingSlot = 0
            },
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FeatureCard(
            title = R.string.feat_quick_ball_title,
            description = R.string.about_desc_quick_ball,
            iconRes = R.drawable.rounded_touch_app_24,
            isEnabled = true,
            showToggle = false,
            hasMoreSettings = false,
            onToggle = {},
            onClick = {},
            modifier = Modifier.highlight(highlightSetting == QUICK_BALL_FEATURE_ID),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = stringResource(R.string.quick_ball_enable_title),
                description = stringResource(R.string.quick_ball_enable_desc),
                isChecked = viewModel.isQuickBallEnabled.value,
                onCheckedChange = { enabled ->
                    HapticUtil.performUIHaptic(view)
                    if (isAccessibilityEnabled) {
                        viewModel.setQuickBallEnabled(enabled)
                    } else {
                        showPermissionSheet = true
                    }
                },
                iconRes = R.drawable.rounded_touch_app_24,
                modifier = Modifier.highlight(highlightSetting == "quick_ball_enabled"),
            )
        }

        AnimatedVisibility(
            visible = viewModel.isQuickBallEnabled.value,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Shortcut Slots Section
                Text(
                    text = stringResource(R.string.quick_ball_slots_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                RoundedCardContainer {
                    val slots =
                        listOf(
                            Triple(1, R.string.quick_ball_slot_1, viewModel.quickBallSlot1Action.value),
                            Triple(2, R.string.quick_ball_slot_2, viewModel.quickBallSlot2Action.value),
                            Triple(3, R.string.quick_ball_slot_3, viewModel.quickBallSlot3Action.value),
                            Triple(4, R.string.quick_ball_slot_4, viewModel.quickBallSlot4Action.value),
                            Triple(5, R.string.quick_ball_slot_5, viewModel.quickBallSlot5Action.value),
                        )

                    slots.forEachIndexed { index, (slotNum, titleRes, currentAction) ->
                        QuickBallSlotRow(
                            slotLabel = stringResource(titleRes),
                            action = currentAction,
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                activePickingSlot = slotNum
                            },
                        )
                        if (index < slots.lastIndex) {
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    }
                }

                // Appearance & Behavior Section
                Text(
                    text = stringResource(R.string.quick_ball_appearance_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                RoundedCardContainer {
                    ConfigSliderItem(
                        title = stringResource(R.string.quick_ball_size_title),
                        value = viewModel.quickBallSize.floatValue,
                        onValueChange = { viewModel.setQuickBallSize(it) },
                        valueRange = 36f..72f,
                        increment = 4f,
                        iconRes = R.drawable.rounded_grid_view_24,
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.quick_ball_idle_alpha_title),
                        value = viewModel.quickBallIdleAlpha.floatValue,
                        onValueChange = { viewModel.setQuickBallIdleAlpha(it) },
                        valueRange = 0.15f..1.0f,
                        increment = 0.05f,
                        iconRes = R.drawable.rounded_visibility_24,
                    )

                    IconToggleItem(
                        title = stringResource(R.string.quick_ball_auto_hide_title),
                        description = stringResource(R.string.quick_ball_auto_hide_desc),
                        isChecked = viewModel.isQuickBallAutoHide.value,
                        onCheckedChange = { checked ->
                            HapticUtil.performUIHaptic(view)
                            viewModel.setQuickBallAutoHide(checked)
                        },
                        iconRes = R.drawable.rounded_chevron_backward_24,
                    )

                    IconToggleItem(
                        title = stringResource(R.string.quick_ball_vibration_title),
                        description = stringResource(R.string.quick_ball_vibration_desc),
                        isChecked = viewModel.isQuickBallHaptic.value,
                        onCheckedChange = { checked ->
                            HapticUtil.performUIHaptic(view)
                            viewModel.setQuickBallHaptic(checked)
                        },
                        iconRes = R.drawable.rounded_mobile_vibrate_24,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickBallSlotRow(
    slotLabel: String,
    action: Action,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slotLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(id = action.title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(id = action.icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Icon(
                painter = painterResource(id = R.drawable.rounded_chevron_backward_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
