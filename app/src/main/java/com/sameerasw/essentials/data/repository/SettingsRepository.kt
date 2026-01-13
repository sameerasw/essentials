package com.sameerasw.essentials.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingSide
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import com.sameerasw.essentials.domain.HapticFeedbackType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        const val PREFS_NAME = "essentials_prefs"
        
        // Keys
        const val KEY_WIDGET_ENABLED = "widget_enabled"
        const val KEY_STATUS_BAR_ICON_CONTROL_ENABLED = "status_bar_icon_control_enabled"
        const val KEY_MAPS_POWER_SAVING_ENABLED = "maps_power_saving_enabled"
        const val KEY_EDGE_LIGHTING_ENABLED = "edge_lighting_enabled"
        const val KEY_EDGE_LIGHTING_ONLY_SCREEN_OFF = "edge_lighting_only_screen_off"
        const val KEY_EDGE_LIGHTING_AMBIENT_DISPLAY = "edge_lighting_ambient_display"
        const val KEY_EDGE_LIGHTING_AMBIENT_SHOW_LOCK_SCREEN = "edge_lighting_ambient_show_lock_screen"
        const val KEY_EDGE_LIGHTING_SKIP_SILENT = "edge_lighting_skip_silent"
        const val KEY_EDGE_LIGHTING_SKIP_PERSISTENT = "edge_lighting_skip_persistent"
        const val KEY_EDGE_LIGHTING_STYLE = "edge_lighting_style"
        const val KEY_EDGE_LIGHTING_COLOR_MODE = "edge_lighting_color_mode"
        const val KEY_EDGE_LIGHTING_CUSTOM_COLOR = "edge_lighting_custom_color"
        const val KEY_EDGE_LIGHTING_PULSE_COUNT = "edge_lighting_pulse_count"
        const val KEY_EDGE_LIGHTING_PULSE_DURATION = "edge_lighting_pulse_duration"
        const val KEY_EDGE_LIGHTING_INDICATOR_X = "edge_lighting_indicator_x"
        const val KEY_EDGE_LIGHTING_INDICATOR_Y = "edge_lighting_indicator_y"
        const val KEY_EDGE_LIGHTING_INDICATOR_SCALE = "edge_lighting_indicator_scale"
        const val KEY_EDGE_LIGHTING_GLOW_SIDES = "edge_lighting_glow_sides"
        const val KEY_EDGE_LIGHTING_CORNER_RADIUS = "edge_lighting_corner_radius"
        const val KEY_EDGE_LIGHTING_STROKE_THICKNESS = "edge_lighting_stroke_thickness"
        const val KEY_EDGE_LIGHTING_SELECTED_APPS = "edge_lighting_selected_apps"
        
        const val KEY_BUTTON_REMAP_ENABLED = "button_remap_enabled"
        const val KEY_FLASHLIGHT_VOLUME_TOGGLE_ENABLED = "flashlight_volume_toggle_enabled" // Legacy
        const val KEY_BUTTON_REMAP_USE_SHIZUKU = "button_remap_use_shizuku"
        const val KEY_SHIZUKU_DETECTED_DEVICE_PATH = "shizuku_detected_device_path"
        const val KEY_FLASHLIGHT_TRIGGER_BUTTON = "flashlight_trigger_button" // Legacy
        const val KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF = "button_remap_vol_up_action_off"
        const val KEY_BUTTON_REMAP_VOL_UP_ACTION = "button_remap_vol_up_action" // Legacy
        const val KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF = "button_remap_vol_down_action_off"
        const val KEY_BUTTON_REMAP_VOL_DOWN_ACTION = "button_remap_vol_down_action" // Legacy
        const val KEY_BUTTON_REMAP_VOL_UP_ACTION_ON = "button_remap_vol_up_action_on"
        const val KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON = "button_remap_vol_down_action_on"
        const val KEY_BUTTON_REMAP_HAPTIC_TYPE = "button_remap_haptic_type"
        const val KEY_FLASHLIGHT_HAPTIC_TYPE = "flashlight_haptic_type" // Legacy
        
        const val KEY_DYNAMIC_NIGHT_LIGHT_ENABLED = "dynamic_night_light_enabled"
        const val KEY_DYNAMIC_NIGHT_LIGHT_SELECTED_APPS = "dynamic_night_light_selected_apps"
        
        const val KEY_SNOOZE_DEBUGGING_ENABLED = "snooze_debugging_enabled"
        const val KEY_SNOOZE_FILE_TRANSFER_ENABLED = "snooze_file_transfer_enabled"
        const val KEY_SNOOZE_CHARGING_ENABLED = "snooze_charging_enabled"
        
        const val KEY_FLASHLIGHT_ALWAYS_TURN_OFF_ENABLED = "flashlight_always_turn_off_enabled"
        const val KEY_FLASHLIGHT_FADE_ENABLED = "flashlight_fade_enabled"
        const val KEY_FLASHLIGHT_ADJUST_INTENSITY_ENABLED = "flashlight_adjust_intensity_enabled"
        const val KEY_FLASHLIGHT_GLOBAL_ENABLED = "flashlight_global_enabled"
        const val KEY_FLASHLIGHT_LIVE_UPDATE_ENABLED = "flashlight_live_update_enabled"
        const val KEY_FLASHLIGHT_LAST_INTENSITY = "flashlight_last_intensity"
        const val KEY_FLASHLIGHT_PULSE_ENABLED = "flashlight_pulse_enabled"
        const val KEY_FLASHLIGHT_PULSE_FACEDOWN_ONLY = "flashlight_pulse_facedown_only"

        const val KEY_SCREEN_LOCKED_SECURITY_ENABLED = "screen_locked_security_enabled"
        
        const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
        const val KEY_UPDATE_NOTIFICATION_ENABLED = "update_notification_enabled"
        const val KEY_LAST_UPDATE_CHECK_TIME = "last_update_check_time"
        const val KEY_CHECK_PRE_RELEASES_ENABLED = "check_pre_releases_enabled"
        
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_APP_LOCK_SELECTED_APPS = "app_lock_selected_apps"
        
        const val KEY_FREEZE_WHEN_LOCKED_ENABLED = "freeze_when_locked_enabled"
        const val KEY_FREEZE_LOCK_DELAY_INDEX = "freeze_lock_delay_index"
        const val KEY_FREEZE_AUTO_EXCLUDED_APPS = "freeze_auto_excluded_apps"
        const val KEY_FREEZE_SELECTED_APPS = "freeze_selected_apps"
        
        const val KEY_DEVELOPER_MODE_ENABLED = "developer_mode_enabled"
        const val KEY_HAPTIC_FEEDBACK_TYPE = "haptic_feedback_type"
        const val KEY_DEFAULT_TAB = "default_tab"
        const val KEY_USE_ROOT = "use_root"
    }

    // Observe changes
    fun observeKeyChanges(): Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            trySend(key)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    // General Getters
    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    fun getString(key: String, default: String? = null): String? = prefs.getString(key, default)
    fun getInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)
    fun getFloat(key: String, default: Float = 0f): Float {
        return try {
            prefs.getFloat(key, default)
        } catch (e: ClassCastException) {
            try {
                // Migrate from Int to Float if necessary
                val intValue = prefs.getInt(key, default.toInt())
                val floatValue = intValue.toFloat()
                putFloat(key, floatValue)
                floatValue
            } catch (e2: Exception) {
                default
            }
        }
    }
    fun getLong(key: String, default: Long = 0L): Long = prefs.getLong(key, default)
    
    // General Setters
    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    
    // Specific Getters with logic from ViewModel
    
    fun getNotificationLightingStyle(): NotificationLightingStyle {
        val styleName = prefs.getString(KEY_EDGE_LIGHTING_STYLE, NotificationLightingStyle.STROKE.name)
        return try {
            NotificationLightingStyle.valueOf(styleName ?: NotificationLightingStyle.STROKE.name)
        } catch (e: Exception) {
            NotificationLightingStyle.STROKE
        }
    }

    fun getNotificationLightingColorMode(): NotificationLightingColorMode {
        val colorModeName = prefs.getString(KEY_EDGE_LIGHTING_COLOR_MODE, NotificationLightingColorMode.SYSTEM.name)
        return try {
            NotificationLightingColorMode.valueOf(colorModeName ?: NotificationLightingColorMode.SYSTEM.name)
        } catch (e: Exception) {
            NotificationLightingColorMode.SYSTEM
        }
    }

    fun getNotificationLightingGlowSides(): Set<NotificationLightingSide> {
        val json = prefs.getString(KEY_EDGE_LIGHTING_GLOW_SIDES, null)
        return if (json != null) {
            val type = object : TypeToken<Set<NotificationLightingSide>>() {}.type
            try {
                gson.fromJson(json, type)
            } catch (e: Exception) {
                setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
            }
        } else {
            setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
        }
    }

    fun saveNotificationLightingGlowSides(sides: Set<NotificationLightingSide>) {
        val json = gson.toJson(sides)
        putString(KEY_EDGE_LIGHTING_GLOW_SIDES, json)
    }

    fun getFreezeAutoExcludedApps(): Set<String> {
        val json = prefs.getString(KEY_FREEZE_AUTO_EXCLUDED_APPS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, object : TypeToken<Set<String>>() {}.type) ?: emptySet()
            } catch (e: Exception) { emptySet() }
        } else emptySet()
    }
    
    fun saveFreezeAutoExcludedApps(apps: Set<String>) {
        val json = gson.toJson(apps)
        putString(KEY_FREEZE_AUTO_EXCLUDED_APPS, json)
    }

    fun getHapticFeedbackType(): HapticFeedbackType {
        val typeName = prefs.getString(KEY_HAPTIC_FEEDBACK_TYPE, HapticFeedbackType.SUBTLE.name)
        return try {
            HapticFeedbackType.valueOf(typeName ?: HapticFeedbackType.SUBTLE.name)
        } catch (e: Exception) {
            HapticFeedbackType.SUBTLE
        }
    }

    fun getDIYTab(): com.sameerasw.essentials.domain.DIYTabs {
        val tabName = prefs.getString(KEY_DEFAULT_TAB, com.sameerasw.essentials.domain.DIYTabs.ESSENTIALS.name)
        return try {
            com.sameerasw.essentials.domain.DIYTabs.valueOf(tabName ?: com.sameerasw.essentials.domain.DIYTabs.ESSENTIALS.name)
        } catch (e: Exception) {
            com.sameerasw.essentials.domain.DIYTabs.ESSENTIALS
        }
    }

    fun saveDIYTab(tab: com.sameerasw.essentials.domain.DIYTabs) {
        putString(KEY_DEFAULT_TAB, tab.name)
    }
    
    // App Selection Helper Generic
    private fun loadAppSelection(key: String): List<AppSelection> {
        val json = prefs.getString(key, null)
        return if (json != null) {
            val type = object : TypeToken<List<AppSelection>>() {}.type
            try {
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun saveAppSelection(key: String, apps: List<AppSelection>) {
        val json = gson.toJson(apps)
        putString(key, json)
    }
    
    // Feature specific App selections
    fun loadNotificationLightingSelectedApps() = loadAppSelection(KEY_EDGE_LIGHTING_SELECTED_APPS)
    fun saveNotificationLightingSelectedApps(apps: List<AppSelection>) = saveAppSelection(KEY_EDGE_LIGHTING_SELECTED_APPS, apps)
    fun updateNotificationLightingAppSelection(packageName: String, enabled: Boolean) = updateAppSelection(KEY_EDGE_LIGHTING_SELECTED_APPS, packageName, enabled)
    
    fun loadDynamicNightLightSelectedApps() = loadAppSelection(KEY_DYNAMIC_NIGHT_LIGHT_SELECTED_APPS)
    fun saveDynamicNightLightSelectedApps(apps: List<AppSelection>) = saveAppSelection(KEY_DYNAMIC_NIGHT_LIGHT_SELECTED_APPS, apps)
    fun updateDynamicNightLightAppSelection(packageName: String, enabled: Boolean) = updateAppSelection(KEY_DYNAMIC_NIGHT_LIGHT_SELECTED_APPS, packageName, enabled)
    
    fun loadAppLockSelectedApps() = loadAppSelection(KEY_APP_LOCK_SELECTED_APPS)
    fun saveAppLockSelectedApps(apps: List<AppSelection>) = saveAppSelection(KEY_APP_LOCK_SELECTED_APPS, apps)
    fun updateAppLockAppSelection(packageName: String, enabled: Boolean) = updateAppSelection(KEY_APP_LOCK_SELECTED_APPS, packageName, enabled)
    
    fun loadFreezeSelectedApps() = loadAppSelection(KEY_FREEZE_SELECTED_APPS)
    fun saveFreezeSelectedApps(apps: List<AppSelection>) = saveAppSelection(KEY_FREEZE_SELECTED_APPS, apps.filter { it.isEnabled })
    fun updateFreezeAppSelection(packageName: String, enabled: Boolean) = updateAppSelection(KEY_FREEZE_SELECTED_APPS, packageName, enabled)

    private fun updateAppSelection(key: String, packageName: String, enabled: Boolean) {
        val current = loadAppSelection(key).toMutableList()
        val index = current.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            current[index] = current[index].copy(isEnabled = enabled)
        } else {
            current.add(AppSelection(packageName, enabled))
        }
        // Special case for freeze apps to only save enabled ones is handled in saveFreezeSelectedApps
        // But here we are using generic generic save for key?
        // Wait, saveFreezeSelectedApps filters. 
        // My generic updateAppSelection calls... wait, no.
        // I should call the specific save method or generic save method?
        // If I use generic saveAppSelection(key, current), for freeze apps, I might save disabled apps if I don't filter.
        // Let's look at saveFreezeSelectedApps: it calls saveAppSelection(KEY..., apps.filter { it.isEnabled })
        
        if (key == KEY_FREEZE_SELECTED_APPS) {
             saveAppSelection(key, current.filter { it.isEnabled })
        } else {
             saveAppSelection(key, current)
        }
    }
    
    // Config Export/Import
    fun getAllConfigsAsJsonString(): String {
        return try {
            val allConfigs = mutableMapOf<String, Map<String, Map<String, Any>>>()
            val prefFiles = listOf("essentials_prefs", "caffeinate_prefs", "link_prefs", "diy_automations_prefs")

            prefFiles.forEach { fileName ->
                val p = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                val wrapperMap = mutableMapOf<String, Map<String, Any>>()
                
                p.all.forEach { (key, value) ->
                    // Skip app lists as requested
                    if (key.endsWith("_selected_apps") || key == "freeze_auto_excluded_apps") {
                        return@forEach
                    }

                    val type = when (value) {
                        is Boolean -> "Boolean"
                        is Int -> "Int"
                        is Long -> "Long"
                        is Float -> "Float"
                        is String -> "String"
                        is Set<*> -> "StringSet"
                        else -> "Unknown"
                    }
                    if (value != null && type != "Unknown") {
                        wrapperMap[key] = mapOf("type" to type, "value" to value)
                    }
                }
                allConfigs[fileName] = wrapperMap
            }

            gson.toJson(allConfigs)
        } catch (e: Exception) {
            "{}"
        }
    }

    fun exportConfigs(outputStream: java.io.OutputStream) {
        try {
            val json = getAllConfigsAsJsonString()
            outputStream.write(json.toByteArray())
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importConfigs(inputStream: java.io.InputStream): Boolean {
        return try {
            val json = inputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, Map<String, Map<String, Any>>>>() {}.type
            val allConfigs: Map<String, Map<String, Map<String, Any>>> = gson.fromJson(json, type)
            
            allConfigs.forEach { (fileName, prefWrapper) ->
                val p = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                p.edit().apply {
                    clear()
                    prefWrapper.forEach { (key, item) ->
                        val itemType = item["type"] as? String
                        val itemValue = item["value"]
                        
                        if (itemType != null && itemValue != null) {
                            try {
                                when (itemType) {
                                    "Boolean" -> putBoolean(key, itemValue as Boolean)
                                    "Int" -> putInt(key, (itemValue as Double).toInt())
                                    "Long" -> putLong(key, (itemValue as Double).toLong())
                                    "Float" -> putFloat(key, (itemValue as Double).toFloat())
                                    "String" -> putString(key, itemValue as String)
                                    "StringSet" -> {
                                        @Suppress("UNCHECKED_CAST")
                                        putStringSet(key, (itemValue as List<String>).toSet())
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }.apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { inputStream.close() } catch(e: Exception) {}
        }
    }
}
