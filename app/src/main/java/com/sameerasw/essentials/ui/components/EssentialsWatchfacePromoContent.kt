/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Components
 * File: EssentialsWatchfacePromoContent.kt
 * Description: Reusable modular component showcasing the Essentials Watchface app promotion and testing setup steps.
 */

package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.WatchViewModel

@Composable
fun EssentialsWatchfacePromoContent(
    modifier: Modifier = Modifier,
    watchViewModel: WatchViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val view = LocalView.current

    val googleGroupUrl = "https://groups.google.com/u/1/g/madebysameerasw"
    val betaTestingUrl = "https://play.google.com/apps/testing/com.sameerasw.essentials.watchface"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            // Step 1: Join Google Group
            IconToggleItem(
                iconRes = R.drawable.rounded_person_24,
                title = stringResource(R.string.watchface_promo_step1_title),
                description = stringResource(R.string.watchface_promo_step1_desc),
                showToggle = false,
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    uriHandler.openUri(googleGroupUrl)
                },
                trailingContent = {
                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            uriHandler.openUri(googleGroupUrl)
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.watchface_promo_step1_action),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
            )

            // Step 2: Join Beta Program
            IconToggleItem(
                iconRes = R.drawable.rounded_science_24,
                title = stringResource(R.string.watchface_promo_step2_title),
                description = stringResource(R.string.watchface_promo_step2_desc),
                showToggle = false,
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    uriHandler.openUri(betaTestingUrl)
                },
                trailingContent = {
                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            uriHandler.openUri(betaTestingUrl)
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.watchface_promo_step2_action),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
            )

            // Step 3: Install on Watch
            IconToggleItem(
                iconRes = R.drawable.rounded_watch_24,
                title = stringResource(R.string.watchface_promo_step3_title),
                description = stringResource(R.string.watchface_promo_step3_desc),
                showToggle = false,
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    watchViewModel.openWatchfacePlayStoreOnWatch(context)
                },
                trailingContent = {
                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            watchViewModel.openWatchfacePlayStoreOnWatch(context)
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_open_in_new_24),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.watchface_promo_step3_action),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
            )
        }
    }
}

// Play Store version, to replace the internal testing steps above once the watchface is approved:
// (the step 3 title string needs its "3. " prefix dropped again)
/*
@Composable
fun EssentialsWatchfacePromoContent(
    modifier: Modifier = Modifier,
    watchViewModel: WatchViewModel = viewModel(),
) {
    val context = LocalContext.current

    StackedActionCard(
        iconRes = R.drawable.rounded_watch_24,
        title = stringResource(R.string.watchface_promo_step3_title),
        description = stringResource(R.string.watchface_promo_step3_desc),
        actionLabel = stringResource(R.string.watchface_promo_step3_action),
        actionIconRes = R.drawable.rounded_open_in_new_24,
        onAction = { watchViewModel.openWatchfacePlayStoreOnWatch(context) },
        modifier = modifier.fillMaxWidth(),
    )
}
*/
