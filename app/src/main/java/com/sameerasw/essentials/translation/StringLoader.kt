package com.sameerasw.essentials.translation

import android.content.Context
import com.sameerasw.essentials.translation.model.StringEntry
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

object StringLoader {
    // Map of key -> Map<locale, value>
    private var cachedTranslations: Map<String, Map<String, String>>? = null

    fun getTranslationsForKey(context: Context, key: String): Map<String, String> {
        val all = getAllTranslations(context)
        val map = all[key] ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        result.putAll(map)
        for ((loc, valStr) in map) {
            if (loc.contains("-")) {
                val base = loc.split("-")[0]
                if (!result.containsKey(base)) {
                    result[base] = valStr
                }
            }
        }
        return result
    }

    @Synchronized
    fun getAllTranslations(context: Context): Map<String, Map<String, String>> {
        cachedTranslations?.let { return it }

        val resultMap = mutableMapOf<String, MutableMap<String, String>>()
        val assetManager = context.assets

        // Standard bundled values directory list
        val res = context.resources
        val availableLocales = getAvailableLocaleDirs(context)

        for (localeDir in availableLocales) {
            val localeCode = extractLocaleCode(localeDir)
            val entries = loadStringsForLocale(context, localeDir)
            for ((key, value) in entries) {
                val localeMap = resultMap.getOrPut(key) { mutableMapOf() }
                localeMap[localeCode] = value
            }
        }

        cachedTranslations = resultMap
        return resultMap
    }

    private fun extractLocaleCode(dirName: String): String {
        if (dirName == "values") return "en"
        val code = dirName.removePrefix("values-")
        return when {
            code.contains("-r") -> code.replace("-r", "-")
            else -> code
        }
    }

    private fun getAvailableLocaleDirs(context: Context): List<String> {
        val list = mutableListOf("values")
        try {
            val resDir = context.resources
            val assets = context.assets
            // Common bundled locale subfolders in res/values-*
            val knownLocales = listOf(
                "values-ach", "values-ach-rUG",
                "values-af", "values-af-rZA",
                "values-ar", "values-ar-rSA",
                "values-bn-rBD",
                "values-ca", "values-ca-rES",
                "values-cs", "values-cs-rCZ",
                "values-da", "values-da-rDK",
                "values-de", "values-de-rDE",
                "values-el", "values-el-rGR",
                "values-en", "values-en-rUS",
                "values-es", "values-es-rES",
                "values-fi", "values-fi-rFI",
                "values-fil-rPH",
                "values-fr", "values-fr-rFR",
                "values-he",
                "values-hi-rIN",
                "values-hu", "values-hu-rHU",
                "values-id", "values-in-rID",
                "values-it", "values-it-rIT",
                "values-iw-rIL",
                "values-ja", "values-ja-rJP",
                "values-ko", "values-ko-rKR",
                "values-ml-rIN",
                "values-ne-rNP",
                "values-nl", "values-nl-rNL",
                "values-no", "values-no-rNO",
                "values-pl", "values-pl-rPL",
                "values-pt", "values-pt-rBR", "values-pt-rPT",
                "values-ro", "values-ro-rRO",
                "values-ru", "values-ru-rRU",
                "values-si", "values-si-rLK",
                "values-sk-rSK",
                "values-sr", "values-sr-rSP",
                "values-sv", "values-sv-rSE",
                "values-ta-rIN",
                "values-tr", "values-tr-rTR",
                "values-uk", "values-uk-rUA",
                "values-vi", "values-vi-rVN",
                "values-zh", "values-zh-rCN", "values-zh-rTW"
            )
            list.addAll(knownLocales)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun loadStringsForLocale(context: Context, dirName: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            // Using Android Resources identifier scan fallback or XML parser
            val res = context.resources
            val localeCode = extractLocaleCode(dirName)
            val config = android.content.res.Configuration(res.configuration)
            
            val localeParts = localeCode.split("-")
            val locale = if (localeParts.size > 1) {
                java.util.Locale(localeParts[0], localeParts[1])
            } else {
                java.util.Locale(localeParts[0])
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                config.setLocales(android.os.LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.setLocale(locale)
            }
            val localizedContext = context.createConfigurationContext(config)
            val localizedRes = localizedContext.resources

            val fields = com.sameerasw.essentials.R.string::class.java.fields
            for (field in fields) {
                try {
                    val id = field.getInt(null)
                    val key = field.name
                    val text = localizedRes.getString(id)
                    map[key] = text
                } catch (e: Exception) {
                    // Skip non-string or unresolvable
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun clearCache() {
        cachedTranslations = null
    }
}
