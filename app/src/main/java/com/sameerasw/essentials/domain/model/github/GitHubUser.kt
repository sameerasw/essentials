package com.sameerasw.essentials.domain.model.github

import com.google.gson.annotations.SerializedName

data class GitHubUser(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url") val avatarUrl: String,
    val name: String?,
    val bio: String?
)
