package com.sameerasw.essentials

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import java.util.concurrent.Executor

class AppLockActivity : FragmentActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var packageToLock: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force Dark Theme
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        window.setBackgroundDrawableResource(android.R.color.black)
        
        // Get accent color (respect Monet on Android 12+)
        val primaryColor = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.getColor(this, android.R.color.system_accent1_300)
        } else {
            val typedValue = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            typedValue.data
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.BLACK)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(0, (140 * resources.displayMetrics.density).toInt(), 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Composite icon layout
        val iconContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (96 * resources.displayMetrics.density).toInt(),
                (96 * resources.displayMetrics.density).toInt()
            )
        }

        val baseIconSize = (80 * resources.displayMetrics.density).toInt()
        val appRegistrationIcon = ImageView(this).apply {
            setImageResource(R.drawable.rounded_shield_lock_24)
            setColorFilter(primaryColor, android.graphics.PorterDuff.Mode.SRC_IN)
            layoutParams = FrameLayout.LayoutParams(baseIconSize, baseIconSize).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
        
        val essentialsIconSize = (32 * resources.displayMetrics.density).toInt()
        val essentialsIconView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher_round)
            layoutParams = FrameLayout.LayoutParams(essentialsIconSize, essentialsIconSize).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
            }
        }
        
        iconContainer.addView(appRegistrationIcon)
        iconContainer.addView(essentialsIconView)
        
        val titleView = TextView(this).apply {
            text = "App is locked"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 22f
            setPadding(0, (24 * resources.displayMetrics.density).toInt(), 0, 0)
            gravity = android.view.Gravity.CENTER
        }
        
        val subtextView = TextView(this).apply {
            text = "Please authenticate to unlock or \ngive the phone to the owner \n( -_-)"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            alpha = 0.6f
            setPadding((48 * resources.displayMetrics.density).toInt(), (8 * resources.displayMetrics.density).toInt(), (48 * resources.displayMetrics.density).toInt(), 0)
            gravity = android.view.Gravity.CENTER
        }
        
        root.addView(iconContainer)
        root.addView(titleView)
        root.addView(subtextView)
        setContentView(root)
        
        packageToLock = intent.getStringExtra("package_to_lock")
        if (packageToLock == null) {
            finish()
            return
        }

        val appLabel = try {
            val appInfo = packageManager.getApplicationInfo(packageToLock!!, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageToLock
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
            .setSubtitle("Unlock to access $appLabel")
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
        val serviceIntent = Intent(this, ScreenOffAccessibilityService::class.java).apply {
            action = "APP_AUTHENTICATED"
            putExtra("package_name", packageToLock)
        }
        startService(serviceIntent)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun notifyFailureAndFinish() {
        val serviceIntent = Intent(this, ScreenOffAccessibilityService::class.java).apply {
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
