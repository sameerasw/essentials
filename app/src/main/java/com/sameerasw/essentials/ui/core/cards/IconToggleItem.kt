/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Components
 * File: IconToggleItem.kt
 * Description: Standard settings item composable with leading icon, title, supporting text,
 * and trailing switch control.
 */

package com.sameerasw.essentials.ui.core.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import com.sameerasw.essentials.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.translation.ui.TranslationBottomSheet
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconToggleItem(
    iconRes: Int = 0,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    isChecked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    onDisabledClick: (() -> Unit)? = null,
    showToggle: Boolean = true,
    onClick: (() -> Unit)? = null,
    subtitle: String? = null,
    icon: Int? = null,
    checked: Boolean? = null,
    isBeta: Boolean = false,
    onCheckedChangeWithPosition: ((Boolean, Offset) -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val finalIconRes = icon ?: iconRes
    val finalDescription = subtitle ?: description
    val finalIsChecked = checked ?: isChecked
    val isTranslationModeActive by TranslationManager.isTranslationModeEnabled

    var showMenu by remember { mutableStateOf(false) }
    var translationSheetKey by remember { mutableStateOf<String?>(null) }

    val onClickAction = {
        if (enabled) {
            HapticUtil.performVirtualKeyHaptic(view)
            if (onClick != null) {
                onClick()
            } else {
                onCheckedChange(!finalIsChecked)
            }
        } else if (onDisabledClick != null) {
            HapticUtil.performVirtualKeyHaptic(view)
            onDisabledClick()
        }
    }

    val onLongClickAction: (() -> Unit)? =
        if (isTranslationModeActive) {
            {
                HapticUtil.performVirtualKeyHaptic(view)
                showMenu = true
            }
        } else {
            null
        }

    val renderMenu: @Composable () -> Unit = {
        SegmentedDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            com.sameerasw.essentials.translation.ui.TranslationMenuItems(
                title = title,
                description = finalDescription,
                onSelectKey = { key ->
                    showMenu = false
                    translationSheetKey = key
                },
            )
        }
    }

    val renderTitle: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (isBeta) {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    Text(
                        text = stringResource(R.string.label_beta),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        renderMenu()
    }

    if (showToggle) {
        if (onClick != null) {
            androidx.compose.foundation.layout.Box {
                ListItem(
                    onClick = {
                        if (enabled) {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onClick()
                        }
                    },
                onLongClick = onLongClickAction,
                enabled = enabled,
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent =
                    if (finalIconRes != 0) {
                        {
                            Icon(
                                painter = painterResource(id = finalIconRes),
                                contentDescription = title,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                supportingContent =
                    if (finalDescription != null) {
                        {
                            Text(
                                text = finalDescription,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        null
                    },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        VerticalDivider(
                            modifier =
                                Modifier
                                    .height(32.dp)
                                    .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Switch(
                            checked = finalIsChecked,
                            onCheckedChange = { c ->
                                if (enabled) {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onCheckedChange(c)
                                }
                            },
                            enabled = enabled,
                        )
                    }
                },
                colors =
                    ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                    ),
                content = renderTitle,
                )
                if (!enabled && onDisabledClick != null) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(1f)
                            .clickable(
                                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onDisabledClick()
                            }
                    )
                }
            }
        } else {
            var switchCenterOffset by remember { mutableStateOf(Offset.Zero) }

            androidx.compose.foundation.layout.Box {
                ListItem(
                    checked = finalIsChecked,
                    onCheckedChange = { c ->
                        if (enabled) {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onCheckedChange(c)
                            onCheckedChangeWithPosition?.invoke(c, switchCenterOffset)
                        }
                    },
                onLongClick = onLongClickAction,
                enabled = enabled,
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent =
                    if (finalIconRes != 0) {
                        {
                            Icon(
                                painter = painterResource(id = finalIconRes),
                                contentDescription = title,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                supportingContent =
                    if (finalDescription != null) {
                        {
                            Text(
                                text = finalDescription,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        null
                    },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (onSettingsClick != null && enabled && finalIsChecked) {
                            IconButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onSettingsClick()
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_settings_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            VerticalDivider(
                                modifier =
                                    Modifier
                                        .height(28.dp)
                                        .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        Switch(
                            checked = finalIsChecked,
                            onCheckedChange = null,
                            enabled = enabled,
                            modifier = Modifier.onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                val size = coords.size
                                switchCenterOffset = Offset(
                                    x = pos.x + (size.width / 2f),
                                    y = pos.y + (size.height / 2f)
                                )
                            },
                        )
                    }
                },
                colors =
                    ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                    ),
                content = renderTitle,
                )
                if (!enabled && onDisabledClick != null) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(1f)
                            .clickable(
                                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onDisabledClick()
                            }
                    )
                }
            }
        }
    } else {
        ListItem(
            onClick = onClickAction,
            onLongClick = onLongClickAction,
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent =
                if (finalIconRes != 0) {
                    {
                        Icon(
                            painter = painterResource(id = finalIconRes),
                            contentDescription = title,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    null
                },
            supportingContent =
                if (finalDescription != null) {
                    {
                        Text(
                            text = finalDescription,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    null
                },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                ),
            content = renderTitle,
        )
    }

    if (translationSheetKey != null) {
        TranslationBottomSheet(
            stringKey = translationSheetKey!!,
            onDismissRequest = { translationSheetKey = null },
        )
    }
}
