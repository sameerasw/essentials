package com.sameerasw.essentials.ui.features.display

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun ProviderAppIcon(packageName: String): (@Composable () -> Unit)? {
    val context = LocalContext.current
    val icon = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
    } ?: return null
    return {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)),
        )
    }
}
