/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeEffects.kt
 * Description: Applies what active modes ask for and puts things back when they end.
 */

package com.sameerasw.essentials.domain.modes

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.utils.FreezeManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ModeEffects {
    private const val TAG = "ModeEffects"
    private const val PREFS_NAME = "modes_runtime_prefs"
    private const val KEY_APPLIED = "applied_modes"
    private const val KEY_BASELINES = "feature_baselines"
    private const val KEY_FROZEN = "frozen_by_mode"

    private val mutex = Mutex()
    private val gson = Gson()

    suspend fun reconcile(
        context: Context,
        activeIds: Set<String>,
        reapply: Boolean = false,
    ) = mutex.withLock {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ModesRepository.init(context)
        val modes = ModesRepository.modes.value

        val applied = prefs.getStringSet(KEY_APPLIED, emptySet()).orEmpty()
        val entered = activeIds - applied
        val exited = applied - activeIds
        if (entered.isEmpty() && exited.isEmpty() && !reapply) return@withLock
        Log.d(TAG, "Reconciling: entered=$entered exited=$exited reapply=$reapply")

        val frozenBy = readFrozen(prefs).toMutableMap()

        exited.forEach { modeId ->
            ModeAutomation.onModeExited(context, modeId)
            val claimed = frozenBy.remove(modeId).orEmpty()
            val stillWanted = frozenBy.values.flatten().toSet()
            val toUnfreeze = claimed - stillWanted
            if (toUnfreeze.isNotEmpty()) run(context, listOf(Action.UnfreezeApps(toUnfreeze.toList())))
            modes.find { it.id == modeId }?.exitActions?.takeIf { it.isNotEmpty() }?.let { run(context, it) }
            modes.find { it.id == modeId }?.takeIf { !it.tripOnStart }?.exitTripAlarmId?.let { ModeTripStarter.start(context, it) }
        }

        entered.forEach { modeId ->
            ModeAutomation.onModeEntered(context, modeId)
            val mode = modes.find { it.id == modeId } ?: return@forEach
            if (mode.freezesApps) {
                val claimedByOthers = frozenBy.values.flatten().toSet()
                // Apps the user froze themselves stay theirs, so this mode never unfreezes them later
                val claim =
                    resolveFreezePackages(context, mode)
                        .filter { it in claimedByOthers || !FreezeManager.isAppFrozen(context, it) }
                        .toSet()
                frozenBy[modeId] = claim
                val toFreeze = claim.filterNot { FreezeManager.isAppFrozen(context, it) }
                if (toFreeze.isNotEmpty()) run(context, listOf(Action.FreezeApps(toFreeze)))
            }
        }

        applyFeatures(context, prefs, modes.filter { it.id in activeIds })

        entered.forEach { modeId ->
            val mode = modes.find { it.id == modeId } ?: return@forEach
            mode.entryActions.takeIf { it.isNotEmpty() }?.let { run(context, it) }
            if (mode.tripOnStart) mode.exitTripAlarmId?.let { ModeTripStarter.start(context, it) }
        }

        prefs
            .edit()
            .putStringSet(KEY_APPLIED, activeIds)
            .putString(KEY_FROZEN, gson.toJson(frozenBy))
            .apply()
    }

    private suspend fun applyFeatures(
        context: Context,
        prefs: SharedPreferences,
        activeModes: List<Mode>,
    ) {
        val desired = mutableMapOf<ModeFeature, Boolean>()
        // Modes higher in the list win when two of them want the same switch
        activeModes.asReversed().forEach { desired.putAll(it.featureOverrides) }

        val baselines = readBaselines(prefs).toMutableMap()
        val actions = mutableListOf<Action>()

        ModeFeature.entries.forEach { feature ->
            val target = desired[feature]
            val current = feature.read(context)
            if (target != null) {
                if (feature.name !in baselines && current != null) baselines[feature.name] = current
                if (current != target) actions += feature.action(target)
            } else {
                val baseline = baselines.remove(feature.name) ?: return@forEach
                if (current != baseline) actions += feature.action(baseline)
            }
        }

        prefs.edit().putString(KEY_BASELINES, gson.toJson(baselines)).apply()
        if (actions.isNotEmpty()) run(context, actions)
    }

    fun resolveFreezePackages(
        context: Context,
        mode: Mode,
    ): Set<String> {
        val settings = SettingsRepository(context)
        val tagMap = settings.getFreezeAppTagMap()
        val byTag =
            if (mode.freezeTagIds.isEmpty()) {
                emptySet()
            } else {
                tagMap.filterValues { tags -> tags.any { it in mode.freezeTagIds } }.keys
            }
        if (mode.freezeCategories.isEmpty()) return byTag

        val protectedTags = settings.getFreezeTags().filter { it.neverAutoFreeze }.map { it.id }.toSet()
        val pm = context.packageManager
        val home =
            pm
                .resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
        val byCategory =
            pm
                .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .map { it.activityInfo.applicationInfo }
                .filter { it.category in mode.freezeCategories }
                .map { it.packageName }
                .filter { it != context.packageName && it != home }
                .filterNot { pkg -> tagMap[pkg].orEmpty().any { it in protectedTags } }
                .toSet()
        return byTag + byCategory
    }

    private suspend fun run(
        context: Context,
        actions: List<Action>,
    ) {
        runCatching { CombinedActionExecutor.executeAll(context, actions) }
            .onFailure { Log.e(TAG, "Failed to run mode actions", it) }
    }

    private fun readBaselines(prefs: SharedPreferences): Map<String, Boolean> =
        prefs.getString(KEY_BASELINES, null)?.let {
            runCatching { gson.fromJson<Map<String, Boolean>>(it, object : TypeToken<Map<String, Boolean>>() {}.type) }.getOrNull()
        } ?: emptyMap()

    private fun readFrozen(prefs: SharedPreferences): Map<String, Set<String>> =
        prefs.getString(KEY_FROZEN, null)?.let {
            runCatching { gson.fromJson<Map<String, Set<String>>>(it, object : TypeToken<Map<String, Set<String>>>() {}.type) }.getOrNull()
        } ?: emptyMap()
}
