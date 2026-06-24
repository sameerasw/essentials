package com.sameerasw.essentials.external

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle

class ExternalControlProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    @Suppress("DEPRECATION")
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val context = context ?: return null
        val path = uri.path ?: return null
        val result = ExternalRouter.query(context, path, null) ?: return null

        val cursor = MatrixCursor(arrayOf("key", "value", "type"))
        val key = path.substringAfterLast('/')
        val value = result.get("value")
        val type = result.getString("type", "")
        cursor.addRow(arrayOf(key, value, type))
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return "vnd.android.cursor.item/vnd.com.sameerasw.essentials.external"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val context = context ?: return 0
        val path = uri.path ?: return 0
        val value = values?.getAsString("value") ?: return 0
        val success = ExternalRouter.update(context, path, value, null)
        return if (success) 1 else 0
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        if (method == "action") {
            val path = arg ?: return null
            val action = extras?.getString("action")
            return ExternalRouter.action(context, path, action, extras)
        }
        return null
    }
}
