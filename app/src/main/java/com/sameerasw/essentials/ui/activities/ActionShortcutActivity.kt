/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: ActionShortcutActivity.kt
 * Description: Activity component for ActionShortcutActivity.kt.
 */

package com.sameerasw.essentials.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.launch

class ActionShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DIYRepository.init(applicationContext)
        val automation =
            DIYRepository.automations.value.find { it.type == Automation.Type.ACTION_SHORTCUT }

        if (automation != null && automation.actionList.isNotEmpty()) {
            lifecycleScope.launch {
                CombinedActionExecutor.executeAll(applicationContext, automation.actionList)
                finish()
            }
        } else {
            Toast
                .makeText(this, "No customized action shortcut configured", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }
}
