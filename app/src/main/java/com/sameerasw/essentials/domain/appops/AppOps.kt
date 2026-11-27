/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: App Ops
 * File: AppOps.kt
 * Description: App op catalog, modes and parsing of appops shell output.
 */

package com.sameerasw.essentials.domain.appops

import android.app.AppOpsManager
import androidx.annotation.StringRes
import com.sameerasw.essentials.R
import org.lsposed.hiddenapibypass.HiddenApiBypass

enum class AppOpMode(
    val shell: String,
    @StringRes val label: Int,
) {
    ALLOW("allow", R.string.app_ops_mode_allow),
    FOREGROUND("foreground", R.string.app_ops_mode_foreground),
    IGNORE("ignore", R.string.app_ops_mode_ignore),
    DENY("deny", R.string.app_ops_mode_deny),
    DEFAULT("default", R.string.app_ops_mode_default),
    ;

    val isRestricted: Boolean get() = this == IGNORE || this == DENY

    companion object {
        fun fromShell(value: String): AppOpMode? =
            when (value.lowercase()) {
                "allow" -> ALLOW
                "foreground" -> FOREGROUND
                "ignore" -> IGNORE
                "deny", "errored" -> DENY
                "default" -> DEFAULT
                else -> null
            }

        fun fromCode(code: Int): AppOpMode =
            when (code) {
                0 -> ALLOW
                1 -> IGNORE
                2 -> DENY
                4 -> FOREGROUND
                else -> DEFAULT
            }
    }
}

data class AppOpInfo(
    val name: String,
    val permission: String?,
    val defaultMode: AppOpMode,
) {
    val label: String =
        name
            .lowercase()
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }
}

data class AppOpEntry(
    val op: String,
    val mode: AppOpMode,
    val isUidMode: Boolean,
    val lastUsed: String?,
)

fun AppOpMode.isEffectivelyAllowed(defaultMode: AppOpMode): Boolean =
    when (this) {
        AppOpMode.ALLOW, AppOpMode.FOREGROUND -> true
        AppOpMode.IGNORE, AppOpMode.DENY -> false
        AppOpMode.DEFAULT -> defaultMode != AppOpMode.IGNORE && defaultMode != AppOpMode.DENY
    }

object AppOpsCatalog {
    private val opNameRegex = Regex("^[A-Z][A-Z0-9_]+$")

    fun isValidOpName(name: String) = opNameRegex.matches(name)

    fun load(): List<AppOpInfo> {
        val cls = AppOpsManager::class.java
        val count = opCount(cls) ?: return emptyList()
        return (0 until count)
            .mapNotNull { code ->
                val name = runCatching { HiddenApiBypass.invoke(cls, null, "opToName", code) as? String }.getOrNull()
                if (name == null || !isValidOpName(name) || name == "NONE") return@mapNotNull null
                val permission = runCatching { HiddenApiBypass.invoke(cls, null, "opToPermission", code) as? String }.getOrNull()
                val defaultMode =
                    runCatching { HiddenApiBypass.invoke(cls, null, "opToDefaultMode", code) as? Int }
                        .getOrNull()
                        ?.let(AppOpMode::fromCode)
                        ?: AppOpMode.DEFAULT
                AppOpInfo(name, permission, defaultMode)
            }.distinctBy { it.name }
            .sortedBy { it.name }
    }

    private fun opCount(cls: Class<AppOpsManager>): Int? =
        runCatching { cls.getDeclaredField("_NUM_OP").apply { isAccessible = true }.getInt(null) }.getOrNull()
            ?: runCatching {
                HiddenApiBypass.getStaticFields(cls).firstOrNull { it.name == "_NUM_OP" }?.let {
                    it.isAccessible = true
                    it.getInt(null)
                }
            }.getOrNull()
}

object AppOpsParser {
    private const val PACKAGE_MARKER = "@@PKG "
    private val packageRegex = Regex("^[A-Za-z0-9_.]+$")
    private val uidLineRegex = Regex("^Uid mode: ([A-Z][A-Z0-9_]+): ([a-z]+)")
    private val opLineRegex = Regex("^([A-Z][A-Z0-9_]+): ([a-z]+)(.*)$")
    private val timeRegex = Regex("time=\\+?([^;]+?)(?:\\s+ago)?\\s*(?:;|$)")

    fun isValidPackage(packageName: String) = packageRegex.matches(packageName)

    fun batchCommand(packages: List<String>): String =
        packages
            .filter(::isValidPackage)
            .joinToString("; ") { "echo '$PACKAGE_MARKER$it'; appops get $it 2>/dev/null" }

    fun parseBatch(output: String): Map<String, Map<String, AppOpEntry>> {
        val result = mutableMapOf<String, MutableList<String>>()
        var current: MutableList<String>? = null
        output.lineSequence().forEach { line ->
            if (line.startsWith(PACKAGE_MARKER)) {
                current = mutableListOf<String>().also { result[line.removePrefix(PACKAGE_MARKER).trim()] = it }
            } else {
                current?.add(line)
            }
        }
        return result.mapValues { (_, lines) -> parse(lines) }
    }

    fun parse(lines: List<String>): Map<String, AppOpEntry> {
        val entries = mutableMapOf<String, AppOpEntry>()
        lines.forEach { raw ->
            if (raw.isEmpty() || raw.first().isWhitespace()) return@forEach
            val line = raw.trim()
            uidLineRegex.find(line)?.let { match ->
                val mode = AppOpMode.fromShell(match.groupValues[2]) ?: return@let
                val op = match.groupValues[1]
                entries[op] = AppOpEntry(op, mode, isUidMode = true, lastUsed = entries[op]?.lastUsed)
                return@forEach
            }
            opLineRegex.find(line)?.let { match ->
                val op = match.groupValues[1]
                val mode = AppOpMode.fromShell(match.groupValues[2]) ?: return@let
                val lastUsed = timeRegex.find(match.groupValues[3])?.groupValues?.get(1)?.trim()
                val existing = entries[op]
                entries[op] =
                    if (existing?.isUidMode == true && existing.mode != AppOpMode.DEFAULT) {
                        existing.copy(lastUsed = lastUsed ?: existing.lastUsed)
                    } else {
                        AppOpEntry(op, mode, isUidMode = false, lastUsed = lastUsed)
                    }
            }
        }
        return entries
    }
}
