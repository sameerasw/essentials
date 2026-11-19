package com.sameerasw.essentials.domain.diy

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sameerasw.essentials.BuildConfig
import com.sameerasw.essentials.domain.configshare.ConfigVersionRelation
import com.sameerasw.essentials.domain.model.AppPermission
import com.sameerasw.essentials.utils.ShellUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

enum class AutomationParseError { INVALID, TOO_LARGE, NEWER_SCHEMA, UNSUPPORTED, CONFLICT, EMPTY }

data class ParsedAutomation(
    val automation: Automation,
    val appVersionName: String,
    val appVersionCode: Int,
    val needsSetup: Boolean,
    val changesSystemSettings: Boolean,
)

sealed interface AutomationParseResult {
    data class Success(val parsed: ParsedAutomation) : AutomationParseResult

    data class Failure(val error: AutomationParseError) : AutomationParseResult
}

object DIYShareManager {
    const val FORMAT = "essentials-automation"
    const val FEATURE = "DIY"
    const val SCHEMA = 1
    const val MAX_BYTES = 64 * 1024
    private const val MAX_ACTIONS = 100
    private const val MAX_TEXT = 300

    private val prettyGson = GsonBuilder().setPrettyPrinting().create()

    fun fileName(automation: Automation): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        return "diy_${automation.type.name.lowercase(Locale.US)}_$stamp.essentials.json"
    }

    fun versionRelation(parsed: ParsedAutomation): ConfigVersionRelation =
        when {
            parsed.appVersionCode < BuildConfig.VERSION_CODE -> ConfigVersionRelation.OLDER
            parsed.appVersionCode > BuildConfig.VERSION_CODE -> ConfigVersionRelation.NEWER
            else -> ConfigVersionRelation.SAME
        }

    fun export(automation: Automation): String {
        val clean = scrub(automation).copy(id = "", isEnabled = true)
        val root = JsonObject()
        root.addProperty("format", FORMAT)
        root.addProperty("schema", SCHEMA)
        root.addProperty("feature", FEATURE)
        root.addProperty("appVersionName", BuildConfig.VERSION_NAME)
        root.addProperty("appVersionCode", BuildConfig.VERSION_CODE)
        root.addProperty(
            "createdAt",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date()),
        )
        root.add("automation", DIYRepository.automationToJson(clean))
        return prettyGson.toJson(root)
    }

    fun parse(
        context: Context,
        text: String?,
    ): AutomationParseResult {
        if (text == null || text.length > MAX_BYTES) return AutomationParseResult.Failure(AutomationParseError.TOO_LARGE)
        val root =
            try {
                JsonParser.parseString(text) as? JsonObject
            } catch (e: Exception) {
                null
            } ?: return AutomationParseResult.Failure(AutomationParseError.INVALID)

        if (root.str("format") != FORMAT || root.str("feature") != FEATURE) {
            return AutomationParseResult.Failure(AutomationParseError.INVALID)
        }
        val schema = root.int("schema") ?: return AutomationParseResult.Failure(AutomationParseError.INVALID)
        if (schema > SCHEMA) return AutomationParseResult.Failure(AutomationParseError.NEWER_SCHEMA)
        val element = root.get("automation") as? JsonObject ?: return AutomationParseResult.Failure(AutomationParseError.INVALID)

        val raw = DIYRepository.automationFromJson(element) ?: return AutomationParseResult.Failure(AutomationParseError.INVALID)
        @Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")
        val lists = listOf(raw.actions, raw.entryActions, raw.exitActions)
        if (lists.any { l -> l != null && (l.size > MAX_ACTIONS || l.any { (it as Action?) == null }) }) {
            return AutomationParseResult.Failure(AutomationParseError.UNSUPPORTED)
        }
        @Suppress("SENSELESS_COMPARISON")
        val typeOk = raw.type != null
        if (!typeOk) return AutomationParseResult.Failure(AutomationParseError.UNSUPPORTED)
        if (raw.type == Automation.Type.TRIGGER && raw.trigger == null) return AutomationParseResult.Failure(AutomationParseError.UNSUPPORTED)
        if (raw.type == Automation.Type.STATE && raw.state == null) return AutomationParseResult.Failure(AutomationParseError.UNSUPPORTED)
        val hasActions = raw.actionList.isNotEmpty() || raw.entryActionList.isNotEmpty() || raw.exitActionList.isNotEmpty()
        if (!hasActions && raw.type != Automation.Type.APP) return AutomationParseResult.Failure(AutomationParseError.EMPTY)

        val type = resolveType(raw.type) ?: return AutomationParseResult.Failure(AutomationParseError.CONFLICT)
        @Suppress("USELESS_ELVIS")
        val installedApps = (raw.selectedApps ?: emptyList()).filter { isInstalled(context, it) }
        var clean = sanitize(raw).copy(id = UUID.randomUUID().toString(), type = type, selectedApps = installedApps)
        val changesSettings =
            (clean.actionList + clean.entryActionList + clean.exitActionList).any { it is Action.CustomSettings }
        val needsSetup = needsSetup(clean) || (raw.type == Automation.Type.APP && installedApps.isEmpty())
        if (needsSetup) clean = clean.copy(isEnabled = false)

        return AutomationParseResult.Success(
            ParsedAutomation(
                automation = clean,
                appVersionName = root.str("appVersionName")?.take(32) ?: "?",
                appVersionCode = root.int("appVersionCode") ?: 0,
                needsSetup = needsSetup,
                changesSystemSettings = changesSettings,
            ),
        )
    }

    fun missingPermissions(
        context: Context,
        automation: Automation,
    ): List<AppPermission> {
        val actionKeys =
            (automation.actionList + automation.entryActionList + automation.exitActionList).flatMap { it.permissions }
        val keys = actionKeys + (automation.trigger?.permissions ?: emptyList())
        return keys
            .mapNotNull { key ->
                if (key.equals("SHIZUKU", true) || key.equals("ROOT", true)) {
                    if (ShellUtils.isRootEnabled(context)) AppPermission.ROOT else AppPermission.SHIZUKU
                } else {
                    AppPermission.fromKey(key)
                }
            }.distinct()
            .filterNot { it.isGranted(context) }
    }

    fun import(
        context: Context,
        parsed: ParsedAutomation,
    ): Boolean {
        val missing = missingPermissions(context, parsed.automation)
        val enabled = parsed.automation.isEnabled && missing.isEmpty()
        DIYRepository.addAutomation(parsed.automation.copy(isEnabled = enabled))
        return enabled
    }

    private fun resolveType(type: Automation.Type): Automation.Type? {
        val existing = DIYRepository.automations.value
        return when (type) {
            Automation.Type.ACTION_SHORTCUT ->
                type.takeIf { existing.none { it.type == Automation.Type.ACTION_SHORTCUT } }
            Automation.Type.PIXEL_SEARCHBAR ->
                type.takeIf { existing.none { it.type == Automation.Type.PIXEL_SEARCHBAR } }
            Automation.Type.ACCESSIBILITY_SHORTCUT,
            Automation.Type.ACCESSIBILITY_SHORTCUT_1,
            Automation.Type.ACCESSIBILITY_SHORTCUT_2,
            Automation.Type.ACCESSIBILITY_SHORTCUT_3,
            -> {
                val used =
                    existing
                        .mapNotNull {
                            when (it.type) {
                                Automation.Type.ACCESSIBILITY_SHORTCUT, Automation.Type.ACCESSIBILITY_SHORTCUT_1 -> 1
                                Automation.Type.ACCESSIBILITY_SHORTCUT_2 -> 2
                                Automation.Type.ACCESSIBILITY_SHORTCUT_3 -> 3
                                else -> null
                            }
                        }.toSet()
                when ((1..3).firstOrNull { it !in used }) {
                    1 -> Automation.Type.ACCESSIBILITY_SHORTCUT_1
                    2 -> Automation.Type.ACCESSIBILITY_SHORTCUT_2
                    3 -> Automation.Type.ACCESSIBILITY_SHORTCUT_3
                    else -> null
                }
            }
            else -> type
        }
    }

    private fun scrub(a: Automation): Automation =
        a.copy(
            trigger =
                when (val t = a.trigger) {
                    is Trigger.BluetoothConnected -> t.copy(deviceAddress = "", deviceName = "")
                    is Trigger.BluetoothDisconnected -> t.copy(deviceAddress = "", deviceName = "")
                    is Trigger.WifiConnected -> t.copy(ssid = "")
                    is Trigger.WifiDisconnected -> t.copy(ssid = "")
                    is Trigger.ArriveAtLocation -> t.copy(latitude = 0.0, longitude = 0.0, placeName = "")
                    is Trigger.LeaveLocation -> t.copy(latitude = 0.0, longitude = 0.0, placeName = "")
                    else -> t
                },
            state =
                when (val s = a.state) {
                    is State.CalendarEvent -> s.copy(calendarIds = emptySet())
                    else -> s
                },
            actions = a.actionList.map(::scrubAction),
            entryActions = a.entryActions?.map(::scrubAction),
            exitActions = a.exitActions?.map(::scrubAction),
            entryAction = a.entryAction?.let(::scrubAction),
            exitAction = a.exitAction?.let(::scrubAction),
        )

    private fun scrubAction(action: Action): Action =
        when (action) {
            is Action.OpenActivity -> action.copy(customIconPath = "")
            is Action.FreezeTag -> action.copy(tagIds = emptyList())
            else -> action
        }

    private fun sanitize(a: Automation): Automation =
        a.copy(
            actions = a.actionList.map(::sanitizeAction),
            entryActions = a.entryActions?.map(::sanitizeAction),
            exitActions = a.exitActions?.map(::sanitizeAction),
            entryAction = a.entryAction?.let(::sanitizeAction),
            exitAction = a.exitAction?.let(::sanitizeAction),
        )

    private val settingKeyRegex = Regex("^[A-Za-z0-9_.:-]{1,100}$")

    private fun sanitizeAction(action: Action): Action =
        when (action) {
            is Action.Wait -> action.copy(seconds = action.seconds.coerceIn(0, 3600))
            is Action.SetVolume -> action.copy(level = action.level.coerceIn(0, 100))
            is Action.SendNotification ->
                action.copy(
                    notificationTitle = action.notificationTitle.take(MAX_TEXT),
                    body = action.body.take(MAX_TEXT),
                    label = action.label.take(MAX_TEXT),
                )
            is Action.OpenActivity -> action.copy(customIconPath = "")
            is Action.CustomSettings ->
                action.copy(
                    entries =
                        action.entries
                            .filter { settingKeyRegex.matches(it.key) && it.value.length <= 200 && it.value.none { c -> c < ' ' } }
                            .take(50),
                )
            else -> action
        }

    private fun needsSetup(a: Automation): Boolean {
        val triggerMissing =
            when (val t = a.trigger) {
                is Trigger.BluetoothConnected -> t.deviceAddress.isEmpty()
                is Trigger.BluetoothDisconnected -> t.deviceAddress.isEmpty()
                is Trigger.WifiConnected -> t.ssid.isEmpty()
                is Trigger.WifiDisconnected -> t.ssid.isEmpty()
                is Trigger.ArriveAtLocation -> t.latitude == 0.0 && t.longitude == 0.0
                is Trigger.LeaveLocation -> t.latitude == 0.0 && t.longitude == 0.0
                else -> false
            }
        val stateMissing = (a.state as? State.CalendarEvent)?.calendarIds?.isEmpty() == true
        val actionMissing =
            (a.actionList + a.entryActionList + a.exitActionList).any { it is Action.FreezeTag && it.tagIds.isEmpty() }
        return triggerMissing || stateMissing || actionMissing
    }

    private fun isInstalled(
        context: Context,
        pkg: String,
    ): Boolean =
        try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: Exception) {
            false
        }

    private fun JsonObject.str(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun JsonObject.int(name: String): Int? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt
}
