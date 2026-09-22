package com.sameerasw.essentials.island.service

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.state.CameraAnchor
import com.sameerasw.essentials.island.state.CameraGeometry

object CameraGeometryResolver {
    fun resolve(context: Context, wm: WindowManager, settings: SettingsRepository): CameraGeometry {
        val density = context.resources.displayMetrics.density
        val (screenWidth, screenHeight) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val size = Point()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealSize(size)
            size.x to size.y
        }
        val sizeScale = settings.getIslandCameraSize()
        val gap = settings.getIslandCutoutGap() * density

        if (settings.isIslandAutoDetectEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val rect = try {
                wm.maximumWindowMetrics.windowInsets.displayCutout?.boundingRects
                    ?.let { rects -> rects.find { it.top == 0 } ?: rects.firstOrNull() }
            } catch (_: Exception) {
                null
            }
            if (rect != null) {
                val base = (minOf(rect.width(), rect.height()) / 2f).coerceAtLeast(12f * density)
                return CameraGeometry(
                    centerX = rect.exactCenterX(),
                    centerY = rect.exactCenterY(),
                    radius = base * sizeScale,
                    gap = gap,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    anchor = anchorFor(rect.exactCenterX(), screenWidth),
                )
            }
        }

        val centerX = settings.getIslandCameraOffsetX() / 100f * screenWidth
        return CameraGeometry(
            centerX = centerX,
            centerY = settings.getIslandCameraOffsetY() / 100f * screenHeight,
            radius = 16f * density * sizeScale,
            gap = gap,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            anchor = anchorFor(centerX, screenWidth),
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun anchorFor(centerX: Float, screenWidth: Int): CameraAnchor = CameraAnchor.Center
}
