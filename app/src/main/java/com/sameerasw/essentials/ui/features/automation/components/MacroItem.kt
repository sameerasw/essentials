/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Automation
 * File: MacroItem.kt
 * Description: UI component and settings composable for Automation feature domain.
 */

package com.sameerasw.essentials.ui.components.diy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Macro
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MacroItem(
    macro: Macro,
    onClick: () -> Unit = {},
    onRun: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
            ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier =
            modifier.combinedClickable(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onClick()
                },
                onLongClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    showMenu = true
                },
            ),
    ) {
        Box {
            SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                offset = DpOffset(0.dp, 0.dp),
            ) {
                SegmentedDropdownMenuItem(
                    text = { Text(stringResource(R.string.action_test)) },
                    onClick = {
                        showMenu = false
                        onRun()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_play_arrow_24),
                            contentDescription = null,
                        )
                    },
                )
                SegmentedDropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_delete_24),
                            contentDescription = null,
                        )
                    },
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_bolt_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = macro.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val actionCount = macro.actionList.size
                    Text(
                        text = pluralStringResource(R.plurals.diy_macro_action_count, actionCount, actionCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
