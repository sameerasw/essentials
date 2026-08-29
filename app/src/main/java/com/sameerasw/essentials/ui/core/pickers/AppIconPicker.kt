/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: AppIconPicker.kt
 * Description: Reusable core UI component for AppIconPicker.
 */

package com.sameerasw.essentials.ui.core.pickers

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.AppIcon
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.translation.ui.TranslationBottomSheet
import com.sameerasw.essentials.translation.ui.TranslationMenuItems
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun AppIconPicker(
    selectedIcon: AppIcon,
    onIconSelected: (AppIcon) -> Unit,
    modifier: Modifier = Modifier,
    options: List<AppIcon> = AppIcon.entries,
    onIconSelectedWithPosition: ((AppIcon, Offset) -> Unit)? = null,
) {
    val view = LocalView.current
    val isTranslationModeActive by TranslationManager.isTranslationModeEnabled

    var showMenu by remember { mutableStateOf(false) }
    var translationSheetKey by remember { mutableStateOf<String?>(null) }

    val onLongClickAction: (() -> Unit)? =
        if (isTranslationModeActive) {
            {
                HapticUtil.performVirtualKeyHaptic(view)
                showMenu = true
            }
        } else {
            null
        }

    val optionTitleRes = options.map { it.titleRes }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceBright),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        ListItem(
            onClick = {},
            onLongClick = onLongClickAction,
            modifier = Modifier.fillMaxWidth(),
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_app_registration_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            contentPadding =
                PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                ),
            content = {
                Box {
                    Column {
                        Text(
                            text = stringResource(R.string.setting_app_icon_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.setting_app_icon_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (showMenu) {
                        SegmentedDropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            TranslationMenuItems(
                                title = R.string.setting_app_icon_title,
                                description = R.string.setting_app_icon_desc,
                                options = optionTitleRes,
                                onSelectKey = { key ->
                                    showMenu = false
                                    translationSheetKey = key
                                },
                            )
                        }
                    }
                }
            },
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { iconItem ->
                val isSelected = iconItem == selectedIcon

                val circleBackground =
                    if (iconItem == AppIcon.BLACK) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        Color.White
                    }

                var iconCenterOffset by remember { mutableStateOf(Offset.Zero) }

                Box(
                    modifier =
                        Modifier
                            .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(56.dp)
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInRoot()
                                    val size = coords.size
                                    iconCenterOffset = Offset(
                                        x = pos.x + (size.width / 2f),
                                        y = pos.y + (size.height / 2f)
                                    )
                                }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 2.5.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    },
                                ).padding(if (isSelected) 3.5.dp else 0.dp)
                                .clip(CircleShape)
                                .background(circleBackground)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true),
                                ) {
                                    HapticUtil.performUIHaptic(view)
                                    onIconSelected(iconItem)
                                    onIconSelectedWithPosition?.invoke(iconItem, iconCenterOffset)
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = iconItem.foregroundRes),
                            contentDescription = stringResource(iconItem.titleRes),
                            colorFilter =
                                if (iconItem == AppIcon.BLACK) {
                                    ColorFilter.tint(
                                        MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    null
                                },
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .scale(1.5f),
                        )
                    }
                }
            }
        }
    }

    if (translationSheetKey != null) {
        TranslationBottomSheet(
            stringKey = translationSheetKey!!,
            onDismissRequest = { translationSheetKey = null },
        )
    }
}
