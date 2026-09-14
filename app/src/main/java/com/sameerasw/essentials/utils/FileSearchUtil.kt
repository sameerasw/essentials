/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: FileSearchUtil.kt
 * Description: Fast indexed universal file and media search utility utilizing MediaStore.
 */

package com.sameerasw.essentials.utils

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.PixelSearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileSearchUtil {

    data class FileSearchResults(
        val mediaItems: List<PixelSearchResultItem.FileItem> = emptyList(),
        val documentItems: List<PixelSearchResultItem.FileItem> = emptyList(),
    )

    suspend fun searchFiles(
        context: Context,
        query: String,
        limit: Int = 16,
    ): FileSearchResults = withContext(Dispatchers.IO) {
        val trimmed = query.trim()

        val mediaList = mutableListOf<PixelSearchResultItem.FileItem>()
        val docList = mutableListOf<PixelSearchResultItem.FileItem>()
        val resolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.DATA,
        )

        // Exclude 0-byte files and hidden dot files
        val selection = if (trimmed.isNotEmpty()) {
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? AND " +
                "${MediaStore.Files.FileColumns.SIZE} > 0 AND " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} NOT LIKE '.%'"
        } else {
            "${MediaStore.Files.FileColumns.SIZE} > 0 AND " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} NOT LIKE '.%'"
        }
        val selectionArgs = if (trimmed.isNotEmpty()) arrayOf("%$trimmed%") else null
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        val queryUri = MediaStore.Files.getContentUri("external")

        try {
            resolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mediaTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

                while (cursor.moveToNext() && (mediaList.size + docList.size < 100)) {
                    val name = cursor.getString(nameCol) ?: continue
                    if (name.startsWith(".") || name.contains(".thumbnails", ignoreCase = true)) continue

                    val id = cursor.getLong(idCol)
                    val mime = cursor.getString(mimeCol) ?: getMimeTypeFromExtension(name)
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol)
                    val mediaType = cursor.getInt(mediaTypeCol)
                    val path = if (dataCol != -1) cursor.getString(dataCol) else null

                    val isImage = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE || mime.startsWith("image/", ignoreCase = true)
                    val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO || mime.startsWith("video/", ignoreCase = true)
                    val isGif = mime.equals("image/gif", ignoreCase = true) || name.endsWith(".gif", ignoreCase = true)
                    val isMedia = isImage || isVideo || isGif

                    val contentUri = if (isImage) {
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    } else if (isVideo) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(queryUri, id)
                    }

                    val iconRes = getIconForMimeType(mime, name)

                    val item = PixelSearchResultItem.FileItem(
                        id = id,
                        uri = contentUri,
                        displayName = name,
                        mimeType = mime,
                        sizeBytes = size,
                        dateModified = date,
                        isMedia = isMedia,
                        isGif = isGif,
                        isVideo = isVideo,
                        path = path,
                        iconRes = iconRes,
                    )

                    if (isMedia) {
                        mediaList.add(item)
                    } else {
                        docList.add(item)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FileSearchUtil", "Error querying MediaStore", e)
        }

        fun score(item: PixelSearchResultItem.FileItem): Int {
            if (trimmed.isEmpty()) return 0
            val stem = item.displayName.substringBeforeLast(".")
            return when {
                stem.equals(trimmed, ignoreCase = true) -> 0
                stem.startsWith(trimmed, ignoreCase = true) -> 1
                item.displayName.startsWith(trimmed, ignoreCase = true) -> 2
                else -> 3
            }
        }

        val sortedMedia = mediaList.sortedWith(
            compareBy<PixelSearchResultItem.FileItem> { score(it) }.thenByDescending { it.dateModified },
        ).take(limit)

        val sortedDocs = docList.sortedWith(
            compareBy<PixelSearchResultItem.FileItem> { score(it) }.thenByDescending { it.dateModified },
        ).take(limit)

        FileSearchResults(
            mediaItems = sortedMedia,
            documentItems = sortedDocs,
        )
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val formatted = bytes / Math.pow(1024.0, index.toDouble())
        return if (index == 0) "$bytes B" else java.text.DecimalFormat("#,##0.0").format(formatted) + " " + units[index]
    }

    private fun getMimeTypeFromExtension(name: String): String {
        val ext = name.substringAfterLast(".", "").lowercase()
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: when (ext) {
                "apk" -> "application/vnd.android.package-archive"
                "pdf" -> "application/pdf"
                "zip" -> "application/zip"
                "rar" -> "application/x-rar-compressed"
                "7z" -> "application/x-7z-compressed"
                "json" -> "application/json"
                "txt" -> "text/plain"
                else -> "application/octet-stream"
            }
    }

    private fun getIconForMimeType(mime: String, name: String): Int {
        val ext = name.substringAfterLast(".", "").lowercase()
        return when {
            ext == "apk" || mime == "application/vnd.android.package-archive" -> R.drawable.rounded_android_24
            mime.startsWith("audio/") -> R.drawable.rounded_music_note_24
            mime.startsWith("image/") -> R.drawable.rounded_image_24
            mime.startsWith("video/") -> R.drawable.rounded_music_video_24
            else -> R.drawable.rounded_description_24
        }
    }
}
