/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: ConsciousGateActivity.kt
 * Description: Activity component for ConsciousGateActivity.kt.
 */

package com.sameerasw.essentials.ui.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.ConsciousGateCountdownStyle
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import com.sameerasw.essentials.ui.features.consciousgate.ConsciousGatePauseScreen
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import kotlinx.coroutines.delay

class ConsciousGateActivity : AppCompatActivity() {
    private var packageToGate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        packageToGate = intent.getStringExtra("package_to_gate")
        if (packageToGate == null) {
            finish()
            return
        }

        val appLabel =
            try {
                val appInfo = packageManager.getApplicationInfo(packageToGate!!, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageToGate
            }

        val delaySeconds = intent.getIntExtra("delay_seconds", 5).coerceAtLeast(0)
        val iconName = intent.getStringExtra("icon_name") ?: "rounded_pause_24"
        val title = intent.getStringExtra("title")
        val message = intent.getStringExtra("message")
        val countdownStyle =
            try {
                ConsciousGateCountdownStyle.valueOf(
                    intent.getStringExtra("countdown_style") ?: ConsciousGateCountdownStyle.CIRCULAR_WAVY.name,
                )
            } catch (e: Exception) {
                ConsciousGateCountdownStyle.CIRCULAR_WAVY
            }

        setContent {
            EssentialsTheme {
                ConsciousGateScreen(
                    appLabel = appLabel ?: "",
                    iconName = iconName,
                    title = title,
                    message = message,
                    delaySeconds = delaySeconds,
                    countdownStyle = countdownStyle,
                    onClose = ::notifyClosedAndFinish,
                    onContinue = ::notifyConfirmedAndFinish,
                )
            }
        }
    }

    @Composable
    private fun ConsciousGateScreen(
        appLabel: String,
        iconName: String,
        title: String?,
        message: String?,
        delaySeconds: Int,
        countdownStyle: ConsciousGateCountdownStyle,
        onClose: () -> Unit,
        onContinue: () -> Unit,
    ) {
        val iconResId =
            remember(iconName) {
                resources
                    .getIdentifier(iconName, "drawable", packageName)
                    .takeIf { it != 0 } ?: R.drawable.rounded_pause_24
            }

        var progress by remember { mutableFloatStateOf(if (delaySeconds <= 0) 1f else 0f) }

        LaunchedEffect(delaySeconds) {
            if (delaySeconds <= 0) {
                progress = 1f
                return@LaunchedEffect
            }
            val totalMillis = delaySeconds * 1000L
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed.toFloat() / totalMillis).coerceIn(0f, 1f)
                if (progress >= 1f) break
                delay(16)
            }
        }

        ConsciousGatePauseScreen(
            iconResId = iconResId,
            title = title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.conscious_gate_default_title),
            message = message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.conscious_gate_default_message),
            targetAppLabel = appLabel,
            countdownStyle = countdownStyle,
            progress = { progress },
            isContinueEnabled = progress >= 1f,
            onClose = onClose,
            onContinue = onContinue,
        )
    }

    private fun notifyConfirmedAndFinish() {
        val intent =
            Intent("CONSCIOUS_GATE_CONFIRMED").apply {
                `package` = packageName
                putExtra("package_name", packageToGate)
            }
        sendBroadcast(intent)

        val accessibilityIntent =
            Intent(this, ScreenOffAccessibilityService::class.java).apply {
                action = "CONSCIOUS_GATE_CONFIRMED"
                putExtra("package_name", packageToGate)
            }
        startService(accessibilityIntent)

        finishAndTransition()
    }

    private fun notifyClosedAndFinish() {
        val intent =
            Intent("CONSCIOUS_GATE_CLOSED").apply {
                `package` = packageName
            }
        sendBroadcast(intent)

        val serviceIntent =
            Intent(this, ScreenOffAccessibilityService::class.java).apply {
                action = "CONSCIOUS_GATE_CLOSED"
            }
        startService(serviceIntent)

        finishAndTransition()
    }

    private fun finishAndTransition() {
        finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
