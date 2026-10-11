/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: LiveUpdateOpSuppressor.kt
 * Description: Demotes live updates by toggling the per-app POST_PROMOTED_NOTIFICATIONS app op through the shell.
 */

package com.sameerasw.essentials.services

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.util.Log
import com.sameerasw.essentials.utils.ShellUtils
import java.util.concurrent.Executors

object LiveUpdateOpSuppressor {
    private const val TAG = "LiveUpdateOpSuppressor"
    private const val OP = "POST_PROMOTED_NOTIFICATIONS"
    private const val PREFS = "live_update_op_state"
    private const val REPOST_MS = 1_000L
    private const val REPOST_TRACK_MS = 3_000L

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val reposting = HashMap<String, StatusBarNotification>()

    fun isAvailable(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 36 && ShellUtils.isAvailable(context) && ShellUtils.hasPermission(context)

    @Synchronized
    fun isReposting(key: String): Boolean = key in reposting

    @Synchronized
    fun repostingNotifications(): List<StatusBarNotification> = reposting.values.toList()

    fun isPromoted(sbn: StatusBarNotification): Boolean =
        LiveUpdateSnoozer.isLiveUpdate(sbn) &&
            (sbn.notification.flags and Notification.FLAG_PROMOTED_ONGOING) != 0

    fun suppress(listener: NotificationListener) {
        val promoted =
            try {
                listener.activeNotifications.orEmpty().filter { isPromoted(it) && !isReposting(it.key) }
            } catch (_: Exception) {
                return
            }
        if (promoted.isEmpty()) return
        val context = listener.applicationContext
        executor.execute {
            promoted.groupBy { it.packageName }.forEach { (pkg, notifications) ->
                if (!isSuppressed(context, pkg)) {
                    val original = readUidMode(context, pkg)
                    saveOriginal(context, pkg, original)
                    setUidMode(context, pkg, "ignore")
                }
                notifications.forEach { repost(listener, it) }
            }
        }
    }

    fun restore(listener: NotificationListener) {
        val context = listener.applicationContext
        val saved = savedState(context)
        if (saved.isEmpty()) return
        val active =
            try {
                listener.activeNotifications.orEmpty()
            } catch (_: Exception) {
                emptyArray()
            }
        executor.execute {
            saved.forEach { (pkg, original) ->
                setUidMode(context, pkg, original)
                clearOriginal(context, pkg)
                active
                    .filter { it.packageName == pkg && LiveUpdateSnoozer.isLiveUpdate(it) && !isPromoted(it) }
                    .forEach { repost(listener, it) }
            }
        }
    }

    // The re-post is what makes the system re-evaluate promotion after the op changed.
    private fun repost(
        listener: NotificationListener,
        sbn: StatusBarNotification,
    ) {
        synchronized(this) { reposting[sbn.key] = sbn }
        try {
            listener.snoozeNotification(sbn.key, REPOST_MS)
        } catch (e: Exception) {
            Log.e(TAG, "repost failed for ${sbn.key}", e)
        }
        handler.postDelayed({ synchronized(this) { reposting.remove(sbn.key) } }, REPOST_TRACK_MS)
    }

    private fun readUidMode(
        context: Context,
        pkg: String,
    ): String {
        val output = ShellUtils.runCommandWithOutput(context, "appops get $pkg $OP", notifyOnError = false)
        val match = Regex("Uid mode: $OP: (\\w+)").find(output.orEmpty())
        return match?.groupValues?.get(1) ?: "default"
    }

    private fun setUidMode(
        context: Context,
        pkg: String,
        mode: String,
    ) {
        ShellUtils.runCommand(context, "appops set --uid $pkg $OP $mode", notifyOnError = false)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun isSuppressed(
        context: Context,
        pkg: String,
    ) = prefs(context).contains(pkg)

    private fun saveOriginal(
        context: Context,
        pkg: String,
        mode: String,
    ) = prefs(context).edit().putString(pkg, mode).apply()

    private fun clearOriginal(
        context: Context,
        pkg: String,
    ) = prefs(context).edit().remove(pkg).apply()

    private fun savedState(context: Context): Map<String, String> =
        prefs(context).all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()
}
