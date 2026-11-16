package com.sameerasw.essentials.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.View

object ForceInvertUtils {
    const val KEY = "accessibility_force_invert_color_enabled"

    fun optOut(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) view.isForceDarkAllowed = false
        runCatching { View::class.java.getMethod("setForceInvertColor", java.lang.Boolean.TYPE).invoke(view, false) }
    }

    fun isEnabled(context: Context): Boolean =
        runCatching { Settings.Secure.getInt(context.contentResolver, KEY, 0) == 1 }.getOrDefault(false)
}
