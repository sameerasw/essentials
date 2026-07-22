package com.sameerasw.essentials.translation

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.sameerasw.essentials.translation.model.TranslationEdit
import com.sameerasw.essentials.translation.model.TranslationSession

object TranslationManager {
    val isTranslationModeEnabled = mutableStateOf(false)
    val session = TranslationSession()

    // Live UI text overrides while session is active: Pair(key, locale) -> newValue
    val liveOverrides = mutableStateMapOf<Pair<String, String>, String>()

    // Currently long-pressed text target for overlay menu
    val activeTargetKey = mutableStateOf<String?>(null)
    val activeTargetText = mutableStateOf<String?>(null)

    fun addEdit(key: String, locale: String, originalValue: String, newValue: String) {
        if (newValue.trim() == originalValue.trim() || newValue.isBlank()) {
            removeEdit(key, locale)
            return
        }
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
            val trimmed = resOrText.trim()
            val all = StringLoader.getAllTranslations(context)

            // 1. Exact match in any locale
            all.entries.firstOrNull { (_, map) ->
                map.values.any { it == resOrText || it.trim() == trimmed }
            }?.let { return it.key }

            // 2. Case insensitive match
            all.entries.firstOrNull { (_, map) ->
                map.values.any { it.trim().equals(trimmed, ignoreCase = true) }
            }?.let { return it.key }

            // 3. Match prefix before format specifiers (%s, %d, %1$s, etc.)
            all.entries.firstOrNull { (_, map) ->
                map.values.any { v ->
                    val cleanV = v.split("%")[0].trim()
                    cleanV.length >= 3 && trimmed.startsWith(cleanV, ignoreCase = true)
                }
            }?.let { return it.key }
        }
        return null
    }
}
