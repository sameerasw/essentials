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
    private val PREF_USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")

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
                useDarkTheme = preferences[PREF_USE_DARK_THEME] ?: false
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

    suspend fun updateUseDarkTheme(useDark: Boolean) {
        context.dataStore.edit { it[PREF_USE_DARK_THEME] = useDark }
    }
}
