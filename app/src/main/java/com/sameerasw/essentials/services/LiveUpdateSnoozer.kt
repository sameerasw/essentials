package com.sameerasw.essentials.services

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.sameerasw.essentials.data.repository.SettingsRepository

object LiveUpdateSnoozer {
    private const val TAG = "LiveUpdateSnoozer"
    private const val SNOOZE_MS = 30 * 60_000L
    private const val UNSNOOZE_MS = 1_000L
    private const val POLL_MS = 1_000L
    private const val MISSES_BEFORE_REMOVED = 2

    private val mine = HashMap<String, StatusBarNotification>()
    private val misses = HashMap<String, Int>()
    private var islandVisible = false
    private var healed = false
    private val handler = Handler(Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() {
            val listener = NotificationListener.instance
            if (listener != null) synchronized(this@LiveUpdateSnoozer) { reconcile(listener) }
            synchronized(this@LiveUpdateSnoozer) {
                if (mine.isNotEmpty()) handler.postDelayed(this, POLL_MS)
            }
        }
    }

    fun isLiveUpdate(sbn: StatusBarNotification): Boolean =
        Build.VERSION.SDK_INT >= 36 &&
            sbn.notification.extras?.getBoolean("android.requestPromotedOngoing", false) == true

    @Synchronized
    fun onPosted(
        listener: NotificationListener,
        sbn: StatusBarNotification,
    ) {
        if (!isLiveUpdate(sbn)) return
        mine.remove(sbn.key)
        misses.remove(sbn.key)
        apply(listener)
    }

    @Synchronized
    fun onRemoved(
        key: String,
        reason: Int,
    ) {
        if (reason == NotificationListenerService.REASON_SNOOZED && key in mine) return
        mine.remove(key)
        misses.remove(key)
    }

    @Synchronized
    fun isSnoozedByUs(key: String): Boolean = key in mine

    @Synchronized
    fun snoozedNotifications(): List<StatusBarNotification> = mine.values.toList()

    @Synchronized
    fun onIslandVisibility(
        context: Context,
        visible: Boolean,
    ) {
        islandVisible = visible
        NotificationListener.instance?.let { apply(it) }
    }

    @Synchronized
    fun onSettingChanged() {
        NotificationListener.instance?.let { apply(it) }
    }

    @Synchronized
    fun release() {
        NotificationListener.instance?.let { unsnoozeAll(it) }
        mine.clear()
        misses.clear()
        handler.removeCallbacks(poll)
    }

    private fun apply(listener: NotificationListener) {
        if (!healed) healStale(listener)
        val enabled = SettingsRepository(listener).isIslandHideLiveUpdatesEnabled()
        if (!enabled || !islandVisible) {
            unsnoozeAll(listener)
            return
        }
        val active = try {
            listener.activeNotifications.orEmpty().filter { isLiveUpdate(it) }
        } catch (_: Exception) {
            emptyList()
        }
        active.filter { it.key !in mine }.forEach { sbn ->
            try {
                listener.snoozeNotification(sbn.key, SNOOZE_MS)
                mine[sbn.key] = sbn
                misses.remove(sbn.key)
                Log.d(TAG, "snoozed ${sbn.key}")
            } catch (e: Exception) {
                Log.e(TAG, "snooze failed for ${sbn.key}", e)
            }
        }
        handler.removeCallbacks(poll)
        if (mine.isNotEmpty()) handler.postDelayed(poll, POLL_MS)
    }

    private fun reconcile(listener: NotificationListener) {
        if (mine.isEmpty()) return
        val snoozedNow = try {
            listener.snoozedNotifications.associateBy { it.key }
        } catch (_: Exception) {
            return
        }
        val activeKeys = try {
            listener.activeNotifications.orEmpty().map { it.key }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
        mine.toList().forEach { (key, last) ->
            val current = snoozedNow[key]
            when {
                current != null -> {
                    misses.remove(key)
                    if (current.postTime != last.postTime) {
                        mine[key] = current
                        listener.feedSnoozedPosted(current)
                    }
                }
                key in activeKeys -> misses.remove(key)
                else -> {
                    val count = (misses[key] ?: 0) + 1
                    if (count >= MISSES_BEFORE_REMOVED) {
                        mine.remove(key)
                        misses.remove(key)
                        listener.feedSnoozedRemoved(last)
                    } else {
                        misses[key] = count
                    }
                }
            }
        }
    }

    private fun unsnoozeAll(listener: NotificationListener) {
        handler.removeCallbacks(poll)
        if (mine.isEmpty()) return
        forgetVanished(listener)
        val keys = mine.keys.toList()
        mine.clear()
        misses.clear()
        keys.forEach { key ->
            try {
                listener.snoozeNotification(key, UNSNOOZE_MS)
                Log.d(TAG, "unsnoozed $key")
            } catch (e: Exception) {
                Log.e(TAG, "unsnooze failed for $key", e)
            }
        }
    }

    private fun forgetVanished(listener: NotificationListener) {
        val snoozedKeys = try {
            listener.snoozedNotifications.map { it.key }.toSet()
        } catch (_: Exception) {
            return
        }
        val activeKeys = try {
            listener.activeNotifications.orEmpty().map { it.key }.toSet()
        } catch (_: Exception) {
            return
        }
        mine.toList().forEach { (key, last) ->
            if (key !in snoozedKeys && key !in activeKeys) listener.feedSnoozedRemoved(last)
        }
    }

    private fun healStale(listener: NotificationListener) {
        healed = true
        try {
            listener.snoozedNotifications
                .filter { isLiveUpdate(it) && it.key !in mine }
                .forEach { listener.snoozeNotification(it.key, UNSNOOZE_MS) }
        } catch (_: Exception) {
        }
    }
}
