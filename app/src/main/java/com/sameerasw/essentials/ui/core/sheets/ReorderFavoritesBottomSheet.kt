/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: ReorderFavoritesBottomSheet.kt
 * Description: Bottom sheet allowing users to reorder and unpin favorite features.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderFavoritesBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val pinnedKeys = viewModel.pinnedFeatureKeys.value
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current
    val lazyListState = rememberLazyListState()

    val featuresMap = remember { FeatureRegistry.ALL_FEATURES.associateBy { it.id } }

    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val updatedList = pinnedKeys.toMutableList().apply {
                val item = removeAt(from.index)
                add(to.index, item)
            }
            viewModel.updatePinnedFeatures(updatedList)
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
            Text(
                text = stringResource(R.string.action_reorder),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item {
                    RoundedCardContainer(
                        modifier = Modifier.fillMaxWidth(),
                        spacing = 2.dp,
                        cornerRadius = 24.dp,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            pinnedKeys.forEach { featureId ->
                                val feature = featuresMap[featureId]
                                val resolvedTitle =
                                    if (feature != null) stringResource(id = feature.title) else featureId

                                ReorderableItem(reorderableLazyListState, key = featureId) { isDragging ->
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
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingContent = {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(40.dp)
                                                        .background(
                                                            color = ColorUtil.getPastelColorFor(resolvedTitle),
                                                            shape = CircleShape,
                                                        ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    painter =
                                                        painterResource(
                                                            id = feature?.iconRes ?: R.drawable.rounded_bookmark_24,
                                                        ),
                                                    contentDescription = resolvedTitle,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = ColorUtil.getVibrantColorFor(resolvedTitle),
                                                )
                                            }
                                        },
                                        content = {
                                            Text(
                                                text = resolvedTitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        trailingContent = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        HapticUtil.performVirtualKeyHaptic(view)
                                                        viewModel.togglePinFeature(featureId)
                                                    },
                                                ) {
                                                    Icon(
                                                        painter =
                                                            painterResource(id = R.drawable.rounded_bookmark_remove_24),
                                                        contentDescription = stringResource(R.string.action_unpin),
                                                        tint = MaterialTheme.colorScheme.error,
                                                    )
                                                }

                                                IconButton(
                                                    modifier =
                                                        Modifier.draggableHandle(
                                                            onDragStarted = {
                                                                hapticFeedback.performHapticFeedback(
                                                                    HapticFeedbackType.GestureThresholdActivate,
                                                                )
                                                            },
                                                            onDragStopped = {
                                                                hapticFeedback.performHapticFeedback(
                                                                    HapticFeedbackType.GestureEnd,
                                                                )
                                                            },
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
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
