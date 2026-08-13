/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: NFC / HCE
 * File: MeDropHceService.kt
 */

package com.sameerasw.essentials.services

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.MeDropContact
import com.google.gson.Gson

class MeDropHceService : HostApduService() {

    companion object {
        // SELECT AID command prefix
        private val SELECT_AID_PREFIX = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)
        // Standard NFC NDEF AID: D2 76 00 00 85 01 01
        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01
        )

        // APDU status words
        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_UNKNOWN_CMD = byteArrayOf(0x00, 0x00)
        private val SW_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        // File IDs
        private val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03)
        private val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04)

        // Capability Container (CC) content for Type 4 Tag
        private val CC_CONTENT = byteArrayOf(
            0x00, 0x0F, // CCLEN
            0x20, // Mapping Version 2.0
            0x00, 0x7F, // MLe (max R-APDU)
            0x00, 0x7F, // MLc (max C-APDU)
            0x04, 0x06, // NDEF File TLV
            0xE1.toByte(), 0x04, // NDEF File ID
            0x04, 0x00, // Max NDEF size (1024 bytes)
            0x00, // Read Access (granted)
            0x00 // Write Access (none)
        )

        // FCI for NDEF AID selection response
        private val NDEF_AID_FCI = byteArrayOf(
            0x6F.toByte(), 0x10, // FCI Template
            0x84.toByte(), 0x07, // DF Name
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01, // AID
            0xA5.toByte(), 0x05, // FCI Prop. Data
            0x50.toByte(), 0x03, // Label
            0x4E, 0x46, 0x43 // "NFC"
        )

        // READ BINARY command prefix
        private val READ_BINARY_CMD_PREFIX = byteArrayOf(0x00, 0xB0.toByte())

        var pendingVCardBytes: ByteArray? = null

        private fun ndefWrap(payload: ByteArray): ByteArray {
            val record = NdefRecord.createMime("text/vcard", payload)
            val message = NdefMessage(record)
            val ndefData = message.toByteArray()
            
            // Type 4 Tag NDEF file format: [2 bytes Length] [NDEF Message]
            val nlen = byteArrayOf((ndefData.size shr 8).toByte(), (ndefData.size and 0xFF).toByte())
            return nlen + ndefData
        }

        fun prepareVCard(context: android.content.Context, contact: MeDropContact) {
            val vcardString = contact.toVCard(context)
            pendingVCardBytes = ndefWrap(vcardString.toByteArray(Charsets.UTF_8))
        }

        fun prepareVCard(contact: MeDropContact) {
            val vcardString = contact.toVCard(null)
            pendingVCardBytes = ndefWrap(vcardString.toByteArray(Charsets.UTF_8))
        }

        fun clearVCard() {
            pendingVCardBytes = null
        }
    }

    private var selectedFile: ByteArray? = null

    override fun onCreate() {
        super.onCreate()
        val json = SettingsRepository(this).getMeDropContactJson()
        if (json != null) {
            try {
                val contact = Gson().fromJson(json, MeDropContact::class.java)
                prepareVCard(this, contact)
            } catch (_: Exception) {}
        }
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (commandApdu.size < 4) return SW_UNKNOWN_CMD

        return when {
            isSelectAidCommand(commandApdu) -> {
                selectedFile = null
                NDEF_AID_FCI + SW_OK
            }
            isSelectFileCommand(commandApdu) -> {
                if (commandApdu.size < 7) return SW_FILE_NOT_FOUND
                val fileId = commandApdu.copyOfRange(5, 7)
                when {
                    fileId.contentEquals(CC_FILE_ID) -> {
                        selectedFile = CC_CONTENT
                        SW_OK
                    }
                    fileId.contentEquals(NDEF_FILE_ID) -> {
                        selectedFile = pendingVCardBytes
                        SW_OK
                    }
                    else -> SW_FILE_NOT_FOUND
                }
            }
            isReadBinaryCommand(commandApdu) -> {
                val data = selectedFile ?: return SW_FILE_NOT_FOUND
                val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
                
                // Le is at offset 4
                if (commandApdu.size < 5) return SW_UNKNOWN_CMD
                val length = commandApdu[4].toInt() and 0xFF
                
                if (offset >= data.size) return SW_FILE_NOT_FOUND
                val end = minOf(offset + length, data.size)
                data.copyOfRange(offset, end) + SW_OK
            }
            else -> SW_UNKNOWN_CMD
        }
    }

    override fun onDeactivated(reason: Int) {
        selectedFile = null
    }

    private fun isSelectAidCommand(apdu: ByteArray): Boolean {
        if (apdu.size < SELECT_AID_PREFIX.size + NDEF_AID.size + 1) return false
        for (i in SELECT_AID_PREFIX.indices) {
            if (apdu[i] != SELECT_AID_PREFIX[i]) return false
        }
        val aidLen = apdu[4].toInt() and 0xFF
        if (aidLen != NDEF_AID.size) return false
        for (i in NDEF_AID.indices) {
            if (apdu[5 + i] != NDEF_AID[i]) return false
        }
        return true
    }

    private fun isSelectFileCommand(apdu: ByteArray): Boolean {
        // SELECT FILE command: 00 A4 00 0C 02 [File ID]
        // Some readers might use P2=00 instead of 0C
        return apdu.size >= 7 && apdu[0] == 0x00.toByte() && apdu[1] == 0xA4.toByte() && 
               apdu[2] == 0x00.toByte() && (apdu[3] == 0x0C.toByte() || apdu[3] == 0x00.toByte()) && 
               apdu[4] == 0x02.toByte()
    }

    private fun isReadBinaryCommand(apdu: ByteArray): Boolean =
        apdu.size >= 5 && apdu[0] == READ_BINARY_CMD_PREFIX[0] && apdu[1] == READ_BINARY_CMD_PREFIX[1]
}
