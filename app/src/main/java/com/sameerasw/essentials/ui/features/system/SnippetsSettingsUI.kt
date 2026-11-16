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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Snippet
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
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
    val isEnabled by viewModel.isTextSnippetsEnabled
    val snippets by viewModel.textSnippets
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled

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

        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                iconRes = R.drawable.rounded_edit_note_24,
                title = stringResource(R.string.feat_text_snippets_title),
                isChecked = isEnabled,
                onCheckedChange = { checked ->
                    if (checked && !isAccessibilityEnabled) {
                        showPermissionSheet = true
                    } else {
                        viewModel.setTextSnippetsEnabled(checked)
                    }
                },
            )
            SnippetPrefixPicker(
                selected = prefix,
                onSelected = { viewModel.setSnippetPrefix(it) },
            )
        }

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

    val view = LocalView.current

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TranslatableText(
                stringResId = if (snippet == null) R.string.snippets_dialog_new_title else R.string.snippets_dialog_edit_title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            RoundedCardContainer(spacing = 2.dp) {
                SnippetFieldCard(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = stringResource(R.string.snippets_keyword_label),
                    placeholder = stringResource(R.string.snippets_keyword_placeholder),
                    isError = errorMessage != null,
                    supportingText = errorMessage,
                    singleLine = true,
                    isFirst = true,
                    isLast = false,
                )
                SnippetFieldCard(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.snippets_title_label),
                    placeholder = stringResource(R.string.snippets_title_placeholder),
                    singleLine = true,
                    isFirst = false,
                    isLast = false,
                )
                SnippetFieldCard(
                    value = content,
                    onValueChange = { content = it },
                    label = stringResource(R.string.snippets_content_label),
                    placeholder = stringResource(R.string.snippets_content_placeholder),
                    singleLine = false,
                    isFirst = false,
                    isLast = true,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    TranslatableText(R.string.action_cancel)
                }
                Button(
                    onClick = {
                        HapticUtil.performMediumHaptic(view)
                        onSave(
                            Snippet(
                                id = snippet?.id ?: UUID.randomUUID().toString(),
                                keyword = trimmedKeyword,
                                title = title.trim(),
                                content = content,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isSaveEnabled,
                ) {
                    TranslatableText(R.string.action_save)
                }
            }

            if (snippet != null && onDelete != null) {
                OutlinedButton(
                    onClick = {
                        HapticUtil.performMediumHaptic(view)
                        onDelete(snippet)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    TranslatableText(R.string.action_delete)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SnippetPrefixPicker(
    selected: String,
    onSelected: (String) -> Unit,
) {
    val view = LocalView.current
    val options = listOf(";", ":", "@@", "")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.snippets_trigger_prefix),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            options.forEachIndexed { index, symbol ->
                ToggleButton(
                    checked = selected == symbol,
                    onCheckedChange = {
                        HapticUtil.performLightHaptic(view)
                        onSelected(symbol)
                    },
                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                ) {
                    Text(text = symbol.ifEmpty { "␣" }, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SnippetFieldCard(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 24.dp else 4.dp,
        topEnd = if (isFirst) 24.dp else 4.dp,
        bottomStart = if (isLast) 24.dp else 4.dp,
        bottomEnd = if (isLast) 24.dp else 4.dp,
    )

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().clip(shape),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        maxLines = if (singleLine) 1 else 6,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
            errorContainerColor = MaterialTheme.colorScheme.surfaceBright,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        ),
    )
}
