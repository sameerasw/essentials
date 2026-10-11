package com.sameerasw.island.sdk.compose

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.delay

@Composable
fun Modifier.islandSnapshot(intervalMs: Long = 500L, onSnapshot: (Bitmap) -> Unit): Modifier {
    val layer = rememberGraphicsLayer()
    val latest = rememberUpdatedState(onSnapshot)
    LaunchedEffect(layer, intervalMs) {
        delay(200L)
        while (true) {
            try {
                val bitmap = layer.toImageBitmap().asAndroidBitmap()
                latest.value(bitmap.copy(Bitmap.Config.ARGB_8888, false))
            } catch (_: Exception) {
            }
            delay(intervalMs)
        }
    }
    return drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }
}
