package com.sameerasw.essentials.utils

import androidx.annotation.DrawableRes
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository

data class BluetoothDeviceIcon(val id: String, val label: String, @DrawableRes val res: Int)

data class BluetoothDeviceIconGroup(val title: String, val icons: List<BluetoothDeviceIcon>)

object BluetoothDeviceIcons {
    val groups: List<BluetoothDeviceIconGroup> = listOf(
        BluetoothDeviceIconGroup(
            "Audio",
            listOf(
                BluetoothDeviceIcon("headphones", "Headphones", R.drawable.rounded_headphones_24),
                BluetoothDeviceIcon("earbuds_2", "Earbuds", R.drawable.rounded_earbuds_2_24),
                BluetoothDeviceIcon("earbud_left", "Left earbud", R.drawable.rounded_earbud_left_24),
                BluetoothDeviceIcon("earbud_right", "Right earbud", R.drawable.rounded_earbud_right_24),
                BluetoothDeviceIcon("earbud_case", "Earbud case", R.drawable.rounded_earbud_case_24),
                BluetoothDeviceIcon("earbuds", "Buds", R.drawable.rounded_earbuds_24),
                BluetoothDeviceIcon("headset_mic", "Headset", R.drawable.rounded_headset_mic_24),
                BluetoothDeviceIcon("speaker", "Speaker", R.drawable.rounded_speaker_24),
                BluetoothDeviceIcon("speaker_group", "Speaker group", R.drawable.rounded_speaker_group_24),
                BluetoothDeviceIcon("soundbar", "Soundbar", R.drawable.rounded_soundbar_24),
                BluetoothDeviceIcon("home_speaker", "Home speaker", R.drawable.rounded_home_speaker_24),
                BluetoothDeviceIcon("nest_audio", "Smart speaker", R.drawable.rounded_nest_audio_24),
                BluetoothDeviceIcon("hearing_aid", "Hearing aid", R.drawable.rounded_hearing_aid_24),
                BluetoothDeviceIcon("hearing", "Hearing", R.drawable.rounded_hearing_24),
                BluetoothDeviceIcon("music_note", "Music", R.drawable.rounded_music_note_24),
                BluetoothDeviceIcon("radio", "Radio", R.drawable.rounded_radio_24),
                BluetoothDeviceIcon("piano", "Piano", R.drawable.rounded_piano_24),
                BluetoothDeviceIcon("mic", "Microphone", R.drawable.rounded_mic_24),
            ),
        ),
        BluetoothDeviceIconGroup(
            "Wearables",
            listOf(
                BluetoothDeviceIcon("watch", "Watch", R.drawable.rounded_watch_24),
                BluetoothDeviceIcon("watch_wake", "Smartwatch", R.drawable.rounded_watch_wake_24),
                BluetoothDeviceIcon("fitness_center", "Fitness", R.drawable.rounded_fitness_center_24),
                BluetoothDeviceIcon("monitor_heart", "Heart monitor", R.drawable.rounded_monitor_heart_24),
                BluetoothDeviceIcon("ecg_heart", "ECG", R.drawable.rounded_ecg_heart_24),
                BluetoothDeviceIcon("directions_run", "Running", R.drawable.rounded_directions_run_24),
                BluetoothDeviceIcon("eyeglasses", "Glasses", R.drawable.rounded_eyeglasses_24),
                BluetoothDeviceIcon("view_in_ar", "VR / AR", R.drawable.rounded_view_in_ar_24),
                BluetoothDeviceIcon("scale", "Scale", R.drawable.rounded_scale_24),
            ),
        ),
        BluetoothDeviceIconGroup(
            "Input",
            listOf(
                BluetoothDeviceIcon("keyboard", "Keyboard", R.drawable.rounded_keyboard_24),
                BluetoothDeviceIcon("keyboard_alt", "Keyboard alt", R.drawable.rounded_keyboard_alt_24),
                BluetoothDeviceIcon("mouse", "Mouse", R.drawable.rounded_mouse_24),
                BluetoothDeviceIcon("trackpad_input", "Trackpad", R.drawable.rounded_trackpad_input_24),
                BluetoothDeviceIcon("stylus_note", "Stylus", R.drawable.rounded_stylus_note_24),
                BluetoothDeviceIcon("stylus", "Pen", R.drawable.rounded_stylus_24),
                BluetoothDeviceIcon("joystick", "Joystick", R.drawable.rounded_joystick_24),
                BluetoothDeviceIcon("gamepad", "Gamepad", R.drawable.rounded_gamepad_24),
                BluetoothDeviceIcon("stadia_controller", "Controller", R.drawable.rounded_stadia_controller_24),
                BluetoothDeviceIcon("sports_esports", "Gaming", R.drawable.rounded_sports_esports_24),
                BluetoothDeviceIcon("videogame_asset", "Console", R.drawable.rounded_videogame_asset_24),
                BluetoothDeviceIcon("settings_remote", "Remote", R.drawable.rounded_settings_remote_24),
                BluetoothDeviceIcon("remote_gen", "Smart remote", R.drawable.rounded_remote_gen_24),
            ),
        ),
        BluetoothDeviceIconGroup(
            "Computers & phones",
            listOf(
                BluetoothDeviceIcon("laptop_mac", "Laptop", R.drawable.rounded_laptop_mac_24),
                BluetoothDeviceIcon("laptop_windows", "Windows laptop", R.drawable.rounded_laptop_windows_24),
                BluetoothDeviceIcon("laptop_chromebook", "Chromebook", R.drawable.rounded_laptop_chromebook_24),
                BluetoothDeviceIcon("computer", "Computer", R.drawable.rounded_computer_24),
                BluetoothDeviceIcon("desktop_windows", "Desktop", R.drawable.rounded_desktop_windows_24),
                BluetoothDeviceIcon("desktop_mac", "iMac", R.drawable.rounded_desktop_mac_24),
                BluetoothDeviceIcon("monitor", "Monitor", R.drawable.rounded_monitor_24),
                BluetoothDeviceIcon("tv", "TV", R.drawable.rounded_tv_24),
                BluetoothDeviceIcon("tv_gen", "Smart TV", R.drawable.rounded_tv_gen_24),
                BluetoothDeviceIcon("smart_display", "Smart display", R.drawable.rounded_smart_display_24),
                BluetoothDeviceIcon("tablet", "Tablet", R.drawable.rounded_tablet_24),
                BluetoothDeviceIcon("tablet_android", "Android tablet", R.drawable.rounded_tablet_android_24),
                BluetoothDeviceIcon("tablet_mac", "iPad", R.drawable.rounded_tablet_mac_24),
                BluetoothDeviceIcon("smartphone", "Phone", R.drawable.rounded_smartphone_24),
                BluetoothDeviceIcon("phone_android", "Android phone", R.drawable.rounded_phone_android_24),
                BluetoothDeviceIcon("phone_iphone", "iPhone", R.drawable.rounded_phone_iphone_24),
            ),
        ),
        BluetoothDeviceIconGroup(
            "Cameras & printers",
            listOf(
                BluetoothDeviceIcon("photo_camera", "Camera", R.drawable.rounded_photo_camera_24),
                BluetoothDeviceIcon("camera", "Lens", R.drawable.rounded_camera_24),
                BluetoothDeviceIcon("videocam", "Video camera", R.drawable.rounded_videocam_24),
                BluetoothDeviceIcon("print", "Printer", R.drawable.rounded_print_24),
            ),
        ),
        BluetoothDeviceIconGroup(
            "Vehicles",
            listOf(
                BluetoothDeviceIcon("directions_car", "Car", R.drawable.rounded_directions_car_24),
                BluetoothDeviceIcon("electric_car", "Electric car", R.drawable.rounded_electric_car_24),
                BluetoothDeviceIcon("two_wheeler", "Motorbike", R.drawable.rounded_two_wheeler_24),
                BluetoothDeviceIcon("pedal_bike", "Bike", R.drawable.rounded_pedal_bike_24),
                BluetoothDeviceIcon("electric_bike", "E-bike", R.drawable.rounded_electric_bike_24),
                BluetoothDeviceIcon("electric_scooter", "Scooter", R.drawable.rounded_electric_scooter_24),
                BluetoothDeviceIcon("local_shipping", "Truck", R.drawable.rounded_local_shipping_24),
                BluetoothDeviceIcon("directions_bus", "Bus", R.drawable.rounded_directions_bus_24),
                BluetoothDeviceIcon("train", "Train", R.drawable.rounded_train_24),
                BluetoothDeviceIcon("flight", "Plane", R.drawable.rounded_flight_24),
                BluetoothDeviceIcon("directions_boat", "Boat", R.drawable.rounded_directions_boat_24),
                BluetoothDeviceIcon("sports_motorsports", "Helmet", R.drawable.rounded_sports_motorsports_24),
                BluetoothDeviceIcon("ev_station", "EV charger", R.drawable.rounded_ev_station_24),
            ),
        ),
        BluetoothDeviceIconGroup(
            "Trackers & other",
            listOf(
                BluetoothDeviceIcon("key", "Key", R.drawable.rounded_key_24),
                BluetoothDeviceIcon("nfc", "NFC", R.drawable.rounded_nfc_24),
                BluetoothDeviceIcon("sell", "Tag", R.drawable.rounded_sell_24),
                BluetoothDeviceIcon("luggage", "Luggage", R.drawable.rounded_luggage_24),
                BluetoothDeviceIcon("backpack", "Backpack", R.drawable.rounded_backpack_24),
                BluetoothDeviceIcon("pets", "Pet", R.drawable.rounded_pets_24),
                BluetoothDeviceIcon("lock", "Lock", R.drawable.rounded_lock_24),
                BluetoothDeviceIcon("usb", "USB", R.drawable.rounded_usb_24),
                BluetoothDeviceIcon("memory", "Chip", R.drawable.rounded_memory_24),
                BluetoothDeviceIcon("sensors", "Sensor", R.drawable.rounded_sensors_24),
                BluetoothDeviceIcon("thermostat", "Thermostat", R.drawable.rounded_thermostat_24),
                BluetoothDeviceIcon("lightbulb", "Light", R.drawable.rounded_lightbulb_24),
                BluetoothDeviceIcon("flashlight_on", "Flashlight", R.drawable.rounded_flashlight_on_24),
                BluetoothDeviceIcon("power", "Power", R.drawable.rounded_power_24),
                BluetoothDeviceIcon("router", "Router", R.drawable.rounded_router_24),
                BluetoothDeviceIcon("hub", "Hub", R.drawable.rounded_hub_24),
                BluetoothDeviceIcon("cast", "Cast", R.drawable.rounded_cast_24),
                BluetoothDeviceIcon("robot_2", "Robot", R.drawable.rounded_robot_2_24),
                BluetoothDeviceIcon("vacuum", "Vacuum", R.drawable.rounded_vacuum_24),
                BluetoothDeviceIcon("mop", "Mop", R.drawable.rounded_mop_24),
                BluetoothDeviceIcon("cleaning_services", "Cleaning", R.drawable.rounded_cleaning_services_24),
                BluetoothDeviceIcon("microwave", "Appliance", R.drawable.rounded_microwave_24),
                BluetoothDeviceIcon("smart_toy", "Toy", R.drawable.rounded_smart_toy_24),
                BluetoothDeviceIcon("toys", "Toys", R.drawable.rounded_toys_24),
                BluetoothDeviceIcon("health_and_safety", "Health", R.drawable.rounded_health_and_safety_24),
                BluetoothDeviceIcon("sports_soccer", "Football", R.drawable.rounded_sports_soccer_24),
                BluetoothDeviceIcon("sports_tennis", "Tennis", R.drawable.rounded_sports_tennis_24),
                BluetoothDeviceIcon("sports_basketball", "Basketball", R.drawable.rounded_sports_basketball_24),
                BluetoothDeviceIcon("sports_baseball", "Baseball", R.drawable.rounded_sports_baseball_24),
                BluetoothDeviceIcon("bluetooth", "Bluetooth", R.drawable.rounded_bluetooth_24),
                BluetoothDeviceIcon("bluetooth_connected", "Bluetooth connected", R.drawable.rounded_bluetooth_connected_24),
                BluetoothDeviceIcon("devices_other", "Other device", R.drawable.rounded_devices_other_24),
                BluetoothDeviceIcon("bed", "Bed", R.drawable.rounded_bed_24),
                BluetoothDeviceIcon("bedtime", "Sleep", R.drawable.rounded_bedtime_24),
                BluetoothDeviceIcon("restaurant", "Kitchen", R.drawable.rounded_restaurant_24),
                BluetoothDeviceIcon("dock", "Dock", R.drawable.rounded_dock_24),
                BluetoothDeviceIcon("cable", "Cable", R.drawable.rounded_cable_24),
                BluetoothDeviceIcon("light_mode", "Lamp", R.drawable.rounded_light_mode_24),
            ),
        ),
    )

    private const val KEY_OVERRIDES = "bluetooth_device_icon_overrides"

    private val byId: Map<String, BluetoothDeviceIcon> by lazy { groups.flatMap { it.icons }.associateBy { it.id } }

    private fun overrides(context: Context): Map<String, String> =
        try {
            val json = SettingsRepository(context).getString(KEY_OVERRIDES, null) ?: return emptyMap()
            Gson().fromJson(json, object : TypeToken<Map<String, String>>() {}.type) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }

    fun getOverrideId(context: Context, address: String): String? = overrides(context)[address]

    fun setOverride(context: Context, address: String, iconId: String?) {
        val next = overrides(context).toMutableMap()
        if (iconId == null) next.remove(address) else next[address] = iconId
        SettingsRepository(context).putString(KEY_OVERRIDES, Gson().toJson(next))
    }

    fun resolve(context: Context, address: String?, name: String): Int {
        val id = address?.let { overrides(context)[it] }
        return id?.let { byId[it]?.res } ?: auto(name)
    }

    fun auto(name: String): Int {
        fun has(vararg keys: String) = keys.any { name.contains(it, ignoreCase = true) }
        return when {
            has("watch", "gear", "fit", "band", "amazfit", "garmin", "galaxy ring") -> R.drawable.rounded_watch_24
            has("bud", "pod", "ear", "tws", "freebuds", "wf-") -> R.drawable.rounded_earbuds_2_24
            has("soundbar") -> R.drawable.rounded_soundbar_24
            has("speaker", "boom", "jbl", "homepod") -> R.drawable.rounded_speaker_24
            has("head", "momentum", "audio", "sound", "wh-", "xm", "buds max") -> R.drawable.rounded_headphones_24
            has("keyboard", "keys") -> R.drawable.rounded_keyboard_24
            has("mouse", "mx master") -> R.drawable.rounded_mouse_24
            has("trackpad") -> R.drawable.rounded_trackpad_input_24
            has("controller", "gamepad", "dualsense", "dualshock", "xbox", "joy-con") -> R.drawable.rounded_stadia_controller_24
            has("pen", "stylus") -> R.drawable.rounded_stylus_note_24
            has("macbook", "laptop", "notebook", "thinkpad") -> R.drawable.rounded_laptop_mac_24
            has("desktop", "computer") -> R.drawable.rounded_computer_24
            has("tv", "bravia", "roku", "chromecast") -> R.drawable.rounded_tv_24
            has("ipad") -> R.drawable.rounded_tablet_android_24
            has("iphone", "pixel", "galaxy", "phone", "oneplus", "xiaomi") -> R.drawable.rounded_smartphone_24
            has("car", "bmw", "audi", "tesla", "toyota", "honda") -> R.drawable.rounded_directions_car_24
            has("tag", "tile", "airtag", "tracker", "key") -> R.drawable.rounded_sell_24
            has("print") -> R.drawable.rounded_print_24
            has("glass", "ray-ban", "vr", "quest") -> R.drawable.rounded_eyeglasses_24
            else -> R.drawable.rounded_bluetooth_24
        }
    }
}
