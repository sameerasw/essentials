package com.sameerasw.essentials.island.plugins.media.particles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp

data class MediaVisualGeometry(val bounds: Rect, val cornerRadius: Float)

data class MediaCameraGeometry(val center: Offset, val radius: Float)

internal val MediaCameraClearance = 4.dp
