/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Radial Menus
 * File: QuickBallShortcutStyle.kt
 * Description: Presentation styles for application icons and action buttons.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.sameerasw.essentials.R

@Keep
enum class QuickBallShortcutStyle(@field:StringRes val titleRes: Int) {
    FULL_BLEED(R.string.quick_ball_shortcut_style_full_bleed),
    CONTAINER_FRAME(R.string.quick_ball_shortcut_style_container),
    MONOCHROME(R.string.quick_ball_shortcut_style_monochrome);

    companion object {
        fun fromName(name: String?): QuickBallShortcutStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FULL_BLEED
        }
    }
}
