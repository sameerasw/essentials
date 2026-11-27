/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: App Ops
 * File: AppOps.kt
 * Description: App op catalog, modes and parsing of appops shell output.
 */

package com.sameerasw.essentials.domain.appops

import android.app.AppOpsManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sameerasw.essentials.R
import org.lsposed.hiddenapibypass.HiddenApiBypass

enum class AppOpMode(
    val shell: String,
    @StringRes val label: Int,
) {
    ALLOW("allow", R.string.app_ops_mode_allow),
    FOREGROUND("foreground", R.string.app_ops_mode_foreground),
    IGNORE("ignore", R.string.app_ops_mode_ignore),
    DENY("deny", R.string.app_ops_mode_deny),
    DEFAULT("default", R.string.app_ops_mode_default),
    ;

    val isRestricted: Boolean get() = this == IGNORE || this == DENY

    companion object {
        fun fromShell(value: String): AppOpMode? =
            when (value.lowercase()) {
                "allow" -> ALLOW
                "foreground" -> FOREGROUND
                "ignore" -> IGNORE
                "deny", "errored" -> DENY
                "default" -> DEFAULT
                else -> null
            }

        fun fromCode(code: Int): AppOpMode =
            when (code) {
                0 -> ALLOW
                1 -> IGNORE
                2 -> DENY
                4 -> FOREGROUND
                else -> DEFAULT
            }
    }
}

data class AppOpInfo(
    val name: String,
    val permission: String?,
    val defaultMode: AppOpMode,
    val code: Int = -1,
) {
    val label: String =
        name
            .lowercase()
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }

    @DrawableRes
    val iconRes: Int = AppOpIcons.iconFor(name)
}

object AppOpIcons {
    private val rules: List<Pair<List<String>, Int>> =
        listOf(
            listOf("RING_VOLUME") to R.drawable.rounded_ring_volume_24,
            listOf("VOLUME") to R.drawable.rounded_volume_up_24,
            listOf("TAKE_AUDIO_FOCUS") to R.drawable.rounded_headphones_24,
            listOf("TAKE_MEDIA_BUTTONS") to R.drawable.rounded_play_pause_24,
            listOf("PLAY_AUDIO") to R.drawable.rounded_music_note_24,
            listOf("CONTROL_AUDIO", "MEDIA_ROUTING") to R.drawable.rounded_speaker_group_24,
            listOf("RECORD_AUDIO_OUTPUT") to R.drawable.rounded_fiber_smart_record_24,
            listOf("MUTE_MICROPHONE") to R.drawable.rounded_mic_off_24,
            listOf("RECORD_AUDIO", "MICROPHONE", "HOTWORD", "TRIGGER_AUDIO", "INTERACTION_AUDIO", "PHONE_AUDIO") to R.drawable.rounded_mic_24,
            listOf("CAMERA") to R.drawable.rounded_photo_camera_24,
            listOf("PROJECT_MEDIA") to R.drawable.rounded_cast_24,
            listOf("ASSIST_SCREENSHOT") to R.drawable.rounded_screenshot_region_24,
            listOf("ASSIST") to R.drawable.rounded_record_voice_over_24,
            listOf("MEDIA_LOCATION") to R.drawable.rounded_add_location_alt_24,
            listOf("MOCK_LOCATION") to R.drawable.rounded_my_location_24,
            listOf("LOCATION", "GPS") to R.drawable.rounded_location_on_24,
            listOf("NEIGHBORING_CELLS") to R.drawable.rounded_signal_cellular_alt_24,
            listOf("VIBRATE") to R.drawable.rounded_mobile_vibrate_24,
            listOf("CONTACTS") to R.drawable.rounded_import_contacts_24,
            listOf("GET_ACCOUNTS") to R.drawable.rounded_person_24,
            listOf("CALL_LOG") to R.drawable.rounded_call_log_24,
            listOf("CALENDAR") to R.drawable.rounded_calendar_month_24,
            listOf("WIFI") to R.drawable.rounded_wifi_24,
            listOf("BLUETOOTH") to R.drawable.rounded_bluetooth_24,
            listOf("UWB", "RANGING") to R.drawable.rounded_devices_other_24,
            listOf("NOTIFICATION") to R.drawable.rounded_notifications_24,
            listOf("BROADCAST") to R.drawable.rounded_campaign_24,
            listOf("SMS_FINANCIAL") to R.drawable.rounded_payments_24,
            listOf("ICC") to R.drawable.rounded_sim_card_24,
            listOf("SMS", "MMS", "WAP_PUSH") to R.drawable.rounded_chat_bubble_24,
            listOf("VOICEMAIL", "SIP", "CALL", "PHONE_STATE", "PHONE_NUMBERS", "HANDOVER") to R.drawable.rounded_call_24,
            listOf("WRITE_SETTINGS", "SYSTEM_PREFERENCES") to R.drawable.rounded_settings_24,
            listOf("RESTRICTED_SETTINGS") to R.drawable.rounded_shield_lock_24,
            listOf("SYSTEM_ALERT_WINDOW", "TOAST_WINDOW", "OVERLAY") to R.drawable.rounded_ad_units_24,
            listOf("WRITE_CLIPBOARD") to R.drawable.rounded_content_copy_24,
            listOf("CLIPBOARD") to R.drawable.rounded_content_paste_24,
            listOf("TURN_SCREEN_ON") to R.drawable.rounded_brightness_6_24,
            listOf("WAKE_LOCK") to R.drawable.rounded_wb_sunny_24,
            listOf("USAGE_STATS") to R.drawable.rounded_data_usage_24,
            listOf("VPN", "IPSEC") to R.drawable.rounded_vpn_key_24,
            listOf("WALLPAPER") to R.drawable.rounded_wallpaper_24,
            listOf("FINGERPRINT", "BIOMETRIC") to R.drawable.rounded_face_24,
            listOf("TEMPERATURE") to R.drawable.rounded_device_thermostat_24,
            listOf("HEART_RATE") to R.drawable.rounded_monitor_heart_24,
            listOf("OXYGEN") to R.drawable.rounded_bloodtype_24,
            listOf("BODY_SENSORS") to R.drawable.rounded_ecg_heart_24,
            listOf("HEALTH") to R.drawable.rounded_health_and_safety_24,
            listOf("ACTIVITY_RECOGNITION") to R.drawable.rounded_directions_walk_24,
            listOf("MEDIA_AUDIO") to R.drawable.rounded_library_music_24,
            listOf("MEDIA_VIDEO") to R.drawable.rounded_movie_24,
            listOf("MEDIA_IMAGES", "VISUAL_USER_SELECTED") to R.drawable.rounded_photo_library_24,
            listOf("MANAGE_MEDIA") to R.drawable.rounded_photo_album_24,
            listOf("STORAGE") to R.drawable.rounded_folder_24,
            listOf("INSTALL_PACKAGES") to R.drawable.rounded_download_24,
            listOf("DELETE_PACKAGES") to R.drawable.rounded_delete_24,
            listOf("QUERY_ALL_PACKAGES", "INSTANT_APP") to R.drawable.rounded_apps_24,
            listOf("POWER_RESTRICTIONS", "HIBERNATION", "SUSPENSION") to R.drawable.rounded_battery_android_frame_shield_24,
            listOf("BACKGROUND", "FOREGROUND", "JOBS", "BG_START") to R.drawable.rounded_sync_24,
            listOf("EXACT_ALARM") to R.drawable.rounded_alarm_24,
            listOf("PICTURE_IN_PICTURE") to R.drawable.rounded_splitscreen_24,
            listOf("FULL_SCREEN_INTENT") to R.drawable.rounded_fullscreen_portrait_24,
            listOf("ACCESSIBILITY") to R.drawable.rounded_accessibility_new_24,
            listOf("DEVICE_IDENTIFIERS") to R.drawable.rounded_badge_24,
            listOf("ACROSS_PROFILES") to R.drawable.rounded_work_24,
            listOf("AUTO_REVOKE") to R.drawable.rounded_history_24,
            listOf("CREDENTIALS") to R.drawable.rounded_key_24,
            listOf("MOBILE_DATA") to R.drawable.rounded_mobiledata_arrows_24,
            listOf("GRAMMATICAL_GENDER") to R.drawable.rounded_translate_24,
            listOf("ARCHIVAL", "ARCHIVE") to R.drawable.rounded_archive_24,
            listOf("CONSENTED_STATE") to R.drawable.rounded_policy_24,
            listOf("TESTING") to R.drawable.rounded_bug_report_24,
            listOf("SENSOR", "DETECTION") to R.drawable.rounded_sensors_24,
        )

    @DrawableRes
    fun iconFor(opName: String): Int =
        rules.firstOrNull { (keys, _) -> keys.any { it in opName } }?.second ?: R.drawable.rounded_android_24
}

data class AppOpEntry(
    val op: String,
    val mode: AppOpMode,
    val isUidMode: Boolean,
    val lastUsed: String?,
)

fun AppOpMode.isEffectivelyAllowed(defaultMode: AppOpMode): Boolean =
    when (this) {
        AppOpMode.ALLOW, AppOpMode.FOREGROUND -> true
        AppOpMode.IGNORE, AppOpMode.DENY -> false
        AppOpMode.DEFAULT -> defaultMode != AppOpMode.IGNORE && defaultMode != AppOpMode.DENY
    }

object AppOpsCatalog {
    private val opNameRegex = Regex("^[A-Z][A-Z0-9_]+$")

    fun isValidOpName(name: String) = opNameRegex.matches(name)

    fun load(): List<AppOpInfo> {
        val cls = AppOpsManager::class.java
        val count = opCount(cls) ?: return emptyList()
        return (0 until count)
            .mapNotNull { code ->
                val name = runCatching { HiddenApiBypass.invoke(cls, null, "opToName", code) as? String }.getOrNull()
                if (name == null || !isValidOpName(name) || name == "NONE") return@mapNotNull null
                val permission = runCatching { HiddenApiBypass.invoke(cls, null, "opToPermission", code) as? String }.getOrNull()
                val defaultMode =
                    runCatching { HiddenApiBypass.invoke(cls, null, "opToDefaultMode", code) as? Int }
                        .getOrNull()
                        ?.let(AppOpMode::fromCode)
                        ?: AppOpMode.DEFAULT
                AppOpInfo(name, permission, defaultMode, code)
            }.distinctBy { it.name }
            .sortedBy { it.name }
    }

    private fun opCount(cls: Class<AppOpsManager>): Int? =
        runCatching { cls.getDeclaredField("_NUM_OP").apply { isAccessible = true }.getInt(null) }.getOrNull()
            ?: runCatching {
                HiddenApiBypass.getStaticFields(cls).firstOrNull { it.name == "_NUM_OP" }?.let {
                    it.isAccessible = true
                    it.getInt(null)
                }
            }.getOrNull()
}

object AppOpsParser {
    private const val PACKAGE_MARKER = "@@PKG "
    private val packageRegex = Regex("^[A-Za-z0-9_.]+$")
    private const val UID_PREFIX = "Uid mode: "
    private val opLineRegex = Regex("^([A-Z][A-Z0-9_]+): ([a-z]+)(.*)$")
    private val timeRegex = Regex("(?<![a-zA-Z])time=\\+?([^;]+?)(?:\\s+ago)?\\s*(?:;|$)")

    fun isValidPackage(packageName: String) = packageRegex.matches(packageName)

    fun batchCommand(packages: List<String>): String =
        packages
            .filter(::isValidPackage)
            .joinToString("; ") { "echo '$PACKAGE_MARKER$it'; appops get $it 2>/dev/null" }

    fun parseBatch(
        output: String,
        opCodes: Map<String, Int>,
    ): Map<String, Map<String, AppOpEntry>> {
        val result = mutableMapOf<String, MutableList<String>>()
        var current: MutableList<String>? = null
        output.lineSequence().forEach { line ->
            if (line.startsWith(PACKAGE_MARKER)) {
                current = mutableListOf<String>().also { result[line.removePrefix(PACKAGE_MARKER).trim()] = it }
            } else {
                current?.add(line)
            }
        }
        return result.mapValues { (_, lines) -> parse(lines, opCodes) }
    }

    fun parse(
        lines: List<String>,
        opCodes: Map<String, Int>,
    ): Map<String, AppOpEntry> {
        val entries = mutableMapOf<String, AppOpEntry>()
        val uidOps = mutableSetOf<String>()
        var inUidSection = false
        var lastUidCode = -1
        lines.forEach { raw ->
            if (raw.isEmpty() || raw.first().isWhitespace()) return@forEach
            var line = raw.trim()
            if (line.startsWith(UID_PREFIX)) {
                inUidSection = true
                line = line.removePrefix(UID_PREFIX)
            }
            val match = opLineRegex.find(line) ?: return@forEach
            val op = match.groupValues[1]
            val mode = AppOpMode.fromShell(match.groupValues[2]) ?: return@forEach
            val rest = match.groupValues[3]
            val code = opCodes[op]
            if (inUidSection) {
                val hasTiming = "time=" in rest || "duration=" in rest
                val orderReset = code != null && lastUidCode >= 0 && code < lastUidCode
                if (hasTiming || orderReset || op in uidOps) inUidSection = false
            }
            if (inUidSection) {
                uidOps += op
                if (code != null) lastUidCode = code
                entries[op] = AppOpEntry(op, mode, isUidMode = true, lastUsed = null)
                return@forEach
            }
            val lastUsed = timeRegex.find(rest)?.groupValues?.get(1)?.trim()
            val existing = entries[op]
            entries[op] =
                if (existing?.isUidMode == true && existing.mode != AppOpMode.DEFAULT) {
                    existing.copy(lastUsed = lastUsed ?: existing.lastUsed)
                } else {
                    AppOpEntry(op, mode, isUidMode = false, lastUsed = lastUsed)
                }
        }
        return entries
    }
}
