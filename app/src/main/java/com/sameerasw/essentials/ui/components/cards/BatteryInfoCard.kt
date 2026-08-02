package com.sameerasw.essentials.ui.components.cards

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.sheets.BatteryDetailsBottomSheet
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.BatteryDetails
import com.sameerasw.essentials.utils.BatteryInfoUtil
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShellUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BatteryInfoCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showSheet by remember { mutableStateOf(false) }

    val hasPermission = remember { ShellUtils.hasPermission(context) }
    var batteryDetails by remember { mutableStateOf(BatteryInfoUtil.getBasicDetails(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED || intent.action == android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    batteryDetails = BatteryInfoUtil.getBasicDetails(ctx)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
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

    val isCharging = batteryDetails.status == BatteryManager.BATTERY_STATUS_CHARGING
    val isPowerSave = DeviceUtils.isPowerSaveMode(context)
    val iconRes = BatteryInfoUtil.getBatteryIconRes(
        context = context,
        level = batteryDetails.level,
        isCharging = isCharging,
        status = batteryDetails.status,
        health = batteryDetails.health,
        isPresent = batteryDetails.isPresent,
        isPowerSave = isPowerSave
    )

    if (showSheet) {
        BatteryDetailsBottomSheet(
            initialDetails = batteryDetails,
            onDismiss = { showSheet = false }
        )
    }

    val isTranslationModeActive by com.sameerasw.essentials.translation.TranslationManager.isTranslationModeEnabled
    var showMenu by remember { mutableStateOf(false) }
    var translationSheetKey by remember { mutableStateOf<String?>(null) }

    val onClickAction = {
        if (hasPermission) {
            HapticUtil.performVirtualKeyHaptic(view)
            showSheet = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceBright,
                shape = Shapes.extraSmall
            )
            .combinedClickable(
                onClick = onClickAction,
                onLongClick = if (isTranslationModeActive) {
                    {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showMenu = true
                    }
                } else null
            )
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "${batteryDetails.level}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.label_device_battery),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (hasPermission) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_chevron_right_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(20.dp)
            )
        }

        com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            com.sameerasw.essentials.translation.ui.TranslationMenuItems(
                title = R.string.label_device_battery,
                onSelectKey = { key ->
                    showMenu = false
                    translationSheetKey = key
                }
            )
        }
    }

    val targetKey = translationSheetKey
    if (targetKey != null) {
        val resolvedKey = remember(targetKey) {
            com.sameerasw.essentials.translation.TranslationManager.resolveKey(context, targetKey) ?: targetKey
        }
        com.sameerasw.essentials.translation.ui.TranslationBottomSheet(
            stringKey = resolvedKey,
            onDismissRequest = { translationSheetKey = null }
        )
    }
}
