package com.sameerasw.essentials.utils

import android.content.Context
import com.google.gson.Gson
import com.sameerasw.essentials.data.model.DeviceSpecs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Utility for caching device specifications and images locally to avoid redundant network requests.
 */
object DeviceSpecsCache {
    private const val SPECS_FILE = "device_specs_cache.json"
    private const val IMAGES_DIR = "device_info_images"
    private val gson = Gson()

    /**
     * Retrieves the cached device specifications if available.
     */
    fun getCachedSpecs(context: Context): DeviceSpecs? {
        return try {
            val file = File(context.filesDir, SPECS_FILE)
            if (!file.exists()) return null
            val json = file.readText()
            gson.fromJson(json, DeviceSpecs::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves the device specifications to local storage.
     */
    fun saveSpecs(context: Context, specs: DeviceSpecs) {
        try {
            val json = gson.toJson(specs)
            File(context.filesDir, SPECS_FILE).writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clears all cached device data.
     */
    fun clearCache(context: Context) {
        try {
            File(context.filesDir, SPECS_FILE).delete()
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (imagesDir.exists()) {
                imagesDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Downloads and saves device images locally.
     * Returns a copy of [specs] with the [DeviceSpecs.localImagePaths] populated.
     */
    suspend fun downloadImages(context: Context, specs: DeviceSpecs): DeviceSpecs = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, IMAGES_DIR)
        if (!dir.exists()) dir.mkdirs()

        val localPaths = mutableListOf<String>()
        specs.imageUrls.forEachIndexed { index, url ->
            try {
                // Use a stable filename based on index and extension
                val extension = url.substringAfterLast(".", "jpg").split("?").first()
                val fileName = "device_image_${index}.${extension}"
                val file = File(dir, fileName)
                
                // Only download if it doesn't already exist or if it's the first image (often updated)
                if (!file.exists()) {
                    URL(url).openStream().use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                localPaths.add(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                // If download fails, we don't add to localPaths, 
                // UI will fall back to imageUrls
            }
        }
        
        val updatedSpecs = specs.copy(localImagePaths = localPaths)
        saveSpecs(context, updatedSpecs)
        updatedSpecs
    }
}
