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
import com.sameerasw.essentials.services.automation.AutomationManager

abstract class BaseAccessibilityShortcutService(private val slot: Int) : AccessibilityService() {

    private var accessibilityButtonController: AccessibilityButtonController? = null
    private var buttonCallback: AccessibilityButtonController.AccessibilityButtonCallback? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            accessibilityButtonController = getAccessibilityButtonController()
            buttonCallback =
                object : AccessibilityButtonController.AccessibilityButtonCallback() {
                    override fun onClicked(controller: AccessibilityButtonController) {
                        AutomationManager.triggerAccessibilityShortcut(this@BaseAccessibilityShortcutService, slot)
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

class AccessibilityShortcutService1 : BaseAccessibilityShortcutService(1)
class AccessibilityShortcutService2 : BaseAccessibilityShortcutService(2)
class AccessibilityShortcutService3 : BaseAccessibilityShortcutService(3)

