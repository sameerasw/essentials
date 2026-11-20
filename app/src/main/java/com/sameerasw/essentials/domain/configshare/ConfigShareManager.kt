package com.sameerasw.essentials.domain.configshare

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sameerasw.essentials.BuildConfig
import com.sameerasw.essentials.data.repository.SettingsRepository
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ConfigShareManager {
    const val FORMAT = "essentials-config"
    const val SCHEMA = 1
    const val MAX_BYTES = 256 * 1024
    private const val MAX_KEYS = 600
    private const val MAX_STRING = 2000
    private const val MAX_SET_SIZE = 200
    private const val MAX_NUMBER = 1_000_000.0

    private val keyRegex = Regex("^[a-z0-9_@x]{1,120}$")
    private val colorRegex = Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
    private val gson = Gson()

    fun fileName(descriptor: ConfigShareDescriptor): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        val slug = descriptor.featureId.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return "${slug}_$stamp.essentials.json"
    }

    fun versionRelation(config: ParsedConfig): ConfigVersionRelation =
        when {
            config.appVersionCode < BuildConfig.VERSION_CODE -> ConfigVersionRelation.OLDER
            config.appVersionCode > BuildConfig.VERSION_CODE -> ConfigVersionRelation.NEWER
            else -> ConfigVersionRelation.SAME
        }

    fun export(
        context: Context,
        descriptor: ConfigShareDescriptor,
        selectedSections: Set<String>,
    ): String {
        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val data = linkedMapOf<String, Map<String, Any>>()
        val included = linkedSetOf<String>()
        prefs.all.toSortedMap().forEach { (key, value) ->
            val section = descriptor.sectionOf(key) ?: return@forEach
            if (section !in selectedSections || value == null) return@forEach
            val type =
                when (value) {
                    is Boolean -> "Boolean"
                    is Int -> "Int"
                    is Long -> "Long"
                    is Float -> "Float"
                    is String -> "String"
                    is Set<*> -> "StringSet"
                    else -> return@forEach
                }
            val clean = sanitize(key, type, gson.toJsonTree(value), prefs.all[key]) ?: return@forEach
            data[key] = mapOf("type" to type, "value" to clean)
            included += section
        }
        val root =
            linkedMapOf<String, Any>(
                "format" to FORMAT,
                "schema" to SCHEMA,
                "feature" to descriptor.featureId,
                "appVersionName" to BuildConfig.VERSION_NAME,
                "appVersionCode" to BuildConfig.VERSION_CODE,
                "createdAt" to isoNow(),
                "sections" to descriptor.sections.map { it.id }.filter { it in included },
                "data" to data,
            )
        return gson.newBuilder().setPrettyPrinting().create().toJson(root)
    }

    fun availableSections(
        context: Context,
        descriptor: ConfigShareDescriptor,
    ): Set<String> =
        context
            .getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
            .all.keys
            .mapNotNull { descriptor.sectionOf(it) }
            .toSet()

    fun readLimited(stream: InputStream): String? {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        stream.use { input ->
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_BYTES) return null
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }

    fun parse(
        context: Context,
        descriptor: ConfigShareDescriptor,
        text: String?,
    ): ConfigParseResult {
        if (text == null) return ConfigParseResult.Failure(ConfigParseError.TOO_LARGE)
        val root =
            try {
                JsonParser.parseString(text) as? JsonObject
            } catch (e: Exception) {
                null
            } ?: return ConfigParseResult.Failure(ConfigParseError.INVALID)

        if (root.optString("format") != FORMAT) return ConfigParseResult.Failure(ConfigParseError.INVALID)
        val schema = root.get("schema")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt
            ?: return ConfigParseResult.Failure(ConfigParseError.INVALID)
        if (schema > SCHEMA) return ConfigParseResult.Failure(ConfigParseError.NEWER_SCHEMA)
        if (root.optString("feature") != descriptor.featureId) return ConfigParseResult.Failure(ConfigParseError.WRONG_FEATURE)
        val data = root.get("data") as? JsonObject ?: return ConfigParseResult.Failure(ConfigParseError.INVALID)
        if (data.size() > MAX_KEYS) return ConfigParseResult.Failure(ConfigParseError.TOO_LARGE)

        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val local = prefs.all
        val bySection = linkedMapOf<String, MutableMap<String, Any>>()
        data.entrySet().forEach { (key, element) ->
            if (descriptor.prefixes.none { key.startsWith(it) }) return@forEach
            val section = descriptor.sectionOf(key) ?: return@forEach
            val obj = element as? JsonObject ?: return@forEach
            val type = obj.optString("type") ?: return@forEach
            val value = obj.get("value") ?: return@forEach
            val clean = sanitize(key, type, value, local[key]) ?: return@forEach
            bySection.getOrPut(section) { linkedMapOf() }[key] = clean
        }
        if (bySection.isEmpty()) return ConfigParseResult.Failure(ConfigParseError.EMPTY)

        val ordered = linkedMapOf<String, Map<String, Any>>()
        descriptor.sections.forEach { s -> bySection[s.id]?.let { ordered[s.id] = it } }
        return ConfigParseResult.Success(
            ParsedConfig(
                featureId = descriptor.featureId,
                appVersionName = root.optString("appVersionName")?.take(32) ?: "?",
                appVersionCode = root.get("appVersionCode")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt ?: 0,
                createdAt = root.optString("createdAt")?.take(40) ?: "",
                entriesBySection = ordered,
            ),
        )
    }

    fun apply(
        context: Context,
        config: ParsedConfig,
        selectedSections: Set<String>,
    ): Int {
        val editor = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE).edit()
        var count = 0
        config.entriesBySection.forEach { (section, entries) ->
            if (section !in selectedSections) return@forEach
            entries.forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is Set<*> -> editor.putStringSet(key, value as Set<String>)
                    else -> return@forEach
                }
                count++
            }
        }
        editor.apply()
        return count
    }

    private fun sanitize(
        key: String,
        type: String,
        value: JsonElement,
        localValue: Any?,
    ): Any? {
        if (!keyRegex.matches(key)) return null
        val target =
            when (localValue) {
                null -> type
                is Boolean -> "Boolean"
                is Int -> "Int"
                is Long -> "Long"
                is Float -> "Float"
                is String -> "String"
                is Set<*> -> "StringSet"
                else -> return null
            }
        val isNumeric = { t: String -> t == "Int" || t == "Long" || t == "Float" }
        if (target != type && !(isNumeric(target) && isNumeric(type))) return null

        return when (target) {
            "Boolean" ->
                if (value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) value.asBoolean else null
            "Int", "Long", "Float" -> {
                if (!value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) return null
                val d = value.asDouble
                if (d.isNaN() || d.isInfinite() || kotlin.math.abs(d) > MAX_NUMBER) return null
                when (target) {
                    "Int" -> d.toInt()
                    "Long" -> d.toLong()
                    else -> d.toFloat()
                }
            }
            "String" -> {
                if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) return null
                val s = value.asString
                if (!cleanString(s)) return null
                if (key.contains("color") && s.isNotEmpty() && !colorRegex.matches(s)) return null
                s
            }
            "StringSet" -> {
                val arr = value as? JsonArray ?: return null
                if (arr.size() > MAX_SET_SIZE) return null
                val out = linkedSetOf<String>()
                for (item in arr) {
                    if (!item.isJsonPrimitive || !item.asJsonPrimitive.isString) return null
                    val s = item.asString
                    if (!cleanString(s)) return null
                    out += s
                }
                out
            }
            else -> null
        }
    }

    private fun cleanString(s: String): Boolean = s.length <= MAX_STRING && s.none { it < ' ' }

    private fun JsonObject.optString(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}
