package com.sameerasw.essentials.ui.components.cards

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeatureCard(
    title: Any, // Can be Int (Resource ID) or String
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    hasMoreSettings: Boolean = true,
    isToggleEnabled: Boolean = true,
    showToggle: Boolean = true,
    onDisabledToggleClick: (() -> Unit)? = null,
    description: Any? = null,
    descriptionOverride: String? = null,
    isBeta: Boolean = false,
    isPinned: Boolean = false,
    onPinToggle: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    val menuState = com.sameerasw.essentials.ui.state.LocalMenuStateManager.current
    androidx.compose.runtime.DisposableEffect(showMenu) {
        if (showMenu) {
            menuState.activeId = title
        } else {
            if (menuState.activeId == title) {
                menuState.activeId = null
            }
        }
        onDispose {
            if (menuState.activeId == title) {
                menuState.activeId = null
            }
        }
    }

    val isBlurred = menuState.activeId != null && menuState.activeId != title
    val blurRadius by animateDpAsState(
        targetValue = if (isBlurred) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 500),
        label = "blur"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isBlurred) 0.5f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "alpha"
    )

    val resolvedTitle = when (title) {
        is Int -> stringResource(id = title)
        is String -> title
        else -> ""
    }

    androidx.compose.material3.ListItem(
        onClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            onClick()
        },
        onLongClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            showMenu = true
        },
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = modifier
            .alpha(alpha)
            .blur(blurRadius),
        leadingContent = if (iconRes != null) {
            {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = ColorUtil.getPastelColorFor(resolvedTitle),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = resolvedTitle,
                        modifier = Modifier.size(24.dp),
                        tint = ColorUtil.getVibrantColorFor(resolvedTitle)
                    )
                }
            }
        } else null,
        supportingContent = if (descriptionOverride != null || description != null) {
            {
                val desc = descriptionOverride ?: description
                val resolvedDescription = when (desc) {
                    is Int -> stringResource(id = desc)
                    is String -> desc
                    else -> ""
                }
                Text(
                    text = resolvedDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (showToggle && hasMoreSettings) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }

                if (showToggle) {
                    Box {
                        Switch(
                            checked = if (isToggleEnabled) isEnabled else false,
                            onCheckedChange = { checked ->
                                if (isToggleEnabled) {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onToggle(checked)
                                }
                            },
                            enabled = isToggleEnabled
                        )

                        if (!isToggleEnabled && onDisabledToggleClick != null) {
                            Box(modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onDisabledToggleClick()
                                })
                        }
                    }
                }
            }
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        ),
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = resolvedTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isBeta) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = stringResource(R.string.label_beta),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (onPinToggle != null) {
                    SegmentedDropdownMenuItem(
                        text = {
                            Text(
                                if (isPinned) stringResource(R.string.action_unpin) else stringResource(
                                    R.string.action_pin
                                )
                            )
                        },
                        onClick = {
                            showMenu = false
                            onPinToggle()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = if (isPinned) R.drawable.rounded_bookmark_remove_24 else R.drawable.rounded_bookmark_24),
                                contentDescription = null
                            )
                        }
                    )
                }

                if (onHelpClick != null) {
                    SegmentedDropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.action_what_is_this))
                        },
                        onClick = {
                            showMenu = false
                            onHelpClick()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_help_24),
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    )
}
