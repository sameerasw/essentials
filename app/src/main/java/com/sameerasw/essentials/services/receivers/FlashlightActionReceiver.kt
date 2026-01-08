package com.sameerasw.essentials.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class FlashlightActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_INCREASE = "com.sameerasw.essentials.ACTION_FLASHLIGHT_INCREASE"
        const val ACTION_DECREASE = "com.sameerasw.essentials.ACTION_FLASHLIGHT_DECREASE"
        const val ACTION_OFF = "com.sameerasw.essentials.ACTION_FLASHLIGHT_OFF"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("FlashlightAction", "Action received: $action")
        
        val serviceIntent = Intent(context, com.sameerasw.essentials.services.ScreenOffAccessibilityService::class.java).apply {
            this.action = action
        }
        context.startService(serviceIntent)
    }
}
