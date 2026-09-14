/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: PixelSearchbarTapActivity.kt
 * Description: Activity component for PixelSearchbarTapActivity.kt.
 */

package com.sameerasw.essentials.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.launch

class PixelSearchbarTapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DIYRepository.init(applicationContext)
        val automation =
            DIYRepository.automations.value.find { it.type == Automation.Type.PIXEL_SEARCHBAR }

        if (automation != null && automation.actions.isNotEmpty() && automation.isEnabled) {
            lifecycleScope.launch {
                automation.actions.forEach { action ->
                    CombinedActionExecutor.execute(applicationContext, action)
                }
                finish()
            }
        } else {
            val resultsIntent =
                android.content.Intent(this, PixelSearchResultsActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            startActivity(resultsIntent)
            finish()
        }
    }
}
