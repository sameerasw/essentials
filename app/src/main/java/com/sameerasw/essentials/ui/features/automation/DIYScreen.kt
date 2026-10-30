/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Automation
 * File: DIYScreen.kt
 * Description: UI component and settings composable for Automation feature domain.
 */

package com.sameerasw.essentials.ui.composables

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sameerasw.essentials.ui.components.rememberPullTitleHeaderState
import com.sameerasw.essentials.ui.components.PullTitleHeader
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import androidx.compose.foundation.lazy.rememberLazyListState
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.ui.activities.AutomationEditorActivity
import com.sameerasw.essentials.ui.components.diy.AutomationItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.NewAutomationSheet
import com.sameerasw.essentials.ui.modifiers.scrollMotionBlur
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.DIYViewModel

@Composable
fun DIYScreen(
    modifier: Modifier = Modifier,
    viewModel: DIYViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showNewAutomationSheet: Boolean = false,
    onDismissNewAutomationSheet: () -> Unit = {},
    onNewAutomationClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val automations by viewModel.automations.collectAsState()
    val focusManager = LocalFocusManager.current

    var showGenAIPill by remember { mutableStateOf(false) }
    val genAIState by viewModel.genAIState.collectAsState()

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    LaunchedEffect(automations) {
        val ids = automations.map { it.id }.toSet()
        selectedIds = selectedIds intersect ids
        if (automations.isEmpty()) exitSelection()
    }

    BackHandler(enabled = selectionMode) { exitSelection() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    },
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            if (automations.isEmpty()) {
                val view = androidx.compose.ui.platform.LocalView.current
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No automations yet",
                        )
                        if (onNewAutomationClick != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onNewAutomationClick()
                                },
                            ) {
                                Text(stringResource(R.string.action_new_automation))
                            }
                        }
                    }
                }
            } else {
                val (enabledAutomations, disabledAutomations) =
                    remember(automations) {
                        automations.partition { it.isEnabled }
                    }
                val isMotionBlurEnabled =
                    remember(context) {
                        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                            .getBoolean("motion_blur", false)
                    }
                val lazyListState = rememberLazyListState()
                val view = androidx.compose.ui.platform.LocalView.current
                val headerState = rememberPullTitleHeaderState()

                @Composable
                fun AutomationRow(automation: Automation) {
                    AutomationItem(
                        automation = automation,
                        onClick = {
                            context.startActivity(
                                AutomationEditorActivity.createIntent(
                                    context,
                                    automation.id,
                                ),
                            )
                        },
                        onDelete = {
                            viewModel.deleteAutomation(automation.id)
                        },
                        onToggle = {
                            viewModel.toggleAutomation(automation.id)
                        },
                        onTest = {
                            viewModel.testAutomation(automation)
                        },
                        selectionMode = selectionMode,
                        selected = automation.id in selectedIds,
                        onSelectedChange = { checked ->
                            selectionMode = true
                            selectedIds =
                                if (checked) selectedIds + automation.id else selectedIds - automation.id
                        },
                    )
                }

                LazyColumn(
                    state = lazyListState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(headerState.nestedScrollConnection)
                            .scrollMotionBlur(lazyListState, enabled = isMotionBlurEnabled),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding =
                        PaddingValues(
                            top = contentPadding.calculateTopPadding(),
                            bottom = contentPadding.calculateBottomPadding(),
                            start = 16.dp,
                            end = 16.dp,
                        ),
                ) {
                    item {
                        Column {
                            PullTitleHeader(
                                title = stringResource(R.string.tab_diy),
                                state = headerState,
                                caption = stringResource(R.string.diy_header_caption),
                            )
                            AnimatedVisibility(
                                visible = selectionMode,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                val selected = automations.filter { it.id in selectedIds }
                                val hasEnabled = selected.any { it.isEnabled }
                                val hasDisabled = selected.any { !it.isEnabled }
                                Column(
                                    modifier = Modifier.padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.diy_selected_count, selected.size),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(start = 16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        SelectionActionButton(
                                            icon = R.drawable.rounded_close_24,
                                            text = stringResource(R.string.action_cancel),
                                            outlined = true,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            exitSelection()
                                        }
                                        if (hasEnabled) {
                                            SelectionActionButton(
                                                icon = R.drawable.rounded_do_not_disturb_on_24,
                                                text = stringResource(R.string.action_disable),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                viewModel.setAutomationsEnabled(selectedIds, false)
                                            }
                                        }
                                        if (hasDisabled) {
                                            SelectionActionButton(
                                                icon = R.drawable.rounded_check_24,
                                                text = stringResource(R.string.action_enable),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                viewModel.setAutomationsEnabled(selectedIds, true)
                                            }
                                        }
                                        SelectionActionButton(
                                            icon = R.drawable.rounded_delete_24,
                                            text = stringResource(R.string.action_delete),
                                            enabled = selected.isNotEmpty(),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            viewModel.deleteAutomations(selectedIds)
                                            exitSelection()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (enabledAutomations.isNotEmpty()) {
                        item {
                            RoundedCardContainer {
                                enabledAutomations.forEach { automation ->
                                    AutomationRow(automation)
                                }
                            }
                        }
                    }

                    if (disabledAutomations.isNotEmpty()) {
                        item {
                            var isDisabledExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                            val chevronRotation by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isDisabledExpanded) 180f else 0f,
                                label = "disabled_chevron",
                            )
                            Column {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.extraLarge)
                                            .clickable {
                                                HapticUtil.performUIHaptic(view)
                                                isDisabledExpanded = !isDisabledExpanded
                                            }.padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.label_disabled)} (${disabledAutomations.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                                    )
                                }
                                AnimatedVisibility(
                                    visible = isDisabledExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut(),
                                ) {
                                    RoundedCardContainer(modifier = Modifier.padding(top = 8.dp)) {
                                        disabledAutomations.forEach { automation ->
                                            AutomationRow(automation)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showNewAutomationSheet) {
            NewAutomationSheet(
                onDismiss = onDismissNewAutomationSheet,
                onOptionSelected = { type ->
                    onDismissNewAutomationSheet()
                    context.startActivity(AutomationEditorActivity.createIntent(context, type))
                },
                onAIDescribeRequested = {
                    showGenAIPill = true
                },
                isGenAILoading = genAIState is com.sameerasw.essentials.viewmodels.GenAIState.Loading,
            )
        }

        if (showGenAIPill) {
            val currentSuggestion =
                (genAIState as? com.sameerasw.essentials.viewmodels.GenAIState.Success)?.suggestion
            com.sameerasw.essentials.ui.components.genai.GenAIFloatingPill(
                onSend = { prompt ->
                    viewModel.requestGenAISuggestion(prompt, context)
                },
                onDismiss = {
                    showGenAIPill = false
                    viewModel.dismissGenAISuggestion()
                },
                onConfirm = { suggestion ->
                    viewModel.confirmGenAISuggestion(suggestion)
                    showGenAIPill = false
                },
                onReset = {
                    viewModel.dismissGenAISuggestion()
                },
                isLoading = genAIState is com.sameerasw.essentials.viewmodels.GenAIState.Loading,
                suggestion = currentSuggestion,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        when (val state = genAIState) {
            is com.sameerasw.essentials.viewmodels.GenAIState.Error -> {
                androidx.compose.runtime.LaunchedEffect(state) {
                    android.widget.Toast
                        .makeText(
                            context,
                            state.message,
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    viewModel.dismissGenAISuggestion()
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun SelectionActionButton(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val content: @Composable () -> Unit = {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(
            text = text,
            maxLines = 1,
            modifier = Modifier.basicMarquee(),
        )
    }
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) { content() }
    } else {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) { content() }
    }
}
