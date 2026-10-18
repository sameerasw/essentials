/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: LocationAutomationWorker.kt
 * Description: Runs the actions of arrive and leave location automations after a geofence transition.
 */

package com.sameerasw.essentials.services.automation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.Geofence
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor

class LocationAutomationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ids = inputData.getStringArray(KEY_AUTOMATION_IDS)?.toSet() ?: return Result.success()
        val transition = inputData.getInt(KEY_TRANSITION, -1)

        DIYRepository.init(applicationContext)
        DIYRepository.automations.value
            .filter { automation ->
                automation.isEnabled &&
                    automation.type == Automation.Type.TRIGGER &&
                    automation.id in ids &&
                    when (automation.trigger) {
                        is Trigger.ArriveAtLocation -> transition == Geofence.GEOFENCE_TRANSITION_ENTER
                        is Trigger.LeaveLocation -> transition == Geofence.GEOFENCE_TRANSITION_EXIT
                        else -> false
                    }
            }.forEach { CombinedActionExecutor.executeAll(applicationContext, it.actionList) }
        return Result.success()
    }

    companion object {
        const val KEY_AUTOMATION_IDS = "automation_ids"
        const val KEY_TRANSITION = "transition"
    }
}
