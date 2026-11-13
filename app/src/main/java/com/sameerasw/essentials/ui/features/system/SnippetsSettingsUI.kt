/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - System
 * File: SnippetsSettingsUI.kt
 * Description: UI screen for managing text expansion snippets and prefix settings.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Snippet
import com.sameerasw.essentials.ui.components.StackedActionCard
import com.sameerasw.essentials.ui.core.cards.SelectionCardItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    isAddSheetVisible: Boolean = false,
    onAddSheetVisibleChange: (Boolean) -> Unit = {},
    editingSnippet: Snippet? = null,
    onEditingSnippetChange: (Snippet?) -> Unit = {},
) {
    val context = LocalContext.current
    val prefix by viewModel.snippetPrefix
    val snippets by viewModel.textSnippets
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled

    var showPrefixSheet by remember { mutableStateOf(false) }
    var showPermissionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadTextSnippets()
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!isAccessibilityEnabled) {
            RoundedCardContainer(spacing = 2.dp) {
                SelectionCardItem(
                    title = stringResource(R.string.perm_accessibility_title),
                    description = stringResource(R.string.perm_accessibility_desc_common),
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    onClick = { showPermissionSheet = true },
                )
            }
        }

        StackedActionCard(
            iconRes = R.drawable.rounded_edit_note_24,
            title = stringResource(R.string.snippets_trigger_prefix),
            description = if (prefix.isEmpty()) {
                stringResource(R.string.snippets_prefix_space_expansion)
            } else {
                stringResource(R.string.snippets_prefix_symbol_desc, prefix)
            },
            actionLabel = stringResource(R.string.snippets_action_change),
            actionIconRes = R.drawable.rounded_edit_24,
            onAction = { showPrefixSheet = true },
        )

        Text(
            text = stringResource(R.string.snippets_saved_title, snippets.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (snippets.isNotEmpty()) {
            RoundedCardContainer(spacing = 2.dp) {
                snippets.forEachIndexed { index, snippet ->
                    val triggerText = if (prefix.isNotEmpty()) "$prefix${snippet.keyword}" else "${snippet.keyword} ␣"
                    SnippetCardItem(
                        trigger = triggerText,
                        content = snippet.content,
                        title = snippet.title,
                        isFirst = index == 0,
                        isLast = index == snippets.lastIndex,
                        onClick = {
                            onEditingSnippetChange(snippet)
                            onAddSheetVisibleChange(true)
                        },
                        onDelete = { viewModel.deleteSnippet(snippet.id) },
                    )
                }
            }
        } else {
            RoundedCardContainer(spacing = 2.dp) {
                SelectionCardItem(
                    title = stringResource(R.string.snippets_empty_title),
                    description = stringResource(R.string.snippets_empty_subtitle),
                    iconRes = R.drawable.rounded_add_24,
                    onClick = {
                        onEditingSnippetChange(null)
                        onAddSheetVisibleChange(true)
                    },
                )
            }
        }
    }

    if (showPrefixSheet) {
        val options = listOf(
            Triple(";", stringResource(R.string.snippets_prefix_semicolon), ";shrug"),
            Triple(":", stringResource(R.string.snippets_prefix_colon), ":shrug"),
            Triple("@@", stringResource(R.string.snippets_prefix_at), "@@shrug"),
            Triple("", stringResource(R.string.snippets_prefix_space), "shrug ␣"),
        )

        EssentialsBottomSheet(onDismissRequest = { showPrefixSheet = false }) {
            val sheetView = LocalView.current
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.snippets_set_prefix_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.snippets_set_prefix_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                RoundedCardContainer(spacing = 2.dp) {
                    options.forEachIndexed { index, (sym, label, example) ->
                        val isSelected = prefix == sym
                        val shape = RoundedCornerShape(
                            topStart = if (index == 0) 24.dp else 4.dp,
                            topEnd = if (index == 0) 24.dp else 4.dp,
                            bottomStart = if (index == options.lastIndex) 24.dp else 4.dp,
                            bottomEnd = if (index == options.lastIndex) 24.dp else 4.dp,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(shape)
                                .clickable {
                                    HapticUtil.performUIHaptic(sheetView)
                                    viewModel.setSnippetPrefix(sym)
                                    showPrefixSheet = false
                                }
                                .background(MaterialTheme.colorScheme.surfaceBright)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    HapticUtil.performUIHaptic(sheetView)
                                    viewModel.setSnippetPrefix(sym)
                                    showPrefixSheet = false
                                },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "e.g., $example",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (isAddSheetVisible) {
        val closeSheet = {
            onAddSheetVisibleChange(false)
            onEditingSnippetChange(null)
        }
        SnippetBottomSheet(
            snippet = editingSnippet,
            existingSnippets = snippets,
            prefix = prefix,
            onDismiss = closeSheet,
            onSave = { updatedSnippet ->
                viewModel.saveSnippet(updatedSnippet)
                closeSheet()
            },
            onDelete = { snippetToDelete ->
                viewModel.deleteSnippet(snippetToDelete.id)
                closeSheet()
            },
        )
    }

    if (showPermissionSheet) {
        val permissionItems = PermissionUIHelper.getPermissionItems(
            listOf("ACCESSIBILITY"),
            context,
            viewModel,
            context as? android.app.Activity,
        )
        if (permissionItems.isNotEmpty()) {
            PermissionsBottomSheet(
                onDismissRequest = { showPermissionSheet = false },
                featureTitle = stringResource(R.string.feat_text_snippets_title),
                permissions = permissionItems,
            )
        }
    }
}

@Composable
private fun SnippetCardItem(
    trigger: String,
    content: String,
    title: String?,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 24.dp else 4.dp,
        topEnd = if (isFirst) 24.dp else 4.dp,
        bottomStart = if (isLast) 24.dp else 4.dp,
        bottomEnd = if (isLast) 24.dp else 4.dp,
    )

    ListItem(
        modifier = modifier.fillMaxWidth().clip(shape).clickable {
            HapticUtil.performUIHaptic(view)
            onClick()
        },
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = trigger,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!title.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        },
        supportingContent = {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        },
        trailingContent = {
            IconButton(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    onDelete()
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_delete_24),
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetBottomSheet(
    snippet: Snippet?,
    existingSnippets: List<Snippet>,
    prefix: String,
    onDismiss: () -> Unit,
    onSave: (Snippet) -> Unit,
    onDelete: ((Snippet) -> Unit)? = null,
) {
    var keyword by remember { mutableStateOf(snippet?.keyword ?: "") }
    var title by remember { mutableStateOf(snippet?.title ?: "") }
    var content by remember { mutableStateOf(snippet?.content ?: "") }

    val trimmedKeyword = keyword.trim()
    val hasWhitespace = keyword.any { it.isWhitespace() }
    val containsPrefix = prefix.isNotEmpty() && keyword.contains(prefix)
    val isDuplicate = existingSnippets.any {
        it.id != (snippet?.id ?: "") && it.keyword.equals(trimmedKeyword, ignoreCase = true)
    }

    val errorMessage = when {
        hasWhitespace -> stringResource(R.string.snippets_error_whitespace)
        containsPrefix -> stringResource(R.string.snippets_error_prefix)
        isDuplicate -> stringResource(R.string.snippets_error_duplicate)
        else -> null
    }
    val isSaveEnabled = errorMessage == null && trimmedKeyword.isNotEmpty() && content.isNotBlank()

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(if (snippet == null) R.string.snippets_dialog_new_title else R.string.snippets_dialog_edit_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text(stringResource(R.string.snippets_keyword_label)) },
                placeholder = { Text(stringResource(R.string.snippets_keyword_placeholder)) },
                isError = errorMessage != null,
                supportingText = if (errorMessage != null) { { Text(errorMessage) } } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.snippets_title_label)) },
                placeholder = { Text(stringResource(R.string.snippets_title_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.snippets_content_label)) },
                placeholder = { Text(stringResource(R.string.snippets_content_placeholder)) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (snippet != null && onDelete != null) {
                    TextButton(onClick = { onDelete(snippet) }) {
                        Text(text = stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.weight(1f))
                }

                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (isSaveEnabled) {
                            onSave(
                                Snippet(
                                    id = snippet?.id ?: UUID.randomUUID().toString(),
                                    keyword = trimmedKeyword,
                                    title = title.trim(),
                                    content = content,
                                ),
                            )
                        }
                    },
                    enabled = isSaveEnabled,
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
