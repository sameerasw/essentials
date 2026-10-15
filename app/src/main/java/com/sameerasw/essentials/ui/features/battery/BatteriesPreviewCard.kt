package com.sameerasw.essentials.ui.features.battery

import android.content.Context
import android.os.BatteryManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.BatteryRingDrawer
import com.sameerasw.essentials.utils.BluetoothBatteryUtils
import com.sameerasw.essentials.utils.BluetoothDeviceIcons
import com.sameerasw.essentials.utils.BluetoothDeviceOrder
import com.sameerasw.essentials.utils.battery.ChargeLimitUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.delay

object BatteriesPreviewTrigger {
    val version = mutableIntStateOf(0)

    fun bump() {
        version.intValue++
    }
}

private class PreviewItem(
    val level: Int,
    val iconRes: Int,
    val statusIconRes: Int?,
)

private fun statusIcon(
    level: Int,
    charging: Boolean,
    shielded: Boolean = false,
): Int? =
    when {
        shielded -> R.drawable.rounded_shield_filled_24
        charging -> R.drawable.rounded_bolt_filled_24
        level <= 15 -> R.drawable.rounded_battery_android_frame_alert_24
        else -> null
    }

private fun previewItems(
    context: Context,
    maxDevices: Int,
): List<PreviewItem> {
    val settings = SettingsRepository(context)
    val keyed = mutableListOf<Pair<String, PreviewItem>>()

    val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val phoneLevel = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val phoneCharging = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
    keyed += BluetoothDeviceOrder.PHONE to PreviewItem(phoneLevel, R.drawable.rounded_mobile_24, statusIcon(phoneLevel, phoneCharging, ChargeLimitUtil.isShielded(context, phoneLevel, phoneCharging)))

    val macLevel = settings.getInt(SettingsRepository.KEY_MAC_BATTERY_LEVEL, -1)
    if (settings.getBoolean(SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED) &&
        settings.getBoolean(SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED) &&
        macLevel != -1
    ) {
        val charging = settings.getBoolean(SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING)
        keyed += BluetoothDeviceOrder.MAC to PreviewItem(macLevel, R.drawable.rounded_laptop_mac_24, statusIcon(macLevel, charging))
    }

    if (settings.isBluetoothDevicesEnabled()) {
        val json = settings.getString(SettingsRepository.KEY_BLUETOOTH_DEVICES_BATTERY)
        val devices =
            try {
                if (json.isNullOrEmpty()) emptyList() else Gson().fromJson(json, Array<BluetoothBatteryUtils.BluetoothDeviceBattery>::class.java).toList()
            } catch (_: Exception) {
                emptyList()
            }
        devices.forEach { device ->
            if (device.hasParts && settings.isBatterySeparateChannelsEnabled()) {
                device.left?.let { keyed += device.address to PreviewItem(it, R.drawable.rounded_earbud_left_24, statusIcon(it, false)) }
                device.right?.let { keyed += device.address to PreviewItem(it, R.drawable.rounded_earbud_right_24, statusIcon(it, false)) }
                device.case?.let { keyed += device.address to PreviewItem(it, R.drawable.rounded_earbud_case_24, statusIcon(it, false)) }
            } else {
                keyed += device.address to
                    PreviewItem(device.level, BluetoothDeviceIcons.resolve(context, device.address, device.name), statusIcon(device.level, false))
            }
        }
    }
    return BluetoothDeviceOrder.apply(context, keyed) { it.first }.map { it.second }.take(maxDevices)
}

@Composable
fun BatteriesPreviewCard(
    viewModel: MainViewModel,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val maxDevices = viewModel.batteryWidgetMaxDevices.intValue
    val version = BatteriesPreviewTrigger.version.intValue
    val showPercentage = remember(version) { SettingsRepository(context).isBatteryShowPercentageEnabled() }
    val bluetoothEnabled = viewModel.isBluetoothDevicesEnabled.value
    val airSyncEnabled = viewModel.isAirSyncConnectionEnabled.value
    var settled by remember { mutableIntStateOf(0) }

    LaunchedEffect(version, maxDevices, bluetoothEnabled, airSyncEnabled) {
        delay(800)
        settled++
    }

    val items = remember(version, maxDevices, bluetoothEnabled, airSyncEnabled, settled) { previewItems(context, maxDevices) }

    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val warning = Color(0xFFFFC107)

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val gap = 8.dp
        val inset = 12.dp
        val availableWidth = maxWidth - inset * 2
        val availableHeight = maxHeight - inset * 2
        var cell = 72.dp
        while (cell > 28.dp) {
            val columns = ((availableWidth + gap) / (cell + gap)).toInt().coerceAtLeast(1)
            val rows = (items.size + columns - 1) / columns
            if (rows * (cell.value + gap.value) - gap.value <= availableHeight.value) break
            cell -= 4.dp
        }
        val density = context.resources.displayMetrics.density
        val px = (cell.value * density).toInt().coerceAtLeast(32)

        FlowRow(
            modifier = Modifier.padding(inset),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically),
        ) {
            items.forEach { item ->
                val ring =
                    when {
                        item.level <= 10 -> error
                        item.level < 20 -> warning
                        else -> primary
                    }
                val bitmap =
                    remember(item.level, item.iconRes, item.statusIconRes, px, ring, onSurface, surface, showPercentage) {
                        BatteryRingDrawer.drawBatteryWidget(
                            context,
                            item.level,
                            ring.toArgb(),
                            onSurface.copy(alpha = 0.15f).toArgb(),
                            onSurface.toArgb(),
                            surface.toArgb(),
                            ContextCompat.getDrawable(context, item.iconRes),
                            item.statusIconRes?.let { ContextCompat.getDrawable(context, it) },
                            px,
                            px,
                            percentText = if (showPercentage) "${item.level}" else null,
                            percentIcon =
                                if (showPercentage && (item.statusIconRes == R.drawable.rounded_bolt_filled_24 || item.statusIconRes == R.drawable.rounded_shield_filled_24)) {
                                    ContextCompat.getDrawable(context, item.statusIconRes)
                                } else {
                                    null
                                },
                        )
                    }
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(cell))
            }
        }
    }
}
