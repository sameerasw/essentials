/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: IslandState.kt
 * Description: Domain model and business logic entry for IslandState.kt.
 */

package com.sameerasw.essentials.domain.model

enum class IslandState {
    HIDDEN,
    COMPACT,
    NORMAL,
    EXPANDED,
    ;

    val isLarge: Boolean
        get() = this == NORMAL || this == EXPANDED
}
