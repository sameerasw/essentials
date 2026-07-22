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

            session.edits.firstOrNull { it.newValue.trim() == trimmed || it.originalValue.trim() == trimmed }?.let {
                return it.key
            }
            liveOverrides.entries.firstOrNull { it.value.trim() == trimmed }?.let {
                return it.key.first
            }

            val all = StringLoader.getAllTranslations(context)

            all.entries.firstOrNull { (_, map) ->
                map.values.any { it == resOrText || it.trim() == trimmed }
            }?.let { return it.key }

            all.entries.firstOrNull { (_, map) ->
                map.values.any { it.trim().equals(trimmed, ignoreCase = true) }
            }?.let { return it.key }

            all.entries.firstOrNull { (_, map) ->
                map.values.any { v ->
                    if (!v.contains("%")) return@any false
                    val parts = v.split(Regex("%[0-9]*\\$?[a-zA-Z]"))
                    if (parts.all { it.isEmpty() }) return@any false
                    val regexPattern = "^" + parts.joinToString(".*?") { Regex.escape(it) } + "$"
                    try {
                        Regex(regexPattern, RegexOption.IGNORE_CASE).matches(trimmed)
                    } catch (e: Exception) {
                        false
                    }
                }
            }?.let { return it.key }

            all.entries.firstOrNull { (_, map) ->
                map.values.any { v ->
                    val cleanV = v.replace(Regex("%[0-9]*\\$?[a-zA-Z]"), "").trim()
                    cleanV.length >= 3 && (trimmed.contains(cleanV, ignoreCase = true) || cleanV.contains(trimmed, ignoreCase = true))
                }
            }?.let { return it.key }
        }
        return null
    }
}
