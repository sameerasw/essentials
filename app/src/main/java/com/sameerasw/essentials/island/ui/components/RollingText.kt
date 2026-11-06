package com.sameerasw.essentials.island.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.sameerasw.essentials.island.ui.IslandTextStyles

@Composable
fun RollingText(text: String, modifier: Modifier = Modifier, style: TextStyle = IslandTextStyles.compact) {
    val tracker = remember { DirectionTracker() }
    val rollDown = tracker.update(text)
    // Enforce LTR layout for numeric/time displays on RTL locales while preserving RTL for text.
    // See: https://github.com/sameerasw/essentials/issues/1555
    val direction = if (text.isRtlText()) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Row(modifier.clipToBounds()) {
            text.forEachIndexed { index, char ->
                key(text.length - index) {
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = {
                            val slide = com.sameerasw.essentials.island.ui.motionSpring<IntOffset>(dampingRatio = 0.8f, stiffness = 500f)
                            val sign = if (rollDown) -1 else 1
                            (slideInVertically(slide) { sign * it } + fadeIn())
                                .togetherWith(slideOutVertically(slide) { -sign * it } + fadeOut())
                                .using(SizeTransform(clip = true))
                        },
                        label = "roll",
                    ) { Text(it.toString(), style = style) }
                }
            }
        }
    }
}

private fun String.isRtlText(): Boolean {
    val first = firstOrNull { !it.isWhitespace() } ?: return false
    if (first.isDigit()) return false
    return any {
        val dir = Character.getDirectionality(it)
        dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
    }
}

private class DirectionTracker {
    private var last: String? = null
    private var down = false

    fun update(text: String): Boolean {
        val previous = last
        if (previous != null && previous != text) {
            val old = previous.filter(Char::isDigit).toLongOrNull()
            val new = text.filter(Char::isDigit).toLongOrNull()
            if (old != null && new != null && old != new) down = new < old
        }
        last = text
        return down
    }
}
