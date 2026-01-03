package com.sameerasw.essentials

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class AppLockActivity : FragmentActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var packageToLock: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Full Screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.setBackgroundDrawableResource(android.R.color.black)
        
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.BLACK)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(0, (120 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        
        val iconSize = (80 * resources.displayMetrics.density).toInt()
        val iconView = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.rounded_security_24)
            setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
            alpha = 0.3f
            layoutParams = android.widget.LinearLayout.LayoutParams(iconSize, iconSize)
        }
        
        val textView = android.widget.TextView(this).apply {
            text = "Locked"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 20f
            alpha = 0.5f
            setPadding(0, (16 * resources.displayMetrics.density).toInt(), 0, 0)
            gravity = android.view.Gravity.CENTER
        }
        
        root.addView(iconView)
        root.addView(textView)
        setContentView(root)
        
        packageToLock = intent.getStringExtra("package_to_lock")
        if (packageToLock == null) {
            finish()
            return
        }

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.d("AppLock", "Authentication error: $errString ($errorCode)")
                    notifyFailureAndFinish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d("AppLock", "Authentication succeeded!")
                    notifySuccessAndFinish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d("AppLock", "Authentication failed")
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Lock")
            .setSubtitle("Unlock to access $packageToLock")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun notifySuccessAndFinish() {
        val intent = Intent("APP_AUTHENTICATED").apply {
            `package` = packageName
            putExtra("package_name", packageToLock)
        }
        sendBroadcast(intent)
        // Also notify via service to be more reliable
        val serviceIntent = Intent(this, com.sameerasw.essentials.services.ScreenOffAccessibilityService::class.java).apply {
            action = "APP_AUTHENTICATED"
            putExtra("package_name", packageToLock)
        }
        startService(serviceIntent)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun notifyFailureAndFinish() {
        val serviceIntent = Intent(this, com.sameerasw.essentials.services.ScreenOffAccessibilityService::class.java).apply {
            action = "APP_AUTHENTICATION_FAILED"
        }
        startService(serviceIntent)
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onBackPressed() {
        // Prevent going back, treated as cancel/failure
        notifyFailureAndFinish()
        super.onBackPressed()
    }
}
