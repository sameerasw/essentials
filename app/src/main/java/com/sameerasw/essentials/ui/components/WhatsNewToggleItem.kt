/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Components
 * File: WhatsNewToggleItem.kt
 * Description: Toggle for the day-limited "what's new" indicators.
 */

package com.sameerasw.essentials.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.modifiers.whatsNewGlow
import com.sameerasw.essentials.ui.state.WhatsNewState

@Composable
fun WhatsNewToggleItem(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    IconToggleItem(
        iconRes = R.drawable.rounded_celebration_24,
        title = stringResource(R.string.setting_show_whats_new_title),
        isChecked = WhatsNewState.enabled,
        onCheckedChange = { WhatsNewState.setEnabled(context, it) },
        modifier = modifier.whatsNewGlow(true),
    )
}
