package com.sameerasw.essentials.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.data.repository.WallpaperRepository
import java.time.LocalDateTime

class DailyWallpaperWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("DailyWallpaperWorker", "Executing daily wallpaper sync")
        val context = applicationContext
        val settingsRepository = SettingsRepository(context)
        val wallpaperRepository = WallpaperRepository()

        return try {
            val isEnabled = settingsRepository.getBoolean(
                SettingsRepository.KEY_DAILY_WALLPAPER_AUTO_UPDATE,
                false
            )
            if (!isEnabled) {
                return Result.success()
            }

            var newWallpaperApplied = false
            val force = inputData.getBoolean("force", false)
            val todayWallpaper = wallpaperRepository.fetchTodayWallpaper()
            if (todayWallpaper != null) {
                val cachedId =
                    settingsRepository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_LAST_ID)
                        ?: ""

                // If it is a new wallpaper (or forced run), fetch and apply
                if (force || todayWallpaper.id != cachedId) {
                    val applyHome = settingsRepository.getDailyWallpaperApplyHome()
                    val applyLock = settingsRepository.getDailyWallpaperApplyLock()
                    var flags = 0
                    if (applyHome) flags = flags or android.app.WallpaperManager.FLAG_SYSTEM
                    if (applyLock) flags = flags or android.app.WallpaperManager.FLAG_LOCK

                    if (flags != 0) {
                        val applied = wallpaperRepository.autoApplyWallpaper(
                            context,
                            todayWallpaper.urlMobile,
                            flags
                        )
                        if (applied) {
                            newWallpaperApplied = true
                            settingsRepository.putString(
                                SettingsRepository.KEY_DAILY_WALLPAPER_LAST_ID,
                                todayWallpaper.id
                            )
                            settingsRepository.putString(
                                SettingsRepository.KEY_DAILY_WALLPAPER_LAST_URL,
                                todayWallpaper.url
                            )
                            settingsRepository.putString(
                                SettingsRepository.KEY_DAILY_WALLPAPER_LAST_URL_MOBILE,
                                todayWallpaper.urlMobile
                            )
                            settingsRepository.putString(
                                SettingsRepository.KEY_DAILY_WALLPAPER_AUTHOR_NAME,
                                todayWallpaper.authorName
                            )
                            settingsRepository.putString(
                                SettingsRepository.KEY_DAILY_WALLPAPER_AUTHOR_LINK,
                                todayWallpaper.authorLink
                            )
                            settingsRepository.putString(
                                SettingsRepository.KEY_DAILY_WALLPAPER_PHOTO_LINK,
                                todayWallpaper.photoLink
                            )
                            settingsRepository.putString(
                                SettingsRepository.KEY_DAILY_WALLPAPER_UPDATED_AT,
                                todayWallpaper.updatedAt
                            )

                            val currentTime = LocalDateTime.now().toString()
                            settingsRepository.putString(
                                SettingsRepository.KEY_DAILY_WALLPAPER_AUTO_UPDATE_TIME,
                                currentTime
                            )
                            settingsRepository.putInt(
                                SettingsRepository.KEY_DAILY_WALLPAPER_RETRY_COUNT,
                                0
                            )

                            Log.d(
                                "DailyWallpaperWorker",
                                "Successfully auto-applied wallpaper (force=$force, flags=$flags): ${todayWallpaper.id}"
                            )
                        }
                    }
                }
            }

            if (!newWallpaperApplied && !force) {
                val retryCount = settingsRepository.getInt(SettingsRepository.KEY_DAILY_WALLPAPER_RETRY_COUNT, 0)
                if (retryCount < 2) {
                    settingsRepository.putInt(SettingsRepository.KEY_DAILY_WALLPAPER_RETRY_COUNT, retryCount + 1)
                    val retryWork = androidx.work.OneTimeWorkRequestBuilder<DailyWallpaperWorker>()
                        .setInitialDelay(30, java.util.concurrent.TimeUnit.MINUTES)
                        .build()
                    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                        "daily_wallpaper_retry_work",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        retryWork
                    )
                    Log.d("DailyWallpaperWorker", "No new wallpaper found. Scheduled retry #${retryCount + 1} in 30 mins")
                } else {
                    settingsRepository.putInt(SettingsRepository.KEY_DAILY_WALLPAPER_RETRY_COUNT, 0)
                    Log.d("DailyWallpaperWorker", "Reached max retries (3 total checks). Waiting for next daily cycle.")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("DailyWallpaperWorker", "Error during daily sync", e)
            Result.retry()
        }
    }
}
