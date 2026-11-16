/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: External
 * File: ExternalTokenActionReceiver.kt
 * Description: Receiver for automation apps that cannot hold the external control permission, authorised by a user-set token.
 */

package com.sameerasw.essentials.external

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.sameerasw.essentials.data.repository.SettingsRepository
import java.security.MessageDigest

class ExternalTokenActionReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_TOKEN = "token"
        private const val MAX_FAILURES = 5
        private const val LOCKOUT_MS = 60_000L
        private var failures = 0
        private var lockedUntil = 0L
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val now = SystemClock.elapsedRealtime()
        synchronized(Companion) {
            if (now < lockedUntil) return
        }
        val expected = SettingsRepository(context).getExternalControlToken()
        val provided = intent.getStringExtra(EXTRA_TOKEN)
        if (expected == null || provided == null ||
            !MessageDigest.isEqual(expected.toByteArray(), provided.toByteArray())
        ) {
            synchronized(Companion) {
                if (++failures >= MAX_FAILURES) {
                    failures = 0
                    lockedUntil = now + LOCKOUT_MS
                }
            }
            Log.w("ExternalTokenReceiver", "Rejected external control request with missing or wrong token")
            return
        }
        synchronized(Companion) { failures = 0 }
        handleExternalControlIntent(context, intent)
    }
}
