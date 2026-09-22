package com.sameerasw.essentials.island.plugins

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.palette.graphics.Palette

fun soften(color: Int): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)
    hsv[1] = (hsv[1] * 0.75f).coerceIn(0.25f, 0.90f)
    hsv[2] = (hsv[2] * 1.25f).coerceIn(0.85f, 1.0f)
    return Color.HSVToColor(hsv)
}

fun accentFrom(bitmap: Bitmap?): Int? {
    bitmap ?: return null
    return try {
        val palette = Palette.from(bitmap).generate()
        val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
        swatch?.rgb?.let(::soften)
    } catch (_: Exception) {
        null
    }
}

fun sendPendingIntent(context: Context, intent: PendingIntent?): Boolean {
    intent ?: return false
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic().apply {
                pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
            intent.send(context, 0, null, null, null, null, options.toBundle())
        } else {
            intent.send()
        }
        true
    } catch (_: Exception) {
        false
    }
}

fun launchPackage(context: Context, packageName: String) {
    try {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    } catch (_: Exception) {
    }
}
