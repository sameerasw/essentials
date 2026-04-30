package com.sameerasw.essentials.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.components.menus.LocalDropdownMenuDismiss
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConfigPickerItem(
    title: String,
    selectedValue: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    iconRes: Int? = null,
    isEnabled: Boolean = true,
    onDisabledClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    var isMenuExpanded by remember { mutableStateOf(false) }

    ListItem(
        onClick = {
            if (isEnabled) {
                HapticUtil.performVirtualKeyHaptic(view)
                isMenuExpanded = true
            } else if (onDisabledClick != null) {
                HapticUtil.performVirtualKeyHaptic(view)
                onDisabledClick()
            }
        },
        enabled = isEnabled,
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = if (iconRes != null && iconRes != 0) {
            {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        ),
        supportingContent = if (description != null) {
            {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        trailingContent = {
            Box {
                Surface(
                    onClick = {
                        if (isEnabled) {
                            HapticUtil.performVirtualKeyHaptic(view)
                            isMenuExpanded = true
                        }
                    },
                    enabled = isEnabled,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = selectedValue,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                SegmentedDropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    CompositionLocalProvider(
                        LocalDropdownMenuDismiss provides { isMenuExpanded = false }
                    ) {
                        content()
                    }
                }
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
}
