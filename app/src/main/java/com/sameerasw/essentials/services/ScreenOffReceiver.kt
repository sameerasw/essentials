package com.sameerasw.essentials.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sameerasw.essentials.domain.MapsState
import com.sameerasw.essentials.utils.ShellUtils

class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF && MapsState.isEnabled && MapsState.hasNavigationNotification) {
            ShellUtils.runCommand(
                context,
                "am start -n com.google.android.apps.maps/com.google.android.apps.gmm.features.minmode.MinModeActivity"
            )
        }
    }
}