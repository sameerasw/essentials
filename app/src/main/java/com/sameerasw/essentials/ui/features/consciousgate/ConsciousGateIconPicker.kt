/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Conscious Gate
 * File: ConsciousGateIconPicker.kt
 * Description: UI component letting the user pick the icon shown on the Conscious Gate
 * pause screen.
 */

package com.sameerasw.essentials.ui.features.consciousgate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsciousGateIconPicker(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val icons =
        listOf(
            "rounded_favorite_24",
            "rounded_heart_smile_24",
            "rounded_ecg_heart_24",
            "rounded_volunteer_activism_24",
            "rounded_self_improvement_24",
            "rounded_health_and_safety_24",
            "rounded_shield_24",
            "rounded_wb_sunny_24",
            "rounded_nightlight_24",
            "rounded_sentiment_satisfied_24",
            "rounded_sentiment_very_satisfied_24",
            "rounded_spa_24",
            "rounded_eco_24",
            "rounded_potted_plant_24",
            "rounded_pause_24",
            "rounded_timer_24",
            "rounded_lock_clock_24",
        )

    val carouselState = rememberCarouselState { icons.size }
    val context = LocalContext.current
    val view = LocalView.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.conscious_gate_icon_picker_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 64.dp,
            minSmallItemWidth = 24.dp,
            maxSmallItemWidth = 36.dp,
            itemSpacing = 6.dp,
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp),
        ) { index ->
            val iconName = icons[index]
            val isSelected = iconName == selectedIconName
            val iconResId =
                context.resources.getIdentifier(iconName, "drawable", context.packageName)

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.medium)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ).clickable {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onIconSelected(iconName)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = if (iconResId != 0) iconResId else R.drawable.rounded_pause_24),
                    contentDescription = null,
                    tint =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
