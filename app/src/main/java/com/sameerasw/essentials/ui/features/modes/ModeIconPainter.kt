/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeIconPainter.kt
 * Description: Painters for mode icons, using the system icon for linked modes.
 */

package com.sameerasw.essentials.ui.features.modes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.domain.modes.Mode
import com.sameerasw.essentials.domain.modes.ModeIcons
import com.sameerasw.essentials.domain.modes.SystemMode
import com.sameerasw.essentials.domain.modes.SystemModeIcons

private const val ICON_BITMAP_SIZE = 96

@Composable
fun rememberModeIconPainter(mode: Mode): Painter {
    val fallback = painterResource(ModeIcons.resolve(mode.iconName))
    if (!mode.isLinked) return fallback
    return rememberSystemIconPainter(mode.linkedIconPackage, mode.linkedIconName, mode.linkedType) ?: fallback
}

@Composable
fun rememberSystemModeIconPainter(systemMode: SystemMode): Painter =
    rememberSystemIconPainter(systemMode.iconPackage, systemMode.iconName, systemMode.type)
        ?: painterResource(ModeIcons.resolve(ModeIcons.DEFAULT))

@Composable
private fun rememberSystemIconPainter(
    packageName: String?,
    iconName: String?,
    type: Int?,
): Painter? {
    val context = LocalContext.current
    return remember(packageName, iconName, type) {
        SystemModeIcons
            .load(context, packageName, iconName, type)
            ?.toBitmap(ICON_BITMAP_SIZE, ICON_BITMAP_SIZE)
            ?.asImageBitmap()
            ?.let(::BitmapPainter)
    }
}
