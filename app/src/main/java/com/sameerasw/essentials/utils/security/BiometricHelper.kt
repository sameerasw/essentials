/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Security Utilities
 * File: BiometricHelper.kt
 * Description: BiometricPrompt wrapper for authenticating user identity.
 */

package com.sameerasw.essentials.utils

import com.sameerasw.essentials.R
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {
    val allowedAuthenticators: Int
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = activity.getString(R.string.auth_required_title),
        subtitle: String = activity.getString(R.string.auth_confirm_identity_proceed),
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt =
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError(activity.getString(com.sameerasw.essentials.R.string.error_auth_failed))
                    }
                },
            )

        val promptInfo =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(allowedAuthenticators)
                .setConfirmationRequired(false)
                .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
