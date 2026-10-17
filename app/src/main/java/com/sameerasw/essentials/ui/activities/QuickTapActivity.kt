/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: QuickTapActivity.kt
 * Description: Activity component for QuickTapActivity.kt.
 */

package com.sameerasw.essentials.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.launch

class QuickTapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepository = SettingsRepository(this)
        val actions = settingsRepository.getRemapActions(SettingsRepository.KEY_QUICK_TAP_ACTIONS)

        if (settingsRepository.isQuickTapEnabled() && actions.isNotEmpty()) {
            lifecycleScope.launch {
                CombinedActionExecutor.executeAll(applicationContext, actions)
                finish()
            }
        } else {
            Toast.makeText(this, R.string.quick_tap_not_set_up, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
