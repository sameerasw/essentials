package com.sameerasw.essentials.translation.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TranslationFocusOutline(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderWidth by animateDpAsState(
        targetValue = if (visible) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "TranslationFocusBorderWidth"
    )

    Box(
        modifier = modifier
            .border(
                width = borderWidth,
                color = if (visible) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(if (visible) 4.dp else 0.dp)
    ) {
        content()
    }
}
