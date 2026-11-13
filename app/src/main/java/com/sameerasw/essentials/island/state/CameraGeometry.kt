package com.sameerasw.essentials.island.state

enum class CameraAnchor { Center, Start, End }

data class CameraGeometry(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val gap: Float,
    val screenWidth: Int,
    val screenHeight: Int,
    val anchor: CameraAnchor = CameraAnchor.Center,
    val verticalGap: Float = gap,
    val hasCamera: Boolean = true,
    val contentGap: Float = gap,
) {
    val diameter: Float get() = radius * 2f
    val compactHeight: Float get() = diameter + verticalGap * 2f
    val surfaceTop: Float get() = (centerY - radius - verticalGap).coerceAtLeast(0f)
    val edgeInset: Float get() = compactHeight * 0.15f * (if (verticalGap > 0f) contentGap / verticalGap else 1f)
    val contentSize: Float get() = (diameter + (verticalGap - contentGap) * 2f).coerceAtLeast(diameter * 0.5f)
    val cameraSlotWidth: Float get() = if (hasCamera) diameter + gap * 2f else 0f
}
