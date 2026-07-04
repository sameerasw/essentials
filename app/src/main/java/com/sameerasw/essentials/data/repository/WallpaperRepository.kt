package com.sameerasw.essentials.data.repository

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.sameerasw.essentials.domain.model.WallpaperInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class WallpaperRepository {

    suspend fun fetchTodayWallpaper(): WallpaperInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://sameerasw.com/unsplash-today.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            if (connection.responseCode != 200) return@withContext null

            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            val gson = Gson()
            val rawMap = gson.fromJson(jsonText, Map::class.java) as Map<*, *>

            val mobileMap = rawMap["mobile"] as? Map<*, *> ?: rawMap

            val id = mobileMap["id"] as? String ?: ""
            val urlMobile = mobileMap["url"] as? String ?: ""
            val urlFull = mobileMap["url_full"] as? String ?: ""
            val author = mobileMap["author"] as? Map<*, *>
            val authorName = author?.get("name") as? String ?: ""
            val authorUsername = author?.get("username") as? String ?: ""
            val authorLink = author?.get("link") as? String ?: ""
            val link = mobileMap["link"] as? String ?: ""
            val updatedAt = mobileMap["updatedAt"] as? String ?: ""

            WallpaperInfo(
                id = id,
                url = rawMap["url"] as? String ?: urlMobile, // Keep landscape in url
                urlMobile = urlMobile,
                urlFull = urlFull,
                authorName = authorName,
                authorUsername = authorUsername,
                authorLink = authorLink,
                photoLink = link,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadBitmap(urlString: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            if (connection.responseCode != 200) return@withContext null
            connection.inputStream.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun applyWallpaper(context: Context, urlString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = downloadBitmap(urlString) ?: return@withContext false
            val cacheFile = File(context.cacheDir, "today_wallpaper.jpg")
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val wallpaperManager = WallpaperManager.getInstance(context)
            val intent = wallpaperManager.getCropAndSetWallpaperIntent(uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun autoApplyWallpaper(context: Context, urlString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val rawBitmap = downloadBitmap(urlString) ?: return@withContext false
            val bitmap = centerCropToScreen(context, rawBitmap)
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun centerCropToScreen(context: Context, bitmap: Bitmap): Bitmap {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        val targetRatio = screenWidth.toFloat() / screenHeight.toFloat()
        val bitmapRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()

        val cropWidth: Int
        val cropHeight: Int
        val cropX: Int
        val cropY: Int

        if (bitmapRatio > targetRatio) {
            cropHeight = bitmapHeight
            cropWidth = (bitmapHeight * targetRatio).toInt()
            cropX = (bitmapWidth - cropWidth) / 2
            cropY = 0
        } else {
            cropWidth = bitmapWidth
            cropHeight = (bitmapWidth / targetRatio).toInt()
            cropX = 0
            cropY = (bitmapHeight - cropHeight) / 2
        }

        return Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
    }
}
