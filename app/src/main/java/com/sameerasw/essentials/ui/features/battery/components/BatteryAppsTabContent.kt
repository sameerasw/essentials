/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Battery
 * File: BatteryAppsTabContent.kt
 * Description: UI component and settings composable for Battery feature domain.
 */

package com.sameerasw.essentials.ui.components.battery

import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.BatteryUsageApp
import com.sameerasw.essentials.utils.HapticUtil
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BatteryAppsTabContent(
    isLoadingAdvanced: Boolean,
    usageApps: List<BatteryUsageApp>,
    showAllApps: Boolean,
    onToggleShowAll: () -> Unit,
    showPercentage: Boolean,
    onToggleUnit: () -> Unit,
    view: View,
    currentLevel: Int = 100,
    chargeTimeRemainingMs: Long? = null,
    avgCurrentMa: Int? = null,
    isPlugged: Boolean = false,
) {
    if (isLoadingAdvanced) {
        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            BatteryLoadingIndicatorCard()
        }
    } else if (usageApps.isEmpty()) {
        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            InfoDetailRow(
                title = stringResource(R.string.label_usage_data),
                value = stringResource(R.string.label_no_data),
                iconRes = R.drawable.rounded_info_24,
            )
        }
    } else {
        val displayedApps = if (showAllApps) usageApps else usageApps.take(20)
        val totalMah = usageApps.sumOf { it.powerMah }.coerceAtLeast(0.0001)

        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            displayedApps.forEach { app ->
                val displayValue =
                    if (showPercentage) {
                        val pct = (app.powerMah / totalMah) * 100.0
                        String.format(Locale.getDefault(), "%.1f %%", pct)
                    } else {
                        String.format(Locale.getDefault(), "%.2f mAh", app.powerMah)
                    }

                val isTranslationModeActive by com.sameerasw.essentials.translation.TranslationManager.isTranslationModeEnabled
                var showMenu by remember { mutableStateOf(false) }
                var translationSheetKey by remember { mutableStateOf<String?>(null) }

                val context = LocalContext.current
                val canOpenAppInfo =
                    remember(app.packageName) {
                        app.packageName != null && app.packageName.contains(".")
                    }

                Box {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceBright,
                                    shape = Shapes.extraSmall,
                                ).combinedClickable(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        onToggleUnit()
                                    },
                                    onLongClick =
                                        if (isTranslationModeActive || canOpenAppInfo) {
                                            {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                showMenu = true
                                            }
                                        } else {
                                            null
                                        },
                                ).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_info_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        AnimatedContent(
                            targetState = displayValue,
                            transitionSpec = {
                                fun parseNum(s: String): Double? {
                                    val digits = s.replace("-", "").replace(Regex("[^0-9.]"), "")
                                    return digits.toDoubleOrNull()
                                }

                                val oldVal = parseNum(initialState)
                                val newVal = parseNum(targetState)
                                val isIncreasing =
                                    if (oldVal != null && newVal != null) newVal > oldVal else true

                                if (isIncreasing) {
                                    (slideInVertically { height -> -height } + fadeIn())
                                        .togetherWith(slideOutVertically { height -> height } + fadeOut())
                                } else {
                                    (slideInVertically { height -> height } + fadeIn())
                                        .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                                }
                            },
                            label = "app_value_anim",
                        ) { targetVal ->
                            Text(
                                text = targetVal,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        if (canOpenAppInfo && app.packageName != null) {
                            com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem(
                                text = { TranslatableText(R.string.label_app_info) },
                                onClick = {
                                    showMenu = false
                                    try {
                                        val intent =
                                            android.content
                                                .Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                                .apply {
                                                    data =
                                                        android.net.Uri.fromParts(
                                                            "package",
                                                            app.packageName,
                                                            null,
                                                        )
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_info_24),
                                        contentDescription = null,
                                    )
                                },
                            )
                        }

                        if (isTranslationModeActive) {
                            com.sameerasw.essentials.translation.ui.TranslationMenuItems(
                                title = app.appName,
                                onSelectKey = { key ->
                                    showMenu = false
                                    translationSheetKey = key
                                },
                            )
                        }
                    }
                }

                if (translationSheetKey != null) {
                    com.sameerasw.essentials.translation.ui.TranslationBottomSheet(
                        stringKey = translationSheetKey!!,
                        onDismissRequest = { translationSheetKey = null },
                    )
                }
            }
        }

        if (usageApps.size > 20) {
            com.sameerasw.essentials.ui.components.buttons.ListExpandToggleButton(
                isExpanded = showAllApps,
                onToggle = onToggleShowAll,
            )
        }
    }
}
