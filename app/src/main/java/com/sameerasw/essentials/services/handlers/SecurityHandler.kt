package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.sameerasw.essentials.services.receivers.SecurityDeviceAdminReceiver

class SecurityHandler(
    private val service: AccessibilityService
) {
    private var originalAnimationScale: Float = 1.0f
    private var isScaleModified: Boolean = false

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val isScreenLockedSecurityEnabled = prefs.getBoolean("screen_locked_security_enabled", false)

        if (isScreenLockedSecurityEnabled) {
            val keyguardManager = service.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (keyguardManager.isKeyguardLocked) {
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    val source = event.source
                    if (source != null) {
                        checkNetworkTileInteraction(source)
                    }
                }
            }
        }
    }

    private fun checkNetworkTileInteraction(source: AccessibilityNodeInfo) {
        val keywords = listOf(
            "Internet", "Mobile Data", "Wi-Fi", // English
            "Daten", "WLAN", // German
            "Datos", // Spanish
            "Donn", // French (Données)
            "Cellular" // Some variants
        )
        
        var isNetworkTile = false
        for (text in keywords) {
             if (findNodeByText(source, text)) {
                isNetworkTile = true
                break
            }
        }

        if (isNetworkTile) {
            setReducedAnimationScale()
            service.performGlobalAction(GLOBAL_ACTION_BACK)
            lockDeviceHard()
            Toast.makeText(service, com.sameerasw.essentials.R.string.error_unlock_network_settings, Toast.LENGTH_SHORT).show()
        }
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): Boolean {
        val nodes = node.findAccessibilityNodeInfosByText(text)
        if (nodes.isNotEmpty()) return true
        
        val desc = node.contentDescription
        return desc != null && desc.toString().contains(text, ignoreCase = true)
    }

    private fun setReducedAnimationScale() {
        if (isScaleModified) return
        try {
            originalAnimationScale = Settings.Global.getFloat(
                service.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            Settings.Global.putFloat(
                service.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                0.1f
            )
            isScaleModified = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreAnimationScale() {
        if (!isScaleModified) return
        try {
            Settings.Global.putFloat(
                service.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                originalAnimationScale
            )
            isScaleModified = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun lockDevice() {
        service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    fun lockDeviceHard() {
        try {
            val dpm = service.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(service, SecurityDeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
            } else {
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
