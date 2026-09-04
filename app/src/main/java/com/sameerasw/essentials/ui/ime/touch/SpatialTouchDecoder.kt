/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: SpatialTouchDecoder.kt
 * Description: Gaussian distance probability touch decoding for gap-free key hits.
 */

package com.sameerasw.essentials.ui.ime.touch

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot

internal interface TouchDecoder {
    fun decode(
        x: Float,
        y: Float,
        layout: KeyboardLayout,
    ): DecodeResult
}

internal class SpatialTouchDecoder : TouchDecoder {
    override fun decode(
        x: Float,
        y: Float,
        layout: KeyboardLayout,
    ): DecodeResult {
        val containing = layout.keys.filter { it.logical.contains(x, y) }
        val inside = containing.singleOrNull()
        val sigmaX = layout.letterWidth * KeyGeometry.SIGMA_X
        val sigmaY = layout.rowHeight * KeyGeometry.SIGMA_Y

        if (inside != null && inside.action == KeyAction.SPACE) {
            val visualOther = layout.keys.firstOrNull { it.id != inside.id && it.visual.contains(x, y) }
            val chosen = visualOther ?: inside
            val cx = chosen.geometricCenterX
            val cy = chosen.geometricCenterY
            val spatial = gaussian(x, y, cx, cy, sigmaX, sigmaY)
            return DecodeResult(
                selected = chosen,
                candidates = listOf(ScoredCandidate(chosen, spatial, hypot(x - cx, y - cy))),
                containing = inside,
                clearCenter = visualOther == null,
            )
        }

        if (inside != null) {
            val cx = inside.geometricCenterX
            val cy = inside.geometricCenterY
            val nx = abs(x - cx) / (inside.logical.width * 0.5f).coerceAtLeast(1f)
            val ny = abs(y - cy) / (inside.logical.height * 0.5f).coerceAtLeast(1f)
            if (nx < KeyGeometry.CLEAR_CENTER && ny < KeyGeometry.CLEAR_CENTER) {
                val spatial = gaussian(x, y, cx, cy, sigmaX, sigmaY)
                return DecodeResult(
                    selected = inside,
                    candidates = listOf(ScoredCandidate(inside, spatial, hypot(x - cx, y - cy))),
                    containing = inside,
                    clearCenter = true,
                )
            }
        }

        val candidates = nearby(x, y, layout, sigmaX, sigmaY)
        if (candidates.isEmpty()) {
            return DecodeResult(inside, emptyList(), inside, false)
        }

        return DecodeResult(
            selected = candidates.first().key,
            candidates = candidates.take(3),
            containing = inside,
            clearCenter = false,
        )
    }

    private fun nearby(
        x: Float,
        y: Float,
        layout: KeyboardLayout,
        sigmaX: Float,
        sigmaY: Float,
    ): List<ScoredCandidate> {
        val radius = layout.letterWidth * KeyGeometry.SEARCH_KEYS
        return layout.keys.map { key ->
            val cx = key.geometricCenterX
            val cy = key.geometricCenterY
            val distance = hypot(x - cx, y - cy)
            ScoredCandidate(key, gaussian(x, y, cx, cy, sigmaX, sigmaY), distance)
        }
            .filter { it.distance <= radius || it.key.logical.contains(x, y) }
            .sortedByDescending { it.spatial }
    }

    private fun gaussian(x: Float, y: Float, cx: Float, cy: Float, sigmaX: Float, sigmaY: Float): Float {
        val dx = (x - cx) / sigmaX.coerceAtLeast(1f)
        val dy = (y - cy) / sigmaY.coerceAtLeast(1f)
        return exp(-0.5f * (dx * dx + dy * dy))
    }
}
