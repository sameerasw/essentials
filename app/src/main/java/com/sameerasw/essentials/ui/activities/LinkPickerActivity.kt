/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: LinkPickerActivity.kt
 * Description: Activity component for LinkPickerActivity.kt.
 */

package com.sameerasw.essentials

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.linkActions.LinkPickerScreen
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.viewmodels.LocationReachedViewModel

class LinkPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val locationViewModel = LocationReachedViewModel(application)
        if (locationViewModel.handleIntent(intent)) {
            val settingsIntent =
                Intent(this, FeatureSettingsActivity::class.java).apply {
                    putExtra("feature", "Location reached")
                }
            startActivity(settingsIntent)
            finish()
            return
        }

        val uri =
            when (intent.action) {
                Intent.ACTION_SEND -> {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                    extractUrl(text)?.let { Uri.parse(it) }
                }

                else -> intent.data
            }

        if (uri == null) {
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val settingsRepository = remember(context) { SettingsRepository(context) }
            val isPitchBlackThemeEnabled =
                settingsRepository.getBoolean(SettingsRepository.KEY_PITCH_BLACK_THEME_ENABLED)
            val disableLinkPreview =
                settingsRepository.getBoolean(SettingsRepository.KEY_DISABLE_LINK_PREVIEW)
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                LinkPickerScreen(
                    uri = uri,
                    disableLinkPreview = disableLinkPreview,
                    onFinish = { finish() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}

private fun extractUrl(text: String): String? {
    val urlRegex = Regex("https?://[\\w\\.-]+(?:\\:[0-9]+)?(?:/[^\\s]*)?")
    return urlRegex.find(text)?.value
}
