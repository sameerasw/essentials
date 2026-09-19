/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: WhatsNewCustomContent.kt
 * Description: UI layout element for WhatsNewCustomContent.kt.
 */

package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.GoogleSansFlexRounded

/**
 * Slot for custom content to be displayed in the "What's New" screen.
 */
@Composable
fun WhatsNewCustomContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "New: Feature tags and what they means",
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp,
                ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        RoundedCardContainer {
            FeatureCard(
                title = "Beta",
                description = "This feature is under active development and may be unfinished",
                iconRes = R.drawable.rounded_science_24,
                isEnabled = false,
                isToggleEnabled = false,
                hasMoreSettings = false,
                isBeta = true,
                onToggle = {},
                onClick = {},
            )
            FeatureCard(
                title = "Unsupported",
                description = "This indication appears if you enable any unsupported/untested features. Use with caution.",
                iconRes = R.drawable.rounded_dangerous_24,
                isEnabled = false,
                isToggleEnabled = false,
                hasMoreSettings = false,
                isUnsupported = true,
                onToggle = {},
                onClick = {},
            )
            FeatureCard(
                title = "Legacy",
                description = "These features may not receive future updates due to technical or other limitations.",
                iconRes = R.drawable.rounded_archive_24,
                isEnabled = false,
                isToggleEnabled = false,
                hasMoreSettings = false,
                isLegacy = true,
                onToggle = {},
                onClick = {},
            )
        }
    }
}
