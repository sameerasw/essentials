package com.sameerasw.essentials.ui.activities

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.edit
import com.sameerasw.essentials.utils.BiometricHelper
import androidx.fragment.app.FragmentActivity

class TileAuthActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity transparent
        window.setBackgroundDrawableResource(android.R.color.transparent)
        
        // This activity should only be launched if we need to authenticate
        val feature = intent.getStringExtra("feature_pref_key")
        
        if (feature == "screen_locked_security_enabled") {
            val title = intent.getStringExtra("auth_title") ?: "Authentication Required"
            val subtitle = intent.getStringExtra("auth_subtitle") ?: "Confirm your identity"
            
            BiometricHelper.showBiometricPrompt(
                activity = this,
                title = title,
                subtitle = subtitle,
                onSuccess = {
                    val prefs = getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                    val isEnabled = prefs.getBoolean(feature, false)
                    prefs.edit { putBoolean(feature, !isEnabled) }
                    
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
