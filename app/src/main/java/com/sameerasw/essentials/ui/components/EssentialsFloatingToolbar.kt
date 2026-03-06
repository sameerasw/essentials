package com.sameerasw.essentials.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.utils.HapticUtil

data class ToolbarItem(
    val iconRes: Int,
    val labelRes: Int,
    val onClick: () -> Unit,
    val hasBadge: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EssentialsFloatingToolbar(
    modifier: Modifier = Modifier,
    // Tabbed Mode
    items: List<ToolbarItem> = emptyList(),
    selectedIndex: Int = -1,
    // Standard Mode
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    // FAB / Action
    fabAction: (() -> Unit)? = null,
    fabIconRes: Int? = null,
    fabContentDescription: String? = null,
    // Slots for custom logic
    floatingActionButton: (@Composable () -> Unit)? = null,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    expanded: Boolean = true
) {
    val view = LocalView.current

    val finalFab: (@Composable () -> Unit)? = when {
        floatingActionButton != null -> floatingActionButton
        onHelpClick != null && fabAction == null -> {
            {
                FloatingActionButton(
                    onClick = {HapticUtil.performUIHaptic(view)
                        onHelpClick()
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_help_24),
                        contentDescription = stringResource(R.string.action_help_guide)
                    )
                }
            }
        }
        fabAction != null && fabIconRes != null -> {
            {
                FloatingActionButton(
                    onClick = {HapticUtil.performUIHaptic(view)
                        fabAction()
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Icon(
                        painter = painterResource(id = fabIconRes),
                        contentDescription = fabContentDescription
                    )
                }
            }
        }
        else -> null
    }

    HorizontalFloatingToolbar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
        expanded = expanded,
        floatingActionButton = finalFab ?: {},
        scrollBehavior = scrollBehavior,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContentColor = MaterialTheme.colorScheme.onSurface,
            toolbarContainerColor = MaterialTheme.colorScheme.primary,
        ),
        content = {
            if (onBackClick != null) {
                // BACK BUTTON - Unified with Tabbed style (pop-out effect)
                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onBackClick()
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_arrow_back_24),
                        contentDescription = stringResource(R.string.content_desc_back),
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (title != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.background,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(min = 100.dp, max = 250.dp)
                            .padding(horizontal = 8.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            } else {
                // TABBED MODE - Expanding labels
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index

                    val itemWidth by animateDpAsState(
                        targetValue = if (expanded || isSelected) 48.dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "item_width_$index"
                    )

                    val labelWidth by animateDpAsState(
                        targetValue = if (isSelected) 80.dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "label_width_$index"
                    )

                    val spacerWidth by animateDpAsState(
                        targetValue = if (index < items.size - 1) 8.dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "spacer_width_$index"
                    )

                    if (itemWidth > 0.dp || isSelected) {
                        IconButton(
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                item.onClick()
                            },
                            modifier = Modifier
                                .width(itemWidth + labelWidth)
                                .height(48.dp),
                            colors = if (isSelected) {
                                IconButtonDefaults.filledIconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            } else {
                                IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.background,
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Box {
                                    Icon(
                                        painter = painterResource(id = item.iconRes),
                                        contentDescription = stringResource(id = item.labelRes),
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.background
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    if (item.hasBadge) {
                                        Canvas(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .align(Alignment.TopEnd)
                                        ) {
                                            drawCircle(
                                                color = Color.Red,
                                            )
                                        }
                                    }
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(id = item.labelRes),
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Animated spacing between buttons
                        if (index < items.size - 1) {
                            Spacer(modifier = Modifier.width(spacerWidth))
                        }
                    }
                }
            }
        }
    )
}

