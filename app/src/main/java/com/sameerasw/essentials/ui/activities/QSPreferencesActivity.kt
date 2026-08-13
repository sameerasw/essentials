/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: QSPreferencesActivity.kt
 * Description: Activity component for QSPreferencesActivity.kt.
 */

package com.sameerasw.essentials.ui.activities

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.utils.DeviceLockUtils

class QSPreferencesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val componentName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
        }

        Log.d("QSPreferences", "Received long-press for: ${componentName?.className}")

        if (componentName != null) {
            if (componentName.className == "com.sameerasw.essentials.services.tiles.LockdownTileService") {
                DeviceLockUtils.performLockdownTileAction(this, isLongPress = true)
                finish()
                return
            }
            // Special case for Sound Mode to open the system volume panel
            if (componentName.className == "com.sameerasw.essentials.services.tiles.SoundModeTileService") {
                val volumeIntent = Intent("android.settings.panel.action.VOLUME").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(volumeIntent)
                finish()
                return
            }

            if (componentName.className == "com.sameerasw.essentials.services.tiles.FlashlightTileService") {
                val intent = Intent(this, FlashlightIntensityActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
                return
            }

            if (componentName.className == "com.sameerasw.essentials.services.tiles.SmartPixelsTileService") {
                val intent = Intent(this, SmartPixelsIntensityActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
                return
            }

            if (componentName.className == "com.sameerasw.essentials.services.tiles.EssentialsOnDisplayTileService") {
                val intent = Intent(this, com.sameerasw.essentials.FeatureSettingsActivity::class.java).apply {
                    putExtra("feature", "Essentials On Display")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
                return
            }

            if (componentName.className == "com.sameerasw.essentials.services.tiles.PrivateDnsTileService") {
                val intent = Intent(this, PrivateDnsSettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
                return
            }

            if (componentName.className == "com.sameerasw.essentials.services.tiles.AdaptiveBrightnessTileService") {
                val displayIntent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(displayIntent)
                finish()
                return
            }

            if (componentName.className == "com.sameerasw.essentials.services.tiles.DeveloperOptionsTileService" ||
                componentName.className == "com.sameerasw.essentials.services.tiles.UsbDebuggingTileService"
            ) {
                val devIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(devIntent)
                finish()
                return
            }

            if (componentName.className == "com.sameerasw.essentials.services.tiles.ChargeQuickTileService" ||
                componentName.className == "com.sameerasw.essentials.services.tiles.BatteryNotificationTileService"
            ) {
                val intent = Intent(this, BatteryDetailsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
                return
            }

            val feature = when (componentName.className) {
                "com.sameerasw.essentials.services.tiles.CaffeinateTileService" -> "Caffeinate"
                "com.sameerasw.essentials.services.tiles.NotificationLightingTileService" -> "Notification lighting"
                "com.sameerasw.essentials.services.tiles.DynamicNightLightTileService" -> "Dynamic night light"
                "com.sameerasw.essentials.services.tiles.AppLockTileService" -> "App lock"
                "com.sameerasw.essentials.services.tiles.ScreenLockedSecurityTileService" -> "Screen locked security"
                "com.sameerasw.essentials.services.tiles.AppFreezingTileService" -> "Freeze"
                "com.sameerasw.essentials.services.tiles.FlashlightPulseTileService" -> "Notification lighting"
                "com.sameerasw.essentials.services.tiles.StayAwakeTileService" -> "Quick settings tiles"
                "com.sameerasw.essentials.services.tiles.NfcTileService" -> "NFC"
                "com.sameerasw.essentials.services.tiles.AdaptiveBrightnessTileService" -> "Quick settings tiles"
                "com.sameerasw.essentials.services.tiles.RefreshRateTileService" -> "Screen refresh rate"
                "com.sameerasw.essentials.services.tiles.MapsPowerSavingTileService" -> "Maps power saving mode"
                "com.sameerasw.essentials.services.tiles.UsbDebuggingTileService" -> "Quick settings tiles"
                "com.sameerasw.essentials.services.tiles.BatteryNotificationTileService" -> "Battery notification"
                "com.sameerasw.essentials.services.tiles.ChargeQuickTileService" -> "Battery notification"
                "com.sameerasw.essentials.services.tiles.AlwaysOnDisplayTileService" -> "Always on Display"
                "com.sameerasw.essentials.services.tiles.LocationReachedTileService" -> "Location reached"
                else -> null
            }


            Log.d("QSPreferences", "Mapping to feature: $feature")

            if (feature != null) {
                // Check if authentication is required
                if (feature == "App lock" || feature == "Screen locked security") {
                    val authIntent = Intent(this, TileAuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("target_feature", feature)
                        putExtra("auth_title", "$feature Settings")
                        putExtra("auth_subtitle", "Confirm identity to open settings")
                    }
                    startActivity(authIntent)
                } else {
                    val settingsIntent = Intent(this, FeatureSettingsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("feature", feature)
                    }
                    startActivity(settingsIntent)
                }
            }
        }

        finish()
    }
}
