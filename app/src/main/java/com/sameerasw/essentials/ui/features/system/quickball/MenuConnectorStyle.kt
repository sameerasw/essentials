/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Radial Menus
 * File: MenuConnectorStyle.kt
 * Description: Styles for connector lines connecting the floating ball to radial shortcuts.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.sameerasw.essentials.R

@Keep
enum class MenuConnectorStyle(@field:StringRes val titleRes: Int) {
    NONE(R.string.quick_ball_connector_none),
    LINE(R.string.quick_ball_connector_line),
    DOTTED(R.string.quick_ball_connector_dotted),
    ARC(R.string.quick_ball_connector_arc),
    LINE_RING(R.string.quick_ball_connector_line_ring),
    DOT_RING(R.string.quick_ball_connector_dot_ring),
    CURVE(R.string.quick_ball_connector_curve),
    FADE(R.string.quick_ball_connector_fade),
    DASH(R.string.quick_ball_connector_dash),
    TAPER(R.string.quick_ball_connector_taper),
    WAVE(R.string.quick_ball_connector_wave),
    BUBBLE(R.string.quick_ball_connector_bubble),
    ZIGZAG(R.string.quick_ball_connector_zigzag),
    DOUBLE(R.string.quick_ball_connector_double),
    GLOW(R.string.quick_ball_connector_glow);

    companion object {
        fun fromName(name: String?): MenuConnectorStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DOTTED
        }
    }
}
