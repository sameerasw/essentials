package com.sameerasw.essentials.island.service

import androidx.compose.ui.unit.IntRect
import com.sameerasw.essentials.island.state.CameraGeometry
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

internal fun islandDrawViewport(geometry: CameraGeometry): IntRect {
    val reach = max(geometry.centerX, geometry.screenWidth - geometry.centerX)
    return IntRect(floor(geometry.centerX - reach).toInt(), 0,
        ceil(geometry.centerX + reach).toInt(), geometry.screenHeight)
}
