package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.battery.BatteryAppsTabContent
import com.sameerasw.essentials.ui.components.battery.BatteryInfoTabContent
import com.sameerasw.essentials.ui.components.battery.BatterySystemTabContent
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.BatteryDetails
import com.sameerasw.essentials.utils.BatteryInfoUtil
import com.sameerasw.essentials.utils.BatteryStatsUtil
import com.sameerasw.essentials.utils.BatteryUsageApp
import com.sameerasw.essentials.utils.CpuWakeupItem
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun BatteryDetailsBottomSheet(
    initialDetails: BatteryDetails,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var batteryDetails by remember { mutableStateOf(initialDetails) }
    var isLoadingAdvanced by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAllApps by remember { mutableStateOf(false) }

    var showPercentage by remember { mutableStateOf(true) }
    var showSystemPercentage by remember { mutableStateOf(false) }

    var usageApps by remember { mutableStateOf<List<BatteryUsageApp>>(emptyList()) }
    var wakeupsList by remember { mutableStateOf<List<CpuWakeupItem>>(emptyList()) }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: android.content.Intent) {
                if (intent.action == android.content.Intent.ACTION_BATTERY_CHANGED || intent.action == android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    val freshBasic = BatteryInfoUtil.getBasicDetails(ctx)
                    batteryDetails = batteryDetails.copy(
                        level = freshBasic.level,
                        scale = freshBasic.scale,
                        status = freshBasic.status,
                        health = freshBasic.health,
                        plugged = freshBasic.plugged,
                        voltage = freshBasic.voltage,
                        temperature = freshBasic.temperature,
                        technology = freshBasic.technology,
                        isPresent = freshBasic.isPresent
                    )
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
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
            val parsedWakeups = BatteryStatsUtil.parseWakeupHistory(context)
            withContext(Dispatchers.Main) {
                batteryDetails = updated
                usageApps = parsedApps
                wakeupsList = parsedWakeups
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
    val isPowerSave = remember { DeviceUtils.isPowerSaveMode(context) }
    val iconRes = BatteryInfoUtil.getBatteryIconRes(
        context = context,
        level = batteryDetails.level,
        isCharging = isCharging,
        status = batteryDetails.status,
        health = batteryDetails.health,
        isPresent = batteryDetails.isPresent,
        isPowerSave = isPowerSave
    )

    val isTranslationModeActive by com.sameerasw.essentials.translation.TranslationManager.isTranslationModeEnabled
    var showTabMenu by remember { mutableStateOf(false) }
    var tabTranslationSheetKey by remember { mutableStateOf<String?>(null) }

    val tabResIds = remember {
        listOf(
            R.string.label_battery_tab_info,
            R.string.label_battery_tab_apps,
            R.string.label_battery_tab_system
        )
    }
    val tabLabels = tabResIds.map { stringResource(it) }

    EssentialsBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .animateContentSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val totalAppsMah = remember(usageApps) { usageApps.sumOf { it.powerMah } }
            val systemDrainMa = remember(batteryDetails.powerProfile) {
                batteryDetails.powerProfile?.values?.mapNotNull { it.toDoubleOrNull() }?.sum() ?: 0.0
            }

            // Estimate breakdown percentages (Apps vs System vs Other)
            val totalCalculated = (totalAppsMah + systemDrainMa).coerceAtLeast(1.0)
            val appsPct = ((totalAppsMah / totalCalculated) * 75.0).toFloat().coerceIn(10f, 80f)
            val systemPct = ((systemDrainMa / totalCalculated) * 75.0).toFloat().coerceIn(10f, 80f)
            val otherPct = (100f - appsPct - systemPct).coerceAtLeast(5f)

            if (selectedTab == 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${batteryDetails.level}%",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily(
                                androidx.compose.ui.text.font.Font(
                                    R.font.google_sans_flex,
                                    variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(
                                        androidx.compose.ui.text.font.FontVariation.width(150f),
                                        androidx.compose.ui.text.font.FontVariation.weight(FontWeight.Normal.weight),
                                        androidx.compose.ui.text.font.FontVariation.Setting("ROND", 100f)
                                    )
                                )
                            )
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                com.sameerasw.essentials.ui.components.battery.BatteryUsageBreakdownHeader(
                    appsPct = appsPct,
                    systemPct = systemPct,
                    otherPct = otherPct,
                    activeTab = selectedTab
                )
            }

            RoundedCardContainer{
                com.sameerasw.essentials.ui.components.pickers.SegmentedPicker(
                    items = tabResIds,
                    selectedItem = tabResIds[selectedTab],
                    onItemSelected = { selectedTab = tabResIds.indexOf(it) },
                    labelProvider = { context.getString(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val targetTabKey = tabTranslationSheetKey
            if (targetTabKey != null) {
                val resolvedTabKey = remember(targetTabKey) {
                    com.sameerasw.essentials.translation.TranslationManager.resolveKey(context, targetTabKey) ?: targetTabKey
                }
                com.sameerasw.essentials.translation.ui.TranslationBottomSheet(
                    stringKey = resolvedTabKey,
                    onDismissRequest = { tabTranslationSheetKey = null }
                )
            }

            when (selectedTab) {
                0 -> BatteryInfoTabContent(
                    batteryDetails = batteryDetails,
                    isLoadingAdvanced = isLoadingAdvanced,
                    onRefresh = {
                        val freshBasic = BatteryInfoUtil.getBasicDetails(context)
                        batteryDetails = BatteryInfoUtil.fetchAdvancedDetails(context, freshBasic)
                    }
                )
                1 -> BatteryAppsTabContent(
                    isLoadingAdvanced = isLoadingAdvanced,
                    usageApps = usageApps,
                    showAllApps = showAllApps,
                    onToggleShowAll = { showAllApps = !showAllApps },
                    showPercentage = showPercentage,
                    onToggleUnit = { showPercentage = !showPercentage },
                    view = view
                )
                2 -> BatterySystemTabContent(
                    isLoadingAdvanced = isLoadingAdvanced,
                    powerProfile = batteryDetails.powerProfile,
                    wakeupsList = wakeupsList,
                    showPercentage = showSystemPercentage,
                    onToggleUnit = { showSystemPercentage = !showSystemPercentage }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
