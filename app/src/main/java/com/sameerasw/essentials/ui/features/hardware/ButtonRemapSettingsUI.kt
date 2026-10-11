/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Hardware Features
 * File: ButtonRemapSettingsUI.kt
 * Description: Composable screen for remapping volume and hardware keys with unified DIY actions.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.RemapInput
import com.sameerasw.essentials.domain.model.RemapScreenState
import com.sameerasw.essentials.domain.model.RemapSlot
import com.sameerasw.essentials.shizuku.ShizukuPermissionHelper
import com.sameerasw.essentials.shizuku.ShizukuStatus
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.HapticFeedbackPicker
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonRemapSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var selectedScreenTab by remember { mutableIntStateOf(0) } // 0: Off, 1: On
    var selectedButtonTab by remember { mutableIntStateOf(0) } // 0: Up, 1: Down

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var shizukuStatus by remember { mutableStateOf(ShizukuStatus.NOT_RUNNING) }
    val shizukuHelper = remember { ShizukuPermissionHelper(context) }

    // Check Shizuku status on resume
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    shizukuStatus = shizukuHelper.getStatus()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentSlot =
        RemapSlot(
            input = if (selectedButtonTab == 0) RemapInput.VOLUME_UP else RemapInput.VOLUME_DOWN,
            screenState = if (selectedScreenTab == 0) RemapScreenState.OFF else RemapScreenState.ON,
        )
    val currentActions: List<Action> = viewModel.remapActions[currentSlot].orEmpty()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Master Toggle
        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                iconRes = R.drawable.rounded_switch_access_3_24,
                title = stringResource(R.string.button_remap_enable_title),
                isChecked = viewModel.isButtonRemapEnabled.value,
                onCheckedChange = { viewModel.setButtonRemapEnabled(it, context) },
                modifier = Modifier.highlight(highlightSetting == "enable_remap"),
            )

            AnimatedVisibility(
                visible = viewModel.isButtonRemapEnabled.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_volume_up_24,
                        title = stringResource(R.string.button_remap_pause_on_volume_dialog_title),
                        isChecked = viewModel.isButtonRemapPauseOnVolumeDialog.value,
                        onCheckedChange = { viewModel.setButtonRemapPauseOnVolumeDialog(it, context) },
                        modifier = Modifier.highlight(highlightSetting == "pause_on_volume_dialog"),
                    )

                    val isRootEnabled =
                        com.sameerasw.essentials.utils.ShellUtils
                            .isRootEnabled(context)
                    IconToggleItem(
                        iconRes = if (isRootEnabled) R.drawable.rounded_numbers_24 else R.drawable.rounded_adb_24,
                        title = stringResource(R.string.button_remap_use_shizuku_title),
                        description = stringResource(R.string.button_remap_use_shizuku_desc),
                        isChecked = viewModel.isButtonRemapUseShizuku.value,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val shellHasPermission =
                                    com.sameerasw.essentials.utils.ShellUtils
                                        .hasPermission(context)
                                val shellIsAvailable =
                                    com.sameerasw.essentials.utils.ShellUtils
                                        .isAvailable(context)

                                if (shellHasPermission) {
                                    viewModel.setButtonRemapUseShizuku(true, context)
                                } else if (shellIsAvailable && !isRootEnabled) {
                                    shizukuHelper.requestPermission { _, grantResult ->
                                        if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            viewModel.setButtonRemapUseShizuku(true, context)
                                        }
                                    }
                                } else if (isRootEnabled && !shellHasPermission) {
                                    viewModel.setButtonRemapUseShizuku(true, context)
                                    com.sameerasw.essentials.utils.ShellUtils.runCommand(
                                        context,
                                        "id",
                                    )
                                } else {
                                    viewModel.setButtonRemapUseShizuku(true, context)
                                    val toastRes =
                                        if (isRootEnabled) R.string.root_not_available_toast else R.string.shizuku_not_running_toast
                                    android.widget.Toast
                                        .makeText(
                                            context,
                                            context.getString(toastRes),
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            } else {
                                viewModel.setButtonRemapUseShizuku(false, context)
                            }
                        },
                        modifier = Modifier.highlight(highlightSetting == "shizuku_remap"),
                    )

                    AnimatedVisibility(
                        visible = viewModel.isButtonRemapUseShizuku.value,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceBright,
                                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                                    ).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val shellAvailable =
                                com.sameerasw.essentials.utils.ShellUtils
                                    .isAvailable(context)
                            val shellPermission =
                                com.sameerasw.essentials.utils.ShellUtils
                                    .hasPermission(context)

                            val statusText =
                                if (shellPermission && viewModel.shizukuDetectedDevicePath.value != null) {
                                    stringResource(
                                        R.string.shizuku_detected_prefix,
                                        viewModel.shizukuDetectedDevicePath.value ?: "",
                                    )
                                } else if (isRootEnabled) {
                                    if (shellPermission) {
                                        "Root Access: Granted"
                                    } else if (shellAvailable) {
                                        "Root Access: Found"
                                    } else {
                                        "Root Access: Not Found"
                                    }
                                } else {
                                    stringResource(
                                        R.string.shizuku_status_prefix,
                                        shizukuStatus.name,
                                    )
                                }

                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (shellPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )

                            if (!shellPermission &&
                                !isRootEnabled &&
                                shizukuStatus != ShizukuStatus.READY &&
                                shizukuStatus != ShizukuStatus.PERMISSION_NEEDED
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val shizukuPackage =
                                                com.sameerasw.essentials.utils.ShizukuUtils
                                                    .getShizukuPackageName(context)
                                            val intent =
                                                context.packageManager.getLaunchIntentForPackage(shizukuPackage)
                                            if (intent != null) context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding =
                                        androidx.compose.foundation.layout.PaddingValues(
                                            horizontal = 12.dp,
                                            vertical = 0.dp,
                                        ),
                                ) {
                                    Text(
                                        stringResource(R.string.shizuku_open_button),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = viewModel.isButtonRemapEnabled.value,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Haptic Feedback (Common)
                Text(
                    text = stringResource(R.string.settings_section_haptic),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RoundedCardContainer(spacing = 0.dp) {
                    HapticFeedbackPicker(
                        selectedFeedback = viewModel.remapHapticType.value,
                        onFeedbackSelected = { viewModel.setRemapHapticType(it, context) },
                        options =
                            listOf(
                                R.string.haptic_none to HapticFeedbackType.NONE,
                                R.string.haptic_tick to HapticFeedbackType.TICK,
                                R.string.haptic_double to HapticFeedbackType.DOUBLE,
                            ),
                        modifier = Modifier.highlight(highlightSetting == "remap_haptic"),
                    )
                }

                Text(
                    text = stringResource(R.string.button_remap_section_long_press),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Button Picker & Tabs
                RoundedCardContainer(spacing = 2.dp) {
                    val screenOptions =
                        listOf(
                            stringResource(R.string.screen_off),
                            stringResource(R.string.screen_on),
                        )
                    SegmentedPicker(
                        items = screenOptions,
                        selectedItem = if (selectedScreenTab == 0) screenOptions[0] else screenOptions[1],
                        onItemSelected = {
                            HapticUtil.performUIHaptic(view)
                            selectedScreenTab = screenOptions.indexOf(it)
                        },
                        labelProvider = { it },
                    )
                    val buttonOptions =
                        listOf(
                            stringResource(R.string.volume_up),
                            stringResource(R.string.volume_down),
                        )
                    SegmentedPicker(
                        items = buttonOptions,
                        selectedItem = if (selectedButtonTab == 0) buttonOptions[0] else buttonOptions[1],
                        onItemSelected = {
                            HapticUtil.performUIHaptic(view)
                            selectedButtonTab = buttonOptions.indexOf(it)
                        },
                        labelProvider = { it },
                    )
                }

                ActionSequenceEditor(
                    viewModel = viewModel,
                    actions = currentActions,
                    onActionsChange = { viewModel.setRemapActions(currentSlot, it) },
                    screenOnOnly = selectedScreenTab == 1,
                    listKey = currentSlot,
                    emptyText = stringResource(R.string.button_remap_no_actions),
                )
            }
        }

        // Hint
        RoundedCardContainer {
            Text(
                text =
                    if (selectedScreenTab == 0) {
                        stringResource(R.string.button_remap_screen_off_hint)
                    } else {
                        stringResource(R.string.button_remap_screen_on_hint)
                    },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RemapActionItem(
    title: Any, // Can be Int or String
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasSettings: Boolean = false,
    onSettingsClick: () -> Unit = {},
) {
    val view = LocalView.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable {
                    HapticUtil.performUIHaptic(view)
                    onClick()
                }.background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                ).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
        )

        val resolvedTitle =
            when (title) {
                is Int -> stringResource(id = title)
                is String -> title
                else -> ""
            }

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = resolvedTitle,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = resolvedTitle,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasSettings && isSelected) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_settings_24),
                    contentDescription = stringResource(R.string.content_desc_settings),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

