/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - System
 * File: SnippetsSettingsUI.kt
 * Description: UI settings composable for managing text snippets and customizable prefix trigger using app core components.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Snippet
import com.sameerasw.essentials.ui.components.StackedActionCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.cards.SelectionCardItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
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
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Missing Accessibility Permission Warning
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

        // Trigger Prefix Hero Card with Wide Action Button
        StackedActionCard(
            iconRes = R.drawable.rounded_edit_note_24,
            title = stringResource(R.string.snippets_trigger_prefix),
            description =
                if (prefix.isEmpty()) {
                    stringResource(R.string.snippets_prefix_space_expansion)
                } else {
                    stringResource(R.string.snippets_prefix_symbol_desc, prefix)
                },
            actionLabel = stringResource(R.string.snippets_action_change),
            actionIconRes = R.drawable.rounded_edit_24,
            onAction = { showPrefixSheet = true },
        )

        // Snippets Section
        Text(
            text = stringResource(R.string.snippets_saved_title, snippets.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (snippets.isNotEmpty()) {
            RoundedCardContainer(spacing = 2.dp) {
                snippets.forEach { snippet ->
                    val triggerText = if (prefix.isNotEmpty()) "$prefix${snippet.keyword}" else "${snippet.keyword} ␣"
                    com.sameerasw.essentials.ui.core.cards.SnippetCardItem(
                        trigger = triggerText,
                        content = snippet.content,
                        title = snippet.title,
                        onClick = {
                            viewModel.editingSnippet.value = snippet
                            viewModel.isAddSnippetSheetVisible.value = true
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
                        viewModel.editingSnippet.value = null
                        viewModel.isAddSnippetSheetVisible.value = true
                    },
                )
            }
        }
    }

    // Set Prefix Bottom Sheet using EssentialsBottomSheet (Options Only)
    if (showPrefixSheet) {
        val options = listOf(
            Triple(";", stringResource(R.string.snippets_prefix_semicolon), ";shrug"),
            Triple(":", stringResource(R.string.snippets_prefix_colon), ":shrug"),
            Triple("@@", stringResource(R.string.snippets_prefix_at), "@@email"),
            Triple("//", stringResource(R.string.snippets_prefix_slash), "//app"),
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
                    options.forEach { (sym, label, example) ->
                        val isSelected = prefix == sym
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
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

    // Add / Edit Snippet Bottom Sheet using EssentialsBottomSheet
    if (viewModel.isAddSnippetSheetVisible.value) {
        val closeSheet = {
            viewModel.isAddSnippetSheetVisible.value = false
            viewModel.editingSnippet.value = null
        }
        SnippetBottomSheet(
            snippet = viewModel.editingSnippet.value,
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

    // Missing Permission Bottom Sheet
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetBottomSheet(
    snippet: Snippet?,
    prefix: String,
    onDismiss: () -> Unit,
    onSave: (Snippet) -> Unit,
    onDelete: ((Snippet) -> Unit)? = null,
) {
    var keyword by remember { mutableStateOf(snippet?.keyword ?: "") }
    var title by remember { mutableStateOf(snippet?.title ?: "") }
    var content by remember { mutableStateOf(snippet?.content ?: "") }

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
                onValueChange = { keyword = it.trim().filterNot { c -> c.isWhitespace() } },
                label = { Text(stringResource(R.string.snippets_keyword_label)) },
                placeholder = { Text(stringResource(R.string.snippets_keyword_placeholder)) },
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
                        if (keyword.isNotBlank() && content.isNotBlank()) {
                            onSave(Snippet(id = snippet?.id ?: System.currentTimeMillis(), keyword = keyword, title = title.trim(), content = content))
                        }
                    },
                    enabled = keyword.isNotBlank() && content.isNotBlank(),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
