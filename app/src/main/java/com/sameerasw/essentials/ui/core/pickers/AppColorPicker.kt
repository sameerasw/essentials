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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.materialkolor.dynamicColorScheme
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.modifiers.consumeHorizontalScroll
import com.sameerasw.essentials.ui.theme.AppColorOption
import com.sameerasw.essentials.ui.theme.AppColorVariant
import com.sameerasw.essentials.ui.theme.AppColorOptions
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppColorPicker(
    selectedSeed: Int,
    selectedStyle: String?,
    selectedVariant: String,
    onVariantSelected: (String) -> Unit,
    onSelected: (Int, String?) -> Unit,
    modifier: Modifier = Modifier,
    onSelectedWithPosition: ((Offset) -> Unit)? = null,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val variant = AppColorVariant.from(selectedVariant)
    var showVariantMenu by remember { mutableStateOf(false) }
    val hasSystemColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val items = remember(hasSystemColors) { if (hasSystemColors) listOf<AppColorOption?>(null) + AppColorOptions else AppColorOptions }
    val schemes =
        remember(items, isDark, variant) {
            items.map { option ->
                if (option == null) {
                    if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    dynamicColorScheme(seedColor = option.seedColor, isDark = isDark, style = option.styleFor(variant))
                }
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
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
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
                modifier = Modifier.padding(start = 16.dp).weight(1f),
            )
            var pillCenter by remember { mutableStateOf(Offset.Zero) }
            Box {
                Row(
                    modifier =
                        Modifier
                            .onGloballyPositioned { pillCenter = it.centerInRoot() }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable {
                                HapticUtil.performVirtualKeyHaptic(view)
                                showVariantMenu = true
                            }.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TranslatableText(
                        stringResId = variant.labelRes,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SegmentedDropdownMenu(
                    expanded = showVariantMenu,
                    onDismissRequest = { showVariantMenu = false },
                ) {
                    AppColorVariant.entries.forEach { entry ->
                        SegmentedDropdownMenuItem(
                            text = { TranslatableText(entry.labelRes) },
                            onClick = {
                                showVariantMenu = false
                                onVariantSelected(entry.name)
                                onSelectedWithPosition?.invoke(pillCenter)
                                val current = items.firstOrNull { it != null && it.seed == selectedSeed && it.styleFor(variant).name == selectedStyle }
                                if (current != null && !current.isMix) {
                                    onSelected(current.seed, entry.style.name)
                                }
                            },
                            trailingIcon =
                                if (entry == variant) {
                                    {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_check_24),
                                            contentDescription = null,
                                        )
                                    }
                                } else {
                                    null
                                },
                        )
                    }
                }
            }
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
                    .height(64.dp)
                    .consumeHorizontalScroll(),
        ) { index ->
            val option = items[index]
            val scheme = schemes[index]
            val isSelected =
                if (option == null) {
                    selectedSeed == 0 && hasSystemColors
                } else {
                    (noneSelected && option === AppColorOptions.first()) ||
                        (selectedSeed == option.seed && selectedStyle == option.styleFor(variant).name)
                }
            val backgroundModifier =
                when {
                    option == null || scheme == null -> Modifier.background(scheme?.primary ?: MaterialTheme.colorScheme.primaryContainer)
                    option.isMix -> Modifier.background(Brush.linearGradient(listOf(scheme.primary, scheme.secondary, scheme.tertiary)))
                    else -> Modifier.background(scheme.primary)
                }
            val contentColor = scheme?.onPrimary ?: MaterialTheme.colorScheme.onPrimaryContainer

            var cardCenter by remember { mutableStateOf(Offset.Zero) }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { cardCenter = it.centerInRoot() }
                        .maskClip(MaterialTheme.shapes.large)
                        .then(backgroundModifier)
                        .clickable {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onSelected(option?.seed ?: 0, option?.styleFor(variant)?.name)
                            onSelectedWithPosition?.invoke(cardCenter)
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

private fun androidx.compose.ui.layout.LayoutCoordinates.centerInRoot(): Offset {
    val pos = positionInRoot()
    return Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
}
