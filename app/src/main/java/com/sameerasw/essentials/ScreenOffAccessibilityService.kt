package com.sameerasw.essentials

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class ScreenOffAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for this feature
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "LOCK_SCREEN") {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
