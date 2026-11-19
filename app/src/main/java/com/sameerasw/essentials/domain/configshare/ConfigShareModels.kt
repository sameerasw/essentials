package com.sameerasw.essentials.domain.configshare

data class ConfigSection(
    val id: String,
    val titleRes: Int,
    val defaultSelected: Boolean = true,
)

data class ConfigShareDescriptor(
    val featureId: String,
    val prefixes: List<String>,
    val sections: List<ConfigSection>,
    val sectionOf: (String) -> String?,
)

enum class ConfigVersionRelation { SAME, OLDER, NEWER }

enum class ConfigParseError { INVALID, TOO_LARGE, WRONG_FEATURE, NEWER_SCHEMA, EMPTY }

data class ParsedConfig(
    val featureId: String,
    val appVersionName: String,
    val appVersionCode: Int,
    val createdAt: String,
    val entriesBySection: Map<String, Map<String, Any>>,
) {
    val sectionIds: Set<String> get() = entriesBySection.keys
}

sealed interface ConfigParseResult {
    data class Success(val config: ParsedConfig) : ConfigParseResult

    data class Failure(val error: ConfigParseError) : ConfigParseResult
}
