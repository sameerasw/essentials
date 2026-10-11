package com.sameerasw.island.sdk

import android.os.Bundle

data class HostInfo(
    val hostPackage: String,
    val hostSchema: Int = IslandSchema.VERSION,
    val accent: Int = 0xFFFFFFFF.toInt(),
    val darkTheme: Boolean = true,
    val density: Float = 1f,
    val fontScale: Float = 1f,
    val expandedWidthPx: Int = 0,
    val expandedPaddingPx: Int = 0,
    val cameraClearancePx: Int = 0,
    val outsetPx: Int = 0,
    val compactHeightPx: Int = 0,
    val cameraSlotWidthPx: Int = 0,
    val cornerPx: Int = 0,
    val powerSave: Boolean = false,
) {
    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_PACKAGE, hostPackage)
        putInt(KEY_SCHEMA, hostSchema)
        putInt(KEY_ACCENT, accent)
        putBoolean(KEY_DARK, darkTheme)
        putFloat(KEY_DENSITY, density)
        putFloat(KEY_FONT_SCALE, fontScale)
        putInt(KEY_WIDTH, expandedWidthPx)
        putInt(KEY_PADDING, expandedPaddingPx)
        putInt(KEY_CAMERA, cameraClearancePx)
        putInt(KEY_OUTSET, outsetPx)
        putInt(KEY_COMPACT_HEIGHT, compactHeightPx)
        putInt(KEY_CAMERA_SLOT, cameraSlotWidthPx)
        putInt(KEY_CORNER, cornerPx)
        putBoolean(KEY_POWER_SAVE, powerSave)
    }

    companion object {
        private const val KEY_PACKAGE = "package"
        private const val KEY_SCHEMA = "schema"
        private const val KEY_ACCENT = "accent"
        private const val KEY_DARK = "dark"
        private const val KEY_DENSITY = "density"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_WIDTH = "width"
        private const val KEY_PADDING = "padding"
        private const val KEY_CAMERA = "camera"
        private const val KEY_OUTSET = "outset"
        private const val KEY_COMPACT_HEIGHT = "compact_height"
        private const val KEY_CAMERA_SLOT = "camera_slot"
        private const val KEY_CORNER = "corner"
        private const val KEY_POWER_SAVE = "power_save"

        fun fromBundle(bundle: Bundle): HostInfo = HostInfo(
            hostPackage = bundle.getString(KEY_PACKAGE).orEmpty(),
            hostSchema = bundle.getInt(KEY_SCHEMA, IslandSchema.VERSION),
            accent = bundle.getInt(KEY_ACCENT, 0xFFFFFFFF.toInt()),
            darkTheme = bundle.getBoolean(KEY_DARK, true),
            density = bundle.getFloat(KEY_DENSITY, 1f),
            fontScale = bundle.getFloat(KEY_FONT_SCALE, 1f),
            expandedWidthPx = bundle.getInt(KEY_WIDTH, 0),
            expandedPaddingPx = bundle.getInt(KEY_PADDING, 0),
            cameraClearancePx = bundle.getInt(KEY_CAMERA, 0),
            outsetPx = bundle.getInt(KEY_OUTSET, 0),
            compactHeightPx = bundle.getInt(KEY_COMPACT_HEIGHT, 0),
            cameraSlotWidthPx = bundle.getInt(KEY_CAMERA_SLOT, 0),
            cornerPx = bundle.getInt(KEY_CORNER, 0),
            powerSave = bundle.getBoolean(KEY_POWER_SAVE, false),
        )
    }
}
