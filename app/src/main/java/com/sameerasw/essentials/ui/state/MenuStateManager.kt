package com.sameerasw.essentials.ui.state

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MenuStateManager {
    var activeId by mutableStateOf<Any?>(null)
}

val LocalMenuStateManager = compositionLocalOf { MenuStateManager() }
