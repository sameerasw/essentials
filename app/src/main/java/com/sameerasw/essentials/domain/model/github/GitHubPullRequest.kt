package com.sameerasw.essentials.domain.model.github

import com.google.gson.annotations.SerializedName

data class GitHubPullRequest(
    val number: Int,
    val title: String,
    val state: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("updated_at") val updatedAt: String,
    val user: GitHubUser? = null,
    val head: GitHubHeadRef? = null
)

data class GitHubHeadRef(
    val ref: String
)
