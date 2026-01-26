package com.sameerasw.essentials.domain.model.github

import com.google.gson.annotations.SerializedName

data class GitHubRepo(
    val id: Long,
    val name: String,
    @SerializedName("full_name") val fullName: String,
    val description: String?,
    @SerializedName("stargazers_count") val stars: Int,
    val owner: GitHubOwner
)

data class GitHubOwner(
    val login: String,
    @SerializedName("avatar_url") val avatarUrl: String
)

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    val name: String?,
    val body: String?,
    @SerializedName("published_at") val publishedAt: String,
    @SerializedName("html_url") val htmlUrl: String,
    val prerelease: Boolean = false,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String
)
