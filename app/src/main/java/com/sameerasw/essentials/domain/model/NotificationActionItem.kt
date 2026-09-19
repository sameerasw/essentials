/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Models
 * File: NotificationActionItem.kt
 * Description: Data class representing an interactive notification action (button or inline reply).
 */

package com.sameerasw.essentials.domain.model

import android.app.PendingIntent
import android.app.RemoteInput

data class NotificationActionItem(
    val title: String,
    val isQuickReply: Boolean = false,
    val pendingIntent: PendingIntent? = null,
    val remoteInputs: Array<RemoteInput>? = null,
    val actionKey: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationActionItem

        if (title != other.title) return false
        if (isQuickReply != other.isQuickReply) return false
        if (pendingIntent != other.pendingIntent) return false
        if (remoteInputs != null) {
            if (other.remoteInputs == null) return false
            if (!remoteInputs.contentEquals(other.remoteInputs)) return false
        } else if (other.remoteInputs != null) return false
        if (actionKey != other.actionKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + isQuickReply.hashCode()
        result = 31 * result + (pendingIntent?.hashCode() ?: 0)
        result = 31 * result + (remoteInputs?.contentHashCode() ?: 0)
        result = 31 * result + actionKey.hashCode()
        return result
    }
}
