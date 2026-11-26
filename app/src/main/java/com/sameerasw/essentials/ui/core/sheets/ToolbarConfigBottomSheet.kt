/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: ToolbarConfigBottomSheet.kt
 * Description: Bottom sheet for reordering and hiding main toolbar tabs.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.DIYTabs
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.rememberDeviceUserFirstName
import com.sameerasw.essentials.viewmodels.MainViewModel
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val HIDDEN_DIVIDER = "hidden_divider"

private fun segmentShape(
    index: Int,
    count: Int,
): Shape {
    val top = if (index == 0) 24.dp else 4.dp
    val bottom = if (index == count - 1) 24.dp else 4.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarConfigBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current
    val lazyListState = rememberLazyListState()

    val entries =
        remember {
            val visible = viewModel.toolbarTabs.value
            mutableStateListOf<Any>().apply {
                addAll(visible)
                add(HIDDEN_DIVIDER)
                addAll(DIYTabs.MOVABLE.filter { it !in visible })
            }
        }

    val dividerIndex = entries.indexOf(HIDDEN_DIVIDER)
    val visibleTabs = entries.subList(0, dividerIndex).filterIsInstance<DIYTabs>()
    val hiddenTabs = entries.subList(dividerIndex + 1, entries.size).filterIsInstance<DIYTabs>()
    val canShowLabels = visibleTabs.size + 2 <= DIYTabs.MAX_LABELED_TOOLBAR_ITEMS
    var showLabelsInfo by remember { mutableStateOf(false) }
    val deviceUserName = rememberDeviceUserFirstName()

    if (showLabelsInfo) {
        ToolbarLabelsInfoBottomSheet(onDismissRequest = { showLabelsInfo = false })
    }

    fun commit() {
        val index = entries.indexOf(HIDDEN_DIVIDER)
        viewModel.setToolbarTabs(entries.subList(0, index).filterIsInstance<DIYTabs>())
    }

    fun moveAcrossDivider(tab: DIYTabs) {
        HapticUtil.performVirtualKeyHaptic(view)
        val isVisible = entries.indexOf(tab) < entries.indexOf(HIDDEN_DIVIDER)
        entries.remove(tab)
        if (isVisible) {
            entries.add(entries.indexOf(HIDDEN_DIVIDER) + 1, tab)
        } else {
            entries.add(entries.indexOf(HIDDEN_DIVIDER), tab)
        }
        commit()
    }

    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromIndex = from.index - 1
            val toIndex = to.index - 1
            if (fromIndex !in entries.indices || toIndex !in entries.indices) return@rememberReorderableLazyListState
            entries.add(toIndex, entries.removeAt(fromIndex))
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.label_toolbar,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(key = DIYTabs.ESSENTIALS.name) {
                    ToolbarTabRow(
                        tab = DIYTabs.ESSENTIALS,
                        shape = segmentShape(0, visibleTabs.size + 2),
                    )
                }

                itemsIndexed(entries, key = { _, entry -> (entry as? DIYTabs)?.name ?: HIDDEN_DIVIDER }) { _, entry ->
                    ReorderableItem(reorderableLazyListState, key = (entry as? DIYTabs)?.name ?: HIDDEN_DIVIDER) { isDragging ->
                        if (entry is DIYTabs) {
                            val isVisible = entry in visibleTabs
                            val shape =
                                if (isDragging) {
                                    RoundedCornerShape(24.dp)
                                } else if (isVisible) {
                                    segmentShape(visibleTabs.indexOf(entry) + 1, visibleTabs.size + 2)
                                } else {
                                    segmentShape(hiddenTabs.indexOf(entry), hiddenTabs.size)
                                }
                            ToolbarTabRow(
                                tab = entry,
                                shape = shape,
                                isDragging = isDragging,
                                isVisible = isVisible,
                                dragScope = this,
                                onToggleVisibility = { moveAcrossDivider(entry) },
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                },
                                onDragStopped = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    commit()
                                },
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                ToolbarTabRow(
                                    tab = DIYTabs.SETTINGS,
                                    shape = segmentShape(visibleTabs.size + 1, visibleTabs.size + 2),
                                )
                                TranslatableText(
                                    stringResId = R.string.toolbar_section_hidden,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp, top = 22.dp, bottom = 8.dp),
                                )
                                if (hiddenTabs.isEmpty()) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(24.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(24.dp),
                                                )
                                                .padding(horizontal = 24.dp, vertical = 28.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        TranslatableText(
                                            stringResId = R.string.toolbar_hidden_hint,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "show_labels") {
                    Column(
                        modifier = Modifier.padding(top = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        IconToggleItem(
                            iconRes = R.drawable.rounded_label_24,
                            title = stringResource(R.string.toolbar_show_tab_names),
                            isChecked = canShowLabels && viewModel.isToolbarShowLabels.value,
                            onCheckedChange = { viewModel.setToolbarShowLabels(it) },
                            enabled = canShowLabels,
                            onDisabledClick = { showLabelsInfo = true },
                            onInfoClick = if (canShowLabels) null else ({ showLabelsInfo = true }),
                            modifier = Modifier.clip(segmentShape(0, 2)),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(segmentShape(1, 2))
                                    .background(MaterialTheme.colorScheme.surfaceBright)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            OutlinedTextField(
                                value = viewModel.userDisplayName.value,
                                onValueChange = { viewModel.setUserDisplayName(it) },
                                label = { TranslatableText(R.string.label_your_name) },
                                placeholder = if (deviceUserName != null) { { Text(deviceUserName) } } else null,
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarLabelsInfoBottomSheet(onDismissRequest: () -> Unit) {
    val view = LocalView.current
    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.toolbar_show_tab_names,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TranslatableText(
                stringResId = R.string.toolbar_show_tab_names_unavailable,
                formatArgs = listOf(DIYTabs.MAX_LABELED_TOOLBAR_ITEMS),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceBright, RoundedCornerShape(24.dp))
                        .padding(16.dp),
            )
            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                TranslatableText(R.string.action_got_it)
            }
        }
    }
}

@Composable
private fun ToolbarTabRow(
    tab: DIYTabs,
    shape: Shape,
    isDragging: Boolean = false,
    isVisible: Boolean = true,
    dragScope: ReorderableCollectionItemScope? = null,
    onToggleVisibility: (() -> Unit)? = null,
    onDragStarted: () -> Unit = {},
    onDragStopped: () -> Unit = {},
) {
    val title = stringResource(tab.displayTitle)
    val isFixed = dragScope == null

    ListItem(
        colors =
            ListItemDefaults.colors(
                containerColor =
                    if (isDragging) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceBright
                    },
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape),
        leadingContent = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(
                            color =
                                if (isVisible) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = tab.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (tab == DIYTabs.ESSENTIALS) 26.dp else 22.dp),
                    tint =
                        if (isVisible) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        trailingContent = {
            if (isFixed) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_lock_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onToggleVisibility?.invoke() }) {
                        Icon(
                            painter =
                                painterResource(
                                    id = if (isVisible) R.drawable.rounded_do_not_disturb_on_24 else R.drawable.rounded_add_24,
                                ),
                            contentDescription = stringResource(if (isVisible) R.string.action_remove else R.string.action_add),
                            tint = if (isVisible) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                    with(dragScope!!) {
                        IconButton(
                            modifier =
                                Modifier.draggableHandle(
                                    onDragStarted = { onDragStarted() },
                                    onDragStopped = { onDragStopped() },
                                ),
                            onClick = {},
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_drag_handle_24),
                                contentDescription = stringResource(R.string.content_desc_drag_reorder),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
    )
}
