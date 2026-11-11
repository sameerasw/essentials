/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: ShakeForHelp.kt
 * Description: Drop-in host that opens the contextual help sheet when the device is shaken.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.ui.geometry.Offset
import com.sameerasw.essentials.ui.modifiers.AppRipple
import com.sameerasw.essentials.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.hardware.ShakeDetector
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun ShakeForHelp(
    feature: Feature? = null,
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val enabled by viewModel.isShakeForHelpEnabled
    var showHelp by remember { mutableStateOf(false) }
    val token = remember { Any() }
    DisposableEffect(token) {
        ShakeHostStack.hosts += token
        onDispose { ShakeHostStack.hosts -= token }
    }

    val currentView by rememberUpdatedState(view)
    val detector =
        remember(context) {
            ShakeDetector(
                context = context,
                onJerk = { count ->
                    if (count == 2 && !HelpSheetTracker.isAnyOpen && ShakeHostStack.isTop(token)) {
                        HapticUtil.performMicroHaptic(currentView)
                    }
                },
            ) {
                if (!HelpSheetTracker.isAnyOpen && ShakeHostStack.isTop(token)) {
                    HapticUtil.performStrongDoubleHaptic(context)
                    val root = currentView.rootView
                    val navBottom = androidx.core.view.ViewCompat.getRootWindowInsets(root)
                        ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
                    val toolbarCenter = 44f * root.resources.displayMetrics.density
                    AppRipple.fire(Offset(root.width / 2f, root.height - navBottom - toolbarCenter))
                    showHelp = true
                }
            }
        }

    LifecycleResumeEffect(enabled) {
        if (enabled) detector.start()
        onPauseOrDispose { detector.stop() }
    }

    if (showHelp) {
        FeatureHelpSheet(feature, viewModel) { showHelp = false }
    }
}

private object ShakeHostStack {
    val hosts = mutableListOf<Any>()
    fun isTop(token: Any) = hosts.lastOrNull() === token
}

@Composable
fun FeatureHelpSheet(
    feature: Feature?,
    viewModel: MainViewModel = viewModel(),
    onDismissRequest: () -> Unit,
) {
    val customHelp = feature?.let { FeatureCustomHelp.forFeature(it.id) }
    if (customHelp != null) {
        customHelp.sheet(onDismissRequest)
    } else if (feature != null) {
        FeatureHelpBottomSheet(
            onDismissRequest = onDismissRequest,
            feature = feature,
            viewModel = viewModel,
        )
    } else {
        InstructionsBottomSheet(onDismissRequest = onDismissRequest)
    }
}

@Composable
fun WhatIsThisButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(
            onClick = {
                HapticUtil.performUIHaptic(view)
                onClick()
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_help_24),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.what_is_this))
        }
    }
}
