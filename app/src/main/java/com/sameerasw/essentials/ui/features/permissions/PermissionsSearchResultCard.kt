/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Permissions
 * File: PermissionsSearchResultCard.kt
 * Description: Grouped permissions card for in-app search results with grant/granted actions and view all navigation.
 */

package com.sameerasw.essentials.ui.features.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.SettingsActivity
import com.sameerasw.essentials.ui.components.buttons.ListExpandToggleButton
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PermissionItem
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun PermissionsSearchResultCard(
    permissions: List<PermissionItem>,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    if (permissions.isEmpty()) return

    val context = LocalContext.current
    val view = LocalView.current
    var isExpanded by remember { mutableStateOf(false) }

    val visiblePermissions =
        if (!isExpanded && permissions.size > 5) {
            permissions.take(5)
        } else {
            permissions
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_section_permissions),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AssistChip(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    val intent =
                        Intent(context, SettingsActivity::class.java).apply {
                            putExtra("expand_permissions", true)
                        }
                    context.startActivity(intent)
                },
                label = {
                    Text(
                        text = stringResource(R.string.action_view_all),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.rounded_chevron_right_24),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors =
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        trailingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                border = AssistChipDefaults.assistChipBorder(true),
            )
        }

        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(durationMillis = 300)),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (permission in visiblePermissions) {
                    val resolvedTitle =
                        when (val t = permission.title) {
                            is Int -> stringResource(id = t)
                            is String -> t
                            else -> ""
                        }

                    val isGranted = permission.isGranted

                    FeatureCard(
                        title = resolvedTitle,
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            permission.action?.invoke()
                        },
                        iconRes = permission.iconRes,
                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
                        showToggle = false,
                        hasMoreSettings = false,
                        customTrailingContent = {
                            if (isGranted) {
                                OutlinedButton(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        permission.action?.invoke()
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.perm_action_granted),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        permission.action?.invoke()
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.perm_action_grant),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }

        if (permissions.size > 5) {
            ListExpandToggleButton(
                isExpanded = isExpanded,
                onToggle = { isExpanded = !isExpanded },
                title = R.string.action_show_more,
                description = R.string.action_show_less,
                expandedText = stringResource(R.string.action_show_less),
                collapsedText = stringResource(R.string.action_show_more),
                modifier =
                    Modifier
                        .padding(horizontal = 0.dp)
                        .padding(top = 4.dp, bottom = 4.dp),
            )
        }
    }
}
