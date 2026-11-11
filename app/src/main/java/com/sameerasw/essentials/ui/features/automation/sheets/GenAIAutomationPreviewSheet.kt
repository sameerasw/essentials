/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Automation
 * File: GenAIAutomationPreviewSheet.kt
 * Description: UI component and settings composable for Automation feature domain.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.genai.AutomationSuggestion
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenAIAutomationPreviewSheet(
    suggestion: AutomationSuggestion,
    onConfirm: (AutomationSuggestion) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val context = LocalContext.current

    EssentialsBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
        ) {
            // Header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_auto_awesome_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                TranslatableText(
                    stringResId = R.string.diy_genai_preview_title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Preview Container using standard RoundedCardContainer
            RoundedCardContainer {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                ) {
                    if (!suggestion.explanation.isNullOrEmpty()) {
                        Text(
                            text = suggestion.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val triggerOrStateTitle =
                        suggestion.triggerType
                            ?: suggestion.stateType
                            ?: suggestion.title.ifEmpty { suggestion.type }

                    val iconRes =
                        when {
                            suggestion.triggerType != null -> R.drawable.rounded_bolt_24
                            suggestion.stateType != null -> R.drawable.rounded_toggle_on_24
                            else -> R.drawable.rounded_apps_24
                        }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = triggerOrStateTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_arrow_cool_down_24),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (suggestion.actionTypes.isNotEmpty()) {
                            suggestion.actionTypes.forEach { actionName ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_play_arrow_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = actionName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                TranslatableText(
                                    stringResId = R.string.diy_no_actions_defined,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(14.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Disclaimer
            TranslatableText(
                stringResId = R.string.diy_genai_disclaimer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDismiss()
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(48.dp),
                ) {
                    TranslatableText(stringResId = R.string.action_dismiss)
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onConfirm(suggestion)
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(48.dp),
                ) {
                    TranslatableText(stringResId = R.string.action_create)
                }
            }
        }
    }
}
