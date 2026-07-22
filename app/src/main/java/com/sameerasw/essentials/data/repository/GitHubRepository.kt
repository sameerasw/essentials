package com.sameerasw.essentials.data.repository

import com.google.gson.Gson
import com.sameerasw.essentials.domain.model.github.GitHubRelease
import com.sameerasw.essentials.domain.model.github.GitHubRepo
import com.sameerasw.essentials.domain.model.github.GitHubUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class GitHubRepository {
    private val gson = Gson()

    suspend fun getRepoInfo(owner: String, repo: String, token: String? = null): GitHubRepo? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/$owner/$repo")
                val connection = url.openConnection() as HttpURLConnection
                if (token != null) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }
                if (connection.responseCode == 200) {
                    val data = connection.inputStream.bufferedReader().readText()
                    gson.fromJson(data, GitHubRepo::class.java)
                } else if (connection.responseCode == 403 || connection.responseCode == 429) {
                    throw Exception("RATE_LIMIT")
                } else null
            } catch (e: Exception) {
                if (e.message == "RATE_LIMIT") throw e
                e.printStackTrace()
                null
            }
        }

    suspend fun getLatestRelease(
        owner: String,
        repo: String,
        token: String? = null
    ): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            if (token != null) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (connection.responseCode == 200) {
                val data = connection.inputStream.bufferedReader().readText()
                gson.fromJson(data, GitHubRelease::class.java)
            } else if (connection.responseCode == 403 || connection.responseCode == 429) {
                throw Exception("RATE_LIMIT")
            } else null
        } catch (e: Exception) {
            if (e.message == "RATE_LIMIT") throw e
            e.printStackTrace()
            null
        }
    }

    suspend fun getReleases(
        owner: String,
        repo: String,
        token: String? = null
    ): List<GitHubRelease> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$owner/$repo/releases")
            val connection = url.openConnection() as HttpURLConnection
            if (token != null) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (connection.responseCode == 200) {
                val data = connection.inputStream.bufferedReader().readText()
                gson.fromJson(data, Array<GitHubRelease>::class.java).toList()
            } else if (connection.responseCode == 403 || connection.responseCode == 429) {
                throw Exception("RATE_LIMIT")
            } else emptyList()
        } catch (e: Exception) {
            if (e.message == "RATE_LIMIT") throw e
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getReadme(owner: String, repo: String, token: String? = null): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/$owner/$repo/readme")
                val connection = url.openConnection() as HttpURLConnection
                if (token != null) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }
                if (connection.responseCode == 200) {
                    val data = connection.inputStream.bufferedReader().readText()
                    val readmeMap = gson.fromJson(data, Map::class.java)
                    val content = readmeMap["content"] as? String ?: return@withContext null
                    val encoding = readmeMap["encoding"] as? String
                    if (encoding == "base64") {
                        String(android.util.Base64.decode(content, android.util.Base64.DEFAULT))
                    } else content
                } else if (connection.responseCode == 403 || connection.responseCode == 429) {
                    throw Exception("RATE_LIMIT")
                } else null
            } catch (e: Exception) {
                if (e.message == "RATE_LIMIT") throw e
                e.printStackTrace()
                null
            }
        }

    suspend fun getUserProfile(token: String): GitHubUser? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/user")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/vnd.github+json")

            if (connection.responseCode == 200) {
                val data = connection.inputStream.bufferedReader().readText()
                gson.fromJson(data, GitHubUser::class.java)
            } else if (connection.responseCode == 403 || connection.responseCode == 429) {
                throw Exception("RATE_LIMIT")
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun triggerWorkflowDispatch(
        token: String,
        owner: String,
        repo: String,
        workflowFile: String,
        ref: String,
        inputs: Map<String, String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url =
                URL("https://api.github.com/repos/$owner/$repo/actions/workflows/$workflowFile/dispatches")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("Content-Type", "application/json")

            val payload = gson.toJson(
                mapOf(
                    "ref" to ref,
                    "inputs" to inputs
                )
            )

            connection.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
            }

            connection.responseCode == 204
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getOpenTranslationPRs(
        owner: String,
        repo: String,
        author: String,
        token: String? = null
    ): List<com.sameerasw.essentials.domain.model.github.GitHubPullRequest> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$owner/$repo/pulls?state=open")
            val connection = url.openConnection() as HttpURLConnection
            if (token != null) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode == 200) {
                val data = connection.inputStream.bufferedReader().readText()
                val allPrs = gson.fromJson(data, Array<com.sameerasw.essentials.domain.model.github.GitHubPullRequest>::class.java).toList()
                val targetRef = "translations-$author"
                allPrs.filter { pr ->
                    pr.user?.login.equals(author, ignoreCase = true) ||
                            pr.head?.ref?.equals(targetRef, ignoreCase = true) == true
                }
            } else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

