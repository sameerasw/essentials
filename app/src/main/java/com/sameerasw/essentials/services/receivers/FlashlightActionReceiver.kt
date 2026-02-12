package com.sameerasw.essentials.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService

class FlashlightActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_INCREASE = "com.sameerasw.essentials.ACTION_FLASHLIGHT_INCREASE"
        const val ACTION_DECREASE = "com.sameerasw.essentials.ACTION_FLASHLIGHT_DECREASE"
        const val ACTION_OFF = "com.sameerasw.essentials.ACTION_FLASHLIGHT_OFF"
        const val ACTION_TOGGLE = "com.sameerasw.essentials.ACTION_FLASHLIGHT_TOGGLE"
        const val ACTION_SET_INTENSITY = "com.sameerasw.essentials.ACTION_SET_INTENSITY"
        const val ACTION_PULSE_NOTIFICATION = "com.sameerasw.essentials.ACTION_PULSE_NOTIFICATION"
        const val EXTRA_INTENSITY = "intensity"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("FlashlightAction", "Action received: $action")

        val serviceIntent = Intent(context, ScreenOffAccessibilityService::class.java).apply {
            this.action = action
            if (intent.hasExtra(EXTRA_INTENSITY)) {
                putExtra(EXTRA_INTENSITY, intent.getIntExtra(EXTRA_INTENSITY, 1))
            }
        }
        context.startService(serviceIntent)
    }
}
