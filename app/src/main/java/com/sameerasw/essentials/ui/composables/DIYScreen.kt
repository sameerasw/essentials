package com.sameerasw.essentials.ui.composables

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.diy.AutomationItem
import com.sameerasw.essentials.ui.components.sheets.NewAutomationSheet
import com.sameerasw.essentials.viewmodels.DIYViewModel

@Composable
fun DIYScreen(
    modifier: Modifier = Modifier,
    viewModel: DIYViewModel = viewModel(),
    showNewAutomationSheet: Boolean = false,
    onDismissNewAutomationSheet: () -> Unit = {}
) {
    val context = LocalContext.current
    val automations by viewModel.automations.collectAsState()
    val focusManager = LocalFocusManager.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (automations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No automations yet"
                    )
                }
            } else {
                val (enabledAutomations, disabledAutomations) = remember(automations) {
                    automations.partition { it.isEnabled }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 150.dp)
                ) {
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
                                        }
                                    )
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
            }
        )
    }
}
