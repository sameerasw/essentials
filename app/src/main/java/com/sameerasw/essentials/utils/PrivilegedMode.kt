/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core Utilities
 * File: PrivilegedMode.kt
 * Description: Privileged access backends selectable by the user.
 */

package com.sameerasw.essentials.utils

import com.sameerasw.essentials.R

enum class PrivilegedMode(
    val key: String,
    val labelRes: Int,
) {
    AUTO("auto", R.string.privileged_mode_auto),
    SHIZUKU("shizuku", R.string.privileged_mode_shizuku),
    DHIZUKU("dhizuku", R.string.privileged_mode_dhizuku),
    PORTER("porter", R.string.privileged_mode_porter),
    ROOT("root", R.string.privileged_mode_root),
    ;

    companion object {
        fun fromKey(key: String?): PrivilegedMode = entries.firstOrNull { it.key == key } ?: AUTO
    }
}
