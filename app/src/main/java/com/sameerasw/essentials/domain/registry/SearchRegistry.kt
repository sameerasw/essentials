package com.sameerasw.essentials.domain.registry

import android.content.Context
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.StatusBarIconRegistry
import com.sameerasw.essentials.domain.model.SearchableItem

object SearchRegistry {

    fun search(context: Context, query: String): List<SearchableItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        val allItems = mutableListOf<SearchableItem>()

        // --- Index Features and Sub-settings ---
        FeatureRegistry.ALL_FEATURES.forEach { feature ->
            val featureTitle = context.getString(feature.title)
            val featureCategory = context.getString(feature.category)
            
            // Index the feature itself
            allItems.add(
                SearchableItem(
                    title = featureTitle,
                    description = context.getString(feature.description),
                    category = featureCategory,
                    icon = feature.iconRes,
                    featureKey = feature.id,
                    keywords = listOf(context.getString(R.string.keyword_feature), context.getString(R.string.keyword_settings)),
                    isBeta = feature.isBeta
                )
            )

            // Index sub-settings
            feature.searchableSettings.forEach { setting ->
                allItems.add(
                    SearchableItem(
                        title = context.getString(setting.title),
                        description = context.getString(setting.description),
                        category = setting.category?.let { context.getString(it) } ?: featureCategory,
                        icon = feature.iconRes,
                        featureKey = feature.id,
                        parentFeature = featureTitle,
                        targetSettingHighlightKey = setting.targetSettingHighlightKey,
                        keywords = if (setting.keywordRes != 0) context.resources.getStringArray(setting.keywordRes).toList() else emptyList(),
                        isBeta = feature.isBeta
                    )
                )
            }
        }

        // --- Dynamic Status Bar Icons ---
        StatusBarIconRegistry.ALL_ICONS.forEach { icon ->
            val title = context.getString(icon.displayNameRes)
            allItems.add(
                SearchableItem(
                    title = title,
                    description = context.getString(R.string.search_desc_toggle_visibility, title),
                    category = context.getString(R.string.feat_statusbar_icons_title),
                    icon = icon.iconRes,
                    featureKey = "Statusbar icons",
                    parentFeature = context.getString(R.string.feat_statusbar_icons_title),
                    targetSettingHighlightKey = title,
                    keywords = icon.blacklistNames + listOf(
                        context.getString(R.string.keyword_hide),
                        context.getString(R.string.keyword_show),
                        context.getString(R.string.keyword_visibility)
                    )
                )
            )
        }

        return allItems.filter { item ->
            item.title.lowercase().contains(q) ||
            item.description.lowercase().contains(q) ||
            item.category.lowercase().contains(q) ||
            item.keywords.any { it.lowercase().contains(q) }
        }.sortedByDescending { it.title.lowercase().startsWith(q) }
    }
}