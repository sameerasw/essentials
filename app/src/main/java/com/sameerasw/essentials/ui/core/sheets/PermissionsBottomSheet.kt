/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: PermissionsBottomSheet.kt
 * Description: Reusable core UI component for PermissionsBottomSheet.kt.
 */

package com.sameerasw.essentials.ui.core.sheets

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sameerasw.essentials.ui.core.cards.PermissionCard
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

data class PermissionItem(
    val iconRes: Int,
    val title: Any, // Can be Int (Resource ID) or String
    val description: Any, // Can be Int or String
    val dependentFeatures: List<Any> = emptyList(), // List of Int or String
    val actionLabel: Any? = null, // Can be Int or String
    val action: (() -> Unit)? = null,
    val secondaryActionLabel: Any? = null, // Can be Int or String
    val secondaryAction: (() -> Unit)? = null,
    val isGranted: Boolean = false,
    val shizukuActionLabel: Any? = null,
    val shizukuActionEnabled: Boolean = false,
    val shizukuAction: (() -> Unit)? = null,
    val instructions: Any? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsBottomSheet(
    onDismissRequest: () -> Unit,
    featureTitle: Any, // Can be Int (Resource ID) or String
    permissions: List<PermissionItem>,
    onHelpClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showGranted by remember { mutableStateOf(false) }

    val pendingPermissions = permissions.filter { !it.isGranted }
    val grantedPermissions = permissions.filter { it.isGranted }

    LaunchedEffect(pendingPermissions.isEmpty()) {
        if (pendingPermissions.isEmpty() && permissions.isNotEmpty()) {
            onDismissRequest()
        }
    }

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.settings_section_permissions),
                    style = MaterialTheme.typography.titleLarge,
                )

                AssistChip(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onDismissRequest()
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

            if (pendingPermissions.isNotEmpty()) {
                RoundedCardContainer {
                    pendingPermissions.forEach { perm ->
                        PermissionCard(
                            iconRes = perm.iconRes,
                            title = perm.title,
                            dependentFeatures = emptyList(),
                            actionLabel = perm.actionLabel ?: R.string.perm_action_enable,
                            isGranted = perm.isGranted,
                            onActionClick = { perm.action?.invoke() },
                            secondaryActionLabel = perm.secondaryActionLabel,
                            onSecondaryActionClick = { perm.secondaryAction?.invoke() },
                            shizukuActionLabel = perm.shizukuActionLabel,
                            shizukuActionEnabled = perm.shizukuActionEnabled,
                            onShizukuActionClick = { perm.shizukuAction?.invoke() },
                            instructions = perm.instructions,
                            description = null,
                        )
                    }
                }
            }

            if (grantedPermissions.isNotEmpty()) {
                ListExpandToggleButton(
                    isExpanded = showGranted,
                    onToggle = {
                        HapticUtil.performUIHaptic(view)
                        showGranted = !showGranted
                    },
                    title = R.string.action_hide_granted,
                    description = R.string.action_show_granted,
                    expandedText = stringResource(R.string.action_hide_granted),
                    collapsedText = stringResource(R.string.action_show_granted),
                )

                AnimatedVisibility(
                    visible = showGranted,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    RoundedCardContainer {
                        grantedPermissions.forEach { perm ->
                            PermissionCard(
                                iconRes = perm.iconRes,
                                title = perm.title,
                                dependentFeatures = emptyList(),
                                actionLabel = perm.actionLabel ?: R.string.perm_action_enable,
                                isGranted = perm.isGranted,
                                onActionClick = { perm.action?.invoke() },
                                secondaryActionLabel = perm.secondaryActionLabel,
                                onSecondaryActionClick = { perm.secondaryAction?.invoke() },
                                shizukuActionLabel = perm.shizukuActionLabel,
                                shizukuActionEnabled = perm.shizukuActionEnabled,
                                onShizukuActionClick = { perm.shizukuAction?.invoke() },
                                instructions = perm.instructions,
                                description = null,
                            )
                        }
                    }
                }
            }
        }
    }
}
