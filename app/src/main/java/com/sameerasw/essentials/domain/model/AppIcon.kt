/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Model
 * File: AppIcon.kt
 * Description: Model enum representing customizable app launcher icons.
 */

package com.sameerasw.essentials.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sameerasw.essentials.R

enum class AppIcon(
    val key: String,
    val aliasName: String,
    @StringRes val titleRes: Int,
    @DrawableRes val foregroundRes: Int,
) {
    DEFAULT(
        key = "default",
        aliasName = "com.sameerasw.essentials.MainActivity",
        titleRes = R.string.app_icon_default,
        foregroundRes = R.drawable.ic_launcher_foreground,
    ),
    LEGACY(
        key = "legacy",
        aliasName = "com.sameerasw.essentials.MainActivityLegacy",
        titleRes = R.string.app_icon_legacy,
        foregroundRes = R.drawable.ic_launcher_foreground_legacy,
    ),
    BLACK(
        key = "black",
        aliasName = "com.sameerasw.essentials.MainActivityBlack",
        titleRes = R.string.app_icon_black,
        foregroundRes = R.drawable.ic_launcher_foreground_black,
    ),
    GTA(
        key = "gta",
        aliasName = "com.sameerasw.essentials.MainActivityGta",
        titleRes = R.string.app_icon_gta,
        foregroundRes = R.drawable.ic_launcher_foreground_gta,
    ),
    GOOGLE(
        key = "google",
        aliasName = "com.sameerasw.essentials.MainActivityGoogle",
        titleRes = R.string.app_icon_google,
        foregroundRes = R.drawable.ic_launcher_foreground_google,
    );

    companion object {
        fun fromKey(key: String?): AppIcon {
            return entries.find { it.key == key } ?: DEFAULT
        }
    }
}
