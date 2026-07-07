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
        Log.d("DailyWallpaperWorker", "Executing periodic wallpaper sync")
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

                            Log.d(
                                "DailyWallpaperWorker",
                                "Successfully auto-applied wallpaper (force=$force, flags=$flags): ${todayWallpaper.id}"
                            )
                        }
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("DailyWallpaperWorker", "Error during periodic sync", e)
            Result.retry()
        }
    }
}
