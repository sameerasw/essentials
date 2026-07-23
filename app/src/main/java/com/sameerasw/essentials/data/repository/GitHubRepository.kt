package com.sameerasw.essentials.data.repository

import android.util.Log
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

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream?.bufferedReader()?.readText() ?: ""
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: ""
            }
            Log.d("GitHubRepository", "triggerWorkflowDispatch responseCode: $responseCode, body: $responseText")

            responseCode == 204
        } catch (e: Exception) {
            Log.e("GitHubRepository", "triggerWorkflowDispatch exception: ${e.message}", e)
            false
        }
    }

    suspend fun addDiscussionComment(
        token: String,
        owner: String,
        repo: String,
        discussionNumber: Int,
        body: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val queryGetDiscussion = """
                query {
                  repository(owner: "$owner", name: "$repo") {
                    discussion(number: $discussionNumber) {
                      id
                    }
                  }
                }
            """.trimIndent()

            val graphqlUrl = URL("https://api.github.com/graphql")
            val conn1 = graphqlUrl.openConnection() as HttpURLConnection
            conn1.requestMethod = "POST"
            conn1.doOutput = true
            conn1.setRequestProperty("Authorization", "Bearer $token")
            conn1.setRequestProperty("Content-Type", "application/json")

            val payload1 = gson.toJson(mapOf("query" to queryGetDiscussion))
            conn1.outputStream.use { os -> os.write(payload1.toByteArray(Charsets.UTF_8)) }

            val code1 = conn1.responseCode
            val text1 = if (code1 in 200..299) conn1.inputStream.bufferedReader().readText() else conn1.errorStream?.bufferedReader()?.readText() ?: ""
            Log.d("GitHubRepository", "getDiscussionId responseCode: $code1, body: $text1")

            val jsonObject1 = gson.fromJson(text1, Map::class.java)
            val data1 = jsonObject1["data"] as? Map<*, *>
            val repository1 = data1?.get("repository") as? Map<*, *>
            val discussion1 = repository1?.get("discussion") as? Map<*, *>
            val discussionId = discussion1?.get("id") as? String

            if (discussionId.isNullOrEmpty()) {
                Log.e("GitHubRepository", "Discussion ID not found for #$discussionNumber")
                return@withContext false
            }

            val escapedBody = body.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            val mutationAddComment = """
                mutation {
                  addDiscussionComment(input: {discussionId: "$discussionId", body: "$escapedBody"}) {
                    comment {
                      id
                    }
                  }
                }
            """.trimIndent()

            val conn2 = graphqlUrl.openConnection() as HttpURLConnection
            conn2.requestMethod = "POST"
            conn2.doOutput = true
            conn2.setRequestProperty("Authorization", "Bearer $token")
            conn2.setRequestProperty("Content-Type", "application/json")

            val payload2 = gson.toJson(mapOf("query" to mutationAddComment))
            conn2.outputStream.use { os -> os.write(payload2.toByteArray(Charsets.UTF_8)) }

            val code2 = conn2.responseCode
            val text2 = if (code2 in 200..299) conn2.inputStream.bufferedReader().readText() else conn2.errorStream?.bufferedReader()?.readText() ?: ""
            Log.d("GitHubRepository", "addDiscussionComment responseCode: $code2, body: $text2")

            val jsonObject2 = gson.fromJson(text2, Map::class.java)
            val data2 = jsonObject2["data"] as? Map<*, *>
            val addCommentResult = data2?.get("addDiscussionComment") as? Map<*, *>
            val commentObj = addCommentResult?.get("comment") as? Map<*, *>
            val commentId = commentObj?.get("id") as? String

            return@withContext !commentId.isNullOrEmpty()
        } catch (e: Exception) {
            Log.e("GitHubRepository", "addDiscussionComment exception: ${e.message}", e)
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

