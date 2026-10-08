/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Components
 * File: EssentialsWatchfacePromoContent.kt
 * Description: Reusable modular component showcasing the Essentials Watchface app and opening its Play Store page on the watch.
 */

package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.viewmodels.WatchViewModel

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
