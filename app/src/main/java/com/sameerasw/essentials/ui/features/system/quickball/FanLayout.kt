/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Radial Menus
 * File: FanLayout.kt
 * Description: Geometry computations for the radial menu shortcuts.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class FanLayout(
    val offsets: List<Pair<Float, Float>>,
    val arcRadius: Float,
    val arcEdgeAngle: Float,
) {
    companion object {
        const val SINGLE_RING_MAX = 5
        const val MAX_SWEEP_ANGLE = 160.0
        const val ITEM_GAP_DP = 6f
        const val ARC_GAP_DP = 8f
        const val ARC_MIN_INSET_DP = 4f

        fun angleRange(offsets: List<Pair<Float, Float>>): Pair<Double, Double> {
            if (offsets.isEmpty()) return Pair(0.0, 0.0)
            val middle = middleAngle(offsets)
            val deltas = angleDeltas(offsets)
            return Pair(middle + deltas.min(), deltas.max() - deltas.min())
        }

        fun curveBends(offsets: List<Pair<Float, Float>>): List<Float> {
            val deltas = angleDeltas(offsets)
            val largest = deltas.maxOfOrNull { abs(it) } ?: 0.0
            return deltas.map { if (largest > 0.0) (it / largest).toFloat() else 0f }
        }

        fun angleDeltas(offsets: List<Pair<Float, Float>>): List<Double> {
            val middle = middleAngle(offsets)
            return offsets.map { (offsetX, offsetY) ->
                var delta = atan2(offsetY.toDouble(), offsetX.toDouble()) - middle
                while (delta > PI) delta -= 2 * PI
                while (delta < -PI) delta += 2 * PI
                delta
            }
        }

        fun middleAngle(offsets: List<Pair<Float, Float>>): Double {
            return atan2(
                offsets.sumOf { it.second.toDouble() },
                offsets.sumOf { it.first.toDouble() },
            )
        }

        fun compute(
            count: Int,
            buttonSize: Float,
            radius: Float,
            halfFab: Float,
            density: Float,
            maxHalfSpan: Float = Float.MAX_VALUE,
        ): FanLayout {
            val isTwoRings = count > SINGLE_RING_MAX
            val itemGap = ITEM_GAP_DP * density
            val minChord = buttonSize + itemGap
            val ringStride = if (isTwoRings) 2 else 1
            val ringSpacing = if (isTwoRings) buttonSize else 0f

            var innerRadius = max(radius, halfFab + buttonSize / 2f + itemGap)

            val preferredStep = Math.toRadians(180.0 / (count + 1))
            val maxStep = Math.toRadians(MAX_SWEEP_ANGLE / (count - 1).coerceAtLeast(1))
            val fitStep = 2 * asin(min(1.0, minChord / (2.0 * innerRadius))) / ringStride
            val stepRad = max(preferredStep, fitStep).coerceAtMost(maxStep)
            val edgeAngleRad = (count - 1) / 2.0 * stepRad

            val minRadius = (minChord / (2 * sin(stepRad * ringStride / 2))).toFloat()
            val maxRadius = (maxHalfSpan / sin(edgeAngleRad)).toFloat() - ringSpacing

            innerRadius = max(minRadius, min(innerRadius, maxRadius))
            val outerRadius = innerRadius + ringSpacing

            val offsets =
                List(count) { index ->
                    val ringRadius = if (isTwoRings && index % 2 == 0) outerRadius else innerRadius
                    val angleRad = edgeAngleRad - index * stepRad

                    Pair(
                        (-cos(angleRad) * ringRadius).toFloat(),
                        (-sin(angleRad) * ringRadius).toFloat(),
                    )
                }

            val arcRadius =
                max(
                    innerRadius - buttonSize / 2f - ARC_GAP_DP * density,
                    halfFab + ARC_MIN_INSET_DP * density,
                )

            return FanLayout(offsets, arcRadius, Math.toDegrees(edgeAngleRad).toFloat())
        }
    }
}
