/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModesController.kt
 * Description: Keeps Essentials modes in sync with the system modes they own or link to, and switches them.
 */

package com.sameerasw.essentials.domain.modes

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.Condition
import android.util.Log
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.domain.controller.ZenModeController
import com.sameerasw.essentials.ui.activities.ModeEditorActivity
import com.sameerasw.essentials.utils.ShizukuUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object ModesController {
    private const val TAG = "ModesController"
    private const val CONDITION_SCHEME = "condition"
    private const val CONDITION_AUTHORITY = "com.sameerasw.essentials"
    private const val CONDITION_PATH = "mode"
    private const val WATCH_DEBOUNCE_MS = 400L
    private val WATCHED_SETTINGS = listOf("zen_mode_config_etag", "zen_mode")

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    private val _activeModeIds = MutableStateFlow<Set<String>>(emptySet())
    val activeModeIds = _activeModeIds.asStateFlow()

    private val ruleIds = mutableMapOf<String, String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observer: ContentObserver? = null

    fun hasAccess(context: Context): Boolean = notificationManager(context).isNotificationPolicyAccessGranted

    fun canLinkSystemModes(): Boolean = isSupported && ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()

    fun startWatching(context: Context) {
        if (observer != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val appContext = context.applicationContext
        val handler = Handler(Looper.getMainLooper())
        val refresh = Runnable { scope.launch { refreshActive(appContext) } }
        val contentObserver =
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    handler.removeCallbacks(refresh)
                    handler.postDelayed(refresh, WATCH_DEBOUNCE_MS)
                }
            }
        WATCHED_SETTINGS.forEach { key ->
            runCatching { appContext.contentResolver.registerContentObserver(Settings.Global.getUriFor(key), false, contentObserver) }
                .onFailure { Log.e(TAG, "Failed to watch $key", it) }
        }
        observer = contentObserver
    }

    fun systemModes(context: Context): List<SystemMode>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM || !canLinkSystemModes()) return null
        val owned =
            if (hasAccess(context)) {
                runCatching { ownedRules(notificationManager(context)).values.map { it.first }.toSet() }.getOrDefault(emptySet())
            } else {
                emptySet()
            }
        return runCatching {
            ZenModeController
                .getModes()
                .filterNot { it.id in owned }
                .map { mode ->
                    SystemMode(
                        id = mode.id,
                        name = mode.name,
                        isEnabled = mode.isEnabled,
                        type = mode.type,
                        iconPackage = mode.packageName,
                        iconName = SystemModeIcons.iconName(context, mode.packageName, mode.iconResId),
                    )
                }
        }.onFailure { Log.e(TAG, "Failed to list system modes", it) }
            .getOrNull()
    }

    // Name and icon of a linked mode belong to the system, so pick up any change made there
    private fun refreshLinkedModes(context: Context) {
        val linked = ModesRepository.modes.value.filter { it.isLinked }
        if (linked.isEmpty()) return
        val systemModes = systemModes(context) ?: return
        linked.forEach { mode ->
            val systemMode = systemModes.find { it.id == mode.linkedModeId } ?: return@forEach
            val updated = mode.linkedTo(systemMode)
            if (updated != mode) ModesRepository.upsert(updated)
        }
    }

    fun modeIdForRule(ruleId: String): String? = synchronized(ruleIds) { ruleIds.entries.find { it.value == ruleId }?.key }

    fun modeIdForRule(
        context: Context,
        ruleId: String,
    ): String? {
        modeIdForRule(ruleId)?.let { return it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM || !hasAccess(context)) return null
        return runCatching { notificationManager(context).getAutomaticZenRule(ruleId)?.let(::modeIdOf) }.getOrNull()
    }

    fun sync(
        context: Context,
        reapply: Boolean = false,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        if (hasAccess(context)) runCatching { syncRules(context) }.onFailure { Log.e(TAG, "Failed to sync modes", it) }
        refreshLinkedModes(context)
        refreshActive(context, reapply)
    }

    fun requestSync(
        context: Context,
        reapply: Boolean = false,
    ) {
        val appContext = context.applicationContext
        scope.launch { sync(appContext, reapply) }
    }

    fun refreshActive(
        context: Context,
        reapply: Boolean = false,
    ): Job? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null
        val appContext = context.applicationContext
        val modes = ModesRepository.modes.value
        val ownedIds = modes.filterNot { it.isLinked }.map { it.id }.toSet()
        val linkedIds = modes.filter { it.isLinked }.map { it.id }.toSet()
        val previous = _activeModeIds.value

        // When a side can't be read (no access, Shizuku gone), keep what was last known rather than ending its modes
        val ownedActive = ownedActiveIds(context)?.intersect(ownedIds) ?: previous.intersect(ownedIds)
        val linkedActive = linkedActiveIds(modes) ?: previous.intersect(linkedIds)
        val active = ownedActive + linkedActive

        _activeModeIds.value = active
        return scope.launch { ModeEffects.reconcile(appContext, active, reapply) }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun ownedActiveIds(context: Context): Set<String>? {
        if (!hasAccess(context)) return null
        return runCatching {
            val nm = notificationManager(context)
            val owned = ownedRules(nm)
            synchronized(ruleIds) {
                ruleIds.clear()
                owned.forEach { (modeId, entry) -> ruleIds[modeId] = entry.first }
            }
            owned
                .filter { (_, entry) -> nm.getAutomaticZenRuleState(entry.first) == Condition.STATE_TRUE }
                .keys
        }.onFailure { Log.e(TAG, "Failed to read mode states", it) }
            .getOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun linkedActiveIds(modes: List<Mode>): Set<String>? {
        val linked = modes.filter { it.isLinked }
        if (linked.isEmpty()) return emptySet()
        if (!canLinkSystemModes()) return null
        val active =
            runCatching { ZenModeController.activeModes() }
                .onFailure { Log.e(TAG, "Failed to read system mode states", it) }
                .getOrNull() ?: return null
        return linked
            .filter { mode -> active.any { it.id == mode.linkedModeId || (mode.linkedModeName != null && it.name == mode.linkedModeName) } }
            .map { it.id }
            .toSet()
    }

    fun setActive(
        context: Context,
        modeId: String,
        active: Boolean,
        source: Int = Condition.SOURCE_USER_ACTION,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return false
        val mode = ModesRepository.getMode(modeId) ?: return false
        if (mode.linkedModeId != null) {
            if (!canLinkSystemModes()) return false
            return runCatching { ZenModeController.setActive(mode.linkedModeId, mode.linkedModeName.orEmpty(), active) }
                .onFailure { Log.e(TAG, "Failed to switch linked mode $modeId", it) }
                .getOrNull()
                .let { it == ZenModeController.Result.DONE }
                .also { refreshActive(context) }
        }
        if (!hasAccess(context)) return false
        return runCatching {
            val nm = notificationManager(context)
            val ruleId =
                ownedRules(nm)[modeId]?.first
                    ?: run {
                        syncRules(context)
                        ownedRules(nm)[modeId]?.first
                    }
                    ?: return false
            val state = if (active) Condition.STATE_TRUE else Condition.STATE_FALSE
            nm.setAutomaticZenRuleState(ruleId, Condition(conditionUri(modeId), mode.name, state, source))
            true
        }.onFailure { Log.e(TAG, "Failed to switch mode $modeId", it) }
            .getOrDefault(false)
            .also { refreshActive(context) }
    }

    fun systemSettingsIntent(
        context: Context,
        modeId: String,
    ): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null
        val ruleId = ModesRepository.getMode(modeId)?.linkedModeId ?: ruleIdFor(context, modeId) ?: return null
        return Intent(Settings.ACTION_AUTOMATIC_ZEN_RULE_SETTINGS)
            .putExtra(Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID, ruleId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun ruleIdFor(
        context: Context,
        modeId: String,
    ): String? {
        synchronized(ruleIds) { ruleIds[modeId] }?.let { return it }
        if (!hasAccess(context)) return null
        return runCatching { ownedRules(notificationManager(context))[modeId]?.first }.getOrNull()
    }

    private fun conditionUri(modeId: String): Uri =
        Uri
            .Builder()
            .scheme(CONDITION_SCHEME)
            .authority(CONDITION_AUTHORITY)
            .appendPath(CONDITION_PATH)
            .appendPath(modeId)
            .build()

    private fun modeIdOf(rule: AutomaticZenRule): String? {
        val uri = rule.conditionId ?: return null
        if (uri.scheme != CONDITION_SCHEME || uri.authority != CONDITION_AUTHORITY) return null
        val segments = uri.pathSegments
        return if (segments.size == 2 && segments[0] == CONDITION_PATH) segments[1] else null
    }

    private fun ownedRules(nm: NotificationManager): Map<String, Pair<String, AutomaticZenRule>> =
        nm.automaticZenRules
            .mapNotNull { (ruleId, rule) -> modeIdOf(rule)?.let { it to (ruleId to rule) } }
            .toMap()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun syncRules(context: Context) {
        val nm = notificationManager(context)
        val owned = ownedRules(nm)
        val modes = ModesRepository.modes.value.filterNot { it.isLinked }

        modes.forEach { mode ->
            val existing = owned[mode.id]
            if (existing == null) {
                nm.addAutomaticZenRule(newRule(context, mode))
            } else {
                val (ruleId, rule) = existing
                // Starting from the live rule keeps whatever the user changed in system settings
                val updated =
                    AutomaticZenRule
                        .Builder(rule)
                        .setName(mode.name)
                        .setIconResId(ModeIcons.resolve(mode.iconName))
                        .setEnabled(mode.isEnabled)
                        .setManualInvocationAllowed(true)
                        .setConfigurationActivity(configurationActivity(context))
                        .build()
                nm.updateAutomaticZenRule(ruleId, updated)
            }
        }

        owned
            .filterKeys { modeId -> modes.none { it.id == modeId } }
            .values
            .forEach { (ruleId, _) -> nm.removeAutomaticZenRule(ruleId) }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun newRule(
        context: Context,
        mode: Mode,
    ): AutomaticZenRule =
        AutomaticZenRule
            .Builder(mode.name, conditionUri(mode.id))
            .setType(AutomaticZenRule.TYPE_OTHER)
            .setIconResId(ModeIcons.resolve(mode.iconName))
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            .setManualInvocationAllowed(true)
            .setConfigurationActivity(configurationActivity(context))
            .setEnabled(mode.isEnabled)
            .build()

    private fun configurationActivity(context: Context) = ComponentName(context, ModeEditorActivity::class.java)

    private fun notificationManager(context: Context) = context.getSystemService(NotificationManager::class.java)
}
