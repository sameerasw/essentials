package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.sin

private const val LOGO_PATH = "M100.61,28.13C106.72,28.13 111.96,32.33 113.11,38.15L113.62,40.73C115.63,50.89 126.47,56.96 136.56,53.57L139.12,52.7C144.89,50.76 151.27,53.06 154.32,58.19L158.94,65.93C161.99,71.06 160.86,77.57 156.24,81.44L154.18,83.16C146.11,89.93 146.11,102.07 154.18,108.84L156.24,110.56C160.86,114.43 161.99,120.94 158.94,126.07L154.32,133.81C151.27,138.94 144.89,141.24 139.12,139.3L136.56,138.43C126.47,135.04 115.63,141.1 113.62,151.27L113.11,153.85C111.96,159.67 106.72,163.87 100.61,163.87H91.39C85.28,163.87 80.04,159.67 78.89,153.85L78.38,151.27C76.37,141.1 65.52,135.04 55.44,138.43L52.88,139.29C47.11,141.24 40.73,138.94 37.68,133.81L33.06,126.07C30.01,120.94 31.14,114.43 35.76,110.56L37.82,108.84C45.89,102.07 45.89,89.93 37.82,83.16L35.76,81.44C31.14,77.57 30.01,71.06 33.06,65.93L37.68,58.19C40.73,53.06 47.11,50.76 52.88,52.7L55.44,53.57C65.52,56.96 76.37,50.89 78.38,40.73L78.89,38.14C80.04,32.33 85.28,28.13 91.39,28.13H100.61ZM105.32,58.71C99.56,57.26 93.49,57.17 87.62,58.48C78.99,60.41 71.29,65.25 65.82,72.19C60.35,79.13 57.43,87.75 57.57,96.59C57.71,105.42 60.89,113.94 66.57,120.71C72.26,127.48 80.1,132.08 88.78,133.74C97.46,135.4 106.45,134.01 114.23,129.82C122.01,125.63 128.11,118.88 131.5,110.72C133.8,105.15 134.76,99.17 134.33,93.24C134,88.62 129.23,86.07 124.79,87.4C120.35,88.72 118,93.49 117.55,98.1C117.34,100.21 116.82,102.3 116,104.29C114.09,108.89 110.66,112.69 106.27,115.05C101.89,117.41 96.82,118.19 91.93,117.26C90.22,116.93 88.55,116.4 86.98,115.68C84.95,114.75 85.37,112.03 87.49,111.31L92.09,109.75C92.34,109.71 92.59,109.64 92.83,109.56L96.06,108.5L99.28,107.39C101.21,106.72 102.65,105.09 103.07,103.09L103.77,99.76L104.42,96.42C104.81,94.41 104.11,92.35 102.59,90.98L100.06,88.71L97.49,86.48C95.94,85.14 93.81,84.71 91.86,85.35L89.03,86.28C88.53,86.37 88.03,86.51 87.53,86.67L79.78,89.3C77.68,90.01 75.7,88.13 76.71,86.15C77.35,84.9 78.11,83.7 79,82.58C82.08,78.67 86.42,75.94 91.28,74.86C93.38,74.39 95.52,74.24 97.64,74.4C102.26,74.75 107.36,73.26 109.43,69.11C111.5,64.97 109.82,59.83 105.32,58.71Z"

private const val FLASK_OUTLINE_PATH =
    "M200,840Q149,840 127.5,794.5Q106,749 138,710L360,440L360,200L320,200Q303,200 291.5,188.5Q280,177 280,160Q280,143 291.5,131.5Q303,120 320,120L640,120Q657,120 668.5,131.5Q680,143 680,160Q680,177 668.5,188.5Q657,200 640,200L600,200L600,440L822,710Q854,749 832.5,794.5Q811,840 760,840L200,840ZM200,760L760,760L520,468L520,200L440,200L440,468L200,760Z"

private const val FLASK_LIQUID_PATH = "M280,720L680,720L544,560L416,560L280,720Z"

private val LOGO_LAYER_ANGLES = floatArrayOf(30f, 24f, 18f, 12f, 6f)
private val LOGO_LAYER_ALPHAS = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f)

@Composable
fun LayeredLogoIcon(
    tint: Color,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val path = remember { PathParser().parsePathString(LOGO_PATH).toPath() }
    Canvas(modifier = modifier) {
        val s = size.minDimension / 140f
        translate(left = size.width / 2f - 96f * s, top = size.height / 2f - 96f * s) {
            scale(s, pivot = Offset.Zero) {
                LOGO_LAYER_ANGLES.forEachIndexed { i, angle ->
                    val delay = (LOGO_LAYER_ANGLES.size - 1 - i) * 0.08f
                    val local = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
                    val eased = 1f - (1f - local) * (1f - local) * (1f - local)
                    val overshoot = sin(PI * local).toFloat() * 0.25f
                    rotate(degrees = angle * (eased + overshoot), pivot = Offset(96f, 96f)) {
                        drawPath(path, tint.copy(alpha = tint.alpha * LOGO_LAYER_ALPHAS[i]))
                    }
                }
                drawPath(path, tint)
            }
        }
    }
}

@Composable
fun FillingFlaskIcon(
    tint: Color,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val outline = remember { PathParser().parsePathString(FLASK_OUTLINE_PATH).toPath() }
    val liquid = remember { PathParser().parsePathString(FLASK_LIQUID_PATH).toPath() }
    val surface = remember { Path() }
    Canvas(modifier = modifier) {
        val s = size.minDimension / 960f
        scale(s, pivot = Offset.Zero) {
            drawPath(outline, tint)
            if (progress >= 1f) {
                drawPath(liquid, tint)
            } else {
                val eased = 1f - (1f - progress) * (1f - progress)
                val level = 740f - 200f * eased
                val amplitude = 28f * (1f - progress)
                val phase = progress * PI.toFloat() * 4f
                surface.reset()
                surface.moveTo(200f, 760f)
                var x = 200f
                while (x <= 760f) {
                    val y = level + amplitude * sin(phase + (x - 200f) / 560f * PI.toFloat() * 2f)
                    surface.lineTo(x, y)
                    x += 20f
                }
                surface.lineTo(760f, 760f)
                surface.close()
                clipPath(surface) { drawPath(liquid, tint) }
            }
        }
    }
}
