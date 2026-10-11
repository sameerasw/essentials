package com.sameerasw.island.sdk

import android.os.Bundle
import android.os.IBinder

data class SurfaceRequest(
    val hostToken: IBinder,
    val displayId: Int,
    val widthPx: Int,
    val heightPx: Int,
) {
    fun toBundle(): Bundle = Bundle().apply {
        putBinder(KEY_TOKEN, hostToken)
        putInt(KEY_DISPLAY, displayId)
        putInt(KEY_WIDTH, widthPx)
        putInt(KEY_HEIGHT, heightPx)
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_DISPLAY = "display"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        const val RESULT_PACKAGE = "package"

        fun fromBundle(bundle: Bundle): SurfaceRequest? {
            val token = bundle.getBinder(KEY_TOKEN) ?: return null
            return SurfaceRequest(
                hostToken = token,
                displayId = bundle.getInt(KEY_DISPLAY),
                widthPx = bundle.getInt(KEY_WIDTH),
                heightPx = bundle.getInt(KEY_HEIGHT),
            )
        }
    }
}
