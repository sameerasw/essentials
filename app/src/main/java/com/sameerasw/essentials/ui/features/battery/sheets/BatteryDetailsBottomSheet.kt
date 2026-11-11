/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Battery
 * File: BatteryDetailsBottomSheet.kt
 * Description: UI component and settings composable for Battery feature domain.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.battery.BatteryAppsTabContent
import com.sameerasw.essentials.ui.components.battery.BatteryInfoTabContent
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.utils.BatteryStatsUtil
import com.sameerasw.essentials.utils.BatteryUsageApp
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.battery.BatteryDetails
import com.sameerasw.essentials.utils.battery.BatteryInfoUtil
import com.sameerasw.essentials.utils.battery.ChargingMode
import com.sameerasw.essentials.utils.battery.ChargingModeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun BatteryDetailsBottomSheet(
    initialDetails: BatteryDetails,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var batteryDetails by remember { mutableStateOf(initialDetails) }
    var isLoadingAdvanced by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAllApps by remember { mutableStateOf(false) }

    var showPercentage by remember { mutableStateOf(true) }

    var usageApps by remember { mutableStateOf<List<BatteryUsageApp>>(emptyList()) }

    DisposableEffect(context) {
        val receiver =
            object : android.content.BroadcastReceiver() {
                override fun onReceive(
                    ctx: android.content.Context,
                    intent: android.content.Intent,
                ) {
                    if (intent.action == android.content.Intent.ACTION_BATTERY_CHANGED ||
                        intent.action == android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
                    ) {
                        val freshBasic = BatteryInfoUtil.getBasicDetails(ctx)
                        batteryDetails =
                            batteryDetails.copy(
                                level = freshBasic.level,
                                scale = freshBasic.scale,
                                status = freshBasic.status,
                                health = freshBasic.health,
                                plugged = freshBasic.plugged,
                                voltage = freshBasic.voltage,
                                temperature = freshBasic.temperature,
                                technology = freshBasic.technology,
                                isPresent = freshBasic.isPresent,
                            )
                    }
                }
            }
        val filter =
            android.content.IntentFilter().apply {
                addAction(android.content.Intent.ACTION_BATTERY_CHANGED)
                addAction(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
            }
        }
    }

    LaunchedEffect(selectedTab) {
        withContext(Dispatchers.IO) {
            val updated = BatteryInfoUtil.fetchAdvancedDetails(context, initialDetails)
            val parsedApps = BatteryStatsUtil.parseUsageApps(context)
            withContext(Dispatchers.Main) {
                batteryDetails = updated
                usageApps = parsedApps
                isLoadingAdvanced = false
            }
        }

        if (selectedTab == 0) {
            while (kotlinx.coroutines.currentCoroutineContext().let { true }) {
                kotlinx.coroutines.delay(5000)
                withContext(Dispatchers.IO) {
                    val freshBasic = BatteryInfoUtil.getBasicDetails(context)
                    val updated = BatteryInfoUtil.fetchAdvancedDetails(context, freshBasic)
                    withContext(Dispatchers.Main) {
                        batteryDetails = updated
                    }
                }
            }
        }
    }

    val isCharging = batteryDetails.status == android.os.BatteryManager.BATTERY_STATUS_CHARGING
    val hasChargingModePermission = remember { ChargingModeUtil.hasPermission(context) }
    val showChargingModePicker = hasChargingModePermission
    var chargingMode by remember { mutableStateOf<ChargingMode?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(showChargingModePicker) {
        if (showChargingModePicker) {
            withContext(Dispatchers.IO) {
                val mode = ChargingModeUtil.getMode(context)
                withContext(Dispatchers.Main) { chargingMode = mode }
            }
        }
    }
    val isPowerSave = remember { DeviceUtils.isPowerSaveMode(context) }
    val iconRes =
        BatteryInfoUtil.getBatteryIconRes(
            context = context,
            level = batteryDetails.level,
            isCharging = isCharging,
            status = batteryDetails.status,
            health = batteryDetails.health,
            isPresent = batteryDetails.isPresent,
            isPowerSave = isPowerSave,
        )

    val isTranslationModeActive by com.sameerasw.essentials.translation.TranslationManager.isTranslationModeEnabled
    var showTabMenu by remember { mutableStateOf(false) }
    var tabTranslationSheetKey by remember { mutableStateOf<String?>(null) }

    val tabResIds =
        remember {
            listOf(
                R.string.label_battery_tab_info,
                R.string.label_battery_tab_apps,
            )
        }
    val tabLabels = tabResIds.map { stringResource(it) }

    EssentialsBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .animateContentSize()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (selectedTab == 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(88.dp),
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${batteryDetails.level}%",
                        style =
                            MaterialTheme.typography.displayMedium.copy(
                                fontFamily =
                                    androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(
                                            R.font.google_sans_flex,
                                            variationSettings =
                                                androidx.compose.ui.text.font.FontVariation.Settings(
                                                    androidx.compose.ui.text.font.FontVariation
                                                        .width(150f),
                                                    androidx.compose.ui.text.font.FontVariation.weight(
                                                        FontWeight.Normal.weight,
                                                    ),
                                                    androidx.compose.ui.text.font.FontVariation.Setting(
                                                        "ROND",
                                                        100f,
                                                    ),
                                                ),
                                        ),
                                    ),
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                com.sameerasw.essentials.ui.components.battery.TopAppsBreakdownHeader(
                    usageApps = usageApps,
                )
            }

            AnimatedVisibility(
                visible = showChargingModePicker && chargingMode != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                val currentMode = chargingMode
                if (currentMode != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TranslatableText(
                            stringResId = R.string.label_charging_mode_title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                        RoundedCardContainer {
                            SegmentedPicker(
                                items = ChargingMode.entries,
                                selectedItem = currentMode,
                                onItemSelected = { mode ->
                                    chargingMode = mode
                                    coroutineScope.launch(Dispatchers.IO) {
                                        ChargingModeUtil.setMode(context, mode)
                                    }
                                },
                                labelProvider = { mode ->
                                    when (mode) {
                                        ChargingMode.OFF -> context.getString(R.string.label_charging_mode_off)
                                        ChargingMode.ADAPTIVE -> context.getString(R.string.adaptive_charging)
                                        ChargingMode.LIMITED -> context.getString(R.string.label_charging_mode_limited)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                title = R.string.label_charging_mode_title,
                            )
                        }
                    }
                }
            }

            RoundedCardContainer {
                SegmentedPicker(
                    items = tabResIds,
                    selectedItem = tabResIds[selectedTab],
                    onItemSelected = { selectedTab = tabResIds.indexOf(it) },
                    labelProvider = { context.getString(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val targetTabKey = tabTranslationSheetKey
            if (targetTabKey != null) {
                val resolvedTabKey =
                    remember(targetTabKey) {
                        com.sameerasw.essentials.translation.TranslationManager.resolveKey(
                            context,
                            targetTabKey,
                        ) ?: targetTabKey
                    }
                com.sameerasw.essentials.translation.ui.TranslationBottomSheet(
                    stringKey = resolvedTabKey,
                    onDismissRequest = { tabTranslationSheetKey = null },
                )
            }

            when (selectedTab) {
                0 ->
                    BatteryInfoTabContent(
                        batteryDetails = batteryDetails,
                        isLoadingAdvanced = isLoadingAdvanced,
                        onRefresh = {
                            val freshBasic = BatteryInfoUtil.getBasicDetails(context)
                            batteryDetails = BatteryInfoUtil.fetchAdvancedDetails(context, freshBasic)
                        },
                    )

                1 ->
                    BatteryAppsTabContent(
                        isLoadingAdvanced = isLoadingAdvanced,
                        usageApps = usageApps,
                        showAllApps = showAllApps,
                        onToggleShowAll = { showAllApps = !showAllApps },
                        showPercentage = showPercentage,
                        onToggleUnit = { showPercentage = !showPercentage },
                        view = view,
                        currentLevel = batteryDetails.level,
                        chargeTimeRemainingMs = batteryDetails.chargeTimeRemainingMs,
                        avgCurrentMa = batteryDetails.currentAvgMa,
                        isPlugged = batteryDetails.plugged > 0,
                    )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
