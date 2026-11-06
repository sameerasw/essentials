package com.sameerasw.essentials.island.ui.components

import com.sameerasw.essentials.island.ui.motionSpring

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.aspectRatio
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import androidx.compose.runtime.collectAsState
import com.sameerasw.essentials.island.gestures.IslandMediaCue
import com.sameerasw.essentials.island.gestures.RingMode
import com.sameerasw.essentials.island.gestures.SlideFeedback
import com.sameerasw.essentials.island.ui.IslandLayoutSpec
import com.sameerasw.essentials.island.ui.IslandMotion
import com.sameerasw.essentials.island.ui.IslandTextStyles

// Volume / brightness / sound mode take over the whole pill: label on the far side, icon on the other.
@Composable
fun SlideFeedbackCompact(feedback: SlideFeedback, spec: IslandLayoutSpec) {
    Layout(
        modifier = Modifier.height(spec.compactHeight),
        content = {
            Box(contentAlignment = Alignment.CenterStart) {
                when (feedback) {
                    is SlideFeedback.Level -> RollingText("${feedback.percent}%")
                    is SlideFeedback.Sound -> AnimatedContent(
                        targetState = feedback.mode,
                        transitionSpec = {
                            (slideInVertically(IslandMotion.contentIn()) { it / 2 } + fadeIn(IslandMotion.contentIn()))
                                .togetherWith(slideOutVertically(IslandMotion.contentOut()) { -it / 2 } + fadeOut(IslandMotion.contentOut()))
                        },
                        label = "soundMode",
                    ) { mode ->
                        Text(stringResource(mode.labelRes), style = IslandTextStyles.compact)
                    }
                    is SlideFeedback.Track -> Unit
                }
            }
            Box(
                modifier = Modifier.defaultMinSize(minWidth = spec.cellSize),
                contentAlignment = Alignment.Center,
            ) {
                IslandIcon(feedback.iconRes(), tint = islandAccent())
            }
        },
    ) { measurables, constraints ->
        val loose = Constraints(maxHeight = constraints.maxHeight)
        val label = measurables[0].measure(loose)
        val icon = measurables[1].measure(loose)
        val spacing = spec.cellSpacing.roundToPx()
        val cameraSlot = spec.cameraSlotWidth.roundToPx()
        val height = spec.compactHeight.roundToPx()
        if (spec.growDirection != 0) {
            val width = cameraSlot + label.width + icon.width + spacing * 3
            return@Layout layout(width, height) {
                var x = if (spec.growDirection > 0) cameraSlot + spacing else spacing
                label.place(x, (height - label.height) / 2)
                x += label.width + spacing
                icon.place(x, (height - icon.height) / 2)
            }
        }
        val side = maxOf(label.width, icon.width) + spacing
        val width = side * 2 + cameraSlot
        layout(width, height) {
            label.place(spacing, (height - label.height) / 2)
            icon.place(width - spacing - icon.width, (height - icon.height) / 2)
        }
    }
}

@Composable
fun TrackSkipPill(next: Boolean, armed: Boolean, accent: Color) {
    val progress by animateFloatAsState(if (armed) 1f else 0f, IslandMotion.float(), label = "trackArmed")
    Box(
        modifier = Modifier
            .height(20.dp)
            .defaultMinSize(minWidth = 34.dp)
            .clip(RoundedCornerShape(50))
            .background(lerpColor(Color.White.copy(alpha = 0.22f), accent, progress)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IslandIcon(
                res = if (next) R.drawable.rounded_skip_next_24 else R.drawable.rounded_skip_previous_24,
                tint = lerpColor(Color.White, Color.Black, progress),
                size = 16.dp,
            )
        }
    }
}

private fun lerpColor(from: Color, to: Color, t: Float) = Color(
    red = from.red + (to.red - from.red) * t,
    green = from.green + (to.green - from.green) * t,
    blue = from.blue + (to.blue - from.blue) * t,
    alpha = from.alpha + (to.alpha - from.alpha) * t,
)

private val RingMode.labelRes: Int
    get() = when (this) {
        RingMode.Normal -> R.string.sound_mode_sound
        RingMode.Vibrate -> R.string.sound_mode_vibrate
        RingMode.Silent -> R.string.sound_mode_silent
    }

private fun SlideFeedback.iconRes(): Int = when (this) {
    is SlideFeedback.Level -> when {
        brightness -> R.drawable.rounded_brightness_6_24
        percent <= 0 -> R.drawable.rounded_volume_off_24
        percent < 50 -> R.drawable.rounded_volume_down_24
        else -> R.drawable.rounded_volume_up_24
    }
    is SlideFeedback.Sound -> when (mode) {
        RingMode.Normal -> R.drawable.rounded_volume_up_24
        RingMode.Vibrate -> R.drawable.rounded_mobile_vibrate_24
        RingMode.Silent -> R.drawable.rounded_volume_off_24
    }
    is SlideFeedback.Track -> if (next) R.drawable.rounded_skip_next_24 else R.drawable.rounded_skip_previous_24
}

@Composable
fun MediaArtCue(artwork: Bitmap?, accent: Color, size: Dp) {
    val liked by IslandMediaCue.liked.collectAsState()
    val pop by animateFloatAsState(
        targetValue = if (liked) 1f else 0f,
        animationSpec = motionSpring(dampingRatio = 0.5f, stiffness = 500f),
        label = "likedPop",
    )
    Box(
        Modifier
            .sizeIn(maxWidth = size, maxHeight = size)
            .aspectRatio(1f, matchHeightConstraintsFirst = true),
        contentAlignment = Alignment.Center,
    ) {
        if (pop < 1f) {
            Box(
                Modifier.graphicsLayer {
                    alpha = 1f - pop
                    val s = 1f - 0.25f * pop
                    scaleX = s
                    scaleY = s
                },
            ) { IslandBitmap(artwork, size, circle = true) }
        }
        if (pop > 0f) {
            IslandIcon(
                res = R.drawable.rounded_favorite_24,
                tint = accent,
                size = size,
                modifier = Modifier.graphicsLayer {
                    alpha = pop
                    val s = 0.6f + 0.4f * pop
                    scaleX = s
                    scaleY = s
                },
            )
        }
    }
}
