/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Activities
 * File: LockscreenActionActivity.kt
 * Description: Invisible trampoline that unlocks the device before running a lock screen shortcut that opens UI.
 */

package com.sameerasw.essentials.ui.activities

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionGsonAdapter
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// Services can't dismiss the keyguard, so UI-opening actions unlock through this activity first
class LockscreenActionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        val action = intent.getStringExtra(EXTRA_ACTION_JSON)?.let { ActionGsonAdapter.fromJson(it) }
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (action == null || keyguardManager == null) {
            finish()
            return
        }

        if (!keyguardManager.isKeyguardLocked) {
            runAndFinish(action)
            return
        }

        keyguardManager.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = runAndFinish(action)

                override fun onDismissCancelled() = finish()

                override fun onDismissError() = finish()
            },
        )
    }

    // Some targets (e.g. the QR scanner) close themselves if started while the keyguard is still going away
    private fun runAndFinish(action: Action) {
        lifecycleScope.launch {
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            withTimeoutOrNull(KEYGUARD_GONE_TIMEOUT_MS) {
                while (keyguardManager?.isKeyguardLocked == true) delay(KEYGUARD_POLL_MS)
            }
            CombinedActionExecutor.execute(this@LockscreenActionActivity, action)
            finish()
        }
    }

    companion object {
        private const val EXTRA_ACTION_JSON = "action_json"
        private const val KEYGUARD_GONE_TIMEOUT_MS = 1500L
        private const val KEYGUARD_POLL_MS = 50L

        fun start(
            context: Context,
            action: Action,
        ) {
            context.startActivity(
                Intent(context, LockscreenActionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    putExtra(EXTRA_ACTION_JSON, ActionGsonAdapter.toJson(action))
                },
            )
        }
    }
}
