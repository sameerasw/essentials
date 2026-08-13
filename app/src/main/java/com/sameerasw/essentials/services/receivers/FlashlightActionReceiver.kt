/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: FlashlightActionReceiver.kt
 * Description: Background service component for FlashlightActionReceiver.kt.
 */

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
        const val ACTION_START_SOS = "com.sameerasw.essentials.ACTION_START_SOS"
        const val ACTION_START_STROBE = "com.sameerasw.essentials.ACTION_START_STROBE"
        const val ACTION_STOP_SPECIAL_MODES = "com.sameerasw.essentials.ACTION_STOP_SPECIAL_MODES"
        const val ACTION_PULSE_NOTIFICATION = "com.sameerasw.essentials.ACTION_PULSE_NOTIFICATION"
        const val EXTRA_INTENSITY = "intensity"
        const val EXTRA_STROBE_SPEED = "strobe_speed"
        const val EXTRA_STROBE_FADE = "strobe_fade"
        const val EXTRA_IS_PREVIEW = "is_preview"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("FlashlightAction", "Action received: $action")

        val serviceIntent = Intent(context, ScreenOffAccessibilityService::class.java).apply {
            this.action = action
            if (intent.hasExtra(EXTRA_INTENSITY)) {
                putExtra(EXTRA_INTENSITY, intent.getIntExtra(EXTRA_INTENSITY, 1))
            }
            if (intent.hasExtra(EXTRA_STROBE_SPEED)) {
                putExtra(EXTRA_STROBE_SPEED, intent.getFloatExtra(EXTRA_STROBE_SPEED, 5f))
            }
            if (intent.hasExtra(EXTRA_STROBE_FADE)) {
                putExtra(EXTRA_STROBE_FADE, intent.getBooleanExtra(EXTRA_STROBE_FADE, false))
            }
            if (intent.hasExtra(EXTRA_IS_PREVIEW)) {
                putExtra(EXTRA_IS_PREVIEW, intent.getBooleanExtra(EXTRA_IS_PREVIEW, false))
            }
        }
        context.startService(serviceIntent)
    }
}
