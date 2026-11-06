/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: BatteryWidgetJobService.kt
 * Description: Background job service that updates the battery widget on charging state transitions.
 */

package com.sameerasw.essentials.services.widgets

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryWidgetJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BatteriesWidgetReceiver.updateWidgets(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BatteriesWidgetReceiver.updateWidgets(applicationContext)
            } finally {
                schedule(applicationContext)
            }
        }
        return false
    }

    companion object {
        const val JOB_ID = 8823

        fun schedule(context: Context) {
            try {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                val component = ComponentName(context, BatteryWidgetJobService::class.java)
                val builder = JobInfo.Builder(JOB_ID, component)
                    .setRequiresCharging(true)
                    .setPersisted(true)
                scheduler.schedule(builder.build())
            } catch (e: Exception) {
                android.util.Log.e("BatteryWidgetJob", "Failed to schedule battery charging job", e)
            }
        }
    }
}
