package com.sameerasw.essentials.ui.components.cards

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.annotation.StringRes
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
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil

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
    LaunchedEffect(showMenu) {
        if (showMenu) {
            menuState.activeId = title
        } else {
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

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
            .alpha(alpha)
            .combinedClickable(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onClick()
                },
                onLongClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    showMenu = true
                }
            )) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .blur(blurRadius)
            .padding(16.dp)) {

            val resolvedTitle = when (title) {
                is Int -> stringResource(id = title)
                is String -> title
                else -> ""
            }

            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (iconRes != null) {
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = resolvedTitle)
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
                    if (descriptionOverride != null) {
                        Text(
                            text = descriptionOverride,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (description != null) {
                        val resolvedDescription = when (description) {
                            is Int -> stringResource(id = description)
                            is String -> description
                            else -> ""
                        }
                        Text(
                            text = resolvedDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (hasMoreSettings) {
                    Icon(
                        modifier = Modifier.padding(end = 12.dp).size(24.dp),
                        painter = painterResource(id = R.drawable.rounded_chevron_right_24),
                        contentDescription = "More settings"
                    )
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
                            // Invisible overlay catches taps even if the child consumes them
                            Box(modifier = Modifier.matchParentSize().clickable {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onDisabledToggleClick()
                            })
                        }
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
                            Text(if (isPinned) stringResource(R.string.action_unpin) else stringResource(R.string.action_pin))
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
    }
}
