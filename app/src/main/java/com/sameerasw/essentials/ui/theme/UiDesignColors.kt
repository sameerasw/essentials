/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: UiDesignColors.kt
 * Description: Design aware color helpers for list item cards.
 */

package com.sameerasw.essentials.ui.theme

import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun cardListItemColors(containerColor: Color = MaterialTheme.colorScheme.surfaceBright): ListItemColors {
    if (LocalUiDesign.current != UiDesign.ONE_UI) {
        return ListItemDefaults.colors(containerColor = containerColor)
    }
    val scheme = MaterialTheme.colorScheme
    return ListItemDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = scheme.onSurface,
        supportingContentColor = scheme.onSurfaceVariant,
        overlineContentColor = scheme.onSurfaceVariant,
        disabledContainerColor = Color.Transparent,
        selectedContainerColor = Color.Transparent,
        selectedContentColor = scheme.onSurface,
        selectedLeadingContentColor = scheme.onSurface,
        selectedTrailingContentColor = scheme.onSurface,
        selectedOverlineContentColor = scheme.onSurfaceVariant,
        selectedSupportingContentColor = scheme.onSurfaceVariant,
        draggedContainerColor = scheme.surfaceBright,
    )
}

@Composable
fun accentColor(): Color =
    if (LocalUiDesign.current == UiDesign.ONE_UI) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary

@Composable
private fun isOneUi(): Boolean = LocalUiDesign.current == UiDesign.ONE_UI

@Composable
fun toolbarContainerColor(): Color =
    if (isOneUi()) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (LocalBlurEnabled.current) 0.6f else 1f)
    } else {
        MaterialTheme.colorScheme.primary
    }

@Composable
fun toolbarContentColor(): Color =
    if (isOneUi()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.background

@Composable
fun toolbarSelectedContentColor(): Color =
    if (isOneUi()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary

@Composable
fun toolbarPillColor(): Color {
    if (!isOneUi()) return MaterialTheme.colorScheme.background
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() < 0.5f) scheme.surfaceContainerLow else scheme.surfaceContainerLowest
}

@Composable
fun fabContainerColor(): Color =
    if (isOneUi()) toolbarContainerColor() else MaterialTheme.colorScheme.primaryContainer

@Composable
fun fabContentColor(): Color =
    if (isOneUi()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer
