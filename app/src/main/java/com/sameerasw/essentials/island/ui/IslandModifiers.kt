package com.sameerasw.essentials.island.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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

// Animates a child to its new slot, measured from the edge it hugs, so the parent resizing never reads as a move
fun Modifier.animatePlacement(fromEnd: Boolean = false): Modifier = composed {
    val scope = rememberCoroutineScope()
    val holder = remember { arrayOfNulls<Animatable<IntOffset, AnimationVector2D>>(1) }
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val position = coordinates?.positionInParent()?.round()
            if (position == null || isLookingAhead) {
                placeable.place(0, 0)
                return@layout
            }
            val parentWidth = coordinates?.parentLayoutCoordinates?.size?.width ?: 0
            val target = if (fromEnd) IntOffset(parentWidth - position.x, position.y) else position
            val anim = holder[0] ?: Animatable(target, IntOffset.VectorConverter).also { holder[0] = it }
            if (anim.targetValue != target) scope.launch { anim.animateTo(target, IslandMotion.compactOffset) }
            val current = anim.value
            val dx = if (fromEnd) target.x - current.x else current.x - target.x
            placeable.place(dx, current.y - target.y)
        }
    }
}

fun Modifier.squareFit(size: Dp): Modifier =
    sizeIn(maxWidth = size, maxHeight = size).aspectRatio(1f, matchHeightConstraintsFirst = true)

fun Modifier.islandOutline(color: Color?, thickness: Dp, shape: Shape): Modifier =
    if (color == null || color.alpha <= 0f) this else border(thickness, color, shape)
