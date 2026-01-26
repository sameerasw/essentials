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

data class DeviceCodeResponse(
    @SerializedName("device_code") val deviceCode: String,
    @SerializedName("user_code") val userCode: String,
    @SerializedName("verification_uri") val verificationUri: String,
    @SerializedName("expires_in") val expiresIn: Int,
    val interval: Int
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("scope") val scope: String?,
    val error: String?
)
