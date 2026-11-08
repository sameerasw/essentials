/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: QsTileLayoutRestorer.kt
 * Description: Keeps a copy of the Quick Settings layout in the exported config and puts the app's tiles back after an import.
 */

package com.sameerasw.essentials.services.tiles

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit
import com.sameerasw.essentials.utils.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * SystemUI owns the tile layout (`sysui_qs_tiles`) and drops an app's tiles when it is uninstalled, so a reinstall
 * (e.g. with a different signing key) loses them. The app keeps the last layout it saw in its prefs, which makes it
 * part of the exported config; after an import the missing tiles become "pending" and are put back either by writing
 * the layout through Shizuku/root (original positions) or through the system add-tile dialog, one tile at a time.
 */
object QsTileLayoutRestorer {
    const val SETTING = "sysui_qs_tiles"

    // Last layout seen on this device, exported with the config.
    const val KEY_LAYOUT_SNAPSHOT = "qs_tiles_layout_snapshot"

    // Layout from an imported config, used only to place tiles back where they were.
    const val KEY_RESTORE_LAYOUT = "qs_tiles_restore_layout"

    // Class names of tiles from an imported config that are not in Quick Settings yet.
    const val KEY_PENDING = "qs_tiles_pending_restore"

    const val ADDED_SUFFIX = "_is_added"
    private const val PREFS = "essentials_prefs"
    private const val OWN_PACKAGE_PREFIX = "com.sameerasw.essentials"
    private const val TAG = "QsTileRestore"

    data class ShellResult(
        val restored: Set<String>,
        val remaining: Set<String>,
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Class name of a `custom(pkg/cls)` tile spec, or null for system tiles. */
    fun customClass(spec: String): String? {
        val inner = spec.trim().takeIf { it.startsWith("custom(") && it.endsWith(")") }
            ?.removePrefix("custom(")?.removeSuffix(")") ?: return null
        val slash = inner.indexOf('/')
        if (slash <= 0) return null
        val pkg = inner.substring(0, slash)
        val cls = inner.substring(slash + 1)
        return if (cls.startsWith(".")) pkg + cls else cls
    }

    /** Same format SystemUI writes: `custom(pkg/.Relative)` when the class lives under the package. */
    fun customSpec(packageName: String, className: String): String {
        val cls = if (className.startsWith("$packageName.")) className.removePrefix(packageName) else className
        return "custom($packageName/$cls)"
    }

    fun parse(layout: String?): List<String> =
        layout.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() && it != "null" }

    /**
     * Inserts [toRestore] tiles into [current], each after the closest tile that preceded it in [saved] (or before
     * the closest one that followed it). Tiles not found in [saved] go at the end. [current] order is never changed.
     */
    fun merge(
        current: List<String>,
        saved: List<String>,
        toRestore: Set<String>,
        packageName: String,
    ): List<String> {
        fun key(spec: String) = customClass(spec) ?: spec
        val result = current.toMutableList()
        val present = result.map(::key).toMutableSet()
        val savedKeys = saved.map(::key)
        savedKeys.forEachIndexed { i, k ->
            if (k !in toRestore || k in present) return@forEachIndexed
            val prev = savedKeys.subList(0, i).lastOrNull { it in present }
            val next = savedKeys.subList(i + 1, savedKeys.size).firstOrNull { it in present }
            val pos = when {
                prev != null -> result.indexOfFirst { key(it) == prev } + 1
                next != null -> result.indexOfFirst { key(it) == next }
                else -> result.size
            }
            result.add(pos, customSpec(packageName, k))
            present += k
        }
        toRestore.filter { it !in present }.sorted().forEach {
            result.add(customSpec(packageName, it))
            present += it
        }
        return result
    }

    /**
     * The saved layout is the truth when the backup has one; the "added" flags can be stale (a tile removed while
     * the app wasn't running never clears its flag), so they're only used for backups made without a layout.
     */
    fun tilesToRestore(fromLayout: Set<String>, fromFlags: Set<String>): Set<String> =
        fromLayout.ifEmpty { fromFlags }

    private fun isOwnTile(className: String) =
        className.startsWith(OWN_PACKAGE_PREFIX) && QsTileRegistry.getTileByClassName(className) != null

    /** Reads the layout without shell first, then through Shizuku/root when the plain read is blocked. */
    fun readLayout(context: Context, allowShell: Boolean = true): String? {
        val direct = runCatching { Settings.Secure.getString(context.contentResolver, SETTING) }.getOrNull()
        if (!direct.isNullOrBlank()) return direct
        if (!allowShell || !ShellUtils.hasPermission(context)) return null
        return ShellUtils.runCommandWithOutput(context, "settings get secure $SETTING", notifyOnError = false)
            ?.takeIf { it.isNotBlank() && it != "null" }
    }

    /** Keeps the exported copy of the layout up to date. Only called with a layout that was actually read. */
    fun rememberLayout(context: Context, layout: String) {
        if (parse(layout).isEmpty()) return
        prefs(context).edit { putString(KEY_LAYOUT_SNAPSHOT, layout) }
    }

    /** Reads the live layout (through Shizuku/root when needed) into the exported copy. False when it couldn't be read. */
    fun captureForExport(context: Context): Boolean {
        val layout = readLayout(context)
        val specs = parse(layout)
        Log.i(TAG, "export: shell=${ShellUtils.hasPermission(context)} read=${layout != null} tiles=${specs.size} own=${specs.mapNotNull(::customClass).filter(::isOwnTile).size}")
        if (layout == null || specs.isEmpty()) return false
        rememberLayout(context, layout)
        return true
    }

    fun pending(context: Context): Set<String> = prefs(context).getStringSet(KEY_PENDING, null).orEmpty()

    fun locallyAddedTiles(context: Context): Set<String> =
        prefs(context).all
            .filter { (k, v) -> k.endsWith(ADDED_SUFFIX) && v == true }
            .keys.map { it.removeSuffix(ADDED_SUFFIX) }.toSet()

    /**
     * Run right after an import. [localAdded] are the tiles flagged as added on this device before the import.
     * Turns the imported layout and "added" flags into pending tiles and resets the flags to this device's state,
     * so imported flags no longer make missing tiles look added.
     */
    fun prepareAfterImport(context: Context, localAdded: Set<String>) {
        val p = prefs(context)
        val importedLayout = p.getString(KEY_LAYOUT_SNAPSHOT, null)
        val fromLayout = parse(importedLayout).mapNotNull(::customClass).toSet()
        val fromFlags = locallyAddedTiles(context)
        val present = parse(readLayout(context)).mapNotNull(::customClass).toSet()
        val pending = tilesToRestore(fromLayout, fromFlags).filter(::isOwnTile).toSet() - localAdded - present
        Log.i(TAG, "import: savedLayout=${importedLayout != null} ownInSaved=${fromLayout.filter(::isOwnTile).size} flagged=${fromFlags.size} present=${present.size} pending=${pending.size}")
        p.edit {
            p.all.keys.filter { it.endsWith(ADDED_SUFFIX) }.forEach { k ->
                putBoolean(k, k.removeSuffix(ADDED_SUFFIX) in localAdded || k.removeSuffix(ADDED_SUFFIX) in present)
            }
            if (importedLayout != null) putString(KEY_RESTORE_LAYOUT, importedLayout) else remove(KEY_RESTORE_LAYOUT)
            if (pending.isEmpty()) remove(KEY_PENDING) else putStringSet(KEY_PENDING, pending)
        }
    }

    /** Drops pending tiles that are now in Quick Settings (however they got there). Returns what is still pending. */
    fun reconcile(context: Context, presentClasses: Set<String>): Set<String> {
        val pending = pending(context)
        if (pending.isEmpty()) return pending
        val left = pending - presentClasses
        if (left != pending) savePending(context, left)
        return left
    }

    fun markAdded(context: Context, className: String) {
        prefs(context).edit { putBoolean("$className$ADDED_SUFFIX", true) }
        savePending(context, pending(context) - className)
    }

    fun clearPending(context: Context) {
        savePending(context, emptySet())
        prefs(context).edit { remove(KEY_RESTORE_LAYOUT) }
    }

    fun hasRestoreLayout(context: Context) = parse(prefs(context).getString(KEY_RESTORE_LAYOUT, null)).isNotEmpty()

    private fun shellGet(context: Context): List<String> =
        parse(ShellUtils.runCommandWithOutput(context, "settings get secure $SETTING", notifyOnError = false))

    /**
     * Applies [specs] as the tile list. Newer SystemUI (Android 16/17 QS pipeline) owns the list and puts the old
     * order back when `sysui_qs_tiles` is written from outside, so the list goes through SystemUI's own
     * `cmd statusbar set-tiles`; the plain setting write is only a fallback for builds without that command.
     */
    private suspend fun shellPut(context: Context, specs: List<String>): Boolean {
        val value = specs.joinToString(",")
        if (value.contains('\'')) return false
        val commands = listOf(
            "set-tiles" to "cmd statusbar set-tiles '$value'",
            "setting" to "settings put secure $SETTING '$value'",
        )
        for ((name, command) in commands) {
            ShellUtils.runCommandWithOutput(context, command, notifyOnError = false)
            if (awaitLayout(context, specs)) {
                Log.i(TAG, "write($name): applied ${specs.size} tiles")
                return true
            }
            Log.w(TAG, "write($name): not applied, layout is ${shellGet(context).size} tiles")
        }
        return false
    }

    // SystemUI applies the list asynchronously and may rewrite the setting shortly after, so check it settled.
    private suspend fun awaitLayout(context: Context, specs: List<String>): Boolean {
        var matched = 0
        repeat(12) {
            delay(250)
            matched = if (shellGet(context) == specs) matched + 1 else 0
            if (matched >= 4) return true
        }
        return false
    }

    /**
     * Moves [classes] (already in Quick Settings, e.g. added through the system dialog, which always appends)
     * to where they were in the imported layout. Needs Shizuku/root.
     */
    suspend fun repositionWithShell(context: Context, classes: Set<String>): Boolean = withContext(Dispatchers.IO) {
        if (classes.isEmpty() || !ShellUtils.hasPermission(context)) return@withContext false
        val saved = parse(prefs(context).getString(KEY_RESTORE_LAYOUT, null))
        val current = shellGet(context)
        Log.i(TAG, "reposition: classes=${classes.size} saved=${saved.size} current=${current.size}")
        if (saved.isEmpty() || current.isEmpty()) return@withContext false
        val stripped = current.filterNot { customClass(it) in classes }
        val merged = merge(stripped, saved, classes, context.packageName)
        if (merged == current) return@withContext true
        shellPut(context, merged)
    }

    private fun savePending(context: Context, pending: Set<String>) {
        prefs(context).edit {
            if (pending.isEmpty()) {
                remove(KEY_PENDING)
            } else {
                putStringSet(KEY_PENDING, pending)
            }
        }
    }

    /**
     * Writes the pending tiles back into `sysui_qs_tiles` at their old positions through Shizuku/root.
     * Never writes when the current layout can't be read, so the user's layout is never replaced by a partial one.
     */
    suspend fun restoreWithShell(context: Context): ShellResult = withContext(Dispatchers.IO) {
        val pending = pending(context)
        val shell = ShellUtils.hasPermission(context)
        Log.i(TAG, "restore: pending=${pending.size} shell=$shell savedLayout=${hasRestoreLayout(context)}")
        if (pending.isEmpty() || !shell) return@withContext ShellResult(emptySet(), pending)
        val current = shellGet(context)
        if (current.isEmpty()) {
            Log.w(TAG, "restore: could not read current layout, not writing")
            return@withContext ShellResult(emptySet(), pending)
        }
        val saved = parse(prefs(context).getString(KEY_RESTORE_LAYOUT, null))
        val merged = merge(current, saved, pending, context.packageName)
        if (merged != current) shellPut(context, merged)
        val now = parse(readLayout(context)).mapNotNull(::customClass).toSet()
        val restored = pending.filter { it in now }.toSet()
        Log.i(TAG, "restore: restored=${restored.size} remaining=${pending.size - restored.size}")
        restored.forEach { markAdded(context, it) }
        ShellResult(restored, pending - restored)
    }
}
