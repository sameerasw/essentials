/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: SearchbarRenderState.kt
 * Description: Snapshot state that makes the running searchbar widget composition redraw.
 */

package com.sameerasw.essentials.services.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object SearchbarRenderState {
    var revision by mutableIntStateOf(0)
        private set

    fun bump() {
        revision++
    }
}
