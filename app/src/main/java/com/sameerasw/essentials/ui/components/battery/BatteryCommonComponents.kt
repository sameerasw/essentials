package com.sameerasw.essentials.ui.components.battery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.HapticUtil
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SectionHeaderTitle(
    title: Any,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    val isTranslationModeActive by com.sameerasw.essentials.translation.TranslationManager.isTranslationModeEnabled
    var showMenu by remember { mutableStateOf(false) }
    var translationSheetKey by remember { mutableStateOf<String?>(null) }

    val displayTitle = when (title) {
        is Int -> stringResource(title)
        is String -> title
        else -> title.toString()
    }

    Box(modifier = modifier) {
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .combinedClickable(
                    onClick = {},
                    onLongClick = if (isTranslationModeActive) {
                        {
                            HapticUtil.performVirtualKeyHaptic(view)
                            showMenu = true
                        }
                    } else null
                )
                .padding(start = 8.dp)
        )

        com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            com.sameerasw.essentials.translation.ui.TranslationMenuItems(
                title = title,
                onSelectKey = { key ->
                    showMenu = false
                    translationSheetKey = key
                }
            )
        }
    }

    val keyForSheet1 = translationSheetKey
    if (keyForSheet1 != null) {
        val resolvedKey = remember(keyForSheet1) {
            com.sameerasw.essentials.translation.TranslationManager.resolveKey(context, keyForSheet1) ?: keyForSheet1
        }
        com.sameerasw.essentials.translation.ui.TranslationBottomSheet(
            stringKey = resolvedKey,
            onDismissRequest = { translationSheetKey = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfoDetailRow(
    title: Any,
    value: String,
    iconRes: Int,
    onClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    val isTranslationModeActive by com.sameerasw.essentials.translation.TranslationManager.isTranslationModeEnabled
    var showMenu by remember { mutableStateOf(false) }
    var translationSheetKey by remember { mutableStateOf<String?>(null) }

    val displayTitle = when (title) {
        is Int -> stringResource(title)
        is String -> title
        else -> title.toString()
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceBright,
                    shape = Shapes.extraSmall
                )
                .combinedClickable(
                    onClick = {
                        if (onClick != null) {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onClick()
                        }
                    },
                    onLongClick = if (isTranslationModeActive) {
                        {
                            HapticUtil.performVirtualKeyHaptic(view)
                            showMenu = true
                        }
                    } else null
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    fun parseNum(s: String): Double? {
                        val digits = s.replace("-", "").replace(Regex("[^0-9.]"), "")
                        return digits.toDoubleOrNull()
                    }
                    val oldVal = parseNum(initialState)
                    val newVal = parseNum(targetState)
                    val isIncreasing = if (oldVal != null && newVal != null) newVal > oldVal else true

                    if (isIncreasing) {
                        (slideInVertically { height -> -height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> height } + fadeOut())
                    } else {
                        (slideInVertically { height -> height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                    }
                },
                label = "info_row_value"
            ) { targetVal ->
                Text(
                    text = targetVal,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            com.sameerasw.essentials.translation.ui.TranslationMenuItems(
                title = title,
                onSelectKey = { key ->
                    showMenu = false
                    translationSheetKey = key
                }
            )
        }
    }

    val keyForSheet2 = translationSheetKey
    if (keyForSheet2 != null) {
        val resolvedKey = remember(keyForSheet2) {
            com.sameerasw.essentials.translation.TranslationManager.resolveKey(context, keyForSheet2) ?: keyForSheet2
        }
        com.sameerasw.essentials.translation.ui.TranslationBottomSheet(
            stringKey = resolvedKey,
            onDismissRequest = { translationSheetKey = null }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteryLoadingIndicatorCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceBright,
                shape = Shapes.extraSmall
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearWavyProgressIndicator(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BatteryUsageBreakdownHeader(
    appsPct: Float,
    systemPct: Float,
    otherPct: Float,
    activeTab: Int // 1: Apps, 2: System
) {
    val safeApps = appsPct.coerceIn(0f, 100f)
    val safeSystem = systemPct.coerceIn(0f, 100f)
    val safeOther = otherPct.coerceIn(0f, 100f)

    val animatedAppsWeight by animateFloatAsState(targetValue = safeApps.coerceAtLeast(1f), label = "apps_weight")
    val animatedSystemWeight by animateFloatAsState(targetValue = safeSystem.coerceAtLeast(1f), label = "system_weight")
    val animatedOtherWeight by animateFloatAsState(targetValue = safeOther.coerceAtLeast(1f), label = "other_weight")

    // Colors: Only selected tab gets Primary accent color, all unselected sections use outlineVariant
    val appsColor = if (activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val systemColor = if (activeTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val otherColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Multi-segment progress bar (Fully rounded outer ends, connected extraSmall inner joints)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(CircleShape),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(animatedAppsWeight)
                        .fillMaxHeight()
                        .clip(ButtonGroupDefaults.connectedLeadingButtonShapes().shape)
                        .background(appsColor)
                )
                Box(
                    modifier = Modifier
                        .weight(animatedSystemWeight)
                        .fillMaxHeight()
                        .clip(ButtonGroupDefaults.connectedMiddleButtonShapes().shape)
                        .background(systemColor)
                )
                Box(
                    modifier = Modifier
                        .weight(animatedOtherWeight)
                        .fillMaxHeight()
                        .clip(ButtonGroupDefaults.connectedTrailingButtonShapes().shape)
                        .background(otherColor)
                )
            }

            // Legend row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BreakdownLegendItem(
                    label = stringResource(R.string.label_battery_tab_apps),
                    percentage = safeApps,
                    isSelected = activeTab == 1
                )
                BreakdownLegendItem(
                    label = stringResource(R.string.label_battery_tab_system),
                    percentage = safeSystem,
                    isSelected = activeTab == 2
                )
                BreakdownLegendItem(
                    label = stringResource(R.string.label_battery_other),
                    percentage = safeOther,
                    isSelected = false
                )
            }
        }
    }
}

@Composable
private fun BreakdownLegendItem(
    label: String,
    percentage: Float,
    isSelected: Boolean
) {
    Text(
        text = "$label ${String.format(Locale.getDefault(), "%.0f%%", percentage)}",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
