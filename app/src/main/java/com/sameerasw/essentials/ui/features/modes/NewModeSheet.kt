/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: NewModeSheet.kt
 * Description: Creates a new mode, or links one the system already has.
 */

package com.sameerasw.essentials.ui.features.modes

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.modes.SystemMode
import com.sameerasw.essentials.domain.model.AppPermission
import com.sameerasw.essentials.domain.modes.Mode
import com.sameerasw.essentials.domain.modes.ModeIcons
import com.sameerasw.essentials.domain.modes.ModesController
import com.sameerasw.essentials.domain.modes.ModesRepository
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.PRESET_PASTEL_COLORS
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewModeSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var name by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf(PRESET_PASTEL_COLORS.random()) }
    var iconName by remember { mutableStateOf(ModeIcons.DEFAULT) }

    val hasAccess by viewModel.isNotificationPolicyAccessGranted
    val shizukuGranted by viewModel.isShizukuPermissionGranted
    val canLink = remember(shizukuGranted) { ModesController.canLinkSystemModes() }
    val modes by ModesRepository.modes.collectAsState()
    val takenIds = remember(modes) { modes.mapNotNull { it.linkedModeId }.toSet() }

    LifecycleResumeEffect(Unit) {
        viewModel.check(context)
        onPauseOrDispose {}
    }

    fun create(mode: Mode) {
        ModesRepository.upsert(mode)
        ModesController.requestSync(context)
        onDismiss()
    }

    fun link(systemMode: SystemMode) {
        create(Mode(colorHex = colorHex).linkedTo(systemMode))
    }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.action_new_mode,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            ModeAppearanceFields(
                name = name,
                onNameChange = { name = it },
                colorHex = colorHex,
                onColorChange = { colorHex = it },
                iconName = iconName,
                onIconChange = { iconName = it },
            )

            if (!hasAccess) ModePermissionRow(AppPermission.NOTIFICATION_POLICY, viewModel)

            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    create(Mode(name = name.trim(), colorHex = colorHex, iconName = iconName))
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_add_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                TranslatableText(R.string.action_create)
            }

            ModeSectionTitle(R.string.mode_link_title)

            if (!canLink) {
                ModePermissionRow(AppPermission.SHIZUKU, viewModel)
            } else {
                val linkable = rememberLinkableSystemModes(takenIds, refreshKey = shizukuGranted)
                val systemModes = linkable.modes
                when {
                    systemModes == null && linkable.isLoading ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }

                    systemModes.isNullOrEmpty() ->
                        TranslatableText(
                            stringResId = R.string.mode_link_none,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp),
                        )

                    else ->
                        RoundedCardContainer(spacing = 2.dp) {
                            systemModes.forEach { systemMode ->
                                val icon = rememberSystemModeIconPainter(systemMode)
                                IconToggleItem(
                                    title = systemMode.name,
                                    leadingIcon = {
                                        Icon(
                                            painter = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    description = if (systemMode.isEnabled) null else stringResource(R.string.diy_set_mode_disabled),
                                    showToggle = false,
                                    onClick = { link(systemMode) },
                                )
                            }
                        }
                }
            }
        }
    }
}
