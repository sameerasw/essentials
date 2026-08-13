/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: NFC / Contact Picker
 * File: MeDropContactPickerHelper.kt
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import com.sameerasw.essentials.domain.model.MeDropContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MeDropContactPickerHelper {

    fun buildPickIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= 37) {
            buildNewPickIntent()
        } else {
            buildLegacyPickIntent()
        }
    }

    private fun buildNewPickIntent(): Intent {
        val requestedFields = arrayListOf(
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
        )
        return Intent("android.provider.action.PICK_CONTACTS").apply {
            putExtra("android.provider.extra.USE_SYSTEM_CONTACTS_PICKER", true)
            putStringArrayListExtra(
                "android.provider.extra.PICK_CONTACTS_REQUESTED_DATA_FIELDS",
                requestedFields
            )
        }
    }

    private fun buildLegacyPickIntent(): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

    suspend fun processResult(uri: Uri, context: Context): MeDropContact? =
        withContext(Dispatchers.IO) {
            return@withContext if (Build.VERSION.SDK_INT >= 37) {
                processSessionUri(uri, context)
            } else {
                processLegacyUri(uri, context)
            }
        }

    private fun processSessionUri(sessionUri: Uri, context: Context): MeDropContact? {
        val projection = arrayOf(
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA4
        )

        var displayName = ""
        var lookupKey = ""
        var photoUri: String? = null
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        var organization: String? = null
        var jobTitle: String? = null
        val addresses = mutableListOf<String>()
        val urls = mutableListOf<String>()
        var note: String? = null

        context.contentResolver.query(sessionUri, projection, null, null, null)?.use { cursor ->
            val lookupIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
            val data4Idx = cursor.getColumnIndex(ContactsContract.Data.DATA4)

            while (cursor.moveToNext()) {
                if (lookupKey.isEmpty() && lookupIdx != -1) {
                    cursor.getString(lookupIdx)?.let { lookupKey = it }
                }
                if (displayName.isEmpty() && nameIdx != -1) {
                    cursor.getString(nameIdx)?.let { displayName = it }
                }
                if (photoUri == null && photoIdx != -1) {
                    photoUri = cursor.getString(photoIdx)
                }

                val mime = if (mimeIdx != -1) cursor.getString(mimeIdx) ?: "" else ""
                val data1 = if (data1Idx != -1) cursor.getString(data1Idx) ?: "" else ""
                val data4 = if (data4Idx != -1) cursor.getString(data4Idx) ?: "" else ""

                when (mime) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) phones.add(data1)
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) emails.add(data1)
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) organization = data1
                        if (data4.isNotBlank()) jobTitle = data4
                    }
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) addresses.add(data1)
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) urls.add(data1)
                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) note = data1
                }
            }
        }

        if (displayName.isBlank() && lookupKey.isBlank()) return null

        return MeDropContact(
            lookupKey = lookupKey,
            displayName = displayName,
            photoUri = photoUri,
            phones = phones.distinct(),
            emails = emails.distinct(),
            organization = organization,
            jobTitle = jobTitle,
            addresses = addresses.distinct(),
            urls = urls.distinct(),
            note = note
        )
    }

    private fun processLegacyUri(contactUri: Uri, context: Context): MeDropContact? {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI
        )
        val contactId: Long
        val lookupKey: String
        val displayName: String
        val photoUri: String?

        context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            contactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)) ?: ""
            displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)) ?: ""
            photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
        } ?: return null

        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        var organization: String? = null
        var jobTitle: String? = null
        val addresses = mutableListOf<String>()
        val urls = mutableListOf<String>()
        var note: String? = null

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA4
            ),
            "${ContactsContract.Data.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
            val data4Idx = cursor.getColumnIndex(ContactsContract.Data.DATA4)

            while (cursor.moveToNext()) {
                val mime = if (mimeIdx != -1) cursor.getString(mimeIdx) ?: continue else continue
                val data1 = if (data1Idx != -1) cursor.getString(data1Idx) ?: "" else ""
                val data4 = if (data4Idx != -1) cursor.getString(data4Idx) ?: "" else ""

                when (mime) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) phones.add(data1)
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) emails.add(data1)
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) organization = data1
                        if (data4.isNotBlank()) jobTitle = data4
                    }
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) addresses.add(data1)
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) urls.add(data1)
                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) note = data1
                }
            }
        }

        return MeDropContact(
            lookupKey = lookupKey,
            displayName = displayName,
            photoUri = photoUri,
            phones = phones.distinct(),
            emails = emails.distinct(),
            organization = organization,
            jobTitle = jobTitle,
            addresses = addresses.distinct(),
            urls = urls.distinct(),
            note = note
        )
    }
}
