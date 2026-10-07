/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: PrivilegedModePicker.kt
 * Description: Dropdown picker for the privileged access backend.
 */

package com.sameerasw.essentials.ui.core.pickers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PrivilegedMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivilegedModePicker(
    selectedMode: PrivilegedMode,
    onModeSelected: (PrivilegedMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var isMenuExpanded by remember { mutableStateOf(false) }

    ListItem(
        onClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            isMenuExpanded = true
        },
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.rounded_numbers_24),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Box {
                Surface(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        isMenuExpanded = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = stringResource(selectedMode.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }

                SegmentedDropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                ) {
                    PrivilegedMode.entries.forEach { mode ->
                        SegmentedDropdownMenuItem(
                            text = { Text(stringResource(mode.labelRes)) },
                            onClick = {
                                isMenuExpanded = false
                                onModeSelected(mode)
                            },
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        content = {
            Text(
                text = stringResource(R.string.setting_privileged_mode_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}
