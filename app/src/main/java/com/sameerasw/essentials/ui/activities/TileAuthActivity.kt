package com.sameerasw.essentials.ui.activities

import android.content.Context
import android.os.Bundle
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import com.sameerasw.essentials.utils.BiometricHelper

class TileAuthActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity transparent
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val feature = intent.getStringExtra("feature_pref_key")
        val targetFeature = intent.getStringExtra("target_feature")

        if (feature != null || targetFeature != null) {
            val title = intent.getStringExtra("auth_title") ?: "Authentication Required"
            val subtitle = intent.getStringExtra("auth_subtitle") ?: "Confirm your identity"

            BiometricHelper.showBiometricPrompt(
                activity = this,
                title = title,
                subtitle = subtitle,
                onSuccess = {
                    if (targetFeature != null) {
                        // Navigate to settings
                        val settingsIntent = android.content.Intent(
                            this,
                            com.sameerasw.essentials.FeatureSettingsActivity::class.java
                        ).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            putExtra("feature", targetFeature)
                        }
                        startActivity(settingsIntent)
                    } else if (feature != null) {
                        // Toggle preference
                        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                        val isEnabled = prefs.getBoolean(feature, false)
                        prefs.edit { putBoolean(feature, !isEnabled) }
                    }

                    finish()
                },
                onError = {
                    finish()
                }
            )
        } else {
            finish()
        }
    }
}
