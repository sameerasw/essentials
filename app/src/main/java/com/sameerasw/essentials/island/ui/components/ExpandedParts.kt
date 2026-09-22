package com.sameerasw.essentials.island.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.IslandHaptics

// The top row of every expanded view: content split around the camera, same height as the compact pill.
@Composable
fun IslandExpandedScope.CameraRow(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = spec.expandedPadding,
    start: @Composable RowScope.() -> Unit,
    end: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(spec.compactHeight)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = start,
        )
        Spacer(Modifier.width(spec.cameraSlotWidth))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            content = end,
        )
    }
}

// Bottom-up tint in the app / artwork colour, matching the old "accent glow".
fun Modifier.accentGlow(color: Color, enabled: Boolean): Modifier =
    if (!enabled) this else background(
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to color.copy(alpha = 0.25f),
            1f to color.copy(alpha = 0.51f),
        ),
    )

class ConnectedItem(
    val onClick: () -> Unit,
    val content: @Composable () -> Unit,
)


@Composable
fun ConnectedButtonRow(
    items: List<ConnectedItem>,
    height: Dp,
    modifier: Modifier = Modifier,
    container: Color = Color.White.copy(alpha = 0.14f),
) {
    val context = LocalContext.current
    val outer = height / 2
    val inner = 4.dp
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { index, item ->
            val shape: Shape = when {
                items.size == 1 -> RoundedCornerShape(outer)
                index == 0 -> RoundedCornerShape(outer, inner, inner, outer)
                index == items.lastIndex -> RoundedCornerShape(inner, outer, outer, inner)
                else -> RoundedCornerShape(inner)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(shape)
                    .background(container)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White),
                    ) {
                        IslandHaptics.button(context)
                        item.onClick()
                    },
                contentAlignment = Alignment.Center,
            ) { item.content() }
        }
    }
}

@Composable
fun ConnectedTextLabel(text: String) {
    Text(
        text = text,
        style = IslandTextStyles.line,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}
