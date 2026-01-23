package com.sameerasw.essentials.domain.watermark

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Setup DataStore extension
private val Context.dataStore by preferencesDataStore(name = "watermark_prefs")

class WatermarkRepository(
    private val context: Context
) {
    private val PREF_STYLE = stringPreferencesKey("watermark_style")
    private val PREF_SHOW_BRAND = booleanPreferencesKey("show_brand")
    private val PREF_SHOW_EXIF = booleanPreferencesKey("show_exif")
    private val PREF_SHOW_FOCAL_LENGTH = booleanPreferencesKey("show_focal_length")
    private val PREF_SHOW_APERTURE = booleanPreferencesKey("show_aperture")
    private val PREF_SHOW_ISO = booleanPreferencesKey("show_iso")
    private val PREF_SHOW_SHUTTER = booleanPreferencesKey("show_shutter")
    private val PREF_SHOW_DATE = booleanPreferencesKey("show_date")
    private val PREF_USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
    private val PREF_MOVE_TO_TOP = booleanPreferencesKey("move_to_top")
    private val PREF_LEFT_ALIGN = booleanPreferencesKey("left_align")
    private val PREF_BRAND_TEXT_SIZE = androidx.datastore.preferences.core.intPreferencesKey("brand_text_size")
    private val PREF_DATA_TEXT_SIZE = androidx.datastore.preferences.core.intPreferencesKey("data_text_size")
    private val PREF_SHOW_CUSTOM_TEXT = booleanPreferencesKey("show_custom_text")
    private val PREF_CUSTOM_TEXT = stringPreferencesKey("custom_text")
    private val PREF_CUSTOM_TEXT_SIZE = androidx.datastore.preferences.core.intPreferencesKey("custom_text_size")
    private val PREF_PADDING = androidx.datastore.preferences.core.intPreferencesKey("padding")
    private val PREF_BORDER_STROKE = androidx.datastore.preferences.core.intPreferencesKey("border_stroke")
    private val PREF_BORDER_CORNER = androidx.datastore.preferences.core.intPreferencesKey("border_corner")

    val watermarkOptions: Flow<WatermarkOptions> = context.dataStore.data
        .map { preferences ->
            val styleStr = preferences[PREF_STYLE] ?: WatermarkStyle.FRAME.name
            val style = try {
                WatermarkStyle.valueOf(styleStr)
            } catch (e: Exception) {
                WatermarkStyle.FRAME
            }

            WatermarkOptions(
                style = style,
                showDeviceBrand = preferences[PREF_SHOW_BRAND] ?: true,
                showExif = preferences[PREF_SHOW_EXIF] ?: true,
                showFocalLength = preferences[PREF_SHOW_FOCAL_LENGTH] ?: true,
                showAperture = preferences[PREF_SHOW_APERTURE] ?: true,
                showIso = preferences[PREF_SHOW_ISO] ?: true,
                showShutterSpeed = preferences[PREF_SHOW_SHUTTER] ?: true,
                showDate = preferences[PREF_SHOW_DATE] ?: false,
                useDarkTheme = preferences[PREF_USE_DARK_THEME] ?: false,
                moveToTop = preferences[PREF_MOVE_TO_TOP] ?: false,
                leftAlignOverlay = preferences[PREF_LEFT_ALIGN] ?: false,
                brandTextSize = preferences[PREF_BRAND_TEXT_SIZE] ?: 50,
                dataTextSize = preferences[PREF_DATA_TEXT_SIZE] ?: 50,
                showCustomText = preferences[PREF_SHOW_CUSTOM_TEXT] ?: false,
                customText = preferences[PREF_CUSTOM_TEXT] ?: "",
                customTextSize = preferences[PREF_CUSTOM_TEXT_SIZE] ?: 50,
                padding = preferences[PREF_PADDING] ?: 50,
                borderStroke = preferences[PREF_BORDER_STROKE] ?: 0,
                borderCorner = preferences[PREF_BORDER_CORNER] ?: 0
            )
        }

    suspend fun updateStyle(style: WatermarkStyle) {
        context.dataStore.edit { it[PREF_STYLE] = style.name }
    }

    suspend fun updateShowBrand(show: Boolean) {
        context.dataStore.edit { it[PREF_SHOW_BRAND] = show }
    }

    suspend fun updateShowExif(show: Boolean) {
        context.dataStore.edit { it[PREF_SHOW_EXIF] = show }
    }
    
    suspend fun updateExifSettings(
        focalLength: Boolean,
        aperture: Boolean,
        iso: Boolean,
        shutterSpeed: Boolean,
        date: Boolean
    ) {
        context.dataStore.edit { 
            it[PREF_SHOW_FOCAL_LENGTH] = focalLength
            it[PREF_SHOW_APERTURE] = aperture
            it[PREF_SHOW_ISO] = iso
            it[PREF_SHOW_SHUTTER] = shutterSpeed
            it[PREF_SHOW_DATE] = date
        }
    }

    suspend fun updateUseDarkTheme(useDark: Boolean) {
        context.dataStore.edit { it[PREF_USE_DARK_THEME] = useDark }
    }
    
    suspend fun updateMoveToTop(move: Boolean) {
        context.dataStore.edit { it[PREF_MOVE_TO_TOP] = move }
    }

    suspend fun updateLeftAlign(left: Boolean) {
        context.dataStore.edit { it[PREF_LEFT_ALIGN] = left }
    }

    suspend fun updateBrandTextSize(size: Int) {
        context.dataStore.edit { it[PREF_BRAND_TEXT_SIZE] = size }
    }

    suspend fun updateDataTextSize(size: Int) {
        context.dataStore.edit { it[PREF_DATA_TEXT_SIZE] = size }
    }

    suspend fun updateCustomTextSettings(show: Boolean, text: String, size: Int) {
        context.dataStore.edit { 
            it[PREF_SHOW_CUSTOM_TEXT] = show
            it[PREF_CUSTOM_TEXT] = text
            it[PREF_CUSTOM_TEXT_SIZE] = size
        }
    }

    suspend fun updatePadding(padding: Int) {
        context.dataStore.edit { it[PREF_PADDING] = padding }
    }

    suspend fun updateCustomTextSize(size: Int) {
        context.dataStore.edit { it[PREF_CUSTOM_TEXT_SIZE] = size }
    }

    suspend fun updateBorderStroke(stroke: Int) {
        context.dataStore.edit { it[PREF_BORDER_STROKE] = stroke }
    }

    suspend fun updateBorderCorner(corner: Int) {
        context.dataStore.edit { it[PREF_BORDER_CORNER] = corner }
    }
}
