/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: FeatureHelpBottomSheet.kt
 * Description: Reusable core UI component for FeatureHelpBottomSheet.kt.
 */

package com.sameerasw.essentials.ui.core.sheets

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TextButton
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.registry.FeatureResetRegistry
import com.sameerasw.essentials.ui.features.consciousgate.HoldToContinueButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.cards.FeatureTagIcon
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.media.FeatureHelpMediaViewer
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun FeatureHelpBottomSheet(
    onDismissRequest: () -> Unit,
    feature: Feature,
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val view = LocalView.current
    val isUnsupported = !feature.isDeviceSupported(context)
    val customHelp = FeatureCustomHelp.forFeature(feature.id)
    var showCustomHelp by remember { mutableStateOf(false) }
    var showResetInfo by remember { mutableStateOf(false) }
    HelpSheetTracker.TrackOpen()

    if (showResetInfo) {
        AlertDialog(
            onDismissRequest = { showResetInfo = false },
            title = { Text(stringResource(R.string.feature_reset_info_title)) },
            text = { Text(stringResource(R.string.feature_reset_info_desc)) },
            confirmButton = {
                TextButton(onClick = { showResetInfo = false }) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }

    if (showCustomHelp && customHelp != null) {
        customHelp.sheet { showCustomHelp = false }
    }

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header with Icon and Title
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(
                                color = ColorUtil.getPastelColorFor(stringResource(feature.title)),
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = feature.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = ColorUtil.getVibrantColorFor(stringResource(feature.title)),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    TranslatableText(
                        stringResId = feature.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                if (feature.isBeta) {
                    FeatureTagIcon(
                        iconRes = R.drawable.rounded_science_24,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        contentDescription = stringResource(R.string.label_beta),
                        label = stringResource(R.string.label_beta),
                    )
                }
                if (isUnsupported) {
                    FeatureTagIcon(
                        iconRes = R.drawable.rounded_dangerous_24,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        contentDescription = stringResource(R.string.label_unsupported),
                        label = stringResource(R.string.label_unsupported),
                    )
                }
                if (feature.isLegacy) {
                    FeatureTagIcon(
                        iconRes = R.drawable.rounded_archive_24,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        contentDescription = stringResource(R.string.label_legacy),
                        label = stringResource(R.string.label_legacy),
                    )
                }
            }

            // Online Help Media 
            FeatureHelpMediaViewer(
                feature = feature,
                viewModel = viewModel,
            )

            // Description Body
            RoundedCardContainer(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val descRes = feature.aboutDescription ?: feature.description

                    TranslatableText(
                        stringResId = descRes,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            if (customHelp != null) {
                FilledTonalButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        showCustomHelp = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(id = customHelp.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(customHelp.label))
                }
            }

            if (feature.contributors.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.label_feature_credits),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        feature.contributors.forEach { login ->
                            AssistChip(
                                onClick = {
                                    HapticUtil.performUIHaptic(view)
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, "https://github.com/$login".toUri()),
                                    )
                                },
                                label = {
                                    Text(
                                        text = "@$login",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                                leadingIcon = {
                                    AsyncImage(
                                        model = "https://github.com/$login.png?size=64",
                                        contentDescription = login,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(24.dp).clip(CircleShape),
                                    )
                                },
                                colors =
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                border = AssistChipDefaults.assistChipBorder(true),
                            )
                        }
                    }
                }
            }

            val resetPrefixes = FeatureResetRegistry.prefixesFor(feature.id)
            if (feature.id == "Lock screen clock") {
                HoldToContinueButton(
                    waitTimeProgress = 1f,
                    onContinue = {
                        viewModel.resetLockScreenClock(context)
                        Toast.makeText(context, R.string.feature_reset_done, Toast.LENGTH_SHORT).show()
                    },
                    onHoldingChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    holdDurationMillis = 2_000L,
                    holdLabel = stringResource(R.string.feature_reset_hold),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    progressColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else if (resetPrefixes != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    HoldToContinueButton(
                        waitTimeProgress = 1f,
                        onContinue = {
                            SettingsRepository(context).resetKeysWithPrefixes(resetPrefixes)
                            viewModel.check(context)
                            Toast.makeText(context, R.string.feature_reset_done, Toast.LENGTH_SHORT).show()
                        },
                        onHoldingChange = {},
                        modifier = Modifier.weight(1f),
                        holdDurationMillis = 2_000L,
                        holdLabel = stringResource(R.string.feature_reset_hold),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        progressColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                    )
                    FilledTonalIconButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            showResetInfo = true
                        },
                        modifier = Modifier.size(width = 58.dp, height = 58.dp),
                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                        colors =
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_info_24),
                            contentDescription = stringResource(R.string.feature_reset_info_title),
                        )
                    }
                }
            }

            // Permissions
            if (feature.permissionKeys.isNotEmpty()) {
                val permissions =
                    PermissionUIHelper.getPermissionItems(
                        feature.permissionKeys,
                        context,
                        viewModel,
                    )
                if (permissions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                    ) {
                        items(permissions) { permission ->
                            AssistChip(
                                onClick = { },
                                label = {
                                    val titleText =
                                        when (val t = permission.title) {
                                            is Int -> stringResource(t)
                                            is String -> t
                                            else -> ""
                                        }
                                    Text(
                                        text = titleText,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = permission.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                colors =
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        leadingIconContentColor = MaterialTheme.colorScheme.primary,
                                    ),
                                border = AssistChipDefaults.assistChipBorder(true),
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
