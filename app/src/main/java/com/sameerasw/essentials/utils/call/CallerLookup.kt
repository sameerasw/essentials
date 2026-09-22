package com.sameerasw.essentials.utils.call

import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log


object CallerLookup {
    private const val TAG = "CallerLookup"

    private fun canRead(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun name(context: Context, number: String?): String? {
        if (number.isNullOrBlank() || !canRead(context)) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val idx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (idx != -1) cursor.getString(idx) else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact name", e)
            null
        }
    }

    fun photo(context: Context, number: String?): Bitmap? {
        if (number.isNullOrBlank() || !canRead(context)) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val contactId = context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val idx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                if (idx != -1) cursor.getLong(idx) else null
            } ?: return null
            val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, contactUri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact photo", e)
            null
        }
    }
}
