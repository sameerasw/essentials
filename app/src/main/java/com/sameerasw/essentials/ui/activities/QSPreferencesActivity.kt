package com.sameerasw.essentials.ui.activities

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.sameerasw.essentials.FeatureSettingsActivity

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
            // Special case for Sound Mode to open the system volume panel
            if (componentName.className == "com.sameerasw.essentials.services.SoundModeTileService") {
                val volumeIntent = Intent("android.settings.panel.action.VOLUME").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(volumeIntent)
                finish()
                return
            }

            val feature = when (componentName.className) {
                "com.sameerasw.essentials.services.CaffeinateTileService" -> "Caffeinate"
                "com.sameerasw.essentials.services.NotificationLightingTileService" -> "Notification lighting"
                "com.sameerasw.essentials.services.DynamicNightLightTileService" -> "Dynamic night light"
                "com.sameerasw.essentials.services.FlashlightTileService" -> "Button remap"
                "com.sameerasw.essentials.services.AppLockTileService" -> "App lock"
                "com.sameerasw.essentials.services.ScreenLockedSecurityTileService" -> "Screen locked security"
                "com.sameerasw.essentials.services.AppFreezingTileService" -> "Freeze"
                "com.sameerasw.essentials.services.FlashlightPulseTileService" -> "Notification lighting"
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
