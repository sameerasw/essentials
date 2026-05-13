package com.sameerasw.essentials.utils

import android.graphics.Path
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import java.util.Random

object AmbientMusicShapeHelper {

    private val allShapes = listOf(
        MaterialShapes.Circle,
        MaterialShapes.Square,
        MaterialShapes.Slanted,
        MaterialShapes.Arch,
        MaterialShapes.Oval,
        MaterialShapes.Pill,
        MaterialShapes.Triangle,
        MaterialShapes.Arrow,
        MaterialShapes.Diamond,
        MaterialShapes.ClamShell,
        MaterialShapes.Pentagon,
        MaterialShapes.Gem,
        MaterialShapes.Sunny,
        MaterialShapes.VerySunny,
        MaterialShapes.Cookie4Sided,
        MaterialShapes.Cookie6Sided,
        MaterialShapes.Cookie7Sided,
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Cookie12Sided,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.Clover8Leaf,
        MaterialShapes.SoftBurst,
        MaterialShapes.Flower,
        MaterialShapes.PuffyDiamond,
        MaterialShapes.Ghostish,
        MaterialShapes.PixelCircle,
        MaterialShapes.Bun,
        MaterialShapes.Heart
    )

    fun getShapePath(seed: String?, size: Float, isRandomEnabled: Boolean = true): Path {
        return getPolygon(seed, isRandomEnabled).toAndroidPath(size)
    }

    fun getRandomShapePath(size: Float, isRandomEnabled: Boolean = true): Path {
        return getRandomPolygon(isRandomEnabled).toAndroidPath(size)
    }

    fun getPolygon(seed: String?, isRandomEnabled: Boolean = true): RoundedPolygon {
        if (!isRandomEnabled) return MaterialShapes.Cookie12Sided
        val hash = seed?.hashCode() ?: 0
        val random = Random(hash.toLong())
        return allShapes[random.nextInt(allShapes.size)]
    }

    fun getRandomPolygon(isRandomEnabled: Boolean = true): RoundedPolygon {
        if (!isRandomEnabled) return MaterialShapes.Cookie12Sided
        val random = Random()
        return allShapes[random.nextInt(allShapes.size)]
    }

    fun updatePathFromMorph(
        morph: androidx.graphics.shapes.Morph,
        progress: Float,
        size: Float,
        targetPath: Path,
        rotation: Float = 0f
    ) {
        val rawPath = morph.toPath(progress)
        val matrix = android.graphics.Matrix()
        matrix.postScale(size, size)
        if (rotation != 0f) {
            matrix.postRotate(rotation, size / 2f, size / 2f)
        }

        targetPath.reset()
        targetPath.set(rawPath)
        targetPath.transform(matrix)
    }

    private fun RoundedPolygon.toAndroidPath(size: Float): Path {
        val resultPath = this.toPath()
        val matrixObj = android.graphics.Matrix()
        matrixObj.postScale(size, size)
        resultPath.transform(matrixObj)
        return resultPath
    }
}
