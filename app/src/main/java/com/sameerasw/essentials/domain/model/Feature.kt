package com.sameerasw.essentials.domain.model

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.viewmodels.MainViewModel

/**
 * Represents a sub-setting within a feature that can be individually searched and highlighted.
 */
data class SearchSetting(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val targetSettingHighlightKey: String,
    @androidx.annotation.ArrayRes val keywordRes: Int = 0,
    @StringRes val category: Int? = null
)

/**
 * Base class for all app features, providing metadata for the main UI and automated search indexing.
 */
abstract class Feature(
    val id: String,
    @StringRes val title: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val category: Int,
    @StringRes val description: Int,
    val permissionKeys: List<String> = emptyList(),
    val searchableSettings: List<SearchSetting> = emptyList(),
    val showToggle: Boolean = true,
    val hasMoreSettings: Boolean = true,
    val isBeta: Boolean = false,
    val parentFeatureId: String? = null,
    val isVisibleInMain: Boolean = true,
    @StringRes val authTitle: Int = 0,
    @StringRes val authSubtitle: Int = 0,
    @StringRes val aboutDescription: Int? = null
) {
    val requiresAuth: Boolean = category == com.sameerasw.essentials.R.string.cat_protection

    abstract fun isEnabled(viewModel: MainViewModel): Boolean
    
    open fun isToggleEnabled(viewModel: MainViewModel, context: Context): Boolean = true

    abstract fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean)

    open fun onClick(context: Context, viewModel: MainViewModel) {
        context.startActivity(Intent(context, FeatureSettingsActivity::class.java).apply {
            putExtra("feature", id)
        })
    }
}
