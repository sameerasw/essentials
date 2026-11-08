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

    private const val STALE_MS = 2 * 60_000L

    private val lastMessageTime = object : LinkedHashMap<String, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?) = size > MAX_TRACKED
    }

    @Synchronized
    fun isUnchangedRepost(sbn: StatusBarNotification): Boolean {
        if (sbn.isOngoing) return false
        val content = contentOf(sbn.notification)
        val sameContent = lastContent.put(sbn.key, content) == content
        val messageTime = messageTimeOf(sbn.notification) ?: return sameContent
        val previous = lastMessageTime[sbn.key]
        if (previous == null || messageTime > previous) lastMessageTime[sbn.key] = messageTime
        if (sameContent) return true
        if (previous != null) return messageTime <= previous
        val now = System.currentTimeMillis()
        return now - messageTime > STALE_MS && sbn.postTime - messageTime > STALE_MS
    }

    @Synchronized
    fun forget(key: String) {
        lastContent.remove(key)
    }

    private fun messageTimeOf(n: Notification): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        val extras = n.extras
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        val isMessaging = !messages.isNullOrEmpty() || n.category == Notification.CATEGORY_MESSAGE
        if (!isMessaging) return null
        val newest = messages?.mapNotNull { (it as? Bundle)?.getLong("time")?.takeIf { t -> t > 0 } }?.maxOrNull()
        return newest ?: n.`when`.takeIf { it > 0 }
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
