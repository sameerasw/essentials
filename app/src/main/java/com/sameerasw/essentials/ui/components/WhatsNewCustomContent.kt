/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Components
 * File: WhatsNewCustomContent.kt
 * Description: UI layout element for WhatsNewCustomContent.kt.
 */

package com.sameerasw.essentials.ui.components

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
import com.sameerasw.essentials.island.remote.OvercastLink
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.GoogleSansFlexRounded
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

/**
 * Slot for custom content to be displayed in the "What's New" screen.
 */
@Composable
fun WhatsNewCustomContent(
    mainViewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val overcastInstalled = OvercastLink.isInstalled(context)
    var expandedSection by rememberSaveable { mutableStateOf<Int?>(null) }

    LifecycleResumeEffect(Unit) {
        mainViewModel.check(context)
        onPauseOrDispose { }
    }

    fun toggle(section: Int) {
        HapticUtil.performUIHaptic(view)
        expandedSection = if (expandedSection == section) null else section
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        WhatsNewSectionCard(
            title = "Overcast is now on Play Store",
            summary = "Get the weather island provider from Google Play",
            expanded = expandedSection == 0,
            onToggle = { toggle(0) },
            icon = { SectionIcon(R.drawable.overcast_logo, tinted = false) },
        ) {
            Text(
                text = "Overcast brings live weather to your island as an external provider. It is now available on Google Play, so you can install it and keep it up to date like any other app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StackedActionCard(
                iconRes = R.drawable.rounded_cloud_24,
                title = "Overcast",
                description = if (overcastInstalled) "Installed and ready to use" else "Not installed yet",
                actionLabel = if (overcastInstalled) "Open" else "Get it on Google Play",
                actionIconRes = if (overcastInstalled) R.drawable.rounded_open_in_new_24 else R.drawable.rounded_download_24,
                isPrimaryAction = !overcastInstalled,
                onAction = {
                    if (overcastInstalled) OvercastLink.openApp(context) else OvercastLink.openInstallPage(context)
                },
            )
        }

        WhatsNewSectionCard(
            title = "Hello foldables",
            summary = "Layouts built for bigger and wider screens",
            expanded = expandedSection == 3,
            onToggle = { toggle(3) },
            icon = { SectionIcon(R.drawable.rounded_tablet_android_24) },
        ) {
            Text(
                text = "Essentials now uses Navigation3 for adaptive layouts. On foldables and tablets, screens make better use of the extra space with side-by-side panes, and phones in landscape get layouts that fit the wider screen instead of stretching a portrait one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Unfold your device or rotate your phone to see it in action.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        WhatsNewSectionCard(
            title = "Island feels new",
            summary = "A fresh look with smoother animations",
            expanded = expandedSection == 4,
            onToggle = { toggle(4) },
            icon = { SectionIcon(R.drawable.rounded_auto_awesome_24) },
        ) {
            Text(
                text = "Expanded views have a new translucent design with a soft glow that follows the colors of what you are viewing, from notifications to your music.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Swipe sideways to move between notifications and media players in carousel order, or switch Dismissals to Down to swipe down to dismiss. Media can also live in a secondary bubble or a wide pill, and colors now change smoothly as the island collapses.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Plus many refinements to animations, gestures and stability. Prefer something lighter? Turn off Island transparency for an efficient mode that keeps just the color glow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = stringResource(R.string.whats_new_previous_updates),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
        )

        ShakeForHelpCard(
            expanded = expandedSection == 2,
            onToggle = { toggle(2) },
            mainViewModel = mainViewModel,
        )

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
    }
}

@Composable
fun ShakeForHelpCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    mainViewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    WhatsNewSectionCard(
        title = "Shake to get help",
        summary = "Shake your phone for instant help",
        expanded = expanded,
        onToggle = onToggle,
        icon = { SectionIcon(R.drawable.rounded_mobile_vibrate_24) },
    ) {
        Text(
            text = "Shake your phone anywhere in Essentials to open help for the screen you are on. Inside a feature you get that feature's guide, and everywhere else you get the general Help and Guides.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_vibrate_24,
                title = stringResource(R.string.label_shake_for_help),
                isChecked = mainViewModel.isShakeForHelpEnabled.value,
                onCheckedChange = { mainViewModel.setShakeForHelpEnabled(it, context) },
            )
        }

        Text(
            text = "Give your phone a shake now to try it!",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
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
