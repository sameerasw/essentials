/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: AccessibilityShortcutService.kt
 * Description: Background accessibility service component dedicated to the accessibility button and volume shortcut trigger.
 */

package com.sameerasw.essentials.services

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.services.automation.AutomationManager

class AccessibilityShortcutService : AccessibilityService() {

    private var accessibilityButtonController: AccessibilityButtonController? = null
    private var buttonCallback: AccessibilityButtonController.AccessibilityButtonCallback? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            accessibilityButtonController = getAccessibilityButtonController()
            buttonCallback =
                object : AccessibilityButtonController.AccessibilityButtonCallback() {
                    override fun onClicked(controller: AccessibilityButtonController) {
                        AutomationManager.triggerAccessibilityShortcut(this@AccessibilityShortcutService)
                    }

                    override fun onAvailabilityChanged(
                        controller: AccessibilityButtonController,
                        available: Boolean,
                    ) {
                    }
                }
            accessibilityButtonController?.registerAccessibilityButtonCallback(buttonCallback!!)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && buttonCallback != null) {
            accessibilityButtonController?.unregisterAccessibilityButtonCallback(buttonCallback!!)
        }
        super.onDestroy()
    }
}
