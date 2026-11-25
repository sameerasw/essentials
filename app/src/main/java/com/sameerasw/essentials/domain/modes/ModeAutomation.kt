/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeAutomation.kt
 * Description: Turns modes on and off from their conditions, respecting manual overrides and confirmation.
 */

package com.sameerasw.essentials.domain.modes

import android.content.Context
import android.content.SharedPreferences
import android.service.notification.Condition
import android.util.Log
import com.sameerasw.essentials.domain.diy.Automation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object ModeAutomation {
    private const val TAG = "ModeAutomation"
    private const val PREFS_NAME = "modes_runtime_prefs"
    private const val KEY_AUTO_ACTIVATED = "auto_activated_modes"
    private const val KEY_SUPPRESSED = "suppressed_modes"
    private const val STARTUP_SETTLE_MS = 20_000L

    private val metConditions = ConcurrentHashMap<String, MutableSet<String>>()
    private val probedKeys = ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var startupCheckScheduled = false

    fun automationsFor(modes: List<Mode>): List<Automation> =
        modes
            .filter { it.isEnabled }
            .flatMap { mode ->
                mode.conditions.flatMapIndexed { index, condition -> condition.toAutomations(mode.id, condition.key(index)) }
            }

    fun automationsFor(context: Context): List<Automation> {
        ModesRepository.init(context)
        return automationsFor(ModesRepository.modes.value)
    }

    @Synchronized
    fun onConditionChanged(
        context: Context,
        modeId: String,
        conditionKey: String,
        met: Boolean,
    ) {
        ModesRepository.init(context)
        val mode = ModesRepository.getMode(modeId) ?: return
        val validKeys = mode.conditions.mapIndexed { index, condition -> condition.key(index) }.toSet()
        if (conditionKey !in validKeys) return

        val keys = metConditions.getOrPut(modeId) { mutableSetOf() }
        val wasMet = keys.isNotEmpty()
        if (met) keys += conditionKey else keys -= conditionKey
        keys.retainAll(validKeys)
        val isMet = keys.isNotEmpty()
        Log.d(TAG, "Mode $modeId condition $conditionKey met=$met, any=$isMet")
        if (wasMet == isMet) return

        if (isMet) onConditionsMet(context, mode) else onConditionsCleared(context, mode)
    }

    fun onModesChanged(
        context: Context,
        modes: List<Mode>,
    ) {
        val appContext = context.applicationContext
        val unprobed =
            modes
                .filter { it.isEnabled }
                .flatMap { mode ->
                    mode.conditions.mapIndexed { index, condition -> ModeConditionProbe.Target(mode.id, condition.key(index), condition) }
                }.filter { probedKeys.add("${it.modeId}:${it.key}") }
        if (unprobed.isNotEmpty()) scope.launch { ModeConditionProbe.probe(appContext, unprobed) }

        if (!startupCheckScheduled) {
            startupCheckScheduled = true
            scope.launch {
                delay(STARTUP_SETTLE_MS)
                releaseStaleModes(appContext)
            }
        }

        scope.launch { pruneConditions(appContext, modes) }
    }

    // Condition state lives in memory, so after a restart anything this turned on whose conditions
    // never reported back is assumed to have ended while the app wasn't running
    @Synchronized
    private fun releaseStaleModes(context: Context) {
        val prefs = prefs(context)
        prefs.getStringSet(KEY_AUTO_ACTIVATED, emptySet()).orEmpty().forEach { modeId ->
            if (metConditions[modeId].orEmpty().isNotEmpty()) return@forEach
            edit(prefs, KEY_AUTO_ACTIVATED) { it - modeId }
            ModesController.setActive(context, modeId, false, Condition.SOURCE_CONTEXT)
        }
    }

    @Synchronized
    private fun pruneConditions(
        context: Context,
        modes: List<Mode>,
    ) {
        metConditions.keys.toList().forEach { modeId ->
            val mode = modes.find { it.id == modeId }
            val validKeys = mode?.conditions?.mapIndexed { index, condition -> condition.key(index) }?.toSet().orEmpty()
            val keys = metConditions[modeId] ?: return@forEach
            val wasMet = keys.isNotEmpty()
            keys.retainAll(validKeys)
            if (mode != null && wasMet && keys.isEmpty()) onConditionsCleared(context, mode)
        }
    }

    fun onModeExited(
        context: Context,
        modeId: String,
    ) {
        val prefs = prefs(context)
        edit(prefs, KEY_AUTO_ACTIVATED) { it - modeId }
        // Turned off by hand while its conditions still hold, so stay off until they stop holding
        if (metConditions[modeId].orEmpty().isNotEmpty()) edit(prefs, KEY_SUPPRESSED) { it + modeId }
    }

    fun respond(
        context: Context,
        modeId: String,
        accept: Boolean,
    ) {
        ModeConfirmNotifier.dismiss(context, modeId)
        val mode = ModesRepository.also { it.init(context) }.getMode(modeId) ?: return
        if (metConditions[modeId].orEmpty().isEmpty()) return
        if (accept) activate(context, mode) else edit(prefs(context), KEY_SUPPRESSED) { it + modeId }
    }

    private fun onConditionsMet(
        context: Context,
        mode: Mode,
    ) {
        val prefs = prefs(context)
        if (mode.id in prefs.getStringSet(KEY_SUPPRESSED, emptySet()).orEmpty()) return
        if (mode.id in ModesController.activeModeIds.value) return
        if (mode.askBeforeActivating) {
            ModeConfirmNotifier.show(context, mode)
        } else {
            activate(context, mode)
        }
    }

    private fun onConditionsCleared(
        context: Context,
        mode: Mode,
    ) {
        val prefs = prefs(context)
        ModeConfirmNotifier.dismiss(context, mode.id)
        edit(prefs, KEY_SUPPRESSED) { it - mode.id }
        if (mode.id in prefs.getStringSet(KEY_AUTO_ACTIVATED, emptySet()).orEmpty()) {
            edit(prefs, KEY_AUTO_ACTIVATED) { it - mode.id }
            ModesController.setActive(context, mode.id, false, Condition.SOURCE_CONTEXT)
        }
    }

    private fun activate(
        context: Context,
        mode: Mode,
    ) {
        edit(prefs(context), KEY_AUTO_ACTIVATED) { it + mode.id }
        if (!ModesController.setActive(context, mode.id, true, Condition.SOURCE_CONTEXT)) {
            edit(prefs(context), KEY_AUTO_ACTIVATED) { it - mode.id }
        }
    }

    private fun prefs(context: Context): SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun edit(
        prefs: SharedPreferences,
        key: String,
        change: (Set<String>) -> Set<String>,
    ) {
        val current = prefs.getStringSet(key, emptySet()).orEmpty()
        prefs.edit().putStringSet(key, change(current)).apply()
    }
}
