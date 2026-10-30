/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: StackSmartPick.kt
 * Description: Picks the most relevant widget to show in a stack.
 */

package com.sameerasw.essentials.services.widgets

object StackSmartPick {
    fun pick(
        hostedComponents: List<String?>,
        playingPackage: String?,
        calendarPackages: Set<String>,
        charging: Boolean,
    ): Int? {
        fun first(match: (pkg: String, cls: String) -> Boolean): Int? =
            hostedComponents
                .indexOfFirst { it != null && match(it.substringBefore('/'), it.substringAfter('/', "")) }
                .takeIf { it >= 0 }

        return playingPackage?.let { playing -> first { pkg, _ -> pkg == playing } }
            ?: first { pkg, _ -> pkg in calendarPackages }
            ?: if (charging) first { _, cls -> cls.contains("batter", ignoreCase = true) } else null
    }
}
