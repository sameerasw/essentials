package com.sameerasw.essentials.island.plugins.devices

import com.sameerasw.essentials.island.ui.components.islandAccent

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.BluetoothDeviceOrder
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.soften
import com.sameerasw.essentials.island.ui.components.BatteryGlyph
import com.sameerasw.essentials.island.ui.components.BatteryRing
import com.sameerasw.essentials.island.ui.components.IslandIcon

class DevicesPlugin : BaseIslandPlugin() {
    override val id = "devices"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_DEVICES,
        SettingsRepository.KEY_ISLAND_LINE_STAGE_ENABLED,
        SettingsRepository.KEY_ISLAND_BATTERY_STYLE,
        SettingsRepository.KEY_ISLAND_DEVICES_BATTERY_STYLE,
        SettingsRepository.KEY_ISLAND_DEVICES_BATTERY_ORDER,
        SettingsRepository.KEY_ISLAND_BATTERY_PERCENTAGE,
        SettingsRepository.KEY_ISLAND_BATTERY_PERCENTAGE_CONDITIONAL,
        SettingsRepository.KEY_ISLAND_DEVICES_BATTERY_ONLY_LOW,
        SettingsRepository.KEY_DUO_BATTERY_LOW_COLOR_ENABLED,
        SettingsRepository.KEY_ISLAND_BATTERY_IDLE_COLOR_ENABLED,
        SettingsRepository.KEY_ISLAND_BATTERY_IDLE_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_LOW_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_CRITICAL_COLOR_ENABLED,
        SettingsRepository.KEY_DUO_BATTERY_CRITICAL_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_LOW_LEVEL,
        SettingsRepository.KEY_DUO_BATTERY_CRITICAL_LEVEL,
    )

    private class BatteryDevice(val name: String, val iconRes: Int, val level: Int)

    private class DeviceEvent(val address: String, val connected: Boolean, val name: String, val iconRes: Int, val battery: Int)

    private var event: DeviceEvent? = null
    private var batteryDevice: BatteryDevice? = null
    private var registered = false
    private val pendingDisconnects = mutableMapOf<String, Runnable>()
    private val pendingConnects = mutableMapOf<String, Runnable>()
    private val announcedConnected = mutableSetOf<String>()
    private val clearEvent = Runnable {
        event = null
        render()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val device = intent?.deviceExtra() ?: return
            ctx?.mainHandler?.postDelayed({ refreshBattery() }, BATTERY_SETTLE_MS)
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val pending = pendingDisconnects.remove(device.address)
                    if (pending != null) {
                        ctx?.mainHandler?.removeCallbacks(pending)
                    } else {
                        pendingConnects.remove(device.address)?.let { ctx?.mainHandler?.removeCallbacks(it) }
                        val confirm = Runnable {
                            pendingConnects.remove(device.address)
                            if (isConnected(device)) {
                                announcedConnected += device.address
                                announce(device, connected = true)
                            }
                        }
                        pendingConnects[device.address] = confirm
                        ctx?.mainHandler?.postDelayed(confirm, CONNECT_SETTLE_MS)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val unconfirmed = pendingConnects.remove(device.address)
                    if (unconfirmed != null) {
                        ctx?.mainHandler?.removeCallbacks(unconfirmed)
                        return
                    }
                    pendingDisconnects.remove(device.address)?.let { ctx?.mainHandler?.removeCallbacks(it) }
                    val confirm = Runnable {
                        pendingDisconnects.remove(device.address)
                        if (announcedConnected.remove(device.address)) announce(device, connected = false)
                    }
                    pendingDisconnects[device.address] = confirm
                    ctx?.mainHandler?.postDelayed(confirm, DISCONNECT_SETTLE_MS)
                }
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val current = event ?: return
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (current.address != device.address || !current.connected || level < 0) return
                    event = DeviceEvent(current.address, true, current.name, current.iconRes, level)
                    render()
                }
            }
        }
    }

    override fun onStart() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(ACTION_BATTERY_LEVEL_CHANGED)
            }
            context.registerReceiver(receiver, filter)
            registered = true
            seedConnected()
        } catch (_: Exception) {
        }
    }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(clearEvent)
        pendingDisconnects.values.forEach { ctx?.mainHandler?.removeCallbacks(it) }
        pendingDisconnects.clear()
        pendingConnects.values.forEach { ctx?.mainHandler?.removeCallbacks(it) }
        pendingConnects.clear()
        announcedConnected.clear()
        event = null
        batteryDevice = null
        if (registered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
            registered = false
        }
    }

    override fun refresh() {
        ctx ?: return
        seedConnected()
        refreshBattery()
    }

    @SuppressLint("MissingPermission")
    private fun seedConnected() {
        if (!hasPermission()) return
        val bonded = try {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.bondedDevices
        } catch (_: SecurityException) {
            null
        }.orEmpty()
        bonded.filter { isConnected(it) }.forEach { announcedConnected += it.address }
    }

    private fun isConnected(device: BluetoothDevice): Boolean = try {
        device.javaClass.getMethod("isConnected").invoke(device) as Boolean
    } catch (_: Exception) {
        true
    }

    @SuppressLint("MissingPermission")
    private fun refreshBattery() {
        if (ctx == null) return
        batteryDevice = if (!settings.isIslandShowDevicesEnabled() || !hasPermission()) {
            null
        } else {
            val bonded = try {
                (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.bondedDevices
            } catch (_: SecurityException) {
                null
            }.orEmpty().associateBy { it.address }
            val order = BluetoothDeviceOrder.visibleAddresses(context, bonded.keys.toList())
            val onlyLow = settings.isIslandDevicesBatteryOnlyLowEnabled()
            order.firstNotNullOfOrNull { address ->
                val device = bonded[address] ?: return@firstNotNullOfOrNull null
                val level = batteryOf(device).takeIf { it >= 0 } ?: return@firstNotNullOfOrNull null
                if (onlyLow && level > settings.getDuoBatteryLowLevel()) return@firstNotNullOfOrNull null
                val name = nameOf(device)
                BatteryDevice(name, iconFor(device, name), level)
            }
        }
        render()
    }

    @SuppressLint("MissingPermission")
    private fun visibleBondedAddresses(): List<String> {
        val bonded = try {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.bondedDevices
        } catch (_: SecurityException) {
            null
        }.orEmpty().map { it.address }
        return BluetoothDeviceOrder.visibleAddresses(context, bonded)
    }

    @SuppressLint("MissingPermission")
    private fun nameOf(device: BluetoothDevice): String = try {
        device.alias ?: device.name
    } catch (_: SecurityException) {
        null
    } ?: context.getString(R.string.island_devices_unknown)

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun announce(device: BluetoothDevice, connected: Boolean) {
        val c = ctx ?: return
        if (!settings.isIslandShowDevicesEnabled() || !settings.isIslandLineStageEnabled() || !hasPermission()) return
        if (isComputer(device) || device.address !in visibleBondedAddresses()) return
        val name = nameOf(device)
        val battery = if (connected) batteryOf(device) else -1
        event = DeviceEvent(device.address, connected, name, iconFor(device, name), battery)
        val duration = settings.getIslandPeekDurationMs()
        render()
        c.request(PluginRequest.Peek(ITEM_KEY, duration))
        c.mainHandler.removeCallbacks(clearEvent)
        c.mainHandler.postDelayed(clearEvent, duration + 500L)
    }

    private fun batteryOf(device: BluetoothDevice): Int = try {
        device.javaClass.getMethod("getBatteryLevel").invoke(device) as Int
    } catch (_: Exception) {
        -1
    }

    @SuppressLint("MissingPermission")
    private fun isComputer(device: BluetoothDevice): Boolean =
        try {
            device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER
        } catch (_: SecurityException) {
            false
        }

    private fun iconFor(device: BluetoothDevice, name: String): Int {
        if (com.sameerasw.essentials.utils.BluetoothDeviceIcons.getOverrideId(context, device.address) != null) {
            return com.sameerasw.essentials.utils.BluetoothDeviceIcons.resolve(context, device.address, name)
        }
        val major = try {
            device.bluetoothClass?.majorDeviceClass
        } catch (_: SecurityException) {
            null
        }
        return when {
            name.contains("watch", true) || name.contains("gear", true) || name.contains("fit", true) ||
                major == BluetoothClass.Device.Major.WEARABLE -> R.drawable.rounded_watch_24
            name.contains("bud", true) || name.contains("pod", true) || name.contains("head", true) ||
                name.contains("audio", true) || name.contains("sound", true) ||
                major == BluetoothClass.Device.Major.AUDIO_VIDEO -> R.drawable.rounded_headphones_24
            name.contains("keyboard", true) -> R.drawable.rounded_keyboard_24
            major == BluetoothClass.Device.Major.COMPUTER -> R.drawable.rounded_laptop_mac_24
            else -> R.drawable.rounded_bluetooth_24
        }
    }

    private fun render() {
        if (ctx == null || !settings.isIslandShowDevicesEnabled()) {
            publish(null)
            return
        }
        val iconStyle = settings.getIslandDevicesBatteryStyle() == SettingsRepository.ISLAND_BATTERY_STYLE_ICON
        val items = mutableListOf<IslandItem>()
        batteryDevice?.let { d ->
            val color = levelColor(d.level)
            val showLevel = settings.isIslandBatteryPercentageEnabled() &&
                (!settings.isIslandBatteryPercentageConditional() || color != null)
            items += IslandItem(
                key = BATTERY_KEY,
                priority = IslandPriority.DEVICES,
                placement = CompactPlacement.Dynamic,
                outlineAccent = color ?: idleColor(),
                compact = listOf(
                    CompactCell("devices.battery.level") { DeviceBattery(d.level, iconStyle, showLevel, color ?: idleColor()) },
                    CompactCell("devices.battery.icon") { IslandIcon(d.iconRes, size = 18.dp, tint = islandAccent()) },
                ),
                line = LineContent(
                    icon = { IslandIcon(d.iconRes, tint = islandAccent()) },
                    start = d.name,
                    end = "${d.level}%",
                ),
            )
        }
        event?.let { e ->
            val tint = if (e.connected) null else Color.White.copy(alpha = 0.6f)
            val battery = e.battery
            items += IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.DEVICES,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("devices.icon") { IslandIcon(e.iconRes, size = 18.dp, tint = tint ?: islandAccent()) },
                ),
                line = LineContent(
                    icon = { IslandIcon(e.iconRes, tint = tint ?: islandAccent()) },
                    start = e.name,
                    end = context.getString(if (e.connected) R.string.island_devices_connected else R.string.island_devices_disconnected),
                    endSlot = if (battery >= 0) {
                        { DeviceBattery(battery, iconStyle, showLevel = true, color = levelColor(battery) ?: idleColor()) }
                    } else {
                        null
                    },
                ),
            )
        }
        publish(items)
    }

    private fun idleColor(): Color? {
        if (!settings.isIslandBatteryIdleColorEnabled()) return null
        val raw = try {
            android.graphics.Color.parseColor(settings.getIslandBatteryIdleColor())
        } catch (_: Exception) {
            android.graphics.Color.WHITE
        }
        return Color(soften(raw))
    }

    private fun levelColor(level: Int): Color? {
        fun parse(hex: String, fallback: Int) = try {
            android.graphics.Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }
        val raw = when {
            level in 0..settings.getDuoBatteryCriticalLevel() && settings.isDuoBatteryCriticalColorEnabled() ->
                parse(settings.getDuoBatteryCriticalColor(), android.graphics.Color.rgb(244, 67, 54))
            level in 0..settings.getDuoBatteryLowLevel() && settings.isDuoBatteryLowColorEnabled() ->
                parse(settings.getDuoBatteryLowColor(), android.graphics.Color.rgb(255, 235, 59))
            else -> return null
        }
        return Color(soften(raw))
    }

    @Suppress("DEPRECATION")
    private fun Intent.deviceExtra(): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

    companion object {
        const val ITEM_KEY = "devices"
        const val BATTERY_KEY = "devices.battery"
        private const val BATTERY_SETTLE_MS = 1500L
        private const val DISCONNECT_SETTLE_MS = 2000L
        private const val CONNECT_SETTLE_MS = 3000L
        private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }
}

@Composable
private fun DeviceBattery(level: Int, iconStyle: Boolean, showLevel: Boolean, color: Color?) {
    if (iconStyle) {
        BatteryGlyph(level, color ?: islandAccent(), showLevel = showLevel)
    } else {
        BatteryRing(level, color ?: islandAccent(), showLevel = showLevel)
    }
}
