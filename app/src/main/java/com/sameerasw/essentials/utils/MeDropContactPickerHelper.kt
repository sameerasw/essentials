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
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
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
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1
        )

        val contactsMap = mutableMapOf<String, Triple<String, MutableList<String>, MutableList<String>>>()

        context.contentResolver.query(sessionUri, projection, null, null, null)?.use { cursor ->
            val lookupIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)

            while (cursor.moveToNext()) {
                val key = cursor.getString(lookupIdx) ?: continue
                val name = cursor.getString(nameIdx) ?: ""
                val mime = cursor.getString(mimeIdx) ?: ""
                val data1 = cursor.getString(data1Idx) ?: ""

                val entry = contactsMap.getOrPut(key) { Triple(name, mutableListOf(), mutableListOf()) }
                when (mime) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> entry.second.add(data1)
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> entry.third.add(data1)
                }
            }
        }

        return contactsMap.entries.firstOrNull()?.let { (key, data) ->
            MeDropContact(
                lookupKey = key,
                displayName = data.first,
                phones = data.second.distinct(),
                emails = data.third.distinct()
            )
        }
    }

    private fun processLegacyUri(contactUri: Uri, context: Context): MeDropContact? {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )
        val contactId: Long
        val lookupKey: String
        val displayName: String

        context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            contactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)) ?: ""
            displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)) ?: ""
        } ?: return null

        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.MIMETYPE, ContactsContract.Data.DATA1),
            "${ContactsContract.Data.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeIdx) ?: continue
                val data = cursor.getString(data1Idx) ?: continue
                when (mime) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> phones.add(data)
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> emails.add(data)
                }
            }
        }

        return MeDropContact(
            lookupKey = lookupKey,
            displayName = displayName,
            phones = phones.distinct(),
            emails = emails.distinct()
        )
    }
}
