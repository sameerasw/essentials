/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Automation
 * File: DIYScreen.kt
 * Description: UI component and settings composable for Automation feature domain.
 */

package com.sameerasw.essentials.ui.composables


import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.activities.AutomationEditorActivity
import com.sameerasw.essentials.ui.components.diy.AutomationItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.NewAutomationSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.DIYViewModel

@Composable
fun DIYScreen(
    modifier: Modifier = Modifier,
    viewModel: DIYViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showNewAutomationSheet: Boolean = false,
    onDismissNewAutomationSheet: () -> Unit = {},
    onNewAutomationClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val automations by viewModel.automations.collectAsState()
    val focusManager = LocalFocusManager.current

    var showGenAIPill by remember { mutableStateOf(false) }
    val genAIState by viewModel.genAIState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (automations.isEmpty()) {
                val view = androidx.compose.ui.platform.LocalView.current
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No automations yet"
                        )
                        if (onNewAutomationClick != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onNewAutomationClick()
                                }
                            ) {
                                Text(stringResource(R.string.action_new_automation))
                            }
                        }
                    }
                }
            } else {
                val (enabledAutomations, disabledAutomations) = remember(automations) {
                    automations.partition { it.isEnabled }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        bottom = contentPadding.calculateBottomPadding(),
                        start = 16.dp,
                        end = 16.dp
                    )
                ) {
                    item {
                        Spacer(modifier = Modifier.height(contentPadding.calculateTopPadding()))
                    }
                    if (enabledAutomations.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.label_enabled),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item {
                            RoundedCardContainer {
                                enabledAutomations.forEach { automation ->
                                    AutomationItem(
                                        automation = automation,
                                        onClick = {
                                            context.startActivity(
                                                AutomationEditorActivity.createIntent(
                                                    context,
                                                    automation.id
                                                )
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
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (disabledAutomations.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.label_disabled),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item {
                            RoundedCardContainer {
                                disabledAutomations.forEach { automation ->
                                    AutomationItem(
                                        automation = automation,
                                        onClick = {
                                            context.startActivity(
                                                AutomationEditorActivity.createIntent(
                                                    context,
                                                    automation.id
                                                )
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
                                        }
                                    )
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
                isGenAILoading = genAIState is com.sameerasw.essentials.viewmodels.GenAIState.Loading
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
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        when (val state = genAIState) {
            is com.sameerasw.essentials.viewmodels.GenAIState.Error -> {
                androidx.compose.runtime.LaunchedEffect(state) {
                    android.widget.Toast.makeText(
                        context,
                        state.message,
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    viewModel.dismissGenAISuggestion()
                }
            }

            else -> {}
        }
    }
}



