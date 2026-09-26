package com.sameerasw.essentials.island.ui

import com.sameerasw.essentials.island.state.CameraAnchor
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class IslandLayoutSpec(
    val cameraDiameter: Dp = 28.dp,
    val cameraGap: Dp = 6.dp,
    val verticalGap: Dp = 6.dp,
    val surfaceTop: Dp = 8.dp,
    val lineWidth: Dp = 360.dp,
    val expandedWidth: Dp = 360.dp,
    val expandedCorner: Dp = 24.dp,
    val expandedPadding: Dp = 16.dp,
    val expandedTopPadding: Dp = 0.dp,
    val expandedBottomPadding: Dp = 12.dp,
    val expandedScale: Float = 1f,
    val fontScale: Float = 1f,
    val expandedOutset: Dp = 0.dp,
    val cameraAnchor: CameraAnchor = CameraAnchor.Center,
    val outlineColor: Color? = null,
    val outlineThickness: Dp = 1.dp,
    val outlineHiddenWhenExpanded: Boolean = false,
    val pulseShadow: Boolean = false,
    val pulseSize: Float = 0.5f,
    val pulseYShift: Float = 0.35f,
    val pulseSpread: Float = 2f,
    val pulseDurationMs: Int = 1450,
) {
    val compactHeight: Dp get() = cameraDiameter + verticalGap * 2
    val cameraSlotWidth: Dp get() = cameraDiameter + cameraGap * 2
    val cellSize: Dp get() = cameraDiameter
    val cellSpacing: Dp get() = cameraGap

    val growDirection: Int get() = when (cameraAnchor) {
        CameraAnchor.Start -> 1
        CameraAnchor.End -> -1
        CameraAnchor.Center -> 0
    }
}
