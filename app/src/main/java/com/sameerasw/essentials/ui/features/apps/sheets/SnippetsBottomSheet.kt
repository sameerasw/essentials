/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Apps (IME Snippets)
 * File: SnippetsBottomSheet.kt
 * Description: BottomSheet UI for managing, adding, editing, and deleting Raycast-style text snippets.
 */

package com.sameerasw.essentials.ui.features.apps.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ime.snippets.Snippet
import com.sameerasw.essentials.ime.snippets.SnippetRepository
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SnippetsBottomSheet(
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val repository = remember { SnippetRepository.getInstance(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val snippets by repository.snippets.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var universalEnabled by remember { mutableStateOf(settingsRepository.isSnippetsUniversalEnabled()) }
    var autoExpandEnabled by remember { mutableStateOf(settingsRepository.isSnippetsUniversalAutoExpandEnabled()) }
    var floatingPillEnabled by remember { mutableStateOf(settingsRepository.isSnippetsUniversalFloatingPillEnabled()) }

    var searchQuery by remember { mutableStateOf("") }
    var editingSnippet by remember { mutableStateOf<Snippet?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var deletingSnippet by remember { mutableStateOf<Snippet?>(null) }

    val filteredSnippets =
        remember(snippets, searchQuery) {
            val query = searchQuery.trim()
            if (query.isEmpty()) {
                snippets
            } else {
                snippets.filter {
                    it.keyword.contains(query, ignoreCase = true) ||
                        it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
                }
            }
        }

    // Delete Confirmation Dialog
    deletingSnippet?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingSnippet = null },
            title = { Text(stringResource(R.string.snippets_delete)) },
            text = { Text(stringResource(R.string.snippets_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        repository.deleteSnippet(target.id)
                        deletingSnippet = null
                    },
                ) {
                    Text(stringResource(R.string.snippets_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSnippet = null }) {
                    Text(stringResource(R.string.snippets_cancel))
                }
            },
        )
    }

    // Add / Edit Dialog
    if (isCreatingNew || editingSnippet != null) {
        val initial = editingSnippet
        SnippetEditDialog(
            snippet = initial,
            onDismiss = {
                isCreatingNew = false
                editingSnippet = null
            },
            onSave = { savedSnippet ->
                HapticUtil.performVirtualKeyHaptic(view)
                if (initial == null) {
                    repository.addSnippet(savedSnippet)
                } else {
                    repository.updateSnippet(savedSnippet)
                }
                isCreatingNew = false
                editingSnippet = null
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header: Title + Add Button
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.snippets_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                FilledTonalButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        isCreatingNew = true
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_add_24),
                        contentDescription = stringResource(R.string.snippets_add),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.snippets_add))
                }
            }

            // Universal External Keyboard Support (Gboard, SwiftKey, etc.)
            RoundedCardContainer(spacing = 2.dp) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.snippets_universal_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.snippets_universal_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = universalEnabled,
                            onCheckedChange = { checked ->
                                HapticUtil.performVirtualKeyHaptic(view)
                                universalEnabled = checked
                                settingsRepository.setSnippetsUniversalEnabled(checked)
                            },
                        )
                    }

                    AnimatedVisibility(visible = universalEnabled) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Option A: Instant Auto-expand
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.snippets_universal_auto_expand),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = stringResource(R.string.snippets_universal_auto_expand_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = autoExpandEnabled,
                                    onCheckedChange = { checked ->
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        autoExpandEnabled = checked
                                        settingsRepository.setSnippetsUniversalAutoExpandEnabled(checked)
                                    },
                                )
                            }

                            // Option B: Floating Pill Overlay
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.snippets_universal_floating_pill),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = stringResource(R.string.snippets_universal_floating_pill_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = floatingPillEnabled,
                                    onCheckedChange = { checked ->
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        floatingPillEnabled = checked
                                        settingsRepository.setSnippetsUniversalFloatingPillEnabled(checked)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                placeholder = { Text(stringResource(R.string.snippets_search_hint)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.rounded_search_24),
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_close_24),
                                contentDescription = "Clear",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                    ),
            )

            // Snippet List
            if (filteredSnippets.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.snippets_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredSnippets, key = { it.id }) { snippet ->
                        RoundedCardContainer(spacing = 2.dp) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceBright)
                                        .clickable { editingSnippet = snippet }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        // Keyword Chip
                                        Box(
                                            modifier =
                                                Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                        ) {
                                            Text(
                                                text = snippet.keyword,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            )
                                        }

                                        Text(
                                            text = snippet.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    Text(
                                        text = snippet.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )

                                    if (snippet.autoExpandOnSpace) {
                                        Text(
                                            text = "⚡ " + stringResource(R.string.snippets_auto_expand),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(
                                        onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            editingSnippet = snippet
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_edit_24),
                                            contentDescription = stringResource(R.string.snippets_edit),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            deletingSnippet = snippet
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_delete_24),
                                            contentDescription = stringResource(R.string.snippets_delete),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SnippetEditDialog(
    snippet: Snippet?,
    onDismiss: () -> Unit,
    onSave: (Snippet) -> Unit,
) {
    var title by remember { mutableStateOf(snippet?.title.orEmpty()) }
    var keyword by remember { mutableStateOf(snippet?.keyword.orEmpty()) }
    var content by remember { mutableStateOf(snippet?.content.orEmpty()) }
    var autoExpandOnSpace by remember { mutableStateOf(snippet?.autoExpandOnSpace ?: true) }

    val isValid = keyword.isNotBlank() && content.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    if (snippet == null) {
                        stringResource(R.string.snippets_add)
                    } else {
                        stringResource(R.string.snippets_edit)
                    },
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.snippets_snippet_title)) },
                    placeholder = { Text("e.g. My Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.snippets_keyword)) },
                    placeholder = { Text("e.g. !email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.snippets_content)) },
                    placeholder = { Text("Expanded snippet text") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                // Quick Variable Chips
                Text(
                    text = stringResource(R.string.snippets_quick_insert),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val variables = listOf("{date}", "{time}", "{clipboard}")
                    variables.forEach { placeholder ->
                        AssistChip(
                            onClick = { content += placeholder },
                            label = { Text(placeholder) },
                            colors =
                                AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }

                // Auto-expand Toggle
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.snippets_auto_expand),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.snippets_auto_expand_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = autoExpandOnSpace,
                        onCheckedChange = { autoExpandOnSpace = it },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        val result =
                            snippet?.copy(
                                title = title.ifBlank { keyword },
                                keyword = keyword.trim(),
                                content = content,
                                autoExpandOnSpace = autoExpandOnSpace,
                            ) ?: Snippet(
                                title = title.ifBlank { keyword },
                                keyword = keyword.trim(),
                                content = content,
                                autoExpandOnSpace = autoExpandOnSpace,
                            )
                        onSave(result)
                    }
                },
                enabled = isValid,
            ) {
                Text(stringResource(R.string.snippets_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.snippets_cancel))
            }
        },
    )
}
