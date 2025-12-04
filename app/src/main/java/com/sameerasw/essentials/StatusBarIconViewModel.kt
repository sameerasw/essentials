package com.sameerasw.essentials

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class StatusBarIconViewModel : ViewModel() {
    val isWriteSecureSettingsEnabled = mutableStateOf(false)
    val isMobileDataVisible = mutableStateOf(true)
    val isWiFiVisible = mutableStateOf(false)

    companion object {
        const val ICON_BLACKLIST_SETTING = "icon_blacklist"
        const val BASE_BLACKLIST = "rotate,vowifi,battery,ims,nfc,vpn,volte,alarm_clock,headset,hotspot,bluetooth,ims_volte,vpn"
    }

    fun check(context: Context) {
        isWriteSecureSettingsEnabled.value = canWriteSecureSettings(context)
        loadIconVisibilityState(context)
    }

    fun setMobileDataVisible(visible: Boolean, context: Context) {
        isMobileDataVisible.value = visible
        updateIconBlacklist(context)
    }

    fun setWiFiVisible(visible: Boolean, context: Context) {
        isWiFiVisible.value = visible
        updateIconBlacklist(context)
    }

    private fun updateIconBlacklist(context: Context) {
        if (!isWriteSecureSettingsEnabled.value) return

        val blacklistItems = BASE_BLACKLIST.split(",").toMutableList()

        // Add or remove mobile from blacklist based on visibility
        if (!isMobileDataVisible.value && !blacklistItems.contains("mobile")) {
            blacklistItems.add("mobile")
        } else if (isMobileDataVisible.value) {
            blacklistItems.remove("mobile")
        }

        // Add or remove wifi from blacklist based on visibility
        if (!isWiFiVisible.value && !blacklistItems.contains("wifi")) {
            blacklistItems.add("wifi")
        } else if (isWiFiVisible.value) {
            blacklistItems.remove("wifi")
        }

        val newBlacklist = blacklistItems.joinToString(",")

        try {
            Settings.Secure.putString(context.contentResolver, ICON_BLACKLIST_SETTING, newBlacklist)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadIconVisibilityState(context: Context) {
        try {
            val currentBlacklist = Settings.Secure.getString(
                context.contentResolver,
                ICON_BLACKLIST_SETTING
            ) ?: "$BASE_BLACKLIST,mobile,wifi"

            isMobileDataVisible.value = !currentBlacklist.contains("mobile")
            isWiFiVisible.value = !currentBlacklist.contains("wifi")
        } catch (e: Exception) {
            e.printStackTrace()
            isMobileDataVisible.value = true
            isWiFiVisible.value = false
        }
    }

    private fun canWriteSecureSettings(context: Context): Boolean {
        return try {
            val currentValue = Settings.Secure.getString(
                context.contentResolver,
                ICON_BLACKLIST_SETTING
            )
            // Try to write the same value back (no-op) to verify permission
            Settings.Secure.putString(
                context.contentResolver,
                ICON_BLACKLIST_SETTING,
                currentValue ?: "$BASE_BLACKLIST,mobile,wifi"
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getAdbCommand(): String {
        return "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
    }
}

