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

    fun buildPickIntent(): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

    suspend fun processResult(uri: Uri, context: Context): MeDropContact? =
        withContext(Dispatchers.IO) {
            processLegacyUri(uri, context)
        }

    private fun processSessionUri(sessionUri: Uri, context: Context): MeDropContact? {
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

        // Query main sessionUri first for metadata
        context.contentResolver.query(sessionUri, null, null, null, null)?.use { cursor ->
            val lookupIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            val photoThumbIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
            val data2Idx = cursor.getColumnIndex(ContactsContract.Data.DATA2)
            val data4Idx = cursor.getColumnIndex(ContactsContract.Data.DATA4)

            while (cursor.moveToNext()) {
                if (lookupKey.isEmpty() && lookupIdx != -1) {
                    cursor.getString(lookupIdx)?.let { lookupKey = it }
                }
                if (displayName.isEmpty() && nameIdx != -1) {
                    cursor.getString(nameIdx)?.let { displayName = it }
                }
                if (photoUri == null) {
                    if (photoIdx != -1 && !cursor.getString(photoIdx).isNullOrBlank()) {
                        photoUri = cursor.getString(photoIdx)
                    } else if (photoThumbIdx != -1 && !cursor.getString(photoThumbIdx).isNullOrBlank()) {
                        photoUri = cursor.getString(photoThumbIdx)
                    }
                }

                val mime = if (mimeIdx != -1) cursor.getString(mimeIdx) ?: "" else ""
                val data1 = if (data1Idx != -1) cursor.getString(data1Idx) ?: "" else ""
                val data2 = if (data2Idx != -1) cursor.getString(data2Idx) ?: "" else ""
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

        // Fallback data query if lookupKey is present to ensure all websites and photos are fetched (requires READ_CONTACTS permission)
        if (lookupKey.isNotBlank()) {
            try {
                val contactLookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
                val contactUri = ContactsContract.Contacts.lookupContact(context.contentResolver, contactLookupUri)
                if (contactUri != null) {
                    val contactId = contactUri.lastPathSegment
                    if (contactId != null) {
                        val additional = fetchContactDetails(context, contactId)
                        if (photoUri.isNullOrBlank()) photoUri = additional.photoUri
                        if (phones.isEmpty()) phones.addAll(additional.phones)
                        if (emails.isEmpty()) emails.addAll(additional.emails)
                        if (organization.isNullOrBlank()) organization = additional.organization
                        if (jobTitle.isNullOrBlank()) jobTitle = additional.jobTitle
                        if (addresses.isEmpty()) addresses.addAll(additional.addresses)
                        if (urls.isEmpty()) urls.addAll(additional.urls)
                        if (note.isNullOrBlank()) note = additional.note
                    }
                }
            } catch (_: SecurityException) {
                // If app lacks READ_CONTACTS permission, fall back safely to session URI data without crashing
            } catch (_: Exception) {}
        }

        if (displayName.isBlank() && lookupKey.isBlank()) return null

        if (!photoUri.isNullOrBlank() && photoUri!!.startsWith("content://")) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    Uri.parse(photoUri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
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

    private fun processLegacyUri(contactUri: Uri, context: Context): MeDropContact? {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
        )
        val contactId: Long
        val lookupKey: String
        val displayName: String
        var photoUri: String?

        context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            contactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)) ?: ""
            displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)) ?: ""
            photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
            if (photoUri.isNullOrBlank()) {
                photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI))
            }
        } ?: return null

        val details = try {
            fetchContactDetails(context, contactId.toString())
        } catch (_: SecurityException) {
            ExtractedDetails()
        } catch (_: Exception) {
            ExtractedDetails()
        }

        return MeDropContact(
            lookupKey = lookupKey,
            displayName = displayName,
            photoUri = photoUri ?: details.photoUri,
            phones = details.phones.distinct(),
            emails = details.emails.distinct(),
            organization = details.organization,
            jobTitle = details.jobTitle,
            addresses = details.addresses.distinct(),
            urls = details.urls.distinct(),
            note = details.note
        )
    }

    private data class ExtractedDetails(
        val photoUri: String? = null,
        val phones: List<String> = emptyList(),
        val emails: List<String> = emptyList(),
        val organization: String? = null,
        val jobTitle: String? = null,
        val addresses: List<String> = emptyList(),
        val urls: List<String> = emptyList(),
        val note: String? = null
    )

    private fun fetchContactDetails(context: Context, contactId: String): ExtractedDetails {
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        var organization: String? = null
        var jobTitle: String? = null
        val addresses = mutableListOf<String>()
        val urls = mutableListOf<String>()
        var note: String? = null
        var photoUri: String? = null

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA4
            ),
            "${ContactsContract.Data.CONTACT_ID} = ?",
            arrayOf(contactId),
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
                    ContactsContract.CommonDataKinds.Website.URL -> if (data1.isNotBlank()) urls.add(data1)
                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) note = data1
                    ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE -> if (data1.isNotBlank()) photoUri = data1
                }
            }
        }

        return ExtractedDetails(
            photoUri = photoUri,
            phones = phones,
            emails = emails,
            organization = organization,
            jobTitle = jobTitle,
            addresses = addresses,
            urls = urls,
            note = note
        )
    }
}
