package com.sameerasw.essentials

import com.sameerasw.essentials.domain.model.SearchableItem
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.domain.StatusBarIconRegistry

object SearchRegistry {

    private val items = mutableListOf<SearchableItem>()

    init {
        // --- Automate Feature and Sub-setting Indexing ---
        FeatureRegistry.ALL_FEATURES.forEach { feature ->
            // Index the feature itself
            addItem(SearchableItem(
                title = feature.title,
                description = feature.description,
                category = feature.category,
                icon = feature.iconRes,
                featureKey = feature.id,
                keywords = listOf("feature", "settings")
            ))

            // Index sub-settings
            feature.searchableSettings.forEach { setting ->
                addItem(SearchableItem(
                    title = setting.title,
                    description = setting.description,
                    category = setting.category ?: feature.category,
                    icon = feature.iconRes,
                    featureKey = feature.id,
                    parentFeature = feature.title,
                    targetSettingHighlightKey = setting.targetSettingHighlightKey,
                    keywords = setting.keywords
                ))
            }
        }

        // --- Dynamic Status Bar Icons ---
        StatusBarIconRegistry.ALL_ICONS.forEach { icon ->
            addItem(SearchableItem(
                title = icon.displayName,
                description = "Toggle visibility for ${icon.displayName}",
                category = "Statusbar icons",
                icon = icon.iconRes,
                featureKey = "Statusbar icons",
                parentFeature = "Statusbar icons",
                targetSettingHighlightKey = icon.displayName,
                keywords = icon.blacklistNames + listOf("hide", "show", "visibility")
            ))
        }

    }

    private fun addItem(item: SearchableItem) {
        items.add(item)
    }

    fun search(query: String): List<SearchableItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        return items.filter { item ->
            item.title.lowercase().contains(q) ||
            item.description.lowercase().contains(q) ||
            item.category.lowercase().contains(q) ||
            item.keywords.any { it.lowercase().contains(q) }
        }.sortedByDescending { it.title.lowercase().startsWith(q) }
    }
}
