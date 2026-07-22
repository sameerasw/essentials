package com.sameerasw.essentials.translation

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.sameerasw.essentials.translation.model.TranslationEdit
import com.sameerasw.essentials.translation.model.TranslationSession

object TranslationManager {
    val isTranslationModeEnabled = mutableStateOf(false)
    val session = TranslationSession()

    // Up to 5 target languages chosen by user
    val selectedLanguages = mutableStateListOf<String>()

    // Live UI text overrides while session is active: Pair(key, locale) -> newValue
    val liveOverrides = mutableStateMapOf<Pair<String, String>, String>()

    // Currently long-pressed text target for overlay menu
    val activeTargetKey = mutableStateOf<String?>(null)
    val activeTargetText = mutableStateOf<String?>(null)

    fun initializeLanguages(currentLocale: String) {
        if (selectedLanguages.isEmpty()) {
            val defaultList = mutableListOf("en")
            if (currentLocale != "en" && currentLocale.isNotBlank()) {
                defaultList.add(0, currentLocale)
            }
            selectedLanguages.addAll(defaultList.distinct().take(5))
        }
    }

    fun setSelectedLanguages(languages: List<String>) {
        selectedLanguages.clear()
        selectedLanguages.addAll(languages.take(5))
    }

    fun addEdit(key: String, locale: String, originalValue: String, newValue: String) {
        val edit = TranslationEdit(key, locale, originalValue, newValue)
        session.addOrUpdate(edit)
        liveOverrides[Pair(key, locale)] = newValue
    }

    fun removeEdit(key: String, locale: String) {
        session.remove(key, locale)
        liveOverrides.remove(Pair(key, locale))
    }

    fun discardSession() {
        session.clear()
        liveOverrides.clear()
    }

    fun getOverriddenText(key: String, locale: String, fallback: String): String {
        return liveOverrides[Pair(key, locale)] ?: fallback
    }

    fun resolveKey(context: Context, resOrText: Any?): String? {
        if (resOrText == null) return null
        if (resOrText is Int && resOrText != 0) {
            return try {
                context.resources.getResourceEntryName(resOrText)
            } catch (e: Exception) {
                null
            }
        }
        if (resOrText is String && resOrText.isNotBlank()) {
            val all = StringLoader.getAllTranslations(context)
            return all.entries.firstOrNull { (_, map) ->
                map["en"] == resOrText || map.values.any { it == resOrText }
            }?.key
        }
        return null
    }
}

