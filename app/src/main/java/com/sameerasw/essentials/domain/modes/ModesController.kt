/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModesController.kt
 * Description: Keeps each Essentials mode in sync with the system mode it owns and switches them.
 */

package com.sameerasw.essentials.domain.modes

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.notification.Condition
import android.util.Log
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.ui.activities.ModeEditorActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ModesController {
    private const val TAG = "ModesController"
    private const val CONDITION_SCHEME = "condition"
    private const val CONDITION_AUTHORITY = "com.sameerasw.essentials"
    private const val CONDITION_PATH = "mode"

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    private val _activeModeIds = MutableStateFlow<Set<String>>(emptySet())
    val activeModeIds = _activeModeIds.asStateFlow()

    private val ruleIds = mutableMapOf<String, String>()

    fun hasAccess(context: Context): Boolean = notificationManager(context).isNotificationPolicyAccessGranted

    fun modeIdForRule(ruleId: String): String? = synchronized(ruleIds) { ruleIds.entries.find { it.value == ruleId }?.key }

    fun modeIdForRule(
        context: Context,
        ruleId: String,
    ): String? {
        modeIdForRule(ruleId)?.let { return it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM || !hasAccess(context)) return null
        return runCatching { notificationManager(context).getAutomaticZenRule(ruleId)?.let(::modeIdOf) }.getOrNull()
    }

    fun sync(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM || !hasAccess(context)) return
        runCatching { syncRules(context) }.onFailure { Log.e(TAG, "Failed to sync modes", it) }
        refreshActive(context)
    }

    fun refreshActive(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM || !hasAccess(context)) {
            _activeModeIds.value = emptySet()
            return
        }
        runCatching {
            val nm = notificationManager(context)
            val owned = ownedRules(nm)
            synchronized(ruleIds) {
                ruleIds.clear()
                owned.forEach { (modeId, entry) -> ruleIds[modeId] = entry.first }
            }
            _activeModeIds.value =
                owned
                    .filter { (_, entry) -> nm.getAutomaticZenRuleState(entry.first) == Condition.STATE_TRUE }
                    .keys
        }.onFailure { Log.e(TAG, "Failed to read mode states", it) }
    }

    fun setActive(
        context: Context,
        modeId: String,
        active: Boolean,
        source: Int = Condition.SOURCE_USER_ACTION,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM || !hasAccess(context)) return false
        val mode = ModesRepository.getMode(modeId) ?: return false
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
        val ruleId = ruleIdFor(context, modeId) ?: return null
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
        val modes = ModesRepository.modes.value

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
