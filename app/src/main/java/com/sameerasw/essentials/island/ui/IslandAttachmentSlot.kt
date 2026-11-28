package com.sameerasw.essentials.island.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.sameerasw.essentials.island.model.IslandAttachment
import kotlin.math.roundToInt

/**
 * Draws [attachment] centred below the expanded surface as a second droplet, bonded to the card through the
 * liquid field. Keeps the last attachment while it fades out so the shape can leave with its content.
 *
 * @param centerX Window x of the card's centre, read during layout so it follows the card live.
 * @param top Window y the attachment starts at (card bottom plus gap), read during layout.
 * @param collapse Collapse progress of the expanded stage; the attachment leaves first and the live surface pauses.
 * @param glow Accent of the card above, so the attachment's inner glow matches it.
 * @param spec Island style; the attachment reuses the card's transparency and glow settings.
 * @param active Whether the anchor is live (media playing); the backdrop's lights hold still while false.
 * @param positionMs Anchor's playback position, which the lights lock to when the attachment carries a beat.
 */
@Composable
fun IslandAttachmentSlot(
    attachment: IslandAttachment?,
    shown: Boolean,
    widthPx: Int,
    centerX: () -> Float,
    top: () -> Float,
    collapse: () -> Float,
    outlineColor: Color?,
    outlineThickness: Dp,
    onSizeChanged: (IntSize) -> Unit,
    glow: Color,
    spec: IslandLayoutSpec,
    active: Boolean,
    positionMs: (() -> Long)?,
) {
    val last = remember { arrayOfNulls<IslandAttachment>(1) }
    if (attachment != null) last[0] = attachment
    val presence = animateFloatAsState(
        if (shown && attachment != null) 1f else 0f,
        IslandMotion.float(),
        label = "attachmentPresence",
    )
    val current = last[0] ?: return
    if (presence.value <= 0.001f && !shown) return
    val latestCollapse by rememberUpdatedState(collapse)
    fun visible(): Float = presence.value * (1f - latestCollapse() * 2f).coerceIn(0f, 1f)
    val maxCornerPx = with(LocalDensity.current) { IslandAttachment.CORNER.toPx() }
    val bond = remember(maxCornerPx) {
        LiquidBond(
            "attachment",
            radius = { minOf(maxCornerPx, it.height / 2f) },
            presence = { visible() })
    }
    val body = rememberLiquidBody(LiquidSpec.Droplet, bond)
    var heightPx by remember { mutableIntStateOf(0) }
    val corner = with(LocalDensity.current) {
        if (heightPx > 0) minOf(
            IslandAttachment.CORNER,
            (heightPx / 2f).toDp()
        ) else IslandAttachment.CORNER
    }
    val shape = RoundedCornerShape(corner)
    val settled by remember { derivedStateOf { presence.value > 0.99f && latestCollapse() <= 0.02f } }
    val cornerPx = with(LocalDensity.current) { corner.toPx() }
    Box(Modifier.fillMaxSize()) {
        if (spec.backdropBlur) {
            val blurRadiusPx = with(LocalDensity.current) { spec.backdropBlurRadius.toPx() }
            IslandBackdropBlur(
                {
                    val v = visible()
                    val s = 0.85f + 0.15f * v
                    val cx = centerX()
                    val y = top()
                    val w = widthPx * s
                    val h = heightPx * s
                    IslandVisualFrame(
                        bounds = Rect(cx - w / 2f, y, cx + w / 2f, y + h),
                        cornerRadius = cornerPx * s,
                        alpha = if (heightPx > 0) v else 0f,
                        pulse = 0f,
                        cameraCenter = Offset.Zero,
                        cameraRadius = 0f,
                    )
                },
                blurRadiusPx,
                Modifier.matchParentSize(),
            )
        }
        Box(
            Modifier
                .layout { measurable, constraints ->
                    val p = measurable.measure(Constraints(minWidth = widthPx, maxWidth = widthPx))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        p.placeWithLayer(
                            (centerX() - p.width / 2f).roundToInt(),
                            top().roundToInt()
                        ) {
                            val v = visible()
                            alpha = v
                            val s = 0.85f + 0.15f * v
                            scaleX = s
                            scaleY = s
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        }
                    }
                }
                .onSizeChanged {
                    heightPx = it.height
                    onSizeChanged(it)
                }
                .liquid(body, TransformOrigin(0.5f, 0f))
                .clip(shape)
                .islandOutline(outlineColor, outlineThickness, shape),
        ) {
            IslandAttachmentSurface(
                accent = glow,
                corner = corner,
                spec = spec,
                active = active,
                beat = current.beat?.takeIf { positionMs != null },
                positionMs = positionMs,
                modifier = Modifier.matchParentSize(),
            )
            current.content(widthPx, settled)
        }
    }
}
