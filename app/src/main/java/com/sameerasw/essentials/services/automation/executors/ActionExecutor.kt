package com.sameerasw.essentials.services.automation.executors

import android.content.Context
import com.sameerasw.essentials.domain.diy.Action

interface ActionExecutor {
    suspend fun execute(context: Context, action: Action)
}
