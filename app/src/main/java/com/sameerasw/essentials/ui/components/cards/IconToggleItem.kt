package com.sameerasw.essentials.ui.components.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.translation.ui.TranslationBottomSheet
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
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
    checked: Boolean? = null
) {
    val view = LocalView.current
    val context = LocalContext.current
    val finalIconRes = icon ?: iconRes
    val finalDescription = subtitle ?: description
    val finalIsChecked = checked ?: isChecked

    var showMenu by remember { mutableStateOf(false) }
    var translationSheetKey by remember { mutableStateOf<String?>(null) }
    val isTranslationModeActive by TranslationManager.isTranslationModeEnabled

    val onClickAction = {
        if (enabled) {
            HapticUtil.performVirtualKeyHaptic(view)
            onCheckedChange(!finalIsChecked)
        } else if (onDisabledClick != null) {
            HapticUtil.performVirtualKeyHaptic(view)
            onDisabledClick()
        }
    }

    Box(
        modifier = if (isTranslationModeActive) {
            modifier.combinedClickable(
                onClick = {
                    if (onClick != null) onClick() else onClickAction()
                },
                onLongClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    showMenu = true
                }
            )
        } else {
            modifier
        }
    ) {
        if (showToggle) {
            if (onClick != null) {
                ListItem(
                    onClick = {
                        if (enabled) {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onClick()
                        } else if (onDisabledClick != null) {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onDisabledClick()
                        }
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = if (finalIconRes != 0) {
                        {
                            Icon(
                                painter = painterResource(id = finalIconRes),
                                contentDescription = title,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null,
                    supportingContent = if (finalDescription != null) {
                        {
                            Text(
                                text = finalDescription,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else null,
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            VerticalDivider(
                                modifier = Modifier
                                    .height(32.dp)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Switch(
                                checked = if (enabled) finalIsChecked else false,
                                onCheckedChange = { c ->
                                    if (enabled) {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        onCheckedChange(c)
                                    }
                                },
                                enabled = enabled
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    content = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            } else {
                ListItem(
                    checked = finalIsChecked && enabled,
                    onCheckedChange = { c ->
                        if (enabled) {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onCheckedChange(c)
                        } else if (onDisabledClick != null) {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onDisabledClick()
                        }
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = if (finalIconRes != 0) {
                        {
                            Icon(
                                painter = painterResource(id = finalIconRes),
                                contentDescription = title,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null,
                    supportingContent = if (finalDescription != null) {
                        {
                            Text(
                                text = finalDescription,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else null,
                    trailingContent = {
                        Switch(
                            checked = if (enabled) finalIsChecked else false,
                            onCheckedChange = null,
                            enabled = enabled
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    content = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        } else {
            ListItem(
                onClick = onClickAction,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = if (finalIconRes != 0) {
                    {
                        Icon(
                            painter = painterResource(id = finalIconRes),
                            contentDescription = title,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null,
                supportingContent = if (finalDescription != null) {
                    {
                        Text(
                            text = finalDescription,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else null,
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ),
                content = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }

        if (isTranslationModeActive) {
            SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                val keyTitle = remember(title) { TranslationManager.resolveKey(context, title) }
                val keyDesc = remember(finalDescription) { TranslationManager.resolveKey(context, finalDescription) }

                if (keyTitle != null) {
                    SegmentedDropdownMenuItem(
                        text = { Text("Translate Title ($keyTitle)") },
                        onClick = {
                            showMenu = false
                            translationSheetKey = keyTitle
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_translate_24),
                                contentDescription = null
                            )
                        }
                    )
                }

                if (keyDesc != null) {
                    SegmentedDropdownMenuItem(
                        text = { Text("Translate Description ($keyDesc)") },
                        onClick = {
                            showMenu = false
                            translationSheetKey = keyDesc
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_translate_24),
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }

    if (translationSheetKey != null) {
        TranslationBottomSheet(
            stringKey = translationSheetKey!!,
            onDismissRequest = { translationSheetKey = null }
        )
    }
}
