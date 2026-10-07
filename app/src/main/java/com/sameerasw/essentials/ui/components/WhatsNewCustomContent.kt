/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Components
 * File: WhatsNewCustomContent.kt
 * Description: UI layout element for WhatsNewCustomContent.kt.
 */

package com.sameerasw.essentials.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.GoogleSansFlexRounded
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.weather.overcast.OvercastWeather

/**
 * Slot for custom content to be displayed in the "What's New" screen.
 */
@Composable
fun WhatsNewCustomContent(
    mainViewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current

    LifecycleResumeEffect(Unit) {
        mainViewModel.check(context)
        onPauseOrDispose { }
    }

    val isInstalled = OvercastWeather.isInstalled(context)
    val hasPermission = OvercastWeather.hasPermission(context)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.overcast_logo),
            contentDescription = "Overcast App Icon",
            modifier = Modifier.size(250.dp),
        )

        Text(
            text = stringResource(R.string.whats_new_overcast_section_title),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(R.string.whats_new_overcast_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            IconToggleItem(
                iconRes = R.drawable.rounded_cloud_24,
                title = stringResource(R.string.whats_new_overcast_card_title),
                description =
                    when {
                        !isInstalled -> stringResource(R.string.whats_new_overcast_status_not_installed)
                        !hasPermission -> stringResource(R.string.whats_new_overcast_status_permission_needed)
                        else -> stringResource(R.string.whats_new_overcast_status_installed)
                    },
                showToggle = false,
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    when {
                        !isInstalled -> OvercastWeather.openInstallPage(context)
                        !hasPermission -> (context as? ComponentActivity)?.let { mainViewModel.requestOvercastWeatherPermission(it) }
                        else -> OvercastWeather.openApp(context)
                    }
                },
                trailingContent = {
                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            when {
                                !isInstalled -> OvercastWeather.openInstallPage(context)
                                !hasPermission -> (context as? ComponentActivity)?.let { mainViewModel.requestOvercastWeatherPermission(it) }
                                else -> OvercastWeather.openApp(context)
                            }
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors =
                            if (isInstalled && hasPermission) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                )
                            },
                    ) {
                        if (!isInstalled) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_download_24),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else if (hasPermission) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_open_in_new_24),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text =
                                when {
                                    !isInstalled -> stringResource(R.string.whats_new_overcast_action_install)
                                    !hasPermission -> stringResource(R.string.whats_new_overcast_action_allow_permission)
                                    else -> stringResource(R.string.whats_new_overcast_action_open)
                                },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
            )
        }

        Text(
            text = stringResource(R.string.setting_hidden_debugging_title),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )

        listOf(
            R.string.setting_hidden_debugging_help_1,
            R.string.setting_hidden_debugging_help_2,
            R.string.setting_hidden_debugging_help_3,
        ).forEach { textRes ->
            Text(
                text = stringResource(textRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        com.sameerasw.essentials.ui.core.sheets.HiddenDebuggingAutoDetectCard(
            onAutoDetected = { mainViewModel.setHiddenDebuggingSupport(true) },
        )
    }
}