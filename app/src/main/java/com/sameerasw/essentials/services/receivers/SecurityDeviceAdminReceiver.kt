package com.sameerasw.essentials.services.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class SecurityDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // Optionally show a toast or log
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // Optionally show a toast or log
    }
}
