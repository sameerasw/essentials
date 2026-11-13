package com.sameerasw.essentials.island.plugins.brief

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import com.sameerasw.essentials.island.ui.LocalIslandTextColor
import com.sameerasw.essentials.island.ui.components.LocalIslandChipInk
import com.sameerasw.essentials.island.ui.components.LocalIslandIconColor
import androidx.compose.ui.unit.dp

internal class BriefCarouselItem(
    val key: String,
    val onClick: (() -> Unit)?,
    val container: Color? = null,
    val content: @Composable () -> Unit,
)

private val PillWidth = 96.dp
private val PillGap = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BriefItemsCarousel(
    entries: List<BriefCarouselItem>,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth().height(height)) {
        val fits = PillWidth * entries.size + PillGap * (entries.size - 1).coerceAtLeast(0) <= maxWidth
        if (fits) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(PillGap, Alignment.CenterHorizontally),
            ) {
                entries.forEach { entry -> CarouselPill(entry, Modifier.width(PillWidth).fillMaxHeight()) }
            }
        } else {
            val state = rememberCarouselState { entries.size }
            HorizontalMultiBrowseCarousel(
                state = state,
                modifier = Modifier.fillMaxSize(),
                preferredItemWidth = PillWidth,
                itemSpacing = PillGap,
                contentPadding = PaddingValues(0.dp),
            ) { index ->
                CarouselPill(entries[index], Modifier.fillMaxSize().maskClip(RoundedCornerShape(50)))
            }
        }
    }
}

@Composable
private fun CarouselPill(entry: BriefCarouselItem, modifier: Modifier) {
    val container = entry.container?.takeIf { it.luminance() <= 0.4f }
    val ink = if (container != null) Color.White else null
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(container ?: Color.Black)
            .clickable(enabled = entry.onClick != null) { entry.onClick?.invoke() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalIslandIconColor provides ink,
            LocalIslandTextColor provides ink,
            LocalIslandChipInk provides ink,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                entry.content()
            }
        }
    }
}
