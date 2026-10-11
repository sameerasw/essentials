/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: System Features
 * File: LockscreenShortcutsSettingsUI.kt
 * Description: Settings screen for Essentials lock screen shortcut buttons.
 */

package com.sameerasw.essentials.ui.features.system

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionRegistry
import com.sameerasw.essentials.domain.model.ActivityIconSource
import com.sameerasw.essentials.domain.model.LockscreenShortcutSide
import com.sameerasw.essentials.domain.model.SystemShortcutsState
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.cards.SelectionCardItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.ActivityIconSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.ActivityLauncherUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun LockscreenShortcutsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var selectedSide by remember { mutableStateOf(LockscreenShortcutSide.LEFT) }
    var showIconSheet by remember { mutableStateOf(false) }

    val systemState = viewModel.lockscreenSystemShortcutsState.value
    val isEnabled = viewModel.isLockscreenShortcutsEnabled.value
    val statusText =
        stringResource(
            when (systemState) {
                SystemShortcutsState.UNKNOWN -> R.string.lockscreen_shortcuts_status_unknown
                SystemShortcutsState.PRESENT -> R.string.lockscreen_shortcuts_status_present
                SystemShortcutsState.NONE -> R.string.lockscreen_shortcuts_status_none
            },
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (systemState == SystemShortcutsState.PRESENT) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                if (systemState == SystemShortcutsState.PRESENT) {
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val intent = Intent(Intent.ACTION_SET_WALLPAPER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            val pixelIntent = Intent(intent).setPackage(PIXEL_WALLPAPER_PACKAGE)
                            try {
                                context.startActivity(
                                    if (pixelIntent.resolveActivity(context.packageManager) != null) pixelIntent else intent,
                                )
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text(stringResource(R.string.lockscreen_shortcuts_open_wallpaper_style))
                    }
                }
            }
        }

        // Only blocked once system shortcuts are found; until the lock screen is checked the buttons just stay hidden
        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_lock_portrait_24,
                title = stringResource(R.string.lockscreen_shortcuts_enable_title),
                isChecked = isEnabled,
                onCheckedChange = { viewModel.setLockscreenShortcutsEnabled(it) },
                enabled = isEnabled || systemState != SystemShortcutsState.PRESENT,
                onDisabledClick = { Toast.makeText(context, statusText, Toast.LENGTH_LONG).show() },
                modifier = Modifier.highlight(highlightSetting == "enable_lockscreen_shortcuts"),
            )
        }

        AnimatedVisibility(
            visible = isEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RoundedCardContainer {
                    val sideLabels =
                        mapOf(
                            LockscreenShortcutSide.LEFT to stringResource(R.string.lockscreen_shortcut_left),
                            LockscreenShortcutSide.RIGHT to stringResource(R.string.lockscreen_shortcut_right),
                        )
                    SegmentedPicker(
                        items = LockscreenShortcutSide.entries,
                        selectedItem = selectedSide,
                        onItemSelected = {
                            HapticUtil.performUIHaptic(view)
                            selectedSide = it
                        },
                        labelProvider = { sideLabels.getValue(it) },
                    )
                }

                ActionSequenceEditor(
                    viewModel = viewModel,
                    actions = viewModel.lockscreenShortcutActions[selectedSide].orEmpty().take(1),
                    onActionsChange = { actions ->
                        // Drop the side's saved image once it's no longer used
                        val oldPath = (viewModel.lockscreenShortcutActions[selectedSide]?.firstOrNull() as? Action.OpenActivity)?.customIconPath
                        val newPath = (actions.firstOrNull() as? Action.OpenActivity)?.customIconPath
                        if (oldPath != null && oldPath != newPath) ActivityLauncherUtil.deleteCustomIcon(oldPath)
                        viewModel.setLockscreenShortcutActions(selectedSide, actions)
                    },
                    screenOnOnly = true,
                    listKey = selectedSide,
                    emptyText = stringResource(R.string.lockscreen_shortcuts_no_actions),
                    maxActions = 1,
                    categories = remember { ActionRegistry.getLockscreenCategories() },
                )

                (viewModel.lockscreenShortcutActions[selectedSide]?.firstOrNull() as? Action.OpenActivity)?.let { action ->
                    RoundedCardContainer {
                        SelectionCardItem(
                            title = stringResource(R.string.activity_icon_title),
                            description = stringResource(ActivityLauncherUtil.iconSourceOf(action).title),
                            iconRes = R.drawable.rounded_image_24,
                            onClick = { showIconSheet = true },
                        )
                    }
                }
            }
        }

        RoundedCardContainer {
            Text(
                text = stringResource(R.string.lockscreen_shortcuts_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val iconAction = viewModel.lockscreenShortcutActions[selectedSide]?.firstOrNull() as? Action.OpenActivity
    if (showIconSheet && iconAction != null) {
        val side = selectedSide
        ActivityIconSheet(
            action = iconAction,
            confirmLabel = stringResource(R.string.action_save),
            onDismiss = { showIconSheet = false },
            onConfirm = { source, icon ->
                // A new file name each time, so the lock screen notices the change and reloads the icon
                val path =
                    if (source == ActivityIconSource.CUSTOM) {
                        ActivityLauncherUtil.saveCustomIcon(context, icon, "lockscreen_${side.name.lowercase()}_${System.currentTimeMillis()}")
                    } else {
                        null
                    }
                if (source == ActivityIconSource.CUSTOM && path == null) {
                    Toast.makeText(context, R.string.activity_icon_load_failed, Toast.LENGTH_SHORT).show()
                    return@ActivityIconSheet
                }
                if (path != iconAction.customIconPath) ActivityLauncherUtil.deleteCustomIcon(iconAction.customIconPath)
                viewModel.setLockscreenShortcutActions(
                    side,
                    listOf(iconAction.copy(iconSource = source, customIconPath = path.orEmpty())),
                )
                showIconSheet = false
            },
        )
    }
}

private const val PIXEL_WALLPAPER_PACKAGE = "com.google.android.apps.wallpaper"
