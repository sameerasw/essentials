package com.sameerasw.essentials.utils.notification

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification

object NotificationRepostFilter {
    private const val MAX_TRACKED = 500
    private val SEPARATOR = Char(0).toString()

    private val lastContent = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > MAX_TRACKED
    }

    @Synchronized
    fun isUnchangedRepost(sbn: StatusBarNotification): Boolean {
        if (sbn.isOngoing) return false
        val content = contentOf(sbn.notification)
        return lastContent.put(sbn.key, content) == content
    }

    @Synchronized
    fun forget(key: String) {
        lastContent.remove(key)
    }

    private fun contentOf(n: Notification): String {
        val extras = n.extras
        val lastMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.lastOrNull()?.let { (it as? Bundle)?.getCharSequence("text") }
        } else {
            null
        }
        return listOf(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            lastMessage,
        ).joinToString(SEPARATOR) { it?.toString().orEmpty() }
    }
}
