package com.sameerasw.essentials.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sameerasw.essentials.data.repository.SettingsRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object AutoBackupManager {
    const val MODE_NEVER = "never"
    const val MODE_UPDATING = "updating"
    const val MODE_DAILY = "daily"
    const val MODE_WEEKLY = "weekly"
    const val KEY_MODE = "auto_backup_mode"
    const val KEY_FOLDER = "auto_backup_folder_uri"
    const val DEFAULT_FOLDER = "essentials-backups"
    private const val WORK_NAME = "auto_backup_work"

    fun getMode(context: Context): String = SettingsRepository(context).getString(KEY_MODE, MODE_NEVER) ?: MODE_NEVER

    fun setMode(context: Context, mode: String) {
        SettingsRepository(context).putString(KEY_MODE, mode)
        schedule(context)
    }

    fun getFolder(context: Context): Uri? =
        SettingsRepository(context).getString(KEY_FOLDER, null)?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

    fun setFolder(context: Context, uri: Uri?) {
        SettingsRepository(context).putString(KEY_FOLDER, uri?.toString() ?: "")
    }

    fun needsLegacyStoragePermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun schedule(context: Context) {
        val days =
            when (getMode(context)) {
                MODE_DAILY -> 1L
                MODE_WEEKLY -> 7L
                else -> {
                    WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                    return
                }
            }
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<AutoBackupWorker>(days, TimeUnit.DAYS).build(),
        )
    }

    data class BackupEntry(
        val uri: Uri,
        val name: String,
        val modified: Long,
    )

    private const val BACKUP_PREFIX = "essentials-config-"

    fun canListBackups(context: Context): Boolean =
        getMode(context) != MODE_NEVER &&
            (!needsLegacyStoragePermission() ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED)

    fun listBackups(context: Context, limit: Int = 10): List<BackupEntry> =
        try {
            val folder = getFolder(context)
            val all =
                when {
                    folder != null -> listTree(context, folder)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> listDownloads(context)
                    else -> listLegacy()
                }
            all.sortedByDescending { it.modified }.take(limit)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

    private fun listTree(context: Context, tree: Uri): List<BackupEntry> {
        val children =
            DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val result = mutableListOf<BackupEntry>()
        context.contentResolver
            .query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                ),
                null,
                null,
                null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1) ?: continue
                    if (!name.startsWith(BACKUP_PREFIX)) continue
                    result += BackupEntry(DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)), name, c.getLong(2))
                }
            }
        return result
    }

    private fun listDownloads(context: Context): List<BackupEntry> {
        val result = mutableListOf<BackupEntry>()
        context.contentResolver
            .query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                ),
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                arrayOf("${Environment.DIRECTORY_DOWNLOADS}/$DEFAULT_FOLDER%", "$BACKUP_PREFIX%"),
                null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val uri = android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0))
                    result += BackupEntry(uri, c.getString(1), c.getLong(2) * 1000L)
                }
            }
        return result
    }

    private fun listLegacy(): List<BackupEntry> {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DEFAULT_FOLDER)
        return dir
            .listFiles { f -> f.name.startsWith(BACKUP_PREFIX) }
            ?.map { BackupEntry(Uri.fromFile(it), it.name, it.lastModified()) }
            .orEmpty()
    }

    fun backupBeforeUpdate(context: Context) {
        if (getMode(context) == MODE_UPDATING) runBackup(context)
    }

    fun runBackup(context: Context): Boolean =
        try {
            val bytes = ByteArrayOutputStream().also { SettingsRepository(context).exportConfigs(it) }.toByteArray()
            val name = "essentials-config-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".json"
            val folder = getFolder(context)
            if (folder != null) writeToTree(context, folder, name, bytes) else writeToDownloads(context, name, bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    private fun writeToTree(context: Context, tree: Uri, name: String, bytes: ByteArray): Boolean {
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val file = DocumentsContract.createDocument(context.contentResolver, parent, "application/json", name) ?: return false
        context.contentResolver.openOutputStream(file)?.use { it.write(bytes) } ?: return false
        return true
    }

    private fun writeToDownloads(context: Context, name: String, bytes: ByteArray): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DEFAULT_FOLDER")
                }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
            return true
        }
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DEFAULT_FOLDER)
        if (!dir.exists() && !dir.mkdirs()) return false
        File(dir, name).writeBytes(bytes)
        return true
    }
}

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        AutoBackupManager.runBackup(applicationContext)
        return Result.success()
    }
}
