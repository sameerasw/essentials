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
        return all[key] ?: emptyMap()
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
                "values-ach", "values-af", "values-ar", "values-ca", "values-cs", "values-da",
                "values-de", "values-el", "values-en", "values-es", "values-fi", "values-fr",
                "values-he", "values-hu", "values-in-rID", "values-it", "values-iw-rIL", "values-ja",
                "values-ko", "values-nl", "values-no", "values-pl", "values-pt-rBR", "values-pt-rPT",
                "values-ro", "values-ru", "values-si", "values-sr", "values-sv", "values-tr",
                "values-uk", "values-vi", "values-zh-rCN", "values-zh-rTW"
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
