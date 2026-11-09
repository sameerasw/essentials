package com.sameerasw.essentials.island.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.sameerasw.essentials.island.plugins.media.MediaSurface
import com.sameerasw.essentials.island.plugins.media.particles.GeminiParticlePill
import com.sameerasw.essentials.island.plugins.media.particles.MediaCameraGeometry
import com.sameerasw.essentials.island.plugins.media.particles.MediaVisualGeometry

@Composable
internal fun IslandSurfaceVisuals(accent: Color, corner: Dp, particles: Boolean): Boolean =
    DetachedSurfaceVisuals { frame ->
        val geometry = { frame().let { MediaVisualGeometry(it.bounds, it.cornerRadius) } }
        val camera = { frame().let { MediaCameraGeometry(it.cameraCenter, it.cameraRadius) } }
        Box(Modifier.fillMaxSize()) {
            MediaSurface(accent, corner, { frame().pulse }, geometry, camera, Modifier.matchParentSize())
            if (particles) {
                GeminiParticlePill(
                    modifier = Modifier.matchParentSize(),
                    color = accent,
                    cornerRadius = corner,
                    activation = { frame().pulse },
                    geometry = geometry,
                    protectedCamera = camera,
                )
            }
        }
    }
