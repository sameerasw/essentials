/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: SquircleShape.kt
 * Description: Superellipse squircle shape used for One UI style icons.
 */

package com.sameerasw.essentials.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

class SquircleShape(private val exponent: Float = 4f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val rx = size.width / 2f
        val ry = size.height / 2f
        val power = 2f / exponent
        val steps = 96
        val path = Path()
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 2f * Math.PI.toFloat()
            val c = cos(t)
            val s = sin(t)
            val x = rx + rx * sign(c) * abs(c).pow(power)
            val y = ry + ry * sign(s) * abs(s).pow(power)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

val OneUiSquircle = SquircleShape(exponent = 3f)
