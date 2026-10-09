/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: StackHostWorker.kt
 * Description: Restarts widget stack hosting if the system stopped the app.
 */

package com.sameerasw.essentials.services.widgets

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class StackHostWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        StackHost.startIfNeeded(applicationContext)
        return Result.success()
    }
}
