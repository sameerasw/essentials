package com.sameerasw.essentials.island.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import kotlinx.coroutines.launch

// Animates a child to its new slot whenever the parent layout moves it
fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    val holder = remember { arrayOfNulls<Animatable<IntOffset, AnimationVector2D>>(1) }
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val target = coordinates?.positionInParent()?.round()
            if (target == null || isLookingAhead) {
                placeable.place(0, 0)
                return@layout
            }
            val anim = holder[0] ?: Animatable(target, IntOffset.VectorConverter).also { holder[0] = it }
            if (anim.targetValue != target) scope.launch { anim.animateTo(target, IslandMotion.compactOffset) }
            val current = anim.value
            placeable.place(current.x - target.x, current.y - target.y)
        }
    }
}

fun Modifier.squareFit(size: Dp): Modifier =
    sizeIn(maxWidth = size, maxHeight = size).aspectRatio(1f, matchHeightConstraintsFirst = true)
