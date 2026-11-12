package com.sameerasw.essentials.island.service

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.RectF
import android.os.Build
import android.view.WindowManager
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.state.CameraAnchor
import com.sameerasw.essentials.island.state.CameraGeometry
import com.sameerasw.essentials.utils.DeviceUtils

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
        val verticalGap = 6f * density

        val manualInLandscape = settings.isIslandKeepOnLandscapeEnabled() &&
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (!manualInLandscape && settings.isIslandAutoDetectEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val rect = cutoutBounds(wm)
            if (rect != null) {
                val base = cutoutRadius(rect, density)
                return CameraGeometry(
                    centerX = rect.centerX(),
                    centerY = rect.centerY(),
                    radius = base * sizeScale,
                    gap = gap,
                    verticalGap = verticalGap,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    anchor = anchorFor(settings),
                )
            }
        }

        val centerX = settings.getIslandCameraOffsetX() / 100f * screenWidth
        val radius = 16f * density * sizeScale
        val centerY = if (manualInLandscape) {
            settings.getIslandLandscapeTopSpacing() * density + verticalGap + radius
        } else {
            settings.getIslandCameraOffsetY() / 100f * screenHeight
        }
        return CameraGeometry(
            centerX = centerX,
            centerY = centerY,
            radius = radius,
            gap = gap,
            verticalGap = verticalGap,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            anchor = anchorFor(settings),
            hasCamera = !manualInLandscape,
        )
    }

    fun cutoutBounds(wm: WindowManager): RectF? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val cutout = try {
            wm.maximumWindowMetrics.windowInsets.displayCutout
        } catch (_: Exception) {
            null
        } ?: return null
        val rect = cutout.boundingRects.let { rects -> rects.find { it.top == 0 } ?: rects.firstOrNull() } ?: return null
        val loose = RectF(rect)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val tight = RectF()
            try {
                cutout.cutoutPath?.computeBounds(tight, true)
            } catch (_: Exception) {
                tight.setEmpty()
            }
            if (!tight.isEmpty && RectF(loose).apply { inset(-1f, -1f) }.contains(tight)) return tight
        }
        return loose
    }

    fun cutoutRadius(bounds: RectF, density: Float): Float =
        (minOf(bounds.width(), bounds.height()) / 2f).coerceAtLeast(6f * density)

    fun lensRadius(bounds: RectF, density: Float): Float {
        val radius = minOf(bounds.width(), bounds.height()) / 2f
        val trimmed = if (DeviceUtils.isGoogleDevice()) radius * PIXEL_LENS_RATIO else radius
        return trimmed.coerceAtLeast(6f * density)
    }

    private const val PIXEL_LENS_RATIO = 0.67f

    @Suppress("UNUSED_PARAMETER")
    private fun anchorFor(settings: SettingsRepository): CameraAnchor = when (settings.getIslandCameraPosition()) {
        SettingsRepository.ISLAND_CAMERA_POSITION_LEFT -> CameraAnchor.Start
        SettingsRepository.ISLAND_CAMERA_POSITION_RIGHT -> CameraAnchor.End
        else -> CameraAnchor.Center
    }
}
