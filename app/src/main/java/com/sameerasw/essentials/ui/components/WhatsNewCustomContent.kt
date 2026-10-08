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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.sheets.HiddenDebuggingAutoDetectCard
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
    var expandedSection by rememberSaveable { mutableStateOf<Int?>(null) }

    LifecycleResumeEffect(Unit) {
        mainViewModel.check(context)
        onPauseOrDispose { }
    }

    val isInstalled = OvercastWeather.isInstalled(context)
    val hasPermission = OvercastWeather.hasPermission(context)

    fun toggle(section: Int) {
        HapticUtil.performUIHaptic(view)
        expandedSection = if (expandedSection == section) null else section
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        WhatsNewSectionCard(
            title = stringResource(R.string.whats_new_overcast_card_title),
            summary = stringResource(R.string.whats_new_overcast_section_title),
            expanded = expandedSection == 0,
            onToggle = { toggle(0) },
            icon = { SectionIcon(R.drawable.overcast_logo, tinted = false) },
        ) {
            Text(
                text = stringResource(R.string.whats_new_overcast_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.whats_new_overcast_play_store_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            StackedActionCard(
                iconRes = R.drawable.rounded_cloud_24,
                title = stringResource(R.string.whats_new_overcast_card_title),
                description =
                    when {
                        !isInstalled -> stringResource(R.string.whats_new_overcast_status_not_installed)
                        !hasPermission -> stringResource(R.string.whats_new_overcast_status_permission_needed)
                        else -> stringResource(R.string.whats_new_overcast_status_installed)
                    },
                actionLabel =
                    when {
                        !isInstalled -> stringResource(R.string.whats_new_overcast_action_install)
                        !hasPermission -> stringResource(R.string.whats_new_overcast_action_allow_permission)
                        else -> stringResource(R.string.whats_new_overcast_action_open)
                    },
                actionIconRes =
                    when {
                        !isInstalled -> R.drawable.rounded_download_24
                        hasPermission -> R.drawable.rounded_open_in_new_24
                        else -> null
                    },
                isPrimaryAction = !(isInstalled && hasPermission),
                onAction = {
                    when {
                        !isInstalled -> OvercastWeather.openInstallPage(context)
                        !hasPermission -> (context as? ComponentActivity)?.let { mainViewModel.requestOvercastWeatherPermission(it) }
                        else -> OvercastWeather.openApp(context)
                    }
                },
            )
        }

        WhatsNewSectionCard(
            title = stringResource(R.string.whats_new_watchface_title),
            summary = stringResource(R.string.whats_new_watchface_summary),
            expanded = expandedSection == 1,
            onToggle = { toggle(1) },
            icon = { SectionIcon(R.drawable.rounded_watch_24) },
        ) {
            Text(
                text = stringResource(R.string.whats_new_watchface_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EssentialsWatchfacePromoContent()
        }

        WhatsNewSectionCard(
            title = stringResource(R.string.setting_hidden_debugging_title),
            summary = stringResource(R.string.whats_new_hidden_debugging_summary),
            expanded = expandedSection == 2,
            onToggle = { toggle(2) },
            icon = { SectionIcon(R.drawable.rounded_adb_24) },
        ) {
            listOf(
                R.string.setting_hidden_debugging_help_1,
                R.string.setting_hidden_debugging_help_2,
                R.string.setting_hidden_debugging_help_3,
            ).forEach { textRes ->
                Text(
                    text = stringResource(textRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HiddenDebuggingAutoDetectCard(
                onAutoDetected = { mainViewModel.setHiddenDebuggingSupport(true) },
            )
        }
    }
}

private val CARD_ICON_SIZE = 64.dp

@Composable
private fun SectionIcon(
    iconRes: Int,
    tinted: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .size(CARD_ICON_SIZE)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (tinted) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(CARD_ICON_SIZE),
            )
        }
    }
}

@Composable
private fun WhatsNewSectionCard(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "whatsNewChevron")

    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                icon()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}
