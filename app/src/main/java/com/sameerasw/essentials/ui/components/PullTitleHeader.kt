package com.sameerasw.essentials.ui.components

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

@Stable
class PullTitleHeaderState(
    val minHeight: Dp,
    val maxHeight: Dp,
    private val density: Density,
    private val view: View,
) {
    var height by mutableStateOf(minHeight)
        private set

    val fraction: Float
        get() = ((height - minHeight) / (maxHeight - minHeight)).coerceIn(0f, 1f)

    val nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val delta = available.y
                if (delta < 0 && height > minHeight) {
                    val oldHeight = height
                    height = with(density) { (oldHeight.toPx() + delta).toDp() }.coerceAtLeast(minHeight)
                    val consumed = oldHeight - height
                    return Offset(0f, with(density) { -consumed.toPx() })
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val delta = available.y
                if (delta > 0 && source == NestedScrollSource.UserInput) {
                    val oldHeight = height
                    height = with(density) { (oldHeight.toPx() + delta).toDp() }.coerceAtMost(maxHeight)
                    if (height == maxHeight && oldHeight < maxHeight) {
                        HapticUtil.performLightHaptic(view)
                    }
                    val produced = height - oldHeight
                    return Offset(0f, with(density) { produced.toPx() })
                }
                return Offset.Zero
            }
        }
}

@Composable
fun rememberPullTitleHeaderState(
    minHeight: Dp = 140.dp,
    maxHeight: Dp = 320.dp,
): PullTitleHeaderState {
    val density = LocalDensity.current
    val view = LocalView.current
    return remember(minHeight, maxHeight) {
        PullTitleHeaderState(minHeight, maxHeight, density, view)
    }
}

@Composable
fun PullTitleHeader(
    title: String,
    state: PullTitleHeaderState,
    modifier: Modifier = Modifier,
) {
    val fraction = state.fraction
    val fontFamily =
        remember(fraction) {
            FontFamily(
                Font(
                    R.font.google_sans_flex,
                    variationSettings =
                        FontVariation.Settings(
                            FontVariation.width(100f + fraction * 25f),
                            FontVariation.weight(500 + (fraction * 200).toInt()),
                            FontVariation.Setting("ROND", 100f),
                        ),
                ),
            )
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(state.height)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = fontFamily),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier =
                Modifier.graphicsLayer {
                    val scale = 1f + fraction * 0.25f
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}
