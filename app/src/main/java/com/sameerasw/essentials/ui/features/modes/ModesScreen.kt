/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModesScreen.kt
 * Description: Lists Essentials modes and switches them.
 */

package com.sameerasw.essentials.ui.features.modes

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.modes.Mode
import com.sameerasw.essentials.domain.modes.ModeIcons
import com.sameerasw.essentials.domain.modes.ModesController
import com.sameerasw.essentials.domain.modes.ModesRepository
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.PullTitleHeader
import com.sameerasw.essentials.ui.components.rememberPullTitleHeaderState
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.cards.PermissionCard
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.scrollMotionBlur
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ModesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onNewModeClick: () -> Unit,
    onModeClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val modes by ModesRepository.modes.collectAsState()
    val activeIds by ModesController.activeModeIds.collectAsState()
    val hasAccess by viewModel.isNotificationPolicyAccessGranted

    LifecycleResumeEffect(hasAccess) {
        scope.launch(Dispatchers.IO) { ModesController.sync(appContext) }
        onPauseOrDispose {}
    }

    val isMotionBlurEnabled =
        remember(context) {
            context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).getBoolean("motion_blur", false)
        }
    val lazyListState = rememberLazyListState()
    val headerState = rememberPullTitleHeaderState()

    LazyColumn(
        state = lazyListState,
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(headerState.nestedScrollConnection)
                .scrollMotionBlur(lazyListState, enabled = isMotionBlurEnabled),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = 16.dp,
                end = 16.dp,
            ),
    ) {
        item {
            PullTitleHeader(
                title = stringResource(R.string.tab_modes),
                state = headerState,
                caption = stringResource(R.string.modes_active_count, activeIds.size),
            )
        }

        when {
            !ModesController.isSupported -> item {
                CenteredMessage(R.string.modes_unsupported)
            }

            !hasAccess -> item {
                PermissionCard(
                    iconRes = R.drawable.rounded_do_not_disturb_on_24,
                    title = R.string.perm_notif_policy_title,
                    dependentFeatures = listOf(R.string.tab_modes),
                    isGranted = false,
                    onActionClick = { PermissionUtils.openNotificationPolicySettings(context) },
                )
            }

            modes.isEmpty() -> item {
                CenteredMessage(R.string.modes_empty) {
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onNewModeClick()
                        },
                    ) {
                        TranslatableText(R.string.action_new_mode)
                    }
                }
            }

            else -> item {
                RoundedCardContainer(spacing = 2.dp) {
                    modes.forEach { mode ->
                        ModeRow(
                            mode = mode,
                            isActive = mode.id in activeIds,
                            onClick = { onModeClick(mode.id) },
                            onActiveChange = { active ->
                                scope.launch(Dispatchers.IO) { ModesController.setActive(appContext, mode.id, active) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeRow(
    mode: Mode,
    isActive: Boolean,
    onClick: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
) {
    val color =
        remember(mode.colorHex) { runCatching { Color(mode.colorHex.toColorInt()) }.getOrNull() }
            ?: MaterialTheme.colorScheme.primaryContainer

    IconToggleItem(
        title = mode.name,
        isChecked = isActive,
        onCheckedChange = onActiveChange,
        enabled = mode.isEnabled,
        onClick = onClick,
        onDisabledClick = onClick,
        leadingIcon = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(ModeIcons.resolve(mode.iconName)),
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp),
                )
            }
        },
    )
}

@Composable
private fun CenteredMessage(
    messageRes: Int,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TranslatableText(stringResId = messageRes)
        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            action()
        }
    }
}
