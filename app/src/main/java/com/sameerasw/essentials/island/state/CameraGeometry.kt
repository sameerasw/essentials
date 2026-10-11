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
) {
    val diameter: Float get() = radius * 2f
    val compactHeight: Float get() = diameter + verticalGap * 2f
    val surfaceTop: Float get() = (centerY - radius - verticalGap).coerceAtLeast(0f)
    val cameraSlotWidth: Float get() = if (hasCamera) diameter + gap * 2f else 0f
}
