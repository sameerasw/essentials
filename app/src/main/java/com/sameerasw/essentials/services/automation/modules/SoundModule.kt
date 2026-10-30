/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: SoundModule.kt
 * Description: Background service component for SoundModule.kt.
 */

package com.sameerasw.essentials.services.automation.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import com.sameerasw.essentials.domain.diy.State as DIYState

class SoundModule : AutomationModule {
    companion object {
        const val ID = "sound_module"

        fun soundModeOf(ringerMode: Int): Action.SoundModeType? =
            when (ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> Action.SoundModeType.SOUND
                AudioManager.RINGER_MODE_VIBRATE -> Action.SoundModeType.VIBRATE
                AudioManager.RINGER_MODE_SILENT -> Action.SoundModeType.SILENT
                else -> null
            }
    }

    override val id: String = ID

    @Volatile
    private var automations: List<Automation> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null
    private var currentMode: Action.SoundModeType? = null
    private val stateEntries = StateEntries()

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (intent.action != AudioManager.RINGER_MODE_CHANGED_ACTION) return
                val audioManager = context.getSystemService(AudioManager::class.java) ?: return
                val mode = soundModeOf(audioManager.ringerMode) ?: return
                if (mode == currentMode) return
                currentMode = mode
                runTriggers(context, mode)
                syncStates(context)
            }
        }

    override fun start(context: Context) {
        appContext = context.applicationContext
        val audioManager = context.getSystemService(AudioManager::class.java)
        currentMode = audioManager?.let { soundModeOf(it.ringerMode) }
        context.registerReceiver(receiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
    }

    override fun stop(context: Context) {
        stateEntries.closeAll().forEach { actions ->
            scope.launch(NonCancellable) {
                CombinedActionExecutor.executeAll(context, actions)
            }
        }
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
        }
        scope.coroutineContext.cancelChildren()
    }

    override fun updateAutomations(automations: List<Automation>) {
        this.automations = automations
        appContext?.let { syncStates(it) }
    }

    private fun runTriggers(
        context: Context,
        mode: Action.SoundModeType,
    ) {
        scope.launch {
            automations
                .filter {
                    it.type == Automation.Type.TRIGGER &&
                        it.isEnabled &&
                        it.trigger is Trigger.SoundModeChanged &&
                        (it.trigger as Trigger.SoundModeChanged).mode == mode
                }.forEach { automation ->
                    CombinedActionExecutor.executeAll(context, automation.actionList)
                }
        }
    }

    private fun syncStates(context: Context) =
        launchAll(context, stateEntries.sync(automations) { (it.state as? DIYState.SoundMode)?.mode == currentMode })

    private fun launchAll(
        context: Context,
        actionLists: List<List<Action>>,
    ) {
        actionLists.forEach { actions ->
            scope.launch {
                CombinedActionExecutor.executeAll(context, actions)
            }
        }
    }
}
