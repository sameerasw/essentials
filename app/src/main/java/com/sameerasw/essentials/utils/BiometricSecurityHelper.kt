package com.sameerasw.essentials.utils

import androidx.fragment.app.FragmentActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Feature

/**
 * Robust, scalable biometric security solution for protected features.
 * Any feature marked with 'requiresAuth' will be intercepted here.
 */
object BiometricSecurityHelper {

    /**
     * Executes the [action] with biometric authentication if the [feature] requires it.
     */
    fun runWithAuth(
        activity: FragmentActivity,
        feature: Feature,
        action: () -> Unit,
        isToggle: Boolean = false,
        onAuthFailed: (String) -> Unit = {}
    ) {
        if (!feature.requiresAuth) {
            action()
            return
        }

        val title = if (feature.authTitle != 0) {
            activity.getString(feature.authTitle)
        } else {
            activity.getString(
                R.string.biometric_title_settings_format,
                activity.getString(feature.title)
            )
        }

        val subtitle = if (feature.authSubtitle != 0) {
            activity.getString(feature.authSubtitle)
        } else if (isToggle) {
            activity.getString(R.string.biometric_subtitle_access_settings)
        } else {
            activity.getString(R.string.biometric_subtitle_access_settings)
        }

        BiometricHelper.showBiometricPrompt(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = action,
            onError = onAuthFailed
        )
    }

    /**
     * Shows a standard biometric prompt for accessing a protected feature.
     */
    fun showFeatureAuth(
        activity: FragmentActivity,
        feature: Feature,
        onSuccess: () -> Unit
    ) {
        runWithAuth(activity, feature, onSuccess)
    }
}
