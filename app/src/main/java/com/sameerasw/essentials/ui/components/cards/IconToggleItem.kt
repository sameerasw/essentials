package com.sameerasw.essentials.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
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
    val finalIconRes = icon ?: iconRes
    val finalDescription = subtitle ?: description
    val finalIsChecked = checked ?: isChecked

    val onClickAction = {
        if (enabled) {
            HapticUtil.performVirtualKeyHaptic(view)
            onCheckedChange(!finalIsChecked)
        } else if (onDisabledClick != null) {
            HapticUtil.performVirtualKeyHaptic(view)
            onDisabledClick()
        }
    }

    if (showToggle) {
        if (onClick != null) {
            androidx.compose.material3.ListItem(
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
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
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
                            onCheckedChange = { checked ->
                                if (enabled) {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onCheckedChange(checked)
                                }
                            },
                            enabled = enabled
                        )
                    }
                },
                colors = androidx.compose.material3.ListItemDefaults.colors(
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
            androidx.compose.material3.ListItem(
                checked = finalIsChecked && enabled,
                onCheckedChange = { checked ->
                    if (enabled) {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onCheckedChange(checked)
                    } else if (onDisabledClick != null) {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDisabledClick()
                    }
                },
                enabled = enabled,
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
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
                        onCheckedChange = null, // Handled by ListItem
                        enabled = enabled
                    )
                },
                colors = androidx.compose.material3.ListItemDefaults.colors(
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
        androidx.compose.material3.ListItem(
            onClick = onClickAction,
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp
            ),
            supportingContent = if (finalDescription != null) {
                {
                    Text(
                        text = finalDescription,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else null,
            colors = androidx.compose.material3.ListItemDefaults.colors(
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
}

