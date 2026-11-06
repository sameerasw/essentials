package com.sameerasw.essentials.island.remote

import com.sameerasw.essentials.island.ui.components.islandAccent

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.island.sdk.IslandTone

@Composable
fun RemoteIcon(bitmap: Bitmap?, tone: String, size: Dp = 18.dp) {
    if (bitmap == null) return
    val color = when (tone) {
        IslandTone.ACCENT -> islandAccent()
        IslandTone.ALERT -> MaterialTheme.colorScheme.error
        else -> null
    }
    Image(
        bitmap = remember(bitmap) { bitmap.asImageBitmap() },
        contentDescription = null,
        colorFilter = color?.let { ColorFilter.tint(it) },
        modifier = Modifier.size(size),
    )
}
