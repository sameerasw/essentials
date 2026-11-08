package com.sameerasw.essentials.island.ui.components

import com.sameerasw.essentials.island.ui.LocalIslandTextColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.IslandHaptics
import com.sameerasw.essentials.island.ui.IslandMorph
import com.sameerasw.essentials.island.ui.morphTag
import androidx.compose.ui.unit.sp

@Composable
fun IslandExpandedScope.CameraRow(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = spec.expandedPadding,
    title: String? = null,
    titlePadding: Dp = spec.expandedPadding + spec.expandedCorner * 0.35f,
    start: @Composable RowScope.() -> Unit,
    end: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(spec.compactHeight)
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val startRow: @Composable RowScope.() -> Unit = {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Row(
                        modifier = Modifier.morphTag(IslandMorph.ICON),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        content = start,
                    )
                }
            }
            val endRow: @Composable RowScope.(Modifier) -> Unit = { outer ->
                Box(outer, contentAlignment = Alignment.CenterEnd) {
                    Row(
                        modifier = Modifier.morphTag(IslandMorph.END),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        content = end,
                    )
                }
            }
            when {
                spec.growDirection > 0 -> {
                    Spacer(Modifier.width(spec.cameraSlotWidth))
                    startRow()
                    endRow(Modifier)
                }
                spec.growDirection < 0 -> {
                    startRow()
                    endRow(Modifier)
                    Spacer(Modifier.width(spec.cameraSlotWidth))
                }
                else -> {
                    startRow()
                    Spacer(Modifier.width(spec.cameraSlotWidth))
                    endRow(Modifier.weight(1f))
                }
            }
        }
        if (!title.isNullOrBlank()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = titlePadding)) {
                MarqueeText(
                    text = title,
                    style = IslandTextStyles.title.copy(fontSize = 17.sp),
                    modifier = Modifier.morphTag(IslandMorph.TITLE),
                )
            }
        }
    }
}

val IslandExpandedScope.cameraClearance: Dp
    get() = spec.expandedOutset + spec.expandedTopPadding + spec.compactHeight + 8.dp

fun Modifier.accentGlow(color: Color, enabled: Boolean, clearTop: Dp): Modifier =
    if (!enabled) this else drawBehind {
        val top = clearTop.toPx().coerceAtMost(size.height)
        if (size.height - top <= 0f) return@drawBehind
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.55f to color.copy(alpha = 0.25f),
                1f to color.copy(alpha = 0.51f),
                startY = top,
                endY = size.height,
            ),
            topLeft = Offset(0f, top),
            size = Size(size.width, size.height - top),
        )
    }

class ConnectedItem(
    val onClick: () -> Unit,
    val container: Color? = null,
    val enabled: Boolean = true,
    val content: @Composable () -> Unit
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
            val alpha = if (item.enabled) 1f else 0.6f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(shape)
                    .alpha(alpha)
                    .background(item.container ?: container)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White),
                    ) {
                        if (item.enabled) {
                            IslandHaptics.button(context)
                            item.onClick()
                        } else {
                            IslandHaptics.wiggle(context)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (item.container != null) {
                    CompositionLocalProvider(LocalIslandIconColor provides null, LocalIslandTextColor provides null) { item.content() }
                } else {
                    item.content()
                }
            }
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

/**
 * How the artwork backdrop is softened.
 *
 * @param sourceWidth Width the artwork is shrunk to before the GPU blur; lower keeps less detail.
 * @param saturation Color saturation of the backdrop; 1 keeps the artwork's colors.
 */
data class ArtworkBlur(
    val sourceWidth: Int = DEFAULT_SOURCE_WIDTH,
    val saturation: Float = DEFAULT_SATURATION,
) {
    companion object {
        const val DEFAULT_SOURCE_WIDTH = 96
        const val DEFAULT_SATURATION = 1.35f
        val SOURCE_WIDTH_RANGE = 8f..192f
        val SATURATION_RANGE = 0f..2f
    }
}

@Composable
fun IslandExpandedScope.ArtworkBackdrop(image: ImageBitmap?, modifier: Modifier = Modifier, blur: ArtworkBlur = ArtworkBlur()) {
    if (image == null) return
    val clearTop = cameraClearance
    val gpuBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val sourceWidth = if (gpuBlur) blur.sourceWidth else SOFT_SOURCE_WIDTH
    val soft by produceState<ImageBitmap?>(null, image, sourceWidth) {
        value = withContext(Dispatchers.Default) {
            runCatching { softened(image.asAndroidBitmap(), sourceWidth) }.getOrNull()?.asImageBitmap()
        }
    }
    val saturation = remember(blur.saturation) { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(blur.saturation) }) }
    Box(modifier) {
        soft?.let { blurred ->
            Image(
                bitmap = blurred,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                colorFilter = saturation,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1.08f
                        scaleY = 1.08f
                    }
                    .then(if (gpuBlur) Modifier.blur(BACKDROP_BLUR, BlurredEdgeTreatment.Rectangle) else Modifier)
                    .alpha(0.67f),
            )
        }
        Box(
            Modifier.matchParentSize().drawBehind {
                val top = clearTop.toPx().coerceAtMost(size.height)
                
                drawRect(Color.Black, size = Size(size.width, (top + 1f).coerceAtMost(size.height)))
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        0.5f to Color.Black.copy(alpha = 0.59f),
                        1f to Color.Transparent,
                        startY = top,
                        endY = size.height,
                    ),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, size.height - top),
                )
            },
        )
    }
}

private const val SOFT_SOURCE_WIDTH = 28
private val BACKDROP_BLUR = 28.dp

private fun softened(source: Bitmap, width: Int): Bitmap {
    val readable = if (source.config == Bitmap.Config.HARDWARE) source.copy(Bitmap.Config.ARGB_8888, false) else source
    val height = (width * readable.height / readable.width.toFloat()).roundToInt().coerceAtLeast(8)
    val mid = Bitmap.createScaledBitmap(readable, width * 2, height * 2, true)
    return Bitmap.createScaledBitmap(mid, width, height, true)
}
