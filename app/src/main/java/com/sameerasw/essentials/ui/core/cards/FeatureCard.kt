/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Components
 * File: FeatureCard.kt
 * Description: Reusable high-level feature card composable with pastel icon containers
 * and status indicators.
 */

package com.sameerasw.essentials.ui.core.cards

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    onHelpClick: (() -> Unit)? = null,
    additionalMenuItems: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
    customTrailingContent: (@Composable () -> Unit)? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    hasBadge: Boolean = false,
    containerColor: androidx.compose.ui.graphics.Color? = null,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    hasIconBackground: Boolean = true,
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }
    var translationSheetKey by remember { mutableStateOf<String?>(null) }

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
        label = "blur",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isBlurred) 0.5f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "alpha",
    )

    val resolvedTitle =
        when (title) {
            is Int -> stringResource(id = title)
            is String -> title
            else -> ""
        }

    androidx.compose.material3.ListItem(
        onClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            if (!isToggleEnabled && !hasMoreSettings && onDisabledToggleClick != null) {
                onDisabledToggleClick()
            } else {
                onClick()
            }
        },
        onLongClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            showMenu = true
        },
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .alpha(alpha)
                .blur(blurRadius),
        leadingContent =
            if (iconPainter != null || iconRes != null) {
                {
                    Box {
                        val containerModifier =
                            if (hasIconBackground) {
                                Modifier
                                    .size(40.dp)
                                    .background(
                                        color = ColorUtil.getPastelColorFor(resolvedTitle),
                                        shape = CircleShape,
                                    )
                            } else {
                                Modifier.size(40.dp)
                            }
                        Box(
                            modifier = containerModifier,
                            contentAlignment = Alignment.Center,
                        ) {
                            if (iconPainter != null) {
                                androidx.compose.foundation.Image(
                                    painter = iconPainter,
                                    contentDescription = resolvedTitle,
                                    modifier = Modifier.size(iconSize),
                                )
                            } else if (iconRes != null) {
                                val context = LocalContext.current
                                val validIconRes =
                                    remember(iconRes) {
                                        try {
                                            if (iconRes != 0 && context.resources.getResourceTypeName(iconRes) == "drawable") {
                                                iconRes
                                            } else {
                                                R.drawable.rounded_settings_24
                                            }
                                        } catch (e: Throwable) {
                                            R.drawable.rounded_settings_24
                                        }
                                    }
                                Icon(
                                    painter = painterResource(id = validIconRes),
                                    contentDescription = resolvedTitle,
                                    modifier = Modifier.size(iconSize),
                                    tint = ColorUtil.getVibrantColorFor(resolvedTitle),
                                )
                            }
                        }

                        if (hasBadge) {
                            androidx.compose.material3.Badge(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(18.dp),
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ) {
                                val composition by com.airbnb.lottie.compose.rememberLottieComposition(
                                    com.airbnb.lottie.compose.LottieCompositionSpec
                                        .RawRes(R.raw.update_motion),
                                )
                                val progress by com.airbnb.lottie.compose.animateLottieCompositionAsState(
                                    composition = composition,
                                    iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever,
                                )
                                val onErrorColor = MaterialTheme.colorScheme.onError
                                val dynamicProperties =
                                    com.airbnb.lottie.compose.rememberLottieDynamicProperties(
                                        com.airbnb.lottie.compose.rememberLottieDynamicProperty(
                                            property = com.airbnb.lottie.LottieProperty.COLOR_FILTER,
                                            value =
                                                android.graphics.PorterDuffColorFilter(
                                                    onErrorColor.toArgb(),
                                                    android.graphics.PorterDuff.Mode.SRC_ATOP,
                                                ),
                                            keyPath = arrayOf("**"),
                                        ),
                                    )

                                com.airbnb.lottie.compose.LottieAnimation(
                                    composition = composition,
                                    progress = { progress },
                                    dynamicProperties = dynamicProperties,
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(1.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                null
            },
        supportingContent =
            if (descriptionOverride != null || description != null) {
                {
                    val desc = descriptionOverride ?: description
                    val resolvedDescription =
                        when (desc) {
                            is Int -> stringResource(id = desc)
                            is String -> desc
                            else -> ""
                        }
                    Text(
                        text = resolvedDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                if (showToggle && hasMoreSettings) {
                    VerticalDivider(
                        modifier =
                            Modifier
                                .height(32.dp)
                                .width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
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
                            enabled = isToggleEnabled,
                        )

                        if (!isToggleEnabled && onDisabledToggleClick != null) {
                            Box(
                                modifier =
                                    Modifier
                                        .matchParentSize()
                                        .zIndex(1f)
                                        .clickable(
                                            interactionSource =
                                                remember {
                                                    androidx.compose.foundation.interaction
                                                        .MutableInteractionSource()
                                                },
                                            indication = null,
                                        ) {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            onDisabledToggleClick()
                                        },
                            )
                        }
                    }
                }

                if (customTrailingContent != null) {
                    customTrailingContent()
                }
            }
        },
        colors =
            androidx.compose.material3.ListItemDefaults.colors(
                containerColor = containerColor ?: MaterialTheme.colorScheme.surfaceBright,
            ),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp,
            ),
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = resolvedTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isBeta) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
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

            SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                val isTranslationModeActive by com.sameerasw.essentials.translation.TranslationManager.isTranslationModeEnabled
                if (isTranslationModeActive) {
                    com.sameerasw.essentials.translation.ui.TranslationMenuItems(
                        title = title,
                        description = description ?: descriptionOverride,
                        onSelectKey = { key ->
                            showMenu = false
                            translationSheetKey = key
                        },
                    )
                }

                if (onPinToggle != null) {
                    SegmentedDropdownMenuItem(
                        text = {
                            Text(
                                if (isPinned) {
                                    stringResource(R.string.action_unpin)
                                } else {
                                    stringResource(
                                        R.string.action_pin,
                                    )
                                },
                            )
                        },
                        onClick = {
                            showMenu = false
                            onPinToggle()
                        },
                        leadingIcon = {
                            Icon(
                                painter =
                                    painterResource(
                                        id = if (isPinned) R.drawable.rounded_bookmark_remove_24 else R.drawable.rounded_bookmark_24,
                                    ),
                                contentDescription = null,
                            )
                        },
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
                                contentDescription = null,
                            )
                        },
                    )
                }

                if (additionalMenuItems != null) {
                    additionalMenuItems { showMenu = false }
                }
            }
        },
    )

    if (translationSheetKey != null) {
        com.sameerasw.essentials.translation.ui.TranslationBottomSheet(
            stringKey = translationSheetKey!!,
            onDismissRequest = { translationSheetKey = null },
        )
    }
}
