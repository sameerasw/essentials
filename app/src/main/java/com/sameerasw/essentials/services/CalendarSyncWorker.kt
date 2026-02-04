package com.sameerasw.essentials.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CalendarSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("CalendarSyncWorker", "Executing periodic calendar sync")
        return try {
            CalendarSyncManager.forceSync(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e("CalendarSyncWorker", "Error during periodic sync", e)
            Result.retry()
        }
    }
}
