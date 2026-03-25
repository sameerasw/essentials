package com.sameerasw.essentials.domain.model.github

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class GitHubUser(
    @SerializedName("login") val login: String,
    @SerializedName("id") val id: Long,
    @SerializedName("avatar_url") val avatarUrl: String,
    @SerializedName("name") val name: String?,
    @SerializedName("bio") val bio: String?
)
