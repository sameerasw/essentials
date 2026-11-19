package com.sameerasw.essentials.island.ui.components

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.sameerasw.essentials.island.state.MarqueeProgress
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val MARQUEE_INITIAL_DELAY_MS = 1200L
private const val MARQUEE_VELOCITY_DP_PER_S = 30f
private val MARQUEE_SPACING = 28.dp

@Composable
fun MarqueeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    fadeStart: Boolean = true,
    fadeEnd: Boolean = true,
    fadeWidth: Dp = 14.dp,
    reportProgress: Boolean = false,
) = MarqueeText(AnnotatedString(text), style, modifier, textAlign, fadeStart, fadeEnd, fadeWidth, reportProgress)

@Composable
fun MarqueeText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    fadeStart: Boolean = true,
    fadeEnd: Boolean = true,
    fadeWidth: Dp = 14.dp,
    reportProgress: Boolean = false,
) {
    val resolved = remember(style, text) { style.forContent(text.text) }
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val textWidth = remember(text, resolved) { measurer.measure(text, resolved, maxLines = 1, softWrap = false).size.width }
        val overflows = constraints.hasBoundedWidth && textWidth > constraints.maxWidth
        if (!overflows) {
            Text(text, style = resolved.copy(textAlign = textAlign), maxLines = 1, softWrap = false, modifier = Modifier.fillMaxWidth())
        } else if (islandStill()) {
            Text(text, style = resolved.copy(textAlign = textAlign), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        } else {
            if (reportProgress) {
                val density = LocalDensity.current
                val owner = remember { Any() }
                DisposableEffect(text, textWidth) {
                    val travelDp = with(density) { textWidth.toDp() } + MARQUEE_SPACING
                    MarqueeProgress.start(owner, MARQUEE_INITIAL_DELAY_MS + (travelDp.value / MARQUEE_VELOCITY_DP_PER_S * 1000).toLong())
                    onDispose { MarqueeProgress.clear(owner) }
                }
            }
            Text(
                text = text,
                style = resolved,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val f = (fadeWidth.toPx() / size.width).coerceIn(0f, 0.4f)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to if (fadeStart) Color.Transparent else Color.Black,
                                f to Color.Black,
                                1f - f to Color.Black,
                                1f to if (fadeEnd) Color.Transparent else Color.Black,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = MARQUEE_INITIAL_DELAY_MS.toInt(),
                        repeatDelayMillis = 1200,
                        velocity = MARQUEE_VELOCITY_DP_PER_S.dp,
                        spacing = MarqueeSpacing(MARQUEE_SPACING),
                    ),
            )
        }
    }
}
