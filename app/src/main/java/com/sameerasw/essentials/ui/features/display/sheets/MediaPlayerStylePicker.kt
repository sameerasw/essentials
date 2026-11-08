package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.island.plugins.media.MediaPlayerStyle
import com.sameerasw.essentials.island.ui.components.EqualizerBars
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPlayerStylePicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val styles = MediaPlayerStyle.entries
    val state = rememberCarouselState { styles.size }
    val selectedStyle = MediaPlayerStyle.from(selected)
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = 240.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
    ) { index ->
        val style = styles[index]
        val isSelected = style == selectedStyle
        val container by animateColorAsState(
            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright,
            label = "playerStyleContainer",
        )
        Box(
            Modifier
                .fillMaxSize()
                .maskClip(RoundedCornerShape(24.dp))
                .background(container)
                .clickable {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onSelect(style.key)
                }
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            PlayerStylePreview(style = style, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PlayerStylePreview(style: MediaPlayerStyle, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val cardW = minOf(maxWidth, maxHeight * 2.7f)
        val cardH = cardW / 2.7f
        val pad = cardH * 0.08f
        val art = cardH - pad * 2
        Box(
            Modifier
                .size(cardW, cardH)
                .clip(RoundedCornerShape(cardH * 0.22f))
                .background(Color.Black)
                .padding(pad),
        ) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(pad)) {
                Box(
                    Modifier
                        .size(art)
                        .clip(RoundedCornerShape(art * 0.14f))
                        .background(Brush.linearGradient(listOf(primary, tertiary))),
                    contentAlignment = Alignment.Center,
                ) {
                    if (style == MediaPlayerStyle.ArtControls) {
                        EqualizerBars(true, Color.White, size = art * 0.28f)
                    } else {
                        Icon(
                            painterResource(R.drawable.rounded_favorite_24),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(art * 0.28f),
                        )
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    val line = cardH * 0.08f
                    if (style == MediaPlayerStyle.ArtControls) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            Icon(
                                painterResource(R.drawable.round_favorite_24),
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(cardH * 0.16f),
                            )
                            Column(
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(bottom = pad * 0.6f),
                                verticalArrangement = Arrangement.spacedBy(line * 0.6f),
                            ) {
                                PreviewLine(0.75f, line, Color.White)
                                PreviewLine(0.45f, line, Color.White.copy(alpha = 0.55f))
                            }
                        }
                    } else {
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(line * 0.6f),
                                ) {
                                    PreviewLine(0.85f, line, Color.White)
                                    PreviewLine(0.55f, line, Color.White.copy(alpha = 0.55f))
                                }
                                Box(
                                    Modifier
                                        .size(cardH * 0.3f, cardH * 0.22f)
                                        .clip(CircleShape)
                                        .background(primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painterResource(R.drawable.rounded_pause_24),
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(cardH * 0.14f),
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(cardH * 0.18f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .weight(0.45f)
                                .height(line * 0.45f)
                                .clip(CircleShape)
                                .background(primary),
                        )
                        Box(
                            Modifier
                                .weight(0.55f)
                                .height(line * 0.45f)
                                .clip(CircleShape)
                                .background(primary.copy(alpha = 0.35f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewLine(fraction: Float, height: androidx.compose.ui.unit.Dp, color: Color) {
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(CircleShape)
            .background(color),
    )
}
