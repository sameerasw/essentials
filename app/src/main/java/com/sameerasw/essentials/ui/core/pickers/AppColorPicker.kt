/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: AppColorPicker.kt
 * Description: Seed color carousel picker for the app theme.
 */

package com.sameerasw.essentials.ui.core.pickers

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.materialkolor.dynamicColorScheme
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.theme.AppColorOption
import com.sameerasw.essentials.ui.theme.AppColorOptions
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppColorPicker(
    selectedSeed: Int,
    selectedStyle: String?,
    onSelected: (Int, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val hasSystemColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val items = remember(hasSystemColors) { if (hasSystemColors) listOf<AppColorOption?>(null) + AppColorOptions else AppColorOptions }
    val schemes =
        remember(items, isDark) {
            items.map { option ->
                option?.let { dynamicColorScheme(seedColor = it.seedColor, isDark = isDark, style = it.style) }
            }
        }
    val noneSelected = !hasSystemColors && selectedSeed == 0
    val carouselState = rememberCarouselState { items.size }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_palette_24),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            TranslatableText(
                stringResId = R.string.setting_app_color_title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 64.dp,
            minSmallItemWidth = 24.dp,
            maxSmallItemWidth = 40.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier =
                Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .height(64.dp),
        ) { index ->
            val option = items[index]
            val scheme = schemes[index]
            val isSelected =
                if (option == null) {
                    selectedSeed == 0 && hasSystemColors
                } else {
                    (noneSelected && option === AppColorOptions.first()) ||
                        (selectedSeed == option.seed && selectedStyle == option.style.name)
                }
            val backgroundModifier =
                when {
                    option == null || scheme == null -> Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    option.isMix -> Modifier.background(Brush.linearGradient(listOf(scheme.primary, scheme.secondary, scheme.tertiary)))
                    else -> Modifier.background(scheme.primary)
                }
            val contentColor = if (scheme == null) MaterialTheme.colorScheme.onPrimaryContainer else scheme.onPrimary

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.large)
                        .then(backgroundModifier)
                        .clickable {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onSelected(option?.seed ?: 0, option?.style?.name)
                        },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected || option == null) {
                    Icon(
                        painter =
                            painterResource(
                                if (isSelected) R.drawable.rounded_check_24 else R.drawable.rounded_palette_24,
                            ),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
