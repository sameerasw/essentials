/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Apps (IME Snippets)
 * File: SnippetsBottomSheet.kt
 * Description: BottomSheet UI for managing, adding, editing, and deleting Raycast-style text snippets
 * adhering to Essentials' core design system (EssentialsBottomSheet, RoundedCardContainer, IconToggleItem).
 */

package com.sameerasw.essentials.ui.features.apps.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetsBottomSheet(
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val repository = remember { SnippetRepository.getInstance(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val snippets by repository.snippets.collectAsState()

    var universalEnabled by remember { mutableStateOf(settingsRepository.isSnippetsUniversalEnabled()) }
    var autoExpandEnabled by remember { mutableStateOf(settingsRepository.isSnippetsUniversalAutoExpandEnabled()) }
    var floatingPillEnabled by remember { mutableStateOf(settingsRepository.isSnippetsUniversalFloatingPillEnabled()) }
    var suggestionDisplayMode by remember { mutableStateOf(settingsRepository.getSnippetsSuggestionDisplayMode()) }
    var isDisplayStyleSheetVisible by remember { mutableStateOf(false) }

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

    // Add / Edit Bottom Sheet
    if (isCreatingNew || editingSnippet != null) {
        val initial = editingSnippet
        SnippetEditSheet(
            snippet = initial,
            onDismissRequest = {
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

    if (isDisplayStyleSheetVisible) {
        SnippetDisplayStyleSheet(
            currentMode = suggestionDisplayMode,
            onModeSelected = { newMode ->
                suggestionDisplayMode = newMode
                settingsRepository.setSnippetsSuggestionDisplayMode(newMode)
            },
            onDismissRequest = { isDisplayStyleSheetVisible = false },
        )
    }

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Title
            item(key = "header_title") {
                Text(
                    text = stringResource(R.string.snippets_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Top Action: Add Snippet Card (Matches UserDictionaryBottomSheet pattern)
            item(key = "add_snippet_action") {
                RoundedCardContainer(spacing = 2.dp) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceBright)
                                .clickable {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    isCreatingNew = true
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_add_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.snippets_add),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.desc_keyboard_snippets),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Settings: External Keyboard Support Group
            item(key = "external_keyboard_settings") {
                RoundedCardContainer(spacing = 2.dp) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_keyboard_24,
                        title = stringResource(R.string.snippets_universal_title),
                        description = stringResource(R.string.snippets_universal_desc),
                        isChecked = universalEnabled,
                        onCheckedChange = { checked ->
                            universalEnabled = checked
                            settingsRepository.setSnippetsUniversalEnabled(checked)
                        },
                    )

                    if (universalEnabled) {
                        IconToggleItem(
                            iconRes = R.drawable.rounded_bolt_24,
                            title = stringResource(R.string.snippets_universal_auto_expand),
                            description = stringResource(R.string.snippets_universal_auto_expand_desc),
                            isChecked = autoExpandEnabled,
                            onCheckedChange = { checked ->
                                autoExpandEnabled = checked
                                settingsRepository.setSnippetsUniversalAutoExpandEnabled(checked)
                            },
                        )

                        val modeLabel = when (suggestionDisplayMode) {
                            SettingsRepository.SNIPPETS_DISPLAY_DYNAMIC_ISLAND -> stringResource(R.string.snippets_display_island_title)
                            SettingsRepository.SNIPPETS_DISPLAY_DUO -> stringResource(R.string.snippets_display_duo_title)
                            SettingsRepository.SNIPPETS_DISPLAY_BOTH -> stringResource(R.string.snippets_display_both_title)
                            else -> stringResource(R.string.snippets_display_pill_title)
                        }
                        val modeIcon = when (suggestionDisplayMode) {
                            SettingsRepository.SNIPPETS_DISPLAY_DYNAMIC_ISLAND -> R.drawable.rounded_bolt_24
                            SettingsRepository.SNIPPETS_DISPLAY_DUO -> R.drawable.rounded_circle_24
                            SettingsRepository.SNIPPETS_DISPLAY_BOTH -> R.drawable.rounded_auto_awesome_24
                            else -> R.drawable.rounded_keyboard_24
                        }

                        IconToggleItem(
                            iconRes = modeIcon,
                            title = stringResource(R.string.snippets_display_style_title),
                            description = modeLabel,
                            isChecked = false,
                            showToggle = false,
                            onClick = { isDisplayStyleSheetVisible = true },
                            onCheckedChange = { isDisplayStyleSheetVisible = true },
                        )
                    }
                }
            }

            // Search Bar
            item(key = "search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
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
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                        ),
                )
            }

            // Snippets List
            if (filteredSnippets.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.snippets_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item(key = "snippets_group") {
                    RoundedCardContainer(spacing = 2.dp) {
                        filteredSnippets.forEach { snippet ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceBright)
                                        .clickable {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            editingSnippet = snippet
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                            color = MaterialTheme.colorScheme.primary,
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
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SnippetEditSheet(
    snippet: Snippet?,
    onDismissRequest: () -> Unit,
    onSave: (Snippet) -> Unit,
) {
    val view = LocalView.current
    var title by remember { mutableStateOf(snippet?.title.orEmpty()) }
    var keyword by remember { mutableStateOf(snippet?.keyword.orEmpty()) }
    var content by remember { mutableStateOf(snippet?.content.orEmpty()) }
    var autoExpandOnSpace by remember { mutableStateOf(snippet?.autoExpandOnSpace ?: true) }

    val isValid = keyword.isNotBlank() && content.isNotBlank()

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text =
                    if (snippet == null) {
                        stringResource(R.string.snippets_add)
                    } else {
                        stringResource(R.string.snippets_edit)
                    },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

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

            // Variable Insert Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                content += placeholder
                            },
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
            }

            // Auto-expand Toggle using standard RoundedCardContainer + IconToggleItem
            RoundedCardContainer(spacing = 2.dp) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_bolt_24,
                    title = stringResource(R.string.snippets_auto_expand),
                    description = stringResource(R.string.snippets_auto_expand_desc),
                    isChecked = autoExpandOnSpace,
                    onCheckedChange = { autoExpandOnSpace = it },
                )
            }

            // Save Button
            Button(
                onClick = {
                    if (isValid) {
                        HapticUtil.performVirtualKeyHaptic(view)
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.snippets_save),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetDisplayStyleSheet(
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val recommendedMode = remember { SettingsRepository(context).getRecommendedSnippetsSuggestionDisplayMode() }

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.snippets_display_style_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.snippets_display_style_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            RoundedCardContainer(spacing = 2.dp) {
                val options =
                    listOf(
                        Triple(
                            SettingsRepository.SNIPPETS_DISPLAY_FLOATING_PILL,
                            Pair(
                                stringResource(R.string.snippets_display_pill_title),
                                stringResource(R.string.snippets_display_pill_desc),
                            ),
                            R.drawable.rounded_keyboard_24,
                        ),
                        Triple(
                            SettingsRepository.SNIPPETS_DISPLAY_DYNAMIC_ISLAND,
                            Pair(
                                stringResource(R.string.snippets_display_island_title),
                                stringResource(R.string.snippets_display_island_desc),
                            ),
                            R.drawable.rounded_bolt_24,
                        ),
                        Triple(
                            SettingsRepository.SNIPPETS_DISPLAY_DUO,
                            Pair(
                                stringResource(R.string.snippets_display_duo_title),
                                stringResource(R.string.snippets_display_duo_desc),
                            ),
                            R.drawable.rounded_circle_24,
                        ),
                        Triple(
                            SettingsRepository.SNIPPETS_DISPLAY_BOTH,
                            Pair(
                                stringResource(R.string.snippets_display_both_title),
                                stringResource(R.string.snippets_display_both_desc),
                            ),
                            R.drawable.rounded_auto_awesome_24,
                        ),
                    )

                options.forEach { (mode, textPair, iconRes) ->
                    val isSelected = currentMode == mode
                    val isRecommended = mode == recommendedMode
                    val title = if (isRecommended) "${textPair.first} (${stringResource(R.string.snippets_recommended_tag)})" else textPair.first
                    IconToggleItem(
                        iconRes = iconRes,
                        title = title,
                        description = textPair.second,
                        isChecked = isSelected,
                        showToggle = false,
                        trailingContent =
                            if (isSelected) {
                                {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_check_24),
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onModeSelected(mode)
                            onDismissRequest()
                        },
                        onCheckedChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onModeSelected(mode)
                            onDismissRequest()
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
