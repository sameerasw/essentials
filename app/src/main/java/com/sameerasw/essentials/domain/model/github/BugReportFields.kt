/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Models
 * File: BugReportFields.kt
 * Description: Issue field definitions, auto-fill and GitHub issue formatting for in-app bug reports.
 */

package com.sameerasw.essentials.domain.model.github

import android.content.Context
import android.os.Build

object BugReportFieldNames {
    const val SEVERITY = "Severity"
    const val ANDROID_VERSION = "Android Version"
    const val OEM = "Device (OEM)"
    const val APP_VERSION = "App Version"
    const val AREA = "Area"
}

// Keys must match the field ids in .github/ISSUE_TEMPLATE/bug_report.yml
object BugReportFormIds {
    const val DESCRIPTION = "description"
    const val SEVERITY = "severity"
    const val ANDROID_VERSION = "android-version"
    const val OEM = "oem"
    const val DEVICE = "device"
    const val APP_VERSION = "app-version"
    const val AREA = "area"
    const val ADDITIONAL = "additional"
}

data class BugReportFieldOptions(
    val severities: List<String>,
    val androidVersions: List<String>,
    val oems: List<String>,
    val areas: List<String>,
    val fieldIds: Map<String, Long> = emptyMap(),
) {
    companion object {
        val DEFAULT =
            BugReportFieldOptions(
                severities = listOf("Crash", "Broken", "Rare", "Minor", "Cosmetic"),
                androidVersions =
                    listOf(
                        "Android 17 (SDK37)",
                        "Android 16 (SDK36)",
                        "Android 15 (SDK35)",
                        "Android 14 (SDK34)",
                        "Android 13 (SDK33)",
                        "Android 12 (SDK31/32)",
                        "Android 11 (SDK30)",
                        "Android 10 (SDK29)",
                        "Android 9 (SDK28)",
                        "Android 8 (SDK26/27)",
                        "Android Canary",
                    ),
                oems =
                    listOf(
                        "Pixel",
                        "Samsung",
                        "Motorola",
                        "OnePlus",
                        "Xiaomi",
                        "Oppo",
                        "Vivo",
                        "Nothing",
                        "Other",
                    ),
                areas =
                    listOf(
                        "System",
                        "Interface",
                        "Interaction",
                        "Display",
                        "Connectivity",
                        "Protection",
                        "Tools",
                        "Other",
                    ),
                fieldIds =
                    mapOf(
                        BugReportFieldNames.APP_VERSION to 49738389L,
                        BugReportFieldNames.ANDROID_VERSION to 49738534L,
                        BugReportFieldNames.OEM to 49738619L,
                        BugReportFieldNames.SEVERITY to 49738830L,
                    ),
            )
    }
}

data class BugReportFields(
    val severity: String? = null,
    val androidVersion: String? = null,
    val oem: String? = null,
    val area: String? = null,
    val device: String = "",
    val appVersion: String = "",
) {
    fun reconcileWith(options: BugReportFieldOptions): BugReportFields =
        copy(
            severity = severity?.takeIf { it in options.severities },
            androidVersion =
                androidVersion?.takeIf { it in options.androidVersions }
                    ?: BugReportAutoFill.androidVersion(options.androidVersions),
            oem = oem?.takeIf { it in options.oems } ?: BugReportAutoFill.oem(options.oems),
            area = area?.takeIf { it in options.areas },
        )

    fun asIssueFieldValues(): Map<String, String> =
        buildMap {
            severity?.let { put(BugReportFieldNames.SEVERITY, it) }
            androidVersion?.let { put(BugReportFieldNames.ANDROID_VERSION, it) }
            oem?.let { put(BugReportFieldNames.OEM, it) }
            area?.let { put(BugReportFieldNames.AREA, it) }
            appVersion.takeIf { it.isNotBlank() }?.let { put(BugReportFieldNames.APP_VERSION, it.trim()) }
        }
}

object BugReportAutoFill {
    private val sdkPattern = Regex("""SDK(\d+)(?:/(\d+))?""")

    fun initial(
        context: Context,
        options: BugReportFieldOptions,
    ): BugReportFields =
        BugReportFields(
            androidVersion = androidVersion(options.androidVersions),
            oem = oem(options.oems),
            device = deviceModel(),
            appVersion = appVersion(context),
        )

    fun androidVersion(options: List<String>): String? {
        if (Build.VERSION.CODENAME != "REL") {
            options.firstOrNull { it.contains("Canary", ignoreCase = true) }?.let { return it }
        }
        val sdk = Build.VERSION.SDK_INT
        return options.firstOrNull { option ->
            val match = sdkPattern.find(option) ?: return@firstOrNull false
            val low = match.groupValues[1].toInt()
            val high = match.groupValues[2].toIntOrNull() ?: low
            sdk in low..high
        }
    }

    fun oem(options: List<String>): String? {
        val brands = listOf(Build.BRAND, Build.MANUFACTURER).map { it.orEmpty().lowercase() }
        if ("google" in brands) {
            options.firstOrNull { it.equals("Pixel", ignoreCase = true) }?.let { return it }
        }
        val other = options.firstOrNull { it.equals("Other", ignoreCase = true) }
        return options.firstOrNull { option ->
            option != other && brands.any { it.contains(option.lowercase()) }
        } ?: other
    }

    fun deviceModel(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty()
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model".trim()
    }

    fun appVersion(context: Context): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (e: Exception) {
            ""
        }
}

object BugReportIssueFormatter {
    // issue-fields.yml looks for this marker so issues created through the API get the bug label, type and fields
    const val APP_REPORT_MARKER = "<!-- essentials-app-bug-report -->"
    private const val NO_RESPONSE = "_No response_"
    private const val TITLE_PREFIX = "[Bug]: "
    private const val MAX_TITLE_LENGTH = 80

    fun title(description: String): String {
        val firstLine = description.trim().lineSequence().firstOrNull().orEmpty()
        val summary =
            if (firstLine.length > MAX_TITLE_LENGTH) firstLine.take(MAX_TITLE_LENGTH).trimEnd() + "…" else firstLine
        return TITLE_PREFIX + summary
    }

    fun additionalContext(
        deviceInfo: String,
        crashLogName: String?,
        crashLog: String?,
    ): String =
        buildString {
            append("<details><summary>Device info</summary>\n\n```\n")
            append(deviceInfo.trim())
            append("\n```\n</details>")
            if (crashLogName != null && crashLog != null) {
                append("\n\n<details><summary>Crash log ($crashLogName)</summary>\n\n```\n")
                append(crashLog.trim())
                append("\n```\n</details>")
            }
        }

    fun body(
        description: String,
        fields: BugReportFields,
        additionalContext: String,
    ): String {
        val sections =
            listOf(
                "Bug Description" to description.trim(),
                BugReportFieldNames.SEVERITY to fields.severity,
                BugReportFieldNames.AREA to fields.area,
                BugReportFieldNames.ANDROID_VERSION to fields.androidVersion,
                BugReportFieldNames.OEM to fields.oem,
                "Device" to fields.device.trim(),
                BugReportFieldNames.APP_VERSION to fields.appVersion.trim(),
                "Additional Context" to additionalContext,
            )
        return buildString {
            sections.forEach { (heading, value) ->
                append("### ").append(heading).append("\n\n")
                append(value?.takeIf { it.isNotBlank() } ?: NO_RESPONSE)
                append("\n\n")
            }
            append(APP_REPORT_MARKER)
        }
    }
}
