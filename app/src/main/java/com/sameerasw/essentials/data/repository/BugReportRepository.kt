/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Data & Repository Layer
 * File: BugReportRepository.kt
 * Description: Loads bug report issue field options from GitHub and files bug reports as GitHub issues.
 */

package com.sameerasw.essentials.data.repository

import android.util.Log
import com.google.gson.Gson
import com.sameerasw.essentials.domain.model.github.BugReportFieldNames
import com.sameerasw.essentials.domain.model.github.BugReportFieldOptions
import com.sameerasw.essentials.domain.model.github.BugReportFormIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class BugReportRepository(
    private val owner: String = "sameerasw",
    private val repo: String = "essentials",
) {
    private val gson = Gson()

    suspend fun fetchFieldOptions(token: String?): BugReportFieldOptions =
        withContext(Dispatchers.IO) {
            token?.let { fetchFromIssueFields(it) }
                ?: fetchFromTemplate()
                ?: BugReportFieldOptions.DEFAULT
        }

    suspend fun createIssue(
        token: String,
        title: String,
        body: String,
        fieldValues: Map<String, String>,
        fieldIds: Map<String, Long>,
    ): String? =
        withContext(Dispatchers.IO) {
            val issueFieldValues =
                fieldValues.mapNotNull { (name, value) ->
                    fieldIds[name]?.let { mapOf("field_id" to it, "value" to value) }
                }
            val fullPayload =
                mapOf(
                    "title" to title,
                    "body" to body,
                    "labels" to listOf("bug"),
                    "type" to "Bug",
                    "assignees" to listOf(owner),
                    "issue_field_values" to issueFieldValues,
                )

            // Reporters without triage access can be rejected for type/fields, so retry with the
            // plain issue and let the issue-fields workflow fill the rest from the body.
            postIssue(token, fullPayload) ?: postIssue(token, mapOf("title" to title, "body" to body))
        }

    private fun postIssue(
        token: String,
        payload: Map<String, Any>,
    ): String? =
        try {
            val connection = URL("https://api.github.com/repos/$owner/$repo/issues").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(gson.toJson(payload).toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            if (code == 201) {
                val response = gson.fromJson(connection.inputStream.bufferedReader().readText(), Map::class.java)
                response["html_url"] as? String
            } else {
                Log.w(TAG, "createIssue $code: ${connection.errorStream?.bufferedReader()?.readText()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "createIssue failed", e)
            null
        }

    private fun fetchFromIssueFields(token: String): BugReportFieldOptions? =
        try {
            val query =
                """
                query {
                  repository(owner: "$owner", name: "$repo") {
                    issueFields(first: 50) {
                      nodes {
                        __typename
                        ... on IssueFieldText { name fullDatabaseId }
                        ... on IssueFieldSingleSelect { name fullDatabaseId options { name priority } }
                      }
                    }
                  }
                }
                """.trimIndent()

            val connection = URL("https://api.github.com/graphql").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("GraphQL-Features", "issue_fields, repo_issue_fields")
            connection.outputStream.use {
                it.write(gson.toJson(mapOf("query" to query)).toByteArray(Charsets.UTF_8))
            }

            if (connection.responseCode != 200) {
                null
            } else {
                val response = gson.fromJson(connection.inputStream.bufferedReader().readText(), Map::class.java)
                val nodes =
                    ((response["data"] as? Map<*, *>)?.get("repository") as? Map<*, *>)
                        ?.let { it["issueFields"] as? Map<*, *> }
                        ?.let { it["nodes"] as? List<*> }
                        ?.filterIsInstance<Map<*, *>>()
                        .orEmpty()

                val byName = nodes.associateBy { it["name"] as? String }
                val fieldIds =
                    nodes
                        .mapNotNull { node ->
                            val name = node["name"] as? String ?: return@mapNotNull null
                            val id = node["fullDatabaseId"]?.toString()?.toDoubleOrNull()?.toLong() ?: return@mapNotNull null
                            name to id
                        }.toMap()

                fun optionsOf(name: String): List<String> =
                    (byName[name]?.get("options") as? List<*>)
                        ?.filterIsInstance<Map<*, *>>()
                        ?.sortedBy { (it["priority"] as? Number)?.toInt() ?: Int.MAX_VALUE }
                        ?.mapNotNull { it["name"] as? String }
                        .orEmpty()

                val defaults = BugReportFieldOptions.DEFAULT
                BugReportFieldOptions(
                    severities = optionsOf(BugReportFieldNames.SEVERITY).ifEmpty { defaults.severities },
                    androidVersions = optionsOf(BugReportFieldNames.ANDROID_VERSION).ifEmpty { defaults.androidVersions },
                    oems = optionsOf(BugReportFieldNames.OEM).ifEmpty { defaults.oems },
                    areas = optionsOf(BugReportFieldNames.AREA).ifEmpty { defaults.areas },
                    fieldIds = defaults.fieldIds + fieldIds,
                ).takeIf { nodes.isNotEmpty() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Issue fields unavailable", e)
            null
        }

    private fun fetchFromTemplate(): BugReportFieldOptions? =
        try {
            val connection =
                URL("https://raw.githubusercontent.com/$owner/$repo/main/.github/ISSUE_TEMPLATE/bug_report.yml")
                    .openConnection() as HttpURLConnection
            if (connection.responseCode != 200) {
                null
            } else {
                val dropdowns = parseDropdownOptions(connection.inputStream.bufferedReader().readText())
                val defaults = BugReportFieldOptions.DEFAULT
                BugReportFieldOptions(
                    severities = dropdowns[BugReportFormIds.SEVERITY] ?: defaults.severities,
                    androidVersions = dropdowns[BugReportFormIds.ANDROID_VERSION] ?: defaults.androidVersions,
                    oems = dropdowns[BugReportFormIds.OEM] ?: defaults.oems,
                    areas = dropdowns[BugReportFormIds.AREA] ?: defaults.areas,
                    fieldIds = defaults.fieldIds,
                ).takeIf { dropdowns.isNotEmpty() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bug report template unavailable", e)
            null
        }

    // Minimal reader for the dropdowns in an issue form, not a general YAML parser.
    internal fun parseDropdownOptions(yaml: String): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        var isDropdown = false
        var currentId: String? = null
        var collecting = false
        var optionsIndent = 0
        val options = mutableListOf<String>()

        fun flush() {
            val id = currentId
            if (isDropdown && id != null && options.isNotEmpty()) result[id] = options.toList()
            options.clear()
            collecting = false
        }

        yaml.lineSequence().forEach { line ->
            val trimmed = line.trim()
            val indent = line.length - line.trimStart().length
            when {
                trimmed.startsWith("- type:") -> {
                    flush()
                    isDropdown = trimmed.removePrefix("- type:").trim() == "dropdown"
                    currentId = null
                }
                !isDropdown -> Unit
                collecting && trimmed.startsWith("- ") && indent >= optionsIndent ->
                    options.add(trimmed.removePrefix("- ").trim().trim('"', '\''))
                collecting && trimmed.isNotEmpty() -> collecting = false
                trimmed.startsWith("id:") && currentId == null ->
                    currentId = trimmed.removePrefix("id:").trim().trim('"', '\'')
                trimmed == "options:" -> {
                    collecting = true
                    optionsIndent = indent
                }
            }
        }
        flush()
        return result
    }

    private companion object {
        const val TAG = "BugReportRepository"
    }
}
